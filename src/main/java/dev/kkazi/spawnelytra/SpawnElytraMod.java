package dev.kkazi.spawnelytra;

import dev.kkazi.spawnelytra.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class SpawnElytraMod implements ModInitializer {
    public static final String MOD_ID = "spawnelytra";
    private int tickCounter = 0;
    private static ModConfig config;

    @Override
    public void onInitialize() {
        AutoConfig.register(ModConfig.class, Toml4jConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> {
            ServerLevel world = server.overworld();

            SpawnElytraListener listener = SpawnElytraListener.init(
                    config.boostStrength,      // multiplyValue
                    config.spawnRadius-1,    // spawnRadius
                    config.boostEnabled,   // boostEnabled
                    world,
                    "Press %key% to boost!"
            );

            System.err.println(config);

            ServerTickEvents.END_SERVER_TICK.register(server1 -> {
                tickCounter++;
                if (tickCounter >= 3) {
                    listener.run();
                    tickCounter = 0;
                }
            });

            ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
                if (!config.damageEnabled) {
                    if (entity instanceof ServerPlayer player) {
                        if (listener.getFlying().contains(player.getUUID())) {
                            if (source.is(net.minecraft.world.damagesource.DamageTypes.FALL) || source.is(net.minecraft.world.damagesource.DamageTypes.FLY_INTO_WALL)) {
                                return false; // cancel damage
                            }
                        }
                    }
                }
                return true; // allow other damage
            });
        });
    }
    public static ModConfig getConfig() {
        return config;
    }
}
