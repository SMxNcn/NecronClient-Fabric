package cn.boop.necron.mixin;

import cn.boop.necron.utils.RotationUtils;
import com.odtheking.odin.OdinMod;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Unique private float raySavedYaw;
    @Unique private float raySavedPitch;

    @Inject(method = "pick(F)V", at = @At("HEAD"))
    private void beforePick(CallbackInfo ci) {
        LocalPlayer player = OdinMod.getMc().player;

        if (player != null) {
            raySavedYaw = player.getYRot();
            raySavedPitch = player.getXRot();

            player.setYRot(RotationUtils.getServerYaw());
            player.setXRot(RotationUtils.getServerPitch());
        }
    }

    @Inject(method = "pick(F)V", at = @At("RETURN"))
    private void afterPick(CallbackInfo ci) {
        LocalPlayer player = OdinMod.getMc().player;

        if (player != null) {
            player.setYRot(raySavedYaw);
            player.setXRot(raySavedPitch);
        }
    }
}
