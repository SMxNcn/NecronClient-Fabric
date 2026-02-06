package cn.boop.necron.mixin;

import cn.boop.necron.features.impl.necron.Nametags;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity> {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
    private void onShouldShowName(T livingEntity, double d, CallbackInfoReturnable<Boolean> cir) {
        if (Nametags.INSTANCE.canDisplayNametags()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
