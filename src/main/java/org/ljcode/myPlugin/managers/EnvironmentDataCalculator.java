package org.ljcode.myPlugin.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 环境数据计算器
 * 负责根据游戏数据计算温度、湿度、光照、风速、周围资源占比等环境参数
 */
public class EnvironmentDataCalculator {

    private final int scanRadius;
    private final int heightRange;

    public EnvironmentDataCalculator(int scanRadius, int heightRange) {
        this.scanRadius = scanRadius;
        this.heightRange = heightRange;
    }

    /**
     * 计算玩家周围的环境数据
     * @param player 目标玩家
     * @return 环境数据Map
     */
    public Map<String, Object> calculateEnvironmentData(Player player) {
        Map<String, Object> data = new HashMap<>();
        Location loc = player.getLocation();
        World world = player.getWorld();

        data.put("temperature", calculateTemperature(loc, world));
        data.put("humidity", calculateHumidity(loc, world));
        data.put("light", calculateLightLevel(loc, world));
        data.put("wind_speed", calculateWindSpeed(loc, world));
        data.put("resources", calculateResourceRatio(loc, world));
        data.put("player_x", loc.getBlockX());
        data.put("player_y", loc.getBlockY());
        data.put("player_z", loc.getBlockZ());
        data.put("world_name", world.getName());
        data.put("world_time", world.getTime());
        data.put("weather", world.hasStorm() ? "storm" : (world.isThundering() ? "thunder" : "clear"));
        data.put("biome", loc.getBlock().getBiome().name());

        return data;
    }

    /**
     * 计算温度（基于生物群系、时间、环境因素的综合计算）
     * @param loc 位置
     * @param world 世界
     * @return 温度值（摄氏度，范围-30到60）
     */
    public double calculateTemperature(Location loc, World world) {
        Block block = loc.getBlock();
        Biome biome = block.getBiome();
        double baseTemp = getBiomeTemperature(biome);

        double temperature = baseTemp;

        // 1. 日照强度调整（基于太阳高度角）
        temperature += calculateSunlightEffect(world);

        // 2. 天气调整
        temperature += calculateWeatherEffect(world);

        // 3. 高度调整（更符合物理规律）
        temperature += calculateAltitudeEffect(loc.getBlockY());

        // 4. 地下深度调整（地下温度更稳定）
        temperature += calculateUndergroundEffect(loc);

        // 5. 周围热源/冷源影响
        temperature += calculateThermalSourcesEffect(loc, world);

        // 6. 水体影响（水的比热容大，调节温度）
        temperature += calculateWaterProximityEffect(loc, world);

        // 7. 风寒效应（风速影响体感温度）
        double windSpeed = calculateWindSpeed(loc, world);
        temperature += calculateWindChillEffect(temperature, windSpeed);

        // 8. 湿度影响（高湿度影响体感温度）
        double humidity = calculateHumidity(loc, world);
        temperature += calculateHumidityEffect(temperature, humidity);

        // 9. 世界环境调整
        temperature += calculateWorldEnvironmentEffect(world);

        // 确保温度在合理范围内
        temperature = Math.max(-30, Math.min(60, temperature));

        return Math.round(temperature * 10.0) / 10.0;
    }

    /**
     * 计算日照强度对温度的影响
     * 基于太阳高度角计算，正午时分影响最大
     */
    private double calculateSunlightEffect(World world) {
        long time = world.getTime();
        double sunAngle;

        // 计算太阳高度角（0-180度，90度为正午）
        if (time >= 0 && time < 6000) {
            // 日出到正午 (0-90度)
            sunAngle = (time / 6000.0) * 90.0;
        } else if (time >= 6000 && time < 12000) {
            // 正午到日落 (90-180度)
            sunAngle = 90.0 + ((time - 6000) / 6000.0) * 90.0;
        } else {
            // 夜晚（无日照）
            return -5.0;
        }

        // 日照强度与太阳高度角的正弦值成正比
        double intensity = Math.sin(Math.toRadians(sunAngle));

        // 正午最大升温8度，早晚升温较少
        return intensity * 8.0;
    }

    /**
     * 计算天气对温度的影响
     */
    private double calculateWeatherEffect(World world) {
        double effect = 0.0;

        if (world.isThundering()) {
            effect -= 4.0;
        } else if (world.hasStorm()) {
            effect -= 2.5;
        }

        return effect;
    }

