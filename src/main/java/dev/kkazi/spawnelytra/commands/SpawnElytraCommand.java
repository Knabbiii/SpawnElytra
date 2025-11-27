package dev.kkazi.spawnelytra.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.kkazi.spawnelytra.SpawnElytraListener;
import dev.kkazi.spawnelytra.SpawnElytraMod;
import dev.kkazi.spawnelytra.config.ModConfig;
import dev.kkazi.spawnelytra.util.PermissionUtil;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class SpawnElytraCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawnelytra")
            .then(Commands.literal("info")
                .executes(SpawnElytraCommand::executeInfo))
            .then(Commands.literal("reload")
                .requires(source -> PermissionUtil.hasPermission(source, "spawnelytra.admin", 2))
                .executes(SpawnElytraCommand::executeReload)));
    }
    
    private static int executeInfo(CommandContext<CommandSourceStack> context) {
        ModConfig config = SpawnElytraMod.getConfig();
        CommandSourceStack source = context.getSource();
        String world = "overworld";
        
        source.sendSystemMessage(Component.literal("§6=== SpawnElytra Info ==="));
        source.sendSystemMessage(Component.literal("§eVersion: §f" + SpawnElytraMod.getModVersion() + " §7(Fabric)"));
        source.sendSystemMessage(Component.literal("§eAuthor: §f" + SpawnElytraMod.getModAuthor()));
        source.sendSystemMessage(Component.literal("§eFabric Port: §f@SchlangeGoto"));
        source.sendSystemMessage(Component.literal("§eWebsite: §f" + SpawnElytraMod.getModWebsite()));
        source.sendSystemMessage(Component.literal(""));
        source.sendSystemMessage(Component.literal("§eWorld: §f" + world));
        source.sendSystemMessage(Component.literal("§eSpawn Radius: §f" + config.spawnRadius));
        source.sendSystemMessage(Component.literal("§eBoost Multiplier: §f" + config.boostStrength));
        source.sendSystemMessage(Component.literal("§eBoost Enabled: §f" + config.boostEnabled));
        source.sendSystemMessage(Component.literal("§eBoost Sound: §f" + config.boostSound));
        
        return 1;
    }
    
    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            AutoConfig.getConfigHolder(ModConfig.class).load();
            ModConfig config = SpawnElytraMod.getConfig();
            
            SpawnElytraListener.getInstance().reloadConfig(
                config.boostStrength,
                config.spawnRadius - 1,
                config.boostEnabled,
                config.message,
                config.boostDirection,
                config.boostSound,
                config.showBoostMessage,
                config.showActivationMessage
            );
            
            source.sendSystemMessage(Component.literal("§8[§d§lSpawnElytra§8] §aConfiguration reloaded successfully!"));
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("§8[§d§lSpawnElytra§8] §cFailed to reload configuration: " + e.getMessage()));
            return 0;
        }
    }
}
