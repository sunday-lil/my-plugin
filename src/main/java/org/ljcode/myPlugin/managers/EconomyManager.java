package org.ljcode.myPlugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EconomyManager {
    
    private final MyPlugin plugin;
    private final Map<UUID, Double> balances;
    private final File dataFile;
    private final FileConfiguration dataConfig;
    private int autoSaveTaskId = -1;
    
    public EconomyManager(MyPlugin plugin) {
        this.plugin = plugin;
        this.balances = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "economy.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    public void loadData() {
        if (!dataFile.exists()) {
            plugin.saveResource("economy.yml", false);
        }
        
        if (dataConfig.contains("balances")) {
            for (String key : dataConfig.getConfigurationSection("balances").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                double balance = dataConfig.getDouble("balances." + key);
                balances.put(uuid, balance);
            }
        }
    }
    
    public void saveData() {
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            dataConfig.set("balances." + entry.getKey().toString(), entry.getValue());
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save economy data: " + e.getMessage());
        }
    }
    
    public void startAutoSave() {
        if (autoSaveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autoSaveTaskId);
        }
        
        autoSaveTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            Bukkit.getScheduler().runTask(plugin, this::saveData);
            plugin.getLogger().info("经济数据已自动保存");
        }, 6000L, 6000L).getTaskId();
    }
    
    public void stopAutoSave() {
        if (autoSaveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autoSaveTaskId);
            autoSaveTaskId = -1;
        }
    }
    
    public double getBalance(Player player) {
        return balances.getOrDefault(player.getUniqueId(), 0.0);
    }
    
    public boolean hasBalance(Player player, double amount) {
        return getBalance(player) >= amount;
    }
    
    public boolean withdraw(Player player, double amount) {
        if (!hasBalance(player, amount)) {
            return false;
        }
        
        double currentBalance = getBalance(player);
        balances.put(player.getUniqueId(), currentBalance - amount);
        return true;
    }
    
    public void deposit(Player player, double amount) {
        double currentBalance = getBalance(player);
        balances.put(player.getUniqueId(), currentBalance + amount);
    }
    
    public boolean transfer(Player from, Player to, double amount) {
        if (!hasBalance(from, amount)) {
            return false;
        }
        
        withdraw(from, amount);
        deposit(to, amount);
        return true;
    }
    
    public Map<UUID, Double> getTopBalances(int limit) {
        return balances.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Map::putAll);
    }
    
    public void setBalance(Player player, double amount) {
        balances.put(player.getUniqueId(), amount);
    }
    
    public boolean setBalance(Player target, double amount, Player operator) {
        if (amount < 0) {
            operator.sendMessage(ChatColor.RED + "余额不能为负数!");
            return false;
        }
        
        setBalance(target, amount);
        operator.sendMessage(ChatColor.GREEN + "已将 " + target.getName() + " 的余额设置为 $" + String.format("%.2f", amount));
        target.sendMessage(ChatColor.GREEN + operator.getName() + " 将你的余额设置为 $" + String.format("%.2f", amount));
        return true;
    }
    
    public boolean setBalanceByUUID(UUID uuid, double amount) {
        if (amount < 0) {
            return false;
        }
        balances.put(uuid, amount);
        return true;
    }
    
    public double getBalanceByUUID(UUID uuid) {
        return balances.getOrDefault(uuid, 0.0);
    }
    
    public boolean hasPlayer(UUID uuid) {
        return balances.containsKey(uuid);
    }
    
    public String formatAmount(double amount) {
        return String.format("%.2f", amount);
    }
}