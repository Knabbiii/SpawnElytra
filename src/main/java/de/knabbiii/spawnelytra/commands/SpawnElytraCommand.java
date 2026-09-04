package de.knabbiii.spawnelytra.commands;

import de.knabbiii.spawnelytra.SpawnElytra;
import de.knabbiii.spawnelytra.data.AreaConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class SpawnElytraCommand implements CommandExecutor, TabCompleter, Listener {

    private final Map<UUID, Location> pendingPos1 = new HashMap<>();
    private final Map<UUID, Location> pendingPos2 = new HashMap<>();

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
                if (!sender.hasPermission("spawnelytra.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }
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

                int seconds = 20;
                if (args.length > 1) {
                    try {
                        seconds = Math.max(1, Math.min(60, Integer.parseInt(args[1])));
                    } catch (NumberFormatException ignored) {
                        sender.sendMessage(ChatColor.RED + "Invalid number of seconds, using default of 20.");
                    }
                }

                getPlugin().getListener().visualizeArea(player, seconds);
                sender.sendMessage(ChatColor.GREEN + "Showing spawn area outline for " + seconds + " seconds.");
                return true;

            case "setup":
                return handleSetup(sender, args);

            case "center":
                return handleCenter(sender, args);

            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    private boolean handleSetup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spawnelytra.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        UUID uuid = player.getUniqueId();
        String sub = args.length > 1 ? args[1].toLowerCase() : "";

        switch (sub) {
            case "" -> sendSetupStatus(player);

            case "pos1" -> {
                if (!requireConfiguredWorld(player)) return true;
                pendingPos1.put(uuid, player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Position 1 set at " + formatLocation(player.getLocation()) + "!"
                        + ChatColor.GRAY + " Now go to the opposite corner and run "
                        + ChatColor.WHITE + "/spawnelytra setup pos2" + ChatColor.GRAY + ".");
                showPendingPreviewIfBothSet(player);
            }

            case "pos2" -> {
                if (!requireConfiguredWorld(player)) return true;
                pendingPos2.put(uuid, player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Position 2 set at " + formatLocation(player.getLocation()) + "!");
                if (pendingPos1.containsKey(uuid)) {
                    player.sendMessage(ChatColor.GRAY + "Both corners are set - showing a preview. Run "
                            + ChatColor.WHITE + "/spawnelytra setup save" + ChatColor.GRAY + " to apply this area.");
                } else {
                    player.sendMessage(ChatColor.GRAY + "Now set the other corner: "
                            + ChatColor.WHITE + "/spawnelytra setup pos1");
                }
                showPendingPreviewIfBothSet(player);
            }

            case "save" -> saveSetup(player);

            case "cancel" -> {
                pendingPos1.remove(uuid);
                pendingPos2.remove(uuid);
                getPlugin().getListener().cancelVisualization(player);
                player.sendMessage(ChatColor.YELLOW + "Setup cancelled - no changes were made.");
            }

            case "reset" -> {
                FileConfiguration area = AreaConfig.load(getPlugin());
                area.set("rectangularArea", null);
                area.set("mode", null);
                AreaConfig.save(getPlugin(), area);
                getPlugin().reloadConfig();
                player.sendMessage(ChatColor.GREEN + "Custom spawn area removed - back to config.yml's spawnAreaMode.");
            }

            default -> player.sendMessage(ChatColor.RED + "Unknown setup command. Use: pos1, pos2, save, cancel, reset.");
        }

        return true;
    }

    private void sendSetupStatus(Player player) {
        UUID uuid = player.getUniqueId();
        List<String> messages = new ArrayList<>();
        messages.add(ChatColor.GOLD + "=== SpawnElytra Setup ===");
        messages.add(ChatColor.YELLOW + "1. " + ChatColor.WHITE + "Stand at a corner: /spawnelytra setup pos1");
        messages.add(ChatColor.YELLOW + "2. " + ChatColor.WHITE + "Stand at the opposite corner: /spawnelytra setup pos2");
        messages.add(ChatColor.YELLOW + "3. " + ChatColor.WHITE + "Save it: /spawnelytra setup save");
        messages.add(ChatColor.GRAY + "Cancel anytime with: /spawnelytra setup cancel");
        messages.add("");

        Location pos1 = pendingPos1.get(uuid);
        Location pos2 = pendingPos2.get(uuid);
        messages.add(ChatColor.YELLOW + "Position 1: " + describePending(pos1));
        messages.add(ChatColor.YELLOW + "Position 2: " + describePending(pos2));

        player.sendMessage(messages.toArray(new String[0]));
    }

    private String describePending(Location location) {
        return location == null
                ? ChatColor.RED + "not set"
                : ChatColor.GREEN + "set at " + formatLocation(location);
    }

    private void showPendingPreviewIfBothSet(Player player) {
        UUID uuid = player.getUniqueId();
        Location pos1 = pendingPos1.get(uuid);
        Location pos2 = pendingPos2.get(uuid);
        if (pos1 != null && pos2 != null && pos1.getWorld().equals(pos2.getWorld())) {
            getPlugin().getListener().visualizePendingArea(player, pos1, pos2);

            if (getPlugin().getConfig().getBoolean("ignoreYInSpawnRadius", false)) {
                player.sendMessage(ChatColor.GOLD + "Note: " + ChatColor.GRAY
                        + "ignoreYInSpawnRadius is enabled, so height isn't enforced right now."
                        + "Only X/Z applies. The Y you set here is still saved and takes effect "
                        + "again once ignoreYInSpawnRadius is false.");
            }
        }
    }

    private void saveSetup(Player player) {
        UUID uuid = player.getUniqueId();
        Location pos1 = pendingPos1.get(uuid);
        Location pos2 = pendingPos2.get(uuid);

        if (pos1 == null || pos2 == null) {
            player.sendMessage(ChatColor.RED + "You need to set both positions first - missing "
                    + (pos1 == null ? "pos1" : "pos2") + ".");
            return;
        }
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            player.sendMessage(ChatColor.RED + "Position 1 and Position 2 are in different worlds - please set them again.");
            return;
        }
        if (!pos1.getWorld().getName().equals(getPlugin().getConfig().getString("world"))) {
            player.sendMessage(ChatColor.RED + "Both positions must be in the world configured in config.yml ('"
                    + getPlugin().getConfig().getString("world") + "').");
            return;
        }

        FileConfiguration area = AreaConfig.load(getPlugin());
        area.set("rectangularArea.x1", pos1.getX());
        area.set("rectangularArea.y1", pos1.getY());
        area.set("rectangularArea.z1", pos1.getZ());
        area.set("rectangularArea.x2", pos2.getX());
        area.set("rectangularArea.y2", pos2.getY());
        area.set("rectangularArea.z2", pos2.getZ());
        area.set("mode", "rectangle");
        AreaConfig.save(getPlugin(), area);
        getPlugin().reloadConfig();

        pendingPos1.remove(uuid);
        pendingPos2.remove(uuid);

        player.sendMessage(ChatColor.GREEN + "Custom spawn area saved! " + ChatColor.GRAY
                + "spawnRadius/circle settings are now ignored - this fixed area is used instead.");

        SpawnElytra plugin = getPlugin();
        plugin.getListener().visualizeSavedAreaPreview(player, 3);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) plugin.getListener().showSaveConfirmation(player);
        }, 3 * 20L);
    }

    private boolean handleCenter(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spawnelytra.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length > 1 && args[1].equalsIgnoreCase("reset")) {
            FileConfiguration area = AreaConfig.load(getPlugin());
            area.set("centerLocation", null);
            AreaConfig.save(getPlugin(), area);
            getPlugin().reloadConfig();
            player.sendMessage(ChatColor.GREEN + "Custom center removed - back to following the world's vanilla spawn point.");
            return true;
        }

        if (!requireConfiguredWorld(player)) return true;

        Location location = player.getLocation();
        FileConfiguration area = AreaConfig.load(getPlugin());
        area.set("centerLocation.x", location.getX());
        area.set("centerLocation.y", location.getY());
        area.set("centerLocation.z", location.getZ());
        AreaConfig.save(getPlugin(), area);
        getPlugin().reloadConfig();

        player.sendMessage(ChatColor.GREEN + "Flight area center set to " + formatLocation(location) + "!");
        getPlugin().getListener().visualizeArea(player, 20);
        return true;
    }

    private boolean requireConfiguredWorld(Player player) {
        String configuredWorld = getPlugin().getConfig().getString("world");
        if (!player.getWorld().getName().equals(configuredWorld)) {
            player.sendMessage(ChatColor.RED + "You must be in the world configured in config.yml ('"
                    + configuredWorld + "') to use this command.");
            return false;
        }
        return true;
    }

    private String formatLocation(Location location) {
        return String.format(Locale.ROOT, "%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pendingPos1.remove(uuid);
        pendingPos2.remove(uuid);
    }

    private void sendHelpMessage(CommandSender sender) {
        List<String> messages = new ArrayList<>();
        messages.add("%s=== SpawnElytra Commands ===".formatted(ChatColor.GOLD));

        if (sender.hasPermission("spawnelytra.admin")) {
            messages.add("%s/spawnelytra info %s- Show plugin information".formatted(ChatColor.YELLOW, ChatColor.WHITE));
            messages.add("%s/spawnelytra reload %s- Reload plugin configuration".formatted(ChatColor.YELLOW, ChatColor.WHITE));
            messages.add("%s/spawnelytra visualize [seconds] %s- Show the spawn area outline with particles".formatted(ChatColor.YELLOW, ChatColor.WHITE));
            messages.add("%s/spawnelytra setup %s- Define a custom rectangular spawn area".formatted(ChatColor.YELLOW, ChatColor.WHITE));
            messages.add("%s/spawnelytra center [reset] %s- Set/reset a custom flight area center point".formatted(ChatColor.YELLOW, ChatColor.WHITE));
        }

        messages.add("%sPlugin by Knabbiii".formatted(ChatColor.GRAY));
        sender.sendMessage(messages.toArray(new String[0]));
    }

    private void sendInfoMessage(CommandSender sender) {
        SpawnElytra plugin = getPlugin();
        FileConfiguration area = AreaConfig.load(plugin);

        String areaMode = area.contains("mode") ? area.getString("mode") : plugin.getConfig().getString("spawnAreaMode", "circle");
        String areaSource;
        if ("rectangle".equalsIgnoreCase(areaMode)) {
            if (area.contains("rectangularArea.x1")) {
                areaSource = "Custom box (/spawnelytra setup)";
            } else if (plugin.getConfig().contains("rectangularArea.x1")) {
                areaSource = "Custom box (config.yml)";
            } else {
                areaSource = "Auto-square (2x spawnRadius)";
            }
        } else {
            areaSource = "spawnRadius around center";
        }
        String centerSource = area.contains("centerLocation.x")
                ? "Custom (/spawnelytra center)"
                : "World spawn (vanilla)";

        String infoBlock = """
                %s=== SpawnElytra Info ===
                %sVersion: %s%s
                %sAuthor: %s%s
                %sWebsite: %s%s

                %sWorld: %s%s
                %sSpawn Radius: %s%d
                %sArea Mode: %s%s %s(%s)
                %sCenter: %s%s
                %sBoost Multiplier: %s%d
                %sBoost Enabled: %s%s
                %sBoost Sound: %s%s
                """.formatted(
                ChatColor.GOLD,
                ChatColor.YELLOW, ChatColor.WHITE, plugin.getDescription().getVersion(),
                ChatColor.YELLOW, ChatColor.WHITE, plugin.getDescription().getAuthors().get(0),
                ChatColor.YELLOW, ChatColor.WHITE, plugin.getDescription().getWebsite(),
                ChatColor.YELLOW, ChatColor.WHITE, plugin.getConfig().getString("world"),
                ChatColor.YELLOW, ChatColor.WHITE, plugin.getConfig().getInt("spawnRadius"),
                ChatColor.YELLOW, ChatColor.WHITE, areaMode, ChatColor.GRAY, areaSource,
                ChatColor.YELLOW, ChatColor.WHITE, centerSource,
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
            List<String> completions = new ArrayList<>();

            if (sender.hasPermission("spawnelytra.admin")) {
                completions.add("info");
                completions.add("reload");
                completions.add("visualize");
                completions.add("setup");
                completions.add("center");
            }

            return completions.stream()
                    .filter(c -> c.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && sender.hasPermission("spawnelytra.admin")) {
            List<String> options = switch (args[0].toLowerCase()) {
                case "setup" -> Arrays.asList("pos1", "pos2", "save", "cancel", "reset");
                case "center" -> Collections.singletonList("reset");
                default -> Collections.emptyList();
            };
            return options.stream()
                    .filter(c -> c.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
