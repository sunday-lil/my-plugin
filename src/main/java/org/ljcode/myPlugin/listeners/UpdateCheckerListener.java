package org.ljcode.myPlugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.ljcode.myPlugin.MyPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class UpdateCheckerListener implements Listener {
    private final MyPlugin plugin;
    private String latestVersion;
    private boolean updateAvailable = false;

    public UpdateCheckerListener(MyPlugin plugin) {
        this.plugin = plugin;
        checkForUpdates();
    }

    /**
     * 检查是否有新版本
     */
    public void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            try {
                // 模拟从 BuiltByBit API 获取最新版本
                // 实际实现中应该使用真实的 API 端点
                URL url = new URL("https://api.example.com/builtbybit/plugin/version");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    latestVersion = response.toString().trim();
                    String currentVersion = plugin.getDescription().getVersion();

                    // 简单的版本比较
                    if (!currentVersion.equals(latestVersion)) {
                        updateAvailable = true;
                        plugin.getLogger().info("发现新版本: " + latestVersion);
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().warning("检查更新时出错: " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 检查玩家是否有权限接收更新通知
        if (updateAvailable && event.getPlayer().hasPermission("chatannouncements.update.notify")) {
            event.getPlayer().sendMessage("§a[更新通知] 发现 ChatAnnouncements 插件新版本: §e" + latestVersion);
            event.getPlayer().sendMessage("§a请访问 BuiltByBit 下载最新版本！");
        }
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}