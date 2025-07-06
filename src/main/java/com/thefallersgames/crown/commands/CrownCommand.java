package com.thefallersgames.crown.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.thefallersgames.crown.CrownPlugin;
import com.thefallersgames.crown.managers.ConfigManager;
import com.thefallersgames.crown.managers.CrownManager;

/**
 * Handles crown-related commands
 */
public class CrownCommand implements CommandExecutor {
    
    private CrownPlugin plugin;
    private CrownManager crownManager;
    private ConfigManager configManager;
    
    /**
     * Creates a new CrownCommand
     * @param plugin The plugin instance
     * @param crownManager The crown manager
     * @param configManager The configuration manager
     */
    public CrownCommand(CrownPlugin plugin, CrownManager crownManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.crownManager = crownManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && args.length < 1) {
            sender.sendMessage(ChatColor.RED + "You must specify a player when running this command from console!");
            return true;
        }
        
        if (args.length == 0) {
            // Self crown command
            if (sender.hasPermission("crown.use")) {
                Player player = (Player) sender;
                crownManager.giveCrown(player);
            } else {
                sender.sendMessage(configManager.getMessage("no_permission"));
            }
            return true;
        }
        
        // Admin commands require permission
        if (!sender.hasPermission("crown.admin")) {
            sender.sendMessage(configManager.getMessage("no_permission"));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        if (subCommand.equals("give")) {
            handleGiveCommand(sender, args);
        } else if (subCommand.equals("remove")) {
            handleRemoveCommand(sender, args);
        } else if (subCommand.equals("reload")) {
            handleReloadCommand(sender);
        } else {
            sender.sendMessage(ChatColor.RED + "Unknown subcommand! Available: give, remove, reload");
        }
        
        return true;
    }
    
    /**
     * Handles the give command
     * @param sender The command sender
     * @param args The command arguments
     */
    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /crown give <player>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(configManager.getMessage("player_not_found"));
            return;
        }
        
        crownManager.giveCrown(target);
        sender.sendMessage(ChatColor.GOLD + "Gave a crown to " + target.getName());
    }
    
    /**
     * Handles the remove command
     * @param sender The command sender
     * @param args The command arguments
     */
    private void handleRemoveCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /crown remove <player>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(configManager.getMessage("player_not_found"));
            return;
        }
        
        crownManager.removeCrown(target);
        sender.sendMessage(ChatColor.GOLD + "Removed the crown from " + target.getName());
    }
    
    /**
     * Handles the reload command
     * @param sender The command sender
     */
    private void handleReloadCommand(CommandSender sender) {
        configManager.reloadConfig();
        sender.sendMessage(configManager.getMessage("config_reloaded"));
    }
} 