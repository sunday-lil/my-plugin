package org.ljcode.myPlugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ljcode.myPlugin.MyPlugin;
import org.ljcode.myPlugin.managers.ShopManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopGUI {
    
    private final MyPlugin plugin;
    private final ShopManager shopManager;
    private final String GUI_TITLE = "§6§l自助商店";
    private final int GUI_SIZE = 9;
    
    public ShopGUI(MyPlugin plugin) {
        this.plugin = plugin;
        this.shopManager = plugin.getShopManager();
    }
    
    public void openShop(Player player) {
        Inventory inventory = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        
        Map<String, ShopManager.ShopItem> items = shopManager.getAllShopItems();
        
        for (ShopManager.ShopItem item : items.values()) {
            ItemStack displayItem = createDisplayItem(item);
            inventory.setItem(item.getSlot(), displayItem);
        }
        
        player.openInventory(inventory);
    }
    
    private ItemStack createDisplayItem(ShopManager.ShopItem item) {
        ItemStack displayItem = new ItemStack(item.getMaterial());
        ItemMeta meta = displayItem.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6&l" + formatMaterialName(item.getMaterial().name())));
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GREEN + "购买价格: $" + item.getPrice());
            lore.add(ChatColor.YELLOW + "回收价格: $" + item.getSellPrice());
            lore.add("");
            
            if (item.getMaxStock() > 0) {
                int currentStock = shopManager.getCurrentStock(item.getKey());
                lore.add(ChatColor.RED + "库存: " + currentStock + "/" + item.getMaxStock());
                lore.add(ChatColor.GRAY + "(全服限购)");
            } else {
                lore.add(ChatColor.GRAY + "库存: 无限");
            }
            
            lore.add("");
            lore.add(ChatColor.WHITE + "左键: 购买1个");
            lore.add(ChatColor.WHITE + "Shift+左键: 购买一组");
            lore.add(ChatColor.WHITE + "右键: 卖出1个");
            lore.add(ChatColor.WHITE + "Shift+右键: 卖出所有");
            
            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }
        
        return displayItem;
    }
    
    private String formatMaterialName(String name) {
        return name.toLowerCase().replace('_', ' ');
    }
    
    public void updateShopGUI(Player player) {
        if (player.getOpenInventory() != null && 
            player.getOpenInventory().getTitle().equals(GUI_TITLE)) {
            Inventory inventory = player.getOpenInventory().getTopInventory();
            
            Map<String, ShopManager.ShopItem> items = shopManager.getAllShopItems();
            
            for (ShopManager.ShopItem item : items.values()) {
                ItemStack displayItem = createDisplayItem(item);
                inventory.setItem(item.getSlot(), displayItem);
            }
            
            player.updateInventory();
        }
    }
    
    public void updateAllShopGUIs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateShopGUI(player);
        }
    }
    
    public String getGUITitle() {
        return GUI_TITLE;
    }
}