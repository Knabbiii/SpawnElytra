package de.knabbiii.spawnelytra.listener;

import de.knabbiii.spawnelytra.SpawnElytra;
import de.knabbiii.spawnelytra.data.DataManager;
import de.knabbiii.spawnelytra.util.UpdateChecker;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class SpawnBoostListener extends BukkitRunnable implements Listener {

    private final Plugin plugin;
    private final int multiplyValue;
    private final int spawnRadius;
    private final boolean ignoreYInSpawnRadius;
    private final boolean useRectangularArea;
    private final double rectMinX, rectMinY, rectMinZ;
    private final double rectMaxX, rectMaxY, rectMaxZ;
    private final boolean boostEnabled;
    private final World world;
    private final Set<UUID> flying = new HashSet<>();
    private final Set<UUID> boosted = new HashSet<>();
    private final Set<UUID> gracePeriod = new HashSet<>();
    private final Set<UUID> managedPlayers = new HashSet<>();
    private final Map<UUID, ItemStack> originalChestplates = new HashMap<>();
    private final Set<UUID> bedrockPlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> visualizationTasks = new HashMap<>();
    private volatile boolean saveScheduled = false; // Track if save is already scheduled
    private boolean updateNotified = false; // Only notify the first op after each restart
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

        boolean useRectangularArea = "rectangle".equalsIgnoreCase(config.getString("spawnAreaMode", "circle"));
        double x1 = config.getDouble("rectangularArea.x1", 0);
        double y1 = config.getDouble("rectangularArea.y1", 0);
        double z1 = config.getDouble("rectangularArea.z1", 0);
        double x2 = config.getDouble("rectangularArea.x2", 0);
        double y2 = config.getDouble("rectangularArea.y2", 0);
        double z2 = config.getDouble("rectangularArea.z2", 0);

        return new SpawnBoostListener(
                plugin,
                config.getInt("multiplyValue"),
                config.getInt("spawnRadius"),
            config.getBoolean("ignoreYInSpawnRadius", false),
                useRectangularArea,
                Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2),
                config.getBoolean("boostEnabled"),
                Objects.requireNonNull(Bukkit.getWorld(config.getString("world"))
                        , "Invalid world " + config.getString("world")),
                config.getString("message"),
                sound,
                config.getString("boostDirection", "forward"),
                config.getBoolean("showBoostMessage", true),
                config.getBoolean("showActivationMessage", true));
    }

    private SpawnBoostListener(Plugin plugin, int multiplyValue, int spawnRadius, boolean ignoreYInSpawnRadius,
                               boolean useRectangularArea, double rectMinX, double rectMinY, double rectMinZ,
                               double rectMaxX, double rectMaxY, double rectMaxZ, boolean boostEnabled,
                               World world, String message, Sound boostSound, String boostDirection,
                               boolean showBoostMessage, boolean showActivationMessage) {
        this.plugin = plugin;
        this.multiplyValue = multiplyValue;
        this.spawnRadius = spawnRadius;
        this.ignoreYInSpawnRadius = ignoreYInSpawnRadius;
        this.useRectangularArea = useRectangularArea;
        this.rectMinX = rectMinX;
        this.rectMinY = rectMinY;
        this.rectMinZ = rectMinZ;
        this.rectMaxX = rectMaxX;
        this.rectMaxY = rectMaxY;
        this.rectMaxZ = rectMaxZ;
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
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (!flying.contains(playerUUID) || !bedrockPlayers.contains(playerUUID)) return;

        // Block right-clicking a chestplate or elytra in the hotbar while inventory is closed,
        // which would auto-equip it and displace the virtual elytra.
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && isChestSlotItem(item.getType())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID playerUUID = player.getUniqueId();

        if (!flying.contains(playerUUID) || !bedrockPlayers.contains(playerUUID)) return;

        // Block any interaction directly on the chestplate armor slot (slot 38)
        if (event.getSlotType() == InventoryType.SlotType.ARMOR && event.getSlot() == 38) {
            event.setCancelled(true);
            return;
        }

        // Block right-clicking / shift-clicking a chestplate-type item or elytra in inventory,
        // which would auto-equip it to slot 38 and displace the virtual elytra.
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null && isChestSlotItem(currentItem.getType())) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                event.setCancelled(true);
            }
        }

        // Block placing a chestplate-type item or elytra onto slot 38 via cursor
        ItemStack cursor = event.getCursor();
        if (cursor != null && isChestSlotItem(cursor.getType())
                && event.getSlotType() == InventoryType.SlotType.ARMOR && event.getSlot() == 38) {
            event.setCancelled(true);
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
            if (SpawnElytra.isDebugMode()) plugin.getLogger().info("[debug] Restoring flight state for " + player.getName());
            if (flying.contains(playerUUID)) {
                player.setGliding(true);
            }
        }

        // Notify the first op that joins after a restart about available updates
        if (!updateNotified && player.isOp() && UpdateChecker.isUpdateAvailable()) {
            updateNotified = true;
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                player.sendMessage("§e[SpawnElytra] §aUpdate available: §fv"
                    + UpdateChecker.getLatestVersion()
                    + "§7 (you have v" + plugin.getDescription().getVersion() + ")\n"
                    + "§e[SpawnElytra] §7Download: §f" + UpdateChecker.getDownloadUrl()),
            40L);
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
        Location playerLocation = player.getLocation();

        if (useRectangularArea) {
            boolean insideY = ignoreYInSpawnRadius
                    || (playerLocation.getY() >= rectMinY && playerLocation.getY() <= rectMaxY);
            return insideY
                    && playerLocation.getX() >= rectMinX && playerLocation.getX() <= rectMaxX
                    && playerLocation.getZ() >= rectMinZ && playerLocation.getZ() <= rectMaxZ;
        }

        Location spawnLocation = player.getWorld().getSpawnLocation();

        if (ignoreYInSpawnRadius) {
            double deltaX = spawnLocation.getX() - playerLocation.getX();
            double deltaZ = spawnLocation.getZ() - playerLocation.getZ();
            double squaredDistance = deltaX * deltaX + deltaZ * deltaZ;
            double squaredRadius = (double) spawnRadius * spawnRadius;
            return squaredDistance <= squaredRadius;
        }

        return spawnLocation.distance(playerLocation) <= spawnRadius;
    }

    private boolean isChestSlotItem(Material material) {
        return material == Material.ELYTRA || material.name().endsWith("_CHESTPLATE");
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

    /**
     * Shows a purple particle outline of the configured spawn area to one player
     * for the given duration. Only visible to that player, not broadcast to others.
     */
    public void visualizeArea(Player player, int seconds) {
        UUID playerUUID = player.getUniqueId();

        BukkitTask existing = visualizationTasks.remove(playerUUID);
        if (existing != null) existing.cancel();

        int maxTicks = seconds * 20;
        BukkitTask task = new BukkitRunnable() {
            private int ticksElapsed = 0;

            @Override
            public void run() {
                if (ticksElapsed >= maxTicks || !player.isOnline()) {
                    visualizationTasks.remove(playerUUID);
                    this.cancel();
                    return;
                }

                drawAreaOutline(player);
                ticksElapsed += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);

        visualizationTasks.put(playerUUID, task);
    }

    private void drawAreaOutline(Player player) {
        if (!player.getWorld().equals(world)) return;

        double y = player.getLocation().getY();
        Particle.DustOptions purple = new Particle.DustOptions(Color.fromRGB(170, 0, 255), 1.2f);

        if (useRectangularArea) {
            for (double x = rectMinX; x <= rectMaxX; x += 1.0) {
                player.spawnParticle(Particle.DUST, new Location(world, x, y, rectMinZ), 1, 0, 0, 0, 0, purple);
                player.spawnParticle(Particle.DUST, new Location(world, x, y, rectMaxZ), 1, 0, 0, 0, 0, purple);
            }
            for (double z = rectMinZ; z <= rectMaxZ; z += 1.0) {
                player.spawnParticle(Particle.DUST, new Location(world, rectMinX, y, z), 1, 0, 0, 0, 0, purple);
                player.spawnParticle(Particle.DUST, new Location(world, rectMaxX, y, z), 1, 0, 0, 0, 0, purple);
            }
        } else {
            Location center = world.getSpawnLocation();
            for (double angle = 0; angle < 360; angle += 4) {
                double rad = Math.toRadians(angle);
                double x = center.getX() + spawnRadius * Math.cos(rad);
                double z = center.getZ() + spawnRadius * Math.sin(rad);
                player.spawnParticle(Particle.DUST, new Location(world, x, y, z), 1, 0, 0, 0, 0, purple);
            }
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        visualizationTasks.values().forEach(BukkitTask::cancel);
        visualizationTasks.clear();
    }

    public void loadData() {
        DataManager dataManager = DataManager.getInstance();
        DataManager.LoadedFlyingData data = dataManager.loadFlyingData();

        flying.addAll(data.flyingPlayers);
        boosted.addAll(data.boosted);
        originalChestplates.putAll(data.originalChestplates);

        if (SpawnElytra.isDebugMode()) {
            plugin.getLogger().info("[debug] Loaded " + data.flyingPlayers.size() + " flying, "
                    + data.boosted.size() + " boosted, "
                    + data.originalChestplates.size() + " chestplates");
            flying.forEach(uuid -> plugin.getLogger().info("[debug] Flying: " + uuid));
        }
    }

    public void saveData() {
        // If a save is already scheduled, don't schedule another one
        if (saveScheduled) {
            return;
        }

        saveScheduled = true;

        if (SpawnElytra.isDebugMode()) plugin.getLogger().info("[debug] Scheduling data save...");
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
