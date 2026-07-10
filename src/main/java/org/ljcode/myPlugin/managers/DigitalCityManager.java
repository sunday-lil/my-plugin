package org.ljcode.myPlugin.managers;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DigitalCityManager {
    private static DigitalCityManager instance;
    private final MyPlugin plugin;
    private final K10TCPManager k10Manager;
    private final Gson gson = new Gson();

    private final AtomicInteger totalPlayersJoined = new AtomicInteger(0);
    private final AtomicInteger totalDeaths = new AtomicInteger(0);
    private final AtomicInteger totalMessages = new AtomicInteger(0);
    private final AtomicInteger totalBlocksBroken = new AtomicInteger(0);
    private final AtomicInteger totalBlocksPlaced = new AtomicInteger(0);
    private final AtomicLong totalEconomyTransactions = new AtomicLong(0);
    private final AtomicLong totalEconomyVolume = new AtomicLong(0);

    private final Map<String, PlayerActivityData> playerActivityMap = new ConcurrentHashMap<>();
    private final List<CityEvent> recentEvents = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Integer> biomeDistribution = new ConcurrentHashMap<>();
    private final Map<String, Integer> activityHeatmap = new ConcurrentHashMap<>();

    private String cityStatus = "NORMAL";
    private String currentWeather = "clear";
    private long serverUptime = 0;
    private Date cityFoundedDate;
    private int peakPlayersToday = 0;

    private int cityDashboardTaskId = -1;
    private int statisticsTaskId = -1;

    public DigitalCityManager(MyPlugin plugin) {
        this.plugin = plugin;
        this.k10Manager = plugin.getK10TCPManager();
        this.cityFoundedDate = new Date();
        instance = this;
    }

    public static DigitalCityManager getInstance() {
        return instance;
    }

    public void startCityManagement() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            startCityDashboard();
            startStatisticsCollection();
            sendCityInitialization();
            plugin.getLogger().info("🏙️ 数字城市管理系统已启动！");
        }, 100L);
    }

    public void stopCityManagement() {
        if (cityDashboardTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cityDashboardTaskId);
        }
        if (statisticsTaskId != -1) {
            Bukkit.getScheduler().cancelTask(statisticsTaskId);
        }
    }

    private void startCityDashboard() {
        cityDashboardTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            try {
                sendCityDashboard();
            } catch (Exception e) {
                plugin.getLogger().warning("发送城市仪表盘数据失败: " + e.getMessage());
            }
        }, 200L, 600L);
    }

    private void startStatisticsCollection() {
        statisticsTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            collectDetailedStatistics();
        }, 600L, 6000L);
    }

    private void sendCityInitialization() {
        Map<String, Object> cityData = new HashMap<>();
        cityData.put("event_type", "CITY_INIT");
        cityData.put("city_name", "Minecraft智慧城市");
        cityData.put("founded_date", cityFoundedDate.getTime());
        cityData.put("server_version", Bukkit.getVersion());
        cityData.put("online_players", Bukkit.getOnlinePlayers().size());
        cityData.put("max_players", Bukkit.getMaxPlayers());

        String cityJson = gson.toJson(cityData);
        k10Manager.sendMessageAsync(cityJson);
        if (plugin.getConfig().getBoolean("environment.debug-mode", false)) {
            plugin.getLogger().info("🏙️ 城市初始化数据已发送至K10");
        }
    }

    private void sendCityDashboard() {
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();
        double tps = getTPS();

        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("event_type", "CITY_DASHBOARD");
        dashboardData.put("timestamp", System.currentTimeMillis());

        Map<String, Object> basicStats = new HashMap<>();
        basicStats.put("online_players", onlinePlayers);
        basicStats.put("max_players", maxPlayers);
        basicStats.put("player_load", maxPlayers > 0 ? (onlinePlayers * 100 / maxPlayers) : 0);
        basicStats.put("tps", Math.round(tps * 100.0) / 100.0);
        basicStats.put("server_status", tps > 18.0 ? "EXCELLENT" : tps > 15.0 ? "GOOD" : tps > 10.0 ? "FAIR" : "POOR");
        basicStats.put("city_status", cityStatus);
        basicStats.put("uptime_hours", getServerUptimeHours());
        dashboardData.put("basic_stats", basicStats);

        Map<String, Object> populationStats = new HashMap<>();
        populationStats.put("total_joined", totalPlayersJoined.get());
        populationStats.put("current_online", onlinePlayers);
        populationStats.put("total_deaths", totalDeaths.get());
        populationStats.put("avg_session_time", calculateAverageSessionTime());
        populationStats.put("peak_today", peakPlayersToday);
        dashboardData.put("population_stats", populationStats);

        Map<String, Object> economyStats = new HashMap<>();
        EconomyManager economyManager = plugin.getEconomyManager();
        if (economyManager != null) {
            economyStats.put("total_transactions", totalEconomyTransactions.get());
            economyStats.put("total_volume", totalEconomyVolume.get());
            economyStats.put("active_bank_accounts", getActiveBankAccounts());
            economyStats.put("server_wealth", getTotalServerWealth());
        }
        dashboardData.put("economy_stats", economyStats);

        Map<String, Object> activityStats = new HashMap<>();
        activityStats.put("messages_sent", totalMessages.get());
        activityStats.put("blocks_broken", totalBlocksBroken.get());
        activityStats.put("blocks_placed", totalBlocksPlaced.get());
        activityStats.put("activity_level", calculateActivityLevel());
        dashboardData.put("activity_stats", activityStats);

        dashboardData.put("recent_events", getRecentEvents(5));

        String dashboardJson = gson.toJson(dashboardData);
        k10Manager.sendMessageAsync(dashboardJson);
        if (plugin.getConfig().getBoolean("environment.debug-mode", false)) {
            plugin.getLogger().info("📊 城市仪表盘数据已更新");
        }
    }

    private void collectDetailedStatistics() {
        biomeDistribution.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String biome = player.getLocation().getBlock().getBiome().name();
            biomeDistribution.merge(biome, 1, Integer::sum);

            String regionKey = getRegionKey(player.getLocation());
            activityHeatmap.merge(regionKey, 1, Integer::sum);
        }

        Map<String, Object> detailedStats = new HashMap<>();
        detailedStats.put("event_type", "DETAILED_STATISTICS");
        detailedStats.put("biome_distribution", new HashMap<>(biomeDistribution));
        detailedStats.put("activity_heatmap", new HashMap<>(activityHeatmap));
        detailedStats.put("player_locations", getPlayerLocations());

        String detailedJson = gson.toJson(detailedStats);
        k10Manager.sendMessageAsync(detailedJson);
    }

    public void recordPlayerJoin(Player player) {
        totalPlayersJoined.incrementAndGet();

        PlayerActivityData data = new PlayerActivityData(player.getName(), System.currentTimeMillis());
        playerActivityMap.put(player.getName().toLowerCase(), data);

        addCityEvent("PLAYER_JOIN", player.getName(), "玩家加入城市", 0x66FF66);

        int onlineCount = Bukkit.getOnlinePlayers().size();
        if (onlineCount > peakPlayersToday) {
            peakPlayersToday = onlineCount;
            addCityEvent("MILESTONE", "系统", "在线人数创新高: " + onlineCount, 0xFFDD44);
        }
    }

    public void recordPlayerQuit(Player player) {
        PlayerActivityData data = playerActivityMap.get(player.getName().toLowerCase());
        if (data != null) {
            data.setLeaveTime(System.currentTimeMillis());
            long sessionTime = (data.getLeaveTime() - data.getJoinTime()) / 1000 / 60;
            data.setSessionMinutes((int) sessionTime);
        }

        addCityEvent("PLAYER_QUIT", player.getName(), "玩家离开城市", 0xFFAA44);
    }

    public void recordPlayerDeath(Player player, String deathCause) {
        totalDeaths.incrementAndGet();
        addCityEvent("PLAYER_DEATH", player.getName(), "阵亡: " + deathCause, 0xFF4444);
        checkCityStatus();
    }

    public void recordChatMessage(Player player, String message) {
        totalMessages.incrementAndGet();
    }

    public void recordBlockBreak(Player player) {
        totalBlocksBroken.incrementAndGet();
        updatePlayerActivity(player.getName(), "mining");
    }

    public void recordBlockPlace(Player player) {
        totalBlocksPlaced.incrementAndGet();
        updatePlayerActivity(player.getName(), "building");
    }

    public void recordEconomyTransaction(double amount) {
        totalEconomyTransactions.incrementAndGet();
        totalEconomyVolume.addAndGet((long)(amount * 100));
    }

    public void recordSystemAnnouncement(String announcement) {
        addCityEvent("ANNOUNCEMENT", "系统", announcement, 0xAA88FF);
    }

    public void recordWeatherChange(String weather) {
        this.currentWeather = weather;
        addCityEvent("WEATHER_CHANGE", "环境", "天气变化: " + translateWeather(weather), 0x44DDFF);
    }

    private void addCityEvent(String type, String source, String description, int color) {
        CityEvent event = new CityEvent(type, source, description, color, System.currentTimeMillis());
        recentEvents.add(0, event);

        while (recentEvents.size() > 50) {
            recentEvents.remove(recentEvents.size() - 1);
        }

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("event_type", "CITY_EVENT");
        eventData.put("event", event.toMap());

        String eventJson = gson.toJson(eventData);
        k10Manager.sendMessageAsync(eventJson);
    }

    private void updatePlayerActivity(String playerName, String activityType) {
        PlayerActivityData data = playerActivityMap.get(playerName.toLowerCase());
        if (data != null) {
            data.recordActivity(activityType);
        }
    }

    private void checkCityStatus() {
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        double tps = getTPS();

        String oldStatus = cityStatus;

        if (tps < 10.0 || totalDeaths.get() > 50) {
            cityStatus = "CRITICAL";
        } else if (tps < 15.0 || totalDeaths.get() > 20) {
            cityStatus = "WARNING";
        } else if (onlinePlayers >= Bukkit.getMaxPlayers() * 0.8) {
            cityStatus = "BUSY";
        } else if (onlinePlayers > 0 && tps > 18.0) {
            cityStatus = "EXCELLENT";
        } else {
            cityStatus = "NORMAL";
        }

        if (!oldStatus.equals(cityStatus)) {
            addCityEvent("STATUS_CHANGE", "系统", "城市状态: " + translateStatus(cityStatus),
                        "CRITICAL".equals(cityStatus) ? 0xFF4444 : "WARNING".equals(cityStatus) ? 0xFFAA44 : 0x66FF66);
        }
    }

    private List<Map<String, Object>> getRecentEvents(int count) {
        List<Map<String, Object>> events = new ArrayList<>();
        int limit = Math.min(count, recentEvents.size());

        for (int i = 0; i < limit; i++) {
            events.add(recentEvents.get(i).toMap());
        }

        return events;
    }

    private double getTPS() {
        try {
            // 尝试使用 Paper/Purpur 的 getTPS() 方法
            Object server = Bukkit.getServer();
            if (server != null) {
                // 使用反射获取 TPS
                java.lang.reflect.Method getTPSMethod = server.getClass().getMethod("getTPS");
                if (getTPSMethod != null) {
                    Object result = getTPSMethod.invoke(server);
                    if (result instanceof double[]) {
                        double[] tpsArray = (double[]) result;
                        if (tpsArray.length > 0) {
                            return tpsArray[0];
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 如果无法获取 TPS，使用备用方案
            plugin.getLogger().fine("无法获取TPS数据，使用默认值: " + e.getMessage());
        }

        // 备用方案：根据在线玩家数量和消息量估算性能状态
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        long totalActivity = totalMessages.get() + totalBlocksBroken.get() + totalBlocksPlaced.get();

        // 简单的性能估算逻辑
        if (onlinePlayers > 15 || totalActivity > 50000) {
            return 16.0; // 高负载
        } else if (onlinePlayers > 10 || totalActivity > 20000) {
            return 18.0; // 中等负载
        } else if (onlinePlayers > 5 || totalActivity > 5000) {
            return 19.0; // 轻微负载
        } else {
            return 20.0; // 正常负载
        }
    }

    private double getServerUptimeHours() {
        return (System.currentTimeMillis() - cityFoundedDate.getTime()) / (1000.0 * 60 * 60);
    }

    private String getRegionKey(org.bukkit.Location loc) {
        int regionSize = 100;
        int regionX = (int)Math.floor(loc.getX() / regionSize);
        int regionZ = (int)Math.floor(loc.getZ() / regionSize);
        return regionX + "," + regionZ;
    }

    private List<Map<String, Object>> getPlayerLocations() {
        List<Map<String, Object>> locations = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<String, Object> loc = new HashMap<>();
            loc.put("name", player.getName());
            loc.put("world", player.getWorld().getName());
            loc.put("x", Math.round(player.getLocation().getX()));
            loc.put("y", Math.round(player.getLocation().getY()));
            loc.put("z", Math.round(player.getLocation().getZ()));
            locations.add(loc);
        }
        return locations;
    }

    private int calculateAverageSessionTime() {
        if (playerActivityMap.isEmpty()) return 0;

        int totalTime = 0;
        int count = 0;

        for (PlayerActivityData data : playerActivityMap.values()) {
            if (data.getSessionMinutes() > 0) {
                totalTime += data.getSessionMinutes();
                count++;
            }
        }

        return count > 0 ? totalTime / count : 0;
    }

    private String calculateActivityLevel() {
        int totalActivity = totalBlocksBroken.get() + totalBlocksPlaced.get() + totalMessages.get();

        if (totalActivity > 10000) return "VERY_HIGH";
        if (totalActivity > 5000) return "HIGH";
        if (totalActivity > 1000) return "MODERATE";
        if (totalActivity > 100) return "LOW";
        return "MINIMAL";
    }

    private int getActiveBankAccounts() {
        // 简化实现：返回在线玩家数量作为活跃账户数
        return Bukkit.getOnlinePlayers().size();
    }

    private double getTotalServerWealth() {
        EconomyManager economyManager = plugin.getEconomyManager();
        if (economyManager == null) return 0.0;

        double totalWealth = 0.0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            totalWealth += economyManager.getBalance(player);
        }
        return totalWealth;
    }

    private String translateWeather(String weather) {
        switch (weather.toLowerCase()) {
            case "clear": return "晴朗";
            case "rain": return "下雨";
            case "storm": return "暴雨";
            case "thunder": return "雷暴";
            default: return weather;
        }
    }

    private String translateStatus(String status) {
        switch (status) {
            case "EXCELLENT": return "卓越运行";
            case "NORMAL": return "正常运行";
            case "BUSY": return "繁忙";
            case "WARNING": return "警告";
            case "CRITICAL": return "紧急";
            default: return status;
        }
    }

    public static class CityEvent {
        private final String type;
        private final String source;
        private final String description;
        private final int color;
        private final long timestamp;

        public CityEvent(String type, String source, String description, int color, long timestamp) {
            this.type = type;
            this.source = source;
            this.description = description;
            this.color = color;
            this.timestamp = timestamp;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("type", type);
            map.put("source", source);
            map.put("description", description);
            map.put("color", String.format("#%06X", color));
            map.put("timestamp", timestamp);
            return map;
        }

        public String getType() { return type; }
        public String getSource() { return source; }
        public String getDescription() { return description; }
        public int getColor() { return color; }
        public long getTimestamp() { return timestamp; }
    }

    public static class PlayerActivityData {
        private final String playerName;
        private final long joinTime;
        private long leaveTime;
        private int sessionMinutes;
        private final Map<String, Integer> activities = new HashMap<>();

        public PlayerActivityData(String playerName, long joinTime) {
            this.playerName = playerName;
            this.joinTime = joinTime;
        }

        public void recordActivity(String activityType) {
            activities.merge(activityType, 1, Integer::sum);
        }

        public String getPlayerName() { return playerName; }
        public long getJoinTime() { return joinTime; }
        public long getLeaveTime() { return leaveTime; }
        public void setLeaveTime(long leaveTime) { this.leaveTime = leaveTime; }
        public int getSessionMinutes() { return sessionMinutes; }
        public void setSessionMinutes(int sessionMinutes) { this.sessionMinutes = sessionMinutes; }
        public Map<String, Integer> getActivities() { return activities; }
    }
}
