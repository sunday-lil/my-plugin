package org.ljcode.myPlugin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.ljcode.myPlugin.MyPlugin;

public class TeleportListener implements Listener {
    
    private final MyPlugin plugin;
    
    public TeleportListener(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // 记录非插件传送（末影珍珠、传送门等）的起点，供 /eback 使用。
        // 插件传送由 TeleportManager.teleport 统一记录，避免把目标点记成起点。
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
            plugin.getTeleportManager().setLastLocation(event.getPlayer(), event.getFrom());
        }
    }
}