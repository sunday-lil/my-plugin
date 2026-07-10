package org.ljcode.myPlugin.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.ljcode.myPlugin.MyPlugin;
import org.ljcode.myPlugin.gui.ShopGUI;
import org.ljcode.myPlugin.managers.ShopManager;

public class ShopListener implements Listener {
    
    private final MyPlugin plugin;
    private final ShopGUI shopGUI;
    private final ShopManager shopManager;
    
    public ShopListener(MyPlugin plugin, ShopGUI shopGUI) {
        this.plugin = plugin;
        this.shopGUI = shopGUI;
        this.shopManager = plugin.getShopManager();
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();
        
        if (inventory == null || !event.getView().getTitle().equals(shopGUI.getGUITitle())) {
            return;
        }
        
        event.setCancelled(true);
        
        int slot = event.getSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        
        ItemStack clickedItem = inventory.getItem(slot);
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        ShopManager.ShopItem shopItem = findShopItem(clickedItem.getType());
        if (shopItem == null) {
            return;
        }
        
        boolean isShiftClick = event.isShiftClick();
        boolean isLeftClick = event.isLeftClick();
        boolean isRightClick = event.isRightClick();
        
        if (isLeftClick && !isShiftClick) {
            handleBuy(player, shopItem, 1);
        } else if (isLeftClick && isShiftClick) {
            handleBuy(player, shopItem, 64);
        } else if (isRightClick && !isShiftClick) {
            handleSell(player, shopItem, 1);
        } else if (isRightClick && isShiftClick) {
            handleSellAll(player, shopItem);
        }
        
        shopGUI.updateShopGUI(player);
    }
    
    private ShopManager.ShopItem findShopItem(Material material) {
        for (ShopManager.ShopItem item : shopManager.getAllShopItems().values()) {
            if (item.getMaterial() == material) {
                return item;
            }
        }
        return null;
    }
    
    private void handleBuy(Player player, ShopManager.ShopItem item, int amount) {
        double totalPrice = item.getPrice() * amount;
        
        if (item.getMaxStock() > 0) {
            int currentStock = shopManager.getCurrentStock(item.getKey());
            if (currentStock < amount) {
                player.sendMessage(ChatColor.RED + "库存不足! 剩余库存: " + currentStock);
                return;
            }
        }
        
        if (!plugin.getEconomyManager().hasBalance(player, totalPrice)) {
            player.sendMessage(ChatColor.RED + "余额不足! 需要 $" + totalPrice + ", 当前余额: $" + 
                plugin.getEconomyManager().getBalance(player));
            return;
        }
        
        ItemStack itemToGive = item.getItemStack(amount);
        if (!hasInventorySpace(player, itemToGive)) {
            player.sendMessage(ChatColor.RED + "背包空间不足!");
            return;
        }
        
        plugin.getEconomyManager().withdraw(player, totalPrice);
        
        if (item.getMaxStock() > 0) {
            shopManager.decreaseStock(item.getKey(), amount);
        }
        
        player.getInventory().addItem(itemToGive);
        player.sendMessage(ChatColor.GREEN + "成功购买 " + amount + " 个 " + formatMaterialName(item.getMaterial().name()) + 
            "，花费 $" + totalPrice);
    }
    
    private void handleSell(Player player, ShopManager.ShopItem item, int amount) {
        ItemStack itemToSell = new ItemStack(item.getMaterial(), amount);
        
        if (!player.getInventory().containsAtLeast(itemToSell, amount)) {
            player.sendMessage(ChatColor.RED + "你没有足够的物品来出售!");
            return;
        }
        
        double sellPrice = item.getSellPrice() * amount;
        
        player.getInventory().removeItem(itemToSell);
        plugin.getEconomyManager().deposit(player, sellPrice);
        
        if (item.getMaxStock() > 0) {
            shopManager.increaseStock(item.getKey(), amount);
        }
        
        player.sendMessage(ChatColor.GREEN + "成功出售 " + amount + " 个 " + formatMaterialName(item.getMaterial().name()) + 
            "，获得 $" + sellPrice);
    }
    
    private void handleSellAll(Player player, ShopManager.ShopItem item) {
        int totalAmount = 0;
        
        for (ItemStack inventoryItem : player.getInventory().getContents()) {
            if (inventoryItem != null && inventoryItem.getType() == item.getMaterial()) {
                totalAmount += inventoryItem.getAmount();
            }
        }
        
        if (totalAmount == 0) {
            player.sendMessage(ChatColor.RED + "你没有任何可出售的物品!");
            return;
        }
        
        double sellPrice = item.getSellPrice() * totalAmount;
        
        ItemStack itemToRemove = new ItemStack(item.getMaterial(), totalAmount);
        player.getInventory().removeItem(itemToRemove);
        plugin.getEconomyManager().deposit(player, sellPrice);
        
        if (item.getMaxStock() > 0) {
            shopManager.increaseStock(item.getKey(), totalAmount);
        }
        
        player.sendMessage(ChatColor.GREEN + "成功出售 " + totalAmount + " 个 " + formatMaterialName(item.getMaterial().name()) + 
            "，获得 $" + sellPrice);
    }
    
    private boolean hasInventorySpace(Player player, ItemStack item) {
        Inventory inventory = player.getInventory();
        for (ItemStack inventoryItem : inventory.getStorageContents()) {
            if (inventoryItem == null || inventoryItem.getType() == Material.AIR) {
                return true;
            }
            if (inventoryItem.isSimilar(item) && inventoryItem.getAmount() + item.getAmount() <= item.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }
    
    private String formatMaterialName(String name) {
        return name.toLowerCase().replace('_', ' ');
    }
}