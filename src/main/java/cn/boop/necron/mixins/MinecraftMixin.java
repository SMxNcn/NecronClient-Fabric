package cn.boop.necron.mixins;

import cn.boop.necron.features.impl.necron.TitleManager;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @ModifyReturnValue(method = "createTitle", at = @At("RETURN"))
    private String modifyTitle(String originalTitle) {
        if (!TitleManager.INSTANCE.getEnabled()) return originalTitle;
        return TitleManager.buildTitle();
    }
}