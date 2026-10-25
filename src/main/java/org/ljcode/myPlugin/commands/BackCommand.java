package org.ljcode.myPlugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;

public class BackCommand implements CommandExecutor {
    
    private final MyPlugin plugin;
    
    public BackCommand(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("essentialsx.back")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }
        
        var lastLocation = plugin.getTeleportManager().getLastLocation(player);
        if (lastLocation == null) {
            player.sendMessage(ChatColor.RED + "没有可返回的位置!");
            return true;
        }
        
        // 统一传送入口会记录当前坐标，/eback 可在两点之间切换
        if (plugin.getTeleportManager().teleport(player, lastLocation)) {
            player.sendMessage(ChatColor.GREEN + "已返回上一个位置!");
        }
        
        return true;
    }
}