package org.ljcode.myPlugin.managers;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.ljcode.myPlugin.MyPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 环境数据调度器（v1.2.0 聚合报告模式）
 *
 * 不再逐玩家轮询推送（旧模式5-30秒一条会导致K10频繁切屏，城市大屏被环境页刷掉），
 * 而是按固定周期（默认60秒）将所有在线玩家的环境数据汇总为一条 environment_summary
 * 消息发送，K10端到达后临时显示约8秒并自动返回城市大屏。
 */
public class EnvironmentDataScheduler {

    private final MyPlugin plugin;
    private final EnvironmentDataCalculator calculator;
    private final K10TCPManager tcpManager;
    private final EnvironmentDataReceiver receiver;
    private final Gson gson = new Gson();

    private BukkitRunnable calculationTask;
    private BukkitRunnable responseCheckTask;

    private int reportInterval;
    private int maxPlayersInReport;

    private final AtomicLong lastCalculationTime;
    private final AtomicLong lastResponseTime;
    private final ConcurrentHashMap<String, Long> pendingRequests;

    private volatile boolean isRunning;
    private long totalReports;
    private long successfulSends;
    private long failedSends;
    private int lastReportedPlayerCount;

    public EnvironmentDataScheduler(MyPlugin plugin, EnvironmentDataCalculator calculator,
                                    K10TCPManager tcpManager, EnvironmentDataReceiver receiver) {
        this.plugin = plugin;
        this.calculator = calculator;
        this.tcpManager = tcpManager;
        this.receiver = receiver;
        this.lastCalculationTime = new AtomicLong(0);
        this.lastResponseTime = new AtomicLong(0);
        this.pendingRequests = new ConcurrentHashMap<>();
        this.isRunning = false;
        this.totalReports = 0;
        this.successfulSends = 0;
        this.failedSends = 0;
        this.lastReportedPlayerCount = 0;

        loadConfig();
    }

    private void loadConfig() {
        // v1.2.0: 旧的 base/min/max-interval 动态间隔已移除，改为固定报告周期
        this.reportInterval = plugin.getConfig().getInt("environment.report-interval", 1200);
        if (reportInterval < 200) {
            reportInterval = 200; // 下限10秒，防止误配置刷屏
        }
        this.maxPlayersInReport = plugin.getConfig().getInt("environment.max-players-in-report", 20);
    }

    public void start() {
        if (isRunning) {
            Bukkit.getLogger().warning("[环境数据] 调度器已在运行中");
            return;
        }

        isRunning = true;
        Bukkit.getLogger().info("[环境数据] 启动环境数据调度器（聚合报告模式）...");

        calculationTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    collectAndSendSummary();
                } catch (Exception e) {
                    Bukkit.getLogger().severe("[环境数据] 聚合报告任务异常: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };

        // 同步任务：collectAndSendSummary 调用 world.getBlockAt / getOnlinePlayers
        // 等非线程安全的Bukkit API；HTTP发送由K10管理器的独立线程池处理，不会阻塞主线程
        calculationTask.runTaskTimer(plugin, 100L, reportInterval);

        responseCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkPendingResponses();
            }
        };

        responseCheckTask.runTaskTimerAsynchronously(plugin, 100L, 100L);

