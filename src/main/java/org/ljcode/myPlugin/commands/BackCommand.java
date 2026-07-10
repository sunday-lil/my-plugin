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
        
        var lastLocation = plugin.getTeleportManager().getLastLocation(player);
        if (lastLocation == null) {
            player.sendMessage(ChatColor.RED + "没有可返回的位置!");
            return true;
        }
        
        // 保存当前位置
        plugin.getTeleportManager().setLastLocation(player, player.getLocation());
        
        // 传送到上一个位置
        player.teleport(lastLocation);
        player.sendMessage(ChatColor.GREEN + "已返回上一个位置!");
        
        return true;
    }
}