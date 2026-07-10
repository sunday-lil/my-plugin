package org.ljcode.myPlugin.managers;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
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
            plugin.saveResource("warps.yml", false);
        }
        
        if (dataConfig.contains("warps")) {
            for (String warpName : dataConfig.getConfigurationSection("warps").getKeys(false)) {
                Location location = dataConfig.getLocation("warps." + warpName);
                if (location != null) {
                    warps.put(warpName, location);
                }
            }
        }
    }
    
    public void saveData() {
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