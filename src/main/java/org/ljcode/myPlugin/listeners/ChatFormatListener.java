package org.ljcode.myPlugin.listeners;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.ljcode.myPlugin.MyPlugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天消息格式化监听器。
 *
 * 注意：AsyncPlayerChatEvent 在异步线程触发，这里只做纯字符串计算；
 * 任何对玩家发包（动作栏）都调度回主线程。
 */
public class ChatFormatListener implements Listener {

    private static final String MESSAGE_TOKEN = "\u0000__CHAT_MESSAGE__\u0000";

    private final MyPlugin plugin;
    private final Map<String, Integer> playerLevels = new ConcurrentHashMap<>();

    public ChatFormatListener(MyPlugin plugin) {
        this.plugin = plugin;
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerLevels.put(player.getName(), randomLevel());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat.format-enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        String template = plugin.getConfig().getString("chat.format", "%prefix%%player%&7:&r %message%");
        String formatted = replaceChatPlaceholders(template, player, event.getMessage());

        // Bukkit 会用 String.format(format, player, message) 渲染，必须转义玩家输入里的 %
        event.setFormat(formatted.replace("%", "%%"));

        if (plugin.getConfig().getBoolean("chat.actionbar-enabled", false)) {
            sendChatActionBarNotification(player, event.getMessage());
        }
    }

    private String replaceChatPlaceholders(String template, Player player, String message) {
        String currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
        int playerLevel = getPlayerLevel(player);
        String levelColor = getLevelColor(playerLevel);
        String playerPrefix = getPlayerPrefix(player);
        String playerSuffix = getPlayerSuffix(player);

        String result = template
                .replace("%prefix%", plugin.getConfig().getString("chat.prefix", "&6[聊天]&r "))
                .replace("%player%", player.getName())
                .replace("%displayname%", player.getDisplayName())
                .replace("%time%", currentTime)
                .replace("%world%", player.getWorld().getName())
                .replace("%level%", String.valueOf(playerLevel))
                .replace("%level_color%", levelColor)
                .replace("%player_prefix%", playerPrefix)
                .replace("%player_suffix%", playerSuffix)
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%ping%", String.valueOf(getPlayerPing(player)))
                .replace("%gamemode%", player.getGameMode().name().toLowerCase(Locale.ROOT))
                // 最后才插入消息，避免玩家输入 %ping%、%gamemode% 被二次替换
                .replace("%message%", MESSAGE_TOKEN);

        if (plugin.getConfig().getBoolean("chat.color-enabled", true)) {
            result = ChatColor.translateAlternateColorCodes('&', result);
        }

        return result.replace(MESSAGE_TOKEN, message == null ? "" : message);
    }

    private void sendChatActionBarNotification(Player player, String message) {
        int truncateLength = plugin.getConfig().getInt("chat-format.actionbar-notification.message-truncate-length", 20);
        String actionBarMessage = ChatColor.translateAlternateColorCodes('&',
                "&a💬 &e" + player.getName() + " &7: &f" +
                        (message.length() > truncateLength ? message.substring(0, truncateLength) + "..." : message));

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(actionBarMessage));
            }
        });
    }

    private int getPlayerLevel(Player player) {
        return playerLevels.computeIfAbsent(player.getName(), key -> randomLevel());
    }

    private int randomLevel() {
        return (int) (Math.random() * 100) + 1;
    }

    private String getLevelColor(int level) {
        int goldThreshold = plugin.getConfig().getInt("chat-format.level-colors.gold-threshold", 80);
        int redThreshold = plugin.getConfig().getInt("chat-format.level-colors.red-threshold", 60);
        int blueThreshold = plugin.getConfig().getInt("chat-format.level-colors.blue-threshold", 40);
        int greenThreshold = plugin.getConfig().getInt("chat-format.level-colors.green-threshold", 20);
        String defaultColor = plugin.getConfig().getString("chat-format.level-colors.default-color", "&7");

        if (level >= goldThreshold) return "&6";
        if (level >= redThreshold) return "&c";
        if (level >= blueThreshold) return "&9";
        if (level >= greenThreshold) return "&a";
        return defaultColor;
    }

    private String getPlayerPrefix(Player player) {
        int level = getPlayerLevel(player);
        int veteranLevel = plugin.getConfig().getInt("chat-format.prefixes.veteran-level", 50);
        int activeLevel = plugin.getConfig().getInt("chat-format.prefixes.active-level", 20);

        if (player.hasPermission("group.admin")) {
            return plugin.getConfig().getString("chat-format.prefixes.admin", "&4[管理员]&r ");
        } else if (player.hasPermission("group.moderator")) {
            return plugin.getConfig().getString("chat-format.prefixes.moderator", "&2[管理]&r ");
        } else if (player.hasPermission("group.vip")) {
            return plugin.getConfig().getString("chat-format.prefixes.vip", "&d[VIP]&r ");
        } else if (level >= veteranLevel) {
            return plugin.getConfig().getString("chat-format.prefixes.veteran", "&6[资深]&r ");
        } else if (level >= activeLevel) {
            return plugin.getConfig().getString("chat-format.prefixes.active", "&e[活跃]&r ");
        }
        return plugin.getConfig().getString("chat-format.prefixes.default", "&7[玩家]&r ");
    }

    private String getPlayerSuffix(Player player) {
        int ping = getPlayerPing(player);
        int lowPingThreshold = plugin.getConfig().getInt("chat-format.suffixes.low-ping-threshold", 50);
        int mediumPingThreshold = plugin.getConfig().getInt("chat-format.suffixes.medium-ping-threshold", 150);

        if (ping < lowPingThreshold) {
            return plugin.getConfig().getString("chat-format.suffixes.low-ping", " &a⚡");
        } else if (ping < mediumPingThreshold) {
            return plugin.getConfig().getString("chat-format.suffixes.medium-ping", " &e📶");
        } else {
            return plugin.getConfig().getString("chat-format.suffixes.high-ping", " &c📡");
        }
    }

    private int getPlayerPing(Player player) {
        return player.getPing();
    }
}
