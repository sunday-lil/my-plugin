package org.ljcode.myPlugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.ljcode.myPlugin.commands.*;
import org.ljcode.myPlugin.gui.ShopGUI;
import org.ljcode.myPlugin.managers.*;
import org.ljcode.myPlugin.listeners.*;
import org.ljcode.myPlugin.listeners.ProfanityFilterListener;
import org.ljcode.myPlugin.listeners.HelpMessageListener;
import org.ljcode.myPlugin.listeners.HoloListener;
import org.ljcode.myPlugin.web.WebServer;

import java.io.IOException;

/**
 * EssentialsX-Clone 插件主类
 * 这是整个插件的核心类，负责初始化所有管理器、注册命令和监听器
 * 以及处理插件的生命周期事件（启用/禁用）
 */
public final class MyPlugin extends JavaPlugin {

    // 静态实例，用于全局访问插件实例
    private static MyPlugin instance;

    // 各种功能管理器实例，负责处理不同类型的业务逻辑
    private EconomyManager economyManager;          // 经济系统管理器
    private HomeManager homeManager;                // 家园系统管理器
    private WarpManager warpManager;                // 传送点系统管理器
    private TeleportManager teleportManager;        // 传送系统管理器
    private AnnouncementManager announcementManager; // 公告系统管理器
    private AnnouncementScheduler announcementScheduler; // 公告调度器
    private UpdateCheckerListener updateCheckerListener; // 更新检查器监听器
    private WebServer webServer;                    // Web配置服务器
    private HoloCommand holoCommand;                // 悬浮文字命令
    private MenuCommand menuCommand;                // 菜单命令
    private ShopManager shopManager;                // 商店系统管理器
    private ShopCommand shopCommand;                // 商店命令
    private K10TCPManager k10TCPManager;            // 行空板K10 TCP通信管理器
    private K10DigitalTwinListener k10DigitalTwinListener; // K10数字孪生事件监听器
    private EnvironmentDataCalculator environmentDataCalculator; // 环境数据计算器
    private EnvironmentDataReceiver environmentDataReceiver; // 环境数据接收器
    private EnvironmentDataScheduler environmentDataScheduler; // 环境数据调度器
    private DigitalCityManager digitalCityManager;            // 数字城市管理器
    private DigitalCityListener digitalCityListener;          // 数字城市事件监听器

    /**
     * 插件启用时调用的方法
     * 初始化所有管理器、注册命令和监听器，并启动定时任务
     * 这是插件启动时的核心逻辑入口
     */
    @Override
    public void onEnable() {
        // 设置静态实例以便全局访问
        instance = this;

        // 保存默认配置文件到插件数据文件夹
        saveDefaultConfig();

        // 初始化所有功能管理器，每个管理器负责不同的业务逻辑
        economyManager = new EconomyManager(this);              // 经济系统管理器
        homeManager = new HomeManager(this);                    // 家园系统管理器
        warpManager = new WarpManager(this);                    // 传送点系统管理器
        teleportManager = new TeleportManager(this);            // 传送系统管理器
        announcementManager = new AnnouncementManager(this);    // 公告系统管理器
        announcementScheduler = new AnnouncementScheduler(this, announcementManager); // 公告调度器
        updateCheckerListener = new UpdateCheckerListener(this); // 更新检查器监听器
        shopManager = new ShopManager(this);                    // 商店系统管理器

        // 初始化行空板K10数字孪生系统
        if (getConfig().getBoolean("k10.enabled", true)) {
            k10TCPManager = new K10TCPManager(getConfig());
            k10DigitalTwinListener = new K10DigitalTwinListener(k10TCPManager);
            getLogger().info("[K10数字孪生] 系统启动成功，已初始化HTTP通信管理器");

            // 初始化环境数据系统
            if (getConfig().getBoolean("environment.enabled", true)) {
                int scanRadius = getConfig().getInt("environment.scan-radius", 10);
                int heightRange = getConfig().getInt("environment.height-range", 5);

                environmentDataCalculator = new EnvironmentDataCalculator(scanRadius, heightRange);
                environmentDataReceiver = new EnvironmentDataReceiver(getConfig());
                environmentDataScheduler = new EnvironmentDataScheduler(
                    this, environmentDataCalculator, k10TCPManager, environmentDataReceiver);

                // 设置K10响应回调
                k10TCPManager.setResponseCallback(responseData -> {
                    if (responseData.containsKey("request_id")) {
                        String requestId = (String) responseData.get("request_id");
                        environmentDataScheduler.handleK10Response(requestId, responseData);
                    } else {
                        environmentDataReceiver.processResponse(responseData);
                    }
                });

                getLogger().info("[环境数据] 系统已初始化");
            } else {
                getLogger().info("[环境数据] 系统已禁用");
            }
        } else {
            getLogger().info("[K10数字孪生] 系统已禁用");
        }

        // 注册所有自定义命令，包括传送、经济、家园等功能相关的命令
        registerCommands();

        // 注册所有事件监听器，用于响应玩家的各种游戏行为
        registerListeners();

        // 从数据文件加载之前保存的数据（如余额、家园位置等）
        loadData();

        // 启动定时自动保存任务（每5分钟）
        economyManager.startAutoSave();

        // 启动定时公告发送任务
        announcementScheduler.startScheduling();

        // 启动环境数据调度器
        if (environmentDataScheduler != null && getConfig().getBoolean("environment.enabled", true)) {
            environmentDataScheduler.start();
        }

        // 初始化数字城市管理系统
        if (k10TCPManager != null && getConfig().getBoolean("digital-city.enabled", true)) {
            digitalCityManager = new DigitalCityManager(this);
            digitalCityListener = new DigitalCityListener(this);
            getLogger().info("🏙️ 数字城市管理系统已启动！");
        } else {
            getLogger().info("🏙️ 数字城市管理系统已禁用");
        }

        // 启动Web配置服务器
        startWebServer();

        // 记录插件启用成功的日志
        getLogger().info("EssentialsX-Clone has been enabled!");
    }

