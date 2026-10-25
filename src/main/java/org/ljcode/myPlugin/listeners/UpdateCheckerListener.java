package org.ljcode.myPlugin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.ljcode.myPlugin.MyPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub Releases 更新检查。
 *
 * 旧实现请求的是不存在的 api.example.com，且永远失败；现在改为读取
 * sunday-lil/my-plugin 的最新 Release tag，并在有更新时通知有权限的管理员。
 */
public class UpdateCheckerListener implements Listener {

    private static final String LATEST_RELEASE_URL =
            "https://api.github.com/repos/sunday-lil/my-plugin/releases/latest";

    private final MyPlugin plugin;
    private volatile String latestVersion;
    private volatile boolean updateAvailable = false;

    public UpdateCheckerListener(MyPlugin plugin) {
        this.plugin = plugin;
        checkForUpdates();
    }

    public void checkForUpdates() {
        if (!plugin.getConfig().getBoolean("update-check.enabled", true)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = URI.create(LATEST_RELEASE_URL).toURL();
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "EssentialsX-Clone-UpdateChecker");

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    plugin.getLogger().fine("更新检查返回 HTTP " + connection.getResponseCode());
                    return;
                }

                String body;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    body = response.toString();
                }

                String tag = extractTagName(body);
                if (tag == null || tag.isEmpty()) {
                    return;
                }

                latestVersion = tag.startsWith("v") ? tag.substring(1) : tag;
                String currentVersion = plugin.getDescription().getVersion();
                updateAvailable = isNewerVersion(currentVersion, latestVersion);
                if (updateAvailable) {
                    plugin.getLogger().info("发现新版本: " + latestVersion + "（当前 " + currentVersion + "）");
                } else {
                    plugin.getLogger().fine("当前已是最新版本: " + currentVersion);
                }
            } catch (IOException e) {
                plugin.getLogger().fine("检查更新失败: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private String extractTagName(String json) {
        Matcher matcher = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 按 x.y.z 逐段比较，忽略前缀 v 和 -SNAPSHOT 等后缀。
     */
    public static boolean isNewerVersion(String current, String latest) {
        int[] currentParts = parseVersion(current);
        int[] latestParts = parseVersion(latest);
        for (int i = 0; i < 3; i++) {
            if (latestParts[i] != currentParts[i]) {
                return latestParts[i] > currentParts[i];
            }
        }
        return false;
    }

    private static int[] parseVersion(String version) {
        int[] parts = new int[3];
        if (version == null) {
            return parts;
        }
        String cleaned = version.trim().replaceFirst("^[vV]", "");
        String[] split = cleaned.split("[.\\-+]");
        for (int i = 0; i < Math.min(3, split.length); i++) {
            try {
                parts[i] = Integer.parseInt(split[i].replaceAll("\\D", ""));
            } catch (NumberFormatException ignored) {
                parts[i] = 0;
            }
        }
        return parts;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (updateAvailable && event.getPlayer().hasPermission("chatannouncements.update.notify")) {
            event.getPlayer().sendMessage("§a[更新通知] 发现 EssentialsX-Clone 新版本: §e" + latestVersion);
            event.getPlayer().sendMessage("§a请访问 https://github.com/sunday-lil/my-plugin/releases 下载");
        }
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
