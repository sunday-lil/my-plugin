package org.ljcode.myPlugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ljcode.myPlugin.MyPlugin;
import org.ljcode.myPlugin.listeners.MenuListener;

import java.util.ArrayList;
import java.util.List;

public class MenuCommand implements CommandExecutor {
    
    private final MyPlugin plugin;
    private final MenuListener menuListener;
    private static int colorIndex = 0;
    private static final Material[] BORDER_COLORS = {
        Material.PURPLE_STAINED_GLASS_PANE,
        Material.MAGENTA_STAINED_GLASS_PANE,
        Material.PINK_STAINED_GLASS_PANE,
        Material.RED_STAINED_GLASS_PANE,
        Material.ORANGE_STAINED_GLASS_PANE,
        Material.YELLOW_STAINED_GLASS_PANE,
        Material.LIME_STAINED_GLASS_PANE,
        Material.GREEN_STAINED_GLASS_PANE,
        Material.CYAN_STAINED_GLASS_PANE,
        Material.LIGHT_BLUE_STAINED_GLASS_PANE,
        Material.BLUE_STAINED_GLASS_PANE
    };
    
    public MenuCommand(MyPlugin plugin) {
        this.plugin = plugin;
        this.menuListener = new MenuListener(plugin);
        startBorderAnimation();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行！");
            return true;
        }
        
