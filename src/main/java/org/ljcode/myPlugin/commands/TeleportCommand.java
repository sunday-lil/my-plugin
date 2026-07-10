package org.ljcode.myPlugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;

public class TeleportCommand implements CommandExecutor {
    
    private final MyPlugin plugin;
    
    public TeleportCommand(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "用法: /" + label + " <玩家>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "玩家未找到或不在线!");
            return true;
        }
        
        // 权限检查
        if (player.equals(target)) {
            if (!player.hasPermission("essentialsx.tp.self")) {
                player.sendMessage(ChatColor.RED + "你没有权限传送自己!");
                return true;
            }
        } else {
            if (!player.hasPermission("essentialsx.tp.others")) {
                player.sendMessage(ChatColor.RED + "你没有权限传送其他玩家!");
                return true;
            }
        }
        
        // 保存当前位置
        plugin.getTeleportManager().setLastLocation(player, player.getLocation());
        
        // 传送玩家
        player.teleport(target.getLocation());
        player.sendMessage(ChatColor.GREEN + "已传送到玩家 " + target.getName());
        
        return true;
    }
}