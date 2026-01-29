package de.knabbiii.spawnelytra.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.inventory.ItemStack;
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
     * Save flying players to JSON
     */
    public void saveFlyingData(List<UUID> flyingPlayers, List<UUID> boosted, Map<UUID, ItemStack> originalChestplates) {
        plugin.getLogger().info("saveFlyingData called with " + flyingPlayers.size() + " flying players");

        // Commented out because of a weird bug
        // Bedrock players disappear from the flying list before server shutdown
        // Flying bedrock players are correctly in the flying list at all times
        // But somehow at shutdown the list is empty
        /**
        if (flyingPlayers.isEmpty()) {
         plugin.getLogger().info("Flying list is empty, deleting file if it exists");
            if (dataFile.exists()) {
                dataFile.delete();
            }
            return;
         } **/

        try {
            // Ensure parent directory exists
            if (!dataFile.getParentFile().exists()) {
                plugin.getLogger().info("Creating plugin data folder: " + dataFile.getParentFile().getAbsolutePath());
                dataFile.getParentFile().mkdirs();
            }
            
            List<PlayerFlyingData> dataList = new ArrayList<>();

            for (UUID uuid : flyingPlayers) {
                PlayerFlyingData data = new PlayerFlyingData();
                data.uuid = uuid.toString();
                data.boosted = boosted.contains(uuid);

                if (originalChestplates.containsKey(uuid)) {
                    ItemStack chestplate = originalChestplates.get(uuid);
                    data.chestplateData = serializeItemStack(chestplate);
                }

                dataList.add(data);
            }

            plugin.getLogger().info("Writing to file: " + dataFile.getAbsolutePath());
            try (Writer writer = new FileWriter(dataFile)) {
                gson.toJson(dataList, writer);
            }
            plugin.getLogger().info("Successfully saved " + dataList.size() + " flying players");

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

                        if (data.chestplateData != null) {
                            ItemStack chestplate = deserializeItemStack(data.chestplateData);
                            if (chestplate != null) {
                                result.originalChestplates.put(uuid, chestplate);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID: " + data.uuid);
                    }
                }

                if (!result.flyingPlayers.isEmpty()) {
                    plugin.getLogger().info("Loaded " + result.flyingPlayers.size() + " Bedrock players to restore");
                }
            }

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load flying data: " + e.getMessage());
        }

        return result;
    }


    /**
     * THIS CODE IS MADE WITH AI!!!!!!
     * I DONT TAKE RESPONSIBILITY FOR ANYTHING
     */
    private String serializeItemStack(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            org.bukkit.util.io.BukkitObjectOutputStream dataOutput =
                    new org.bukkit.util.io.BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * THIS CODE IS MADE WITH AI!!!!!!
     * I DONT TAKE RESPONSIBILITY FOR ANYTHING
     */
    private ItemStack deserializeItemStack(String data) {
        try {
            ByteArrayInputStream inputStream =
                    new ByteArrayInputStream(Base64.getDecoder().decode(data));
            org.bukkit.util.io.BukkitObjectInputStream dataInput =
                    new org.bukkit.util.io.BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    private static class PlayerFlyingData {
        String uuid;
        String chestplateData;
        Boolean boosted;
    }

    public static class LoadedFlyingData {
        public Set<UUID> flyingPlayers = new HashSet<>();
        public Set<UUID> boosted = new HashSet<>();
        public Map<UUID, ItemStack> originalChestplates = new HashMap<>();
    }

}