        Player player = (Player) sender;
        openMainMenu(player);
        return true;
    }
    
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§l§e✦ 超级控制中心 - 主菜单 ✦");
        
        inv.setItem(13, createHealItem());
        inv.setItem(11, createTeleportMenuItem());
        inv.setItem(15, createPlayerMenuItem());
        inv.setItem(29, createEconomyMenuItem());
        inv.setItem(33, createAdminMenuItem(player));
        inv.setItem(49, createCloseItem());
        
        fillBorder(inv);
        
        player.openInventory(inv);
    }
    
    public void openTeleportMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§l§d✦ 超级控制中心 - 传送菜单 ✦");
        
        inv.setItem(10, createMenuItem(Material.ENDER_PEARL, "§d传送到玩家", "§7点击选择玩家传送", "§8/etp <玩家>", "§a支持指定其他玩家"));
        inv.setItem(11, createMenuItem(Material.COMPASS, "§d传送到家", "§7点击传送到你的家", "§8/ehome [名称]", "§a支持多个家园"));
        inv.setItem(12, createMenuItem(Material.WHITE_BED, "§d设置家园", "§7点击设置当前位置为家", "§8/esethome [名称]", "§a管理员可设置多个"));
        inv.setItem(13, createMenuItem(Material.FEATHER, "§d传送到出生点", "§7点击传送到服务器出生点", "§8/espawn", "§a服务器主出生点"));
        inv.setItem(14, createMenuItem(Material.BEACON, "§d传送到传送点", "§7点击选择传送点", "§8/ewarp <名称>", "§a管理员可设置传送点"));
        inv.setItem(15, createMenuItem(Material.RED_BED, "§d返回上一个位置", "§7点击返回上一个位置", "§8/eback", "§a自动记录上次位置"));
        inv.setItem(16, createMenuItem(Material.OAK_DOOR, "§d设置传送点", "§7点击设置当前位置为传送点", "§8/esetwarp <名称>", "§c仅限管理员"));
        
        inv.setItem(40, createBackItem());
        inv.setItem(49, createCloseItem());
        
        fillBorder(inv);
        
        player.openInventory(inv);
    }
    
    public void openPlayerMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§l§a✦ 超级控制中心 - 玩家菜单 ✦");
        
        inv.setItem(10, createMenuItem(Material.ELYTRA, "§a切换飞行模式", "§7点击开启/关闭飞行", "§8/efly [玩家]", "§a管理员可指定其他玩家"));
        inv.setItem(11, createMenuItem(Material.TOTEM_OF_UNDYING, "§a切换上帝模式", "§7点击开启/关闭上帝模式", "§8/egod [玩家]", "§a无敌状态"));
        inv.setItem(12, createMenuItem(Material.GOLDEN_APPLE, "§a治疗自己", "§7点击恢复生命值", "§8/eheal [玩家]", "§a恢复满血满饥饿"));
        inv.setItem(13, createMenuItem(Material.COOKED_BEEF, "§a填饱肚子", "§7点击恢复饥饿值", "§8/efeed [玩家]", "§a恢复饱食度"));
        inv.setItem(14, createMenuItem(Material.GRASS_BLOCK, "§a更改游戏模式", "§7点击切换游戏模式", "§8/egm <0|1|2|3> [玩家]", "§a0=生存 1=创造 2=冒险 3=旁观"));
        
        inv.setItem(40, createBackItem());
        inv.setItem(49, createCloseItem());
        
        fillBorder(inv);
        
        player.openInventory(inv);
    }
    
    public void openEconomyMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§l§6✦ 超级控制中心 - 经济菜单 ✦");
        
        inv.setItem(11, createMenuItem(Material.GOLD_INGOT, "§6查看余额", "§7点击查看你的余额", "§8/emoney [玩家]", "§a管理员可查看其他玩家"));
        inv.setItem(12, createMenuItem(Material.EMERALD, "§6转账给玩家", "§7点击转账给其他玩家", "§8/epay <玩家> <金额>", "§a支持小数金额"));
        inv.setItem(13, createMenuItem(Material.DIAMOND, "§6余额排行榜", "§7点击查看余额排行榜", "§8/ebalancetop", "§a显示前10名"));
        inv.setItem(14, createMenuItem(Material.CHEST, "§6银行存取款", "§7点击打开银行界面", "§8/bank", "§a存款/取款/转账"));
        
        inv.setItem(40, createBackItem());
        inv.setItem(49, createCloseItem());
        
        fillBorder(inv);
        
        player.openInventory(inv);
    }
    
    public void openAdminMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§l§c✦ 超级控制中心 - 管理员菜单 ✦");
        
        inv.setItem(10, createMenuItem(Material.NETHERITE_SWORD, "§c满附魔下界合金盔甲", "§7点击获取满附魔盔甲", "§8/eall66", "§a全套下界合金盔甲"));
        inv.setItem(11, createMenuItem(Material.NETHERITE_PICKAXE, "§c满附魔下界合金工具", "§7点击获取满附魔工具", "§8/eall22", "§a全套下界合金工具"));
        inv.setItem(12, createMenuItem(Material.BLAZE_ROD, "§c火焰刀", "§7点击获取火焰刀", "§8/eflameblade", "§a火焰伤害武器"));
        inv.setItem(13, createMenuItem(Material.TNT, "§c核弹法阵", "§7点击释放核弹法阵", "§8/enuke", "§a大范围爆炸"));
        inv.setItem(14, createMenuItem(Material.BARRIER, "§c超级攻击力", "§7点击设置超高攻击力", "§8/e12503", "§a一击必杀"));
        inv.setItem(15, createMenuItem(Material.BOOK, "§c发送公告", "§7点击发送服务器公告", "§8/ean <消息>", "§a全服广播"));
        inv.setItem(16, createMenuItem(Material.PAPER, "§c重载配置", "§7点击重载插件配置", "§8/eanreload", "§a重新加载配置"));
        
        inv.setItem(20, createMenuItem(Material.GRASS_BLOCK, "§c设置出生点", "§7点击设置当前位置为出生点", "§8/esetspawn", "§a服务器主出生点"));
        inv.setItem(29, createMenuItem(Material.DEBUG_STICK, "§c调试模式", "§7点击切换调试模式", "§8/edebug", "§a显示调试信息"));
        
        inv.setItem(40, createBackItem());
        inv.setItem(49, createCloseItem());
        
        fillBorder(inv);
        
        player.openInventory(inv);
    }
    
    private void fillBorder(Inventory inv) {
        Material currentColor = BORDER_COLORS[colorIndex];
        
        for (int i = 0; i <= 8; i++) {
            inv.setItem(i, createBorderItem(currentColor));
        }
        
        for (int i = 45; i <= 53; i++) {
            inv.setItem(i, createBorderItem(currentColor));
        }
        
        inv.setItem(9, createBorderItem(currentColor));
        inv.setItem(17, createBorderItem(currentColor));
        inv.setItem(18, createBorderItem(currentColor));
        inv.setItem(26, createBorderItem(currentColor));
        inv.setItem(27, createBorderItem(currentColor));
        inv.setItem(35, createBorderItem(currentColor));
        inv.setItem(36, createBorderItem(currentColor));
        inv.setItem(44, createBorderItem(currentColor));
    }
    
    private ItemStack createBorderItem(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§8");
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createHealItem() {
        ItemStack item = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§a✦ 即时回血 ✦");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7点击恢复满状态");
        lore.add("§a恢复生命值");
        lore.add("§a恢复饥饿值");
        lore.add("§a清除火焰效果");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createTeleportMenuItem() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§d✦ 传送菜单 ✦");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7点击打开传送功能");
        lore.add("§a传送到玩家");
        lore.add("§a传送到家");
        lore.add("§a传送到出生点");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createPlayerMenuItem() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§a✦ 玩家菜单 ✦");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7点击打开玩家功能");
        lore.add("§a飞行模式");
        lore.add("§a上帝模式");
        lore.add("§a治疗功能");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createEconomyMenuItem() {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§6✦ 经济菜单 ✦");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7点击打开经济功能");
        lore.add("§a查看余额");
        lore.add("§a转账功能");
        lore.add("§a余额排行榜");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createAdminMenuItem(Player player) {
        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        
        if (player.hasPermission("essentialsx.admin")) {
            meta.setDisplayName("§c✦ 管理员菜单 ✦");
            
            List<String> lore = new ArrayList<>();
            lore.add("§7点击打开管理员功能");
            lore.add("§a你拥有管理员权限");
            lore.add("§e高级管理工具");
            meta.setLore(lore);
        } else {
            meta.setDisplayName("§8✦ 管理员菜单 ✦");
            
            List<String> lore = new ArrayList<>();
            lore.add("§7你需要管理员权限才能访问");
            lore.add("§c权限不足");
            lore.add("§7请联系管理员获取权限");
            meta.setLore(lore);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createMenuItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(name);
        
        List<String> loreList = new ArrayList<>();
        for (String line : loreLines) {
            loreList.add(line);
        }
        meta.setLore(loreList);
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createMenuItem(Material material, String name, String lore, String command) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(name);
        
        List<String> loreList = new ArrayList<>();
        loreList.add(lore);
        loreList.add("§8命令: /" + command);
        meta.setLore(loreList);
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§c返回");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7点击返回主菜单");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§c关闭菜单");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7点击关闭此菜单");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    private void startBorderAnimation() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            colorIndex = (colorIndex + 1) % BORDER_COLORS.length;
            updateAllMenus();
        }, 20L, 20L);
    }
    
    private void updateAllMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory() != null && 
                player.getOpenInventory().getTitle().startsWith("§l超级控制中心")) {
                Inventory inv = player.getOpenInventory().getTopInventory();
                fillBorder(inv);
                player.updateInventory();
            }
        }
    }
    
    public MenuListener getMenuListener() {
        return menuListener;
    }
    
    public static Material getCurrentBorderColor() {
        return BORDER_COLORS[colorIndex];
    }
}