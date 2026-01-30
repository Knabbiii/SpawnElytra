package de.knabbiii.spawnelytra.listener;

import de.knabbiii.spawnelytra.data.DataManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.KeybindComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SpawnBoostListener extends BukkitRunnable implements Listener {

    private final Plugin plugin;
    private final int multiplyValue;
    private final int spawnRadius;
    private final boolean boostEnabled;
    private final World world;
    private final Set<UUID> flying = new HashSet<>();
    private final Set<UUID> boosted = new HashSet<>();
    private final Set<UUID> gracePeriod = new HashSet<>();
    private final Set<UUID> managedPlayers = new HashSet<>();
    private final Map<UUID, ItemStack> originalChestplates = new HashMap<>();
    private final Set<UUID> bedrockPlayers = new HashSet<>();
    private volatile boolean saveScheduled = false; // Track if save is already scheduled
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
        //Detect Players near Spawn and allow them to toggle flight
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;
            UUID playerUUID = player.getUniqueId();
            boolean inSpawnRadius = isInSpawnRadius(player);
            boolean isCurrentlyFlying = flying.contains(playerUUID);

            if (isCurrentlyFlying || player.isGliding()) {
                // Keep allowFlight disabled while flying/gliding to prevent re-triggering
                player.setAllowFlight(false);
            } else if (inSpawnRadius) {
                // Player is in spawn radius - give them flight if they don't have it
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                    managedPlayers.add(playerUUID); // Track that we gave them flight
                }
            } else if (managedPlayers.contains(playerUUID)) {
                // Player left spawn radius and we were managing their flight - remove it
                player.setAllowFlight(false);
                managedPlayers.remove(playerUUID);
            }
        });
    }

    @EventHandler
    public void onDoubleJump(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return;
        if (!isInSpawnRadius(player)) return;

        // If player is already flying or gliding, just cancel - don't process again
        if (flying.contains(playerUUID) || player.isGliding()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        player.setAllowFlight(false);

        boolean isBedrock = bedrockPlayers.contains(player.getUniqueId());
        
        // Bedrock: Equip virtual elytra
        if (isBedrock) {
            ItemStack currentChestplate = player.getInventory().getChestplate();
            if (currentChestplate == null || currentChestplate.getType() != Material.ELYTRA) {
                originalChestplates.put(playerUUID, currentChestplate);
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
        
        // Immediately add to flying list BEFORE starting glide to block rapid re-triggers
        flying.add(playerUUID);
        saveData();
        managedPlayers.remove(playerUUID); // No longer managed - now in flight mode
        gracePeriod.add(playerUUID);
        
        // Now set flight states
        player.setGliding(true);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            //Player can be detected as not flying when removed
            gracePeriod.remove(playerUUID);
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
                && flying.contains(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapItem(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (!player.hasPermission("spawnelytra.useboost")) return;
        if (bedrockPlayers.contains(playerUUID)) return; // Bedrock uses sneak
        if (!boostEnabled || !flying.contains(playerUUID) || boosted.contains(playerUUID)) return;

        event.setCancelled(true);
        applyBoost(player);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (!player.hasPermission("spawnelytra.useboost")) return;
        if (!bedrockPlayers.contains(playerUUID)) return;
        if (!event.isSneaking()) return;
        if (!boostEnabled || !flying.contains(playerUUID) || boosted.contains(playerUUID)) return;
        
        applyBoost(player);
    }

    private void applyBoost(Player player) {
        boosted.add(player.getUniqueId());
        saveData();
        
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
        UUID playerUUID = player.getUniqueId();
        if (flying.contains(playerUUID)) {
            // Only cancel if the player is trying to STOP gliding
            // This prevents Bedrock clients (via GeyserMC) from stopping flight prematurely
            if (!event.isGliding()) {
                event.setCancelled(true);
            }

            //Detect Landing and remove elytra - only check when player tries to stop gliding
            if (!event.isGliding() && !gracePeriod.contains(playerUUID) && isPlayerOnGround(player)) {
                player.setAllowFlight(false);
                player.setGliding(false);
                boosted.remove(playerUUID);
                
                // Restore original chestplate for Bedrock
                if (originalChestplates.containsKey(playerUUID)) {
                    player.getInventory().setChestplate(originalChestplates.remove(playerUUID));
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    flying.remove(playerUUID);
                }, 5L);
                saveData();
            }
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (flying.contains(playerUUID)) {
            player.setAllowFlight(false);
            player.setGliding(false);
            flying.remove(playerUUID);
            boosted.remove(playerUUID);
            managedPlayers.remove(playerUUID);
            saveData();
            // Restore chestplate for Bedrock
            if (originalChestplates.containsKey(playerUUID)) {
                player.getInventory().setChestplate(originalChestplates.remove(playerUUID));
            }
        } else if (managedPlayers.contains(playerUUID)) {
            // Player was managed and changed worlds - remove flight and clean up
            player.setAllowFlight(false);
            managedPlayers.remove(playerUUID);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID playerUUID = player.getUniqueId();

        // Prevent Bedrock players from removing virtual elytra while flying
        if (flying.contains(playerUUID) && bedrockPlayers.contains(playerUUID)) {
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (isBedrockPlayer(player)) {
            bedrockPlayers.add(playerUUID);
        }

        // Check if player was flying before restart
        if (flying.contains(playerUUID)) {
            plugin.getLogger().info("Restoring flight state for " + player.getName());
            if (flying.contains(playerUUID)) {
                player.setGliding(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();

        // Clean up tracking for this player
        bedrockPlayers.remove(playerUUID);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUniqueId();

        // Prevent virtual elytra from dropping on death
        if (flying.contains(playerUUID) && bedrockPlayers.contains(playerUUID)) {
            // Remove virtual elytra from drops
            event.getDrops().removeIf(item -> {
                if (item.getType() == Material.ELYTRA) {
                    ItemMeta meta = item.getItemMeta();
                    return meta != null && "§7Spawn Elytra".equals(meta.getDisplayName());
                }
                return false;
            });
            
            // Restore original chestplate to drops if there was one
            if (originalChestplates.containsKey(playerUUID)) {
                ItemStack original = originalChestplates.remove(playerUUID);
                if (original != null) {
                    event.getDrops().add(original);
                }
            }
            
            // Clean up flying state
            flying.remove(playerUUID);
            boosted.remove(playerUUID);

            saveData();
        }
    }


    private boolean isInSpawnRadius(Player player) {
        if (!player.getWorld().equals(world)) return false;
        return player.getWorld().getSpawnLocation().distance(player.getLocation()) <= spawnRadius;
    }

    private boolean isPlayerOnGround(Player player) {
        // Check if there's a solid block below the player
        Block blockBelow = player.getLocation().subtract(0, 0.1, 0).getBlock();
        return !blockBelow.getType().isAir() && blockBelow.getType().isSolid();
    }

    private boolean isBedrockPlayer(Player player) {
        try {
            Class<?> floodgateApi = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = floodgateApi.getMethod("getInstance").invoke(null);
            return (boolean) floodgateApi.getMethod("isFloodgatePlayer", java.util.UUID.class)
                    .invoke(api, player.getUniqueId());
        } catch (Exception ignored) {}

        String uuid = player.getUniqueId().toString();
        return uuid.startsWith("00000000-0000-0000");
    }

    public void loadData() {
        plugin.getLogger().info("Loading flying players data...");

        DataManager dataManager = DataManager.getInstance();
        DataManager.LoadedFlyingData data = dataManager.loadFlyingData();

        plugin.getLogger().info("Loaded " + data.flyingPlayers.size() + " flying players");
        plugin.getLogger().info("Loaded " + data.boosted.size() + " boosted players");
        plugin.getLogger().info("Loaded " + data.originalChestplates.size() + " original chestplates");
        
        flying.addAll(data.flyingPlayers);
        boosted.addAll(data.boosted);
        originalChestplates.putAll(data.originalChestplates);

        // Debug: Print loaded UUIDs
        for (UUID uuid : flying) {
            plugin.getLogger().info("Loaded flying players: " + uuid);
        }
    }

    public void saveData() {
        // If a save is already scheduled, don't schedule another one
        if (saveScheduled) {
            return;
        }

        saveScheduled = true;

        plugin.getLogger().info("Scheduling data save...");
        // Wait 2 seconds (40 ticks) before saving to batch multiple changes
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            List<UUID> flyingCopy = new ArrayList<>(flying);
            List<UUID> boostedCopy = new ArrayList<>(boosted);
            Map<UUID, ItemStack> chestplatesCopy = new HashMap<>(originalChestplates);

            // Save asynchronously to prevent server lag
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                DataManager.getInstance().saveFlyingData(flyingCopy, boostedCopy, chestplatesCopy);
            });

            saveScheduled = false;
        }, 40L);
    }

    public void saveDataSync() {
        // Synchronous save for shutdown
        DataManager.getInstance().saveFlyingData(
                new ArrayList<>(flying),
                new ArrayList<>(boosted),
                new HashMap<>(originalChestplates)
        );
    }
}
