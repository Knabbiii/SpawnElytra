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
    
    private SpawnElytra getPlugin() {
        return SpawnElytra.getInstance();
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
                getPlugin().reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "SpawnElytra configuration reloaded successfully!");
                return true;

            case "info":
                sendInfoMessage(sender);
                return true;

            case "visualize":
                if (!sender.hasPermission("spawnelytra.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                    return true;
                }

                int seconds = 10;
                if (args.length > 1) {
                    try {
                        seconds = Math.max(1, Math.min(60, Integer.parseInt(args[1])));
                    } catch (NumberFormatException ignored) {
                        sender.sendMessage(ChatColor.RED + "Invalid number of seconds, using default of 10.");
                    }
                }

                getPlugin().getListener().visualizeArea(player, seconds);
                sender.sendMessage(ChatColor.GREEN + "Showing spawn area outline for " + seconds + " seconds.");
                return true;

            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        List<String> messages = new ArrayList<>();
        messages.add("%s=== SpawnElytra Commands ===".formatted(ChatColor.GOLD));
        messages.add("%s/spawnelytra info %s- Show plugin information".formatted(ChatColor.YELLOW, ChatColor.WHITE));
        
        if (sender.hasPermission("spawnelytra.admin")) {
            messages.add("%s/spawnelytra reload %s- Reload plugin configuration".formatted(ChatColor.YELLOW, ChatColor.WHITE));
            messages.add("%s/spawnelytra visualize [seconds] %s- Show the spawn area outline with particles".formatted(ChatColor.YELLOW, ChatColor.WHITE));
        }

        messages.add("%sPlugin by Knabbiii - Enhanced with ideas from blax-k".formatted(ChatColor.GRAY));
        sender.sendMessage(messages.toArray(new String[0]));
    }

    private void sendInfoMessage(CommandSender sender) {
        SpawnElytra plugin = getPlugin();
        String infoBlock = """
                %s=== SpawnElytra Info ===
                %sVersion: %s%s
                %sAuthor: %s%s
                %sWebsite: %s%s
                
                %sWorld: %s%s
                %sSpawn Radius: %s%d
                %sBoost Multiplier: %s%d
                %sBoost Enabled: %s%s
                %sBoost Sound: %s%s
                
                %sEnhanced with features inspired by blax-k's implementation
                """.formatted(
                ChatColor.GOLD,
                ChatColor.YELLOW, ChatColor.WHITE, plugin.getDescription().getVersion(),
                ChatColor.YELLOW, ChatColor.WHITE, plugin.getDescription().getAuthors().get(0),
                ChatColor.YELLOW, ChatColor.WHITE, plugin.getDescription().getWebsite(),
                ChatColor.YELLOW, ChatColor.WHITE, plugin.getConfig().getString("world"),
                ChatColor.YELLOW, ChatColor.WHITE, plugin.getConfig().getInt("spawnRadius"),
                ChatColor.YELLOW, ChatColor.WHITE, plugin.getConfig().getInt("multiplyValue"),
                ChatColor.YELLOW, ChatColor.WHITE, plugin.getConfig().getBoolean("boostEnabled"),
                ChatColor.YELLOW, ChatColor.WHITE, plugin.getConfig().getString("boostSound"),
                ChatColor.GRAY
        );
        
        sender.sendMessage(infoBlock.trim().split("\n"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("info"));
            
            if (sender.hasPermission("spawnelytra.admin")) {
                completions.add("reload");
                completions.add("visualize");
            }
            
            return completions.stream()
                    .filter(c -> c.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}