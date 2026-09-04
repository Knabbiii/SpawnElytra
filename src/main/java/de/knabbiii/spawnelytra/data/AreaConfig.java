package de.knabbiii.spawnelytra.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

/**
 * Reads/writes area.yml - the plugin-managed overrides for the spawn area's shape
 * and center point, set via /spawnelytra setup and /spawnelytra center.
 *
 * Kept separate from config.yml because Bukkit's FileConfiguration#save() re-serializes
 * the whole file and drops every comment in it - fine for this machine-managed file,
 * but it would silently destroy all the documentation comments in config.yml.
 */
public final class AreaConfig {

    private AreaConfig() {}

    private static File file(Plugin plugin) {
        return new File(plugin.getDataFolder(), "area.yml");
    }

    public static FileConfiguration load(Plugin plugin) {
        return YamlConfiguration.loadConfiguration(file(plugin));
    }

    public static void save(Plugin plugin, FileConfiguration area) {
        try {
            area.save(file(plugin));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save area.yml: " + e.getMessage());
        }
    }
}
