package dev.kkazi.spawnelytra;

import dev.kkazi.spawnelytra.commands.SpawnElytraCommand;
import dev.kkazi.spawnelytra.config.ModConfig;
import dev.kkazi.spawnelytra.util.UpdateNotifier;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpawnElytraMod implements ModInitializer {
    public static final String MOD_ID = "spawnelytra";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private int tickCounter = 0;
    private static ModConfig config;

    @Override
    public void onInitialize() {
        AutoConfig.register(ModConfig.class, Toml4jConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        
        UpdateNotifier.setEnabled(config.checkForUpdates);
        UpdateNotifier.setVersion(getModVersion());
        UpdateNotifier.register();
        
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SpawnElytraCommand.register(dispatcher);
        });
        
        ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> {
            ServerLevel world = server.overworld();

            SpawnElytraListener listener = SpawnElytraListener.init(
                    config.boostStrength,
                    config.spawnRadius - 1,
                    config.boostEnabled,
                    world,
                    config.message,
                    config.boostDirection,
                    config.boostSound,
                    config.showBoostMessage,
                    config.showActivationMessage
            );

            LOGGER.info("SpawnElytra v{} enabled!", getModVersion());
            
            ServerPlayConnectionEvents.JOIN.register((handler, sender, server1) -> {
                UpdateNotifier.notifyPlayer(handler.getPlayer());
            });

            ServerTickEvents.END_SERVER_TICK.register(server1 -> {
                tickCounter++;
                if (tickCounter >= 3) {
                    listener.run();
                    tickCounter = 0;
                }
            });
        });
    }
    public static ModConfig getConfig() {
        return config;
    }
    
    public static String getModVersion() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
    
    public static String getModAuthor() {
        return "Knabbiii";
    }
    
    public static String getModWebsite() {
        return "https://github.com/Knabbiii/SpawnElytra";
    }
}
