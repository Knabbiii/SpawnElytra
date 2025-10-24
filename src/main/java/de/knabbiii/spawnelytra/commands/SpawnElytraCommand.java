package de.knabbiii.spawnelytra.commands;

import de.knabbiii.spawnelytra.SpawnElytra;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SpawnElytraCommand implements CommandExecutor, TabCompleter {
    private final SpawnElytra plugin;

    public SpawnElytraCommand(SpawnElytra plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("spawnelytra.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }
                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "SpawnElytra configuration reloaded successfully!");
                return true;

            case "info":
                sendInfoMessage(sender);
                return true;

            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== SpawnElytra Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/spawnelytra info " + ChatColor.WHITE + "- Show plugin information");
        
        if (sender.hasPermission("spawnelytra.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/spawnelytra reload " + ChatColor.WHITE + "- Reload plugin configuration");
        }
        
        sender.sendMessage(ChatColor.GRAY + "Plugin by Knabbiii - Enhanced with ideas from blax-k");
    }

    private void sendInfoMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== SpawnElytra Info ===");
        sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "Author: " + ChatColor.WHITE + plugin.getDescription().getAuthors().get(0));
        sender.sendMessage(ChatColor.YELLOW + "Website: " + ChatColor.WHITE + plugin.getDescription().getWebsite());
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "World: " + ChatColor.WHITE + plugin.getConfig().getString("world"));
        sender.sendMessage(ChatColor.YELLOW + "Spawn Radius: " + ChatColor.WHITE + plugin.getConfig().getInt("spawnRadius"));
        sender.sendMessage(ChatColor.YELLOW + "Boost Multiplier: " + ChatColor.WHITE + plugin.getConfig().getInt("multiplyValue"));
        sender.sendMessage(ChatColor.YELLOW + "Boost Enabled: " + ChatColor.WHITE + plugin.getConfig().getBoolean("boostEnabled"));
        sender.sendMessage(ChatColor.YELLOW + "Boost Sound: " + ChatColor.WHITE + plugin.getConfig().getString("boostSound"));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Enhanced with features inspired by blax-k's implementation");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("info"));
            
            if (sender.hasPermission("spawnelytra.admin")) {
                completions.add("reload");
            }
            
            return completions.stream()
                    .filter(c -> c.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}