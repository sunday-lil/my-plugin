package org.ljcode.myPlugin.managers;

import org.bukkit.scheduler.BukkitRunnable;
import org.ljcode.myPlugin.MyPlugin;

import java.util.List;

public class AnnouncementScheduler {
    private final MyPlugin plugin;
    private final AnnouncementManager announcementManager;
    private BukkitRunnable scheduledTask;

    public AnnouncementScheduler(MyPlugin plugin, AnnouncementManager announcementManager) {
        this.plugin = plugin;
        this.announcementManager = announcementManager;
    }

    public void startScheduling() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
        }

        int interval = plugin.getConfig().getInt("announcements.interval", 60) * 20; // Convert seconds to ticks
        
        scheduledTask = new BukkitRunnable() {
            @Override
            public void run() {
                sendScheduledAnnouncement();
            }
        };

        // 首次延迟1秒，避免启动/重载瞬间立刻广播
        scheduledTask.runTaskTimer(plugin, 20L, interval);
    }

    private void sendScheduledAnnouncement() {
        if (!plugin.getConfig().getBoolean("announcements.enabled", true)) {
            return;
        }
        // Get announcement types to send based on config
        List<String> messages = plugin.getConfig().getStringList("announcements.messages");
        List<String> actionBarMessages = plugin.getConfig().getStringList("announcements.actionbar-messages");
        // title-messages 是 List<List<String>>，不能用 getStringList（会跳过嵌套列表）
        List<?> titleMessages = plugin.getConfig().getList("announcements.title-messages");
        List<String> bossBarMessages = plugin.getConfig().getStringList("announcements.bossbar.messages");
        boolean bossBarEnabled = plugin.getConfig().getBoolean("announcements.bossbar.enabled", true);

        boolean randomOrder = plugin.getConfig().getBoolean("announcements.random-order", true);

        // Send chat announcement if available
        if (!messages.isEmpty()) {
            String message;
            if (randomOrder) {
                message = announcementManager.getRandomMessage(messages);
            } else {
                message = announcementManager.getSequentialMessage(messages);
            }
            if (!message.isEmpty()) {
                announcementManager.sendChatAnnouncement(message);
            }
        }

        // Send action bar announcement if available
        if (!actionBarMessages.isEmpty()) {
            String message;
            if (randomOrder) {
                message = announcementManager.getRandomMessage(actionBarMessages);
            } else {
                message = announcementManager.getSequentialMessage(actionBarMessages);
            }
            if (!message.isEmpty()) {
                announcementManager.sendActionBarAnnouncement(message);
            }
        }

        // Title messages are a bit different - they come as lists of [title, subtitle]
        if (titleMessages != null && !titleMessages.isEmpty()) {
            Object messageObj;
            if (randomOrder) {
                messageObj = announcementManager.getRandomObject(titleMessages);
            } else {
                messageObj = announcementManager.getSequentialObject(titleMessages);
            }
            
            if (messageObj instanceof List) {
                List<?> titleParts = (List<?>) messageObj;
                if (titleParts.size() >= 2) {
                    String title = titleParts.get(0).toString();
                    String subtitle = titleParts.get(1).toString();
                    announcementManager.sendTitleAnnouncement(title, subtitle);
                }
            }
        }

        // Send BossBar announcement if available and enabled
        if (bossBarEnabled && !bossBarMessages.isEmpty()) {
            String message;
            if (randomOrder) {
                message = announcementManager.getRandomMessage(bossBarMessages);
            } else {
                message = announcementManager.getSequentialMessage(bossBarMessages);
            }
            if (!message.isEmpty()) {
                announcementManager.sendBossBarAnnouncement(message);
            }
        }
    }

    public void stopScheduling() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
    }
}