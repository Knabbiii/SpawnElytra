package dev.kkazi.spawnelytra.mixin;

import dev.kkazi.spawnelytra.SpawnElytraListener;
import dev.kkazi.spawnelytra.SpawnElytraMod;
import dev.kkazi.spawnelytra.config.ModConfig;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerMixin {
    private static final ModConfig config = SpawnElytraMod.getConfig();

    @Shadow public ServerPlayer player;

    @Inject(method = "handlePlayerAbilities", at = @At("HEAD") , cancellable = true)
    private void onHandlePlayerAbilities(ServerboundPlayerAbilitiesPacket packet, CallbackInfo ci) {
        boolean tryToFly = packet.isFlying() && !player.getAbilities().flying;

        if (tryToFly) {
            if (player.gameMode() == GameType.SURVIVAL || player.gameMode() == GameType.ADVENTURE) {
                ci.cancel();
                SpawnElytraListener.getInstance().tryToFly(player);
            }
        }
    }
    
    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void onSwapHands(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (packet.getAction() == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
            SpawnElytraListener listener = SpawnElytraListener.getInstance();
            UUID playerUUID = player.getUUID();
            
            if (config.boostEnabled && listener.getFlying().contains(playerUUID) && !listener.getBoosted().contains(playerUUID)) {
                ci.cancel();
                listener.triggerBoost(player);
            }
        }
    }

}
