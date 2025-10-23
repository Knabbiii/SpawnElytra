package de.knabbiii.spawnelytra;

import de.knabbiii.spawnelytra.listener.SpawnBoostListener;
import org.bukkit.plugin.java.JavaPlugin;

public class SpawnElytra extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(SpawnBoostListener.create(this), this);
    }

}