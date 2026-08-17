package org.ljcode.myPlugin.listeners;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.ljcode.myPlugin.MyPlugin;
import org.ljcode.myPlugin.managers.TeleportManager;

public class PlayerListener implements Listener, CommandExecutor {
    
    private final MyPlugin plugin;
    
    public PlayerListener(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 给新玩家发放初始资金（金额由 config.yml economy.starting-balance 决定）
        if (!plugin.getEconomyManager().hasPlayer(player.getUniqueId())) {
            double startingBalance = plugin.getConfig().getDouble("economy.starting-balance", 100.0);
            plugin.getEconomyManager().setBalance(player, startingBalance);
            player.sendMessage(ChatColor.GREEN + "欢迎来到服务器! 你获得了 $" + startingBalance + " 初始资金!");
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 清理传送请求
        plugin.getTeleportManager().removeTeleportRequest(player);
    }
    
    // 处理传送请求接受命令
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }
        
        Player player = (Player) sender;
        
        TeleportManager.TeleportRequest request = plugin.getTeleportManager().getTeleportRequest(player);
        if (request == null) {
            player.sendMessage(ChatColor.RED + "没有待处理的传送请求!");
            return true;
        }
        
        if (label.equalsIgnoreCase("etpaccept")) {
            return handleTpAccept(player, request);
        } else if (label.equalsIgnoreCase("etpdeny")) {
            return handleTpDeny(player, request);
        }
        
        return false;
    }
    
    private boolean handleTpAccept(Player player, TeleportManager.TeleportRequest request) {
        Player from = request.getFrom();
        
        if (!from.isOnline()) {
            player.sendMessage(ChatColor.RED + "发送请求的玩家已离线!");
            plugin.getTeleportManager().removeTeleportRequest(player);
            return true;
        }
        
        // 保存当前位置
        plugin.getTeleportManager().setLastLocation(from, from.getLocation());
        
        if (request.isToPlayer()) {
            // tpahere - 请求者传送到接收者
            from.teleport(player.getLocation());
            from.sendMessage(ChatColor.GREEN + player.getName() + " 接受了你的传送请求!");
        } else {
            // tpa - 接收者传送到请求者
            player.teleport(from.getLocation());
            from.sendMessage(ChatColor.GREEN + player.getName() + " 接受了你的传送请求!");
        }
        
        player.sendMessage(ChatColor.GREEN + "已接受传送请求!");
        plugin.getTeleportManager().removeTeleportRequest(player);
        
        return true;
    }
    
    private boolean handleTpDeny(Player player, TeleportManager.TeleportRequest request) {
        Player from = request.getFrom();
        
        if (from.isOnline()) {
            from.sendMessage(ChatColor.RED + player.getName() + " 拒绝了你的传送请求!");
        }
        
        player.sendMessage(ChatColor.YELLOW + "已拒绝传送请求!");
        plugin.getTeleportManager().removeTeleportRequest(player);
        
        return true;
    }
}