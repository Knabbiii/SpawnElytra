package de.knabbiii.spawnelytra;

import de.knabbiii.spawnelytra.commands.SpawnElytraCommand;
import de.knabbiii.spawnelytra.data.DataManager;
import de.knabbiii.spawnelytra.listener.SpawnBoostListener;
import de.knabbiii.spawnelytra.util.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class SpawnElytra extends JavaPlugin {
    
    private static SpawnElytra instance;
    private SpawnBoostListener listener;
    private static boolean debugMode = false;

    public static boolean isDebugMode() { return debugMode; }

    public static SpawnElytra getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        debugMode = getConfig().getBoolean("debugMode", false);

        new DataManager(this);
        
        if (getConfig().getBoolean("enableMetrics", true)) {
            new Metrics(this, 28033);
        }
        if (getConfig().getBoolean("checkForUpdates", true)) {
            UpdateChecker.checkAsync(this);
        }
        if (this.listener == null) {
            this.listener = SpawnBoostListener.create(this);
            getServer().getPluginManager().registerEvents(listener, this);
        }

        listener.loadData();
        SpawnElytraCommand commandHandler = new SpawnElytraCommand();
        Objects.requireNonNull(getCommand("spawnelytra")).setExecutor(commandHandler);
        Objects.requireNonNull(getCommand("spawnelytra")).setTabCompleter(commandHandler);
        
        getLogger().info("SpawnElytra v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            listener.cancel();
            listener.saveDataSync(); // Use sync version on shutdown
        }
        instance = null;
        getLogger().info("SpawnElytra v" + getDescription().getVersion() + " disabled!");
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        debugMode = getConfig().getBoolean("debugMode", false);

        if (listener != null) {
            listener.saveData(); // Save before canceling
            listener.cancel();
        }

        this.listener = SpawnBoostListener.create(this);
        listener.loadData(); // Load data into new listener
        getServer().getPluginManager().registerEvents(listener, this);

        getLogger().info("SpawnElytra configuration reloaded!");
    }
}