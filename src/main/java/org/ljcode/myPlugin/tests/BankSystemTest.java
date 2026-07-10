package org.ljcode.myPlugin.tests;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;
import org.ljcode.myPlugin.managers.BankManager;

public class BankSystemTest {
    
    private final MyPlugin plugin;
    private final BankManager bankManager;
    
    public BankSystemTest(MyPlugin plugin) {
        this.plugin = plugin;
        this.bankManager = new BankManager(plugin);
    }
    
    public void runAllTests(Player admin) {
        admin.sendMessage(ChatColor.GOLD + "=== 开始银行系统测试 ===");
        
        testInitialBalance(admin);
        testDeposit(admin);
        testWithdraw(admin);
        testTransfer(admin);
        testInvalidAmounts(admin);
        testInsufficientFunds(admin);
        
        admin.sendMessage(ChatColor.GOLD + "=== 银行系统测试完成 ===");
    }
    
    private void testInitialBalance(Player admin) {
        admin.sendMessage(ChatColor.YELLOW + "测试1: 检查初始余额...");
        double balance = bankManager.getBalance(admin);
        if (balance >= 1000.0) {
            admin.sendMessage(ChatColor.GREEN + "✓ 初始余额测试通过: $" + balance);
        } else {
            admin.sendMessage(ChatColor.RED + "✗ 初始余额测试失败: $" + balance);
        }
    }
    
    private void testDeposit(Player admin) {
        admin.sendMessage(ChatColor.YELLOW + "测试2: 测试存款功能...");
        double beforeBalance = bankManager.getBalance(admin);
        boolean success = bankManager.deposit(admin, 500.0);
        double afterBalance = bankManager.getBalance(admin);
        
        if (success && afterBalance == beforeBalance + 500.0) {
            admin.sendMessage(ChatColor.GREEN + "✓ 存款测试通过: 余额从 $" + beforeBalance + " 增加到 $" + afterBalance);
        } else {
            admin.sendMessage(ChatColor.RED + "✗ 存款测试失败");
        }
    }
    
    private void testWithdraw(Player admin) {
        admin.sendMessage(ChatColor.YELLOW + "测试3: 测试取款功能...");
        double beforeBalance = bankManager.getBalance(admin);
        boolean success = bankManager.withdraw(admin, 200.0);
        double afterBalance = bankManager.getBalance(admin);
        
        if (success && afterBalance == beforeBalance - 200.0) {
            admin.sendMessage(ChatColor.GREEN + "✓ 取款测试通过: 余额从 $" + beforeBalance + " 减少到 $" + afterBalance);
        } else {
            admin.sendMessage(ChatColor.RED + "✗ 取款测试失败");
        }
    }
    
    private void testTransfer(Player admin) {
        admin.sendMessage(ChatColor.YELLOW + "测试4: 测试转账功能...");
        Player testPlayer = findTestPlayer();
        
        if (testPlayer == null) {
            admin.sendMessage(ChatColor.RED + "✗ 转账测试失败: 没有找到测试玩家");
            return;
        }
        
        double adminBefore = bankManager.getBalance(admin);
        double playerBefore = bankManager.getBalance(testPlayer);
        
        boolean success = bankManager.transfer(admin, testPlayer, 100.0);
        
        double adminAfter = bankManager.getBalance(admin);
        double playerAfter = bankManager.getBalance(testPlayer);
        
        if (success && adminAfter == adminBefore - 100.0 && playerAfter == playerBefore + 100.0) {
            admin.sendMessage(ChatColor.GREEN + "✓ 转账测试通过: 成功转账 $100 给 " + testPlayer.getName());
        } else {
            admin.sendMessage(ChatColor.RED + "✗ 转账测试失败");
        }
    }
    
    private void testInvalidAmounts(Player admin) {
        admin.sendMessage(ChatColor.YELLOW + "测试5: 测试无效金额处理...");
        
        boolean negativeTest = !bankManager.deposit(admin, -100.0);
        boolean zeroTest = !bankManager.deposit(admin, 0.0);
        
        if (negativeTest && zeroTest) {
            admin.sendMessage(ChatColor.GREEN + "✓ 无效金额测试通过: 正确拒绝负数和零金额");
        } else {
            admin.sendMessage(ChatColor.RED + "✗ 无效金额测试失败");
        }
    }
    
    private void testInsufficientFunds(Player admin) {
        admin.sendMessage(ChatColor.YELLOW + "测试6: 测试余额不足处理...");
        
        double currentBalance = bankManager.getBalance(admin);
        boolean success = bankManager.withdraw(admin, currentBalance + 1000000.0);
        
        if (!success) {
            admin.sendMessage(ChatColor.GREEN + "✓ 余额不足测试通过: 正确拒绝超额取款");
        } else {
            admin.sendMessage(ChatColor.RED + "✗ 余额不足测试失败");
        }
    }
    
    private Player findTestPlayer() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.equals(plugin.getServer().getPlayer("admin"))) {
                return player;
            }
        }
        return null;
    }
}