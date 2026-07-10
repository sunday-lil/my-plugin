package org.ljcode.myPlugin.listeners;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.ljcode.myPlugin.commands.HoloCommand;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HoloListener implements Listener {
    
    private final HoloCommand holoCommand;
    
    public HoloListener(HoloCommand holoCommand) {
        this.holoCommand = holoCommand;
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        try {
            Field playerHologramsField = HoloCommand.class.getDeclaredField("playerHolograms");
            playerHologramsField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Map<UUID, List<ArmorStand>> playerHolograms = (Map<UUID, List<ArmorStand>>) playerHologramsField.get(holoCommand);
            
            List<ArmorStand> holograms = playerHolograms.remove(playerId);
            
            if (holograms != null) {
                for (ArmorStand armorStand : holograms) {
                    if (armorStand.isValid()) {
                        armorStand.remove();
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}