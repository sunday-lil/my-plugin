package org.ljcode.myPlugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;
import org.ljcode.myPlugin.managers.BankManager;

public class BankCommand implements CommandExecutor {
    
    private final MyPlugin plugin;
    private final BankManager bankManager;
    
    public BankCommand(MyPlugin plugin) {
        this.plugin = plugin;
        this.bankManager = new BankManager(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            showHelp(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "balance":
            case "bal":
                handleBalance(player, args);
                break;
            case "deposit":
            case "dep":
                handleDeposit(player, args);
                break;
            case "withdraw":
            case "wd":
                handleWithdraw(player, args);
                break;
            case "transfer":
            case "pay":
                handleTransfer(player, args);
                break;
            case "help":
                showHelp(player);
                break;
            default:
                player.sendMessage(ChatColor.RED + "未知命令! 使用 /bank help 查看帮助");
                break;
        }
        
        return true;
    }
    
    private void handleBalance(Player player, String[] args) {
        Player targetPlayer = player;
        
        if (args.length > 1) {
            if (!player.hasPermission("bank.balance.others")) {
                player.sendMessage(ChatColor.RED + "你没有权限查看其他玩家的余额!");
                return;
            }
            
            targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                player.sendMessage(ChatColor.RED + "玩家未找到或不在线!");
                return;
            }
        }
        
        bankManager.showBalance(player, targetPlayer);
    }
    
    private void handleDeposit(Player player, String[] args) {
        if (!player.hasPermission("bank.deposit")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用存款功能!");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /bank deposit <金额>");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "无效的金额! 金额必须为正数");
            return;
        }
        
        bankManager.deposit(player, amount);
    }
    
    private void handleWithdraw(Player player, String[] args) {
        if (!player.hasPermission("bank.withdraw")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用取款功能!");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /bank withdraw <金额>");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "无效的金额! 金额必须为正数");
            return;
        }
        
        bankManager.withdraw(player, amount);
    }
    
    private void handleTransfer(Player player, String[] args) {
        if (!player.hasPermission("bank.transfer")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用转账功能!");
            return;
        }
        
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "用法: /bank transfer <玩家> <金额>");
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "玩家未找到或不在线!");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
            if (amount <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "无效的金额! 金额必须为正数");
            return;
        }
        
        bankManager.transfer(player, targetPlayer, amount);
    }
    
    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== 银行系统帮助 ===");
        player.sendMessage(ChatColor.YELLOW + "/bank balance [玩家] - 查看余额");
        player.sendMessage(ChatColor.YELLOW + "/bank deposit <金额> - 存款");
        player.sendMessage(ChatColor.YELLOW + "/bank withdraw <金额> - 取款");
        player.sendMessage(ChatColor.YELLOW + "/bank transfer <玩家> <金额> - 转账");
        player.sendMessage(ChatColor.YELLOW + "/bank help - 显示此帮助信息");
        player.sendMessage(ChatColor.GOLD + "==================");
    }
}