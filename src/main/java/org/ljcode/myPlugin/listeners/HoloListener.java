package org.ljcode.myPlugin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.ljcode.myPlugin.commands.HoloCommand;

public class HoloListener implements Listener {

    private final HoloCommand holoCommand;

    public HoloListener(HoloCommand holoCommand) {
        this.holoCommand = holoCommand;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        holoCommand.cleanup(event.getPlayer());
    }
}
