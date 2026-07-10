package org.ljcode.myPlugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;

public class DebugInfoCommand implements CommandExecutor {
    
    private final MyPlugin plugin;
    
    public DebugInfoCommand(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // 发送简化版的调试信息
        player.sendMessage(ChatColor.DARK_GRAY + "------------------- " + 
                          ChatColor.WHITE + "调试信息" + ChatColor.DARK_GRAY + " -------------------");
        
        // 基本位置信息
        player.sendMessage(ChatColor.AQUA + "位置: " + 
                          ChatColor.RESET + String.format("%.2f, %.2f, %.2f", 
                          player.getLocation().getX(), 
                          player.getLocation().getY(), 
                          player.getLocation().getZ()));
        
        // 维度信息
        player.sendMessage(ChatColor.AQUA + "维度: " + 
                          ChatColor.RESET + player.getWorld().getName());
        
        // 生物群系
        player.sendMessage(ChatColor.AQUA + "生物群系: " + 
                          ChatColor.RESET + player.getLocation().getBlock().getBiome().name());
        
        // 时间信息
        long worldTime = player.getWorld().getTime();
        long dayTime = worldTime % 24000;
        String timeOfDay = getTimeOfDay(dayTime);
        player.sendMessage(ChatColor.AQUA + "时间: " + 
                          ChatColor.RESET + String.format("%d (%s)", worldTime, timeOfDay));
        
        // 玩家健康和饥饿值
        player.sendMessage(ChatColor.AQUA + "生命值: " + 
                          ChatColor.RESET + String.format("%.1f/%.1f", 
                          player.getHealth(), player.getMaxHealth()));
        player.sendMessage(ChatColor.AQUA + "饥饿值: " + 
                          ChatColor.RESET + String.format("%d/%d", 
                          player.getFoodLevel(), 20));
        
        // 游戏模式
        player.sendMessage(ChatColor.AQUA + "游戏模式: " + 
                          ChatColor.RESET + player.getGameMode().name());
        
        // 服务器版本信息
        player.sendMessage(ChatColor.AQUA + "服务器版本: " + 
                          ChatColor.RESET + plugin.getServer().getVersion());
        
        // 世界信息
        player.sendMessage(ChatColor.AQUA + "世界类型: " + 
                          ChatColor.RESET + player.getWorld().getEnvironment().name());
        
        // 玩家延迟（使用替代方法）
        int ping = getPlayerPing(player);
        player.sendMessage(ChatColor.AQUA + "延迟: " + 
                          ChatColor.RESET + ping + "ms");
        
        player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
        
        return true;
    }
    
    private String getTimeOfDay(long time) {
        // 从配置获取时间分段定义
        int dawnStart = plugin.getConfig().getInt("debug-info.time-segments.dawn[0]", 0);
        int dawnEnd = plugin.getConfig().getInt("debug-info.time-segments.dawn[1]", 1000);
        int morningStart = plugin.getConfig().getInt("debug-info.time-segments.morning[0]", 1000);
        int morningEnd = plugin.getConfig().getInt("debug-info.time-segments.morning[1]", 6000);
        int noonStart = plugin.getConfig().getInt("debug-info.time-segments.noon[0]", 6000);
        int noonEnd = plugin.getConfig().getInt("debug-info.time-segments.noon[1]", 12000);
        int afternoonStart = plugin.getConfig().getInt("debug-info.time-segments.afternoon[0]", 12000);
        int afternoonEnd = plugin.getConfig().getInt("debug-info.time-segments.afternoon[1]", 18000);
        int nightStart = plugin.getConfig().getInt("debug-info.time-segments.night[0]", 18000);
        int nightEnd = plugin.getConfig().getInt("debug-info.time-segments.night[1]", 23000);
        int lateNightStart = plugin.getConfig().getInt("debug-info.time-segments.late-night[0]", 23000);
        int lateNightEnd = plugin.getConfig().getInt("debug-info.time-segments.late-night[1]", 24000);
        
        if (time >= dawnStart && time < dawnEnd) {
            return plugin.getConfig().getString("debug-info.time-segments.dawn-label", "黎明");
        } else if (time >= morningStart && time < morningEnd) {
            return plugin.getConfig().getString("debug-info.time-segments.morning-label", "上午");
        } else if (time >= noonStart && time < noonEnd) {
            return plugin.getConfig().getString("debug-info.time-segments.noon-label", "中午");
        } else if (time >= afternoonStart && time < afternoonEnd) {
            return plugin.getConfig().getString("debug-info.time-segments.afternoon-label", "下午");
        } else if (time >= nightStart && time < nightEnd) {
            return plugin.getConfig().getString("debug-info.time-segments.night-label", "夜晚");
        } else {
            return plugin.getConfig().getString("debug-info.time-segments.late-night-label", "深夜");
        }
    }
    
    /**
     * 获取玩家的网络延迟
     * 使用Bukkit API提供的方法
     */
    private int getPlayerPing(Player player) {
        try {
            // In newer versions of Spigot, the ping property can be accessed differently
            // Using the standard ping property from CraftPlayer if available
            return player.getPing();
        } catch (Exception e) {
            // Fallback to -1 if ping cannot be retrieved
            return -1;
        }
    }
}