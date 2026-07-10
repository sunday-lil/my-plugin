package org.ljcode.myPlugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 行空板K10 HTTP通信管理器 (优化版)
 * 
 * 改进点：
 * 1. 安全性增强 - 输入验证和JSON注入防护
 * 2. 连接状态监控 - 健康检查、状态追踪
 * 3. 消息节流/防抖 - 防止消息洪泛
 * 4. 性能统计 - 成功/失败率、响应时间
 * 5. 状态查询接口 - 获取系统运行状态
 * 6. 响应处理 - 支持接收和处理K10返回的数据
 */
public class K10TCPManager {

    /**
     * 响应回调接口
     */
    public interface ResponseCallback {
        void onResponse(Map<String, Object> responseData);
    }

    private ResponseCallback responseCallback;
    private FileConfiguration config;
    private String k10Host;
    private int k10Port;
    private int maxRetries;
    private int connectionTimeout;
    
    private final AtomicBoolean isProcessingQueue;
    private final ExecutorService executorService;
    private final ConcurrentLinkedQueue<String> messageQueue;
    
    // 统计信息
    private final AtomicLong totalMessagesSent;
    private final AtomicLong totalSuccessCount;
    private final AtomicLong totalFailureCount;
    private final AtomicLong lastResponseTime;
    private final AtomicLong lastSuccessTime;
    private volatile boolean isHealthy;
    private volatile String lastError;
    
    // 消息节流控制
    private static final long THROTTLE_INTERVAL_MS = 500; // 同类型消息最小间隔
    private String lastMessageType;
    private long lastMessageTime;
    
    // 最大队列长度
    private static final int MAX_QUEUE_SIZE = 1000;

    /**
     * 从配置文件加载K10相关设置
     */
    private void loadConfig() {
        this.k10Host = config.getString("k10.host");
        this.k10Port = config.getInt("k10.port");
        this.maxRetries = config.getInt("k10.max-retries");
        this.connectionTimeout = config.getInt("k10.connection-timeout");
        
        // 验证和清理主机地址
        this.k10Host = validateAndCleanHost(this.k10Host);
    }
    
    /**
     * 验证并清理主机地址
     * @param host 原始主机地址
     * @return 清理后的有效主机地址
     */
    private String validateAndCleanHost(String host) {
        if (host == null || host.trim().isEmpty()) {
            Bukkit.getLogger().severe("[K10数字孪生] ✗ 配置错误: K10主机地址为空!");
            return "127.0.0.1"; // 默认回环地址
        }
        
        // 移除首尾空格
        String cleanedHost = host.trim();
        
        // 移除可能的协议前缀 (http://, https://)
        if (cleanedHost.toLowerCase().startsWith("http://")) {
            cleanedHost = cleanedHost.substring(7);
            Bukkit.getLogger().warning("[K10数字孪生] ⚠ 主机地址包含 http:// 前缀，已自动移除");
        } else if (cleanedHost.toLowerCase().startsWith("https://")) {
            cleanedHost = cleanedHost.substring(8);
            Bukkit.getLogger().warning("[K10数字孪生] ⚠ 主机地址包含 https:// 前缀，已自动移除");
        }
        
        // 移除可能的路径部分 (/mc_event 等)
        int slashIndex = cleanedHost.indexOf('/');
        if (slashIndex > 0) {
            cleanedHost = cleanedHost.substring(0, slashIndex);
            Bukkit.getLogger().warning("[K10数字孪生] ⚠ 主机地址包含路径，已自动移除: " + cleanedHost);
        }
        
        // 移除端口号（如果用户误填了 host:port 格式）
        int colonIndex = cleanedHost.lastIndexOf(':');
        if (colonIndex > 0) {
            String possiblePort = cleanedHost.substring(colonIndex + 1);
            try {
                int port = Integer.parseInt(possiblePort);
                if (port > 0 && port <= 65535) {
                    // 用户在 host 中填写了端口号
                    Bukkit.getLogger().warning("[K10数字孪生] ⚠ 主机地址包含端口号，建议将端口配置到 k10.port 选项中");
                    cleanedHost = cleanedHost.substring(0, colonIndex);
                }
            } catch (NumberFormatException e) {
                // 不是端口号，可能是 IPv6 地址，保持原样
            }
        }
        
        // 最终验证：检查是否为有效的 IP 或域名
        if (!isValidHostname(cleanedHost)) {
            Bukkit.getLogger().severe("[K10数字孪生] ✗ 无效的主机地址格式: " + cleanedHost);
            return "127.0.0.1"; // 回退到默认值
        }
        
        return cleanedHost;
    }
    
