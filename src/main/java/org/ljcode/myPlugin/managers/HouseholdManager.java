package org.ljcode.myPlugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.ljcode.myPlugin.MyPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 住户结构扫描管理器（v1.2.0）
 *
 * "户数"判定规则：一张床 + 半径R内至少一扇门 = 一户（有床有门才算家）。
 * 多张床共用一扇门按多户计（相当于合租/多房间）。
 *
 * 扫描方式（性能安全）：
 * 1. 主线程：对在线玩家周围的已加载区块拍摄 ChunkSnapshot（快照拍摄开销极小）
 * 2. 异步线程：分批遍历快照寻找床/门方块（不访问实时世界，线程安全）
 * 3. 主线程回调：更新户数统计，户数变动时推送 K10 城市事件
 */
public class HouseholdManager {

    /** 单次扫描最多处理的区块数（多玩家重叠区域自动去重） */
    private static final int MAX_CHUNKS_PER_SCAN = 256;
    /** 异步批处理每批处理的区块数 */
    private static final int CHUNKS_PER_BATCH = 8;

    private final MyPlugin plugin;
    private int scanTaskId = -1;

    private volatile int householdCount = 0;
    private volatile boolean scanning = false;
    private long lastScanTime = 0;

    // 方块坐标三元组（跨区块匹配床↔门用）
    private record BlockPos(String world, int x, int y, int z) {}

    // 待扫描区块（快照+世界信息，异步线程只读快照，线程安全）
    private record ScanChunk(ChunkSnapshot snapshot, String worldName, int chunkX, int chunkZ, int minY, int maxY) {}

    public HouseholdManager(MyPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        long interval = plugin.getConfig().getLong("digital-city.households.scan-interval", 6000L);
        scanTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            try {
                beginScan();
            } catch (Exception e) {
                plugin.getLogger().warning("[住户扫描] 扫描失败: " + e.getMessage());
                scanning = false;
            }
        }, 400L, interval);
        plugin.getLogger().info("[住户扫描] 已启动，周期: " + (interval / 20) + "秒，规则: 床+门(半径"
                + plugin.getConfig().getInt("digital-city.households.door-bed-radius", 6) + "格)=一户");
    }

    public void stop() {
        if (scanTaskId != -1) {
            Bukkit.getScheduler().cancelTask(scanTaskId);
            scanTaskId = -1;
        }
    }

    /** 第1步（主线程）：收集玩家周围区块快照，交给异步批处理 */
    private void beginScan() {
        if (scanning) {
            return; // 上一轮未完成，跳过本轮
        }

        int scanRadius = plugin.getConfig().getInt("digital-city.households.scan-radius", 64);
        int chunkRadius = Math.max(1, (scanRadius + 15) / 16);

        Set<String> visited = new HashSet<>();
        List<ScanChunk> chunks = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Location loc = player.getLocation();
            int baseX = loc.getBlockX() >> 4;
            int baseZ = loc.getBlockZ() >> 4;
            World world = player.getWorld();

            for (int dx = -chunkRadius; dx <= chunkRadius && chunks.size() < MAX_CHUNKS_PER_SCAN; dx++) {
                for (int dz = -chunkRadius; dz <= chunkRadius && chunks.size() < MAX_CHUNKS_PER_SCAN; dz++) {
                    String key = world.getName() + ":" + (baseX + dx) + ":" + (baseZ + dz);
                    if (visited.contains(key)) {
                        continue;
                    }
                    visited.add(key);

                    if (world.isChunkLoaded(baseX + dx, baseZ + dz)) {
                        chunks.add(new ScanChunk(
                                world.getChunkAt(baseX + dx, baseZ + dz).getChunkSnapshot(),
                                world.getName(), baseX + dx, baseZ + dz,
                                world.getMinHeight(), world.getMaxHeight() - 1));
                    }
                }
            }
        }

        if (chunks.isEmpty()) {
            return; // 无可扫描区块（无人在线或区块未加载），保留上次户数
        }

        scanning = true;
        lastScanTime = System.currentTimeMillis();
        processBatch(chunks, 0, new ArrayList<>(), new ArrayList<>());
    }

    /** 第2步（异步）：分批遍历快照，收集床/门坐标 */
    private void processBatch(List<ScanChunk> chunks, int index, List<BlockPos> beds, List<BlockPos> doors) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    int end = Math.min(index + CHUNKS_PER_BATCH, chunks.size());
                    for (int i = index; i < end; i++) {
                        scanChunk(chunks.get(i), beds, doors);
                    }

                    if (end < chunks.size()) {
                        processBatch(chunks, end, beds, doors);
                    } else {
                        matchHouseholds(beds, doors);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[住户扫描] 异步处理异常: " + e.getMessage());
                    finishScan(householdCount); // 出错时保留旧值
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void scanChunk(ScanChunk chunk, List<BlockPos> beds, List<BlockPos> doors) {
        int minY = Math.max(-64, chunk.minY());
        int maxY = Math.min(320, chunk.maxY());

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Material type = chunk.snapshot().getBlockType(x, y, z);
                    // 本地坐标(0-15)转世界坐标，跨区块配对床↔门必需
                    int worldX = chunk.chunkX() * 16 + x;
                    int worldZ = chunk.chunkZ() * 16 + z;
                    if (Tag.BEDS.isTagged(type)) {
                        beds.add(new BlockPos(chunk.worldName(), worldX, y, worldZ));
                    } else if (Tag.DOORS.isTagged(type)) {
                        doors.add(new BlockPos(chunk.worldName(), worldX, y, worldZ));
                    }
                }
            }
        }
    }

    /** 床↔门配对统计（纯计算，仍在异步线程） */
    private void matchHouseholds(List<BlockPos> beds, List<BlockPos> doors) {
        int radius = plugin.getConfig().getInt("digital-city.households.door-bed-radius", 6);
        int radiusSq = radius * radius;

        int count = 0;
        if (!doors.isEmpty()) {
            for (BlockPos bed : beds) {
                for (BlockPos door : doors) {
                    if (!bed.world().equals(door.world())) {
                        continue;
                    }
                    int dx = bed.x() - door.x();
                    int dy = bed.y() - door.y();
                    int dz = bed.z() - door.z();
                    if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                        count++;
                        break; // 一张床配到任意一扇门即为一户
                    }
                }
            }
        }

        // 第3步：回主线程更新统计（涉及 Bukkit API 与 K10 事件推送）
        final int result = count;
        new BukkitRunnable() {
            @Override
            public void run() {
                finishScan(result);
            }
        }.runTask(plugin);
    }

    /** 第3步（主线程）：更新户数，变动时推送K10事件 */
    private void finishScan(int newCount) {
        scanning = false;

        int oldCount = householdCount;
        householdCount = newCount;

        if (newCount != oldCount) {
            plugin.getLogger().info("[住户扫描] 户数变化: " + oldCount + " → " + newCount);

            DigitalCityManager cityManager = DigitalCityManager.getInstance();
            if (cityManager != null) {
                cityManager.recordHouseholdChange(oldCount, newCount);
            }
        }

        if (plugin.getConfig().getBoolean("digital-city.debug-mode", false)) {
            plugin.getLogger().info("[住户扫描] 扫描完成，当前户数: " + newCount);
        }
    }

    public int getHouseholdCount() {
        return householdCount;
    }

    public long getLastScanTime() {
        return lastScanTime;
    }

    public boolean isScanning() {
        return scanning;
    }
}
