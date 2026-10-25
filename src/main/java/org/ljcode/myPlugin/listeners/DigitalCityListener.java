package org.ljcode.myPlugin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;
import org.ljcode.myPlugin.managers.DigitalCityManager;

public class DigitalCityListener implements Listener {
    private final MyPlugin plugin;
    private final DigitalCityManager cityManager;

    public DigitalCityListener(MyPlugin plugin) {
        this.plugin = plugin;
        this.cityManager = DigitalCityManager.getInstance();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        cityManager.recordPlayerJoin(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        cityManager.recordPlayerQuit(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String deathCause = event.getDeathMessage();
        if (deathCause == null) {
            deathCause = "未知原因";
        }
        cityManager.recordPlayerDeath(player, deathCause);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("digital-city.chat-tracking-enabled", true)) {
            return;
        }
        cityManager.recordChatMessage(event.getPlayer(), event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("digital-city.block-tracking-enabled", true)) {
            return;
        }
        cityManager.recordBlockBreak(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("digital-city.block-tracking-enabled", true)) {
            return;
        }
        cityManager.recordBlockPlace(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWeatherChange(WeatherChangeEvent event) {
        cityManager.recordWeatherChange(event.toWeatherState() ? "storm" : "clear");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onThunderChange(ThunderChangeEvent event) {
        cityManager.recordWeatherChange(event.toThunderState() ? "thunder" : (event.getWorld().hasStorm() ? "storm" : "clear"));
    }
}
