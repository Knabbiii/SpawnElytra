package dev.kkazi.spawnelytra.mixin;

import dev.kkazi.spawnelytra.SpawnElytraListener;
import dev.kkazi.spawnelytra.SpawnElytraMod;
import dev.kkazi.spawnelytra.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerMixin {
    private static ModConfig config = SpawnElytraMod.getConfig();

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

    @Inject(method = "handleUseItem", at = @At("HEAD"), cancellable = true)
    private void disableFireworks(ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (!config.fireworksEnabled) {
            ItemStack item = player.getItemInHand(packet.getHand());
            if (SpawnElytraListener.getInstance().getFlying().contains(player.getUUID()) && item.is(Items.FIREWORK_ROCKET))
                ci.cancel();
        }
    }

}
