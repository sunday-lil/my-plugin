package org.ljcode.myPlugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;

import java.io.File;
import java.io.IOException;

public class SpawnCommand implements CommandExecutor {
    
    private final MyPlugin plugin;
    private final File dataFile;
    private final FileConfiguration dataConfig;
    
    public SpawnCommand(MyPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "spawn.yml");
        this.dataConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(dataFile);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("esetspawn")) {
            return handleSetSpawn(sender);
        } else {
            return handleSpawn(sender);
        }
    }
    
    private boolean handleSetSpawn(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("essentialsx.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限设置出生点!");
            return true;
        }
        
        dataConfig.set("spawn", player.getLocation());
        
        try {
            dataConfig.save(dataFile);
            player.sendMessage(ChatColor.GREEN + "出生点设置成功!");
        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "保存出生点失败!");
            plugin.getLogger().severe("Could not save spawn data: " + e.getMessage());
        }
        
        return true;
    }
    
    private boolean handleSpawn(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }
        
        Player player = (Player) sender;
        
        Location spawnLocation = dataConfig.getLocation("spawn");
        if (spawnLocation == null) {
            player.sendMessage(ChatColor.RED + "出生点未设置!");
            return true;
        }
        
        // 保存当前位置
        plugin.getTeleportManager().setLastLocation(player, player.getLocation());
        
        // 传送到出生点
        player.teleport(spawnLocation);
        player.sendMessage(ChatColor.GREEN + "已传送到出生点!");
        
        return true;
    }
}