    /**
     * 启动Web配置服务器
     * 提供一个网页界面来可视化编辑配置文件
     */
    private void startWebServer() {
        try {
            // 从配置获取Web服务器端口
            int port = getConfig().getInt("web-server.port", 8080);

            // 启动Web服务器
            webServer = new WebServer(this, port);
            webServer.startServer();
            getLogger().info("Web配置服务器已在端口" + port + "启动 - 访问 http://localhost:" + port);
        } catch (IOException e) {
            getLogger().severe("无法启动Web服务器: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 插件禁用时调用的方法
     * 清理资源、停止定时任务并保存数据
     * 这是插件关闭时的核心逻辑入口
     */
    @Override
    public void onDisable() {
        // 停止定时自动保存任务
        if (economyManager != null) {
            economyManager.stopAutoSave();
        }

        // 停止定时公告发送任务，避免内存泄漏
        if (announcementScheduler != null) {
            announcementScheduler.stopScheduling();
        }

        // 保存所有数据到文件，确保玩家数据不会丢失
        saveData();

        // 清理公告管理器中的BossBar资源，避免服务器重启后残留
        if (announcementManager != null) {
            announcementManager.removeAllBossBars();
        }

        // 停止Web服务器
        if (webServer != null) {
            webServer.stop();
            getLogger().info("Web配置服务器已停止");
        }

        // 关闭环境数据系统
        if (environmentDataScheduler != null) {
            environmentDataScheduler.stop();
            getLogger().info("[环境数据] 系统已关闭");
        }

        // 关闭数字城市管理系统
        if (digitalCityManager != null) {
            digitalCityManager.stopCityManagement();
            getLogger().info("🏙️ 数字城市管理系统已关闭");
        }

        // 关闭行空板K10数字孪生系统
        if (k10TCPManager != null) {
            k10TCPManager.shutdown();
            getLogger().info("[K10数字孪生] 系统已关闭");
        }

        // 记录插件禁用成功的日志
        getLogger().info("EssentialsX-Clone has been disabled!");
    }

    /**
     * 注册所有自定义命令
     * 所有命令都使用'e'前缀以避免与其他插件冲突
     * 包括传送、玩家控制、经济、公告等多种功能的命令
     */
    private void registerCommands() {
        // 传送相关命令（使用'e'前缀避免与其他插件冲突）
        getCommand("etp").setExecutor(new TeleportCommand(this));           // 传送到指定玩家
        getCommand("etpa").setExecutor(new TeleportRequestCommand(this));   // 请求传送到其他玩家
        getCommand("etpahere").setExecutor(new TeleportRequestCommand(this)); // 请求其他玩家传送到自己这里
        getCommand("ehome").setExecutor(new HomeCommand(this));             // 传送到家
        getCommand("esethome").setExecutor(new HomeCommand(this));          // 设置家
        getCommand("ewarp").setExecutor(new WarpCommand(this));             // 传送到传送点
        getCommand("esetwarp").setExecutor(new WarpCommand(this));          // 设置传送点
        getCommand("espawn").setExecutor(new SpawnCommand(this));           // 传送到出生点
        getCommand("esetspawn").setExecutor(new SpawnCommand(this));        // 设置出生点
        getCommand("eback").setExecutor(new BackCommand(this));             // 传送到上一个位置

        // 玩家控制命令（使用'e'前缀避免与其他插件冲突）
        getCommand("efly").setExecutor(new PlayerCommand(this));            // 切换飞行模式
        getCommand("egod").setExecutor(new PlayerCommand(this));            // 切换上帝模式
        getCommand("eheal").setExecutor(new PlayerCommand(this));           // 治疗玩家
        getCommand("efeed").setExecutor(new PlayerCommand(this));           // 填饱肚子
        getCommand("egm").setExecutor(new PlayerCommand(this));             // 更改游戏模式

        // 经济系统命令（使用'e'前缀避免与其他插件冲突）
        getCommand("emoney").setExecutor(new EconomyCommand(this));         // 查看余额
        getCommand("epay").setExecutor(new EconomyCommand(this));           // 转账给其他玩家
        getCommand("ebalancetop").setExecutor(new EconomyCommand(this));    // 查看余额排行榜
        getCommand("eco").setExecutor(new EconomyCommand(this));            // 经济管理命令

        // 银行系统命令
        getCommand("bank").setExecutor(new BankCommand(this));              // 银行系统

        // 商店系统命令
        shopCommand = new ShopCommand(this);                                // 商店系统
        getCommand("shop").setExecutor(shopCommand);                        // 商店命令

        // 公告系统命令（使用'e'前缀避免与其他插件冲突）
        getCommand("ean").setExecutor(new AnnouncementCommand(this));       // 发送聊天公告
        getCommand("eanreload").setExecutor(new AnnouncementCommand(this)); // 重载配置
        getCommand("eanactionbar").setExecutor(new AnnouncementCommand(this)); // 发送动作栏公告
        getCommand("eantitle").setExecutor(new AnnouncementCommand(this));  // 发送标题公告
        getCommand("eanbossbar").setExecutor(new AnnouncementCommand(this)); // 发送BossBar公告

        // 传送请求命令（使用'e'前缀避免与其他插件冲突）
        getCommand("etpaccept").setExecutor(new PlayerListener(this));      // 接受传送请求
        getCommand("etpdeny").setExecutor(new PlayerListener(this));        // 拒绝传送请求

        // 自定义特殊命令
        getCommand("e12503").setExecutor(new AttackCommand(this));          // 自定义攻击力指令
        getCommand("eflameblade").setExecutor(new FlameBladeCommand(this)); // 火焰刀指令（获取特殊物品）
        getCommand("edebug").setExecutor(new DebugInfoCommand(this));       // 调试信息命令
        getCommand("ehelp").setExecutor(new HelpCommand(this));             // 帮助命令
        getCommand("eall66").setExecutor(new Eall66Command(this));          // 满附魔下界合金盔甲指令
        getCommand("eall22").setExecutor(new Eall22Command(this));          // 满附魔下界合金工具指令
        getCommand("enuke").setExecutor(new EnukeCommand(this));            // 核弹法阵指令

        // 菜单命令
        menuCommand = new MenuCommand(this);
        getCommand("menu").setExecutor(menuCommand);                        // 超级控制中心菜单指令

        // 悬浮文字命令
        holoCommand = new HoloCommand();
        getCommand("holo").setExecutor(holoCommand);                        // 悬浮文字指令
    }

    /**
     * 注册所有事件监听器
     * 监听各种游戏事件，如玩家加入/离开、传送、聊天等
     * 每个监听器负责处理特定类型的事件
     */
    private void registerListeners() {
        // 玩家事件监听器：处理玩家加入、离开等事件
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // 传送事件监听器：处理玩家传送相关事件
        getServer().getPluginManager().registerEvents(new TeleportListener(this), this);

        // 加入消息监听器：处理玩家加入时的欢迎消息、烟花效果等
        getServer().getPluginManager().registerEvents(new JoinMessageListener(this), this);

        // 武器效果监听器：处理特殊武器（如火焰刀）的攻击效果
        getServer().getPluginManager().registerEvents(new WeaponEffectListener(this), this);

        // 脏话过滤监听器：检测并处理玩家聊天中的不当言论
        getServer().getPluginManager().registerEvents(new ProfanityFilterListener(this), this);

        // 帮助消息监听器：在玩家加入时发送帮助信息
        getServer().getPluginManager().registerEvents(new HelpMessageListener(this), this);

        // 聊天格式化监听器：处理玩家聊天消息的格式化和美化
        getServer().getPluginManager().registerEvents(new ChatFormatListener(this), this);

        // 聊天提示监听器：提供智能提示、表情转换、命令建议等功能
        getServer().getPluginManager().registerEvents(new ChatHintListener(this), this);

        // 方块破坏监听器：处理方块破坏事件，实现自动拾取功能
        getServer().getPluginManager().registerEvents(new org.ljcode.myPlugin.listeners.BlockBreakListener(this), this);

        // 更新检查器监听器：检查插件更新并通知管理员
        getServer().getPluginManager().registerEvents(updateCheckerListener, this);

        // 粒子拖尾监听器：监听玩家移动并生成火焰粒子
        getServer().getPluginManager().registerEvents(new ParticleTrailListener(), this);

        // 菜单监听器：处理超级控制中心菜单的点击事件
        getServer().getPluginManager().registerEvents(menuCommand.getMenuListener(), this);

        // 悬浮文字监听器：处理玩家离开时自动清理悬浮文字
        getServer().getPluginManager().registerEvents(new HoloListener(holoCommand), this);

        // 商店系统监听器：处理商店GUI的点击事件
        getServer().getPluginManager().registerEvents(new ShopListener(this, shopCommand.getShopGUI()), this);

        // 行空板K10数字孪生监听器：处理与K10的数字孪生联动
        if (k10DigitalTwinListener != null && getConfig().getBoolean("k10.enabled", true)) {
            getServer().getPluginManager().registerEvents(k10DigitalTwinListener, this);
            getLogger().info("[K10数字孪生] 事件监听器已注册");
        }

        // 数字城市事件监听器：处理玩家行为统计和城市管理
        if (digitalCityListener != null && getConfig().getBoolean("digital-city.enabled", true)) {
            getServer().getPluginManager().registerEvents(digitalCityListener, this);
            getLogger().info("[数字城市] 事件监听器已注册");
        }
    }

    /**
     * 从配置文件加载所有持久化数据
     * 包括经济数据（余额）、家园数据、传送点数据和传送历史数据
     * 在插件启用时调用
     */
    private void loadData() {
        economyManager.loadData();      // 加载经济系统数据（玩家余额等）
        homeManager.loadData();         // 加载家园数据（玩家设置的家的位置）
        warpManager.loadData();         // 加载传送点数据（服务器设置的公共传送点）
        teleportManager.loadData();     // 加载传送历史数据（玩家最后位置等）
        shopManager.loadData();         // 加载商店数据（商品库存等）
    }

    /**
     * 保存所有持久化数据到配置文件
     * 包括经济数据（余额）、家园数据、传送点数据和传送历史数据
     * 在插件禁用时调用
     */
    private void saveData() {
        economyManager.saveData();      // 保存经济系统数据（玩家余额等）
        homeManager.saveData();         // 保存家园数据（玩家设置的家的位置）
        warpManager.saveData();         // 保存传送点数据（服务器设置的公共传送点）
        teleportManager.saveData();     // 保存传送历史数据（玩家最后位置等）
        shopManager.saveData();         // 保存商店数据（商品库存等）
        // 公告管理器不需要保存数据，因为它只处理临时的公告消息
    }

    public static MyPlugin getInstance() {
        return instance;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public AnnouncementManager getAnnouncementManager() {
        return announcementManager;
    }

    public AnnouncementScheduler getAnnouncementScheduler() {
        return announcementScheduler;
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public WarpManager getWarpManager() {
        return warpManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public MenuCommand getMenuCommand() {
        return menuCommand;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public K10TCPManager getK10TCPManager() {
        return k10TCPManager;
    }
}
