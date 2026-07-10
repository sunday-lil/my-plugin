package org.ljcode.myPlugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.ljcode.myPlugin.MyPlugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * 玩家加入/退出事件监听器
 * 监听玩家加入和退出服务器事件，展示欢迎/告别消息、烟花特效等
 * 提供丰富的玩家加入体验
 */
public class JoinMessageListener implements Listener {
    
    // 插件主类实例，用于访问插件的各种功能和配置
    private final MyPlugin plugin;
    
    // 随机数生成器，用于生成随机的烟花效果等
    private final Random random;
    
    /**
     * 构造函数，初始化加入消息监听器
     * 
     * @param plugin 插件主类实例
     */
    public JoinMessageListener(MyPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random(); // 初始化随机数生成器
    }
    
    /**
     * 处理玩家加入事件
     * 根据配置显示欢迎标题、动作栏消息、聊天消息和烟花特效
     * 如果出现异常则回退到默认消息
     * 
     * @param event 玩家加入事件对象
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        try {
            // 取消默认的加入消息，使用自定义消息系统
            event.setJoinMessage(null);
            
            // 从配置文件读取各项功能的启用状态
            boolean titleEnabled = plugin.getConfig().getBoolean("join-message.title.enabled", true);        // 标题功能启用状态
            boolean actionBarEnabled = plugin.getConfig().getBoolean("join-message.action-bar.enabled", true); // 动作栏功能启用状态
            boolean chatMessageEnabled = plugin.getConfig().getBoolean("join-message.chat-message.enabled", true); // 聊天消息功能启用状态
            boolean fireworksEnabled = plugin.getConfig().getBoolean("join-message.fireworks.enabled", true); // 烟花功能启用状态
            
            // 如果标题功能启用，则显示欢迎标题
            if (titleEnabled) {
                showWelcomeTitle(player);
            }
            
            // 如果动作栏功能启用，则显示动作栏消息
            if (actionBarEnabled) {
                showActionBarMessage(player);
            }
            
            // 如果聊天消息功能启用，则发送聊天框消息
            if (chatMessageEnabled) {
                sendChatMessage(player, event);
            }
            
            // 如果烟花功能启用，则播放欢迎烟花特效
            if (fireworksEnabled) {
                spawnWelcomeFireworks(player);
            }
            
            // 发送功能使用指南
            sendHelpGuide(player);
            
        } catch (Exception e) {
            // 记录错误日志
            plugin.getLogger().warning("处理玩家加入事件时出错: " + e.getMessage());
            
            // 出现异常时回退到默认消息
            event.setJoinMessage(ChatColor.YELLOW + player.getName() + " 加入了游戏");
        }
    }
    
    /**
     * 处理玩家退出事件
     * 根据配置显示告别消息
     * 如果出现异常则回退到默认消息
     * 
     * @param event 玩家退出事件对象
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        try {
            // 取消默认的退出消息，使用自定义消息系统
            event.setQuitMessage(null);
            
            // 从配置文件读取退出消息功能的启用状态
            boolean quitMessageEnabled = plugin.getConfig().getBoolean("join-message.quit-message.enabled", true);
            
            // 如果退出消息功能启用，则发送告别消息
            if (quitMessageEnabled) {
                sendQuitMessage(event);
            }
            
        } catch (Exception e) {
            // 记录错误日志
            plugin.getLogger().warning("处理玩家退出事件时出错: " + e.getMessage());
            
            // 出现异常时回退到默认消息
            event.setQuitMessage(ChatColor.YELLOW + event.getPlayer().getName() + " 离开了游戏");
        }
    }
    
/**
     * 向玩家显示欢迎标题
     * 从配置中获取标题文本和样式设置，并发送给玩家
     * 
     * @param player 要发送标题的玩家
     */
    private void showWelcomeTitle(Player player) {
        // 从配置文件获取标题文本，默认为"欢迎回来!"
        String titleText = plugin.getConfig().getString("join-message.title.text", "&6&l欢迎回来!");
        
        // 从配置文件获取副标题文本，默认为包含玩家名的文本
        String subtitleText = plugin.getConfig().getString("join-message.title.subtitle", "&e%player% &7- 享受游戏时光!");
        
        // 替换文本中的占位符（如%player%等）
        titleText = replacePlaceholders(titleText, player);
        subtitleText = replacePlaceholders(subtitleText, player);
        
        // 从配置获取标题淡入时间（默认10tick）
        int fadeIn = plugin.getConfig().getInt("join-message.title.fade-in", 10);
        
        // 从配置获取标题停留时间（默认40tick）
        int stay = plugin.getConfig().getInt("join-message.title.stay", 40);
        
        // 从配置获取标题淡出时间（默认10tick）
        int fadeOut = plugin.getConfig().getInt("join-message.title.fade-out", 10);
        
        // 向玩家发送标题消息，包括主标题、副标题和显示时间设置
        player.sendTitle(
            ChatColor.translateAlternateColorCodes('&', titleText),      // 解析颜色代码的主标题
            ChatColor.translateAlternateColorCodes('&', subtitleText),   // 解析颜色代码的副标题
            fadeIn, stay, fadeOut                                       // 淡入、停留、淡出时间
        );
    }
    
