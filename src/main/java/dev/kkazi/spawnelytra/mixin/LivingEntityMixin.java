package dev.kkazi.spawnelytra.mixin;

import dev.kkazi.spawnelytra.SpawnElytraListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "canGlide", at = @At("HEAD"), cancellable = true)
    private void canGlide(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (livingEntity instanceof ServerPlayer player) {
            if (SpawnElytraListener.getInstance().getFlying().contains(player.getUUID())) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "updateFallFlying", at = @At("HEAD"), cancellable = true)
    private void updateFallFlying(CallbackInfo ci) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (livingEntity instanceof ServerPlayer player) {
            if (SpawnElytraListener.getInstance().getFlying().contains(player.getUUID())) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void jumpFromGround(CallbackInfo ci) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (livingEntity instanceof ServerPlayer player) {
            if (SpawnElytraListener.getInstance().getFlying().contains(player.getUUID())) {
                ci.cancel();
            }
        }
    }
}