package org.ljcode.myPlugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;

public class WarpCommand implements CommandExecutor {
    
    private final MyPlugin plugin;
    
    public WarpCommand(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("esetwarp")) {
            return handleSetWarp(sender, args);
        } else {
            return handleWarp(sender, args);
        }
    }
    
    private boolean handleSetWarp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!plugin.getConfig().getBoolean("warps.enabled", true)) {
            player.sendMessage(ChatColor.RED + "传送点系统已禁用!");
            return true;
        }
        
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "用法: /esetwarp <名称>");
            return true;
        }
        
        if (!player.hasPermission("essentialsx.warp.set")) {
            player.sendMessage(ChatColor.RED + "你没有权限设置传送点!");
            return true;
        }
        
        String warpName = args[0].toLowerCase(java.util.Locale.ROOT);
        int maxWarps = plugin.getConfig().getInt("warps.max-warps", 5);
        if (!plugin.getWarpManager().warpExists(warpName)
                && plugin.getWarpManager().getWarpNames().size() >= maxWarps
                && !player.hasPermission("essentialsx.admin")) {
            player.sendMessage(ChatColor.RED + "传送点数量已达上限 (" + maxWarps + ")!");
            return true;
        }
        
        if (plugin.getWarpManager().setWarp(warpName, player.getLocation())) {
            player.sendMessage(ChatColor.GREEN + "传送点 '" + warpName + "' 设置成功!");
        } else {
            player.sendMessage(ChatColor.RED + "传送点 '" + warpName + "' 已存在!");
        }
        
        return true;
    }
    
    private boolean handleWarp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!plugin.getConfig().getBoolean("warps.enabled", true)) {
            player.sendMessage(ChatColor.RED + "传送点系统已禁用!");
            return true;
        }
        
        if (args.length == 0) {
            // 显示所有传送点
            var warpNames = plugin.getWarpManager().getWarpNames();
            if (warpNames.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "没有可用的传送点!");
            } else {
                player.sendMessage(ChatColor.GREEN + "可用的传送点: " + String.join(", ", warpNames));
            }
            return true;
        }
        
        if (!player.hasPermission("essentialsx.warp.teleport")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用传送点!");
            return true;
        }
        
        String warpName = args[0].toLowerCase(java.util.Locale.ROOT);
        
        var warpLocation = plugin.getWarpManager().getWarp(warpName);
        if (warpLocation == null) {
            player.sendMessage(ChatColor.RED + "传送点 '" + warpName + "' 不存在!");
            return true;
        }
        
        // 保存当前位置并传送（统一冷却/安全检查）
        if (plugin.getTeleportManager().teleport(player, warpLocation)) {
            player.sendMessage(ChatColor.GREEN + "已传送到传送点 '" + warpName + "'!");
        }
        
        return true;
    }
}