    /**
     * 计算高度对温度的影响
     * 海拔每升高100米，温度下降约0.6度（符合真实物理规律）
     */
    private double calculateAltitudeEffect(int y) {
        double effect = 0.0;

        if (y > 64) {
            // 海平面以上，每升高100格降温0.6度
            effect -= (y - 64) * 0.006;
        } else if (y < 40) {
            // 地下深处，温度略微升高（地热效应）
            effect += (40 - y) * 0.02;
        }

        return effect;
    }

    /**
     * 计算地下深度对温度的影响
     * 地下温度更稳定，受地表温度影响较小
     */
    private double calculateUndergroundEffect(Location loc) {
        int y = loc.getBlockY();

        // 检测是否在地下（头顶有方块遮挡）
        boolean isUnderground = false;
        World world = loc.getWorld();
        if (world != null) {
            for (int checkY = y + 1; checkY <= Math.min(y + 10, world.getMaxHeight()); checkY++) {
                Block aboveBlock = world.getBlockAt(loc.getBlockX(), checkY, loc.getBlockZ());
                if (aboveBlock.getType().isSolid() || aboveBlock.getType().name().contains("LEAVES")) {
                    isUnderground = true;
                    break;
                }
            }
        }

        if (isUnderground) {
            // 地下温度更稳定，减少极端温度
            return 2.0;
        }

        return 0.0;
    }

    /**
     * 计算周围热源和冷源对温度的影响
     * 优先检测玩家直接接触的热源/冷源
     */
    private double calculateThermalSourcesEffect(Location loc, World world) {
        double effect = 0.0;
        int checkRadius = 5;

        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();

        // ★ 关键修复：首先检测玩家是否直接接触极端热源/冷源
        Block playerBlock = world.getBlockAt(cx, cy, cz);
        Block feetBlock = world.getBlockAt(cx, cy - 1, cz);
        Block headBlock = world.getBlockAt(cx, cy + 1, cz);

        // 玩家身体位置检测（最高优先级）
        Material playerMaterial = playerBlock.getType();
        Material feetMaterial = feetBlock.getType();
        Material headMaterial = headBlock.getType();

        // 如果玩家站在或处于岩浆中 → 极高温度
        if (playerMaterial == Material.LAVA || feetMaterial == Material.LAVA) {
            return 45.0; // 直接接触岩浆：+45度
        }
        if (headMaterial == Material.LAVA) {
            return 35.0; // 头部在岩浆中：+35度
        }

        // 如果玩家站在火焰中 → 高温
        if (playerMaterial == Material.FIRE || playerMaterial == Material.SOUL_FIRE ||
            feetMaterial == Material.FIRE || feetMaterial == Material.SOUL_FIRE) {
            return 30.0; // 直接接触火焰：+30度
        }

        // 如果玩家站在岩浆块上 → 高温
        if (feetMaterial == Material.MAGMA_BLOCK) {
            return 25.0; // 站在岩浆块上：+25度
        }

        // 如果玩家接触极寒方块 → 极低温度
        if (playerMaterial == Material.BLUE_ICE || feetMaterial == Material.BLUE_ICE ||
            headMaterial == Material.BLUE_ICE) {
            return -30.0; // 接触蓝冰：-30度
        }
        if (playerMaterial == Material.PACKED_ICE || feetMaterial == Material.PACKED_ICE) {
            return -20.0; // 接触浮冰：-20度
        }
        if (playerMaterial == Material.ICE || feetMaterial == Material.ICE) {
            return -12.0; // 接触普通冰：-12度
        }

        // 周围环境热源/冷源影响（距离衰减）
        for (int x = cx - checkRadius; x <= cx + checkRadius; x++) {
            for (int y = cy - checkRadius; y <= cy + checkRadius; y++) {
                for (int z = cz - checkRadius; z <= cz + checkRadius; z++) {
                    Block nearbyBlock = world.getBlockAt(x, y, z);
                    Material type = nearbyBlock.getType();

                    // 计算距离衰减
                    double distance = Math.sqrt(Math.pow(x - cx, 2) + Math.pow(y - cy, 2) + Math.pow(z - cz, 2));
                    if (distance == 0) continue; // 跳过玩家所在位置（已处理）
                    double distanceFactor = 1.0 / distance;

                    // 热源（提高权重）
                    if (type == Material.LAVA) {
                        effect += 15.0 * distanceFactor; // 提高：8→15
                    } else if (type == Material.FIRE || type == Material.SOUL_FIRE) {
                        effect += 8.0 * distanceFactor; // 提高：5→8
                    } else if (type.name().contains("CAMPFIRE") || type.name().contains("SOUL_CAMPFIRE")) {
                        effect += 7.0 * distanceFactor; // 提高：6→7
                    } else if (type == Material.MAGMA_BLOCK) {
                        effect += 5.0 * distanceFactor; // 提高：3→5
                    } else if (type.name().contains("TORCH") || type.name().contains("LANTERN")) {
                        effect += 1.5 * distanceFactor; // 提高：1→1.5
                    } else if (type.name().contains("FURNACE") || type.name().contains("BLAST_FURNACE") || type.name().contains("SMOKER")) {
                        effect += 3.0 * distanceFactor; // 提高：2→3
                    }

                    // 冷源（提高权重）
                    else if (type == Material.BLUE_ICE) {
                        effect -= 6.0 * distanceFactor; // 提高：4→6
                    } else if (type == Material.PACKED_ICE) {
                        effect -= 4.0 * distanceFactor; // 提高：3→4
                    } else if (type == Material.ICE || type.name().contains("FROSTED_ICE")) {
                        effect -= 3.0 * distanceFactor; // 提高：2→3
                    } else if (type.name().contains("SNOW")) {
                        effect -= 2.0 * distanceFactor; // 提高：1.5→2
                    }
                }
            }
        }

        // 扩大允许的范围（原：-10到15，新：-20到30）
        return Math.max(-20, Math.min(30, effect));
    }

