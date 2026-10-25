package org.ljcode.myPlugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;

import java.util.Locale;

/**
 * /efly /egod /eheal /efeed /egm（含 /gamemode 别名）。
 *
 * 参数约定：
 *   /efly [玩家]
 *   /egm <0|1|2|3> [玩家]
 */
public class PlayerCommand implements CommandExecutor {

    private final MyPlugin plugin;

    public PlayerCommand(MyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = normalizeLabel(label);

        if (!isEnabled(name)) {
            sender.sendMessage(ChatColor.RED + "该命令已在配置中禁用!");
            return true;
        }

        if ("gm".equals(name)) {
            return handleGameMode(sender, args);
        }

        // /efly [玩家] 等：args[0] 是可选目标玩家
        Player target = resolveTarget(sender, args, 0);
        if (target == null) {
            return true;
        }

        if (!checkPermission(sender, name, target)) {
            return true;
        }

        switch (name) {
            case "fly":
                return handleFly(sender, target);
            case "god":
                return handleGod(sender, target);
            case "heal":
                return handleHeal(sender, target);
            case "feed":
                return handleFeed(sender, target);
            default:
                sender.sendMessage(ChatColor.RED + "未知命令!");
                return true;
        }
    }

    private String normalizeLabel(String label) {
        String name = label.toLowerCase(Locale.ROOT);
        if ("gamemode".equals(name)) {
            return "gm";
        }
        return name.startsWith("e") ? name.substring(1) : name;
    }

    private boolean isEnabled(String name) {
        String keyName = "gm".equals(name) ? "gamemode" : name;
        return plugin.getConfig().getBoolean("player-commands." + keyName + "-enabled", true);
    }

    /**
     * @param index 目标玩家在 args 中的下标；-1 表示必须为自己
     */
    private Player resolveTarget(CommandSender sender, String[] args, int index) {
        if (index < 0 || args.length <= index) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令，或需要指定在线玩家!");
                return null;
            }
            return player;
        }

        Player target = Bukkit.getPlayerExact(args[index]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "玩家未找到或不在线!");
        }
        return target;
    }

    private boolean checkPermission(CommandSender sender, String name, Player target) {
        boolean self = sender instanceof Player player && player.equals(target);
        String permission = "essentialsx." + name + (self ? "" : ".others");
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此操作!");
            return false;
        }
        return true;
    }

    private boolean handleFly(CommandSender sender, Player target) {
        boolean newState = !target.getAllowFlight();
        target.setAllowFlight(newState);
        if (!newState) {
            target.setFlying(false);
        }

        target.sendMessage(newState ? ChatColor.GREEN + "飞行模式已启用!" : ChatColor.YELLOW + "飞行模式已禁用!");
        if (!sender.equals(target)) {
            sender.sendMessage((newState ? ChatColor.GREEN + "已为 " : ChatColor.YELLOW + "已为 ")
                    + target.getName() + (newState ? " 启用飞行模式!" : " 禁用飞行模式!"));
        }
        return true;
    }

    private boolean handleGod(CommandSender sender, Player target) {
        boolean newState = !target.isInvulnerable();
        target.setInvulnerable(newState);

        target.sendMessage(newState ? ChatColor.GREEN + "上帝模式已启用!" : ChatColor.YELLOW + "上帝模式已禁用!");
        if (!sender.equals(target)) {
            sender.sendMessage((newState ? ChatColor.GREEN + "已为 " : ChatColor.YELLOW + "已为 ")
                    + target.getName() + (newState ? " 启用上帝模式!" : " 禁用上帝模式!"));
        }
        return true;
    }

    private boolean handleHeal(CommandSender sender, Player target) {
        AttributeInstance maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double health = maxHealth != null ? maxHealth.getValue() : 20.0;
        target.setHealth(Math.min(health, target.getMaxHealth()));
        target.setFoodLevel(20);
        target.setSaturation(20);
        target.setFireTicks(0);

        target.sendMessage(ChatColor.GREEN + "你已被治愈!");
        if (!sender.equals(target)) {
            sender.sendMessage(ChatColor.GREEN + "已治愈 " + target.getName() + "!");
        }
        return true;
    }

    private boolean handleFeed(CommandSender sender, Player target) {
        target.setFoodLevel(20);
        target.setSaturation(20);

        target.sendMessage(ChatColor.GREEN + "你已被喂食!");
        if (!sender.equals(target)) {
            sender.sendMessage(ChatColor.GREEN + "已喂食 " + target.getName() + "!");
        }
        return true;
    }

    private boolean handleGameMode(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "用法: /egm <0|1|2|3> [玩家]");
            return true;
        }

        GameMode gameMode;
        try {
            int mode = Integer.parseInt(args[0]);
            GameMode[] modes = GameMode.values();
            if (mode < 0 || mode >= modes.length) {
                throw new NumberFormatException();
            }
            gameMode = modes[mode];
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "无效的游戏模式! 使用 0(生存), 1(创造), 2(冒险), 3(旁观)");
            return true;
        }

        Player target = resolveTarget(sender, args, 1);
        if (target == null) {
            return true;
        }

        if (!checkPermission(sender, "gm", target)) {
            return true;
        }

        target.setGameMode(gameMode);
        target.sendMessage(ChatColor.GREEN + "游戏模式已设置为 " + getGameModeName(gameMode) + "!");
        if (!sender.equals(target)) {
            sender.sendMessage(ChatColor.GREEN + "已将 " + target.getName() + " 的游戏模式设置为 "
                    + getGameModeName(gameMode) + "!");
        }
        return true;
    }

    private String getGameModeName(GameMode mode) {
        switch (mode) {
            case SURVIVAL: return "生存模式";
            case CREATIVE: return "创造模式";
            case ADVENTURE: return "冒险模式";
            case SPECTATOR: return "旁观模式";
            default: return "未知模式";
        }
    }
}
