package org.ljcode.myPlugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.ljcode.myPlugin.MyPlugin;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 聊天提示监听器
 * 监听玩家聊天事件，提供智能提示、命令建议、表情转换等功能
 * 增强聊天体验，提高玩家互动性
 */
public class ChatHintListener implements Listener {
    
    // 插件主类实例
    private final MyPlugin plugin;
    
    // 表情符号映射表
    private final Map<String, String> emojiMap;
    
    // 常用命令提示映射
    private final Map<String, String> commandHints;
    
    // 玩家聊天历史（用于智能提示）
    private final Map<String, List<String>> playerChatHistory;
    
    /**
     * 构造函数，初始化聊天提示监听器
     * 
     * @param plugin 插件主类实例
     */
    public ChatHintListener(MyPlugin plugin) {
        this.plugin = plugin;
        this.emojiMap = initializeEmojiMap();
        this.commandHints = initializeCommandHints();
        this.playerChatHistory = new HashMap<>();
    }
    
    /**
     * 处理玩家聊天事件
     * 提供聊天提示、表情转换、命令建议等功能
     * 
     * @param event 异步玩家聊天事件
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // 检查是否启用聊天提示功能
        boolean chatHintsEnabled = plugin.getConfig().getBoolean("chat.hints-enabled", true);
        if (!chatHintsEnabled) {
            return;
        }
        
        // 更新玩家聊天历史
        updateChatHistory(player.getName(), message);
        
        // 异步处理聊天提示（避免阻塞主线程）
        new BukkitRunnable() {
            @Override
            public void run() {
                // 表情符号自动转换
                if (plugin.getConfig().getBoolean("chat.emoji-conversion", true)) {
                    String convertedMessage = convertEmojis(message);
                    if (!convertedMessage.equals(message)) {
                        // 如果消息被转换，向玩家发送提示
                        player.sendMessage(ChatColor.GRAY + "💡 提示：您的消息已自动转换为表情符号");
                    }
                }
                
                // 命令建议提示
                if (plugin.getConfig().getBoolean("chat.command-suggestions", true)) {
                    suggestCommands(player, message);
                }
                
                // 智能回复提示
                if (plugin.getConfig().getBoolean("chat.reply-suggestions", true)) {
                    suggestReplies(player, message);
                }
                
                // 聊天统计提示
                if (plugin.getConfig().getBoolean("chat.stats-enabled", true)) {
                    showChatStats(player);
                }
            }
        }.runTaskLater(plugin, 2L); // 延迟2tick执行，确保聊天消息已经发送
    }
    
    /**
     * 将文本表情转换为表情符号
     * 例如：:smile: -> 😊
     * 
     * @param message 原始消息
     * @return 转换后的消息
     */
    private String convertEmojis(String message) {
        String converted = message;
        
        for (Map.Entry<String, String> entry : emojiMap.entrySet()) {
            if (converted.contains(entry.getKey())) {
                converted = converted.replace(entry.getKey(), entry.getValue());
            }
        }
        
        return converted;
    }
    
