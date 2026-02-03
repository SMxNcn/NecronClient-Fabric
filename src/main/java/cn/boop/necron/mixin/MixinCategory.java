package cn.boop.necron.mixin;

import com.odtheking.odin.features.Category;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Category.class)
public class MixinCategory {
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void injectNecronCategory(CallbackInfo ci) {
        Category.Companion.custom("Necron");
    }
}
