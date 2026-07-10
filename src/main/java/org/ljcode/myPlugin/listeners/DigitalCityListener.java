package org.ljcode.myPlugin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        cityManager.recordChatMessage(player, message);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        cityManager.recordBlockBreak(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        cityManager.recordBlockPlace(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWeatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            String weather = event.getWorld().hasStorm() ? "storm" : "rain";
            cityManager.recordWeatherChange(weather);
        } else {
            cityManager.recordWeatherChange("clear");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        // 可以用于更精细的位置追踪（如果需要）
        // 目前在DigitalCityManager中定期收集位置信息
    }
}
