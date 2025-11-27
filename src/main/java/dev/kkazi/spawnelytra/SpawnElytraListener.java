package dev.kkazi.spawnelytra;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;


import java.util.*;

public class SpawnElytraListener {
    // Singleton
    private static SpawnElytraListener instance;

    public static SpawnElytraListener getInstance() {
        if (instance == null) throw new IllegalStateException("SpawnElytraListener not initialized! Call init() first.");
        return instance;
    }

    public static SpawnElytraListener init(int multiplyValue, int spawnRadius, boolean boostEnabled, Level world, String message) {
        if (instance != null) throw new IllegalStateException("SpawnElytraListener already initialized!");
        instance = new SpawnElytraListener(multiplyValue, spawnRadius, boostEnabled, world, message);
        return instance;
    }

    private final int multiplyValue;
    private final int spawnRadius;
    private final boolean boostEnabled;
    private final Level world;
    private final String message;
    private final List<UUID> flying = new ArrayList<>();
    private final List<UUID> boosted = new ArrayList<>();
    private final Map<UUID, Integer> landingDelay = new HashMap<>();


    public SpawnElytraListener(int multiplyValue, int spawnRadius, boolean boostEnabled, Level world, String message) {
        this.multiplyValue = multiplyValue;
        this.spawnRadius = spawnRadius;
        this.boostEnabled = boostEnabled;
        this.world = world;
        this.message = message;
    }

    public void run() {
        for (Player playerEntity : world.players()) {
            if (!(playerEntity instanceof ServerPlayer player)) continue;
            if (player.gameMode() != GameType.SURVIVAL && player.gameMode() != GameType.ADVENTURE) continue;

            UUID uuid = player.getUUID();
            if (landingDelay.containsKey(uuid)) {
                if (landingDelay.get(uuid) <= 0) {
                    flying.remove(uuid);
                    boosted.remove(uuid);
                    landingDelay.remove(uuid);
                } else {
                    landingDelay.put(uuid, landingDelay.get(uuid) - 1);
                    continue;
                }
            }
            player.getAbilities().mayfly = isInSpawn(player) && !flying.contains(uuid);
            player.onUpdateAbilities();

            if (flying.contains(uuid) && player.onGround()) {
                player.getAbilities().mayfly = false;
                player.stopFallFlying();
                player.onUpdateAbilities();

                landingDelay.put(uuid, 5); // Start delay
            }

            if (boostEnabled && flying.contains(uuid) && !boosted.contains(uuid) && player.getLastClientInput().shift() && !player.onGround()) {
                boosted.add(uuid);
                Vec3 lookDirection = player.getLookAngle();
                Vec3 velocity = lookDirection.scale(multiplyValue);
                player.setDeltaMovement(velocity);
                player.hurtMarked = true;
            }
        }
    }

    public void tryToFly(ServerPlayer player) {
        if (flying.contains(player.getUUID())) return;
        if (isInSpawn(player)) {
            player.getAbilities().mayfly = false;
            player.startFallFlying();
            player.onUpdateAbilities();
            flying.add(player.getUUID());

            if (boostEnabled) {
                String[] messageParts = message.split("%key%");
                String keybindName = "key.sneak"; // F key
                String formatted = messageParts.length == 2
                        ? messageParts[0] + "§e[" + keybindName + "]§r" + messageParts[1]
                        : message;
                player.displayClientMessage(Component.literal(formatted), true); // action bar
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
}
