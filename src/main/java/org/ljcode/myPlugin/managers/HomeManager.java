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
 * 家园管理系统
 * 负责管理玩家设置的家园位置，包括设置、删除、获取家园等功能
 * 每个玩家可以拥有多个家园（根据权限而定）
 */
public class HomeManager {
    
    // 插件主类实例，用于访问插件的各种功能
    private final MyPlugin plugin;
    
    // 存储玩家UUID与该玩家所有家园位置的映射关系
    // 外层Map的键是玩家UUID，值是另一个Map，内层Map的键是家园名称，值是位置对象
    private final Map<UUID, Map<String, Location>> playerHomes;
    
    // 数据存储文件路径，用于持久化保存家园数据
    private final File dataFile;
    
    // YAML配置文件对象，用于读写数据文件
    private final FileConfiguration dataConfig;
    
    /**
     * 构造函数，初始化家园管理器
     * 创建必要的数据结构和文件对象
     * 
     * @param plugin 插件主类实例
     */
    public HomeManager(MyPlugin plugin) {
        // 保存插件引用以便后续使用
        this.plugin = plugin;
        
        // 初始化玩家家园映射表，用于内存中快速存取玩家的家园数据
        this.playerHomes = new HashMap<>();
        
        // 创建数据文件对象，存储位置在插件数据文件夹中
        this.dataFile = new File(plugin.getDataFolder(), "homes.yml");
        
        // 加载或创建YAML配置对象
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    /**
     * 从配置文件加载家园数据
     * 如果配置文件不存在，则创建默认配置文件
     * 将所有玩家的家园数据加载到内存中
     */
    public void loadData() {
        // 检查数据文件是否存在，如果不存在则从资源文件复制默认配置
        if (!dataFile.exists()) {
            plugin.saveResource("homes.yml", false);
        }
        
        // 检查配置文件中是否包含家园数据段
        if (dataConfig.contains("homes")) {
            // 遍历所有玩家UUID键
            for (String playerUUID : dataConfig.getConfigurationSection("homes").getKeys(false)) {
                // 将字符串形式的UUID转换为UUID对象
                UUID uuid = UUID.fromString(playerUUID);
                
                // 创建新的家园位置映射表
                Map<String, Location> homes = new HashMap<>();
                
                // 遍历该玩家的所有家园名称
                for (String homeName : dataConfig.getConfigurationSection("homes." + playerUUID).getKeys(false)) {
                    // 从配置中获取对应家园的位置信息
                    Location location = dataConfig.getLocation("homes." + playerUUID + "." + homeName);
                    
                    // 如果位置信息存在，则将其添加到家园映射表中
                    if (location != null) {
                        homes.put(homeName, location);
                    }
                }
                
                // 将玩家UUID和其所有家园数据存入内存映射表
                playerHomes.put(uuid, homes);
            }
        }
    }
    
    /**
     * 将家园数据保存到配置文件
     * 将内存中的所有家园数据写入YAML文件
     * 如果保存失败则记录错误日志
     */
    public void saveData() {
        // 遍历所有玩家及其家园数据
        for (Map.Entry<UUID, Map<String, Location>> playerEntry : playerHomes.entrySet()) {
            // 遍历当前玩家的所有家园
            for (Map.Entry<String, Location> homeEntry : playerEntry.getValue().entrySet()) {
                // 设置配置项：homes.玩家UUID.家园名称 = 位置对象
                dataConfig.set("homes." + playerEntry.getKey().toString() + "." + homeEntry.getKey(), homeEntry.getValue());
            }
        }
        
        // 尝试将配置数据保存到文件
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            // 如果保存失败，记录严重错误日志
            plugin.getLogger().severe("Could not save homes data: " + e.getMessage());
        }
    }
    
    /**
     * 设置玩家的家园位置
     * 检查玩家权限并验证家园数量限制
     * 
     * @param player 需要设置家园的玩家
     * @param homeName 家园名称
     * @return 设置成功返回true，超出限制返回false
     */
    public boolean setHome(Player player, String homeName) {
        // 获取玩家UUID
        UUID uuid = player.getUniqueId();
        
        // 获取或创建玩家的家园映射表
        Map<String, Location> homes = playerHomes.getOrDefault(uuid, new HashMap<>());
        
        // 检查玩家是否有权限设置多个家园（上限由 config.yml homes.* 决定）
        int maxHomes = player.hasPermission("essentialsx.home.multiple")
                ? plugin.getConfig().getInt("homes.max-homes-with-permission", 10)
                : plugin.getConfig().getInt("homes.max-homes", 5);
        
        // 如果玩家尚未拥有此家园名称，但已达到最大家园数量限制，则返回false
        if (homes.size() >= maxHomes && !homes.containsKey(homeName)) {
            return false;
        }
        
        // 将当前玩家位置设置为指定名称的家园
        homes.put(homeName, player.getLocation());
        
        // 更新内存中的玩家家园数据
        playerHomes.put(uuid, homes);
        
        // 返回设置成功
        return true;
    }
    
    /**
     * 获取指定玩家的指定家园位置
     * 
     * @param player 需要获取家园的玩家
     * @param homeName 家园名称
     * @return 家园位置对象，如果不存在则返回null
     */
    public Location getHome(Player player, String homeName) {
        // 获取玩家的家园映射表
        Map<String, Location> homes = playerHomes.get(player.getUniqueId());
        
        // 如果映射表为空，则返回null
        if (homes == null) {
            return null;
        }
        
        // 返回指定名称的家园位置
        return homes.get(homeName);
    }
    
    /**
     * 删除指定玩家的指定家园
     * 
     * @param player 需要删除家园的玩家
     * @param homeName 家园名称
     * @return 删除成功返回true，家园不存在返回false
     */
    public boolean deleteHome(Player player, String homeName) {
        // 获取玩家的家园映射表
        Map<String, Location> homes = playerHomes.get(player.getUniqueId());
        
        // 如果映射表为空或指定家园不存在，则返回false
        if (homes == null || !homes.containsKey(homeName)) {
            return false;
        }
        
        // 从映射表中移除指定家园
        homes.remove(homeName);
        
        // 返回删除成功
        return true;
    }
    
    /**
     * 获取指定玩家的所有家园名称列表
     * 
     * @param player 需要获取家园名称列表的玩家
     * @return 包含所有家园名称的列表
     */
    public List<String> getHomeNames(Player player) {
        // 获取玩家的家园映射表
        Map<String, Location> homes = playerHomes.get(player.getUniqueId());
        
        // 如果映射表为空，则返回空列表
        if (homes == null) {
            return new ArrayList<>();
        }
        
        // 返回包含所有家园名称的新列表
        return new ArrayList<>(homes.keySet());
    }
    
    /**
     * 获取指定玩家的家园数量
     * 
     * @param player 需要获取家园数量的玩家
     * @return 玩家拥有的家园数量
     */
    public int getHomeCount(Player player) {
        // 获取玩家的家园映射表
        Map<String, Location> homes = playerHomes.get(player.getUniqueId());
        
        // 如果映射表为空则返回0，否则返回映射表大小
        return homes == null ? 0 : homes.size();
    }
}