    /**
     * 验证主机名是否有效
     * @param hostname 主机名
     * @return 是否有效
     */
    private boolean isValidHostname(String hostname) {
        if (hostname == null || hostname.isEmpty() || hostname.length() > 253) {
            return false;
        }
        
        // 简单的 IP 地址或域名验证
        String pattern = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)*[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?$|^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$";
        return hostname.matches(pattern);
    }

    /**
     * 构造函数
     * @param config 插件配置文件
     */
    public K10TCPManager(FileConfiguration config) {
        this.config = config;
        loadConfig();

        this.isProcessingQueue = new AtomicBoolean(false);
        this.executorService = Executors.newSingleThreadExecutor();
        this.messageQueue = new ConcurrentLinkedQueue<>();
        
        // 初始化统计信息
        this.totalMessagesSent = new AtomicLong(0);
        this.totalSuccessCount = new AtomicLong(0);
        this.totalFailureCount = new AtomicLong(0);
        this.lastResponseTime = new AtomicLong(0);
        this.lastSuccessTime = new AtomicLong(0);
        this.isHealthy = true;
        this.lastError = "";
        this.lastMessageType = "";
        this.lastMessageTime = 0;

        Bukkit.getLogger().info("[K10数字孪生] HTTP管理器已初始化 (优化版)");
        Bukkit.getLogger().info("[K10数字孪生] 目标: " + k10Host + ":" + k10Port);
        Bukkit.getLogger().info("[K10数字孪生] 功能: 安全防护 | 状态监控 | 消息节流 | 性能统计");
    }
    
    /**
     * 从配置文件重新加载K10相关设置
     */
    public void reloadConfig() {
        this.k10Host = config.getString("k10.host");
        this.k10Port = config.getInt("k10.port");
        this.maxRetries = config.getInt("k10.max-retries");
        this.connectionTimeout = config.getInt("k10.connection-timeout");
        
        // 重新验证和清理主机地址
        this.k10Host = validateAndCleanHost(this.k10Host);

        Bukkit.getLogger().info("[K10数字孪生] 配置已重新加载 - 新目标: " + k10Host + ":" + k10Port);
    }

    /**
     * 更新配置文件引用（用于reload时传入新配置）
     * @param config 新的配置文件对象
     */
    public void setConfig(FileConfiguration config) {
        this.config = config;
    }
    
    /**
     * 发送HTTP POST消息到K10（异步方式，带节流控制）
     * @param message 要发送的JSON消息
     */
    public void sendMessageAsync(String message) {
        if (message == null || message.isEmpty()) {
            Bukkit.getLogger().warning("[K10数字孪生] 尝试发送空消息，已忽略");
            return;
        }
        
        // 安全验证：检查消息是否包含危险字符
        if (!isMessageSafe(message)) {
            Bukkit.getLogger().warning("[K10数字孪生] 消息包含不安全内容，已拒绝: " + message);
            return;
        }
        
        // 消息节流：防止同类型消息洪泛
        String messageType = extractEventType(message);
        long currentTime = System.currentTimeMillis();
        
        if (messageType.equals(lastMessageType) && (currentTime - lastMessageTime) < THROTTLE_INTERVAL_MS) {
            Bukkit.getLogger().fine("[K10数字孪生] 消息被节流: " + messageType + " (间隔: " + (currentTime - lastMessageTime) + "ms)");
            return;
        }
        
        // 检查队列长度，防止内存溢出
        if (messageQueue.size() >= MAX_QUEUE_SIZE) {
            Bukkit.getLogger().warning("[K10数字孪生] 消息队列已满 (" + MAX_QUEUE_SIZE + ")，丢弃最旧消息");
            messageQueue.poll();
        }
        
        messageQueue.add(message);
        totalMessagesSent.incrementAndGet();
        lastMessageType = messageType;
        lastMessageTime = currentTime;
        
        Bukkit.getLogger().info("[K10数字孪生] 消息已加入队列 [" + messageQueue.size() + "/" + MAX_QUEUE_SIZE + "]: " + messageType);
        
        processMessageQueue();
    }
    
