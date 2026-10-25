package org.ljcode.myPlugin.listeners;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.ljcode.myPlugin.MyPlugin;

import java.util.Collection;
import java.util.Map;

/**
 * 方块破坏自动拾取。
 *
 * 修复点：
 * 1. 使用 HIGHEST + ignoreCancelled，避免在保护插件取消事件后仍发放物品；
 * 2. 按工具计算掉落（时运/精准采集），而不是无条件 getDrops()；
 * 3. 只把“背包放不下的剩余物”掉落一次，不再重复掉落已入包物品；
 * 4. 创造模式不额外发放物品。
 */
public class BlockBreakListener implements Listener {

    private final MyPlugin plugin;

    public BlockBreakListener(MyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("gameplay.auto-pickup.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        Block block = event.getBlock();
        Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand(), player);
        if (drops.isEmpty()) {
            return;
        }

        event.setDropItems(false);
        dropLeftovers(player, block.getLocation(), drops);
    }

    private void dropLeftovers(Player player, Location location, Collection<ItemStack> drops) {
        boolean inventoryFull = false;

        for (ItemStack item : drops) {
            if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
                continue;
            }

            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
            if (leftover.isEmpty()) {
                continue;
            }

            inventoryFull = true;
            for (ItemStack remaining : leftover.values()) {
                player.getWorld().dropItemNaturally(location.clone().add(0.5, 0.5, 0.5), remaining);
            }
        }

        if (inventoryFull) {
            player.sendMessage(ChatColor.RED + "背包已满，部分掉落物已掉落在方块位置");
        }
    }
}
