package de.knabbiii.spawnelytra.data;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

/**
 * Loads messages.yml - customizable, player-facing text (boost/flight-timer messages).
 * Unlike area.yml, this is a bundled default resource the plugin never writes to itself;
 * it's extracted to disk once (like config.yml) and only ever edited by the server admin.
 */
public final class MessagesConfig {

    private MessagesConfig() {}

    public static FileConfiguration load(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Reads a message string and translates &-color codes to §, so messages.yml authors
     * can write "&a" instead of raw section-sign codes.
     */
    public static String getMessage(FileConfiguration messages, String key, String def) {
        return ChatColor.translateAlternateColorCodes('&', messages.getString(key, def));
    }
}