    /**
     * 处理消息队列（线程安全）
     */
    private void processMessageQueue() {
        if (isProcessingQueue.get()) {
            return;
        }
        
        if (messageQueue.isEmpty()) {
            return;
        }
        
        isProcessingQueue.set(true);
        
        executorService.submit(() -> {
            try {
                while (!messageQueue.isEmpty()) {
                    String message = messageQueue.poll();
                    if (message != null) {
                        boolean success = sendMessageInternal(message);
                        if (success) {
                            totalSuccessCount.incrementAndGet();
                            lastSuccessTime.set(System.currentTimeMillis());
                        } else {
                            totalFailureCount.incrementAndGet();
                        }
                    }
                }
                
                // 更新健康状态
                updateHealthStatus();
                
            } finally {
                isProcessingQueue.set(false);
            }
        });
    }
    
    /**
     * 内部发送消息方法（同步方式）
     * @param message 要发送的JSON消息
     * @return 发送是否成功
     */
    private boolean sendMessageInternal(String message) {
        int retryCount = 0;
        long startTime = System.currentTimeMillis();
        
        while (retryCount < maxRetries) {
            try {
                // 构建URL前进行最终验证
                if (k10Host == null || k10Host.trim().isEmpty() || k10Host.equals("127.0.0.1")) {
                    Bukkit.getLogger().severe("[K10数字孪生] ✗ 配置错误: K10主机地址无效 (" + 
                        (k10Host == null ? "null" : "\"" + k10Host + "\"") + 
                        ")，请检查 config.yml 中的 k10.host 设置");
                    lastError = "配置错误: 无效的主机地址";
                    isHealthy = false;
                    return false;
                }
                
                String urlString = "http://" + k10Host.trim() + ":" + k10Port + "/mc_event";
                
                // 调试日志：显示完整的URL（仅在首次重试时）
                if (retryCount == 0) {
                    Bukkit.getLogger().info("[K10数字孪生] 🌐 连接目标: " + urlString);
                }
                
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("X-K10-Source", "Minecraft-Plugin"); // 标识来源
                conn.setConnectTimeout(connectionTimeout);
                conn.setReadTimeout(connectionTimeout);
                conn.setDoOutput(true);
                
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = message.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                int responseCode = conn.getResponseCode();
                long responseTime = System.currentTimeMillis() - startTime;
                lastResponseTime.set(responseTime);
                
                if (responseCode == 200) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        
                        String responseStr = response.toString();
                        Bukkit.getLogger().info("[K10数字孪生] ✓ 发送成功 | 类型: " + extractEventType(message) + 
                            " | 耗时: " + responseTime + "ms" +
                            " | 响应: " + (responseStr.length() > 50 ? responseStr.substring(0, 50) + "..." : responseStr));
                        
                        if (responseCallback != null && !responseStr.isEmpty()) {
                            try {
                                Map<String, Object> responseData = parseJsonResponse(responseStr);
                                if (responseData != null && !responseData.isEmpty()) {
                                    responseCallback.onResponse(responseData);
                                    Bukkit.getLogger().info("[K10数字孪生] ✓ 响应已处理并回调");
                                }
                            } catch (Exception e) {
                                Bukkit.getLogger().warning("[K10数字孪生] ⚠ 响应解析失败: " + e.getMessage());
                            }
                        }
                    }
                    
                    lastError = "";
                    isHealthy = true;
                    return true;
                } else {
                    String errorMsg = "HTTP " + responseCode;
                    lastError = errorMsg;
                    Bukkit.getLogger().warning("[K10数字孪生] ✗ 响应异常: " + errorMsg);
                }
                
                conn.disconnect();
                
            } catch (IOException e) {
                retryCount++;
                lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
                isHealthy = false;
                
                Bukkit.getLogger().warning("[K10数字孪生] ✗ 发送失败 (" + retryCount + "/" + maxRetries + "): " + lastError);
                
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(1000 * retryCount); // 递增延迟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        Bukkit.getLogger().severe("[K10数字孪生] ✗ 发送最终失败 (已达最大重试): " + extractEventType(message));
        return false;
    }
    
    /**
     * 安全性验证：检查消息是否包含潜在的危险内容
     * @param message JSON消息
     * @return 是否安全
     */
    private boolean isMessageSafe(String message) {
        if (message == null) return false;
        
        // 检查长度限制
        if (message.length() > 10000) {
            Bukkit.getLogger().warning("[K10数字孪生] 消息过长: " + message.length() + " 字符");
            return false;
        }
        
        // 检查基本JSON格式（简单验证）
        if (!message.trim().startsWith("{") || !message.trim().endsWith("}")) {
            Bukkit.getLogger().warning("[K10数字孪生] 消息格式异常: 不是有效JSON对象");
            return false;
        }
        
        // 检查是否包含潜在的注入攻击字符（转义后的引号是允许的）
        String unescaped = message.replace("\\\"", "").replace("\\\\", "");
        if (unescaped.contains("<script") || unescaped.contains("javascript:")) {
            Bukkit.getLogger().warning("[K10数字孪生] 检测到潜在脚本注入");
            return false;
        }
        
        return true;
    }
    
