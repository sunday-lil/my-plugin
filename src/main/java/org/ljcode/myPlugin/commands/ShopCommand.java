package org.ljcode.myPlugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;
import org.ljcode.myPlugin.gui.ShopGUI;

public class ShopCommand implements CommandExecutor {
    
    private final MyPlugin plugin;
    private final ShopGUI shopGUI;
    
    public ShopCommand(MyPlugin plugin) {
        this.plugin = plugin;
        this.shopGUI = new ShopGUI(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以使用此命令!");
            return true;
        }
        
        Player player = (Player) sender;
        shopGUI.openShop(player);
        return true;
    }
    
    public ShopGUI getShopGUI() {
        return shopGUI;
    }
}