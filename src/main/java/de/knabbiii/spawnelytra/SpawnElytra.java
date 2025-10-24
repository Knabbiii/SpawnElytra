package de.knabbiii.spawnelytra;

import de.knabbiii.spawnelytra.commands.SpawnElytraCommand;
import de.knabbiii.spawnelytra.listener.SpawnBoostListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class SpawnElytra extends JavaPlugin {
    
    private SpawnBoostListener listener;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        // Create and register the listener
        this.listener = SpawnBoostListener.create(this);
        getServer().getPluginManager().registerEvents(listener, this);
        
        // Register commands
        SpawnElytraCommand commandHandler = new SpawnElytraCommand(this);
        Objects.requireNonNull(getCommand("spawnelytra")).setExecutor(commandHandler);
        Objects.requireNonNull(getCommand("spawnelytra")).setTabCompleter(commandHandler);
        
        getLogger().info("SpawnElytra has been enabled! Enhanced with features inspired by blax-k's implementation.");
    }

    @Override
    public void onDisable() {
        // Cancel the scheduler task
        if (listener != null) {
            listener.cancel();
        }
        
        getLogger().info("SpawnElytra has been disabled!");
    }
    
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        
        // Restart the listener with new config
        if (listener != null) {
            listener.cancel();
        }
        
        this.listener = SpawnBoostListener.create(this);
        getServer().getPluginManager().registerEvents(listener, this);
        
        getLogger().info("SpawnElytra configuration reloaded!");
    }
}