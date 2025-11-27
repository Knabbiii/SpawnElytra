package dev.kkazi.spawnelytra;

import dev.kkazi.spawnelytra.util.PermissionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class SpawnElytraListener {
    private static SpawnElytraListener instance;

    public static SpawnElytraListener getInstance() {
        if (instance == null) throw new IllegalStateException("SpawnElytraListener not initialized! Call init() first.");
        return instance;
    }

    public static SpawnElytraListener init(int multiplyValue, int spawnRadius, boolean boostEnabled, Level world, String message, String boostDirection, String boostSound, boolean showBoostMessage, boolean showActivationMessage) {
        if (instance != null) throw new IllegalStateException("SpawnElytraListener already initialized!");
        instance = new SpawnElytraListener(multiplyValue, spawnRadius, boostEnabled, world, message, boostDirection, boostSound, showBoostMessage, showActivationMessage);
        return instance;
    }

    private int multiplyValue;
    private int spawnRadius;
    private boolean boostEnabled;
    private final Level world;
    private String message;
    private String boostDirection;
    private String boostSound;
    private boolean showBoostMessage;
    private boolean showActivationMessage;
    private final List<UUID> flying = new ArrayList<>();
    private final List<UUID> boosted = new ArrayList<>();
    private final List<UUID> gracePeriod = new ArrayList<>();
    private final Map<UUID, Integer> landingDelay = new HashMap<>();
    private final Map<UUID, Integer> gracePeriodDelay = new HashMap<>();

    public SpawnElytraListener(int multiplyValue, int spawnRadius, boolean boostEnabled, Level world, String message, String boostDirection, String boostSound, boolean showBoostMessage, boolean showActivationMessage) {
        this.multiplyValue = multiplyValue;
        this.spawnRadius = spawnRadius;
        this.boostEnabled = boostEnabled;
        this.world = world;
        this.message = message;
        this.boostDirection = boostDirection;
        this.boostSound = boostSound;
        this.showBoostMessage = showBoostMessage;
        this.showActivationMessage = showActivationMessage;
    }

    public void run() {
        for (Player playerEntity : world.players()) {
            if (!(playerEntity instanceof ServerPlayer player)) continue;
            if (player.gameMode() != GameType.SURVIVAL && player.gameMode() != GameType.ADVENTURE) continue;

            UUID uuid = player.getUUID();
            
            if (gracePeriodDelay.containsKey(uuid)) {
                int remaining = gracePeriodDelay.get(uuid) - 1;
                if (remaining <= 0) {
                    gracePeriod.remove(uuid);
                    gracePeriodDelay.remove(uuid);
                } else {
                    gracePeriodDelay.put(uuid, remaining);
                }
            }
            
            if (landingDelay.containsKey(uuid)) {
                if (landingDelay.get(uuid) <= 0) {
                    flying.remove(uuid);
                    boosted.remove(uuid);
                    gracePeriod.remove(uuid);
                    landingDelay.remove(uuid);
                } else {
                    landingDelay.put(uuid, landingDelay.get(uuid) - 1);
                    continue;
                }
            }
            
            if (!PermissionUtil.canUseFlight(player)) {
                player.getAbilities().mayfly = false;
                player.onUpdateAbilities();
                continue;
            }
            
            boolean isCurrentlyFlying = flying.contains(uuid);
            boolean inSpawnRadius = isInSpawn(player);
            
            if (isCurrentlyFlying) {
                if (!player.isFallFlying()) player.startFallFlying();
                player.getAbilities().mayfly = false;
            } else {
                player.getAbilities().mayfly = inSpawnRadius;
            }
            player.onUpdateAbilities();

            if (flying.contains(uuid) && player.onGround() && !gracePeriod.contains(uuid)) {
                player.getAbilities().mayfly = false;
                player.stopFallFlying();
                player.onUpdateAbilities();

                landingDelay.put(uuid, 3);
            }
        }
    }
    
    public void triggerBoost(ServerPlayer player) {
        UUID uuid = player.getUUID();
        
        if (!PermissionUtil.canUseBoost(player)) return; // Check spawnelytra.useboost permission
        if (!boostEnabled || !flying.contains(uuid) || boosted.contains(uuid)) return;
        
        applyBoost(player, uuid);
    }
    
    private void applyBoost(ServerPlayer player, UUID uuid) {
        boosted.add(uuid);
        
        Vec3 velocity;
        if ("upward".equalsIgnoreCase(boostDirection)) {
            velocity = new Vec3(0, multiplyValue, 0);
        } else {
            velocity = player.getLookAngle().scale(multiplyValue);
        }
        
        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        
        try {
            ResourceLocation soundLocation = ResourceLocation.parse(boostSound);
            BuiltInRegistries.SOUND_EVENT.getOptional(soundLocation).ifPresent(sound -> 
                player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0f, 1.0f)
            );
        } catch (Exception ignored) {}
        
        if (showBoostMessage) {
            player.displayClientMessage(Component.literal("§aBoost activated!"), true);
        }
    }

    public void tryToFly(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (flying.contains(uuid)) return;
        if (landingDelay.containsKey(uuid)) return;
        if (!PermissionUtil.canUseFlight(player)) return;
        if (isInSpawn(player)) {
            player.getAbilities().mayfly = false;
            player.startFallFlying();
            player.onUpdateAbilities();
            flying.add(uuid);
            gracePeriod.add(uuid);
            gracePeriodDelay.put(uuid, 5);

            if (boostEnabled && showActivationMessage) {
                String[] messageParts = message.split("%key%");
                String keybindName = "F";
                String formatted = messageParts.length == 2
                        ? messageParts[0] + "§e[" + keybindName + "]§r" + messageParts[1]
                        : message;
                player.displayClientMessage(Component.literal(formatted), true);
            }
        }
    }

    private boolean isInSpawn(ServerPlayer player) {
        if (!player.level().equals(world)) return false;
        BlockPos spawn = world.getRespawnData().pos();
        return player.blockPosition().closerThan(spawn, spawnRadius);
    }

    public List<UUID> getFlying() {
        return new ArrayList<>(flying);
    }
    
    public List<UUID> getBoosted() {
        return new ArrayList<>(boosted);
    }
    
    public List<UUID> getGracePeriod() {
        return new ArrayList<>(gracePeriod);
    }
    
    public void reloadConfig(int multiplyValue, int spawnRadius, boolean boostEnabled, String message, String boostDirection, String boostSound, boolean showBoostMessage, boolean showActivationMessage) {
        this.multiplyValue = multiplyValue;
        this.spawnRadius = spawnRadius;
        this.boostEnabled = boostEnabled;
        this.message = message;
        this.boostDirection = boostDirection;
        this.boostSound = boostSound;
        this.showBoostMessage = showBoostMessage;
        this.showActivationMessage = showActivationMessage;
    }
}
