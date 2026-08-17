package org.ljcode.myPlugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.ljcode.myPlugin.managers.K10TCPManager;

/**
 * 行空板K10数字孪生事件监听器 (优化版)
 * 
 * 改进点：
 * 1. 输入安全处理 - 清理特殊字符防止注入
 * 2. 智能事件过滤 - 更灵活的事件控制
 * 3. 消息格式增强 - 结构化JSON输出
 * 4. 增强日志记录 - 结构化日志输出
 * 5. 性能优化 - 减少不必要的字符串操作
 */
public class K10DigitalTwinListener implements Listener {
    
    private final K10TCPManager tcpManager;
    private FileConfiguration config;
    
    // 统计信息
    private long totalEventsProcessed = 0;
    private long eventsFiltered = 0;
    
    // 最大消息长度限制
    private static final int MAX_PLAYER_NAME_LENGTH = 50;
    private static final int MAX_CHAT_MESSAGE_LENGTH = 200;

    /**
     * 构造函数
     * @param tcpManager HTTP通信管理器
     */
    public K10DigitalTwinListener(K10TCPManager tcpManager) {
        this.tcpManager = tcpManager;
        this.config = tcpManager.getConfig();
        
        Bukkit.getLogger().info("[K10数字孪生] 事件监听器已初始化 (优化版)");
        Bukkit.getLogger().info("[K10数字孪生] 功能: 安全清理 | 智能过滤 | 结构化日志");
    }

    /**
     * 更新配置引用（配置重载后由插件主类调用，避免持有过期配置对象）
     * @param config 最新的配置文件对象
     */
    public void setConfig(FileConfiguration config) {
        this.config = config;
    }
    
