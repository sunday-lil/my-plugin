package org.ljcode.myPlugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;
import org.ljcode.myPlugin.listeners.HelpMessageListener;

public class HelpCommand implements CommandExecutor {
    
    private final MyPlugin plugin;
    private final HelpMessageListener helpMessageListener;
    
    public HelpCommand(MyPlugin plugin) {
        this.plugin = plugin;
        this.helpMessageListener = new HelpMessageListener(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            helpMessageListener.sendHelpMessage(player);
        } else {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行！");
        }
        
        return true;
    }
}