package org.ljcode.myPlugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.ljcode.myPlugin.MyPlugin;

import java.util.Random;

/**
 * 核弹法阵命令处理器
 * 以玩家为圆心创建法阵特效并召唤点燃的TNT
 */
public class EnukeCommand implements CommandExecutor {

    private final MyPlugin plugin;
    private final Random random;

    // 法阵配置参数
    private final double CIRCLE_RADIUS = 15.0;      // 法阵半径
    private final int PARTICLE_COUNT = 100;        // 粒子数量  
    private final int TNT_COUNT = 10;              // TNT数量
    private final double TNT_SPAWN_RADIUS = 14.0;   // TNT生成半径
    private final int EFFECT_DURATION = 100;       // 特效持续时间(ticks)
    private final double ROTATION_SPEED = 0.05;     // 旋转速度

    /**
     * 构造函数
     *
     * @param plugin 插件主类实例
     */
    public EnukeCommand(MyPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    /**
     * 命令执行方法
     *
     * @param sender 命令发送者
     * @param command 命令对象
     * @param label 命令标签
     * @param args 命令参数
     * @return 是否执行成功
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 检查发送者是否为玩家
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家才能使用此命令！");
            return true;
        }

        Player player = (Player) sender;

        // 检查玩家权限
        if (!player.hasPermission("myplugin.enuke")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
            return true;
        }

        // 发送开始消息
        player.sendMessage(ChatColor.RED + "⚠ " + ChatColor.YELLOW + "核弹法阵启动中...");

        // 开始法阵特效
        startEnukeEffect(player);

        return true;
    }

    /**
     * 启动核弹法阵特效
     *
     * @param player 触发法阵的玩家
     */
    private void startEnukeEffect(Player player) {
        Location center = player.getLocation();
        World world = player.getWorld();

        // 创建法阵特效任务
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                // 绘制法阵圆形
                drawMagicCircle(center, world, tick);

                // 每隔一段时间生成TNT
                if (tick % 20 == 0 && tick < EFFECT_DURATION - 20) {
                    spawnRandomTNT(center, world);
                }

                tick++;

                // 特效结束
                if (tick >= EFFECT_DURATION) {
                    // 最终爆炸效果
                    createFinalExplosion(center, world);
                    player.sendMessage(ChatColor.RED + "💥 " + ChatColor.YELLOW + "核弹法阵完成！");
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // 每tick执行一次
    }

    /**
     * 绘制法阵圆形特效
     *
     * @param center 圆心位置
     * @param world 世界
     * @param tick 当前tick数
     */
    private void drawMagicCircle(Location center, World world, int tick) {
        double rotation = tick * ROTATION_SPEED;

        // 第一层：外层红色法阵（旋转）
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double angle = 2 * Math.PI * i / PARTICLE_COUNT + rotation;
            double x = CIRCLE_RADIUS * Math.cos(angle);
            double z = CIRCLE_RADIUS * Math.sin(angle);

            Location particleLoc = center.clone().add(x, 0.5, z);
            // 修复1：FLAME粒子 - 严格匹配参数类型（double转float）
            world.spawnParticle(Particle.FLAME, particleLoc, 1,
                    0.0d, 0.0d, 0.0d, 0.1d);
            // 修复2：DUST粒子 - 1.21+ 正确参数顺序（count, offsetX, offsetY, offsetZ, extra, data）
            Particle.DustOptions redDust = new Particle.DustOptions(org.bukkit.Color.RED, 1.0f);
            world.spawnParticle(Particle.DUST, particleLoc, 1,
                    0.0d, 0.0d, 0.0d, 0.0d, redDust);
        }

        // 第二层：蓝色光环
        for (int i = 0; i < 80; i++) {
            double angle = 2 * Math.PI * i / 80 + rotation;
            double x = (CIRCLE_RADIUS - 1) * Math.cos(angle);
            double z = (CIRCLE_RADIUS - 1) * Math.sin(angle);

            Location particleLoc = center.clone().add(x, 0.5, z);
            world.spawnParticle(Particle.END_ROD, particleLoc, 1,
                    0.0d, 0.0d, 0.0d, 0.1d);
            Particle.DustOptions blueDust = new Particle.DustOptions(org.bukkit.Color.BLUE, 1.0f);
            world.spawnParticle(Particle.DUST, particleLoc, 1,
                    0.0d, 0.0d, 0.0d, 0.0d, blueDust);
        }

        // 第三层：紫色符文环（快速旋转）
        for (int i = 0; i < 60; i++) {
            double angle = 2 * Math.PI * i / 60 + rotation * 2;
            double x = (CIRCLE_RADIUS - 3) * Math.cos(angle);
            double z = (CIRCLE_RADIUS - 3) * Math.sin(angle);

            Location particleLoc = center.clone().add(x, 0.5, z);
            world.spawnParticle(Particle.DRAGON_BREATH, particleLoc, 1,
                    0.0d, 0.0d, 0.0d, 0.1d,1.0f);
            Particle.DustOptions purpleDust = new Particle.DustOptions(org.bukkit.Color.PURPLE, 1.5f);
            world.spawnParticle(Particle.DUST, particleLoc, 1,
                    0.0d, 0.0d, 0.0d, 0.0d, purpleDust);
        }

        // 第四层：金色能量环（慢速旋转）
        for (int i = 0; i < 40; i++) {
            double angle = 2 * Math.PI * i / 40 - rotation * 0.5;
            double x = (CIRCLE_RADIUS - 5) * Math.cos(angle);
            double z = (CIRCLE_RADIUS - 5) * Math.sin(angle);

            Location particleLoc = center.clone().add(x, 0.5, z);
            world.spawnParticle(Particle.END_ROD, particleLoc, 1,
                    0.0d, 0.0d, 0.0d, 0.1d);
            Particle.DustOptions yellowDust = new Particle.DustOptions(org.bukkit.Color.YELLOW, 1.0f);
            world.spawnParticle(Particle.DUST, particleLoc, 1,
                    0.0d, 0.0d, 0.0d, 0.0d, yellowDust);
        }

        // 第五层：青色内环（中速旋转）
        for (int i = 0; i < 30; i++) {
            double angle = 2 * Math.PI * i / 30 + rotation * 1.2;
            double x = (CIRCLE_RADIUS - 7) * Math.cos(angle);
            double z = (CIRCLE_RADIUS - 7) * Math.sin(angle);

            Location particleLoc = center.clone().add(x, 0.5, z);
            world.spawnParticle(Particle.DRAGON_BREATH, particleLoc, 1,
                    0.0d, 0.0d, 0.0d, 0.1d,1.0f);
            Particle.DustOptions aquaDust = new Particle.DustOptions(org.bukkit.Color.AQUA, 1.0f);
            world.spawnParticle(Particle.DUST, particleLoc, 1,
                    0.0d, 0.0d, 0.0d, 0.0d, aquaDust);
        }

        // 绘制六芒星图案
        drawHexagram(center, world, rotation);

        // 绘制能量波纹
        drawEnergyRipple(center, world, tick);

        // 绘制符文点
        drawRunePoints(center, world, rotation);

        // 中心点特效
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.5, 0), 5,
                0.0d, 0.0d, 0.0d, 0.1d);
        world.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 1, 0), 3,
                0.0d, 0.0d, 0.0d, 0.1d);

        // 垂直能量柱
        drawEnergyColumn(center, world, tick);
    }

    /**
     * 绘制六芒星图案
     */
    private void drawHexagram(Location center, World world, double rotation) {
        double radius = CIRCLE_RADIUS - 4;

        for (int i = 0; i < 6; i++) {
            double angle1 = 2 * Math.PI * i / 6 + rotation;
            double angle2 = 2 * Math.PI * (i + 2) / 6 + rotation;

            double x1 = radius * Math.cos(angle1);
            double z1 = radius * Math.sin(angle1);
            double x2 = radius * Math.cos(angle2);
            double z2 = radius * Math.sin(angle2);

            for (int j = 0; j <= 20; j++) {
                double t = j / 20.0;
                double x = x1 + (x2 - x1) * t;
                double z = z1 + (z2 - z1) * t;

                Location particleLoc = center.clone().add(x, 0.5, z);
                Particle.DustOptions whiteDust = new Particle.DustOptions(org.bukkit.Color.WHITE, 1.0f);
                world.spawnParticle(Particle.DUST, particleLoc, 1,
                        0.0d, 0.0d, 0.0d, 0.0d, whiteDust);
            }
        }
    }

    /**
     * 绘制能量波纹
     *
     * @param center 圆心位置
     * @param world 世界
     * @param tick 当前tick数
     */
    private void drawEnergyRipple(Location center, World world, int tick) {
        double rippleRadius = (tick % 30) * 0.3;
        float alpha = 1.0f - (tick % 30) / 30.0f;

        if (rippleRadius < CIRCLE_RADIUS) {
            for (int i = 0; i < 60; i++) {
                double angle = 2 * Math.PI * i / 60;
                double x = rippleRadius * Math.cos(angle);
                double z = rippleRadius * Math.sin(angle);

                Location particleLoc = center.clone().add(x, 0.5, z);
                org.bukkit.Color rippleColor = org.bukkit.Color.fromRGB(
                        (int)(255 * alpha),
                        (int)(100 * alpha),
                        (int)(200 * alpha)
                );
                Particle.DustOptions rippleDust = new Particle.DustOptions(rippleColor, 1.0f);
                world.spawnParticle(Particle.DUST, particleLoc, 1,
                        0.0d, 0.0d, 0.0d, 0.0d, rippleDust);
            }
        }
    }

    /**
     * 绘制符文点
     */
    private void drawRunePoints(Location center, World world, double rotation) {
        int runeCount = 8;
        double runeRadius = CIRCLE_RADIUS - 2;

        for (int i = 0; i < runeCount; i++) {
            double angle = 2 * Math.PI * i / runeCount + rotation;
            double x = runeRadius * Math.cos(angle);
            double z = runeRadius * Math.sin(angle);

            Location particleLoc = center.clone().add(x, 0.5, z);

            world.spawnParticle(Particle.ENCHANT, particleLoc, 3,
                    0.2d, 0.2d, 0.2d, 0.1d);
            Particle.DustOptions goldDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 215, 0), 1.5f);
            world.spawnParticle(Particle.DUST, particleLoc, 2,
                    0.0d, 0.0d, 0.0d, 0.0d, goldDust);
        }
    }

    /**
     * 绘制垂直能量柱
     *
     * @param center 圆心位置
     * @param world 世界
     * @param tick 当前tick数
     */
    private void drawEnergyColumn(Location center, World world, int tick) {
        for (double y = 0; y <= 5; y += 0.5) {
            double radius = 1 + y * 0.3;

            for (int i = 0; i < 12; i++) {
                double angle = 2 * Math.PI * i / 12 + tick * 0.1;
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);

                Location particleLoc = center.clone().add(x, y, z);

                if (y < 2) {
                    world.spawnParticle(Particle.FLAME, particleLoc, 1,
                            0.0d, 0.0d, 0.0d, 0.1d);
                } else if (y < 4) {
                    world.spawnParticle(Particle.END_ROD, particleLoc, 1,
                            0.0d, 0.0d, 0.0d, 0.1d);
                } else {
                    world.spawnParticle(Particle.DRAGON_BREATH, particleLoc, 1,
                            0.0d, 0.0d, 0.0d, 0.1d,1.0f);
                }
            }
        }
    }

    /**
     * 在法阵范围内随机生成点燃的TNT
     *
     * @param center 圆心位置
     * @param world 世界
     */
    private void spawnRandomTNT(Location center, World world) {
        for (int i = 0; i < TNT_COUNT; i++) {
            // 随机位置
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * TNT_SPAWN_RADIUS;
            double x = distance * Math.cos(angle);
            double z = distance * Math.sin(angle);

            Location spawnLoc = center.clone().add(x, 0.5, z);

            // 生成点燃的TNT
            TNTPrimed tnt = (TNTPrimed) world.spawnEntity(spawnLoc, EntityType.TNT);
            tnt.setFuseTicks(40 + random.nextInt(20)); // 随机引爆时间

            // 添加随机速度使TNT飞散
            Vector velocity = new Vector(
                    (random.nextDouble() - 0.5) * 0.5,
                    random.nextDouble() * 0.8 + 0.2,
                    (random.nextDouble() - 0.5) * 0.5
            );
            tnt.setVelocity(velocity);
        }
    }

    /**
     * 创建最终爆炸效果
     *
     * @param center 圆心位置
     * @param world 世界
     */
    private void createFinalExplosion(Location center, World world) {
        // 主爆炸
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 10, 3d, 3d, 3d, 0d);
        world.spawnParticle(Particle.FLAME, center, 100, 5d, 5d, 5d, 0.1d);

        // 烟雾环
        for (int i = 0; i < 80; i++) {
            double angle = 2 * Math.PI * i / 80;
            double x = 8 * Math.cos(angle);
            double z = 8 * Math.sin(angle);

            Location particleLoc = center.clone().add(x, 0, z);
            world.spawnParticle(Particle.SMOKE, particleLoc, 5, 0.0d, 0.0d, 0.0d, 0.1d);
        }

        // 能量爆发环
        for (int i = 0; i < 60; i++) {
            double angle = 2 * Math.PI * i / 60;
            double x = 12 * Math.cos(angle);
            double z = 12 * Math.sin(angle);

            Location particleLoc = center.clone().add(x, 0, z);
            world.spawnParticle(Particle.DRAGON_BREATH, particleLoc, 3, 0.0d, 0.0d, 0.0d, 0.1d,1.0f);
            Particle.DustOptions purpleDust = new Particle.DustOptions(org.bukkit.Color.PURPLE, 2.0f);
            world.spawnParticle(Particle.DUST, particleLoc, 2, 0.0d, 0.0d, 0.0d, 0.0d, purpleDust);
        }

        // 垂直冲击波
        for (double y = 0; y <= 10; y += 0.5) {
            double radius = y * 1.5;

            for (int i = 0; i < 20; i++) {
                double angle = 2 * Math.PI * i / 20;
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);

                Location particleLoc = center.clone().add(x, y, z);

                if (y < 3) {
                    world.spawnParticle(Particle.FLAME, particleLoc, 2, 0.0d, 0.0d, 0.0d, 0.1d);
                } else if (y < 6) {
                    world.spawnParticle(Particle.LAVA, particleLoc, 1, 0.0d, 0.0d, 0.0d, 0.1d);
                } else {
                    world.spawnParticle(Particle.SMOKE, particleLoc, 2, 0.0d, 0.0d, 0.0d, 0.1d);
                }
            }
        }

        // 闪电效果
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * 10;
            double x = distance * Math.cos(angle);
            double z = distance * Math.sin(angle);

            Location particleLoc = center.clone().add(x, 0, z);
            world.spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 5, 0.0d, 0.0d, 0.0d, 0.1d);
        }

        // 魔法符文爆发
        for (int i = 0; i < 16; i++) {
            double angle = 2 * Math.PI * i / 16;
            double x = 15 * Math.cos(angle);
            double z = 15 * Math.sin(angle);

            Location particleLoc = center.clone().add(x, 0, z);
            world.spawnParticle(Particle.ENCHANT, particleLoc, 5, 0.3d, 0.3d, 0.3d, 0.1d);
            Particle.DustOptions goldDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 215, 0), 2.0f);
            world.spawnParticle(Particle.DUST, particleLoc, 3, 0.0d, 0.0d, 0.0d, 0.0d, goldDust);
        }
    }
}