    /**
     * 计算水体对温度的调节作用
     * 水的比热容大，能稳定周围温度
     */
    private double calculateWaterProximityEffect(Location loc, World world) {
        int waterCount = countNearbyBlocks(loc, Material.WATER, 7);

        if (waterCount > 0) {
            // 水体调节温度，使极端温度趋向温和
            double currentTemp = getBiomeTemperature(loc.getBlock().getBiome());

            if (currentTemp > 25) {
                // 炎热环境，水降温
                return -Math.min(waterCount * 0.3, 5.0);
            } else if (currentTemp < 10) {
                // 寒冷环境，水略微升温（水的比热容大，温度稳定）
                return Math.min(waterCount * 0.1, 2.0);
            }
        }

        return 0.0;
    }

    /**
     * 计算风寒效应对体感温度的影响
     * 风速越大，体感温度越低
     */
    private double calculateWindChillEffect(double currentTemp, double windSpeed) {
        // 只在低温时风寒效应明显
        if (currentTemp < 15 && windSpeed > 3.0) {
            // 风寒公式简化版：风速每增加5m/s，体感温度降低约1-2度
            double windChill = -(windSpeed / 5.0) * 1.5;
            return windChill;
        }
        return 0.0;
    }

    /**
     * 计算湿度对体感温度的影响
     * 高湿度在高温时让人感觉更热，低温时感觉更冷
     */
    private double calculateHumidityEffect(double currentTemp, double humidity) {
        double effect = 0.0;

        if (currentTemp > 25 && humidity > 60) {
            // 高温高湿：体感温度升高
            effect = (humidity - 60) * 0.05;
        } else if (currentTemp < 10 && humidity > 70) {
            // 低温高湿：体感温度降低（湿冷）
            effect = -(humidity - 70) * 0.03;
        }

        return effect;
    }

    /**
     * 计算世界环境对温度的影响
     */
    private double calculateWorldEnvironmentEffect(World world) {
        if (world.getEnvironment() == World.Environment.NETHER) {
            return 25.0;
        } else if (world.getEnvironment() == World.Environment.THE_END) {
            return -8.0;
        }
        return 0.0;
    }

    /**
     * 计算湿度（基于生物群系和天气）
     * @param loc 位置
     * @param world 世界
     * @return 湿度值（百分比，范围0-100）
     */
    public double calculateHumidity(Location loc, World world) {
        Block block = loc.getBlock();
        Biome biome = block.getBiome();
        double baseHumidity = getBiomeHumidity(biome);

        double humidity = baseHumidity;

        if (world.hasStorm()) {
            humidity += 20;
        }
        if (world.isThundering()) {
            humidity += 10;
        }

        String biomeName = biome.name().toUpperCase();
        if (biomeName.contains("OCEAN") || biomeName.contains("RIVER") || biomeName.contains("SWAMP")) {
            humidity += 15;
        } else if (biomeName.contains("DESERT") || biomeName.contains("BADLANDS")) {
            humidity -= 20;
        } else if (biomeName.contains("JUNGLE")) {
            humidity += 10;
        }

        int nearbyWater = countNearbyBlocks(loc, Material.WATER, 5);
        humidity += nearbyWater * 2;

        humidity = Math.max(0, Math.min(100, humidity));

        return Math.round(humidity * 10.0) / 10.0;
    }

