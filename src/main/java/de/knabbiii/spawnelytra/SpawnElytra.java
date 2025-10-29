package de.knabbiii.spawnelytra;

import de.knabbiii.spawnelytra.commands.SpawnElytraCommand;
import de.knabbiii.spawnelytra.listener.SpawnBoostListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class SpawnElytra extends JavaPlugin {
    
    private static SpawnElytra instance;
    private SpawnBoostListener listener;

    public static SpawnElytra getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        
        this.listener = SpawnBoostListener.create(this);
        getServer().getPluginManager().registerEvents(listener, this);
        
        SpawnElytraCommand commandHandler = new SpawnElytraCommand();
        Objects.requireNonNull(getCommand("spawnelytra")).setExecutor(commandHandler);
        Objects.requireNonNull(getCommand("spawnelytra")).setTabCompleter(commandHandler);
        
        getLogger().info("SpawnElytra v2.2 enabled! Modern Java 21 implementation with enhanced features.");
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            listener.cancel();
        }
        instance = null;
        getLogger().info("SpawnElytra v2.2 disabled!");
    }
    
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        
        if (listener != null) {
            listener.cancel();
        }
        
        this.listener = SpawnBoostListener.create(this);
        getServer().getPluginManager().registerEvents(listener, this);
        
        getLogger().info("SpawnElytra configuration reloaded!");
    }
}