package org.ljcode.myPlugin.managers;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;
import me.clip.placeholderapi.PlaceholderAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnnouncementManager {
    private final MyPlugin plugin;
    private final Random random = new Random();
    private final ConcurrentHashMap<String, BossBar> activeBossBars = new ConcurrentHashMap<>();
    private FileConfiguration config;

    public AnnouncementManager(MyPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        config = plugin.getConfig();
        
        // 添加公告相关的默认配置值
        if (!config.contains("announcements")) {
            config.addDefault("announcements.messages", new ArrayList<>());
            config.addDefault("announcements.actionbar-messages", new ArrayList<>());
            config.addDefault("announcements.title-messages", new ArrayList<>());
            config.addDefault("announcements.bossbar.enabled", true);
            config.addDefault("announcements.bossbar.messages", new ArrayList<>());
            config.addDefault("announcements.bossbar.color", "BLUE");
            config.addDefault("announcements.bossbar.style", "SOLID");
            config.addDefault("announcements.random-order", true);
            config.addDefault("announcements.interval", 30);
            
            plugin.saveConfig();
        }
    }

    /**
     * 发送聊天消息给所有玩家
     */
    public void sendChatAnnouncement(String message) {
        String processedMessage = processPlaceholders(null, message);
        processedMessage = applyColorCodes(processedMessage);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerProcessedMessage = processPlaceholders(player, processedMessage);
            sendFormattedMessage(player, playerProcessedMessage);
        }
    }

    /**
     * 发送聊天消息给指定玩家
     */
    public void sendChatAnnouncement(Player player, String message) {
        String processedMessage = processPlaceholders(player, message);
        processedMessage = applyColorCodes(processedMessage);
        sendFormattedMessage(player, processedMessage);
    }

    /**
     * 发送动作栏消息给所有玩家
     */
    public void sendActionBarAnnouncement(String message) {
        String processedMessage = processPlaceholders(null, message);
        processedMessage = applyColorCodes(processedMessage);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerProcessedMessage = processPlaceholders(player, processedMessage);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(playerProcessedMessage));
        }
    }

    /**
     * 发送动作栏消息给指定玩家
     */
    public void sendActionBarAnnouncement(Player player, String message) {
        String processedMessage = processPlaceholders(player, message);
        processedMessage = applyColorCodes(processedMessage);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(processedMessage));
    }

    /**
     * 发送标题消息给所有玩家
     */
    public void sendTitleAnnouncement(String title, String subtitle) {
        String processedTitle = processPlaceholders(null, title != null ? title : "");
        String processedSubtitle = processPlaceholders(null, subtitle != null ? subtitle : "");
        processedTitle = applyColorCodes(processedTitle);
        processedSubtitle = applyColorCodes(processedSubtitle);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerProcessedTitle = processPlaceholders(player, processedTitle);
            String playerProcessedSubtitle = processPlaceholders(player, processedSubtitle);
            player.sendTitle(playerProcessedTitle, playerProcessedSubtitle, 10, 70, 20);
        }
    }

    /**
     * 发送标题消息给指定玩家
     */
    public void sendTitleAnnouncement(Player player, String title, String subtitle) {
        String processedTitle = processPlaceholders(player, title != null ? title : "");
        String processedSubtitle = processPlaceholders(player, subtitle != null ? subtitle : "");
        processedTitle = applyColorCodes(processedTitle);
        processedSubtitle = applyColorCodes(processedSubtitle);
        player.sendTitle(processedTitle, processedSubtitle, 10, 70, 20);
    }

    /**
     * 发送BossBar消息给所有玩家
     */
    public void sendBossBarAnnouncement(String message) {
        String processedMessage = processPlaceholders(null, message);
        processedMessage = applyColorCodes(processedMessage);
        
        // 移除现有的BossBar
        removeExistingBossBars();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerProcessedMessage = processPlaceholders(player, processedMessage);
            BossBar playerBossBar = Bukkit.createBossBar(playerProcessedMessage, getBossBarColor(), getBossBarStyle());
            playerBossBar.setProgress(1.0); // 设置进度为100%，确保BossBar显示
            playerBossBar.addPlayer(player);
            activeBossBars.put(player.getName(), playerBossBar);
        }
    }

    /**
     * 发送BossBar消息给指定玩家
     */
    public void sendBossBarAnnouncement(Player player, String message) {
        String processedMessage = processPlaceholders(player, message);
        processedMessage = applyColorCodes(processedMessage);
        
        // 移除玩家现有的BossBar
        removeBossBar(player);
        
        BossBar bossBar = Bukkit.createBossBar(processedMessage, getBossBarColor(), getBossBarStyle());
        bossBar.setProgress(1.0); // 设置进度为100%，确保BossBar显示
        bossBar.addPlayer(player);
        activeBossBars.put(player.getName(), bossBar);
    }
    
    /**
     * 获取BossBar颜色配置
     */
    private BarColor getBossBarColor() {
        String colorStr = config.getString("announcements.bossbar.color", "BLUE").toUpperCase();
        try {
            return BarColor.valueOf(colorStr);
        } catch (IllegalArgumentException e) {
            // 如果配置的颜色无效，默认使用蓝色
            return BarColor.BLUE;
        }
    }
    
    /**
     * 获取BossBar样式配置
     */
    private BarStyle getBossBarStyle() {
        String styleStr = config.getString("announcements.bossbar.style", "SOLID").toUpperCase();
        try {
            return BarStyle.valueOf(styleStr);
        } catch (IllegalArgumentException e) {
            // 如果配置的样式无效，默认使用实线样式
            return BarStyle.SOLID;
        }
    }

    /**
     * 移除所有活跃的BossBar
     */
    public void removeAllBossBars() {
        for (BossBar bossBar : activeBossBars.values()) {
            bossBar.removeAll();
        }
        activeBossBars.clear();
    }

    /**
     * 移除指定玩家的BossBar
     */
    public void removeBossBar(Player player) {
        BossBar bossBar = activeBossBars.remove(player.getName());
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    /**
     * 移除所有现有BossBar
     */
    private void removeExistingBossBars() {
        for (String playerName : new ArrayList<>(activeBossBars.keySet())) {
            BossBar bossBar = activeBossBars.remove(playerName);
            if (bossBar != null) {
                Player player = Bukkit.getPlayerExact(playerName);
                if (player != null) {
                    bossBar.removePlayer(player);
                } else {
                    bossBar.removeAll();
                }
            }
        }
    }

    /**
     * 处理占位符
     */
    private String processPlaceholders(Player player, String message) {
        if (player == null) {
            // 如果没有指定玩家，使用通用占位符
            message = message.replace("%server_online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                           .replace("%server_max_players%", String.valueOf(Bukkit.getMaxPlayers()));
        } else {
            // 如果有指定玩家，处理玩家相关占位符
            if (isPlaceholderAPIAvailable()) {
                message = PlaceholderAPI.setPlaceholders(player, message);
            } else {
                message = message.replace("%player_name%", player.getName())
                               .replace("%player_displayname%", player.getDisplayName());
            }
            
            // 通用占位符
            message = message.replace("%server_online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                           .replace("%server_max_players%", String.valueOf(Bukkit.getMaxPlayers()))
                           .replace("%player_world%", player.getWorld().getName())
                           .replace("%time%", String.valueOf(System.currentTimeMillis() / 1000))
                           .replace("%date%", java.time.LocalDate.now().toString());
        }
        
        return message;
    }

    /**
     * 应用颜色代码
     */
    private String applyColorCodes(String message) {
        // 支持 & 格式的颜色代码
        message = org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
        
        // 支持十六进制颜色代码 &#RRGGBB
        message = convertHexColors(message);
        
        // 支持迷你信息格式
        message = convertMiniMessageFormat(message);
        
        return message;
    }

    /**
     * 转换迷你信息格式
     */
    private String convertMiniMessageFormat(String message) {
        // 简单的迷你信息格式转换
        // 支持 <red>, <green>, <blue>, <bold>, <gradient>, <rainbow> 等基本格式
        message = message
            .replace("<red>", "§c")
            .replace("</red>", "§r")
            .replace("<green>", "§a")
            .replace("</green>", "§r")
            .replace("<blue>", "§9")
            .replace("</blue>", "§r")
            .replace("<yellow>", "§e")
            .replace("</yellow>", "§r")
            .replace("<purple>", "§d")
            .replace("</purple>", "§r")
            .replace("<bold>", "§l")
            .replace("</bold>", "§r")
            .replace("<italic>", "§o")
            .replace("</italic>", "§r")
            .replace("<underlined>", "§n")
            .replace("</underlined>", "§r")
            .replace("<strikethrough>", "§m")
            .replace("</strikethrough>", "§r")
            .replace("<obfuscated>", "§k")
            .replace("</obfuscated>", "§r");
        
        // 处理渐变颜色和彩虹效果（简化版）
        message = message.replace("<gradient:#00c6ff:#0072ff>", "§b");
        message = message.replace("</gradient>", "§r");
        message = message.replace("<rainbow>", "§6§l");
        message = message.replace("</rainbow>", "§r");
        
        return message;
    }

    /**
     * 转换十六进制颜色代码
     */
    private String convertHexColors(String message) {
        // 检查服务端版本是否支持十六进制颜色
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            try {
                net.md_5.bungee.api.ChatColor color = net.md_5.bungee.api.ChatColor.of("#" + matcher.group(1));
                matcher.appendReplacement(buffer, color.toString());
            } catch (Exception e) {
                // 如果方法不存在或不支持，则跳过十六进制颜色转换
                // 继续处理下一个匹配项而不是中断整个循环
                matcher.appendReplacement(buffer, matcher.group(0)); // 保留原始文本
            }
        }
        
        if (buffer.length() > 0) {
            matcher.appendTail(buffer);
            return buffer.toString();
        }
        
        // 如果无法转换十六进制颜色，则返回原始消息
        return message;
    }

    /**
     * 发送格式化消息（包含可点击链接）
     */
    private void sendFormattedMessage(Player player, String message) {
        // 检测并转换链接为可点击格式
        TextComponent[] components = createClickableText(message);
        player.spigot().sendMessage(components);
    }

    /**
     * 创建可点击的文本组件
     */
    private TextComponent[] createClickableText(String message) {
        List<TextComponent> components = new ArrayList<>();
        
        // 正则表达式匹配各种URL格式
        String urlRegex = "(https?://[\\w.-]+(?:\\.[\\w\\.-]+)+[\\w\\-\\._~:/?#\\[\\]@!\\$&'\\(\\)\\*\\+,;=.]+|www\\.[\\w.-]+(?:\\.[\\w\\.-]+)+[\\w\\-\\._~:/?#\\[\\]@!\\$&'\\(\\)\\*\\+,;=.]+|discord\\.gg/[\\w.-]+)";
        Pattern urlPattern = Pattern.compile(urlRegex);
        Matcher matcher = urlPattern.matcher(message);
        
        int lastEnd = 0;
        while (matcher.find()) {
            // 添加非链接部分
            if (lastEnd < matcher.start()) {
                String nonLinkPart = message.substring(lastEnd, matcher.start());
                if (!nonLinkPart.isEmpty()) {
                    TextComponent nonLinkComponent = new TextComponent(org.bukkit.ChatColor.translateAlternateColorCodes('&', nonLinkPart));
                    components.add(nonLinkComponent);
                }
            }
            
            // 添加链接部分
            String url = matcher.group();
            String fullUrl = url;
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                if (url.startsWith("discord.gg/")) {
                    fullUrl = "https://" + url;
                } else if (url.startsWith("www.")) {
                    fullUrl = "http://" + url;
                } else {
                    fullUrl = "https://" + url;
                }
            }
            
            TextComponent linkComponent = new TextComponent(url);
            linkComponent.setColor(net.md_5.bungee.api.ChatColor.AQUA);
            linkComponent.setUnderlined(true);
            linkComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, fullUrl));
            linkComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("点击打开链接: " + fullUrl).create()));
            components.add(linkComponent);
            
            lastEnd = matcher.end();
        }
        
        // 添加剩余的非链接部分
        if (lastEnd < message.length()) {
            String remainingPart = message.substring(lastEnd);
            if (!remainingPart.isEmpty()) {
                TextComponent remainingComponent = new TextComponent(org.bukkit.ChatColor.translateAlternateColorCodes('&', remainingPart));
                components.add(remainingComponent);
            }
        }
        
        return components.toArray(new TextComponent[0]);
    }

    /**
     * 检查PlaceholderAPI是否可用
     */
    private boolean isPlaceholderAPIAvailable() {
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 获取随机消息
     */
    public String getRandomMessage(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        return messages.get(random.nextInt(messages.size()));
    }

    /**
     * 获取随机对象（用于处理不同类型的消息列表）
     */
    public Object getRandomObject(List<?> objects) {
        if (objects == null || objects.isEmpty()) {
            return null;
        }
        return objects.get(random.nextInt(objects.size()));
    }

    /**
     * 获取按顺序的消息
     */
    private int sequenceIndex = 0;
    public String getSequentialMessage(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        String message = messages.get(sequenceIndex % messages.size());
        sequenceIndex++;
        return message;
    }

    /**
     * 获取按顺序的对象（用于处理不同类型的消息列表）
     */
    private int sequenceObjectIndex = 0;
    public Object getSequentialObject(List<?> objects) {
        if (objects == null || objects.isEmpty()) {
            return null;
        }
        Object obj = objects.get(sequenceObjectIndex % objects.size());
        sequenceObjectIndex++;
        return obj;
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
}