    /**
     * 向玩家显示动作栏消息
     * 从配置中获取动作栏文本，并使用定时任务循环显示
     * 
     * @param player 要发送动作栏消息的玩家
     */
    private void showActionBarMessage(Player player) {
        // 从配置获取动作栏文本并替换占位符
        final String actionBarText = replacePlaceholders(
            plugin.getConfig().getString("join-message.action-bar.text", 
                "&a欢迎 &e%player% &a加入! &7在线玩家: &6%online% &7/ &6%max_players%"), 
            player
        );
        
        // 创建定时任务来循环显示动作栏消息
        // 由于原版Minecraft没有专门的动作栏API，这里使用标题API模拟动作栏效果
        new BukkitRunnable() {
            int count = 0; // 计数器，用于跟踪显示次数
            final int maxCount = plugin.getConfig().getInt("join-message.action-bar.duration", 5); // 最大显示秒数
            
            @Override
            public void run() {
                // 检查是否达到最大显示次数或玩家是否仍然在线
                if (count >= maxCount || !player.isOnline()) {
                    this.cancel(); // 达到条件时取消任务
                    return;
                }
                
                // 替换倒计时占位符
                String currentText = actionBarText.replace("%count%", String.valueOf(maxCount - count));
                
                // 使用标题API显示在屏幕底部（模拟动作栏效果）
                // 参数：主标题、副标题、淡入、停留、淡出时间
                player.sendTitle("", ChatColor.translateAlternateColorCodes('&', currentText), 0, 20, 0);
                
                count++; // 增加计数
            }
        }.runTaskTimer(plugin, 0L, 20L); // 立即开始，每秒执行一次（20个tick）
    }
    
    /**
     * 向所有玩家发送加入聊天消息
     * 从配置中随机选择一条欢迎消息并广播给所有玩家
     * 
     * @param player 加入的玩家
     * @param event 玩家加入事件对象
     */
    private void sendChatMessage(Player player, PlayerJoinEvent event) {
        // 从配置文件获取欢迎消息列表
        List<String> joinMessages = plugin.getConfig().getStringList("join-message.chat-message.messages");
        
        // 如果配置中没有设置欢迎消息，则使用默认消息
        if (joinMessages.isEmpty()) {
            joinMessages.add("&a欢迎 &e%player% &a加入服务器! &7当前在线: &6%online% &7玩家");
        }
        
        // 从消息列表中随机选择一条消息
        String message = joinMessages.get(random.nextInt(joinMessages.size()));
        
        // 替换消息中的占位符
        message = replacePlaceholders(message, player);
        
        // 添加消息前缀并解析颜色代码
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("join-message.prefix", "&6[欢迎]&r ") + message);
        
