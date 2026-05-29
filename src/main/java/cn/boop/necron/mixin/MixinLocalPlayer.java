package cn.boop.necron.mixin;

import cn.boop.necron.utils.RotationUtils;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {

    @Unique private float savedYaw;
    @Unique private float savedPitch;

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void beforeSendPosition(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer)(Object)this;

        this.savedYaw = self.getYRot();
        this.savedPitch = self.getXRot();

        self.setYRot(RotationUtils.getServerYaw());
        self.setXRot(RotationUtils.getServerPitch());
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void afterSendPosition(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;

        self.setYRot(this.savedYaw);
        self.setXRot(this.savedPitch);
    }
}
