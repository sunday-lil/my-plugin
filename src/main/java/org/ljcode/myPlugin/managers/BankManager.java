package org.ljcode.myPlugin.managers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BankManager {
    
    private final MyPlugin plugin;
    private final EconomyManager economyManager;
    
    public BankManager(MyPlugin plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
    }
    
    public double getBalance(Player player) {
        return economyManager.getBalance(player);
    }
    
    public boolean deposit(Player player, double amount) {
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "存款金额必须大于0!");
            return false;
        }
        
        economyManager.deposit(player, amount);
        player.sendMessage(ChatColor.GREEN + "成功存入 $" + formatAmount(amount));
        return true;
    }
    
    public boolean withdraw(Player player, double amount) {
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "取款金额必须大于0!");
            return false;
        }
        
        if (!economyManager.hasBalance(player, amount)) {
            player.sendMessage(ChatColor.RED + "余额不足! 当前余额: $" + formatAmount(getBalance(player)));
            return false;
        }
        
        economyManager.withdraw(player, amount);
        player.sendMessage(ChatColor.GREEN + "成功取出 $" + formatAmount(amount));
        return true;
    }
    
    public boolean transfer(Player from, Player to, double amount) {
        if (amount <= 0) {
            from.sendMessage(ChatColor.RED + "转账金额必须大于0!");
            return false;
        }
        
        if (from.equals(to)) {
            from.sendMessage(ChatColor.RED + "不能向自己转账!");
            return false;
        }
        
        if (!economyManager.hasBalance(from, amount)) {
            from.sendMessage(ChatColor.RED + "余额不足! 当前余额: $" + formatAmount(getBalance(from)));
            return false;
        }
        
        economyManager.transfer(from, to, amount);
        from.sendMessage(ChatColor.GREEN + "成功向 " + to.getName() + " 转账 $" + formatAmount(amount));
        to.sendMessage(ChatColor.GREEN + from.getName() + " 向你转账 $" + formatAmount(amount));
        return true;
    }
    
    public void setBalance(Player player, double amount) {
        if (amount < 0) {
            player.sendMessage(ChatColor.RED + "余额不能为负数!");
            return;
        }
        
        economyManager.setBalance(player, amount);
        player.sendMessage(ChatColor.GREEN + "余额已设置为 $" + formatAmount(amount));
    }
    
    public void showBalance(Player player) {
        double balance = getBalance(player);
        player.sendMessage(ChatColor.GREEN + "你的余额: $" + formatAmount(balance));
    }
    
    public void showBalance(Player player, Player target) {
        double balance = getBalance(target);
        if (player.equals(target)) {
            player.sendMessage(ChatColor.GREEN + "你的余额: $" + formatAmount(balance));
        } else {
            player.sendMessage(ChatColor.GREEN + target.getName() + " 的余额: $" + formatAmount(balance));
        }
    }
    
    private String formatAmount(double amount) {
        return String.format("%.2f", amount);
    }
    
    public Map<UUID, Double> getTopBalances(int limit) {
        return economyManager.getTopBalances(limit);
    }
}