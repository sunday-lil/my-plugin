package org.ljcode.myPlugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ljcode.myPlugin.MyPlugin;

/**
 * Eall66命令处理器
 * 为OP管理员提供一套满附魔的下界合金盔甲，名字为"lucky"
 */
public class Eall66Command implements CommandExecutor {
    
    // 插件主类实例
    private final MyPlugin plugin;
    
    /**
     * 构造函数，初始化Eall66命令处理器
     * 
     * @param plugin 插件主类实例
     */
    public Eall66Command(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 命令执行方法
     * 检查权限并给玩家发放满附魔的下界合金盔甲
     * 
     * @param sender 命令发送者
     * @param command 命令对象
     * @param label 命令标签
     * @param args 命令参数
     * @return 是否执行成功
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 检查命令发送者是否为玩家
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家才能执行此命令！");
            return true;
        }
        
        Player player = (Player) sender;
        
        // 检查玩家是否为OP或拥有管理员权限
        if (!player.isOp() && !player.hasPermission("myplugin.eall66")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
            return true;
        }
        
        // 创建满附魔的下界合金盔甲
        ItemStack helmet = createEnchantedArmor(Material.NETHERITE_HELMET, "lucky");
        ItemStack chestplate = createEnchantedArmor(Material.NETHERITE_CHESTPLATE, "lucky");
        ItemStack leggings = createEnchantedArmor(Material.NETHERITE_LEGGINGS, "lucky");
        ItemStack boots = createEnchantedArmor(Material.NETHERITE_BOOTS, "lucky");
        
        // 给玩家装备盔甲
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
        
        // 发送成功消息
        player.sendMessage(ChatColor.GREEN + "已获得满附魔的下界合金盔甲套装！");
        player.sendMessage(ChatColor.YELLOW + "盔甲名称：" + ChatColor.GOLD + "lucky");
        
        return true;
    }
    
    /**
     * 创建满附魔的盔甲
     * 
     * @param material 盔甲材质
     * @param name 盔甲名称
     * @return 附魔后的盔甲物品
     */
    private ItemStack createEnchantedArmor(Material material, String name) {
        ItemStack armor = new ItemStack(material);
        ItemMeta meta = armor.getItemMeta();
        
        if (meta != null) {
            // 设置盔甲名称
            meta.setDisplayName(ChatColor.GOLD + name);
            
            // 根据盔甲类型添加不同的附魔
            switch (material) {
                case NETHERITE_HELMET:
                    // 头盔附魔
                    meta.addEnchant(Enchantment.PROTECTION, 4, true);
                    meta.addEnchant(Enchantment.FIRE_PROTECTION, 4, true);
                    meta.addEnchant(Enchantment.BLAST_PROTECTION, 4, true);
                    meta.addEnchant(Enchantment.PROJECTILE_PROTECTION, 4, true);
                    meta.addEnchant(Enchantment.RESPIRATION, 3, true);
                    meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
                    meta.addEnchant(Enchantment.THORNS, 3, true);
                    meta.addEnchant(Enchantment.MENDING, 1, true);
                    meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                    break;
                    
                case NETHERITE_CHESTPLATE:
                    // 胸甲附魔
                    meta.addEnchant(Enchantment.PROTECTION, 4, true);
                    meta.addEnchant(Enchantment.FIRE_PROTECTION, 4, true);
                    meta.addEnchant(Enchantment.BLAST_PROTECTION, 4, true);
                    meta.addEnchant(Enchantment.PROJECTILE_PROTECTION, 4, true);
                    meta.addEnchant(Enchantment.THORNS, 3, true);
                    meta.addEnchant(Enchantment.MENDING, 1, true);
                    meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                    break;
                    
                case NETHERITE_LEGGINGS:
                    // 护腿附魔
                    meta.addEnchant(Enchantment.PROTECTION, 4, true);
                    meta.addEnchant(Enchantment.FIRE_PROTECTION, 4, true);
                    meta.addEnchant(Enchantment.BLAST_PROTECTION, 4, true);
                    meta.addEnchant(Enchantment.PROJECTILE_PROTECTION, 4, true);
                    meta.addEnchant(Enchantment.THORNS, 3, true);
                    meta.addEnchant(Enchantment.MENDING, 1, true);
                    meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                    break;
                    
                case NETHERITE_BOOTS:
                    // 靴子附魔
                    meta.addEnchant(Enchantment.PROTECTION, 4, true);
                    meta.addEnchant(Enchantment.FIRE_PROTECTION, 4, true);
                    meta.addEnchant(Enchantment.BLAST_PROTECTION, 4, true);
                    meta.addEnchant(Enchantment.PROJECTILE_PROTECTION, 4, true);
                    meta.addEnchant(Enchantment.FEATHER_FALLING, 4, true);
                    meta.addEnchant(Enchantment.DEPTH_STRIDER, 3, true);
                    meta.addEnchant(Enchantment.SOUL_SPEED, 3, true);
                    meta.addEnchant(Enchantment.THORNS, 3, true);
                    meta.addEnchant(Enchantment.MENDING, 1, true);
                    meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                    break;
            }
            
            armor.setItemMeta(meta);
        }
        
        return armor;
    }
}