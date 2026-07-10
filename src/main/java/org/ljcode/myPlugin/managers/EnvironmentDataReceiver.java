package org.ljcode.myPlugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 环境数据接收器
 * 负责接收和处理从K10设备返回的数据
 */
public class EnvironmentDataReceiver {

    private final FileConfiguration config;
    private final ConcurrentHashMap<String, Object> receivedData;
    private final ConcurrentHashMap<String, Long> dataTimestamps;

    private long totalReceived;
    private long processedCount;
    private long errorCount;
    private String lastReceivedType;
    private long lastReceivedTime;

    public EnvironmentDataReceiver(FileConfiguration config) {
        this.config = config;
        this.receivedData = new ConcurrentHashMap<>();
        this.dataTimestamps = new ConcurrentHashMap<>();
        this.totalReceived = 0;
        this.processedCount = 0;
        this.errorCount = 0;
        this.lastReceivedType = "";
        this.lastReceivedTime = 0;

        Bukkit.getLogger().info("[环境数据接收器] 已初始化");
    }

    /**
     * 处理从K10设备接收到的响应数据
     * @param responseData 响应数据Map
     */
    public void processResponse(Map<String, Object> responseData) {
        if (responseData == null || responseData.isEmpty()) {
            Bukkit.getLogger().warning("[环境数据接收器] 收到空响应数据");
            return;
        }

        totalReceived++;
        lastReceivedTime = System.currentTimeMillis();

        try {
            String responseType = (String) responseData.getOrDefault("response_type", "unknown");
            lastReceivedType = responseType;

            if (config.getBoolean("environment.debug-mode", false)) {
                Bukkit.getLogger().info("[环境数据接收器] 处理响应类型: " + responseType);
            }

            switch (responseType) {
                case "acknowledgment":
                    handleAcknowledgment(responseData);
                    break;
                case "data_update":
                    handleDataUpdate(responseData);
                    break;
                case "error":
                    handleError(responseData);
                    break;
                case "status":
                    handleStatus(responseData);
                    break;
                case "command":
                    handleCommand(responseData);
                    break;
                default:
                    Bukkit.getLogger().warning("[环境数据接收器] 未知响应类型: " + responseType);
                    handleUnknownResponse(responseData);
                    break;
            }

            processedCount++;

            if (responseData.containsKey("request_id")) {
                String requestId = (String) responseData.get("request_id");
                dataTimestamps.put(requestId, System.currentTimeMillis());
            }

        } catch (Exception e) {
            errorCount++;
            Bukkit.getLogger().severe("[环境数据接收器] 处理响应数据异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理确认响应
     * @param data 响应数据
     */
    private void handleAcknowledgment(Map<String, Object> data) {
        String requestId = (String) data.get("request_id");
        String status = (String) data.getOrDefault("status", "unknown");
        String message = (String) data.getOrDefault("message", "");

        if ("success".equals(status)) {
            if (config.getBoolean("environment.debug-mode", false)) {
                Bukkit.getLogger().info("[环境数据接收器] 请求成功确认: " + requestId);
            }
        } else {
            Bukkit.getLogger().warning("[环境数据接收器] 请求失败: " + requestId + " - " + message);
        }
    }

    /**
     * 处理数据更新响应
     * @param data 响应数据
     */
    private void handleDataUpdate(Map<String, Object> data) {
        String updateType = (String) data.getOrDefault("update_type", "general");
        Map<String, Object> updateData = (Map<String, Object>) data.get("data");

        if (updateData != null) {
            synchronized (receivedData) {
                receivedData.putAll(updateData);
            }

            if (config.getBoolean("environment.debug-mode", false)) {
                Bukkit.getLogger().info("[环境数据接收器] 收到数据更新: " + updateType + 
                        ", 数据项数: " + updateData.size());
            }
        }
    }

    /**
     * 处理错误响应
     * @param data 响应数据
     */
    private void handleError(Map<String, Object> data) {
        String errorCode = (String) data.getOrDefault("error_code", "unknown");
        String errorMessage = (String) data.getOrDefault("error_message", "未知错误");
        String requestId = (String) data.get("request_id");

        Bukkit.getLogger().severe("[环境数据接收器] K10设备错误 [" + errorCode + "]: " + errorMessage + 
                (requestId != null ? " (请求ID: " + requestId + ")" : ""));

        if (data.containsKey("suggested_action")) {
            String suggestedAction = (String) data.get("suggested_action");
            Bukkit.getLogger().info("[环境数据接收器] 建议操作: " + suggestedAction);
        }
    }

    /**
     * 处理状态响应
     * @param data 响应数据
     */
    private void handleStatus(Map<String, Object> data) {
        String status = (String) data.getOrDefault("status", "unknown");
        Map<String, Object> statusData = (Map<String, Object>) data.get("status_data");

        if (config.getBoolean("environment.debug-mode", false)) {
            Bukkit.getLogger().info("[环境数据接收器] K10设备状态: " + status);
            if (statusData != null) {
                Bukkit.getLogger().info("[环境数据接收器] 状态数据: " + statusData.toString());
            }
        }
    }

    /**
     * 处理命令响应
     * @param data 响应数据
     */
    private void handleCommand(Map<String, Object> data) {
        String command = (String) data.getOrDefault("command", "");
        Map<String, Object> params = (Map<String, Object>) data.get("params");

        Bukkit.getLogger().info("[环境数据接收器] 收到K10命令: " + command);

        switch (command) {
            case "adjust_interval":
                handleAdjustIntervalCommand(params);
                break;
            case "request_resend":
                handleRequestResendCommand(params);
                break;
            case "update_config":
                handleUpdateConfigCommand(params);
                break;
            default:
                Bukkit.getLogger().warning("[环境数据接收器] 未知命令: " + command);
                break;
        }
    }

    /**
     * 处理调整间隔命令
     * @param params 命令参数
     */
    private void handleAdjustIntervalCommand(Map<String, Object> params) {
        if (params != null && params.containsKey("new_interval")) {
            int newInterval = ((Number) params.get("new_interval")).intValue();
            Bukkit.getLogger().info("[环境数据接收器] K10请求调整间隔: " + newInterval + " ticks");
        }
    }

    /**
     * 处理重新发送请求命令
     * @param params 命令参数
     */
    private void handleRequestResendCommand(Map<String, Object> params) {
        String requestId = params != null ? (String) params.get("request_id") : null;
        Bukkit.getLogger().info("[环境数据接收器] K10请求重新发送数据: " + 
                (requestId != null ? requestId : "最新数据"));
    }

    /**
     * 处理更新配置命令
     * @param params 命令参数
     */
    private void handleUpdateConfigCommand(Map<String, Object> params) {
        if (params != null) {
            Bukkit.getLogger().info("[环境数据接收器] K10请求更新配置: " + params.toString());
        }
    }

    /**
     * 处理未知响应
     * @param data 响应数据
     */
    private void handleUnknownResponse(Map<String, Object> data) {
        Bukkit.getLogger().warning("[环境数据接收器] 收到未知格式响应，原始数据: " + data.toString());
        
        synchronized (receivedData) {
            receivedData.put("last_unknown_response", data);
            receivedData.put("last_unknown_time", System.currentTimeMillis());
        }
    }

    /**
     * 获取接收到的数据
     * @return 数据Map的副本
     */
    public Map<String, Object> getReceivedData() {
        synchronized (receivedData) {
            return new java.util.HashMap<>(receivedData);
        }
    }

    /**
     * 获取指定键的数据
     * @param key 数据键
     * @return 数据值
     */
    public Object getData(String key) {
        return receivedData.get(key);
    }

    /**
     * 清除所有接收到的数据
     */
    public void clearData() {
        synchronized (receivedData) {
            receivedData.clear();
        }
        synchronized (dataTimestamps) {
            dataTimestamps.clear();
        }
        Bukkit.getLogger().info("[环境数据接收器] 已清除所有缓存数据");
    }

    /**
     * 获取统计信息
     * @return 格式化的统计字符串
     */
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n§6[环境数据接收器] 统计信息§r\n");
        sb.append("§7─────────────────────────────§r\n");
        sb.append("§e总接收数: §f").append(totalReceived).append("\n");
        sb.append("§e处理成功: §a").append(processedCount).append("\n");
        sb.append("§e处理失败: §c").append(errorCount).append("\n");
        sb.append("§e成功率: §f").append(
                totalReceived > 0 ? String.format("%.1f", processedCount * 100.0 / totalReceived) : "0.0")
                .append("%\n");
        sb.append("§7─────────────────────────────§r\n");
        sb.append("§e最后接收类型: §f").append(lastReceivedType.isEmpty() ? "无" : lastReceivedType).append("\n");
        sb.append("§e最后接收时间: §f").append(formatTimestamp(lastReceivedTime)).append("\n");
        sb.append("§e缓存数据项: §f").append(receivedData.size()).append("\n");
        sb.append("§7─────────────────────────────§r");
        
        return sb.toString();
    }

    /**
     * 格式化时间戳
     * @param timestamp 时间戳
     * @return 格式化字符串
     */
    private String formatTimestamp(long timestamp) {
        if (timestamp == 0) return "无";
        long elapsed = System.currentTimeMillis() - timestamp;
        if (elapsed < 1000) return elapsed + "ms 前";
        if (elapsed < 60000) return (elapsed / 1000) + "秒 前";
        return (elapsed / 60000) + "分钟 前";
    }

    /**
     * 重置统计数据
     */
    public void resetStatistics() {
        totalReceived = 0;
        processedCount = 0;
        errorCount = 0;
        lastReceivedType = "";
        lastReceivedTime = 0;
        Bukkit.getLogger().info("[环境数据接收器] 统计数据已重置");
    }

    public long getTotalReceived() {
        return totalReceived;
    }

    public long getProcessedCount() {
        return processedCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public String getLastReceivedType() {
        return lastReceivedType;
    }

    public long getLastReceivedTime() {
        return lastReceivedTime;
    }
}