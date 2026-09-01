package de.knabbiii.spawnelytra.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.knabbiii.spawnelytra.SpawnElytra;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class DataManager {
    private static DataManager instance;
    private final JavaPlugin plugin;
    private final File dataFile;
    private final Gson gson;

    public DataManager(JavaPlugin plugin) {
        instance = this;
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "flying_players.json");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
    }

    public static DataManager getInstance() {
        return instance;
    }

    /**
     * Save flying players to JSON.
     * Chestplate backups for Bedrock players are stored on the player's own
     * PersistentDataContainer instead, so they survive restarts without needing to be saved here.
     */
    public void saveFlyingData(List<UUID> flyingPlayers, List<UUID> boosted) {
        if (SpawnElytra.isDebugMode()) plugin.getLogger().info("saveFlyingData called with " + flyingPlayers.size() + " flying players");

        try {
            // Ensure parent directory exists
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }

            List<PlayerFlyingData> dataList = new ArrayList<>();

            for (UUID uuid : flyingPlayers) {
                PlayerFlyingData data = new PlayerFlyingData();
                data.uuid = uuid.toString();
                data.boosted = boosted.contains(uuid);
                dataList.add(data);
            }

            if (SpawnElytra.isDebugMode()) plugin.getLogger().info("Writing to file: " + dataFile.getAbsolutePath());
            try (Writer writer = new FileWriter(dataFile)) {
                gson.toJson(dataList, writer);
            }
            if (SpawnElytra.isDebugMode()) plugin.getLogger().info("Successfully saved " + dataList.size() + " flying players");

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save flying data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load flying players from JSON
     * Called on server startup
     */
    public LoadedFlyingData loadFlyingData() {
        LoadedFlyingData result = new LoadedFlyingData();

        if (!dataFile.exists()) {
            return result;
        }

        try (Reader reader = new FileReader(dataFile)) {
            Type listType = new TypeToken<List<PlayerFlyingData>>() {
            }.getType();
            List<PlayerFlyingData> dataList = gson.fromJson(reader, listType);

            if (dataList != null) {
                for (PlayerFlyingData data : dataList) {
                    try {
                        UUID uuid = UUID.fromString(data.uuid);
                        result.flyingPlayers.add(uuid);

                        if (data.boosted) result.boosted.add(uuid);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID: " + data.uuid);
                    }
                }

                if (!result.flyingPlayers.isEmpty() && SpawnElytra.isDebugMode()) {
                    plugin.getLogger().info("Loaded " + result.flyingPlayers.size() + " Bedrock players to restore");
                }
            }

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load flying data: " + e.getMessage());
        }

        return result;
    }

    private static class PlayerFlyingData {
        String uuid;
        Boolean boosted;
    }

    public static class LoadedFlyingData {
        public Set<UUID> flyingPlayers = new HashSet<>();
        public Set<UUID> boosted = new HashSet<>();
    }

}
