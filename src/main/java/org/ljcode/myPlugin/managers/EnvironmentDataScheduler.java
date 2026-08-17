package org.ljcode.myPlugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.ljcode.myPlugin.MyPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 环境数据调度器
 * 负责轮流计算环境数据，并根据计算速度动态调整
 */
public class EnvironmentDataScheduler {

    private final MyPlugin plugin;
    private final EnvironmentDataCalculator calculator;
    private final K10TCPManager tcpManager;
    private final EnvironmentDataReceiver receiver;

    private BukkitRunnable calculationTask;
    private BukkitRunnable responseCheckTask;

    private int baseInterval;
    private int currentInterval;
    private int minInterval;
    private int maxInterval;
    private int maxRetries;

    private final AtomicLong lastCalculationTime;
    private final AtomicLong lastResponseTime;
    private final ConcurrentHashMap<String, Long> pendingRequests;
    private final Map<String, Object> latestEnvironmentData;

    private volatile boolean isRunning;
    private int currentPlayerIndex;
    private long totalCalculations;
    private long successfulSends;
    private long failedSends;

    public EnvironmentDataScheduler(MyPlugin plugin, EnvironmentDataCalculator calculator,
                                    K10TCPManager tcpManager, EnvironmentDataReceiver receiver) {
        this.plugin = plugin;
        this.calculator = calculator;
        this.tcpManager = tcpManager;
        this.receiver = receiver;
        this.lastCalculationTime = new AtomicLong(0);
        this.lastResponseTime = new AtomicLong(0);
        this.pendingRequests = new ConcurrentHashMap<>();
        this.latestEnvironmentData = new HashMap<>();
        this.isRunning = false;
        this.currentPlayerIndex = 0;
        this.totalCalculations = 0;
        this.successfulSends = 0;
        this.failedSends = 0;

        loadConfig();
    }

    private void loadConfig() {
        this.baseInterval = plugin.getConfig().getInt("environment.base-interval", 100);
        this.currentInterval = baseInterval;
        this.minInterval = plugin.getConfig().getInt("environment.min-interval", 50);
        this.maxInterval = plugin.getConfig().getInt("environment.max-interval", 500);
        this.maxRetries = plugin.getConfig().getInt("environment.max-retries", 3);
    }

    public void start() {
        if (isRunning) {
            Bukkit.getLogger().warning("[环境数据] 调度器已在运行中");
            return;
        }

        isRunning = true;
        Bukkit.getLogger().info("[环境数据] 启动环境数据调度器...");

        calculationTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    calculateAndSend();
                } catch (Exception e) {
                    Bukkit.getLogger().severe("[环境数据] 计算任务异常: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };

        // 必须使用同步任务：calculateAndSend 会调用 world.getBlockAt / getOnlinePlayers
        // 等非线程安全的Bukkit API；HTTP发送由K10管理器的独立线程池处理，不会阻塞主线程
        calculationTask.runTaskTimer(plugin, 20L, currentInterval);

        responseCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkPendingResponses();
            }
        };

        responseCheckTask.runTaskTimerAsynchronously(plugin, 100L, 100L);

        Bukkit.getLogger().info("[环境数据] 调度器已启动，初始间隔: " + currentInterval + " ticks");
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

    private void calculateAndSend() {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        boolean calculationValid = false;
        Map<String, Object> envData = null;

        try {
            Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
            if (currentPlayerIndex >= players.length) {
                currentPlayerIndex = 0;
            }

            Player targetPlayer = players[currentPlayerIndex];
            currentPlayerIndex = (currentPlayerIndex + 1) % players.length;

            // ★ 兜底机制1：计算环境数据
            envData = calculator.calculateEnvironmentData(targetPlayer);

            // ★ 兜底机制2：验证数据完整性
            calculationValid = validateEnvironmentData(envData);

            if (!calculationValid) {
                Bukkit.getLogger().warning("[环境数据] 数据验证失败，跳过本次发送 - 玩家: " + targetPlayer.getName());
                failedSends++;
                return; // 不发送无效数据
            }

            envData.put("player_name", targetPlayer.getName());
            envData.put("timestamp", System.currentTimeMillis());
            envData.put("calculation_time_ms", System.currentTimeMillis() - startTime); // 记录计算耗时
            envData.put("request_id", generateRequestId(targetPlayer));

            String jsonData = buildJsonMessage(envData);

            synchronized (latestEnvironmentData) {
                latestEnvironmentData.clear();
                latestEnvironmentData.putAll(envData);
            }

            String requestId = (String) envData.get("request_id");
            pendingRequests.put(requestId, System.currentTimeMillis());

            // ★ 兜底机制3：发送前再次检查连接状态
            boolean sendSuccess = sendToK10(jsonData);

            long calculationTime = System.currentTimeMillis() - startTime;
            lastCalculationTime.set(System.currentTimeMillis());
            totalCalculations++;

            if (sendSuccess) {
                successfulSends++;
                adjustInterval(calculationTime);
            } else {
                failedSends++;
                handleSendFailure();
            }

            if (plugin.getConfig().getBoolean("environment.debug-mode", false)) {
                Bukkit.getLogger().info("[环境数据] ✓ 计算完成 | 耗时: " + calculationTime + "ms | " +
                        "玩家: " + targetPlayer.getName() + " | " +
                        "温度: " + envData.get("temperature") + "°C | " +
                        "当前间隔: " + currentInterval + " ticks");
            }

        } catch (Exception e) {
            Bukkit.getLogger().severe("[环境数据] 计算过程异常: " + e.getMessage());
            e.printStackTrace();
            failedSends++;
            handleSendFailure();
        }
    }

