package org.ljcode.myPlugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;

public class HelpCommand implements CommandExecutor {

    private final MyPlugin plugin;

    public HelpCommand(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            // 延迟到执行时取，避免 registerCommands 早于 registerListeners 导致 null
            if (plugin.getHelpMessageListener() != null) {
                plugin.getHelpMessageListener().sendHelpMessage(player);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行！");
        }
        
        return true;
    }
}
