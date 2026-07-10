package org.ljcode.myPlugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;

public class PlayerCommand implements CommandExecutor {
    
    private final MyPlugin plugin;
    
    public PlayerCommand(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cleanLabel = label.toLowerCase().replaceFirst("^e", "");
        Player targetPlayer;
        
        if (args.length > 0) {
            // 对其他玩家操作
            if (!sender.hasPermission("essentialsx." + cleanLabel + ".others")) {
                sender.sendMessage(ChatColor.RED + "你没有权限对其他玩家使用此命令!");
                return true;
            }
            
            targetPlayer = Bukkit.getPlayer(args[0]);
            if (targetPlayer == null) {
                sender.sendMessage(ChatColor.RED + "玩家未找到或不在线!");
                return true;
            }
        } else {
            // 对自己操作
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
                return true;
            }
            
            if (!sender.hasPermission("essentialsx." + cleanLabel)) {
                sender.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
                return true;
            }
            
            targetPlayer = (Player) sender;
        }
        
        switch (cleanLabel) {
            case "fly":
                return handleFly(sender, targetPlayer);
            case "god":
                return handleGod(sender, targetPlayer);
            case "heal":
                return handleHeal(sender, targetPlayer);
            case "feed":
                return handleFeed(sender, targetPlayer);
            case "gm":
                return handleGameMode(sender, targetPlayer, args);
            default:
                sender.sendMessage(ChatColor.RED + "未知命令!");
                return true;
        }
    }
    
    private boolean handleFly(CommandSender sender, Player target) {
        boolean newState = !target.getAllowFlight();
        target.setAllowFlight(newState);
        
        if (newState) {
            target.sendMessage(ChatColor.GREEN + "飞行模式已启用!");
            if (!sender.equals(target)) {
                sender.sendMessage(ChatColor.GREEN + "已为 " + target.getName() + " 启用飞行模式!");
            }
        } else {
            target.sendMessage(ChatColor.YELLOW + "飞行模式已禁用!");
            if (!sender.equals(target)) {
                sender.sendMessage(ChatColor.YELLOW + "已为 " + target.getName() + " 禁用飞行模式!");
            }
        }
        
        return true;
    }
    
    private boolean handleGod(CommandSender sender, Player target) {
        boolean newState = !target.isInvulnerable();
        target.setInvulnerable(newState);
        
        if (newState) {
            target.sendMessage(ChatColor.GREEN + "上帝模式已启用!");
            if (!sender.equals(target)) {
                sender.sendMessage(ChatColor.GREEN + "已为 " + target.getName() + " 启用上帝模式!");
            }
        } else {
            target.sendMessage(ChatColor.YELLOW + "上帝模式已禁用!");
            if (!sender.equals(target)) {
                sender.sendMessage(ChatColor.YELLOW + "已为 " + target.getName() + " 禁用上帝模式!");
            }
        }
        
        return true;
    }
    
    private boolean handleHeal(CommandSender sender, Player target) {
        target.setHealth(target.getMaxHealth());
        target.setFoodLevel(20);
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
    
    private boolean handleGameMode(CommandSender sender, Player target, String[] args) {
        if (args.length < 1 && sender.equals(target)) {
            sender.sendMessage(ChatColor.RED + "用法: /gm <0|1|2|3> [玩家]");
            return true;
        }
        
        String modeStr = args.length > 0 ? args[0] : "";
        
        GameMode gameMode;
        try {
            int modeInt = Integer.parseInt(modeStr);
            gameMode = GameMode.getByValue(modeInt);
            if (gameMode == null) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "无效的游戏模式! 使用 0(生存), 1(创造), 2(冒险), 3(旁观)");
            return true;
        }
        
        target.setGameMode(gameMode);
        
        String modeName = getGameModeName(gameMode);
        target.sendMessage(ChatColor.GREEN + "游戏模式已设置为 " + modeName + "!");
        if (!sender.equals(target)) {
            sender.sendMessage(ChatColor.GREEN + "已将 " + target.getName() + " 的游戏模式设置为 " + modeName + "!");
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