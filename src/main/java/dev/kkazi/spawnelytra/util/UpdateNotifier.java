package dev.kkazi.spawnelytra.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class UpdateNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger("SpawnElytra");
    private static final Set<String> notifiedPlayers = new HashSet<>();
    private static String currentVersion;
    private static String latestVersion;
    private static boolean updateAvailable;
    private static boolean checkEnabled = true;

    public static void register() {}

    public static void setVersion(String version) {
        currentVersion = version;
        if (checkEnabled) {
            checkForUpdates();
        }
    }

    public static void setEnabled(boolean enabled) {
        checkEnabled = enabled;
    }

    private static void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            try {
                URI uri = new URI("https://api.modrinth.com/v2/project/spawnelytra/version");
                HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("User-Agent", "Knabbiii/SpawnElytra");

                if (connection.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    var versions = JsonParser.parseString(response.toString()).getAsJsonArray();
                    for (int i = 0; i < versions.size(); i++) {
                        JsonObject version = versions.get(i).getAsJsonObject();
                        var loaders = version.getAsJsonArray("loaders");
                        
                        boolean isFabric = false;
                        for (int j = 0; j < loaders.size(); j++) {
                            if (loaders.get(j).getAsString().equals("fabric")) {
                                isFabric = true;
                                break;
                            }
                        }
                        
                        if (isFabric) {
                            latestVersion = version.get("version_number").getAsString();
                            break;
                        }
                    }

                    if (latestVersion != null && !currentVersion.equals(latestVersion)) {
                        updateAvailable = true;
                        LOGGER.info("SpawnElytra - Update available: v{} (current: v{})", latestVersion, currentVersion);
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                LOGGER.warn("SpawnElytra - Failed to check for updates: {}", e.getMessage());
            }
        });
    }

    public static void notifyPlayer(ServerPlayer player) {
        if (!checkEnabled || !updateAvailable) return;
        
        String uuid = player.getStringUUID();
        if (notifiedPlayers.contains(uuid)) return;
        
        if (player.hasPermissions(2)) {
            notifiedPlayers.add(uuid);
            
            MinecraftServer server = player.level().getServer();
            if (server != null) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(2000);
                        server.execute(() -> player.sendSystemMessage(Component.literal(
                            "§8[§d§lSpawnElytra§8] §7Update available: §av" + latestVersion 
                            + " §8(§7current: §cv" + currentVersion + "§8) §8- §b§nhttps://modrinth.com/plugin/spawnelytra"
                        )));
                    } catch (InterruptedException ignored) {}
                });
            }
        }
    }
}

