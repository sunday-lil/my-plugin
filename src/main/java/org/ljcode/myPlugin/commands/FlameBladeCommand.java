package org.ljcode.myPlugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ljcode.myPlugin.MyPlugin;

import java.util.UUID;

/**
 * 火焰刀命令处理器
 * 为玩家创建一把具有满级附魔和额外伤害的特殊武器
 * 当玩家使用此武器攻击生物时，会在攻击位置生成岩浆
 */
public class FlameBladeCommand implements CommandExecutor {
    
    // 插件主类实例，用于访问插件的各种功能和配置
    private final MyPlugin plugin;
    
    // 从配置文件加载的设置项，用于动态调整火焰刀属性
    private boolean isFlameBladeEnabled;      // 是否启用火焰刀功能
    private String flameBladeDisplayName;     // 火焰刀的显示名称
    private double extraDamage;               // 火焰刀的额外基础伤害
    
    /**
     * 构造函数，初始化火焰刀命令处理器
     * 加载配置文件中的设置
     * 
     * @param plugin 插件主类实例
     */
    public FlameBladeCommand(MyPlugin plugin) {
        this.plugin = plugin;
        loadConfig(); // 初始化时加载配置
    }
    
    /**
     * 从配置文件加载设置
     * 更新火焰刀的各项配置参数
     */
    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        
        // 从配置文件读取火焰刀是否启用
        isFlameBladeEnabled = config.getBoolean("special-items.flame-blade.enabled", true);
        
        // 从配置文件读取火焰刀显示名称，并解析颜色代码
        flameBladeDisplayName = ChatColor.translateAlternateColorCodes('&',
            config.getString("special-items.flame-blade.display-name", "&c&l火焰刀"));
        
        // 从配置文件读取火焰刀额外伤害值
        extraDamage = config.getDouble("special-items.flame-blade.extra-damage", 100.0);
    }
    
    /**
     * 执行火焰刀命令的主要方法
     * 验证命令执行者身份，检查功能是否启用，
     * 创建具有满级附魔和额外伤害的火焰刀并给予玩家
     * 
     * @param sender 命令执行者
     * @param command 执行的命令对象
     * @param label 命令标签
     * @param args 命令参数数组
     * @return 命令执行成功返回true
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 验证命令执行者是否为玩家，只有玩家才能使用此命令
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // 检查功能是否在配置中启用
        if (!isFlameBladeEnabled) {
            player.sendMessage(ChatColor.RED + "火焰刀功能当前未启用！");
            return true;
        }
        
        // 创建一把钻石剑作为火焰刀的基础
        ItemStack flameBlade = new ItemStack(Material.DIAMOND_SWORD);
        
        // 获取物品元数据以便进行自定义设置
        ItemMeta meta = flameBlade.getItemMeta();
        
        // 检查元数据是否有效
        if (meta != null) {
            // 设置火焰刀的显示名称
            meta.setDisplayName(flameBladeDisplayName);
            
            // 遍历所有可用的附魔类型，将兼容的附魔添加到最大等级
            for (Enchantment enchantment : Enchantment.values()) {
                // 检查当前附魔是否可以应用到钻石剑上
                if (enchantment.canEnchantItem(flameBlade)) {
                    // 获取该附魔的最大允许等级
                    int maxLevel = getMaxEnchantLevel(enchantment);
                    // 将附魔添加到物品上，冲突保护设为true
                    meta.addEnchant(enchantment, maxLevel, true);
                }
            }
            
            // 创建属性修饰符以增加基础攻击伤害
            AttributeModifier damageModifier = new AttributeModifier(
                UUID.randomUUID(),              // 随机生成唯一ID
                "generic.attack_damage",        // 属性名称
                extraDamage,                    // 伤害增加值
                AttributeModifier.Operation.ADD_NUMBER  // 操作类型：直接相加
            );
            // 将伤害修饰符添加到物品的攻击伤害属性上
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, damageModifier);
            
            // 添加物品标志以隐藏某些信息显示（如附魔、属性、耐久等）
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);      // 隐藏附魔信息
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);    // 隐藏属性信息
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);   // 隐藏不可破坏信息
            
            // 将修改后的元数据应用回物品
            flameBlade.setItemMeta(meta);
        }
        
        // 将创建好的火焰刀添加到玩家背包中
        player.getInventory().addItem(flameBlade);
        
        // 向玩家发送获得火焰刀的通知消息
        player.sendMessage(ChatColor.GOLD + "你获得了一把" + flameBladeDisplayName + 
                          ChatColor.GOLD + "！攻击生物时会在攻击位置生成岩浆！");
        
        return true;
    }
    
    /**
     * 获取指定附魔的最大等级
     * 根据附魔类型返回Spigot API中该附魔的实际最大等级
     * 某些附魔在原版Minecraft中有固定的最高等级
     * 
     * @param enchantment 要查询的附魔对象
     * @return 该附魔的最大等级
     */
    private int getMaxEnchantLevel(Enchantment enchantment) {
        // 根据附魔类型返回最大等级，这些是Minecraft原版中各附魔的固定最高等级
        switch (enchantment.getKey().getKey().toLowerCase()) {
            // 保护类附魔最高等级为4
            case "protection":
            case "fire_protection":
            case "blast_protection":
            case "projectile_protection":
                return 4;
                
            // 荆棘附魔最高等级为3
            case "thorns":
                return 3;
                
            // 大部分常用附魔最高等级为5
            case "respiration":
            case "depth_strider":
            case "sharpness":
            case "smite":
            case "bane_of_arthropods":
            case "knockback":
            case "fire_aspect":
            case "looting":
            case "efficiency":
            case "silk_touch":
            case "unbreaking":
            case "fortune":
            case "power":
            case "punch":
            case "flame":
            case "infinity":
            case "luck_of_the_sea":
            case "lure":
                return 5;
                
            // 部分特殊附魔最高等级为3
            case "feather_falling":
            case "swift_sneak":
            case "sweeping":
            case "impaling":
            case "loyalty":
            case "riptide":
            case "channeling":
            case "multishot":
            case "quick_charge":
            case "piercing":
            case "mending":
            case "vanishing_curse":
            case "binding_curse":
                return 3;
                
            // 对于未知附魔类型，返回API提供的最大等级
            default:
                return enchantment.getMaxLevel();
        }
    }
    
    /**
     * 重新加载配置
     * 用于在运行时更新火焰刀的配置参数
     */
    public void reloadConfig() {
        plugin.reloadConfig();  // 重新加载配置文件
        loadConfig();           // 解析并应用新的配置值
    }
}