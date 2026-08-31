package de.knabbiii.spawnelytra.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects whether a player is connected via a Bedrock client (Geyser/Floodgate).
 * Tries the Floodgate API first, then the Geyser-Spigot API, falling back to a
 * UUID heuristic if neither plugin is present or their API can't be reached.
 */
public final class BedrockSupport {

    private enum DetectionMode {
        UNRESOLVED,
        FLOODGATE,
        GEYSER,
        UUID_HEURISTIC
    }

    private static final Map<UUID, Boolean> CACHE = new ConcurrentHashMap<>();

    private static DetectionMode detectionMode = DetectionMode.UNRESOLVED;

    private static Object floodgateApi;
    private static Method floodgateIsBedrockPlayer;

    private static Object geyserApi;
    private static Method geyserConnectionByUuid;

    private BedrockSupport() {}

    private static void resolveDetectionMode(Plugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("floodgate") != null) {
            try {
                Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                floodgateApi = apiClass.getMethod("getInstance").invoke(null);
                floodgateIsBedrockPlayer = apiClass.getMethod("isFloodgatePlayer", UUID.class);
                if (floodgateApi != null) {
                    detectionMode = DetectionMode.FLOODGATE;
                    plugin.getLogger().info("Bedrock support: detecting Bedrock players via the Floodgate API.");
                    return;
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Floodgate is installed but its API could not be accessed: " + t.getMessage());
            }
        }

        if (Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null) {
            try {
                Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
                geyserApi = apiClass.getMethod("api").invoke(null);
                geyserConnectionByUuid = apiClass.getMethod("connectionByUuid", UUID.class);
                if (geyserApi != null) {
                    detectionMode = DetectionMode.GEYSER;
                    plugin.getLogger().info("Bedrock support: detecting Bedrock players via the Geyser API.");
                    return;
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Geyser-Spigot is installed but its API could not be accessed: " + t.getMessage());
            }
        }

        detectionMode = DetectionMode.UUID_HEURISTIC;
        plugin.getLogger().info("Bedrock support: no Floodgate/Geyser-Spigot API found, falling back to UUID heuristic.");
    }

    public static boolean isBedrockPlayer(Plugin plugin, Player player) {
        if (player == null) return false;
        return CACHE.computeIfAbsent(player.getUniqueId(), uuid -> detect(plugin, uuid));
    }

    /**
     * Drops the cached result for a player, e.g. on quit, to avoid unbounded growth.
     */
    public static void forget(UUID uuid) {
        if (uuid != null) CACHE.remove(uuid);
    }

    private static boolean detect(Plugin plugin, UUID uuid) {
        if (detectionMode == DetectionMode.UNRESOLVED) {
            resolveDetectionMode(plugin);
        }

        switch (detectionMode) {
            case FLOODGATE:
                try {
                    return (boolean) floodgateIsBedrockPlayer.invoke(floodgateApi, uuid);
                } catch (Throwable t) {
                    return isFloodgateStyleUuid(uuid);
                }
            case GEYSER:
                try {
                    return geyserConnectionByUuid.invoke(geyserApi, uuid) != null;
                } catch (Throwable t) {
                    return isFloodgateStyleUuid(uuid);
                }
            default:
                return isFloodgateStyleUuid(uuid);
        }
    }

    private static boolean isFloodgateStyleUuid(UUID uuid) {
        return uuid != null && uuid.getMostSignificantBits() == 0L;
    }
}
