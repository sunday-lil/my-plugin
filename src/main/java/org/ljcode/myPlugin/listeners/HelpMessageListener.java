package org.ljcode.myPlugin.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.ljcode.myPlugin.MyPlugin;

/**
 * 帮助消息事件监听器
 * 监听玩家加入事件，向新加入的玩家发送服务器功能指南
 * 提供基本命令使用说明和注意事项
 */
public class HelpMessageListener implements Listener {
    
    // 插件主类实例，用于访问插件的各种功能
    private final MyPlugin plugin;
    
    /**
     * 构造函数，初始化帮助消息监听器
     * 
     * @param plugin 插件主类实例
     */
    public HelpMessageListener(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 处理玩家加入事件
     * 在玩家加入服务器后延迟发送帮助信息
     * 
     * @param event 玩家加入事件对象
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 检查配置是否启用了加入时发送帮助指南功能
        boolean sendHelpGuideOnJoin = plugin.getConfig().getBoolean("join-message.send-help-guide-on-join", true);
        
        if (sendHelpGuideOnJoin) {
            // 延迟发送帮助信息，确保玩家完全加入后才显示
            // 使用调度器延迟1秒执行，避免在玩家刚加入时尚未完全加载完成
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                sendHelpMessage(player);
            }, 20L); // 延迟1秒（20 ticks，每tick为0.05秒）
        }
    }
    
    /**
     * 向玩家发送帮助信息
     * 包含服务器主要功能的命令说明和使用指南
     * 
     * @param player 目标玩家
     */
    public void sendHelpMessage(Player player) {
        player.sendMessage("");
        
        player.sendMessage(ChatColor.GOLD + "========== " + ChatColor.YELLOW + "服务器功能指南" + ChatColor.GOLD + " ==========");
        player.sendMessage(ChatColor.GRAY + "欢迎来到服务器! 以下是所有可用功能的详细说明:");
        player.sendMessage("");
        
        player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "🎁 特殊物品功能:");
        player.sendMessage(ChatColor.RED + "/eflameblade" + ChatColor.WHITE + " - 获取火焰刀（具有满级附魔和极高伤害）");
        player.sendMessage(ChatColor.GRAY + "  • 攻击生物时会在攻击位置生成岩浆");
        int flameBladeDamage = plugin.getConfig().getInt("special-items.flame-blade.extra-damage", 100);
        player.sendMessage(ChatColor.GRAY + "  • 火焰刀拥有" + flameBladeDamage + "点额外基础伤害");
        player.sendMessage(ChatColor.RED + "/eall66" + ChatColor.WHITE + " - 获取满附魔下界合金盔甲套装");
        player.sendMessage(ChatColor.GRAY + "  • 包含全套下界合金盔甲（头盔、胸甲、护腿、靴子）");
        player.sendMessage(ChatColor.GRAY + "  • 所有装备都拥有最高等级的保护附魔");
        player.sendMessage(ChatColor.RED + "/eall22" + ChatColor.WHITE + " - 获取满附魔下界合金工具套装");
        player.sendMessage(ChatColor.GRAY + "  • 包含全套下界合金工具（镐、斧、锹、锄）");
        player.sendMessage(ChatColor.GRAY + "  • 所有工具都拥有最高等级的效率附魔");
        player.sendMessage(ChatColor.RED + "/e12503" + ChatColor.WHITE + " - 获取自定义攻击力武器");
        player.sendMessage(ChatColor.GRAY + "  • 将你的攻击力提升至 9999999999999999");
        player.sendMessage(ChatColor.GRAY + "  • 适合需要快速击杀生物或BOSS的场景");
        player.sendMessage(ChatColor.RED + "/enuke" + ChatColor.WHITE + " - 释放核弹法阵特效");
        player.sendMessage(ChatColor.GRAY + "  • 以玩家为圆心创建炫酷的法阵粒子特效");
        player.sendMessage(ChatColor.GRAY + "  • 在法阵范围内随机生成点燃的TNT");
        player.sendMessage(ChatColor.GRAY + "  • 最终产生震撼的爆炸效果");
        player.sendMessage("");
        
        player.sendMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "🚀 传送功能命令:");
        player.sendMessage(ChatColor.AQUA + "/etp <玩家>" + ChatColor.WHITE + " - 传送到其他玩家");
        player.sendMessage(ChatColor.GRAY + "  • 直接传送到指定玩家的位置");
        player.sendMessage(ChatColor.AQUA + "/etpa <玩家>" + ChatColor.WHITE + " - 请求传送到其他玩家");
        player.sendMessage(ChatColor.GRAY + "  • 发送传送请求，等待对方接受");
        player.sendMessage(ChatColor.AQUA + "/etpahere <玩家>" + ChatColor.WHITE + " - 请求其他玩家传送到自己这里");
        player.sendMessage(ChatColor.GRAY + "  • 发送召唤请求，让对方传送到你的位置");
        player.sendMessage(ChatColor.AQUA + "/etpaccept" + ChatColor.WHITE + " - 接受传送请求");
        player.sendMessage(ChatColor.GRAY + "  • 接受待处理的传送请求");
        player.sendMessage(ChatColor.AQUA + "/etpdeny" + ChatColor.WHITE + " - 拒绝传送请求");
        player.sendMessage(ChatColor.GRAY + "  • 拒绝待处理的传送请求");
        player.sendMessage(ChatColor.AQUA + "/ehome [名称]" + ChatColor.WHITE + " - 传送到家园");
        player.sendMessage(ChatColor.GRAY + "  • 传送到指定的家园位置");
        player.sendMessage(ChatColor.GRAY + "  • 不指定名称则传送到默认家园");
        player.sendMessage(ChatColor.AQUA + "/esethome [名称]" + ChatColor.WHITE + " - 设置家园");
        player.sendMessage(ChatColor.GRAY + "  • 将当前位置设置为家园");
        player.sendMessage(ChatColor.GRAY + "  • 支持设置多个家园（需要权限）");
        player.sendMessage(ChatColor.AQUA + "/ewarp <名称>" + ChatColor.WHITE + " - 传送到公共传送点");
        player.sendMessage(ChatColor.GRAY + "  • 传送到服务器预设的公共传送点");
        player.sendMessage(ChatColor.AQUA + "/esetwarp <名称>" + ChatColor.WHITE + " - 设置传送点");
        player.sendMessage(ChatColor.GRAY + "  • 将当前位置设置为公共传送点");
        player.sendMessage(ChatColor.GRAY + "  • 需要管理员权限");
        player.sendMessage(ChatColor.AQUA + "/espawn" + ChatColor.WHITE + " - 传送到出生点");
        player.sendMessage(ChatColor.GRAY + "  • 传送到服务器的出生点");
        player.sendMessage(ChatColor.AQUA + "/esetspawn" + ChatColor.WHITE + " - 设置出生点");
        player.sendMessage(ChatColor.GRAY + "  • 将当前位置设置为服务器出生点");
        player.sendMessage(ChatColor.GRAY + "  • 需要管理员权限");
        player.sendMessage(ChatColor.AQUA + "/eback" + ChatColor.WHITE + " - 传送到上一个位置");
        player.sendMessage(ChatColor.GRAY + "  • 返回到上一次传送前的位置");
        player.sendMessage("");
        
        player.sendMessage(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "👤 玩家功能命令:");
        player.sendMessage(ChatColor.GREEN + "/efly [玩家]" + ChatColor.WHITE + " - 切换飞行模式");
        player.sendMessage(ChatColor.GRAY + "  • 开启或关闭飞行能力");
        player.sendMessage(ChatColor.GRAY + "  • 管理员可以指定其他玩家");
        player.sendMessage(ChatColor.GREEN + "/egod [玩家]" + ChatColor.WHITE + " - 切换上帝模式");
        player.sendMessage(ChatColor.GRAY + "  • 开启或关闭无敌状态");
        player.sendMessage(ChatColor.GRAY + "  • 上帝模式下免疫所有伤害");
        player.sendMessage(ChatColor.GREEN + "/eheal [玩家]" + ChatColor.WHITE + " - 治疗自己");
        player.sendMessage(ChatColor.GRAY + "  • 恢复生命值到最大值");
        player.sendMessage(ChatColor.GRAY + "  • 管理员可以指定其他玩家");
        player.sendMessage(ChatColor.GREEN + "/efeed [玩家]" + ChatColor.WHITE + " - 恢复饥饿值");
        player.sendMessage(ChatColor.GRAY + "  • 恢复饥饿值到最大值");
        player.sendMessage(ChatColor.GRAY + "  • 管理员可以指定其他玩家");
        player.sendMessage(ChatColor.GREEN + "/egm <0|1|2|3> [玩家]" + ChatColor.WHITE + " - 更改游戏模式");
        player.sendMessage(ChatColor.GRAY + "  • 0=生存 1=创造 2=冒险 3=旁观");
        player.sendMessage(ChatColor.GRAY + "  • 管理员可以指定其他玩家");
        player.sendMessage("");
        
        player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "💰 经济系统命令:");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/emoney [玩家]" + ChatColor.WHITE + " - 查看余额");
        player.sendMessage(ChatColor.GRAY + "  • 查看自己或其他玩家的余额");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/epay <玩家> <金额>" + ChatColor.WHITE + " - 转账给其他玩家");
        player.sendMessage(ChatColor.GRAY + "  • 向指定玩家转账指定金额");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/ebalancetop" + ChatColor.WHITE + " - 查看余额排行榜");
        player.sendMessage(ChatColor.GRAY + "  • 显示服务器财富排行榜前10名");
        player.sendMessage("");
        
        player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "🏦 银行系统命令:");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/bank balance [玩家]" + ChatColor.WHITE + " - 查看银行余额");
        player.sendMessage(ChatColor.GRAY + "  • 查看自己或其他玩家的银行余额");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/bank deposit <金额>" + ChatColor.WHITE + " - 存款");
        player.sendMessage(ChatColor.GRAY + "  • 将指定金额存入银行");
        player.sendMessage(ChatColor.GRAY + "  • 需要确保钱包有足够的余额");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/bank withdraw <金额>" + ChatColor.WHITE + " - 取款");
        player.sendMessage(ChatColor.GRAY + "  • 从银行取出指定金额");
        player.sendMessage(ChatColor.GRAY + "  • 需要确保银行有足够的余额");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/bank transfer <玩家> <金额>" + ChatColor.WHITE + " - 转账");
        player.sendMessage(ChatColor.GRAY + "  • 向其他玩家的银行账户转账");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/bank help" + ChatColor.WHITE + " - 显示银行系统帮助");
        player.sendMessage(ChatColor.GRAY + "  • 查看银行系统的详细帮助信息");
        player.sendMessage("");
        
        player.sendMessage(ChatColor.DARK_BLUE + "" + ChatColor.BOLD + "📢 公告系统命令:");
        player.sendMessage(ChatColor.BLUE + "/ean <消息>" + ChatColor.WHITE + " - 发送聊天公告");
        player.sendMessage(ChatColor.GRAY + "  • 向所有玩家发送聊天框公告");
        player.sendMessage(ChatColor.BLUE + "/eanactionbar <消息>" + ChatColor.WHITE + " - 发送动作栏公告");
        player.sendMessage(ChatColor.GRAY + "  • 在玩家动作栏显示公告");
        player.sendMessage(ChatColor.BLUE + "/eantitle <标题> | <副标题>" + ChatColor.WHITE + " - 发送标题公告");
        player.sendMessage(ChatColor.GRAY + "  • 显示标题和副标题公告");
        player.sendMessage(ChatColor.GRAY + "  • 使用 | 分隔标题和副标题");
        player.sendMessage(ChatColor.BLUE + "/eanbossbar <消息>" + ChatColor.WHITE + " - 发送BossBar公告");
        player.sendMessage(ChatColor.GRAY + "  • 在玩家Boss栏显示公告");
        player.sendMessage(ChatColor.BLUE + "/eanreload" + ChatColor.WHITE + " - 重载公告配置");
        player.sendMessage(ChatColor.GRAY + "  • 重新加载公告系统配置文件");
        player.sendMessage("");
        
        player.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "🔧 其他实用命令:");
        player.sendMessage(ChatColor.GRAY + "/menu" + ChatColor.WHITE + " - 打开超级控制中心");
        player.sendMessage(ChatColor.GRAY + "  • 打开可视化GUI菜单界面");
        player.sendMessage(ChatColor.GRAY + "  • 包含传送、玩家、经济、管理员等功能");
        player.sendMessage(ChatColor.GRAY + "/ehelp" + ChatColor.WHITE + " - 重新显示此帮助信息");
        player.sendMessage(ChatColor.GRAY + "  • 查看完整的功能指南");
        player.sendMessage(ChatColor.GRAY + "/edebug" + ChatColor.WHITE + " - 查看调试信息");
        player.sendMessage(ChatColor.GRAY + "  • 显示简化的调试信息（F3替代）");
        player.sendMessage("");
        
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "✨ 悬浮文字功能:");
        player.sendMessage(ChatColor.YELLOW + "/holo <文字>" + ChatColor.WHITE + " - 创建单行悬浮文字");
        player.sendMessage(ChatColor.GRAY + "  • 在你当前位置创建悬浮文字");
        player.sendMessage(ChatColor.YELLOW + "/holo multiline <行1> <行2> <行3>" + ChatColor.WHITE + " - 创建多行悬浮文字");
        player.sendMessage(ChatColor.GRAY + "  • 创建多行悬浮文字（最多3行）");
        player.sendMessage(ChatColor.YELLOW + "/holo online" + ChatColor.WHITE + " - 创建动态在线人数显示");
        player.sendMessage(ChatColor.GRAY + "  • 显示实时在线玩家数量");
        player.sendMessage(ChatColor.YELLOW + "/holo follow" + ChatColor.WHITE + " - 创建跟随悬浮文字");
        player.sendMessage(ChatColor.GRAY + "  • 创建跟随你移动的悬浮文字");
        player.sendMessage(ChatColor.YELLOW + "/holo stop" + ChatColor.WHITE + " - 停止所有动态显示");
        player.sendMessage(ChatColor.GRAY + "  • 停止所有动态悬浮文字的更新");
        player.sendMessage(ChatColor.YELLOW + "/holo clear" + ChatColor.WHITE + " - 清除你的所有悬浮文字");
        player.sendMessage(ChatColor.GRAY + "  • 删除你创建的所有悬浮文字");
        player.sendMessage(ChatColor.YELLOW + "/holo list" + ChatColor.WHITE + " - 查看你的悬浮文字列表");
        player.sendMessage(ChatColor.GRAY + "  • 列出你创建的所有悬浮文字");
        player.sendMessage(ChatColor.GRAY + "  • 每个玩家最多创建20个悬浮文字");
        player.sendMessage(ChatColor.GRAY + "  • 使用 & 符号添加颜色代码 (如 &a绿色)");
        player.sendMessage("");
        
        player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "⚠️  违规规则:");
        player.sendMessage(ChatColor.RED + "• 严禁使用不当语言或脏话");
        player.sendMessage(ChatColor.RED + "• 禁止恶意攻击其他玩家");
        player.sendMessage(ChatColor.RED + "• 禁止恶意破坏他人建筑");
        player.sendMessage(ChatColor.RED + "• 请文明游戏，友好交流");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "⚠️  注意：不当言论将被警告，3次违规将被踢出服务器！ ⚠️");
        
        player.sendMessage(ChatColor.GOLD + "=====================================");
        player.sendMessage("");
    }
}