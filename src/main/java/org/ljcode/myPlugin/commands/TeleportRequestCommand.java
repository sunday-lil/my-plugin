package org.ljcode.myPlugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;
import org.ljcode.myPlugin.managers.TeleportManager;

public class TeleportRequestCommand implements CommandExecutor {
    
    private final MyPlugin plugin;
    
    public TeleportRequestCommand(MyPlugin plugin) {
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
        
        if (player.equals(target)) {
            player.sendMessage(ChatColor.RED + "不能向自己发送传送请求!");
            return true;
        }
        
        boolean isToPlayer = label.equalsIgnoreCase("etpahere");
        
        // 发送传送请求
        plugin.getTeleportManager().addTeleportRequest(player, target, isToPlayer);
        
        String requestType = isToPlayer ? "传送到你这里" : "传送到他那里";
        player.sendMessage(ChatColor.GREEN + "已向 " + target.getName() + " 发送传送请求 (" + requestType + ")");
        target.sendMessage(ChatColor.YELLOW + player.getName() + " 请求传送到你这里!");
        target.sendMessage(ChatColor.YELLOW + "输入 /tpaccept 接受 或 /tpdeny 拒绝");
        
        return true;
    }
}