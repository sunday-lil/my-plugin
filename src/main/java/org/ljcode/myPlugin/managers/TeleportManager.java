package org.ljcode.myPlugin.managers;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 传送管理系统
 * 负责管理玩家的传送功能，包括记录最后位置、处理传送请求等
 * 提供安全的玩家间传送机制
 */
public class TeleportManager {
    
    // 插件主类实例，用于访问插件的各种功能
    private final MyPlugin plugin;
    
    // 存储玩家UUID与其最后位置的映射关系，用于/back命令
    private final Map<UUID, Location> lastLocations;
    
    // 存储玩家传送请求的映射表，键是接收请求的玩家UUID
    private final Map<UUID, TeleportRequest> teleportRequests;
    
    // 数据存储文件路径，用于持久化保存传送相关数据
    private final File dataFile;
    
    // YAML配置文件对象，用于读写数据文件
    private final FileConfiguration dataConfig;
    
    /**
     * 构造函数，初始化传送管理器
     * 创建必要的数据结构和文件对象
     * 
     * @param plugin 插件主类实例
     */
    public TeleportManager(MyPlugin plugin) {
        // 保存插件引用以便后续使用
        this.plugin = plugin;
        
        // 初始化最后位置映射表，用于存储玩家的最后位置
        this.lastLocations = new HashMap<>();
        
        // 初始化传送请求映射表，用于存储待处理的传送请求
        this.teleportRequests = new HashMap<>();
        
        // 创建数据文件对象，存储位置在插件数据文件夹中
        this.dataFile = new File(plugin.getDataFolder(), "teleport.yml");
        
        // 加载或创建YAML配置对象
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    /**
     * 从配置文件加载传送相关数据
     * 如果配置文件不存在，则创建默认配置文件
     * 将所有玩家的最后位置数据加载到内存中
     */
    public void loadData() {
        // 检查数据文件是否存在，如果不存在则从资源文件复制默认配置
        if (!dataFile.exists()) {
            plugin.saveResource("teleport.yml", false);
        }
        
        // 检查配置文件中是否包含最后位置数据段
        if (dataConfig.contains("lastLocations")) {
            // 遍历所有玩家UUID键
            for (String playerUUID : dataConfig.getConfigurationSection("lastLocations").getKeys(false)) {
                // 将字符串形式的UUID转换为UUID对象
                UUID uuid = UUID.fromString(playerUUID);
                
                // 从配置中获取对应玩家的最后位置
                Location location = dataConfig.getLocation("lastLocations." + playerUUID);
                
                // 如果位置信息存在，则将其添加到最后位置映射表中
                if (location != null) {
                    lastLocations.put(uuid, location);
                }
            }
        }
    }
    
    /**
     * 将传送相关数据保存到配置文件
     * 仅保存最后位置数据，传送请求不进行持久化存储
     * 如果保存失败则记录错误日志
     */
    public void saveData() {
        // 遍历所有玩家最后位置条目并写入配置对象
        for (Map.Entry<UUID, Location> entry : lastLocations.entrySet()) {
            // 设置配置项：lastLocations.玩家UUID = 位置对象
            dataConfig.set("lastLocations." + entry.getKey().toString(), entry.getValue());
        }
        
        // 尝试将配置数据保存到文件
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            // 如果保存失败，记录严重错误日志
            plugin.getLogger().severe("Could not save teleport data: " + e.getMessage());
        }
    }
    
    /**
     * 设置玩家的最后位置
     * 用于/back命令，让玩家能够返回到上一个位置
     * 
     * @param player 需要设置最后位置的玩家
     * @param location 要设置的位置
     */
    public void setLastLocation(Player player, Location location) {
        // 将玩家UUID和指定位置存储到最后位置映射表中
        lastLocations.put(player.getUniqueId(), location);
    }
    