    /**
     * 计算光照等级
     * @param loc 位置
     * @param world 世界
     * @return 光照等级（0-15）
     */
    public int calculateLightLevel(Location loc, World world) {
        Block block = loc.getBlock();
        int lightLevel = block.getLightLevel();

        if (world.getEnvironment() == World.Environment.NETHER) {
            return 10;
        } else if (world.getEnvironment() == World.Environment.THE_END) {
            return 8;
        }

        return lightLevel;
    }

    /**
     * 计算风速（基于天气和高度）
     * @param loc 位置
     * @param world 世界
     * @return 风速（米/秒，范围0-30）
     */
    public double calculateWindSpeed(Location loc, World world) {
        double windSpeed = 2.0;

        if (world.isThundering()) {
            windSpeed = 15.0 + Math.random() * 10;
        } else if (world.hasStorm()) {
            windSpeed = 8.0 + Math.random() * 5;
        }

        int y = loc.getBlockY();
        if (y > 64) {
            windSpeed += (y - 64) * 0.1;
        }

        if (world.getEnvironment() == World.Environment.THE_END) {
            windSpeed *= 0.5;
        } else if (world.getEnvironment() == World.Environment.NETHER) {
            windSpeed *= 0.3;
        }

        windSpeed = Math.max(0, Math.min(30, windSpeed));

        return Math.round(windSpeed * 10.0) / 10.0;
    }

    /**
     * 计算周围资源占比
     * @param loc 位置
     * @param world 世界
     * @return 资源占比数据
     */
    public Map<String, Double> calculateResourceRatio(Location loc, World world) {
        Map<String, Integer> resourceCounts = new HashMap<>();
        int totalBlocks = 0;

        int startX = loc.getBlockX() - scanRadius;
        int startY = Math.max(0, loc.getBlockY() - heightRange);
        int endY = Math.min(world.getMaxHeight() - 1, loc.getBlockY() + heightRange);
        int startZ = loc.getBlockZ() - scanRadius;

        for (int x = startX; x <= startX + scanRadius * 2; x += 2) {
            for (int y = startY; y <= endY; y += 2) {
                for (int z = startZ; z <= startZ + scanRadius * 2; z += 2) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();

                    if (type != Material.AIR && type != Material.CAVE_AIR && type != Material.VOID_AIR) {
                        totalBlocks++;
                        String category = categorizeMaterial(type);
                        resourceCounts.merge(category, 1, Integer::sum);
                    }
                }
            }
        }

        Map<String, Double> ratios = new HashMap<>();
        if (totalBlocks > 0) {
            for (Map.Entry<String, Integer> entry : resourceCounts.entrySet()) {
                double ratio = (entry.getValue() * 100.0) / totalBlocks;
                ratios.put(entry.getKey(), Math.round(ratio * 10.0) / 10.0);
            }
        }

