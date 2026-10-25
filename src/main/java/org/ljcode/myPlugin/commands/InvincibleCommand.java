package org.ljcode.myPlugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * /我无敌了
 *
 * 第一次使用：获得所有有益 Buff，等级 255（amplifier 254），持续时间无限。
 * 再次使用：清空玩家当前所有药水效果。
 */
public class InvincibleCommand implements CommandExecutor {

    private static final int AMPLIFIER = 254; // 显示等级 255

    /** 只包含有益效果，避免把失明、缓慢、中毒等当作“无敌”。 */
    private static final List<PotionEffectType> BENEFICIAL_EFFECTS = List.of(
            PotionEffectType.SPEED,
            PotionEffectType.HASTE,
            PotionEffectType.STRENGTH,
            PotionEffectType.JUMP_BOOST,
            PotionEffectType.REGENERATION,
            PotionEffectType.RESISTANCE,
            PotionEffectType.FIRE_RESISTANCE,
            PotionEffectType.WATER_BREATHING,
            PotionEffectType.NIGHT_VISION,
            PotionEffectType.HEALTH_BOOST,
            PotionEffectType.ABSORPTION,
            PotionEffectType.SATURATION,
            PotionEffectType.LUCK,
            PotionEffectType.SLOW_FALLING,
            PotionEffectType.CONDUIT_POWER,
            PotionEffectType.DOLPHINS_GRACE,
            PotionEffectType.HERO_OF_THE_VILLAGE,
            PotionEffectType.INVISIBILITY
    );

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }

        if (!player.hasPermission("myplugin.invincible")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }

        if (hasInvincibleBuffs(player)) {
            // 清空全部效果，包括其它来源的 debuff
            for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
                player.removePotionEffect(effect.getType());
            }
            player.sendMessage(ChatColor.YELLOW + "已清空所有药水效果。");
            return true;
        }

        for (PotionEffectType type : BENEFICIAL_EFFECTS) {
            player.addPotionEffect(new PotionEffect(
                    type,
                    PotionEffect.INFINITE_DURATION,
                    AMPLIFIER,
                    false,  // ambient
                    false,  // particles
                    true    // icon
            ), true);
        }

        player.sendMessage(ChatColor.GREEN + "你无敌了！已获得全部有益 Buff（255级、无限时间）。");
        player.sendMessage(ChatColor.GRAY + "再次使用 /" + label + " 可清空所有效果。");
        return true;
    }

    private boolean hasInvincibleBuffs(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (BENEFICIAL_EFFECTS.contains(effect.getType())
                    && effect.getAmplifier() >= AMPLIFIER
                    && effect.getDuration() == PotionEffect.INFINITE_DURATION) {
                return true;
            }
        }
        return false;
    }
}
