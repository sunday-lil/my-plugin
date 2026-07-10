package org.ljcode.myPlugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.ljcode.myPlugin.MyPlugin;
import org.ljcode.myPlugin.commands.MenuCommand;

public class MenuListener implements Listener {
    
    private final MyPlugin plugin;
    
    public MenuListener(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    private MenuCommand getMenuCommand() {
        return plugin.getMenuCommand();
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (!title.contains("超级控制中心")) {
            return;
        }
        
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        int slot = event.getSlot();
        
        if (title.contains("主菜单")) {
            handleMainMenuClick(player, slot, clickedItem);
        } else if (title.contains("传送菜单")) {
            handleTeleportMenuClick(player, slot, clickedItem);
        } else if (title.contains("玩家菜单")) {
            handlePlayerMenuClick(player, slot, clickedItem);
        } else if (title.contains("经济菜单")) {
            handleEconomyMenuClick(player, slot, clickedItem);
        } else if (title.contains("管理员菜单")) {
            handleAdminMenuClick(player, slot, clickedItem);
        }
    }
    
    private void handleMainMenuClick(Player player, int slot, ItemStack clickedItem) {
        MenuCommand menuCommand = getMenuCommand();
        
        if (slot == 13 && clickedItem.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setFireTicks(0);
            player.sendMessage(ChatColor.GREEN + "✓ 你已恢复满状态！");
            player.closeInventory();
        } else if (slot == 11 && clickedItem.getType() == Material.COMPASS) {
            menuCommand.openTeleportMenu(player);
        } else if (slot == 15 && clickedItem.getType() == Material.PLAYER_HEAD) {
            menuCommand.openPlayerMenu(player);
        } else if (slot == 29 && clickedItem.getType() == Material.GOLD_BLOCK) {
            menuCommand.openEconomyMenu(player);
        } else if (slot == 33 && clickedItem.getType() == Material.REDSTONE_BLOCK) {
            if (player.hasPermission("essentialsx.admin")) {
                menuCommand.openAdminMenu(player);
            } else {
                player.sendMessage(ChatColor.RED + "✗ 你没有管理员权限！");
            }
        } else if (slot == 49 && clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
        }
    }
    
    private void handleTeleportMenuClick(Player player, int slot, ItemStack clickedItem) {
        MenuCommand menuCommand = getMenuCommand();
        
        if (slot == 40 && clickedItem.getType() == Material.ARROW) {
            menuCommand.openMainMenu(player);
        } else if (slot == 49 && clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
        } else if (slot == 10 && clickedItem.getType() == Material.ENDER_PEARL) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请使用 /etp <玩家> 传送到指定玩家");
        } else if (slot == 11 && clickedItem.getType() == Material.COMPASS) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "ehome");
        } else if (slot == 12 && clickedItem.getType() == Material.WHITE_BED) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请使用 /esethome [名称] 设置当前位置为家");
        } else if (slot == 13 && clickedItem.getType() == Material.FEATHER) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "espawn");
        } else if (slot == 14 && clickedItem.getType() == Material.BEACON) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请使用 /ewarp <名称> 传送到传送点");
        } else if (slot == 15 && clickedItem.getType() == Material.RED_BED) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "eback");
        } else if (slot == 16 && clickedItem.getType() == Material.OAK_DOOR) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请使用 /esetwarp <名称> 设置当前位置为传送点");
        }
    }
    
    private void handlePlayerMenuClick(Player player, int slot, ItemStack clickedItem) {
        MenuCommand menuCommand = getMenuCommand();
        
        if (slot == 40 && clickedItem.getType() == Material.ARROW) {
            menuCommand.openMainMenu(player);
        } else if (slot == 49 && clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
        } else if (slot == 10 && clickedItem.getType() == Material.ELYTRA) {
            Bukkit.dispatchCommand(player, "efly");
        } else if (slot == 11 && clickedItem.getType() == Material.TOTEM_OF_UNDYING) {
            Bukkit.dispatchCommand(player, "egod");
        } else if (slot == 12 && clickedItem.getType() == Material.GOLDEN_APPLE) {
            Bukkit.dispatchCommand(player, "eheal");
        } else if (slot == 13 && clickedItem.getType() == Material.COOKED_BEEF) {
            Bukkit.dispatchCommand(player, "efeed");
        } else if (slot == 14 && clickedItem.getType() == Material.GRASS_BLOCK) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请使用 /egm <0|1|2|3> [玩家] 切换游戏模式");
        } else if (slot == 15 && clickedItem.getType() == Material.POTION) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请使用 /espeed [玩家] 提升移动速度");
        } else if (slot == 16 && clickedItem.getType() == Material.NETHER_STAR) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请使用 /eclear [玩家] 清除药水效果");
        }
    }
    
    private void handleEconomyMenuClick(Player player, int slot, ItemStack clickedItem) {
        MenuCommand menuCommand = getMenuCommand();
        
        if (slot == 40 && clickedItem.getType() == Material.ARROW) {
            menuCommand.openMainMenu(player);
        } else if (slot == 49 && clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
        } else if (slot == 11 && clickedItem.getType() == Material.GOLD_INGOT) {
            Bukkit.dispatchCommand(player, "emoney");
        } else if (slot == 12 && clickedItem.getType() == Material.EMERALD) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请使用 /epay <玩家> <金额> 转账");
        } else if (slot == 13 && clickedItem.getType() == Material.DIAMOND) {
            Bukkit.dispatchCommand(player, "ebalancetop");
        } else if (slot == 14 && clickedItem.getType() == Material.CHEST) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请使用 /ebank 打开银行界面");
        } else if (slot == 15 && clickedItem.getType() == Material.GOLD_BLOCK) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请使用 /ehistory 查看交易记录");
        }
    }
    
    private void handleAdminMenuClick(Player player, int slot, ItemStack clickedItem) {
        if (!player.hasPermission("essentialsx.admin")) {
            player.sendMessage(ChatColor.RED + "✗ 你没有管理员权限！");
            player.closeInventory();
            return;
        }
        
        MenuCommand menuCommand = getMenuCommand();
        
        if (slot == 40 && clickedItem.getType() == Material.ARROW) {
            menuCommand.openMainMenu(player);
        } else if (slot == 49 && clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
        } else if (slot == 10 && clickedItem.getType() == Material.NETHERITE_SWORD) {
            Bukkit.dispatchCommand(player, "eall66");
        } else if (slot == 11 && clickedItem.getType() == Material.NETHERITE_PICKAXE) {
            Bukkit.dispatchCommand(player, "eall22");
        } else if (slot == 12 && clickedItem.getType() == Material.BLAZE_ROD) {
            Bukkit.dispatchCommand(player, "eflameblade");
        } else if (slot == 13 && clickedItem.getType() == Material.TNT) {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "enuke");
        } else if (slot == 14 && clickedItem.getType() == Material.BARRIER) {
            Bukkit.dispatchCommand(player, "e12503");
        } else if (slot == 15 && clickedItem.getType() == Material.BOOK) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请使用 /ean <消息> 发送公告");
        } else if (slot == 16 && clickedItem.getType() == Material.PAPER) {
            Bukkit.dispatchCommand(player, "eanreload");
        } else if (slot == 19 && clickedItem.getType() == Material.PLAYER_HEAD) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请使用 /ekick <玩家> 踢出玩家");
        } else if (slot == 20 && clickedItem.getType() == Material.GRASS_BLOCK) {
            Bukkit.dispatchCommand(player, "esetspawn");
        } else if (slot == 21 && clickedItem.getType() == Material.BEACON) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请使用 /etime <day|night> 更改时间");
        } else if (slot == 22 && clickedItem.getType() == Material.CLOCK) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请使用 /eweather <clear|rain> 更改天气");
        } else if (slot == 23 && clickedItem.getType() == Material.COMMAND_BLOCK) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请使用 /ecommand <命令> 执行命令");
        } else if (slot == 28 && clickedItem.getType() == Material.REDSTONE) {
            Bukkit.dispatchCommand(player, "einfo");
        } else if (slot == 29 && clickedItem.getType() == Material.DEBUG_STICK) {
            Bukkit.dispatchCommand(player, "edebug");
        } else if (slot == 30 && clickedItem.getType() == Material.ENDER_CHEST) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请使用 /einvsee <玩家> 查看玩家背包");
        } else if (slot == 31 && clickedItem.getType() == Material.NAME_TAG) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请使用 /enick <玩家> <名称> 修改玩家名称");
        } else if (slot == 32 && clickedItem.getType() == Material.LEAD) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "请使用 /etphere <玩家> 传送玩家到你这里");
        }
    }
}