    /**
     * 获取玩家的最后位置
     * 用于/back命令，获取玩家上一个所在位置
     * 
     * @param player 需要获取最后位置的玩家
     * @return 玩家的最后位置，如果不存在则返回null
     */
    public Location getLastLocation(Player player) {
        // 通过玩家UUID从最后位置映射表中获取位置
        return lastLocations.get(player.getUniqueId());
    }
    
    /**
     * 添加传送请求
     * 创建一个新的传送请求并将其存储到请求映射表中
     * 
     * @param from 发起请求的玩家
     * @param to 接收请求的玩家
     * @param isToPlayer 是否是请求对方传送到自己这里（true为对方来，false为自己去）
     */
    public void addTeleportRequest(Player from, Player to, boolean isToPlayer) {
        // 创建传送请求对象，包含发起者、接收者、请求类型和时间戳
        TeleportRequest request = new TeleportRequest(from, to, isToPlayer, System.currentTimeMillis());
        
        // 将请求存储到映射表中，以接收者的UUID为键
        teleportRequests.put(to.getUniqueId(), request);
    }
    
    /**
     * 获取玩家的传送请求
     * 检查请求是否过期（超过1分钟），如果过期则自动移除
     * 
     * @param player 需要获取传送请求的玩家
     * @return 有效的传送请求对象，如果没有或已过期则返回null
     */
    public TeleportRequest getTeleportRequest(Player player) {
        // 从映射表中获取玩家的传送请求
        TeleportRequest request = teleportRequests.get(player.getUniqueId());
        
        // 检查请求是否存在且是否已过期（超时时间由 config.yml teleport.request-timeout 决定，单位秒）
        long timeoutMs = plugin.getConfig().getInt("teleport.request-timeout", 60) * 1000L;
        if (request != null && System.currentTimeMillis() - request.getTimestamp() > timeoutMs) {
            // 请求已过期，从映射表中移除
            teleportRequests.remove(player.getUniqueId());
            return null;
        }
        
        // 返回有效的请求对象
        return request;
    }
    
    /**
     * 移除玩家的传送请求
     * 通常在请求被接受或拒绝后调用
     * 
     * @param player 需要移除传送请求的玩家
     */
    public void removeTeleportRequest(Player player) {
        // 从映射表中移除以该玩家UUID为键的传送请求
        teleportRequests.remove(player.getUniqueId());
    }
    
    /**
     * 传送请求内部类
     * 封装传送请求的相关信息，包括发起者、接收者、请求类型和时间戳
     */
    public static class TeleportRequest {
        // 发起传送请求的玩家
        private final Player from;
        
        // 接收传送请求的玩家
        private final Player to;
        
        // 请求类型：true表示请求对方传送到自己这里，false表示自己传送到对方那里
        private final boolean isToPlayer;
        
        // 请求创建的时间戳，用于判断请求是否过期
        private final long timestamp;
        
        /**
         * 构造函数，创建一个新的传送请求对象
         * 
         * @param from 发起请求的玩家
         * @param to 接收请求的玩家
         * @param isToPlayer 请求类型（true为对方来，false为自己去）
         * @param timestamp 请求创建的时间戳
         */
        public TeleportRequest(Player from, Player to, boolean isToPlayer, long timestamp) {
            this.from = from;          // 设置请求发起者
            this.to = to;              // 设置请求接收者
            this.isToPlayer = isToPlayer; // 设置请求类型
            this.timestamp = timestamp;  // 设置请求时间戳
        }
        
        /**
         * 获取请求发起者
         * 
         * @return 发起传送请求的玩家
         */
        public Player getFrom() {
            return from;
        }
        
        /**
         * 获取请求接收者
         * 
         * @return 接收传送请求的玩家
         */
        public Player getTo() {
            return to;
        }
        
        /**
         * 获取请求类型
         * 
         * @return true表示请求对方传送到自己这里，false表示自己传送到对方那里
         */
        public boolean isToPlayer() {
            return isToPlayer;
        }
        
        /**
         * 获取请求时间戳
         * 
         * @return 请求创建的时间戳
         */
        public long getTimestamp() {
            return timestamp;
        }
    }
}