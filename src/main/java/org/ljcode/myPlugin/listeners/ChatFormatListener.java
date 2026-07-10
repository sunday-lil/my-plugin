package org.ljcode.myPlugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.ljcode.myPlugin.MyPlugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 聊天消息格式化监听器
 * 监听玩家聊天事件，提供丰富的聊天格式化和美化功能
 * 支持自定义前缀、后缀、时间戳、玩家等级等功能
 */
public class ChatFormatListener implements Listener {
    
    // 插件主类实例
    private final MyPlugin plugin;
    
    // 玩家等级缓存（实际项目中应该从数据库或文件中读取）
    private final Map<String, Integer> playerLevels;
    
    /**
     * 构造函数，初始化聊天格式化监听器
     * 
     * @param plugin 插件主类实例
     */
    public ChatFormatListener(MyPlugin plugin) {
        this.plugin = plugin;
        this.playerLevels = new HashMap<>();
        
        // 初始化一些示例玩家等级（实际项目中应该从数据库加载）
        initializePlayerLevels();
    }
    
    /**
     * 处理玩家聊天事件
     * 对玩家发送的聊天消息进行格式化和美化
     * 
     * @param event 异步玩家聊天事件
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String originalMessage = event.getMessage();
        
        // 检查是否启用聊天格式化功能
        boolean chatFormatEnabled = plugin.getConfig().getBoolean("chat.format-enabled", true);
        if (!chatFormatEnabled) {
            return; // 如果未启用，使用默认聊天格式
        }
        
        // 获取聊天格式模板
        String formatTemplate = plugin.getConfig().getString("chat.format", 
            "%prefix%%player%&7:&r %message%");
        
        // 替换所有占位符
        String formattedMessage = replaceChatPlaceholders(formatTemplate, player, originalMessage);
        
        // 设置格式化后的消息
        event.setFormat(formattedMessage);
        
        // 检查是否启用聊天动作栏提示
        boolean chatActionBarEnabled = plugin.getConfig().getBoolean("chat.actionbar-enabled", false);
        if (chatActionBarEnabled) {
            sendChatActionBarNotification(player, originalMessage);
        }
    }
    
    /**
     * 替换聊天消息中的占位符
     * 支持丰富的占位符替换功能
     * 
     * @param template 聊天格式模板
     * @param player 发送消息的玩家
     * @param message 原始消息内容
     * @return 替换占位符后的格式化消息
     */
    private String replaceChatPlaceholders(String template, Player player, String message) {
        // 创建日期格式化器
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        String currentTime = dateFormat.format(new Date());
        
        // 获取玩家等级
        int playerLevel = getPlayerLevel(player);
        String levelColor = getLevelColor(playerLevel);
        
        // 获取玩家前缀和后缀（实际项目中应该从权限插件获取）
        String playerPrefix = getPlayerPrefix(player);
        String playerSuffix = getPlayerSuffix(player);
        
        // 替换所有占位符
        String result = template
            .replace("%prefix%", plugin.getConfig().getString("chat.prefix", "&6[聊天]&r "))
            .replace("%player%", player.getName())
            .replace("%displayname%", player.getDisplayName())
            .replace("%message%", message)
            .replace("%time%", currentTime)
            .replace("%world%", player.getWorld().getName())
            .replace("%level%", String.valueOf(playerLevel))
            .replace("%level_color%", levelColor)
            .replace("%player_prefix%", playerPrefix)
            .replace("%player_suffix%", playerSuffix)
            .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
            .replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()))
            .replace("%ping%", String.valueOf(getPlayerPing(player)))
            .replace("%gamemode%", player.getGameMode().toString().toLowerCase());
        
        return ChatColor.translateAlternateColorCodes('&', result);
    }
    
    /**
     * 发送聊天动作栏通知
     * 当玩家发送消息时，在动作栏显示通知
     * 
     * @param player 发送消息的玩家
     * @param message 消息内容
     */
    private void sendChatActionBarNotification(Player player, String message) {
        // 从配置获取消息截断长度
        int truncateLength = plugin.getConfig().getInt("chat-format.actionbar-notification.message-truncate-length", 20);
        
        // 创建动作栏消息
        String actionBarMessage = ChatColor.translateAlternateColorCodes('&', 
            "&a💬 &e" + player.getName() + " &7: &f" + 
            (message.length() > truncateLength ? message.substring(0, truncateLength) + "..." : message));
        
        // 从配置获取标题显示时间
        int fadeIn = plugin.getConfig().getInt("chat-format.actionbar-notification.title-fade-in", 0);
        int stay = plugin.getConfig().getInt("chat-format.actionbar-notification.title-stay", 40);
        int fadeOut = plugin.getConfig().getInt("chat-format.actionbar-notification.title-fade-out", 10);
        
        // 向所有在线玩家发送动作栏通知
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            // 使用标题API模拟动作栏效果
            onlinePlayer.sendTitle("", actionBarMessage, fadeIn, stay, fadeOut);
        }
    }
    
    /**
     * 获取玩家等级
     * 从缓存中获取玩家等级，如果不存在则生成一个随机等级
     * 
     * @param player 玩家对象
     * @return 玩家等级
     */
    private int getPlayerLevel(Player player) {
        return playerLevels.computeIfAbsent(player.getName(), 
            k -> (int) (Math.random() * 100) + 1);
    }
    
    /**
     * 根据玩家等级获取等级颜色
     * 
     * @param level 玩家等级
     * @return 等级对应的颜色代码
     */
    private String getLevelColor(int level) {
        // 从配置获取等级颜色阈值
        int goldThreshold = plugin.getConfig().getInt("chat-format.level-colors.gold-threshold", 80);
        int redThreshold = plugin.getConfig().getInt("chat-format.level-colors.red-threshold", 60);
        int blueThreshold = plugin.getConfig().getInt("chat-format.level-colors.blue-threshold", 40);
        int greenThreshold = plugin.getConfig().getInt("chat-format.level-colors.green-threshold", 20);
        String defaultColor = plugin.getConfig().getString("chat-format.level-colors.default-color", "&7");
        
        if (level >= goldThreshold) return "&6"; // 金色
        if (level >= redThreshold) return "&c"; // 红色
        if (level >= blueThreshold) return "&9"; // 蓝色
        if (level >= greenThreshold) return "&a"; // 绿色
        return defaultColor; // 默认颜色
    }
    
    /**
     * 获取玩家前缀
     * 根据玩家权限和等级生成前缀
     * 
     * @param player 玩家对象
     * @return 玩家前缀
     */
    private String getPlayerPrefix(Player player) {
        int level = getPlayerLevel(player);
        
        // 从配置获取前缀等级阈值
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
    
    /**
     * 获取玩家后缀
     * 根据玩家状态生成后缀
     * 
     * @param player 玩家对象
     * @return 玩家后缀
     */
    private String getPlayerSuffix(Player player) {
        int ping = getPlayerPing(player);
        
        // 从配置获取延迟阈值
        int lowPingThreshold = plugin.getConfig().getInt("chat-format.suffixes.low-ping-threshold", 50);
        int mediumPingThreshold = plugin.getConfig().getInt("chat-format.suffixes.medium-ping-threshold", 150);
        
        if (ping < lowPingThreshold) {
            return plugin.getConfig().getString("chat-format.suffixes.low-ping", " &a⚡"); // 低延迟
        } else if (ping < mediumPingThreshold) {
            return plugin.getConfig().getString("chat-format.suffixes.medium-ping", " &e📶"); // 中等延迟
        } else {
            return plugin.getConfig().getString("chat-format.suffixes.high-ping", " &c📡"); // 高延迟
        }
    }
    
    /**
     * 获取玩家Ping值
     * 通过反射获取玩家的ping值
     * 
     * @param player 玩家对象
     * @return 玩家ping值
     */
    private int getPlayerPing(Player player) {
        try {
            Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
            return (int) entityPlayer.getClass().getField("ping").get(entityPlayer);
        } catch (Exception e) {
            // 从配置获取默认Ping值
            return plugin.getConfig().getInt("debug-info.default-ping", 100); // 默认值
        }
    }
    
    /**
     * 初始化玩家等级缓存
     * 为在线玩家生成初始等级
     */
    private void initializePlayerLevels() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerLevels.put(player.getName(), (int) (Math.random() * 100) + 1);
        }
    }
}