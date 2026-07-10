package org.ljcode.myPlugin.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.ljcode.myPlugin.MyPlugin;

/**
 * 武器效果事件监听器
 * 监听实体攻击事件，检测玩家是否使用特殊武器攻击生物
 * 并在攻击位置生成相应的效果（如岩浆）
 */
public class WeaponEffectListener implements Listener {
    
    // 插件主类实例，用于访问插件的各种功能和配置
    private final MyPlugin plugin;
    
    // 从配置文件加载的设置项，用于动态调整武器效果参数
    private boolean isFlameBladeEnabled;      // 是否启用火焰刀功能
    private String flameBladeDisplayName;     // 火焰刀的显示名称
    private int magmaDuration;                // 岩浆持续时间（秒）
    private double extraDamage;               // 火焰刀的额外基础伤害
    
    /**
     * 构造函数，初始化武器效果监听器
     * 加载配置文件中的设置
     * 
     * @param plugin 插件主类实例
     */
    public WeaponEffectListener(MyPlugin plugin) {
        this.plugin = plugin;
        loadConfig(); // 初始化时加载配置
    }
    
    /**
     * 从配置文件加载设置
     * 更新武器效果的各项配置参数
     */
    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        
        // 从配置文件读取火焰刀是否启用
        isFlameBladeEnabled = config.getBoolean("special-items.flame-blade.enabled", true);
        
        // 从配置文件读取火焰刀显示名称，并解析颜色代码
        flameBladeDisplayName = ChatColor.translateAlternateColorCodes('&',
            config.getString("special-items.flame-blade.display-name", "&c&l火焰刀"));
        
        // 从配置文件读取岩浆持续时间
        magmaDuration = config.getInt("special-items.flame-blade.magma-duration", 3);
        
        // 从配置文件读取火焰刀额外伤害值
        extraDamage = config.getDouble("special-items.flame-blade.extra-damage", 100.0);
    }
    
    /**
     * 处理实体攻击事件
     * 检测玩家是否使用火焰刀攻击生物，如果是则在攻击位置生成岩浆效果
     * 
     * @param event 实体攻击事件对象
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 检查火焰刀功能是否在配置中启用
        if (!isFlameBladeEnabled) {
            return; // 如果未启用，则直接返回，不进行任何处理
        }
        
        // 获取攻击者和被攻击者实体
        Entity damager = event.getDamager();
        Entity damaged = event.getEntity();
        
        // 检查是否是玩家攻击生物（排除其他实体之间的攻击）
        if (damager instanceof Player && damaged instanceof LivingEntity) {
            Player player = (Player) damager;
            LivingEntity entity = (LivingEntity) damaged;
            
            // 检查玩家是否正在使用主手持有物品
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            
            // 检查物品是否有元数据（包含自定义名称等信息）
            if (itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasDisplayName()) {
                // 获取物品的显示名称
                String displayName = itemInHand.getItemMeta().getDisplayName();
                
                // 检查物品名称是否与火焰刀名称匹配
                if (displayName.equals(flameBladeDisplayName)) {
                    // 获取被攻击实体的位置
                    Location loc = entity.getLocation();
                    
                    // 在被攻击实体脚下放置岩浆
                    Block block = loc.getBlock();
                    Material originalBlock = block.getType(); // 保存原始方块类型以便后续恢复
                    
                    // 将方块设置为岩浆
                    block.setType(Material.LAVA);
                    
                    // 创建延时任务，在配置的时间后恢复原始方块
                    int ticks = magmaDuration * 20; // 每秒20ticks，将秒转换为游戏刻度
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            // 检查方块是否仍然是岩浆（可能已被其他因素改变）
                            if (block.getType() == Material.LAVA) {
                                // 恢复原始方块类型
                                block.setType(originalBlock);
                            }
                        }
                    }.runTaskLater(plugin, ticks); // 在指定的游戏刻度后执行恢复任务
                }
            }
        }
    }
    
    /**
     * 重新加载配置
     * 用于在运行时更新武器效果的配置参数
     */
    public void reloadConfig() {
        plugin.reloadConfig();  // 重新加载配置文件
        loadConfig();           // 解析并应用新的配置值
    }
}