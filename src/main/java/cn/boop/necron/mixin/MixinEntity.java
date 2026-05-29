package cn.boop.necron.mixin;

import cn.boop.necron.features.impl.necron.Nametags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;

        if (Nametags.INSTANCE.shouldRemoveGlowing() && Nametags.INSTANCE.isDungeonTeammate(player)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