    /**
     * 提供命令建议
     * 当玩家聊天内容包含特定关键词时，提示相关命令
     * 
     * @param player 玩家对象
     * @param message 聊天消息
     */
    private void suggestCommands(Player player, String message) {
        List<String> suggestions = new ArrayList<>();
        
        // 检查消息中是否包含关键词
        String lowerMessage = message.toLowerCase();
        
        for (Map.Entry<String, String> entry : commandHints.entrySet()) {
            if (lowerMessage.contains(entry.getKey())) {
                suggestions.add(entry.getValue());
            }
        }
        
        // 如果有建议，发送给玩家
        if (!suggestions.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "💡 您可能需要这些命令：");
            for (String suggestion : suggestions) {
                player.sendMessage(ChatColor.GRAY + "  • " + suggestion);
            }
        }
    }
    
    /**
     * 提供智能回复建议
     * 基于聊天历史提供回复建议
     * 
     * @param player 玩家对象
     * @param message 当前消息
     */
    private void suggestReplies(Player player, String message) {
        // 检查是否是回复性消息（包含@或回复关键词）
        if (message.contains("@") || containsReplyKeywords(message)) {
            // 获取最近的其他玩家消息
            List<String> recentMessages = getRecentOtherPlayerMessages(player.getName());
            
            if (!recentMessages.isEmpty()) {
                player.sendMessage(ChatColor.AQUA + "💬 最近聊天记录：");
                // 从配置获取最近消息显示数量
                int displayCount = plugin.getConfig().getInt("chat-hints.chat-history.recent-messages-display", 5);
                for (int i = 0; i < Math.min(displayCount, recentMessages.size()); i++) {
                    player.sendMessage(ChatColor.GRAY + "  " + (i + 1) + ". " + recentMessages.get(i));
                }
            }
        }
    }
    
    /**
     * 显示聊天统计信息
     * 定期向玩家显示聊天统计数据
     * 
     * @param player 玩家对象
     */
    private void showChatStats(Player player) {
        String playerName = player.getName();
        List<String> history = playerChatHistory.getOrDefault(playerName, new ArrayList<>());
        
        // 从配置获取统计显示间隔
        int statsInterval = plugin.getConfig().getInt("chat-hints.chat-history.stats-interval", 10);
        
        // 每N条消息显示一次统计
        if (history.size() % statsInterval == 0 && history.size() > 0) {
            int totalMessages = history.size();
            int avgLength = (int) history.stream().mapToInt(String::length).average().orElse(0);
            
            player.sendMessage(ChatColor.GOLD + "📊 聊天统计：");
            player.sendMessage(ChatColor.GRAY + "  总消息数：" + ChatColor.WHITE + totalMessages);
            player.sendMessage(ChatColor.GRAY + "  平均长度：" + ChatColor.WHITE + avgLength + " 字符");
            player.sendMessage(ChatColor.GRAY + "  活跃度：" + ChatColor.WHITE + getActivityLevel(totalMessages));
        }
    }
    
    /**
     * 更新玩家聊天历史
     * 
     * @param playerName 玩家名
     * @param message 消息内容
     */
    private void updateChatHistory(String playerName, String message) {
        List<String> history = playerChatHistory.computeIfAbsent(playerName, k -> new ArrayList<>());
        
        // 从配置获取历史记录大小限制
        int maxHistorySize = plugin.getConfig().getInt("chat-hints.chat-history.max-history-size", 50);
        
        // 限制历史记录大小
        if (history.size() >= maxHistorySize) {
            history.remove(0);
        }
        
        history.add(message);
    }
    
    /**
     * 获取其他玩家最近的消息
     * 
     * @param currentPlayer 当前玩家名
     * @return 其他玩家的最近消息列表
     */
    private List<String> getRecentOtherPlayerMessages(String currentPlayer) {
        List<String> recentMessages = new ArrayList<>();
        
        for (Map.Entry<String, List<String>> entry : playerChatHistory.entrySet()) {
            if (!entry.getKey().equals(currentPlayer) && !entry.getValue().isEmpty()) {
                recentMessages.add(entry.getKey() + ": " + entry.getValue().get(entry.getValue().size() - 1));
            }
        }
        
        // 按时间排序（简单的逆序）
        Collections.reverse(recentMessages);
        
        return recentMessages.subList(0, Math.min(5, recentMessages.size()));
    }
    
    /**
     * 检查消息是否包含回复关键词
     * 
     * @param message 消息内容
     * @return 是否包含回复关键词
     */
    private boolean containsReplyKeywords(String message) {
        String[] replyKeywords = {"回复", "回答", "回应", "reply", "answer", "response"};
        String lowerMessage = message.toLowerCase();
        
        for (String keyword : replyKeywords) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 根据消息数量获取活跃度等级
     * 
     * @param messageCount 消息数量
     * @return 活跃度等级描述
     */
    private String getActivityLevel(int messageCount) {
        // 从配置获取活跃度等级阈值
        int veryActiveThreshold = plugin.getConfig().getInt("chat-hints.activity-levels.very-active", 100);
        int activeThreshold = plugin.getConfig().getInt("chat-hints.activity-levels.active", 50);
        int normalThreshold = plugin.getConfig().getInt("chat-hints.activity-levels.normal", 20);
        
        // 从配置获取活跃度标签
        String veryActiveLabel = plugin.getConfig().getString("chat-hints.activity-levels.very-active-label", "🔥 非常活跃");
        String activeLabel = plugin.getConfig().getString("chat-hints.activity-levels.active-label", "🌟 活跃");
        String normalLabel = plugin.getConfig().getString("chat-hints.activity-levels.normal-label", "💫 一般");
        String defaultLabel = plugin.getConfig().getString("chat-hints.activity-levels.default-label", "🌱 新手");
        
        if (messageCount >= veryActiveThreshold) return ChatColor.GOLD + veryActiveLabel;
        if (messageCount >= activeThreshold) return ChatColor.GREEN + activeLabel;
        if (messageCount >= normalThreshold) return ChatColor.YELLOW + normalLabel;
        return ChatColor.GRAY + defaultLabel;
    }
    
    /**
     * 初始化表情符号映射表
     * 
     * @return 表情符号映射表
     */
    private Map<String, String> initializeEmojiMap() {
        Map<String, String> map = new HashMap<>();
        
        // 笑脸表情
        map.put(":)", "😊");
        map.put(":D", "😄");
        map.put(":(", "😞");
        map.put(":P", "😛");
        map.put(";)", "😉");
        map.put(":O", "😮");
        
        // 其他常用表情
        map.put("<3", "❤️");
        map.put(":heart:", "❤️");
        map.put(":star:", "⭐");
        map.put(":fire:", "🔥");
        map.put(":thumbsup:", "👍");
        map.put(":thumbsdown:", "👎");
        map.put(":ok:", "👌");
        map.put(":clap:", "👏");
        map.put(":wave:", "👋");
        map.put(":rocket:", "🚀");
        
        return map;
    }
    
    /**
     * 初始化命令提示映射
     * 
     * @return 命令提示映射表
     */
    private Map<String, String> initializeCommandHints() {
        Map<String, String> map = new HashMap<>();
        
        // 经济相关
        map.put("钱", "/emoney - 查看余额");
        map.put("余额", "/emoney - 查看余额");
        map.put("转账", "/epay <玩家> <金额> - 转账给其他玩家");
        map.put("支付", "/epay <玩家> <金额> - 转账给其他玩家");
        
        // 传送相关
        map.put("回家", "/ehome - 传送到家园");
        map.put("家园", "/esethome - 设置家园");
        map.put("传送", "/etp <玩家> - 传送到其他玩家");
        map.put("传送点", "/ewarp <名称> - 传送到传送点");
        map.put("设置传送点", "/esetwarp <名称> - 设置传送点");
        
        // 功能相关
        map.put("飞行", "/efly - 切换飞行模式");
        map.put("上帝", "/egod - 切换上帝模式");
        map.put("治疗", "/eheal - 恢复生命值");
        map.put("喂食", "/efeed - 恢复饥饿值");
        map.put("游戏模式", "/egm <模式> - 切换游戏模式");
        
        // 帮助相关
        map.put("帮助", "/ehelp - 查看帮助信息");
        map.put("命令", "/ehelp - 查看可用命令");
        map.put("功能", "/ehelp - 查看服务器功能");
        
        return map;
    }
}