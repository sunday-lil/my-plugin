package org.ljcode.myPlugin.managers;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.ljcode.myPlugin.MyPlugin;

import java.util.HashMap;
import java.util.Map;

public class ShopManager {
    
    private final MyPlugin plugin;
    private final Map<String, ShopItem> shopItems;
    private final Map<String, Integer> globalStock;
    
    public ShopManager(MyPlugin plugin) {
        this.plugin = plugin;
        this.shopItems = new HashMap<>();
        this.globalStock = new HashMap<>();
        loadShopItems();
    }
    
    private void loadShopItems() {
        FileConfiguration config = plugin.getConfig();
        
        if (!config.contains("shop")) {
            setupDefaultShopItems();
            plugin.saveConfig();
        }
        
        ConfigurationSection shopSection = config.getConfigurationSection("shop");
        if (shopSection != null) {
            for (String key : shopSection.getKeys(false)) {
                ConfigurationSection itemSection = shopSection.getConfigurationSection(key);
                if (itemSection != null) {
                    String materialName = itemSection.getString("material");
                    double price = itemSection.getDouble("price");
                    int slot = itemSection.getInt("slot");
                    int maxStock = itemSection.getInt("maxStock", -1);
                    
                    try {
                        Material material = Material.valueOf(materialName);
                        ShopItem item = new ShopItem(key, material, price, slot, maxStock);
                        shopItems.put(key, item);
                        
                        if (maxStock > 0) {
                            globalStock.put(key, itemSection.getInt("currentStock", maxStock));
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("无效的物品材质: " + materialName);
                    }
                }
            }
        }
    }
    
    private void setupDefaultShopItems() {
        FileConfiguration config = plugin.getConfig();
        
        config.set("shop.diamond.material", "DIAMOND");
        config.set("shop.diamond.price", 500);
        config.set("shop.diamond.slot", 0);
        config.set("shop.diamond.maxStock", -1);
        
        config.set("shop.netherite_ingot.material", "NETHERITE_INGOT");
        config.set("shop.netherite_ingot.price", 2000);
        config.set("shop.netherite_ingot.slot", 1);
        config.set("shop.netherite_ingot.maxStock", 10);
        config.set("shop.netherite_ingot.currentStock", 10);
    }
    
    public ShopItem getShopItem(String key) {
        return shopItems.get(key);
    }
    
    public Map<String, ShopItem> getAllShopItems() {
        return new HashMap<>(shopItems);
    }
    
    public int getCurrentStock(String key) {
        return globalStock.getOrDefault(key, -1);
    }
    
    public void setCurrentStock(String key, int amount) {
        ShopItem item = shopItems.get(key);
        if (item != null && item.getMaxStock() > 0) {
            globalStock.put(key, amount);
            saveStock();
        }
    }
    
    public boolean decreaseStock(String key, int amount) {
        ShopItem item = shopItems.get(key);
        if (item == null || item.getMaxStock() <= 0) {
            return true;
        }
        
        int currentStock = getCurrentStock(key);
        if (currentStock < amount) {
            return false;
        }
        
        setCurrentStock(key, currentStock - amount);
        return true;
    }
    
    public boolean increaseStock(String key, int amount) {
        ShopItem item = shopItems.get(key);
        if (item == null || item.getMaxStock() <= 0) {
            return true;
        }
        
        int currentStock = getCurrentStock(key);
        if (currentStock + amount > item.getMaxStock()) {
            return false;
        }
        
        setCurrentStock(key, currentStock + amount);
        return true;
    }
    
    private void saveStock() {
        FileConfiguration config = plugin.getConfig();
        for (Map.Entry<String, Integer> entry : globalStock.entrySet()) {
            config.set("shop." + entry.getKey() + ".currentStock", entry.getValue());
        }
        plugin.saveConfig();
    }
    
    public void saveData() {
        saveStock();
    }
    
    public void loadData() {
        loadShopItems();
    }
    
    public static class ShopItem {
        private final String key;
        private final Material material;
        private final double price;
        private final int slot;
        private final int maxStock;
        
        public ShopItem(String key, Material material, double price, int slot, int maxStock) {
            this.key = key;
            this.material = material;
            this.price = price;
            this.slot = slot;
            this.maxStock = maxStock;
        }
        
        public String getKey() {
            return key;
        }
        
        public Material getMaterial() {
            return material;
        }
        
        public double getPrice() {
            return price;
        }
        
        public int getSlot() {
            return slot;
        }
        
        public int getMaxStock() {
            return maxStock;
        }
        
        public double getSellPrice() {
            return price * 0.5;
        }
        
        public ItemStack getItemStack(int amount) {
            return new ItemStack(material, amount);
        }
    }
}