    /**
     * 从JSON消息中提取事件类型
     * @param message JSON消息
     * @return 事件类型字符串
     */
    private String extractEventType(String message) {
        try {
            int eventStart = message.indexOf("\"event\":\"");
            if (eventStart == -1) return "unknown";
            
            eventStart += "\"event\":\"".length();
            int eventEnd = message.indexOf("\"", eventStart);
            if (eventEnd == -1) return "unknown";
            
            return message.substring(eventStart, eventEnd);
        } catch (Exception e) {
            return "parse_error";
        }
    }
    
    /**
     * 更新健康状态
     */
    private void updateHealthStatus() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastSuccess = currentTime - lastSuccessTime.get();
        
        // 如果超过30秒没有成功连接，标记为不健康
        if (timeSinceLastSuccess > 30000 && totalSuccessCount.get() > 0) {
            isHealthy = false;
        }
    }
    
    /**
     * 关闭管理器，释放资源
     */
    public void shutdown() {
        Bukkit.getLogger().info("[K10数字孪生] 正在关闭HTTP管理器...");
        Bukkit.getLogger().info("[K10数字孪生] 统计信息:");
        logStatistics();
        
        isProcessingQueue.set(false);
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        Bukkit.getLogger().info("[K10数字孪生] HTTP管理器已关闭");
    }
    
    // ==================== 状态查询接口 ====================
    
    /**
     * 获取完整的系统状态报告
     * @return 格式化的状态字符串
     */
    public String getStatusReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n§6[K10数字孪生] 系统状态报告§r\n");
        sb.append("§7─────────────────────────────§r\n");
        sb.append("§e目标地址: §f").append(k10Host).append(":").append(k10Port).append("\n");
        sb.append("§e连接状态: ").append(isHealthy ? "§a● 健康" : "§c● 异常").append("\n");
        sb.append("§e最后错误: §f").append(lastError.isEmpty() ? "无" : lastError).append("\n");
        sb.append("§7─────────────────────────────§r\n");
        sb.append("§e总发送数: §f").append(totalMessagesSent.get()).append("\n");
        sb.append("§e成功次数: §a").append(totalSuccessCount.get()).append("\n");
        sb.append("§e失败次数: §c").append(totalFailureCount.get()).append("\n");
        sb.append("§e成功率: §f").append(calculateSuccessRate()).append("%\n");
        sb.append("§e队列深度: §f").append(messageQueue.size()).append("/").append(MAX_QUEUE_SIZE).append("\n");
        sb.append("§7─────────────────────────────§r\n");
        sb.append("§e最后响应时间: §f").append(lastResponseTime.get()).append("ms\n");
        sb.append("§e最后成功时间: §f").append(formatTimestamp(lastSuccessTime.get())).append("\n");
        sb.append("§7─────────────────────────────§r");
        
        return sb.toString();
    }
    
    /**
     * 获取简短的状态摘要
     * @return 简短状态字符串
     */
    public String getStatusSummary() {
        return String.format("%s | %s:%d | 队列:%d | 成功率:%.1f%%",
            isHealthy ? "✓" : "✗",
            k10Host, k10Port,
            messageQueue.size(),
            calculateSuccessRate()
        );
    }
    
    /**
     * 记录详细统计信息到日志
     */
    public void logStatistics() {
        Bukkit.getLogger().info("[K10数字孪生] ── 性能统计 ──");
        Bukkit.getLogger().info("[K10数字孪生] 总消息数: " + totalMessagesSent.get());
        Bukkit.getLogger().info("[K10数字孪生] 成功: " + totalSuccessCount.get() + " | 失败: " + totalFailureCount.get());
        Bukkit.getLogger().info("[K10数字孪生] 成功率: " + calculateSuccessRate() + "%");
        Bukkit.getLogger().info("[K10数字孪生] 平均响应时间: " + lastResponseTime.get() + "ms");
        Bukkit.getLogger().info("[K10数字孪生] 当前队列: " + messageQueue.size() + "/" + MAX_QUEUE_SIZE);
        Bukkit.getLogger().info("[K10数字孪生] 健康状态: " + (isHealthy ? "正常" : "异常"));
        if (!lastError.isEmpty()) {
            Bukkit.getLogger().info("[K10数字孪生] 最后错误: " + lastError);
        }
        Bukkit.getLogger().info("[K10数字孪生] ────────────");
    }
    
    /**
     * 计算成功率
     * @return 成功率百分比
     */
    private double calculateSuccessRate() {
        long total = totalSuccessCount.get() + totalFailureCount.get();
        if (total == 0) return 100.0;
        return (totalSuccessCount.get() * 100.0) / total;
    }
    
    /**
     * 格式化时间戳为可读字符串
     * @param timestamp 时间戳
     * @return 格式化时间字符串
     */
    private String formatTimestamp(long timestamp) {
        if (timestamp == 0) return "从未";
        
        long diff = System.currentTimeMillis() - timestamp;
        if (diff < 1000) return "刚刚";
        if (diff < 60000) return (diff / 1000) + "秒前";
        if (diff < 3600000) return (diff / 60000) + "分钟前";
        return (diff / 3600000) + "小时前";
    }
    
    // ==================== Getter 方法 ====================
    
    /**
     * 获取当前队列中的消息数量
     * @return 队列消息数量
     */
    public int getQueueSize() {
        return messageQueue.size();
    }
    
    /**
     * 获取配置文件
     * @return 配置文件对象
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * 设置响应回调函数
     * @param callback 响应回调接口
     */
    public void setResponseCallback(ResponseCallback callback) {
        this.responseCallback = callback;
        Bukkit.getLogger().info("[K10数字孪生] 响应回调已设置");
    }

    /**
     * 解析JSON响应字符串
     * @param jsonResponse JSON响应字符串
     * @return 解析后的Map数据
     */
    private Map<String, Object> parseJsonResponse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        
        try {
            jsonResponse = jsonResponse.trim();
            if (!jsonResponse.startsWith("{") || !jsonResponse.endsWith("}")) {
                Bukkit.getLogger().warning("[K10数字孪生] 响应不是有效的JSON对象: " + jsonResponse);
                return null;
            }

            jsonResponse = jsonResponse.substring(1, jsonResponse.length() - 1).trim();
            
            if (jsonResponse.isEmpty()) {
                return result;
            }

            String[] pairs = jsonResponse.split(",");
            for (String pair : pairs) {
                pair = pair.trim();
                if (pair.isEmpty()) continue;

                int colonIndex = pair.indexOf(':');
                if (colonIndex <= 0) continue;

                String key = pair.substring(0, colonIndex).trim();
                String value = pair.substring(colonIndex + 1).trim();

                if (key.startsWith("\"") && key.endsWith("\"")) {
                    key = key.substring(1, key.length() - 1);
                }

                Object parsedValue = parseJsonValue(value);
                if (parsedValue != null) {
                    result.put(key, parsedValue);
                }
            }

        } catch (Exception e) {
            Bukkit.getLogger().warning("[K10数字孪生] JSON解析异常: " + e.getMessage());
            return null;
        }

        return result;
    }

    /**
     * 解析JSON值
     * @param valueStr 值字符串
     * @return 解析后的对象
     */
    private Object parseJsonValue(String valueStr) {
        if (valueStr == null || valueStr.trim().isEmpty()) {
            return null;
        }

        valueStr = valueStr.trim();

        if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
            return valueStr.substring(1, valueStr.length() - 1);
        }

        if ("true".equalsIgnoreCase(valueStr)) {
            return true;
        }
        if ("false".equalsIgnoreCase(valueStr)) {
            return false;
        }
        if ("null".equalsIgnoreCase(valueStr)) {
            return null;
        }

        try {
            if (valueStr.contains(".")) {
                return Double.parseDouble(valueStr);
            } else {
                return Long.parseLong(valueStr);
            }
        } catch (NumberFormatException e) {
            return valueStr;
        }
    }

    /**
     * 获取当前K10主机地址
     * @return K10主机地址
     */
    public String getCurrentHost() {
        return k10Host;
    }

    /**
     * 获取当前K10端口号
     * @return K10端口号
     */
    public int getCurrentPort() {
        return k10Port;
    }
    
    /**
     * 获取是否处于健康状态
     * @return 健康状态
     */
    public boolean isHealthy() {
        return isHealthy;
    }
    
    /**
     * 获取总发送消息数量
     * @return 总发送数
     */
    public long getTotalMessagesSent() {
        return totalMessagesSent.get();
    }
    
    /**
     * 获取成功次数
     * @return 成功次数
     */
    public long getSuccessCount() {
        return totalSuccessCount.get();
    }
    
    /**
     * 获取失败次数
     * @return 失败次数
     */
    public long getFailureCount() {
        return totalFailureCount.get();
    }
}