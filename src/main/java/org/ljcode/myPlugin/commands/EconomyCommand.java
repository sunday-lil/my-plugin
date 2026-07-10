package org.ljcode.myPlugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;
import org.ljcode.myPlugin.managers.BankManager;
import org.ljcode.myPlugin.managers.EconomyManager;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;

public class EconomyCommand implements CommandExecutor {
    
    private final MyPlugin plugin;
    private final EconomyManager economyManager;
    private final DecimalFormat decimalFormat;
    
    public EconomyCommand(MyPlugin plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.decimalFormat = new DecimalFormat("#,##0.00");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cleanLabel = label.toLowerCase().replaceFirst("^e", "");
        
        switch (cleanLabel) {
            case "money":
            case "balance":
                return handleMoney(sender, args);
            case "pay":
                return handlePay(sender, args);
            case "balancetop":
            case "baltop":
                return handleBalanceTop(sender);
            case "eco":
                return handleEco(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "未知命令!");
                return true;
        }
    }
    
    private boolean handleMoney(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }
        
        Player player = (Player) sender;
        Player targetPlayer = player;
        
        if (args.length > 0) {
            if (!player.hasPermission("essentialsx.money.see.others")) {
                player.sendMessage(ChatColor.RED + "你没有权限查看其他玩家的余额!");
                return true;
            }
            
            targetPlayer = Bukkit.getPlayer(args[0]);
            if (targetPlayer == null) {
                player.sendMessage(ChatColor.RED + "玩家未找到或不在线!");
                return true;
            }
        }
        
        double balance = economyManager.getBalance(targetPlayer);
        String formattedBalance = economyManager.formatAmount(balance);
        
        if (player.equals(targetPlayer)) {
            player.sendMessage(ChatColor.GREEN + "你的余额: $" + formattedBalance);
        } else {
            player.sendMessage(ChatColor.GREEN + targetPlayer.getName() + " 的余额: $" + formattedBalance);
        }
        return true;
    }
    
    private boolean handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /pay <玩家> <金额>");
            return true;
        }
        
        if (!player.hasPermission("essentialsx.money.pay")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用支付功能!");
            return true;
        }
        
        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "玩家未找到或不在线!");
            return true;
        }
        
        if (player.equals(targetPlayer)) {
            player.sendMessage(ChatColor.RED + "不能向自己转账!");
            return true;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "无效的金额! 金额必须为正数!");
            return true;
        }
        
        if (!economyManager.hasBalance(player, amount)) {
            player.sendMessage(ChatColor.RED + "余额不足! 当前余额: $" + economyManager.formatAmount(economyManager.getBalance(player)));
            return true;
        }
        
        boolean success = economyManager.transfer(player, targetPlayer, amount);
        if (success) {
            player.sendMessage(ChatColor.GREEN + "成功向 " + ChatColor.YELLOW + targetPlayer.getName() + ChatColor.GREEN + " 转账 $" + ChatColor.YELLOW + economyManager.formatAmount(amount));
            targetPlayer.sendMessage(ChatColor.GREEN + player.getName() + " 向你转账 $" + ChatColor.YELLOW + economyManager.formatAmount(amount));
        } else {
            player.sendMessage(ChatColor.RED + "转账失败!");
        }
        return true;
    }
    
    private boolean handleBalanceTop(CommandSender sender) {
        if (!sender.hasPermission("essentialsx.money.baltop")) {
            sender.sendMessage(ChatColor.RED + "你没有权限查看财富榜!");
            return true;
        }
        
        Map<UUID, Double> topBalances = economyManager.getTopBalances(10);
        
        sender.sendMessage(ChatColor.GOLD + "=== 财富榜 Top 10 ===");
        
        if (topBalances.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "暂无数据");
        } else {
            int rank = 1;
            for (Map.Entry<UUID, Double> entry : topBalances.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                String playerName = player != null ? player.getName() : "未知玩家";
                
                sender.sendMessage(ChatColor.GREEN + String.valueOf(rank) + ". " + playerName + " - $" + decimalFormat.format(entry.getValue()));
                rank++;
            }
        }
        
        return true;
    }
    
    private boolean handleEco(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "用法: /eco set <玩家> <金额>");
            return true;
        }
        
        if (!sender.hasPermission("essentialsx.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("set")) {
            return handleEcoSet(sender, args);
        }
        
        sender.sendMessage(ChatColor.RED + "未知命令! 用法: /eco set <玩家> <金额>");
        return true;
    }
    
    private boolean handleEcoSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "用法: /eco set <玩家> <金额>");
            return true;
        }
        
        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "玩家未找到或不在线!");
            return true;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
            if (amount < 0) {
                sender.sendMessage(ChatColor.RED + "金额不能为负数!");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "无效的金额!");
            return true;
        }
        
        economyManager.setBalance(targetPlayer, amount);
        sender.sendMessage(ChatColor.GREEN + "已将 " + targetPlayer.getName() + " 的余额设置为 $" + economyManager.formatAmount(amount));
        targetPlayer.sendMessage(ChatColor.GREEN + sender.getName() + " 将你的余额设置为 $" + economyManager.formatAmount(amount));
        return true;
    }
}