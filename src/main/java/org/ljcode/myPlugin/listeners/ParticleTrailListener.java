package org.ljcode.myPlugin.listeners;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class ParticleTrailListener implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null) {
            return;
        }
        
        if (!hasMoved(from, to)) {
            return;
        }
        
        player.getWorld().spawnParticle(
            Particle.FLAME,
            to,
            5,
            0.1,
            0.1,
            0.1,
            0
        );
    }
    
    private boolean hasMoved(Location from, Location to) {
        return from.getBlockX() != to.getBlockX() 
            || from.getBlockY() != to.getBlockY() 
            || from.getBlockZ() != to.getBlockZ();
    }
}