        // 向所有在线玩家广播格式化后的消息
        Bukkit.broadcastMessage(formattedMessage);
    }
    
    /**
     * 向所有玩家发送退出聊天消息
     * 从配置中随机选择一条告别消息并广播给所有玩家
     * 
     * @param event 玩家退出事件对象
     */
    private void sendQuitMessage(PlayerQuitEvent event) {
        // 获取退出的玩家对象
        Player player = event.getPlayer();
        
        // 从配置文件获取告别消息列表
        List<String> quitMessages = plugin.getConfig().getStringList("join-message.quit-message.messages");
        
        // 如果配置中没有设置告别消息，则使用默认消息
        if (quitMessages.isEmpty()) {
            quitMessages.add("&c%player% &7离开了服务器，期待再次相遇!");
        }
        
        // 从消息列表中随机选择一条消息
        String message = quitMessages.get(random.nextInt(quitMessages.size()));
        
        // 替换消息中的占位符
        message = replacePlaceholders(message, player);
        
        // 添加消息前缀并解析颜色代码
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("join-message.prefix", "&6[告别]&r ") + message);
        
        // 向所有在线玩家广播格式化后的消息
        Bukkit.broadcastMessage(formattedMessage);
    }
    
    /**
     * 为加入的玩家生成欢迎烟花
     * 在玩家周围随机位置生成指定数量的烟花效果
     * 
     * @param player 加入的玩家
     */
    private void spawnWelcomeFireworks(Player player) {
        // 获取玩家当前位置
        Location location = player.getLocation();
        
        // 创建定时任务来连续生成烟花
        new BukkitRunnable() {
            int count = 0; // 烟花计数器
            final int maxFireworks = plugin.getConfig().getInt("join-message.fireworks.amount", 3); // 从配置获取烟花数量
            
            @Override
            public void run() {
                // 检查是否达到最大烟花数量或玩家是否仍然在线
                if (count >= maxFireworks || !player.isOnline()) {
                    this.cancel(); // 达到条件时取消任务
                    return;
                }
                
                try {
                    // 在玩家周围随机位置生成烟花（x和z坐标偏移-2到+2之间）
                    Firework firework = location.getWorld().spawn(
                        location.clone().add(random.nextInt(5) - 2, 0, random.nextInt(5) - 2), 
                        Firework.class
                    );
                    
                    // 获取烟花元数据以进行自定义设置
                    FireworkMeta meta = firework.getFireworkMeta();
                    
                    // 创建随机烟花效果
                    FireworkEffect.Builder effectBuilder = FireworkEffect.builder();
                    effectBuilder.with(getRandomFireworkType());        // 随机烟花类型
                    effectBuilder.withColor(getRandomColors());         // 随机颜色
                    effectBuilder.withFade(getRandomColors());          // 随机渐变色
                    effectBuilder.trail(random.nextBoolean());          // 随机轨迹效果
                    effectBuilder.flicker(random.nextBoolean());        // 随机闪烁效果
                    
                    // 将构建的烟花效果添加到元数据中
                    meta.addEffect(effectBuilder.build());
                    
                    // 设置烟花强度（1-2之间随机）
                    meta.setPower(1 + random.nextInt(2));
                    
                    // 应用元数据到烟花实体
                    firework.setFireworkMeta(meta);
                    
                } catch (Exception e) {
                    // 记录烟花生成错误日志
                    plugin.getLogger().warning("生成烟花时出错: " + e.getMessage());
                }
                
                count++; // 增加烟花计数
            }
        }.runTaskTimer(plugin, 10L, 10L); // 延迟半秒开始，每隔半秒生成一次烟花
    }
    
    /**
     * 替换文本中的占位符
     * 将预定义的占位符替换为实际值，如玩家名、在线人数等
     * 
     * @param text 包含占位符的原始文本
     * @param player 相关的玩家对象
     * @return 替换占位符后的文本
     */
    private String replacePlaceholders(String text, Player player) {
        // 创建日期格式化器以获取当前时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        
        // 格式化当前时间
        String currentTime = dateFormat.format(new Date());
        
        // 获取玩家坐标
        Location location = player.getLocation();
        
        // 获取玩家游戏模式
        String gamemode = player.getGameMode().toString().toLowerCase();
        
        // 获取世界时间（转换为12小时制）
        long worldTime = player.getWorld().getTime();
        long hours = (worldTime / 1000 + 6) % 24;
        long minutes = (worldTime % 1000) * 60 / 1000;
        String worldTimeStr = String.format("%02d:%02d", hours, minutes);
        
        // 获取UUID短格式（前8位）
        String uuidShort = player.getUniqueId().toString().substring(0, 8);
        
        // 获取玩家Ping
        int ping = getPlayerPing(player);
        
        // 依次替换所有占位符为实际值
        return text
            .replace("%player%", player.getName())                                    // 玩家名
            .replace("%displayname%", player.getDisplayName())                        // 玩家显示名
            .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))    // 当前在线玩家数量
            .replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()))        // 服务器最大玩家数量
            .replace("%server_name%", plugin.getConfig().getString("join-message.server-name", "我的服务器")) // 服务器名称
            .replace("%time%", currentTime)                                          // 当前时间
            .replace("%world%", player.getWorld().getName())                         // 玩家所在世界名
            .replace("%uuid%", player.getUniqueId().toString())                      // 玩家UUID
            .replace("%uuid_short%", uuidShort)                                      // 玩家UUID短格式
            .replace("%x%", String.valueOf((int)location.getX()))                    // X坐标
            .replace("%y%", String.valueOf((int)location.getY()))                    // Y坐标
            .replace("%z%", String.valueOf((int)location.getZ()))                    // Z坐标
            .replace("%gamemode%", gamemode)                                         // 游戏模式
            .replace("%world_time%", worldTimeStr)                                   // 世界时间
            .replace("%ping%", String.valueOf(ping))                                 // 玩家Ping
            .replace("%join_count%", getTodayJoinCount())                            // 今日加入次数
            .replace("%total_players_today%", getTotalPlayersToday())                // 今日累计玩家数
            .replace("%uptime%", getServerUptime());                                 // 服务器运行时间
    }
    
    /**
     * 获取随机烟花类型
     * 从所有可用的烟花类型中随机选择一种
     * 
     * @return 随机选择的烟花类型
     */
    private FireworkEffect.Type getRandomFireworkType() {
        // 获取所有可用的烟花类型数组
        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        
        // 随机选择一个类型并返回
        return types[random.nextInt(types.length)];
    }
    
    /**
     * 获取随机颜色列表
     * 从预定义的颜色集合中随机选择一个颜色
     * 
     * @return 包含随机选择颜色的列表
     */
    private List<Color> getRandomColors() {
        // 定义可用的颜色数组
        Color[] colors = {
            Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, 
            Color.BLUE, Color.PURPLE, Color.FUCHSIA, Color.WHITE, Color.AQUA
        };
        
        // 随机确定选择的颜色数量（1-3个）
        int count = 1 + random.nextInt(3);
        
        // 随机选择一个颜色并返回单元素列表
        return List.of(colors[random.nextInt(colors.length)]);
    }
    
    /**
     * 获取玩家Ping值
     * 通过反射获取玩家的ping值，如果无法获取则返回0
     * 
     * @param player 玩家对象
     * @return 玩家ping值（毫秒）
     */
    private int getPlayerPing(Player player) {
        try {
            // 使用反射获取玩家的ping值
            Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
            return (int) entityPlayer.getClass().getField("ping").get(entityPlayer);
        } catch (Exception e) {
            // 如果无法获取ping值，返回默认值0
            return 0;
        }
    }
    
    /**
     * 获取今日加入次数
     * 模拟获取今日玩家加入次数（实际项目中需要持久化存储）
     * 
     * @return 今日加入次数
     */
    private String getTodayJoinCount() {
        // 这里应该从数据库或文件中获取实际数据
        // 目前返回一个模拟值
        return String.valueOf(random.nextInt(50) + 1);
    }
    
    /**
     * 获取今日累计玩家数
     * 模拟获取今日累计在线玩家数量（实际项目中需要持久化存储）
     * 
     * @return 今日累计玩家数
     */
    private String getTotalPlayersToday() {
        // 这里应该从数据库或文件中获取实际数据
        // 目前返回一个模拟值
        return String.valueOf(random.nextInt(100) + 1);
    }
    
    /**
     * 获取服务器运行时间
     * 计算服务器启动后的运行时间（小时）
     * 
     * @return 服务器运行时间（小时）
     */
    private String getServerUptime() {
        // 获取服务器启动时间
        long startTime = getServerStartTime();
        long currentTime = System.currentTimeMillis();
        
        // 计算运行时间（小时）
        long uptimeHours = (currentTime - startTime) / (1000 * 60 * 60);
        
        return String.valueOf(uptimeHours);
    }
    
    /**
     * 获取服务器启动时间
     * 如果插件没有提供启动时间，则使用当前时间减去一个随机值
     * 
     * @return 服务器启动时间戳
     */
    private long getServerStartTime() {
        // 如果插件没有记录启动时间，则使用当前时间减去一个随机值（1-24小时）
        return System.currentTimeMillis() - (random.nextInt(24) + 1) * 60 * 60 * 1000;
    }
    
    /**
     * 向玩家发送功能使用指南
     * 根据配置决定是否发送，展示插件的主要功能和使用方法
     * 
     * @param player 加入的玩家
     */
    private void sendHelpGuide(Player player) {
        // 检查是否启用功能指南
        if (!plugin.getConfig().getBoolean("join-message.send-help-guide-on-join", true)) {
            return;
        }
        
        // 延迟发送，避免与其他欢迎消息冲突
        new BukkitRunnable() {
            @Override
            public void run() {
                // 检查玩家是否仍然在线
                if (!player.isOnline()) {
                    return;
                }
                
                // 发送功能指南标题
                player.sendMessage("");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&l========== &e&l服务器功能指南 &6&l=========="));
                player.sendMessage("");
                
                // 传送系统
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a&l🚀 传送系统"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /etp <玩家> &f- 传送到其他玩家"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /etpa <玩家> &f- 请求传送到玩家"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /ehome [名称] &f- 传送到家园"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /esethome [名称] &f- 设置家园"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /ewarp <名称> &f- 传送到传送点"));
                player.sendMessage("");
                
                // 经济系统
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l💰 经济系统"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /emoney [玩家] &f- 查看余额"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /epay <玩家> <金额> &f- 转账给其他玩家"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /ebalancetop &f- 查看财富排行榜"));
                player.sendMessage("");
                
                // 银行系统
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b&l🏦 银行系统"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /bank balance &f- 查看银行余额"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /bank deposit <金额> &f- 存款"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /bank withdraw <金额> &f- 取款"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /bank transfer <玩家> <金额> &f- 转账"));
                player.sendMessage("");
                
                // 玩家管理
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d&l🎮 玩家管理"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /efly [玩家] &f- 切换飞行模式"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /egod [玩家] &f- 切换上帝模式"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /eheal [玩家] &f- 治疗玩家"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /efeed [玩家] &f- 喂食玩家"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /egm <0|1|2|3> [玩家] &f- 切换游戏模式"));
                player.sendMessage("");
                
                // 商店系统
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&l🛒 商店系统"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /shop &f- 打开自助商店"));
                player.sendMessage("");
                
                // 菜单系统
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f&l📋 控制中心"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /menu &f- 打开超级控制中心菜单"));
                player.sendMessage("");
                
                // 悬浮文字
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&l🌟 悬浮文字"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /holo create <内容> &f- 创建悬浮文字"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /holo online <内容> &f- 动态在线人数"));
                player.sendMessage("");
                
                // 特殊功能
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&l🎯 特殊功能"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /eflameblade &f- 获取火焰刀"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /edebug &f- 显示调试信息"));
                player.sendMessage("");
                
                // 公告系统
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&3&l📢 公告系统"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /ean <消息> &f- 发送聊天公告"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  /eanactionbar <消息> &f- 发送动作栏公告"));
                player.sendMessage("");
                
                // 帮助命令
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7  输入 &f/ehelp &7查看更多命令帮助"));
                player.sendMessage("");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&l===================================="));
                player.sendMessage("");
                
                // 发送提示信息
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) {
                            return;
                        }
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e💡 提示：使用 &f/menu &e打开控制中心快速访问所有功能！"));
                    }
                }.runTaskLater(plugin, 40L); // 2秒后发送提示
            }
        }.runTaskLater(plugin, 60L); // 3秒后发送指南
    }
}