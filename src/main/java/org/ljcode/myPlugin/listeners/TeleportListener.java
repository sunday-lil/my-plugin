package org.ljcode.myPlugin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.ljcode.myPlugin.MyPlugin;

public class TeleportListener implements Listener {
    
    private final MyPlugin plugin;
    
    public TeleportListener(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // 可以在这里添加传送相关的逻辑
        // 例如：传送冷却时间、传送费用等
        
        // 记录传送前的最后一个位置
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
            plugin.getTeleportManager().setLastLocation(event.getPlayer(), event.getFrom());
        }
    }
}