        return ratios;
    }

    /**
     * 将材料分类
     * @param material 材料类型
     * @return 分类名称
     */
    private String categorizeMaterial(Material material) {
        String name = material.name();

        if (name.contains("ORE") || name.contains("DIAMOND") || name.contains("EMERALD") ||
            name.contains("GOLD") || name.contains("IRON") || name.contains("COAL") ||
            name.contains("REDSTONE") || name.contains("LAPIS") || name.contains("COPPER") ||
            name.contains("QUARTZ") || name.contains("ANCIENT_DEBRIS")) {
            return "ores";
        }
        if (name.contains("STONE") || name.contains("COBBLESTONE") || name.contains("GRAVEL") ||
            name.contains("SAND") || name.contains("DIRT") || name.contains("GRASS") ||
            name.contains("BEDROCK") || name.contains("NETHERRACK") || name.contains("END_STONE")) {
            return "building";
        }
        if (name.contains("LOG") || name.contains("WOOD") || name.contains("LEAVES") ||
            name.contains("PLANKS") || name.contains("SAPLING") || name.contains("WART")) {
            return "wood";
        }
        if (name.contains("WATER") || name.contains("LAVA") || name.contains("ICE") ||
            name.contains("SNOW") || name.contains("FROSTED")) {
            return "liquid";
        }
        if (name.contains("FLOWER") || name.contains("GRASS") || name.contains("FERN") ||
            name.contains("MUSHROOM") || name.contains("CROP") || name.contains("PLANT") ||
            name.contains("CACTUS") || name.contains("BAMBOO") || name.contains("SUGAR_CANE")) {
            return "plants";
        }
        if (name.contains("CHEST") || name.contains("BARREL") || name.contains("SHULKER") ||
            name.contains("FURNACE") || name.contains("BLAST_FURNACE") || name.contains("SMOKER") ||
            name.contains("BREWING_STAND") || name.contains("ANVIL") || name.contains("CRAFTING")) {
            return "utility";
        }

        return "other";
    }

    /**
     * 计算附近指定方块的数量
     * @param loc 中心位置
     * @param material 目标材料
     * @param radius 搜索半径
     * @return 方块数量
     */
    private int countNearbyBlocks(Location loc, Material material, int radius) {
        int count = 0;
        World world = loc.getWorld();
        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = cy - radius; y <= cy + radius; y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    if (world.getBlockAt(x, y, z).getType() == material) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    /**
     * 获取扫描半径
     * @return 扫描半径
     */
    public int getScanRadius() {
        return scanRadius;
    }

    /**
     * 获取高度范围
     * @return 高度范围
     */
    public int getHeightRange() {
        return heightRange;
    }

    /**
     * 根据生物群系获取基础温度
     * @param biome 生物群系
     * @return 基础温度（摄氏度）
     */
    private double getBiomeTemperature(Biome biome) {
        String biomeName = biome.name().toUpperCase();

        if (biomeName.contains("DESERT") || biomeName.contains("BADLANDS") || biomeName.contains("SAVANNA")) {
            return 35.0;
        } else if (biomeName.contains("JUNGLE")) {
            return 28.0;
        } else if (biomeName.contains("OCEAN") || biomeName.contains("RIVER") || biomeName.contains("SWAMP")) {
            return 22.0;
        } else if (biomeName.contains("FOREST") || biomeName.contains("PLAINS")) {
            return 20.0;
        } else if (biomeName.contains("TAIGA") || biomeName.contains("SNOWY") || biomeName.contains("ICE")) {
            return -10.0;
        } else if (biomeName.contains("MOUNTAINS") || biomeName.contains("HILLS")) {
            return 15.0;
        } else if (biomeName.contains("BEACH")) {
            return 25.0;
        } else if (biomeName.contains("MUSHROOM")) {
            return 18.0;
        } else if (biomeName.contains("CRIMSON") || biomeName.contains("WARPED")) {
            return 40.0;
        } else if (biomeName.contains("BASALT")) {
            return 45.0;
        } else if (biomeName.contains("SOUL")) {
            return 50.0;
        } else if (biomeName.contains("NETHER")) {
            return 45.0;
        } else if (biomeName.contains("END") || biomeName.contains("VOID")) {
            return 0.0;
        } else if (biomeName.contains("CAVE") || biomeName.contains("DEEP")) {
            return 10.0;
        } else if (biomeName.contains("LUKEWARM") || biomeName.contains("WARM")) {
            return 30.0;
        } else if (biomeName.contains("COLD") || biomeName.contains("FROZEN")) {
            return -5.0;
        } else {
            return 20.0;
        }
    }

    /**
     * 根据生物群系获取基础湿度
     * @param biome 生物群系
     * @return 基础湿度（百分比）
     */
    private double getBiomeHumidity(Biome biome) {
        String biomeName = biome.name().toUpperCase();

        if (biomeName.contains("DESERT") || biomeName.contains("BADLANDS") || biomeName.contains("SAVANNA")) {
            return 10.0;
        } else if (biomeName.contains("JUNGLE")) {
            return 85.0;
        } else if (biomeName.contains("OCEAN") || biomeName.contains("RIVER") || biomeName.contains("BEACH")) {
            return 90.0;
        } else if (biomeName.contains("SWAMP")) {
            return 95.0;
        } else if (biomeName.contains("FOREST")) {
            return 60.0;
        } else if (biomeName.contains("PLAINS")) {
            return 50.0;
        } else if (biomeName.contains("TAIGA") || biomeName.contains("SNOWY") || biomeName.contains("ICE")) {
            return 70.0;
        } else if (biomeName.contains("MOUNTAINS") || biomeName.contains("HILLS")) {
            return 40.0;
        } else if (biomeName.contains("MUSHROOM")) {
            return 75.0;
        } else if (biomeName.contains("CRIMSON") || biomeName.contains("WARPED") || biomeName.contains("NETHER")) {
            return 5.0;
        } else if (biomeName.contains("END") || biomeName.contains("VOID")) {
            return 20.0;
        } else if (biomeName.contains("CAVE") || biomeName.contains("DEEP")) {
            return 30.0;
        } else if (biomeName.contains("LUKEWARM") || biomeName.contains("WARM")) {
            return 80.0;
        } else if (biomeName.contains("COLD") || biomeName.contains("FROZEN")) {
            return 75.0;
        } else {
            return 50.0;
        }
    }
}