        Bukkit.getLogger().info("[环境数据] 调度器已启动，报告周期: " + reportInterval + " ticks ("
                + (reportInterval / 20) + "秒)");
    }

    public void stop() {
        if (!isRunning) {
            return;
        }

        isRunning = false;

        if (calculationTask != null) {
            calculationTask.cancel();
            calculationTask = null;
        }

        if (responseCheckTask != null) {
            responseCheckTask.cancel();
            responseCheckTask = null;
        }

        Bukkit.getLogger().info("[环境数据] 调度器已停止");
        logStatistics();
    }

    /**
     * 采集所有在线玩家的环境数据，聚合成一条 environment_summary 消息发送
     */
    private void collectAndSendSummary() {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        if (online.isEmpty()) {
            return; // 无人在线不发送，K10保持城市大屏
        }

        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> players = new ArrayList<>();
        int skipped = 0;

        for (Player player : online) {
            if (players.size() >= maxPlayersInReport) {
                skipped++;
                continue;
            }

            try {
                Map<String, Object> envData = calculator.calculateEnvironmentData(player);
                if (!validateEnvironmentData(envData)) {
                    skipped++;
                    continue;
                }

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", player.getName());
                row.put("temperature", envData.get("temperature"));
                row.put("humidity", envData.get("humidity"));
                row.put("light", envData.get("light"));
                row.put("wind_speed", envData.get("wind_speed"));
                row.put("weather", envData.get("weather"));
                row.put("biome", envData.get("biome"));
                players.add(row);
            } catch (Exception e) {
                skipped++;
                Bukkit.getLogger().warning("[环境数据] 玩家 " + player.getName() + " 数据计算失败: " + e.getMessage());
            }
        }

        if (players.isEmpty()) {
            Bukkit.getLogger().warning("[环境数据] 本周期无有效数据，跳过发送");
            failedSends++;
            return;
        }

        String requestId = "env_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 10000);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("event", "environment_summary");
        summary.put("request_id", requestId);
        summary.put("timestamp", System.currentTimeMillis());
        summary.put("player_count", online.size());
        summary.put("players", players);

        String jsonData = gson.toJson(summary);
        pendingRequests.put(requestId, System.currentTimeMillis());

        boolean sendSuccess = sendToK10(jsonData);

        lastCalculationTime.set(System.currentTimeMillis());
        totalReports++;
        lastReportedPlayerCount = online.size();

        if (sendSuccess) {
            successfulSends++;
        } else {
            failedSends++;
        }

        if (plugin.getConfig().getBoolean("environment.debug-mode", false)) {
            Bukkit.getLogger().info("[环境数据] ✓ 聚合报告完成 | 耗时: " +
                    (System.currentTimeMillis() - startTime) + "ms | 玩家: " + players.size() +
                    "/" + online.size() + (skipped > 0 ? " (跳过" + skipped + ")" : "") +
                    " | JSON: " + jsonData.length() + "字符");
        }
    }

    /**
     * 验证单个玩家环境数据的完整性和合理性
     */
    private boolean validateEnvironmentData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return false;
        }

        if (!data.containsKey("temperature") || !data.containsKey("humidity") ||
            !data.containsKey("light") || !data.containsKey("wind_speed")) {
            return false;
        }

        Object tempObj = data.get("temperature");
        if (!(tempObj instanceof Number)) return false;
        double temp = ((Number) tempObj).doubleValue();
        if (temp < -30 || temp > 60) return false;

        Object humidityObj = data.get("humidity");
        if (!(humidityObj instanceof Number)) return false;
        double humidity = ((Number) humidityObj).doubleValue();
        if (humidity < 0 || humidity > 100) return false;

        Object lightObj = data.get("light");
        if (!(lightObj instanceof Integer)) return false;
        int light = (Integer) lightObj;
        return light >= 0 && light <= 15;
    }

    private boolean sendToK10(String jsonData) {
        try {
            tcpManager.sendMessageAsync(jsonData);
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[环境数据] 发送到K10失败: " + e.getMessage());
            return false;
        }
    }

    private void checkPendingResponses() {
        long currentTime = System.currentTimeMillis();
        long timeout = plugin.getConfig().getLong("environment.response-timeout", 5000);

        pendingRequests.entrySet().removeIf(entry -> {
            long elapsedTime = currentTime - entry.getValue();
            if (elapsedTime > timeout) {
                Bukkit.getLogger().warning("[环境数据] 请求超时: " + entry.getKey());
                return true;
            }
            return false;
        });
    }

    public void handleK10Response(String requestId, Map<String, Object> responseData) {
        if (pendingRequests.containsKey(requestId)) {
            pendingRequests.remove(requestId);
            lastResponseTime.set(System.currentTimeMillis());

            receiver.processResponse(responseData);

            if (plugin.getConfig().getBoolean("environment.debug-mode", false)) {
                Bukkit.getLogger().info("[环境数据] 收到K10响应: " + requestId);
            }
        }
    }

    public void reloadConfig() {
        loadConfig();
        Bukkit.getLogger().info("[环境数据] 配置已重新加载（新周期将在调度器重启后生效，当前: "
                + reportInterval + " ticks）");
    }

    private void logStatistics() {
        Bukkit.getLogger().info("[环境数据] ── 统计信息 ──");
        Bukkit.getLogger().info("[环境数据] 已发送聚合报告: " + totalReports + " 次");
        Bukkit.getLogger().info("[环境数据] 成功发送: " + successfulSends);
        Bukkit.getLogger().info("[环境数据] 失败发送: " + failedSends);
        Bukkit.getLogger().info("[环境数据] 报告周期: " + reportInterval + " ticks");
        Bukkit.getLogger().info("[环境数据] 待响应请求: " + pendingRequests.size());
    }

    public String getStatusReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n§6[环境数据] 系统状态报告§r\n");
        sb.append("§7─────────────────────────────§r\n");
        sb.append("§e运行状态: ").append(isRunning ? "§a● 运行中" : "§c● 已停止").append("\n");
        sb.append("§e报告模式: §f聚合(全玩家汇总)\n");
        sb.append("§e报告周期: §f").append(reportInterval).append(" ticks (").append(reportInterval / 20).append("秒)\n");
        sb.append("§e单报告玩家上限: §f").append(maxPlayersInReport).append("\n");
        sb.append("§e扫描半径: §f").append(calculator.getScanRadius()).append(" 格\n");
        sb.append("§7─────────────────────────────§r\n");
        sb.append("§e已发送报告: §f").append(totalReports).append("\n");
        sb.append("§e成功发送: §a").append(successfulSends).append("\n");
        sb.append("§e失败发送: §c").append(failedSends).append("\n");
        sb.append("§e上次报告玩家数: §f").append(lastReportedPlayerCount).append("\n");
        sb.append("§7─────────────────────────────§r\n");
        sb.append("§e待响应请求: §f").append(pendingRequests.size()).append("\n");
        sb.append("§e最后计算: §f").append(formatTimestamp(lastCalculationTime.get())).append("\n");
        sb.append("§e最后响应: §f").append(formatTimestamp(lastResponseTime.get())).append("\n");
        sb.append("§7─────────────────────────────§r");

        return sb.toString();
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp == 0) return "无";
        long elapsed = System.currentTimeMillis() - timestamp;
        if (elapsed < 1000) return elapsed + "ms 前";
        if (elapsed < 60000) return (elapsed / 1000) + "秒 前";
        return (elapsed / 60000) + "分钟 前";
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getReportInterval() {
        return reportInterval;
    }

    public long getTotalReports() {
        return totalReports;
    }

    public long getSuccessfulSends() {
        return successfulSends;
    }

    public long getFailedSends() {
        return failedSends;
    }
}
