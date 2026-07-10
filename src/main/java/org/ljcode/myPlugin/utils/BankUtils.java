package org.ljcode.myPlugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class BankUtils {
    
    public static boolean isValidAmount(double amount) {
        return amount > 0 && !Double.isInfinite(amount) && !Double.isNaN(amount);
    }
    
    public static boolean isValidAmount(String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);
            return isValidAmount(amount);
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static String formatAmount(double amount) {
        return String.format("%.2f", amount);
    }
    
    public static void sendInsufficientFundsMessage(Player player, double currentBalance) {
        player.sendMessage(ChatColor.RED + "余额不足! 当前余额: $" + formatAmount(currentBalance));
    }
    
    public static void sendInvalidAmountMessage(Player player) {
        player.sendMessage(ChatColor.RED + "无效的金额! 金额必须为正数");
    }
    
    public static void sendSuccessMessage(Player player, String message) {
        player.sendMessage(ChatColor.GREEN + message);
    }
    
    public static void sendErrorMessage(Player player, String message) {
        player.sendMessage(ChatColor.RED + message);
    }
    
    public static void sendInfoMessage(Player player, String message) {
        player.sendMessage(ChatColor.YELLOW + message);
    }
}