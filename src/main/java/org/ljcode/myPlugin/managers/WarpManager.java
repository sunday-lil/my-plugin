package org.ljcode.myPlugin.managers;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.ljcode.myPlugin.MyPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WarpManager {
    
    private final MyPlugin plugin;
    private final Map<String, Location> warps;
    private final File dataFile;
    private final FileConfiguration dataConfig;
    
    public WarpManager(MyPlugin plugin) {
        this.plugin = plugin;
        this.warps = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "warps.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    public void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create warps data file: " + e.getMessage());
            }
        }
        
        if (dataConfig.isConfigurationSection("warps")) {
            for (String warpName : dataConfig.getConfigurationSection("warps").getKeys(false)) {
                Location location = dataConfig.getLocation("warps." + warpName);
                if (location != null) {
                    warps.put(warpName, location);
                }
            }
        }
    }
    
    public void saveData() {
        // 先清空旧数据，确保删除的传送点不会在重启后复活
        dataConfig.set("warps", null);
        for (Map.Entry<String, Location> entry : warps.entrySet()) {
            dataConfig.set("warps." + entry.getKey(), entry.getValue());
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save warps data: " + e.getMessage());
        }
    }
    
    public boolean setWarp(String warpName, Location location) {
        // 传送点名会写入 YAML 路径，点号会把名称拆成嵌套节点
        if (warpName == null || warpName.isBlank() || warpName.contains(".")) {
            return false;
        }
        if (warps.containsKey(warpName)) {
            return false;
        }
        
        warps.put(warpName, location);
        return true;
    }
    
    public boolean deleteWarp(String warpName) {
        if (!warps.containsKey(warpName)) {
            return false;
        }
        
        warps.remove(warpName);
        return true;
    }
    
    public Location getWarp(String warpName) {
        return warps.get(warpName);
    }
    
    public List<String> getWarpNames() {
        return new ArrayList<>(warps.keySet());
    }
    
    public boolean warpExists(String warpName) {
        return warps.containsKey(warpName);
    }
}