    /**
     * 监听玩家加入游戏事件
     * 发送格式：{"event":"player_join","player":"玩家名","timestamp":时间戳}
     * @param event 玩家加入事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!config.getBoolean("k10.player-events-enabled", true)) {
            return;
        }
        
        String playerName = sanitizeString(event.getPlayer().getName(), MAX_PLAYER_NAME_LENGTH);
        long timestamp = System.currentTimeMillis();
        
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\"event\":\"player_join\",\"player\":\"")
                  .append(escapeJson(playerName))
                  .append("\",\"timestamp\":")
                  .append(timestamp)
                  .append("}");
        
        String jsonBody = jsonBuilder.toString();
        
        totalEventsProcessed++;
        logEvent("PLAYER_JOIN", playerName, jsonBody);
        tcpManager.sendMessageAsync(jsonBody);
    }
    
    /**
     * 监听玩家退出游戏事件
     * 发送格式：{"event":"player_quit","player":"玩家名","timestamp":时间戳}
     * @param event 玩家退出事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!config.getBoolean("k10.player-events-enabled", true)) {
            return;
        }
        
        String playerName = sanitizeString(event.getPlayer().getName(), MAX_PLAYER_NAME_LENGTH);
        long timestamp = System.currentTimeMillis();
        
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\"event\":\"player_quit\",\"player\":\"")
                  .append(escapeJson(playerName))
                  .append("\",\"timestamp\":")
                  .append(timestamp)
                  .append("}");
        
        String jsonBody = jsonBuilder.toString();
        
        totalEventsProcessed++;
        logEvent("PLAYER_QUIT", playerName, jsonBody);
        tcpManager.sendMessageAsync(jsonBody);
    }
    
    /**
     * 监听玩家死亡事件
     * 发送格式：{"event":"player_death","player":"玩家名","death_message":"死亡消息","timestamp":时间戳}
     * @param event 玩家死亡事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!config.getBoolean("k10.player-events-enabled", true)) {
            return;
        }
        
        String playerName = sanitizeString(event.getEntity().getName(), MAX_PLAYER_NAME_LENGTH);
        String deathMessage = sanitizeString(event.getDeathMessage(), MAX_CHAT_MESSAGE_LENGTH);
        long timestamp = System.currentTimeMillis();
        
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\"event\":\"player_death\",\"player\":\"")
                  .append(escapeJson(playerName))
                  .append("\",\"death_message\":\"")
                  .append(escapeJson(deathMessage))
                  .append("\",\"timestamp\":")
                  .append(timestamp)
                  .append("}");
        
        String jsonBody = jsonBuilder.toString();
        
        totalEventsProcessed++;
        logEvent("PLAYER_DEATH", playerName + " - " + deathMessage, jsonBody);
        tcpManager.sendMessageAsync(jsonBody);
    }
    
    /**
     * 监听玩家聊天事件
     * 发送格式：{"event":"custom_msg","player":"玩家名","message":"聊天内容","world":"世界名","timestamp":时间戳}
     * @param event 玩家聊天事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!config.getBoolean("k10.chat-events-enabled", true)) {
            eventsFiltered++;
            return;
        }
        
        String playerName = sanitizeString(event.getPlayer().getName(), MAX_PLAYER_NAME_LENGTH);
        String chatMessage = sanitizeString(event.getMessage(), MAX_CHAT_MESSAGE_LENGTH);
        String worldName = sanitizeString(event.getPlayer().getWorld().getName(), 50);
        long timestamp = System.currentTimeMillis();
        
        // 额外检查：如果消息为空或只有空格，则跳过
        if (chatMessage.trim().isEmpty()) {
            eventsFiltered++;
            return;
        }
        
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\"event\":\"custom_msg\",\"player\":\"")
                  .append(escapeJson(playerName))
                  .append("\",\"message\":\"[")
                  .append(escapeJson(playerName))
                  .append("] ")
                  .append(escapeJson(chatMessage))
                  .append("\",\"world\":\"")
                  .append(escapeJson(worldName))
                  .append("\",\"timestamp\":")
                  .append(timestamp)
                  .append("}");
        
        String jsonBody = jsonBuilder.toString();
        
        totalEventsProcessed++;
        logEvent("CHAT_MSG", playerName + ": " + chatMessage, jsonBody);
        tcpManager.sendMessageAsync(jsonBody);
    }
    
    // ==================== 安全处理工具方法 ====================
    
    /**
     * 清理字符串：移除潜在的危险字符并限制长度
     * @param input 原始字符串
     * @param maxLength 最大长度
     * @return 清理后的安全字符串
     */
    private String sanitizeString(String input, int maxLength) {
        if (input == null || input.isEmpty()) {
            return "unknown";
        }
        
        // 移除控制字符（除了换行和制表符）
        String sanitized = input.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        
        // 截断到最大长度
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength);
            Bukkit.getLogger().fine("[K10数字孪生] 字符串已截断: " + maxLength + " 字符");
        }
        
        return sanitized;
    }
    
    /**
     * 转义JSON字符串中的特殊字符
     * @param input 输入字符串
     * @return 转义后的字符串
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        // 其他控制字符转义为Unicode
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
            }
        }
        
        return escaped.toString();
    }
    
    /**
     * 结构化日志记录
     * @param eventType 事件类型
     * @param summary 事件摘要
     * @param jsonData 完整JSON数据
     */
    private void logEvent(String eventType, String summary, String jsonData) {
        Bukkit.getLogger().info(String.format(
            "[K10数字孪生] ✓ %s | %s | #%d",
            eventType,
            summary.length() > 50 ? summary.substring(0, 50) + "..." : summary,
            totalEventsProcessed
        ));
        
        // 在调试模式下显示完整JSON
        if (config.getBoolean("k10.debug-mode", false)) {
            Bukkit.getLogger().info("[K10数字孪生] [DEBUG] JSON: " + jsonData);
        }
    }
    
    // ==================== 状态查询接口 ====================
    
    /**
     * 获取监听器统计信息
     * @return 格式化的统计字符串
     */
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n§6[K10数字孪生] 事件监听器统计§r\n");
        sb.append("§7─────────────────────────────§r\n");
        sb.append("§e总处理事件数: §f").append(totalEventsProcessed).append("\n");
        sb.append("§e过滤事件数: §f").append(eventsFiltered).append("\n");
        sb.append("§e处理效率: §f").append(calculateEfficiency()).append("%\n");
        sb.append("§7─────────────────────────────§r");
        
        return sb.toString();
    }
    
    /**
     * 计算处理效率（非过滤事件的百分比）
     * @return 效率百分比
     */
    private double calculateEfficiency() {
        long total = totalEventsProcessed + eventsFiltered;
        if (total == 0) return 100.0;
        return (totalEventsProcessed * 100.0) / total;
    }
    
    /**
     * 重置统计数据（用于测试或管理命令）
     */
    public void resetStatistics() {
        totalEventsProcessed = 0;
        eventsFiltered = 0;
        Bukkit.getLogger().info("[K10数字孪生] 统计数据已重置");
    }
    
    /**
     * 获取总处理事件数
     * @return 总处理数
     */
    public long getTotalEventsProcessed() {
        return totalEventsProcessed;
    }
    
    /**
     * 获取过滤事件数
     * @return 过滤数
     */
    public long getEventsFiltered() {
        return eventsFiltered;
    }
}