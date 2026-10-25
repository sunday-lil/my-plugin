package org.ljcode.myPlugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class BankUtils {
    
    public static boolean isValidAmount(double amount) {
        return amount > 0 && !Double.isInfinite(amount) && !Double.isNaN(amount);
    }
    
    public static boolean isValidAmount(String amountStr) {
        try {
            return isValidAmount(parseAmount(amountStr));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 解析金额，并拒绝 NaN、Infinity、负数和 0。
     *
     * @throws NumberFormatException 金额非法
     */
    public static double parseAmount(String amountStr) {
        double amount = Double.parseDouble(amountStr);
        if (!isValidAmount(amount)) {
            throw new NumberFormatException("Invalid amount: " + amountStr);
        }
        return amount;
    }

    /** 解析允许为 0 的金额（用于 /eco set），仍然拒绝 NaN/Infinity/负数。 */
    public static double parseNonNegativeAmount(String amountStr) {
        double amount = Double.parseDouble(amountStr);
        if (amount < 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new NumberFormatException("Invalid amount: " + amountStr);
        }
        return amount;
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