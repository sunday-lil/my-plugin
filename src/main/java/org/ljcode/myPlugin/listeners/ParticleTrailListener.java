package org.ljcode.myPlugin.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.ljcode.myPlugin.MyPlugin;

public class ParticleTrailListener implements Listener {

    private final MyPlugin plugin;

    public ParticleTrailListener(MyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("gameplay.particle-trail.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

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