    /**
     * ★ 新增：验证环境数据的完整性和合理性
     * @param data 环境数据
     * @return 是否有效
     */
    private boolean validateEnvironmentData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return false;
        }

        // 检查必需字段是否存在
        if (!data.containsKey("temperature") || !data.containsKey("humidity") ||
            !data.containsKey("light") || !data.containsKey("wind_speed")) {
            Bukkit.getLogger().warning("[环境数据] 缺少必要字段");
            return false;
        }

        // 检查温度值是否在合理范围（-30到60度）
        Object tempObj = data.get("temperature");
        if (!(tempObj instanceof Number)) {
            return false;
        }
        double temp = ((Number) tempObj).doubleValue();
        if (temp < -30 || temp > 60) {
            Bukkit.getLogger().warning("[环境数据] 温度值异常: " + temp + "°C");
            return false;
        }

        // 检查湿度值是否在合理范围（0-100%）
        Object humidityObj = data.get("humidity");
        if (!(humidityObj instanceof Number)) {
            return false;
        }
        double humidity = ((Number) humidityObj).doubleValue();
        if (humidity < 0 || humidity > 100) {
            Bukkit.getLogger().warning("[环境数据] 湿度值异常: " + humidity + "%");
            return false;
        }

        // 检查光照等级是否合理（0-15）
        Object lightObj = data.get("light");
        if (!(lightObj instanceof Integer)) {
            return false;
        }
        int light = (Integer) lightObj;
        if (light < 0 || light > 15) {
            Bukkit.getLogger().warning("[环境数据] 光照等级异常: " + light);
            return false;
        }

        return true;
    }

    private String generateRequestId(Player player) {
        return player.getName() + "_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }

    private String buildJsonMessage(Map<String, Object> data) {
        StringBuilder json = new StringBuilder();
        json.append("{\"event\":\"environment_data\"");

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            json.append(",\"").append(entry.getKey()).append("\":");

            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number) {
                json.append(value);
            } else if (value instanceof Map) {
                json.append(buildResourceJson((Map<String, Double>) value));
            } else if (value instanceof Boolean) {
                json.append(value);
            } else {
                json.append("\"").append(escapeJson(value.toString())).append("\"");
            }
        }

        json.append("}");
        return json.toString();
    }

    private String buildResourceJson(Map<String, Double> resources) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Double> entry : resources.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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

    private void adjustInterval(long calculationTime) {
        if (calculationTime < 50) {
            currentInterval = Math.max(minInterval, currentInterval - 10);
        } else if (calculationTime > 200) {
            currentInterval = Math.min(maxInterval, currentInterval + 20);
        } else if (calculationTime > 100) {
            currentInterval = Math.min(maxInterval, currentInterval + 5);
        }

        if (calculationTask != null && isRunning) {
            calculationTask.cancel();
            calculationTask = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        calculateAndSend();
                    } catch (Exception e) {
                        Bukkit.getLogger().severe("[环境数据] 计算任务异常: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            };
            // 保持同步任务：calculateAndSend 调用非线程安全的Bukkit API
            calculationTask.runTaskTimer(plugin, currentInterval, currentInterval);
        }
    }

    private void handleSendFailure() {
        currentInterval = Math.min(maxInterval, currentInterval + 50);

        if (calculationTask != null && isRunning) {
            calculationTask.cancel();
            calculationTask = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        calculateAndSend();
                    } catch (Exception e) {
                        Bukkit.getLogger().severe("[环境数据] 计算任务异常: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            };
            // 保持同步任务：calculateAndSend 调用非线程安全的Bukkit API
            calculationTask.runTaskTimer(plugin, currentInterval, currentInterval);
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

    public Map<String, Object> getLatestEnvironmentData() {
        synchronized (latestEnvironmentData) {
            return new HashMap<>(latestEnvironmentData);
        }
    }

    public void reloadConfig() {
        loadConfig();
        Bukkit.getLogger().info("[环境数据] 配置已重新加载");
    }

    private void logStatistics() {
        Bukkit.getLogger().info("[环境数据] ── 统计信息 ──");
        Bukkit.getLogger().info("[环境数据] 总计算次数: " + totalCalculations);
        Bukkit.getLogger().info("[环境数据] 成功发送: " + successfulSends);
        Bukkit.getLogger().info("[环境数据] 失败发送: " + failedSends);
        Bukkit.getLogger().info("[环境数据] 成功率: " +
                (totalCalculations > 0 ? (successfulSends * 100.0 / totalCalculations) : 0) + "%");
        Bukkit.getLogger().info("[环境数据] 当前间隔: " + currentInterval + " ticks");
        Bukkit.getLogger().info("[环境数据] 待响应请求: " + pendingRequests.size());
    }

    public String getStatusReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n§6[环境数据] 系统状态报告§r\n");
        sb.append("§7─────────────────────────────§r\n");
        sb.append("§e运行状态: ").append(isRunning ? "§a● 运行中" : "§c● 已停止").append("\n");
        sb.append("§e当前间隔: §f").append(currentInterval).append(" ticks\n");
        sb.append("§e扫描半径: §f").append(calculator.getScanRadius()).append(" 格\n");
        sb.append("§7─────────────────────────────§r\n");
        sb.append("§e总计算次数: §f").append(totalCalculations).append("\n");
        sb.append("§e成功发送: §a").append(successfulSends).append("\n");
        sb.append("§e失败发送: §c").append(failedSends).append("\n");
        sb.append("§e成功率: §f").append(
                totalCalculations > 0 ? String.format("%.1f", successfulSends * 100.0 / totalCalculations) : "0.0")
                .append("%\n");
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

    public int getCurrentInterval() {
        return currentInterval;
    }

    public long getTotalCalculations() {
        return totalCalculations;
    }

    public long getSuccessfulSends() {
        return successfulSends;
    }

    public long getFailedSends() {
        return failedSends;
    }
}
