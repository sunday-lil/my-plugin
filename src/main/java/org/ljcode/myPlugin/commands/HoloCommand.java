package org.ljcode.myPlugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HoloCommand implements CommandExecutor {
    
    private final Map<UUID, ArmorStand> followHolograms = new HashMap<>();
    private final Map<UUID, BukkitRunnable> followTasks = new HashMap<>();
    private final Map<UUID, ArmorStand> onlineHolograms = new HashMap<>();
    private final Map<UUID, BukkitRunnable> onlineTasks = new HashMap<>();
    
    private final Map<UUID, List<ArmorStand>> playerHolograms = new HashMap<>();
    private final Map<UUID, Long> hologramCreationTimes = new HashMap<>();
    
    private static final int MAX_HOLOGRAMS_PER_PLAYER = 20;
    private static final long HOLOGRAM_EXPIRY_TIME = 10 * 60 * 1000;
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "multiline":
                return handleMultiline(player, args);
            case "online":
                return handleOnline(player);
            case "follow":
                return handleFollow(player);
            case "stop":
                return handleStop(player);
            case "clear":
                return handleClear(player);
            case "clearall":
                return handleClearAll(player);
            case "list":
                return handleList(player);
            case "stats":
                return handleStats(player);
            default:
                return handleSimple(player, args);
        }
    }
    
    private boolean handleSimple(Player player, String[] args) {
        if (!checkHologramLimit(player)) {
            return true;
        }
        
        String text = ChatColor.translateAlternateColorCodes('&', String.join(" ", args));
        
        Location location = player.getLocation();
        ArmorStand armorStand = createArmorStand(location.clone(), text);
        
        trackHologram(player, armorStand);
        
        player.sendMessage(ChatColor.GREEN + "悬浮文字已创建: " + text);
        
        return true;
    }
    
    private boolean handleMultiline(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "用法: /holo multiline <行1> <行2> <行3>");
            return true;
        }
        
        if (!checkHologramLimit(player, 3)) {
            return true;
        }
        
        String line1 = ChatColor.translateAlternateColorCodes('&', args[1]);
        String line2 = ChatColor.translateAlternateColorCodes('&', args[2]);
        String line3 = ChatColor.translateAlternateColorCodes('&', args[3]);
        
        Location baseLocation = player.getLocation();
        
        ArmorStand as1 = createArmorStand(baseLocation.clone(), line1);
        ArmorStand as2 = createArmorStand(baseLocation.clone().add(0, -0.25, 0), line2);
        ArmorStand as3 = createArmorStand(baseLocation.clone().add(0, -0.5, 0), line3);
        
        trackHologram(player, as1);
        trackHologram(player, as2);
        trackHologram(player, as3);
        
        player.sendMessage(ChatColor.GREEN + "多行悬浮文字已创建!");
        return true;
    }
    
    private boolean handleOnline(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (onlineHolograms.containsKey(playerId)) {
            player.sendMessage(ChatColor.YELLOW + "你已经有一个在线人数显示，使用 /holo stop 来停止");
            return true;
        }
        
        Location location = player.getLocation();
        ArmorStand armorStand = createArmorStand(location.clone(), ChatColor.GREEN + "当前在线: " + ChatColor.WHITE + player.getServer().getOnlinePlayers().size() + " 人");
        
        onlineHolograms.put(playerId, armorStand);
        
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                int onlineCount = player.getServer().getOnlinePlayers().size();
                armorStand.setCustomName(ChatColor.GREEN + "当前在线: " + ChatColor.WHITE + onlineCount + " 人");
            }
        };
        
        task.runTaskTimer(player.getServer().getPluginManager().getPlugin("EssentialsX-Clone"), 100L, 100L);
        onlineTasks.put(playerId, task);
        
        player.sendMessage(ChatColor.GREEN + "动态在线人数显示已创建!");
        return true;
    }
    
    private boolean handleFollow(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (followHolograms.containsKey(playerId)) {
            player.sendMessage(ChatColor.YELLOW + "你已经有一个跟随显示，使用 /holo stop 来停止");
            return true;
        }
        
        Location location = player.getLocation().add(0, 1, 0);
        ArmorStand armorStand = createArmorStand(location, ChatColor.GOLD + "跟随我!");
        
        followHolograms.put(playerId, armorStand);
        
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }
                
                Location newLocation = player.getLocation().add(0, 1, 0);
                armorStand.teleport(newLocation);
            }
        };
        
        task.runTaskTimer(player.getServer().getPluginManager().getPlugin("EssentialsX-Clone"), 1L, 1L);
        followTasks.put(playerId, task);
        
        player.sendMessage(ChatColor.GREEN + "跟随悬浮文字已创建!");
        return true;
    }
    
    private boolean handleStop(Player player) {
        UUID playerId = player.getUniqueId();
        boolean stopped = false;
        
        if (followHolograms.containsKey(playerId)) {
            followHolograms.get(playerId).remove();
            followHolograms.remove(playerId);
            
            BukkitRunnable task = followTasks.get(playerId);
            if (task != null) {
                task.cancel();
                followTasks.remove(playerId);
            }
            
            player.sendMessage(ChatColor.GREEN + "跟随显示已停止");
            stopped = true;
        }
        
        if (onlineHolograms.containsKey(playerId)) {
            onlineHolograms.get(playerId).remove();
            onlineHolograms.remove(playerId);
            
            BukkitRunnable task = onlineTasks.get(playerId);
            if (task != null) {
                task.cancel();
                onlineTasks.remove(playerId);
            }
            
            player.sendMessage(ChatColor.GREEN + "在线人数显示已停止");
            stopped = true;
        }
        
        if (!stopped) {
            player.sendMessage(ChatColor.RED + "没有运行中的显示");
        }
        
        return true;
    }
    
    private ArmorStand createArmorStand(Location location, String text) {
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, org.bukkit.entity.EntityType.ARMOR_STAND);
        
        armorStand.setInvisible(true);
        armorStand.setGravity(false);
        armorStand.setCustomName(text);
        armorStand.setCustomNameVisible(true);
        armorStand.setMarker(true);
        
        return armorStand;
    }
    
    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== 悬浮文字帮助 ===");
        player.sendMessage(ChatColor.YELLOW + "/holo <文字>" + ChatColor.WHITE + " - 创建单行悬浮文字");
        player.sendMessage(ChatColor.YELLOW + "/holo multiline <行1> <行2> <行3>" + ChatColor.WHITE + " - 创建多行悬浮文字");
        player.sendMessage(ChatColor.YELLOW + "/holo online" + ChatColor.WHITE + " - 创建动态在线人数显示");
        player.sendMessage(ChatColor.YELLOW + "/holo follow" + ChatColor.WHITE + " - 创建跟随悬浮文字");
        player.sendMessage(ChatColor.YELLOW + "/holo stop" + ChatColor.WHITE + " - 停止所有动态显示");
        player.sendMessage(ChatColor.YELLOW + "/holo clear" + ChatColor.WHITE + " - 清除你的所有悬浮文字");
        player.sendMessage(ChatColor.YELLOW + "/holo list" + ChatColor.WHITE + " - 查看你的悬浮文字列表");
        player.sendMessage(ChatColor.YELLOW + "/holo stats" + ChatColor.WHITE + " - 查看悬浮文字统计信息");
        player.sendMessage(ChatColor.GRAY + "提示: 使用 & 符号来添加颜色代码");
    }
    
    private boolean checkHologramLimit(Player player) {
        return checkHologramLimit(player, 1);
    }
    
    private boolean checkHologramLimit(Player player, int count) {
        UUID playerId = player.getUniqueId();
        List<ArmorStand> holograms = playerHolograms.getOrDefault(playerId, new ArrayList<>());
        
        if (holograms.size() + count > MAX_HOLOGRAMS_PER_PLAYER) {
            player.sendMessage(ChatColor.RED + "你已达到悬浮文字上限 (" + MAX_HOLOGRAMS_PER_PLAYER + ")!");
            player.sendMessage(ChatColor.YELLOW + "使用 /holo clear 清除旧的悬浮文字");
            return false;
        }
        
        return true;
    }
    
    private void trackHologram(Player player, ArmorStand armorStand) {
        UUID playerId = player.getUniqueId();
        List<ArmorStand> holograms = playerHolograms.computeIfAbsent(playerId, k -> new ArrayList<>());
        holograms.add(armorStand);
        hologramCreationTimes.put(armorStand.getUniqueId(), System.currentTimeMillis());
    }
    
    private boolean handleClear(Player player) {
        UUID playerId = player.getUniqueId();
        List<ArmorStand> holograms = playerHolograms.get(playerId);
        
        if (holograms == null || holograms.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "你没有创建任何悬浮文字");
            return true;
        }
        
        int count = 0;
        for (ArmorStand armorStand : holograms) {
            if (armorStand.isValid()) {
                armorStand.remove();
                hologramCreationTimes.remove(armorStand.getUniqueId());
                count++;
            }
        }
        
        playerHolograms.remove(playerId);
        player.sendMessage(ChatColor.GREEN + "已清除 " + count + " 个悬浮文字");
        
        return true;
    }
    
    private boolean handleClearAll(Player player) {
        if (!player.hasPermission("essentialsx.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }
        
        int totalCleared = 0;
        
        for (List<ArmorStand> holograms : playerHolograms.values()) {
            for (ArmorStand armorStand : holograms) {
                if (armorStand.isValid()) {
                    armorStand.remove();
                    hologramCreationTimes.remove(armorStand.getUniqueId());
                    totalCleared++;
                }
            }
        }
        
        playerHolograms.clear();
        
        for (ArmorStand armorStand : followHolograms.values()) {
            if (armorStand.isValid()) {
                armorStand.remove();
                totalCleared++;
            }
        }
        followHolograms.clear();
        
        for (ArmorStand armorStand : onlineHolograms.values()) {
            if (armorStand.isValid()) {
                armorStand.remove();
                totalCleared++;
            }
        }
        onlineHolograms.clear();
        
        for (BukkitRunnable task : followTasks.values()) {
            task.cancel();
        }
        followTasks.clear();
        
        for (BukkitRunnable task : onlineTasks.values()) {
            task.cancel();
        }
        onlineTasks.clear();
        
        player.sendMessage(ChatColor.GREEN + "已清除所有悬浮文字 (共 " + totalCleared + " 个)");
        player.getServer().broadcastMessage(ChatColor.YELLOW + player.getName() + " 清除了所有悬浮文字");
        
        return true;
    }
    
    private boolean handleList(Player player) {
        UUID playerId = player.getUniqueId();
        List<ArmorStand> holograms = playerHolograms.get(playerId);
        
        if (holograms == null || holograms.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "你没有创建任何悬浮文字");
            return true;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== 你的悬浮文字列表 ===");
        player.sendMessage(ChatColor.WHITE + "总计: " + holograms.size() + " 个");
        
        int index = 1;
        long currentTime = System.currentTimeMillis();
        
        for (ArmorStand armorStand : holograms) {
            if (armorStand.isValid()) {
                String name = armorStand.getCustomName();
                Long creationTime = hologramCreationTimes.get(armorStand.getUniqueId());
                long age = creationTime != null ? (currentTime - creationTime) / 1000 : 0;
                
                player.sendMessage(ChatColor.YELLOW + "#" + index + ": " + ChatColor.WHITE + 
                    (name != null ? name : "未命名") + ChatColor.GRAY + " (" + age + "秒前)");
                index++;
            }
        }
        
        return true;
    }
    
    private boolean handleStats(Player player) {
        if (!player.hasPermission("essentialsx.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }
        
        int totalHolograms = 0;
        int totalFollow = followHolograms.size();
        int totalOnline = onlineHolograms.size();
        
        for (List<ArmorStand> holograms : playerHolograms.values()) {
            totalHolograms += holograms.size();
        }
        
        player.sendMessage(ChatColor.GOLD + "=== 悬浮文字统计 ===");
        player.sendMessage(ChatColor.YELLOW + "静态悬浮文字: " + ChatColor.WHITE + totalHolograms);
        player.sendMessage(ChatColor.YELLOW + "跟随显示: " + ChatColor.WHITE + totalFollow);
        player.sendMessage(ChatColor.YELLOW + "在线人数显示: " + ChatColor.WHITE + totalOnline);
        player.sendMessage(ChatColor.YELLOW + "总计: " + ChatColor.WHITE + (totalHolograms + totalFollow + totalOnline));
        player.sendMessage(ChatColor.YELLOW + "活跃玩家: " + ChatColor.WHITE + player.getServer().getOnlinePlayers().size());
        
        return true;
    }
}