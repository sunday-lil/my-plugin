package org.ljcode.myPlugin.managers;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 银行系统管理器
 * 独立于钱包的经济子系统：
 * - 银行账户余额持久化到 bank.yml
 * - 存款 = 钱包 -> 银行（需要钱包有足够资金）
 * - 取款 = 银行 -> 钱包
 * - 转账 = 银行账户之间划转，可配置手续费
 */
public class BankManager {

    private final MyPlugin plugin;
    private final EconomyManager economyManager;
    private final Map<UUID, Double> accounts;
    private final File dataFile;
    private YamlConfiguration dataConfig;

    public BankManager(MyPlugin plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.accounts = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "bank.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    /**
     * 从 bank.yml 加载银行账户数据
     */
    public void loadData() {
        if (dataConfig.contains("accounts")) {
            for (String key : dataConfig.getConfigurationSection("accounts").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    accounts.put(uuid, dataConfig.getDouble("accounts." + key));
                } catch (IllegalArgumentException ignored) {
                    // 跳过损坏的 UUID 键
                }
            }
        }
        plugin.getLogger().info("银行数据已加载: " + accounts.size() + " 个账户");
    }

    /**
     * 保存银行账户数据到 bank.yml
     */
    public void saveData() {
        for (Map.Entry<UUID, Double> entry : accounts.entrySet()) {
            dataConfig.set("accounts." + entry.getKey().toString(), entry.getValue());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存银行数据: " + e.getMessage());
        }
    }

    /**
     * 获取玩家银行余额（首次访问自动开设账户并获得初始余额）
     */
    public double getBalance(Player player) {
        return accounts.computeIfAbsent(player.getUniqueId(),
                k -> plugin.getConfig().getDouble("bank.initial-balance", 1000.0));
    }

    /**
     * 存款：钱包 -> 银行
     * @return 操作是否成功
     */
    public boolean deposit(Player player, double amount) {
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "存款金额必须大于0!");
            return false;
        }

        double minDeposit = plugin.getConfig().getDouble("bank.min-deposit", 1.0);
        double maxDeposit = plugin.getConfig().getDouble("bank.max-deposit", 1000000.0);
        if (amount < minDeposit || amount > maxDeposit) {
            player.sendMessage(ChatColor.RED + "存款金额必须在 " + formatAmount(minDeposit)
                    + " ~ " + formatAmount(maxDeposit) + " 之间!");
            return false;
        }

        // 资金必须从钱包扣除，不能凭空生成
        if (!economyManager.hasBalance(player, amount)) {
            player.sendMessage(ChatColor.RED + "钱包余额不足! 当前钱包: $"
                    + formatAmount(economyManager.getBalance(player)));
            return false;
        }

        economyManager.withdraw(player, amount);
        accounts.put(player.getUniqueId(), getBalance(player) + amount);
        recordCityTransaction(amount);
        player.sendMessage(ChatColor.GREEN + "成功存入 $" + formatAmount(amount)
                + " (钱包 $" + formatAmount(economyManager.getBalance(player))
                + " | 银行 $" + formatAmount(getBalance(player)) + ")");
        return true;
    }

    /**
     * 取款：银行 -> 钱包
     * @return 操作是否成功
     */
    public boolean withdraw(Player player, double amount) {
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "取款金额必须大于0!");
            return false;
        }

        if (getBalance(player) < amount) {
            player.sendMessage(ChatColor.RED + "银行余额不足! 当前银行余额: $" + formatAmount(getBalance(player)));
            return false;
        }

        accounts.put(player.getUniqueId(), getBalance(player) - amount);
        economyManager.deposit(player, amount);
        recordCityTransaction(amount);
        player.sendMessage(ChatColor.GREEN + "成功取出 $" + formatAmount(amount)
                + " (钱包 $" + formatAmount(economyManager.getBalance(player))
                + " | 银行 $" + formatAmount(getBalance(player)) + ")");
        return true;
    }

    /**
     * 转账：银行账户之间划转，收取可配置的手续费
     * @return 操作是否成功
     */
    public boolean transfer(Player from, Player to, double amount) {
        if (amount <= 0) {
            from.sendMessage(ChatColor.RED + "转账金额必须大于0!");
            return false;
        }

        if (from.equals(to)) {
            from.sendMessage(ChatColor.RED + "不能向自己转账!");
            return false;
        }

        double fee = plugin.getConfig().getDouble("bank.transfer-fee", 0.0);
        double totalCost = amount + fee;

        if (getBalance(from) < totalCost) {
            from.sendMessage(ChatColor.RED + "银行余额不足! 需要 $" + formatAmount(totalCost)
                    + " (含手续费 $" + formatAmount(fee) + "), 当前银行余额: $" + formatAmount(getBalance(from)));
            return false;
        }

        accounts.put(from.getUniqueId(), getBalance(from) - totalCost);
        accounts.put(to.getUniqueId(), getBalance(to) + amount);
        recordCityTransaction(amount);

        from.sendMessage(ChatColor.GREEN + "成功向 " + to.getName() + " 转账 $" + formatAmount(amount)
                + (fee > 0 ? " (手续费 $" + formatAmount(fee) + ")" : ""));
        to.sendMessage(ChatColor.GREEN + from.getName() + " 向你的银行账户转入 $" + formatAmount(amount));
        return true;
    }

    /**
     * 显示玩家自己的银行余额
     */
    public void showBalance(Player player) {
        player.sendMessage(ChatColor.GREEN + "你的银行余额: $" + formatAmount(getBalance(player))
                + ChatColor.GRAY + " (钱包: $" + formatAmount(economyManager.getBalance(player)) + ")");
    }

    /**
     * 显示目标玩家的银行余额
     */
    public void showBalance(Player player, Player target) {
        if (player.equals(target)) {
            showBalance(player);
        } else {
            player.sendMessage(ChatColor.GREEN + target.getName() + " 的银行余额: $" + formatAmount(getBalance(target)));
        }
    }

    /**
     * 获取活跃银行账户数（余额大于0的账户），供数字城市统计使用
     */
    public int getActiveAccountCount() {
        return (int) accounts.values().stream().filter(b -> b > 0).count();
    }

    /**
     * 向数字城市系统上报经济交易（城市系统未启用时静默跳过）
     */
    private void recordCityTransaction(double amount) {
        DigitalCityManager cityManager = DigitalCityManager.getInstance();
        if (cityManager != null) {
            cityManager.recordEconomyTransaction(amount);
        }
    }

    private String formatAmount(double amount) {
        return String.format("%.2f", amount);
    }
}
