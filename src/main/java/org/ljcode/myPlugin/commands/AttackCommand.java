package org.ljcode.myPlugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ljcode.myPlugin.MyPlugin;

public class AttackCommand implements CommandExecutor {
    
    private final MyPlugin plugin;
    
    public AttackCommand(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // 设置玩家的攻击力为9999999999999999
        double attackDamage = 9999999999999999.0;
        
        // 获取玩家的属性并设置攻击伤害
        if (player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(attackDamage);
        }
        
        player.sendMessage(ChatColor.GREEN + "攻击力已设置为 " + attackDamage);
        
        return true;
    }
}