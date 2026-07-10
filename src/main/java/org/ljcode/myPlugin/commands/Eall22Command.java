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
 * Eall22命令处理器
 * 为OP管理员提供一套满附魔的下界合金工具，名字为"lucky"
 */
public class Eall22Command implements CommandExecutor {
    
    // 插件主类实例
    private final MyPlugin plugin;
    
    /**
     * 构造函数，初始化Eall22命令处理器
     * 
     * @param plugin 插件主类实例
     */
    public Eall22Command(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 命令执行方法
     * 检查权限并给玩家发放满附魔的下界合金工具
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
        if (!player.isOp() && !player.hasPermission("myplugin.eall22")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
            return true;
        }
        
        // 创建满附魔的下界合金工具
        ItemStack sword = createEnchantedTool(Material.NETHERITE_SWORD, "lucky");
        ItemStack pickaxe = createEnchantedTool(Material.NETHERITE_PICKAXE, "lucky");
        ItemStack axe = createEnchantedTool(Material.NETHERITE_AXE, "lucky");
        ItemStack shovel = createEnchantedTool(Material.NETHERITE_SHOVEL, "lucky");
        ItemStack hoe = createEnchantedTool(Material.NETHERITE_HOE, "lucky");
        
        // 给玩家发放工具
        player.getInventory().addItem(sword, pickaxe, axe, shovel, hoe);
        
        // 发送成功消息
        player.sendMessage(ChatColor.GREEN + "已获得满附魔的下界合金工具套装！");
        player.sendMessage(ChatColor.YELLOW + "工具名称：" + ChatColor.GOLD + "lucky");
        
        return true;
    }
    
    /**
     * 创建满附魔的工具
     * 
     * @param material 工具材质
     * @param name 工具名称
     * @return 附魔后的工具物品
     */
    private ItemStack createEnchantedTool(Material material, String name) {
        ItemStack tool = new ItemStack(material);
        ItemMeta meta = tool.getItemMeta();
        
        if (meta != null) {
            // 设置工具名称
            meta.setDisplayName(ChatColor.GOLD + name);
            
            // 根据工具类型添加不同的附魔
            switch (material) {
                case NETHERITE_SWORD:
                    // 剑的附魔
                    meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                    meta.addEnchant(Enchantment.SMITE, 5, true);
                    meta.addEnchant(Enchantment.BANE_OF_ARTHROPODS, 5, true);
                    meta.addEnchant(Enchantment.KNOCKBACK, 2, true);
                    meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
                    meta.addEnchant(Enchantment.LOOTING, 3, true);
                    meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
                    meta.addEnchant(Enchantment.MENDING, 1, true);
                    meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                    break;
                    
                case NETHERITE_PICKAXE:
                    // 镐的附魔
                    meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
                    meta.addEnchant(Enchantment.FORTUNE, 3, true);
                    meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
                    meta.addEnchant(Enchantment.MENDING, 1, true);
                    meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                    break;
                    
                case NETHERITE_AXE:
                    // 斧的附魔
                    meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                    meta.addEnchant(Enchantment.SMITE, 5, true);
                    meta.addEnchant(Enchantment.BANE_OF_ARTHROPODS, 5, true);
                    meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
                    meta.addEnchant(Enchantment.FORTUNE, 3, true);
                    meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
                    meta.addEnchant(Enchantment.MENDING, 1, true);
                    meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                    break;
                    
                case NETHERITE_SHOVEL:
                    // 锹的附魔
                    meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
                    meta.addEnchant(Enchantment.FORTUNE, 3, true);
                    meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
                    meta.addEnchant(Enchantment.MENDING, 1, true);
                    meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                    break;
                    
                case NETHERITE_HOE:
                    // 锄的附魔
                    meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
                    meta.addEnchant(Enchantment.FORTUNE, 3, true);
                    meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
                    meta.addEnchant(Enchantment.MENDING, 1, true);
                    meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                    break;
            }
            
            tool.setItemMeta(meta);
        }
        
        return tool;
    }
}