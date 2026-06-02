package cn.boop.necron.mixins;

import cn.boop.necron.utils.RotationUtils;
import com.odtheking.odin.OdinMod;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "turnPlayer", at = @At("TAIL"))
    private void turnPlayer(CallbackInfo ci) {
        Player player = OdinMod.getMc().player;
        if (player != null) {
            RotationUtils.update(player.getYRot(), player.getXRot(), null, 0f, false);
        }
    }
}
