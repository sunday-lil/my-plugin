package org.ljcode.myPlugin.listeners;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.ljcode.myPlugin.MyPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 脏话过滤事件监听器
 * 监听玩家聊天事件，检测并处理包含不当言论的消息
 * 实现违规次数累计和自动踢出功能
 */
public class ProfanityFilterListener implements Listener {
    
    // 插件主类实例，用于访问插件的各种功能和配置
    private final MyPlugin plugin;
    
    // 存储玩家违规次数，使用 UUID 避免改名/重名串号
    private final Map<UUID, Integer> playerViolationCount = new ConcurrentHashMap<>();
    
    // 从配置文件加载的设置项，用于动态调整过滤规则
    private boolean isEnabled;              // 是否启用脏话过滤功能
    private int violationThreshold;         // 违规踢出阈值（达到此次数后踢出玩家）
    private String warningMessage;          // 警告消息内容
    private String kickMessage;             // 踢出消息内容
    private volatile List<String> profanityList;     // 脏话词库列表
    
    /**
     * 构造函数，初始化脏话过滤监听器
     * 加载配置文件中的设置
     * 
     * @param plugin 插件主类实例
     */
    public ProfanityFilterListener(MyPlugin plugin) {
        this.plugin = plugin;
        loadConfig(); // 初始化时加载配置
    }
    
    /**
     * 从配置文件加载设置
     * 更新脏话过滤的各项配置参数
     */
    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        
        // 从配置文件读取是否启用脏话过滤
        isEnabled = config.getBoolean("profanity-filter.enabled", true);
        
        // 从配置文件读取违规踢出阈值
        violationThreshold = config.getInt("profanity-filter.violation-threshold", 3);
        
        // 从配置文件读取警告消息，并解析颜色代码
        warningMessage = ChatColor.translateAlternateColorCodes('&', 
            config.getString("profanity-filter.warning-message", "&c[系统警告] 检测到不当言论，请注意文明用语！"));
        
        // 从配置文件读取踢出消息，并解析颜色代码
        kickMessage = ChatColor.translateAlternateColorCodes('&', 
            config.getString("profanity-filter.kick-message", "&c由于多次发送不当言论，您已被踢出服务器！请注意网络礼仪！"));
        
        // 初始化脏话词库列表（先构建再一次性发布，避免异步聊天读到半成品）
        List<String> loadedWords = new ArrayList<>();
        
        // 从配置文件加载中文脏话词汇列表
        List<String> chineseWords = config.getStringList("profanity-filter.profanity-list.chinese");
        
        // 从配置文件加载英文脏话词汇列表
        List<String> englishWords = config.getStringList("profanity-filter.profanity-list.english");
        
        // 将中文和英文脏话词汇合并到总词库中
        loadedWords.addAll(chineseWords);
        loadedWords.addAll(englishWords);
        profanityList = loadedWords;
    }
    
    /**
     * 处理玩家聊天事件
     * 检测聊天消息是否包含不当言论，如有则执行相应的警告或踢出操作
     * 
     * @param event 异步玩家聊天事件对象
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // 检查脏话过滤功能是否在配置中启用
        if (!isEnabled) {
            return; // 如果未启用，则直接返回，不进行任何处理
        }
        
        // 获取发送消息的玩家对象
        Player player = event.getPlayer();
        
        // 获取玩家发送的消息内容
        String message = event.getMessage();
        
        // 检查消息是否包含脏话词汇
        if (containsProfanity(message)) {
            UUID uuid = player.getUniqueId();
            
            // 更新该玩家的违规次数
            int violations = playerViolationCount.getOrDefault(uuid, 0) + 1;
            playerViolationCount.put(uuid, violations);
            
            // 聊天事件是异步的，消息发送回主线程
            plugin.getServer().getScheduler().runTask(plugin, () -> player.sendMessage(warningMessage));
            
            // 检查违规次数是否达到踢出阈值
            if (violations >= violationThreshold) {
                // 违规次数达到阈值，将玩家踢出服务器
                // 使用同步任务来执行踢出操作，避免异步上下文问题
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.kickPlayer(kickMessage);
                    
                    // 记录踢出事件到服务器日志
                    plugin.getLogger().info("玩家 " + player.getName() + " 因违规次数过多(" + violations + ")被踢出服务器");
                    
                    // 清除该玩家的违规记录（因为已经被踢出）
                    playerViolationCount.remove(uuid);
                });
                
                // 取消原始聊天消息的发送
                event.setCancelled(true);
            } else {
                // 违规次数未达阈值，取消原始聊天消息的发送
                event.setCancelled(true);
                
                // 记录警告事件到服务器日志
                plugin.getLogger().info("玩家 " + player.getName() + " 发送不当言论(" + violations + "/" + violationThreshold + ")，已发送警告");
            }
        }
    }
    
    /**
     * 检查消息是否包含脏话
     * 将消息和脏话词库都转换为小写进行比较，以实现不区分大小写的匹配
     * 
     * @param message 待检查的消息
     * @return 如果包含脏话返回true，否则返回false
     */
    private boolean containsProfanity(String message) {
        // 将消息转换为小写以实现不区分大小写的匹配
        String lowerMessage = message.toLowerCase(Locale.ROOT);
        
        // 遍历脏话词库中的每个词汇
        for (String profanityWord : profanityList) {
            if (!profanityWord.isEmpty() && lowerMessage.contains(profanityWord.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        
        return false;
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerViolationCount.remove(event.getPlayer().getUniqueId());
    }
    
    /**
     * 重新加载配置（/eanreload 调用）。
     */
    public void reloadConfig() {
        loadConfig();
    }

    /** 清空所有玩家的违规记录。 */
    public void clearAllViolationCounts() {
        playerViolationCount.clear();
    }
}