package de.knabbiii.spawnelytra.listener;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.KeybindComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SpawnBoostListener extends BukkitRunnable implements Listener {

    private final Plugin plugin;
    private final int multiplyValue;
    private final int spawnRadius;
    private final boolean boostEnabled;
    private final World world;
    private final List<Player> flying = new ArrayList<>();
    private final List<Player> boosted = new ArrayList<>();
    private final List<Player> gracePeriod = new ArrayList<>();
    private final Map<Player, ItemStack> originalChestplates = new HashMap<>();
    private final String message;
    private final Sound boostSound;
    private final String boostDirection;
    private final boolean showBoostMessage;
    private final boolean showActivationMessage;

    public static SpawnBoostListener create(Plugin plugin) {
        var config = plugin.getConfig();
        if (!config.contains("multiplyValue") || !config.contains("spawnRadius") || !config.contains("boostEnabled") || !config.contains("world") || !config.contains("message")) {
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

        this.runTaskTimer(this.plugin, 0, 5);
    }

    @Override
    public void run() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;

            boolean inSpawnRadius = isInSpawnRadius(player);
            boolean isCurrentlyFlying = flying.contains(player);
            boolean isBedrock = isBedrockPlayer(player);

            if (isCurrentlyFlying) {
                if (isBedrock) {
                    if (!player.isGliding()) player.setGliding(true);
                    player.setAllowFlight(true);
                } else {
                    if (!player.isGliding()) player.setGliding(true);
                    player.setAllowFlight(false);
                }
            } else {
                player.setAllowFlight(inSpawnRadius);
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
        boolean isBedrock = isBedrockPlayer(player);
        
        if (isBedrock) {
            ItemStack currentChestplate = player.getInventory().getChestplate();
            
            if (currentChestplate == null || currentChestplate.getType() != Material.ELYTRA) {
                originalChestplates.put(player, currentChestplate);
                ItemStack virtualElytra = new ItemStack(Material.ELYTRA);
                ItemMeta meta = virtualElytra.getItemMeta();
                if (meta != null) {
                    meta.setUnbreakable(true);
                    meta.setDisplayName("§7Spawn Elytra");
                    virtualElytra.setItemMeta(meta);
                }
                player.getInventory().setChestplate(virtualElytra);
            }
        }
        
        flying.add(player);
        gracePeriod.add(player);
        
        if (isBedrock) {
            player.setAllowFlight(true);
            player.setGliding(true);
        } else {
            player.setGliding(true);
            player.setAllowFlight(false);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            gracePeriod.remove(player);
        }, 5);


        if (showActivationMessage && boostEnabled) {
            if (isBedrock) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                    new ComponentBuilder("§aPress SNEAK to boost yourself!").create());
            } else {
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

        if (!player.hasPermission("spawnelytra.useboost")) return;
        if (isBedrockPlayer(player)) return;
        if (!boostEnabled || !flying.contains(player) || boosted.contains(player)) return;

        event.setCancelled(true);
        applyBoost(player);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        
        if (!player.hasPermission("spawnelytra.useboost")) return;
        if (!isBedrockPlayer(player)) return;
        if (!event.isSneaking()) return;
        if (!boostEnabled || !flying.contains(player) || boosted.contains(player)) return;
        
        applyBoost(player);
    }

    private void applyBoost(Player player) {
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
        if (event.getEntityType() != EntityType.PLAYER) return;
        Player player = (Player) event.getEntity();
        if (flying.contains(player)) {
<<<<<<< HEAD
            // Only cancel if the player is trying to STOP gliding
            // This prevents Bedrock clients (via GeyserMC) from stopping flight prematurely
            if (!event.isGliding()) {
                event.setCancelled(true);
            }

            //Detect Landing and remove elytra - only check when player tries to stop gliding
            if (!event.isGliding() && !gracePeriod.contains(player) && isPlayerOnGround(player)) {
                player.setAllowFlight(false);
                player.setGliding(false);
                boosted.remove(player);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    flying.remove(player);
                }, 5L);
=======
            boolean isBedrock = isBedrockPlayer(player);
            
            if (!event.isGliding() && isBedrock) {
                if (!gracePeriod.contains(player) && !player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isAir()) {
                    player.setAllowFlight(false);
                    player.setGliding(false);
                    boosted.remove(player);
                    if (isBedrock && originalChestplates.containsKey(player)) {
                        player.getInventory().setChestplate(originalChestplates.remove(player));
                    }
                    Bukkit.getScheduler().runTaskLater(plugin, () -> flying.remove(player), 5L);
                } else {
                    event.setCancelled(true);
                }
            } else if (!isBedrock) {
                event.setCancelled(true);
                if (!gracePeriod.contains(player) && !player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isAir()) {
                    player.setAllowFlight(false);
                    player.setGliding(false);
                    boosted.remove(player);
                    if (originalChestplates.containsKey(player)) {
                        player.getInventory().setChestplate(originalChestplates.remove(player));
                    }
                    Bukkit.getScheduler().runTaskLater(plugin, () -> flying.remove(player), 5L);
                }
>>>>>>> 178c568261fc29069d6cb56f300d15ce927b0b59
            }
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
            if (originalChestplates.containsKey(player)) {
                player.getInventory().setChestplate(originalChestplates.remove(player));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        if (flying.contains(player) && isBedrockPlayer(player)) {
            if (event.getSlotType() == InventoryType.SlotType.ARMOR && event.getSlot() == 38) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && clicked.getType() == Material.ELYTRA) {
                    ItemMeta meta = clicked.getItemMeta();
                    if (meta != null && "§7Spawn Elytra".equals(meta.getDisplayName())) {
                        event.setCancelled(true);
                        player.sendMessage("§cYou can't remove the elytra while flying!");
                    }
                }
            }
        }
    }

    private boolean isInSpawnRadius(Player player) {
        if (!player.getWorld().equals(world)) return false;
        return player.getWorld().getSpawnLocation().distance(player.getLocation()) <= spawnRadius;
    }

<<<<<<< HEAD
    private boolean isPlayerOnGround(Player player) {
        // Check if there's a solid block below the player
        Block blockBelow = player.getLocation().subtract(0, 0.1, 0).getBlock();
        return !blockBelow.getType().isAir() && blockBelow.getType().isSolid();
=======
    private boolean isBedrockPlayer(Player player) {
        try {
            Class<?> floodgateApi = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = floodgateApi.getMethod("getInstance").invoke(null);
            return (boolean) floodgateApi.getMethod("isFloodgatePlayer", java.util.UUID.class)
                    .invoke(api, player.getUniqueId());
        } catch (Exception ignored) {}

        String uuid = player.getUniqueId().toString();
        return uuid.startsWith("00000000-0000-0000");
>>>>>>> 178c568261fc29069d6cb56f300d15ce927b0b59
    }
}
