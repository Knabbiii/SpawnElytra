package de.knabbiii.spawnelytra.listener;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.KeybindComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpawnBoostListener extends BukkitRunnable implements Listener {

    private final Plugin plugin;
    private final int multiplyValue;
    private final int spawnRadius;
    private final boolean boostEnabled;
    private final World world;
    private final List<Player> flying = new ArrayList<>();
    private final List<Player> boosted = new ArrayList<>();
    private final String message;
    private final Sound boostSound;
    private final String boostDirection;
    private final boolean showBoostMessage;
    private final boolean showActivationMessage;

    public static SpawnBoostListener create(Plugin plugin) {
        var config = plugin.getConfig();
        if (!config.contains("multiplyValue") || !config.contains("spawnRadius") || !config.contains("boostEnabled") || !config.contains("world") ||  !config.contains("message")) {
            plugin.saveResource("config.yml", true);
            plugin.reloadConfig();
        }
        
        String soundName = config.getString("boostSound", "ENTITY_BAT_TAKEOFF");
        Sound sound;
        try {
            sound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + soundName + ". Using default sound.");
            sound = Sound.ENTITY_BAT_TAKEOFF;
        }
        
        return new SpawnBoostListener(
                plugin,
                config.getInt("multiplyValue"),
                config.getInt("spawnRadius"),
                config.getBoolean("boostEnabled"),
                Objects.requireNonNull(Bukkit.getWorld(config.getString("world"))
                        , "Invalid world " + config.getString("world")),
                config.getString("message"),
                sound,
                config.getString("boostDirection", "forward"),
                config.getBoolean("showBoostMessage", true),
                config.getBoolean("showActivationMessage", true));
    }

    private SpawnBoostListener(Plugin plugin, int multiplyValue, int spawnRadius, boolean boostEnabled, 
                              World world, String message, Sound boostSound, String boostDirection,
                              boolean showBoostMessage, boolean showActivationMessage) {
        this.plugin = plugin;
        this.multiplyValue = multiplyValue;
        this.spawnRadius = spawnRadius;
        this.boostEnabled = boostEnabled;
        this.world = world;
        this.message = message;
        this.boostSound = boostSound;
        this.boostDirection = boostDirection.toLowerCase();
        this.showBoostMessage = showBoostMessage;
        this.showActivationMessage = showActivationMessage;

        this.runTaskTimer(this.plugin, 0, 3);
    }

    @Override
    public void run() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;
            
            boolean inSpawnRadius = isInSpawnRadius(player);
            boolean isCurrentlyFlying = flying.contains(player);
            
            if (isCurrentlyFlying) {
                player.setAllowFlight(false);
            } else {
                player.setAllowFlight(inSpawnRadius);
            }
            
            if (isCurrentlyFlying && !player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isAir()) {
                player.setAllowFlight(false);
                player.setGliding(false);
                boosted.remove(player);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    flying.remove(player);
                }, 5);
            }
        });
    }

    @EventHandler
    public void onDoubleJump(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;
        if (!isInSpawnRadius(player)) return;
        
        if (flying.contains(player)) {
            event.setCancelled(true);
            return;
        }
        
        event.setCancelled(true);
        player.setGliding(true);
        flying.add(player);
        
        if (showActivationMessage && boostEnabled) {
            String[] messageParts = message.split("%key%");
            
            try {
                BaseComponent[] components = new ComponentBuilder(messageParts[0])
                        .append(new KeybindComponent("key.swapOffhand"))
                        .append(messageParts[1])
                        .create();
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
            } catch (NoClassDefFoundError | NoSuchMethodError e) {
                player.sendMessage(message.replace("%key%", "[F]"));
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.PLAYER
                && (event.getCause() == EntityDamageEvent.DamageCause.FALL
                || event.getCause() == EntityDamageEvent.DamageCause.FLY_INTO_WALL)
                && flying.contains(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapItem(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        
        if (!player.hasPermission("spawnelytra.useboost")) {
            return;
        }
        
        if (!boostEnabled || !flying.contains(player) || boosted.contains(player)) return;
        
        event.setCancelled(true);
        boosted.add(player);
        
        Vector velocity;
        if ("upward".equalsIgnoreCase(boostDirection)) {
            velocity = new Vector(0, multiplyValue, 0);
        } else {
            velocity = player.getLocation().getDirection().multiply(multiplyValue);
        }
        
        player.setVelocity(velocity);
        
        player.playSound(player.getLocation(), boostSound, 1.0f, 1.0f);
        
        if (showBoostMessage) {
            try {
                BaseComponent[] components = new ComponentBuilder("§aBoost activated!")
                        .create();
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
            } catch (NoClassDefFoundError | NoSuchMethodError e) {
                player.sendMessage("§aBoost activated!");
            }
        }
    }

    @EventHandler
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (event.getEntityType() == EntityType.PLAYER && flying.contains(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (flying.contains(player)) {
            player.setAllowFlight(false);
            player.setGliding(false);
            flying.remove(player);
            boosted.remove(player);
        }
    }

    private boolean isInSpawnRadius(Player player) {
        if (!player.getWorld().equals(world)) return false;
        return player.getWorld().getSpawnLocation().distance(player.getLocation()) <= spawnRadius;
    }
}