package de.knabbiii.spawnelytra.listener;

import de.knabbiii.spawnelytra.SpawnElytra;
import de.knabbiii.spawnelytra.data.AreaConfig;
import de.knabbiii.spawnelytra.data.DataManager;
import de.knabbiii.spawnelytra.util.BedrockSupport;
import de.knabbiii.spawnelytra.util.UpdateChecker;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.KeybindComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class SpawnBoostListener extends BukkitRunnable implements Listener {

    private static final long BOOST_MESSAGE_HOLD_MS = 2000L;
    private static final double PARTICLE_TARGET_SPACING = 1.0; // desired blocks between circle/sphere particles

    private final Plugin plugin;
    private final int multiplyValue;
    private final int spawnRadius;
    private final boolean ignoreYInSpawnRadius;
    private final boolean useRectangularArea;
    private final double rectMinX, rectMinY, rectMinZ;
    private final double rectMaxX, rectMaxY, rectMaxZ;
    // Custom center point (circle mode + rectangle-mode fallback square); always
    // resolved to concrete coordinates in create() - vanilla world spawn if not overridden.
    private final double customSpawnX, customSpawnY, customSpawnZ;
    private final boolean boostEnabled;
    private final World world;
    private final Set<UUID> flying = new HashSet<>();
    private final Map<UUID, Integer> boostCount = new HashMap<>();
    private final Map<UUID, Long> lastBoostTime = new HashMap<>();
    private final Set<UUID> boostReadyAnnounced = new HashSet<>();
    private final Set<UUID> gracePeriod = new HashSet<>();
    private final Set<UUID> managedPlayers = new HashSet<>();
    private final Set<UUID> bedrockPlayers = new HashSet<>();
    private final NamespacedKey keyTempElytra;
    private final NamespacedKey keyStoredChestplate;
    private final Map<UUID, BukkitTask> visualizationTasks = new HashMap<>();
    private volatile boolean saveScheduled = false; // Track if save is already scheduled
    private boolean updateNotified = false; // Only notify the first op after each restart
    private final String message;
    private final Sound boostSound;
    private final String boostDirection;
    private final boolean showBoostMessage;
    private final boolean showActivationMessage;
    private final boolean disableInAdventure;
    private final int totalBoosts;
    private final long boostToBoostCooldownMs;
    private final boolean disableFireworksInSpawnElytra;

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

        World world = Objects.requireNonNull(Bukkit.getWorld(config.getString("world")),
                "Invalid world " + config.getString("world"));
        int spawnRadius = config.getInt("spawnRadius");
        FileConfiguration area = AreaConfig.load(plugin);

        // 1. Center point - custom override (area.yml) or vanilla world spawn
        double customSpawnX, customSpawnY, customSpawnZ;
        if (area.contains("centerLocation.x")) {
            customSpawnX = area.getDouble("centerLocation.x");
            customSpawnY = area.getDouble("centerLocation.y");
            customSpawnZ = area.getDouble("centerLocation.z");
        } else {
            Location vanillaSpawn = world.getSpawnLocation();
            customSpawnX = vanillaSpawn.getX();
            customSpawnY = vanillaSpawn.getY();
            customSpawnZ = vanillaSpawn.getZ();
        }

        // 2. Area mode - area.yml override takes priority over config.yml's spawnAreaMode
        String mode = area.contains("mode") ? area.getString("mode") : config.getString("spawnAreaMode", "circle");
        boolean useRectangularArea = "rectangle".equalsIgnoreCase(mode);

        // 3. Rectangle bounds - custom override (area.yml or config.yml), or a square/cube
        // derived from the center point + spawnRadius
        double rectMinX, rectMinY, rectMinZ, rectMaxX, rectMaxY, rectMaxZ;
        FileConfiguration rectSource = area.contains("rectangularArea.x1") ? area
                : config.contains("rectangularArea.x1") ? config : null;
        if (rectSource != null) {
            double x1 = rectSource.getDouble("rectangularArea.x1", 0);
            double y1 = rectSource.getDouble("rectangularArea.y1", 0);
            double z1 = rectSource.getDouble("rectangularArea.z1", 0);
            double x2 = rectSource.getDouble("rectangularArea.x2", 0);
            double y2 = rectSource.getDouble("rectangularArea.y2", 0);
            double z2 = rectSource.getDouble("rectangularArea.z2", 0);
            rectMinX = Math.min(x1, x2); rectMaxX = Math.max(x1, x2);
            rectMinY = Math.min(y1, y2); rectMaxY = Math.max(y1, y2);
            rectMinZ = Math.min(z1, z2); rectMaxZ = Math.max(z1, z2);
        } else {
            rectMinX = customSpawnX - spawnRadius; rectMaxX = customSpawnX + spawnRadius;
            rectMinY = customSpawnY - spawnRadius; rectMaxY = customSpawnY + spawnRadius;
            rectMinZ = customSpawnZ - spawnRadius; rectMaxZ = customSpawnZ + spawnRadius;
        }

        return new SpawnBoostListener(
                plugin,
                config.getInt("multiplyValue"),
                spawnRadius,
            config.getBoolean("ignoreYInSpawnRadius", false),
                useRectangularArea,
                rectMinX, rectMinY, rectMinZ,
                rectMaxX, rectMaxY, rectMaxZ,
                customSpawnX, customSpawnY, customSpawnZ,
                config.getBoolean("boostEnabled"),
                world,
                config.getString("message"),
                sound,
                config.getString("boostDirection", "forward"),
                config.getBoolean("showBoostMessage", true),
                config.getBoolean("showActivationMessage", true),
                config.getBoolean("disableInAdventure", false),
                Math.max(1, config.getInt("totalBoosts", 1)),
                Math.max(0L, (long) (config.getDouble("boostToBoostCooldown", 0) * 1000)),
                config.getBoolean("disableFireworksInSpawnElytra", false));
    }

    private SpawnBoostListener(Plugin plugin, int multiplyValue, int spawnRadius, boolean ignoreYInSpawnRadius,
                               boolean useRectangularArea, double rectMinX, double rectMinY, double rectMinZ,
                               double rectMaxX, double rectMaxY, double rectMaxZ,
                               double customSpawnX, double customSpawnY, double customSpawnZ, boolean boostEnabled,
                               World world, String message, Sound boostSound, String boostDirection,
                               boolean showBoostMessage, boolean showActivationMessage,
                               boolean disableInAdventure,
                               int totalBoosts, long boostToBoostCooldownMs,
                               boolean disableFireworksInSpawnElytra) {
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
        this.customSpawnX = customSpawnX;
        this.customSpawnY = customSpawnY;
        this.customSpawnZ = customSpawnZ;
        this.boostEnabled = boostEnabled;
        this.world = world;
        this.message = message;
        this.boostSound = boostSound;
        this.boostDirection = boostDirection.toLowerCase();
        this.showBoostMessage = showBoostMessage;
        this.showActivationMessage = showActivationMessage;
        this.disableInAdventure = disableInAdventure;
        this.totalBoosts = totalBoosts;
        this.boostToBoostCooldownMs = boostToBoostCooldownMs;
        this.keyTempElytra = new NamespacedKey(plugin, "temp_elytra");
        this.keyStoredChestplate = new NamespacedKey(plugin, "stored_chestplate");
        this.disableFireworksInSpawnElytra = disableFireworksInSpawnElytra;

        this.runTaskTimer(this.plugin, 0, 5);
    }

    @Override
    public void run() {
        //Detect Players near Spawn and allow them to toggle flight
        Bukkit.getOnlinePlayers().forEach(player -> {
            UUID playerUUID = player.getUniqueId();

            if (!player.hasPermission("spawnelytra.use") || !isGameModeAllowed(player.getGameMode())) {
                // Permission was revoked or game mode became disallowed (e.g. mid-flight,
                // or via a config reload) - forcibly strip any flight WE granted. Only check our
                // own tracked state here, not raw getAllowFlight() - that's also true for native
                // Creative-mode flight, which has nothing to do with us and must stay untouched.
                if (flying.contains(playerUUID) || managedPlayers.contains(playerUUID)) {
                    if (!hasNativeFlight(player.getGameMode())) {
                        player.setAllowFlight(false);
                    }
                    player.setGliding(false);
                    flying.remove(playerUUID);
                    managedPlayers.remove(playerUUID);
                    resetBoosts(playerUUID);
                    saveData();
                }
                return;
            }

            boolean inSpawnRadius = isInSpawnRadius(player);
            boolean isCurrentlyFlying = flying.contains(playerUUID);

            if (isCurrentlyFlying || player.isGliding()) {
                // Keep allowFlight disabled while flying/gliding to prevent re-triggering
                player.setAllowFlight(false);
                showBoostCooldownIfActive(player, playerUUID);
            } else if (inSpawnRadius) {
                // Player is in spawn radius - give them flight if they don't have it, and track
                // them as managed either way (even if allowFlight was already true - e.g. left
                // over from a listener instance replaced by /spawnelytra reload) so leaving the
                // area later correctly revokes it instead of leaving it stuck on forever.
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                }
                managedPlayers.add(playerUUID);
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
        if (!player.hasPermission("spawnelytra.use") || !isGameModeAllowed(player.getGameMode())) {
            // No permission, or game mode not allowed - if we previously granted flight/gliding
            // via our own mechanic, revoke it. Otherwise this toggle isn't ours to touch (e.g.
            // native Creative-mode flight, which we never manage in the first place).
            if ((managedPlayers.contains(playerUUID) || flying.contains(playerUUID)) && !hasNativeFlight(player.getGameMode())) {
                event.setCancelled(true);
                player.setAllowFlight(false);
            }
            return;
        }
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
            if (!isTempElytra(currentChestplate)) {
                backupChestplate(player, currentChestplate);
                player.getInventory().setChestplate(createTempElytra());
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


        if (showActivationMessage && boostEnabled && player.hasPermission("spawnelytra.useboost")) {
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
        if (!boostEnabled || !flying.contains(playerUUID) || !canBoost(playerUUID)) return;

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
        if (!boostEnabled || !flying.contains(playerUUID) || !canBoost(playerUUID)) return;

        applyBoost(player);
    }

    private boolean canBoost(UUID playerUUID) {
        int used = boostCount.getOrDefault(playerUUID, 0);
        if (used >= totalBoosts) return false;

        if (used > 0 && boostToBoostCooldownMs > 0) {
            long last = lastBoostTime.getOrDefault(playerUUID, 0L);
            if (System.currentTimeMillis() - last < boostToBoostCooldownMs) return false;
        }

        return true;
    }

    /**
     * Shows a live actionbar countdown until the player's next boost is available.
     * Only meaningful when totalBoosts > 1 with a cooldown configured - see canBoost().
     */
    private void showBoostCooldownIfActive(Player player, UUID playerUUID) {
        if (boostToBoostCooldownMs <= 0 || totalBoosts <= 1) return;
        if (!player.hasPermission("spawnelytra.useboost")) return;

        int used = boostCount.getOrDefault(playerUUID, 0);
        if (used == 0 || used >= totalBoosts) return; // no boost taken yet, or none left to wait for

        long elapsedSinceBoost = System.currentTimeMillis() - lastBoostTime.getOrDefault(playerUUID, 0L);
        if (elapsedSinceBoost < BOOST_MESSAGE_HOLD_MS) return; // let "Boost activated!" stay visible a bit first

        long remainingMs = boostToBoostCooldownMs - elapsedSinceBoost;

        String text;
        if (remainingMs > 0) {
            long remainingSeconds = (remainingMs + 999) / 1000; // round up to the next full second
            text = "§7Next boost in " + remainingSeconds + "s";
        } else if (boostReadyAnnounced.add(playerUUID)) {
            // Cooldown just expired - show this once, not on every tick afterwards
            text = "§aYou can boost now!";
        } else {
            return;
        }

        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder(text).create());
        } catch (NoClassDefFoundError | NoSuchMethodError ignored) {
            // No BungeeChat API available - skip rather than spam the chat every tick
        }
    }

    private void applyBoost(Player player) {
        UUID playerUUID = player.getUniqueId();
        int used = boostCount.getOrDefault(playerUUID, 0) + 1;
        boostCount.put(playerUUID, used);
        lastBoostTime.put(playerUUID, System.currentTimeMillis());
        boostReadyAnnounced.remove(playerUUID);
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
            String text = totalBoosts > 1
                    ? "§aBoost activated! §7(" + used + "/" + totalBoosts + ")"
                    : "§aBoost activated!";
            try {
                BaseComponent[] components = new ComponentBuilder(text).create();
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
            } catch (NoClassDefFoundError | NoSuchMethodError e) {
                player.sendMessage(text);
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
                if (!hasNativeFlight(player.getGameMode())) {
                    player.setAllowFlight(false);
                }
                player.setGliding(false);
                resetBoosts(playerUUID);
                restoreChestplateIfPresent(player);

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
            if (!hasNativeFlight(player.getGameMode())) {
                player.setAllowFlight(false);
            }
            player.setGliding(false);
            flying.remove(playerUUID);
            resetBoosts(playerUUID);
            managedPlayers.remove(playerUUID);
            saveData();
            restoreChestplateIfPresent(player);
        } else if (managedPlayers.contains(playerUUID)) {
            // Player was managed and changed worlds - remove flight and clean up
            if (!hasNativeFlight(player.getGameMode())) {
                player.setAllowFlight(false);
            }
            managedPlayers.remove(playerUUID);
        }
    }

    @EventHandler
    public void onFireworkUseAttempt(PlayerInteractEvent event) {
        if (!disableFireworksInSpawnElytra) return;

        Player player = event.getPlayer();
        if (!flying.contains(player.getUniqueId())) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.FIREWORK_ROCKET) return;

        // Only block if the player isn't actually wearing a real elytra -
        // firework use with a genuine elytra is unrelated to spawn elytra flight.
        if (!isWearingRealElytra(player)) {
            event.setCancelled(true);
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

        if (BedrockSupport.isBedrockPlayer(plugin, player)) {
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
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (!flying.contains(playerUUID) || isGameModeAllowed(event.getNewGameMode())) return;

        // Switched into a disallowed game mode while flying via spawn elytra - force landing
        player.setGliding(false);
        if (!hasNativeFlight(event.getNewGameMode())) {
            player.setAllowFlight(false);
        }
        resetBoosts(playerUUID);
        flying.remove(playerUUID);
        managedPlayers.remove(playerUUID);
        saveData();
        restoreChestplateIfPresent(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();

        // Clean up tracking for this player
        bedrockPlayers.remove(playerUUID);
        BedrockSupport.forget(playerUUID);
        boostReadyAnnounced.remove(playerUUID);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUniqueId();

        // Prevent virtual elytra from dropping on death
        if (flying.contains(playerUUID) && bedrockPlayers.contains(playerUUID)) {
            // Remove virtual elytra from drops
            event.getDrops().removeIf(this::isTempElytra);

            // Restore original chestplate to drops if there was one
            ItemStack original = takeStoredChestplate(player);
            if (original != null) {
                event.getDrops().add(original);
            }

            // Clean up flying state
            flying.remove(playerUUID);
            resetBoosts(playerUUID);

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

        Location spawnLocation = getSpawnCenter();

        if (ignoreYInSpawnRadius) {
            double deltaX = spawnLocation.getX() - playerLocation.getX();
            double deltaZ = spawnLocation.getZ() - playerLocation.getZ();
            double squaredDistance = deltaX * deltaX + deltaZ * deltaZ;
            double squaredRadius = (double) spawnRadius * spawnRadius;
            return squaredDistance <= squaredRadius;
        }

        return spawnLocation.distance(playerLocation) <= spawnRadius;
    }

    private Location getSpawnCenter() {
        return new Location(world, customSpawnX, customSpawnY, customSpawnZ);
    }

    private boolean isGameModeAllowed(GameMode gameMode) {
        return switch (gameMode) {
            case SURVIVAL -> true;
            case ADVENTURE -> !disableInAdventure;
            default -> false;
        };
    }

    /**
     * Creative and Spectator always have their own, vanilla-granted flight ability,
     * completely independent of us. Never call setAllowFlight(false) for these modes as
     * part of our own cleanup - Minecraft only re-grants it on the next gamemode switch,
     * not automatically, so disabling it here would permanently strip native flight.
     */
    private boolean hasNativeFlight(GameMode gameMode) {
        return gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR;
    }

    private boolean isWearingRealElytra(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        return chestplate != null && chestplate.getType() == Material.ELYTRA && !isTempElytra(chestplate);
    }

    private boolean isChestSlotItem(Material material) {
        return material == Material.ELYTRA || material.name().endsWith("_CHESTPLATE");
    }

    private boolean isPlayerOnGround(Player player) {
        // Check if there's a solid block below the player
        Block blockBelow = player.getLocation().subtract(0, 0.1, 0).getBlock();
        return !blockBelow.getType().isAir() && blockBelow.getType().isSolid();
    }

    private void resetBoosts(UUID playerUUID) {
        boostCount.remove(playerUUID);
        lastBoostTime.remove(playerUUID);
        boostReadyAnnounced.remove(playerUUID);
    }

    private boolean isTempElytra(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(keyTempElytra, PersistentDataType.BYTE);
    }

    private ItemStack createTempElytra() {
        ItemStack virtualElytra = new ItemStack(Material.ELYTRA);
        ItemMeta meta = virtualElytra.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.setDisplayName("§7Spawn Elytra");
            meta.getPersistentDataContainer().set(keyTempElytra, PersistentDataType.BYTE, (byte) 1);
            virtualElytra.setItemMeta(meta);
        }
        return virtualElytra;
    }

    /**
     * Stores the player's real chestplate (or the absence of one) on their own
     * PersistentDataContainer, so it survives a server restart without a separate data file.
     */
    private void backupChestplate(Player player, ItemStack chestplate) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        byte[] data = chestplate == null ? new byte[0] : serializeItemStack(chestplate);
        pdc.set(keyStoredChestplate, PersistentDataType.BYTE_ARRAY, data);
    }

    private void restoreChestplateIfPresent(Player player) {
        player.getInventory().setChestplate(takeStoredChestplate(player));
    }

    /**
     * Removes and returns the backed-up chestplate from the player's PDC, or null
     * if none was stored (or the player had no chestplate equipped before).
     */
    private ItemStack takeStoredChestplate(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (!pdc.has(keyStoredChestplate, PersistentDataType.BYTE_ARRAY)) return null;

        byte[] data = pdc.get(keyStoredChestplate, PersistentDataType.BYTE_ARRAY);
        pdc.remove(keyStoredChestplate);
        return (data == null || data.length == 0) ? null : deserializeItemStack(data);
    }

    private byte[] serializeItemStack(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return outputStream.toByteArray();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to serialize chestplate backup: " + e.getMessage());
            return new byte[0];
        }
    }

    private ItemStack deserializeItemStack(byte[] data) {
        try (BukkitObjectInputStream dataInput = new BukkitObjectInputStream(new ByteArrayInputStream(data))) {
            return (ItemStack) dataInput.readObject();
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().warning("Failed to deserialize chestplate backup: " + e.getMessage());
            return null;
        }
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

    /**
     * Shows the full 3D wireframe of the just-saved rectangular area for a fixed duration,
     * ignoring ignoreYInSpawnRadius - used right after /spawnelytra setup save so the
     * preview stays consistent with what was shown while picking pos1/pos2 (always the
     * full box) instead of switching to the flat 2D outline visualizeArea() would show.
     */
    public void visualizeSavedAreaPreview(Player player, int seconds) {
        UUID playerUUID = player.getUniqueId();

        BukkitTask existing = visualizationTasks.remove(playerUUID);
        if (existing != null) existing.cancel();

        if (!useRectangularArea) return;

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

                Particle.DustOptions purple = new Particle.DustOptions(Color.fromRGB(170, 0, 255), 1.2f);
                drawBoxWireframe(player, purple, rectMinX, rectMinY, rectMinZ, rectMaxX, rectMaxY, rectMaxZ);
                ticksElapsed += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);

        visualizationTasks.put(playerUUID, task);
    }

    /**
     * Shows a purple particle box between two not-yet-saved corners, so an admin running
     * /spawnelytra setup can see the resulting area before committing it with "save".
     * Independent of the currently active (committed) area - draws exactly pos1..pos2.
     * Runs indefinitely until cancelVisualization() is called (on "save" or "cancel") or
     * the player goes offline - there's no fixed duration, unlike visualizeArea().
     */
    public void visualizePendingArea(Player player, Location pos1, Location pos2) {
        UUID playerUUID = player.getUniqueId();

        BukkitTask existing = visualizationTasks.remove(playerUUID);
        if (existing != null) existing.cancel();

        double minX = Math.min(pos1.getX(), pos2.getX()), maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY()), maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ()), maxZ = Math.max(pos1.getZ(), pos2.getZ());

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    visualizationTasks.remove(playerUUID);
                    this.cancel();
                    return;
                }

                Particle.DustOptions purple = new Particle.DustOptions(Color.fromRGB(170, 0, 255), 1.2f);
                drawBoxWireframe(player, purple, minX, minY, minZ, maxX, maxY, maxZ);
            }
        }.runTaskTimer(plugin, 0L, 10L);

        visualizationTasks.put(playerUUID, task);
    }

    /**
     * Stops any active particle preview (visualizeArea or visualizePendingArea) for a
     * player. Used when a /spawnelytra setup session ends via "save" or "cancel" - the
     * latter has no follow-up preview call of its own to naturally replace/stop the task.
     */
    public void cancelVisualization(Player player) {
        BukkitTask existing = visualizationTasks.remove(player.getUniqueId());
        if (existing != null) existing.cancel();
    }

    /**
     * Mirrors the actual area check's dimensionality: when ignoreYInSpawnRadius is on,
     * the check is 2D (X/Z only), so a flat outline at the player's height is accurate.
     * When it's off, the check is genuinely 3D (a sphere for the radius, a full box for
     * the rectangle), so the outline is drawn as a wireframe sphere/box instead.
     */
    private void drawAreaOutline(Player player) {
        if (!player.getWorld().equals(world)) return;

        Particle.DustOptions purple = new Particle.DustOptions(Color.fromRGB(170, 0, 255), 1.2f);

        if (useRectangularArea) {
            if (ignoreYInSpawnRadius) {
                double y = player.getLocation().getY();
                drawRectangleOutline(player, purple, rectMinX, rectMaxX, rectMinZ, rectMaxZ, y);
                drawPerimeterVerticalIndicators(player, purple, rectMinX, rectMaxX, rectMinZ, rectMaxZ, y);
            } else {
                drawBoxWireframe(player, purple, rectMinX, rectMinY, rectMinZ, rectMaxX, rectMaxY, rectMaxZ);
            }
        } else {
            if (ignoreYInSpawnRadius) {
                double y = player.getLocation().getY();
                drawCircleOutline(player, purple, y);

                Location center = getSpawnCenter();
                int tickCount = radialMarkerCount(spawnRadius);
                for (int i = 0; i < tickCount; i++) {
                    double rad = Math.toRadians(360.0 * i / tickCount);
                    double x = center.getX() + spawnRadius * Math.cos(rad);
                    double z = center.getZ() + spawnRadius * Math.sin(rad);
                    drawVerticalIndicator(player, purple, x, z, y);
                }
            } else {
                drawSphereWireframe(player, purple);
            }
        }
    }

    /**
     * A short vertical stroke through a point, used to hint that the area extends
     * indefinitely up/down when ignoreYInSpawnRadius makes the check purely 2D (X/Z) -
     * the flat outline alone would otherwise look like a bounded, flat shape.
     */
    private void drawVerticalIndicator(Player player, Particle.DustOptions options, double x, double z, double centerY) {
        for (double dy = -5.0; dy <= 5.0; dy += 1.0) {
            player.spawnParticle(Particle.REDSTONE, new Location(world, x, centerY + dy, z), 1, 0, 0, 0, 0, options);
        }
    }

    /**
     * Vertical indicator ticks all along a flat rectangle's perimeter (not just its 4
     * corners), spaced at a fixed distance in blocks - unlike the circle case, a
     * rectangle's edges are already linear in world space, so a constant spacing here
     * naturally stays constant regardless of the box's size, no radius-based scaling needed.
     */
    private void drawPerimeterVerticalIndicators(Player player, Particle.DustOptions options,
                                                  double minX, double maxX, double minZ, double maxZ, double y) {
        double spacing = PARTICLE_TARGET_SPACING * 3;
        for (double x = minX; x <= maxX; x += spacing) {
            drawVerticalIndicator(player, options, x, minZ, y);
            drawVerticalIndicator(player, options, x, maxZ, y);
        }
        for (double z = minZ; z <= maxZ; z += spacing) {
            drawVerticalIndicator(player, options, minX, z, y);
            drawVerticalIndicator(player, options, maxX, z, y);
        }
    }

    private void drawRectangleOutline(Player player, Particle.DustOptions options, double minX, double maxX, double minZ, double maxZ, double y) {
        drawRectangleOutline(player, options, minX, maxX, minZ, maxZ, y, 1.0);
    }

    private void drawRectangleOutline(Player player, Particle.DustOptions options, double minX, double maxX, double minZ, double maxZ, double y, double step) {
        for (double x = minX; x <= maxX; x += step) {
            player.spawnParticle(Particle.REDSTONE, new Location(world, x, y, minZ), 1, 0, 0, 0, 0, options);
            player.spawnParticle(Particle.REDSTONE, new Location(world, x, y, maxZ), 1, 0, 0, 0, 0, options);
        }
        for (double z = minZ; z <= maxZ; z += step) {
            player.spawnParticle(Particle.REDSTONE, new Location(world, minX, y, z), 1, 0, 0, 0, 0, options);
            player.spawnParticle(Particle.REDSTONE, new Location(world, maxX, y, z), 1, 0, 0, 0, 0, options);
        }
    }

    /**
     * Degrees between particles along a circle of the given radius, aiming for a roughly
     * constant distance in blocks between them (angle-based spacing otherwise grows with
     * radius, leaving large gaps on bigger spawn areas) - clamped so tiny circles don't
     * get an absurd particle count and huge ones don't get spaced too sparsely either.
     */
    private double angleStepDegrees(double radius) {
        double step = Math.toDegrees(PARTICLE_TARGET_SPACING / Math.max(radius, 1));
        return Math.max(2.0, Math.min(15.0, step));
    }

    /**
     * How many evenly-spaced radial markers (sphere meridians, vertical indicator ticks
     * around a circle, ...) to place around a circle of the given radius, aiming for a
     * roughly constant gap between adjacent ones instead of a fixed count that leaves
     * huge gaps on big areas or clutters tiny ones.
     */
    private int radialMarkerCount(double radius) {
        return (int) Math.max(4, Math.min(24,
                Math.round(2 * Math.PI * radius / (PARTICLE_TARGET_SPACING * 3))));
    }

    private void drawCircleOutline(Player player, Particle.DustOptions options, double y) {
        Location center = getSpawnCenter();
        double angleStep = angleStepDegrees(spawnRadius);
        for (double angle = 0; angle < 360; angle += angleStep) {
            double rad = Math.toRadians(angle);
            double x = center.getX() + spawnRadius * Math.cos(rad);
            double z = center.getZ() + spawnRadius * Math.sin(rad);
            player.spawnParticle(Particle.REDSTONE, new Location(world, x, y, z), 1, 0, 0, 0, 0, options);
        }
    }

    private void drawBoxWireframe(Player player, Particle.DustOptions options,
                                   double minX, double minY, double minZ,
                                   double maxX, double maxY, double maxZ) {
        drawBoxWireframe(player, options, minX, minY, minZ, maxX, maxY, maxZ, 1.0);
    }

    private void drawBoxWireframe(Player player, Particle.DustOptions options,
                                   double minX, double minY, double minZ,
                                   double maxX, double maxY, double maxZ, double step) {
        drawRectangleOutline(player, options, minX, maxX, minZ, maxZ, minY, step);
        drawRectangleOutline(player, options, minX, maxX, minZ, maxZ, maxY, step);

        double[][] corners = {{minX, minZ}, {minX, maxZ}, {maxX, minZ}, {maxX, maxZ}};
        for (double[] corner : corners) {
            for (double y = minY; y <= maxY; y += step) {
                player.spawnParticle(Particle.REDSTONE, new Location(world, corner[0], y, corner[1]), 1, 0, 0, 0, 0, options);
            }
        }
    }

    /**
     * A one-time, denser and thicker golden flash of the currently active rectangular
     * area, shown right after a successful /spawnelytra setup save to confirm it stuck -
     * not a repeating task like visualizeArea()/visualizePendingArea().
     */
    public void showSaveConfirmation(Player player) {
        if (!player.getWorld().equals(world) || !useRectangularArea) return;

        Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 2.5f);
        drawBoxWireframe(player, gold, rectMinX, rectMinY, rectMinZ, rectMaxX, rectMaxY, rectMaxZ, 0.5);
    }

    private void drawSphereWireframe(Player player, Particle.DustOptions options) {
        // A globe-style wireframe: one equator plus several evenly-spaced meridians
        // (vertical great circles through the poles), instead of just 3 orthogonal circles.
        Location center = getSpawnCenter();
        double cx = center.getX(), cy = center.getY(), cz = center.getZ();
        double r = spawnRadius;
        double angleStep = angleStepDegrees(r);

        // Equator
        for (double angle = 0; angle < 360; angle += angleStep) {
            double rad = Math.toRadians(angle);
            double x = cx + r * Math.cos(rad);
            double z = cz + r * Math.sin(rad);
            player.spawnParticle(Particle.REDSTONE, new Location(world, x, cy, z), 1, 0, 0, 0, 0, options);
        }

        // Meridians - each one traced fully (phi 0-360) covers a whole vertical great
        // circle, so spacing their planes across only 0-180 degrees is enough to avoid
        // drawing the same circle twice. More meridians for bigger spheres, so the gaps
        // between adjacent meridian lines at the equator don't grow with the radius.
        int meridianCount = radialMarkerCount(r);
        for (int m = 0; m < meridianCount; m++) {
            double theta = Math.toRadians(180.0 * m / meridianCount);
            double cosT = Math.cos(theta), sinT = Math.sin(theta);

            for (double phi = 0; phi < 360; phi += angleStep) {
                double rad = Math.toRadians(phi);
                double horizontal = r * Math.sin(rad);
                double y = cy + r * Math.cos(rad);
                double x = cx + horizontal * cosT;
                double z = cz + horizontal * sinT;
                player.spawnParticle(Particle.REDSTONE, new Location(world, x, y, z), 1, 0, 0, 0, 0, options);
            }
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        visualizationTasks.values().forEach(BukkitTask::cancel);
        visualizationTasks.clear();
    }

    /**
     * Re-detects which already-online players are on Bedrock. onPlayerJoin() only fires
     * for new connections, so a fresh SpawnBoostListener (created on /spawnelytra reload)
     * would otherwise have an empty bedrockPlayers set for anyone already connected,
     * silently treating them as Java players (wrong messages, no virtual elytra) until
     * they reconnect.
     */
    public void detectOnlineBedrockPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (BedrockSupport.isBedrockPlayer(plugin, player)) {
                bedrockPlayers.add(player.getUniqueId());
            }
        }
    }

    public void loadData() {
        DataManager dataManager = DataManager.getInstance();
        DataManager.LoadedFlyingData data = dataManager.loadFlyingData();

        flying.addAll(data.flyingPlayers);
        boostCount.putAll(data.boostCounts);

        if (SpawnElytra.isDebugMode()) {
            plugin.getLogger().info("[debug] Loaded " + data.flyingPlayers.size() + " flying, "
                    + data.boostCounts.size() + " with boosts used");
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
            Map<UUID, Integer> boostCountCopy = new HashMap<>(boostCount);

            // Save asynchronously to prevent server lag
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                DataManager.getInstance().saveFlyingData(flyingCopy, boostCountCopy);
            });

            saveScheduled = false;
        }, 40L);
    }

    public void saveDataSync() {
        // Synchronous save for shutdown
        DataManager.getInstance().saveFlyingData(new ArrayList<>(flying), new HashMap<>(boostCount));
    }
}
