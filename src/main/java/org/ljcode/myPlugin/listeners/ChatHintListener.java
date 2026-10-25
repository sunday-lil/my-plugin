package org.ljcode.myPlugin.listeners;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.ljcode.myPlugin.MyPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天提示监听器：表情转换、命令建议、回复建议、聊天统计。
 *
 * 表情转换会真正写回 AsyncPlayerChatEvent#setMessage，因此必须比
 * ChatFormatListener 更早注册（NORMAL 优先级、注册顺序决定执行顺序）。
 */
public class ChatHintListener implements Listener {

    private final MyPlugin plugin;
    private final Map<String, String> emojiMap = new ConcurrentHashMap<>();
    private final Map<String, String> commandHints = new ConcurrentHashMap<>();
    private final Map<String, List<String>> playerChatHistory = new ConcurrentHashMap<>();

    public ChatHintListener(MyPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        this.commandHints.putAll(initializeCommandHints());
    }

    private void loadConfig() {
        emojiMap.clear();
        emojiMap.putAll(initializeEmojiMap());

        ConfigurationSection custom = plugin.getConfig().getConfigurationSection("chat.hints.custom-emojis");
        if (custom != null) {
            for (String key : custom.getKeys(false)) {
                String value = custom.getString(key);
                if (key != null && value != null) {
                    emojiMap.put(key, value);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat.hints.enabled",
                plugin.getConfig().getBoolean("chat.hints.hints-enabled", true))) {
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage();

        // 真正的表情转换：写回事件，后续 ChatFormat 会使用转换后的消息
        if (plugin.getConfig().getBoolean("chat.hints.emoji-conversion", true)) {
            String converted = convertEmojis(message);
            if (!converted.equals(message)) {
                event.setMessage(converted);
            }
        }

        updateChatHistory(player.getName(), message);

        // 提示类操作统一回主线程，避免异步线程操作 Player/背包等 API
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                if (plugin.getConfig().getBoolean("chat.hints.command-suggestions", true)) {
                    suggestCommands(player, message);
                }
                if (plugin.getConfig().getBoolean("chat.hints.reply-suggestions", true)) {
                    suggestReplies(player, message);
                }
                if (plugin.getConfig().getBoolean("chat.hints.stats-enabled", true)) {
                    showChatStats(player);
                }
            }
        }.runTaskLater(plugin, 2L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerChatHistory.remove(event.getPlayer().getName());
    }

    private String convertEmojis(String message) {
        String converted = message;
        for (Map.Entry<String, String> entry : emojiMap.entrySet()) {
            if (converted.contains(entry.getKey())) {
                converted = converted.replace(entry.getKey(), entry.getValue());
            }
        }
        return converted;
    }

    private void suggestCommands(Player player, String message) {
        List<String> suggestions = new ArrayList<>();
        String lowerMessage = message.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, String> entry : commandHints.entrySet()) {
            if (lowerMessage.contains(entry.getKey())) {
                suggestions.add(entry.getValue());
            }
        }

        if (!suggestions.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "💡 您可能需要这些命令：");
            for (String suggestion : suggestions) {
                player.sendMessage(ChatColor.GRAY + "  • " + suggestion);
            }
        }
    }

    private void suggestReplies(Player player, String message) {
        if (!message.contains("@") && !containsReplyKeywords(message)) {
            return;
        }

        List<String> recentMessages = getRecentOtherPlayerMessages(player.getName());
        if (recentMessages.isEmpty()) {
            return;
        }

        player.sendMessage(ChatColor.AQUA + "💬 最近聊天记录：");
        int displayCount = plugin.getConfig().getInt("chat-hints.chat-history.recent-messages-display", 5);
        for (int i = 0; i < Math.min(displayCount, recentMessages.size()); i++) {
            player.sendMessage(ChatColor.GRAY + "  " + (i + 1) + ". " + recentMessages.get(i));
        }
    }

    private void showChatStats(Player player) {
        List<String> history = playerChatHistory.get(player.getName());
        if (history == null || history.isEmpty()) {
            return;
        }

        int statsInterval = Math.max(1, plugin.getConfig().getInt("chat-hints.chat-history.stats-interval", 10));
        int totalMessages;
        int averageLength;
        synchronized (history) {
            totalMessages = history.size();
            if (totalMessages % statsInterval != 0) {
                return;
            }
            averageLength = (int) history.stream().mapToInt(String::length).average().orElse(0);
        }

        player.sendMessage(ChatColor.GOLD + "📊 聊天统计：");
        player.sendMessage(ChatColor.GRAY + "  总消息数：" + ChatColor.WHITE + totalMessages);
        player.sendMessage(ChatColor.GRAY + "  平均长度：" + ChatColor.WHITE + averageLength + " 字符");
        player.sendMessage(ChatColor.GRAY + "  活跃度：" + ChatColor.WHITE + getActivityLevel(totalMessages));
    }

    private void updateChatHistory(String playerName, String message) {
        List<String> history = playerChatHistory.computeIfAbsent(playerName,
                key -> Collections.synchronizedList(new ArrayList<>()));
        int maxHistorySize = Math.max(1, plugin.getConfig().getInt("chat-hints.chat-history.max-history-size", 50));
        synchronized (history) {
            if (history.size() >= maxHistorySize) {
                history.remove(0);
            }
            history.add(message);
        }
    }

    private List<String> getRecentOtherPlayerMessages(String currentPlayer) {
        List<String> recentMessages = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : playerChatHistory.entrySet()) {
            if (entry.getKey().equals(currentPlayer)) {
                continue;
            }
            List<String> history = entry.getValue();
            synchronized (history) {
                if (!history.isEmpty()) {
                    recentMessages.add(entry.getKey() + ": " + history.get(history.size() - 1));
                }
            }
        }
        Collections.reverse(recentMessages);
        return new ArrayList<>(recentMessages.subList(0, Math.min(5, recentMessages.size())));
    }

    private boolean containsReplyKeywords(String message) {
        String[] replyKeywords = {"回复", "回答", "回应", "reply", "answer", "response"};
        String lowerMessage = message.toLowerCase(Locale.ROOT);
        for (String keyword : replyKeywords) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String getActivityLevel(int messageCount) {
        int veryActiveThreshold = plugin.getConfig().getInt("chat-hints.activity-levels.very-active", 100);
        int activeThreshold = plugin.getConfig().getInt("chat-hints.activity-levels.active", 50);
        int normalThreshold = plugin.getConfig().getInt("chat-hints.activity-levels.normal", 20);

        String veryActiveLabel = plugin.getConfig().getString("chat-hints.activity-levels.very-active-label", "🔥 非常活跃");
        String activeLabel = plugin.getConfig().getString("chat-hints.activity-levels.active-label", "🌟 活跃");
        String normalLabel = plugin.getConfig().getString("chat-hints.activity-levels.normal-label", "💫 一般");
        String defaultLabel = plugin.getConfig().getString("chat-hints.activity-levels.default-label", "🌱 新手");

        if (messageCount >= veryActiveThreshold) return ChatColor.GOLD + veryActiveLabel;
        if (messageCount >= activeThreshold) return ChatColor.GREEN + activeLabel;
        if (messageCount >= normalThreshold) return ChatColor.YELLOW + normalLabel;
        return ChatColor.GRAY + defaultLabel;
    }

    private Map<String, String> initializeEmojiMap() {
        Map<String, String> map = new ConcurrentHashMap<>();
        map.put(":)", "😊");
        map.put(":D", "😄");
        map.put(":(", "😞");
        map.put(":P", "😛");
        map.put(";)", "😉");
        map.put(":O", "😮");
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

    private Map<String, String> initializeCommandHints() {
        Map<String, String> map = new ConcurrentHashMap<>();
        map.put("钱", "/emoney - 查看余额");
        map.put("余额", "/emoney - 查看余额");
        map.put("转账", "/epay <玩家> <金额> - 转账给其他玩家");
        map.put("支付", "/epay <玩家> <金额> - 转账给其他玩家");
        map.put("回家", "/ehome - 传送到家园");
        map.put("家园", "/esethome - 设置家园");
        map.put("传送点", "/ewarp <名称> - 传送到传送点");
        map.put("传送", "/etp <玩家> - 传送到其他玩家");
        map.put("设置传送点", "/esetwarp <名称> - 设置传送点");
        map.put("飞行", "/efly - 切换飞行模式");
        map.put("上帝", "/egod - 切换上帝模式");
        map.put("治疗", "/eheal - 恢复生命值");
        map.put("喂食", "/efeed - 恢复饥饿值");
        map.put("游戏模式", "/egm <模式> [玩家] - 切换游戏模式");
        map.put("帮助", "/ehelp - 查看帮助信息");
        map.put("命令", "/ehelp - 查看可用命令");
        map.put("功能", "/ehelp - 查看服务器功能");
        return map;
    }
}
