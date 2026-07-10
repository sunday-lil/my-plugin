package org.ljcode.myPlugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;

public class HomeCommand implements CommandExecutor {
    
    private final MyPlugin plugin;
    
    public HomeCommand(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (label.equalsIgnoreCase("esethome")) {
            return handleSetHome(player, args);
        } else {
            return handleHome(player, args);
        }
    }
    
    private boolean handleSetHome(Player player, String[] args) {
        String homeName = "home";
        if (args.length > 0) {
            homeName = args[0].toLowerCase();
        }
        
        if (!player.hasPermission("essentialsx.home.set")) {
            player.sendMessage(ChatColor.RED + "你没有权限设置家!");
            return true;
        }
        
        if (plugin.getHomeManager().setHome(player, homeName)) {
            player.sendMessage(ChatColor.GREEN + "家 '" + homeName + "' 设置成功!");
        } else {
            player.sendMessage(ChatColor.RED + "无法设置家 '" + homeName + "'!");
        }
        
        return true;
    }
    
    private boolean handleHome(Player player, String[] args) {
        String homeName = "home";
        if (args.length > 0) {
            homeName = args[0].toLowerCase();
        }
        
        if (!player.hasPermission("essentialsx.home.teleport")) {
            player.sendMessage(ChatColor.RED + "你没有权限传送回家!");
            return true;
        }
        
        var homeLocation = plugin.getHomeManager().getHome(player, homeName);
        if (homeLocation == null) {
            player.sendMessage(ChatColor.RED + "家 '" + homeName + "' 不存在!");
            return true;
        }
        
        // 保存当前位置
        plugin.getTeleportManager().setLastLocation(player, player.getLocation());
        
        // 传送回家
        player.teleport(homeLocation);
        player.sendMessage(ChatColor.GREEN + "已传送回家 '" + homeName + "'!");
        
        return true;
    }
}