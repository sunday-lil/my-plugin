package org.ljcode.myPlugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.ljcode.myPlugin.MyPlugin;

import java.util.Collection;

/**
 * 方块破坏事件监听器
 * 监听玩家破坏方块事件，实现自动拾取功能
 * 取消默认掉落，直接将方块掉落物放入玩家背包
 * 如果背包已满，物品将掉落在玩家脚下并发送提示消息
 */
public class BlockBreakListener implements Listener {
    
    private final MyPlugin plugin;
    
    public BlockBreakListener(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 处理方块破坏事件
     * 取消默认掉落，将掉落物直接放入玩家背包
     * 
     * @param event 方块破坏事件对象
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // 取消默认的物品掉落
        event.setDropItems(false);
        
        // 获取方块原本会掉落的物品
        Collection<ItemStack> drops = block.getDrops();
        
        // 如果没有掉落物，直接返回
        if (drops.isEmpty()) {
            return;
        }
        
        // 尝试将物品放入玩家背包
        boolean allItemsAdded = addItemsToInventory(player, drops);
        
        if (!allItemsAdded) {
            // 如果背包已满，将物品掉落在玩家脚下
            dropItemsAtPlayerFeet(player, drops);
            
            // 发送动作栏消息提示背包已满
            sendActionBarMessage(player, "§c背包已满，物品已掉落在地面");
        }
    }
    
    /**
     * 将物品添加到玩家背包
     * 
     * @param player 玩家对象
     * @param items 要添加的物品集合
     * @return 是否所有物品都成功添加
     */
    private boolean addItemsToInventory(Player player, Collection<ItemStack> items) {
        PlayerInventory inventory = player.getInventory();
        
        for (ItemStack item : items) {
            // 检查物品是否为空或空气
            if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
                continue;
            }
            
            // 尝试将物品添加到背包
            java.util.HashMap<Integer, ItemStack> leftover = inventory.addItem(item);
            
            // 如果有剩余物品，说明背包已满
            if (!leftover.isEmpty()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 在玩家脚下掉落物品
     * 
     * @param player 玩家对象
     * @param items 要掉落的物品集合
     */
    private void dropItemsAtPlayerFeet(Player player, Collection<ItemStack> items) {
        Location dropLocation = player.getLocation();
        
        for (ItemStack item : items) {
            // 检查物品是否为空或空气
            if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
                continue;
            }
            
            // 在玩家脚下掉落物品
            Item droppedItem = player.getWorld().dropItemNaturally(dropLocation, item);
            
            // 设置物品的拾取延迟（防止玩家立即拾取）
            droppedItem.setPickupDelay(10); // 10 ticks = 0.5秒
        }
    }
    
    /**
     * 发送动作栏消息给玩家
     * 
     * @param player 玩家对象
     * @param message 要发送的消息
     */
    private void sendActionBarMessage(Player player, String message) {
        try {
            // 使用Spigot API发送动作栏消息
            player.spigot().sendMessage(
                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message)
            );
        } catch (Exception e) {
            // 如果Spigot API不可用，使用普通消息作为备选
            player.sendMessage(ChatColor.RED + "背包已满，物品已掉落在地面");
        }
    }
}