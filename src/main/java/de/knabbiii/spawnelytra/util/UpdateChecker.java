package de.knabbiii.spawnelytra.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;

/**
 * Checks for updates on Modrinth asynchronously.
 */
public final class UpdateChecker {

    private static final String MODRINTH_API =
            "https://api.modrinth.com/v2/project/spawnelytra/version";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static volatile String latestVersion = null;
    private static volatile String downloadUrl = null;
    private static volatile boolean updateAvailable = false;

    private UpdateChecker() {}

    /**
     * Fires an async update check against the Modrinth API.
     * Results are stored in static fields and can be read via the getters.
     */
    public static void checkAsync(Plugin plugin) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MODRINTH_API))
                .header("User-Agent", "SpawnElytra/" + plugin.getDescription().getVersion()
                        + " (https://modrinth.com/plugin/spawnelytra)")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        plugin.getLogger().warning("[UpdateChecker] Modrinth returned HTTP "
                                + response.statusCode());
                        return;
                    }
                    try {
                        JsonArray versions = JsonParser.parseString(response.body())
                                .getAsJsonArray();
                        if (versions.isEmpty()) return;

                        var latest = versions.get(0).getAsJsonObject();
                        String latestVer = latest.get("version_number").getAsString();
                        String current = plugin.getDescription().getVersion();

                        var files = latest.getAsJsonArray("files");
                        downloadUrl = (files != null && !files.isEmpty())
                                ? files.get(0).getAsJsonObject().get("url").getAsString()
                                : "https://modrinth.com/plugin/spawnelytra";

                        latestVersion = latestVer;
                        updateAvailable = isNewer(current, latestVer);

                        if (updateAvailable) {
                            plugin.getLogger().info("[UpdateChecker] A new version is available:"
                                    + " v" + latestVer + " (you have v" + current + ")");
                            plugin.getLogger().info("[UpdateChecker] Download: " + downloadUrl);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING,
                                "[UpdateChecker] Failed to parse Modrinth response", e);
                    }
                })
                .exceptionally(e -> {
                    plugin.getLogger().warning("[UpdateChecker] Could not reach Modrinth: "
                            + e.getMessage());
                    return null;
                });
    }

    /**
     * Returns true if {@code latest} is a higher version than {@code current}.
     */
    static boolean isNewer(String current, String latest) {
        current = current.replaceFirst("^[vV]", "");
        latest  = latest .replaceFirst("^[vV]", "");

        String[] cur = current.split("\\.");
        String[] lat = latest .split("\\.");
        int len = Math.max(cur.length, lat.length);

        for (int i = 0; i < len; i++) {
            int c = i < cur.length ? parseVersionPart(cur[i]) : 0;
            int l = i < lat.length ? parseVersionPart(lat[i]) : 0;
            if (l > c) return true;
            if (l < c) return false;
        }
        return false;
    }

    private static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9].*", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean isUpdateAvailable() { return updateAvailable; }
    public static String  getLatestVersion()  { return latestVersion;   }
    public static String  getDownloadUrl()    { return downloadUrl;     }
}
