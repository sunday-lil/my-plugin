package org.ljcode.myPlugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;
import org.ljcode.myPlugin.managers.AnnouncementManager;

public class AnnouncementCommand implements CommandExecutor {
    private final MyPlugin plugin;
    private final AnnouncementManager announcementManager;

    public AnnouncementCommand(MyPlugin plugin) {
        this.plugin = plugin;
        this.announcementManager = plugin.getAnnouncementManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 移除前缀"e"，以支持带前缀和不带前缀的命令
        String cleanLabel = label.replaceFirst("^e", "");
        
        if (cleanLabel.equalsIgnoreCase("anreload")) {
            return handleAnReload(sender, args);
        } else if (cleanLabel.equalsIgnoreCase("an")) {
            return handleAnCommand(sender, args);
        } else if (cleanLabel.equalsIgnoreCase("anactionbar")) {
            return handleAnActionBarCommand(sender, args);
        } else if (cleanLabel.equalsIgnoreCase("antitle")) {
            return handleAnTitleCommand(sender, args);
        } else if (cleanLabel.equalsIgnoreCase("anbossbar")) {
            return handleAnBossBarCommand(sender, args);
        }

        return false;
    }

    // /anreload 命令
    private boolean handleAnReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chatannouncements.reload")) {
            sender.sendMessage("§c你没有权限执行此命令！");
            return true;
        }

        plugin.reloadConfig();
        announcementManager.reloadConfig();

        // 重新加载K10数字孪生系统配置（含监听器配置引用同步）
        if (plugin.getK10TCPManager() != null) {
            plugin.syncK10Config();
            sender.sendMessage("§a[K10数字孪生] 配置已重新加载 - 新目标: " +
                plugin.getK10TCPManager().getCurrentHost());
        }

        sender.sendMessage("§a公告插件配置已重新加载！");
        return true;
    }

    // /an <message> 或 /an <player> <message> 命令
    private boolean handleAnCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chatannouncements.send.chat")) {
            sender.sendMessage("§c你没有权限发送公告消息！");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§c用法: /an <message>");
            sender.sendMessage("§c用法: /an <player> <message>");
            return true;
        }

        if (args.length == 1) {
            // 发送给所有玩家
            String message = String.join(" ", args);
            announcementManager.sendChatAnnouncement(message);
            sender.sendMessage("§a已向所有在线玩家发送公告消息！");
        } else {
            // 可能是指定玩家
            Player targetPlayer = plugin.getServer().getPlayerExact(args[0]);
            if (targetPlayer != null) {
                // 发送给指定玩家
                String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                announcementManager.sendChatAnnouncement(targetPlayer, message);
                sender.sendMessage("§a已向玩家 " + targetPlayer.getName() + " 发送公告消息！");
            } else {
                // 发送给所有玩家（参数不是有效玩家名）
                String message = String.join(" ", args);
                announcementManager.sendChatAnnouncement(message);
                sender.sendMessage("§a已向所有在线玩家发送公告消息！");
            }
        }

        return true;
    }

    // /anactionbar <message> 或 /anactionbar <player> <message> 命令
    private boolean handleAnActionBarCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chatannouncements.send.actionbar")) {
            sender.sendMessage("§c你没有权限发送动作栏消息！");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§c用法: /anactionbar <message>");
            sender.sendMessage("§c用法: /anactionbar <player> <message>");
            return true;
        }

        if (args.length == 1) {
            // 发送给所有玩家
            String message = String.join(" ", args);
            announcementManager.sendActionBarAnnouncement(message);
            sender.sendMessage("§a已向所有在线玩家发送动作栏消息！");
        } else {
            // 可能是指定玩家
            Player targetPlayer = plugin.getServer().getPlayerExact(args[0]);
            if (targetPlayer != null) {
                // 发送给指定玩家
                String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                announcementManager.sendActionBarAnnouncement(targetPlayer, message);
                sender.sendMessage("§a已向玩家 " + targetPlayer.getName() + " 发送动作栏消息！");
            } else {
                // 发送给所有玩家（参数不是有效玩家名）
                String message = String.join(" ", args);
                announcementManager.sendActionBarAnnouncement(message);
                sender.sendMessage("§a已向所有在线玩家发送动作栏消息！");
            }
        }

        return true;
    }

    // /antitle <title> | <subtitle> 或 /antitle <player> <title> | <subtitle> 命令
    private boolean handleAnTitleCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chatannouncements.send.title")) {
            sender.sendMessage("§c你没有权限发送标题消息！");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§c用法: /antitle <title> | <subtitle>");
            sender.sendMessage("§c用法: /antitle <player> <title> | <subtitle>");
            return true;
        }

        String title = "";
        String subtitle = "";

        if (args.length >= 1) {
            // 检查是否有玩家参数
            Player targetPlayer = plugin.getServer().getPlayerExact(args[0]);
            int startIndex = 0;
            
            if (targetPlayer != null) {
                startIndex = 1;
            } else {
                targetPlayer = null; // 没有找到玩家，将目标设为null
            }

            if (startIndex >= args.length) {
                sender.sendMessage("§c用法: /antitle <title> | <subtitle>");
                sender.sendMessage("§c用法: /antitle <player> <title> | <subtitle>");
                return true;
            }

            // 将参数组合成完整字符串，然后分割标题和副标题
            String fullArgs = String.join(" ", java.util.Arrays.copyOfRange(args, startIndex, args.length));
            
            // 查找分隔符 |
            int delimiterIndex = fullArgs.indexOf(" | ");
            if (delimiterIndex != -1) {
                title = fullArgs.substring(0, delimiterIndex).trim();
                subtitle = fullArgs.substring(delimiterIndex + 3).trim(); // +3 to skip " | "
            } else {
                title = fullArgs.trim();
                subtitle = ""; // 副标题是可选的
            }

            if (targetPlayer != null) {
                // 发送给指定玩家
                announcementManager.sendTitleAnnouncement(targetPlayer, title, subtitle);
                sender.sendMessage("§a已向玩家 " + targetPlayer.getName() + " 发送标题消息！");
            } else {
                // 发送给所有玩家
                announcementManager.sendTitleAnnouncement(title, subtitle);
                sender.sendMessage("§a已向所有在线玩家发送标题消息！");
            }
        }

        return true;
    }

    // /anbossbar <message> 或 /anbossbar <player> <message> 命令
    private boolean handleAnBossBarCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chatannouncements.send.bossbar")) {
            sender.sendMessage("§c你没有权限发送BossBar消息！");
            return true;
        }

        // 检查配置是否启用了BossBar功能
        if (!plugin.getConfig().getBoolean("announcements.bossbar.enabled", false)) {
            sender.sendMessage("§c服务器未启用BossBar功能！请在配置中设置 announcements.bossbar.enabled: true");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§c用法: /anbossbar <message>");
            sender.sendMessage("§c用法: /anbossbar <player> <message>");
            return true;
        }

        if (args.length == 1) {
            // 发送给所有玩家
            String message = String.join(" ", args);
            announcementManager.sendBossBarAnnouncement(message);
            sender.sendMessage("§a已向所有在线玩家发送BossBar消息！");
        } else {
            // 可能是指定玩家
            Player targetPlayer = plugin.getServer().getPlayerExact(args[0]);
            if (targetPlayer != null) {
                // 发送给指定玩家
                String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                announcementManager.sendBossBarAnnouncement(targetPlayer, message);
                sender.sendMessage("§a已向玩家 " + targetPlayer.getName() + " 发送BossBar消息！");
            } else {
                // 发送给所有玩家（参数不是有效玩家名）
                String message = String.join(" ", args);
                announcementManager.sendBossBarAnnouncement(message);
                sender.sendMessage("§a已向所有在线玩家发送BossBar消息！");
            }
        }

        return true;
    }
}