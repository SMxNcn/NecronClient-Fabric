package cn.boop.necron.mixin;

import cn.boop.necron.Necron;
import cn.boop.necron.features.impl.necron.TitleManager;
import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

@Mixin(Window.class)
public class MixinWindow {

    @Redirect(method = "setIcon", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/IconSet;getStandardIcons(Lnet/minecraft/server/packs/PackResources;)Ljava/util/List;"))
    public List<IoSupplier<InputStream>> setCustomIcon(IconSet instance, PackResources packResources) throws IOException {
        if (!TitleManager.INSTANCE.isCustomIcon()) return instance.getStandardIcons(packResources);
        InputStream icon32x = Necron.class.getResourceAsStream("/assets/necron/icon32x.png");
        InputStream icon16x = Necron.class.getResourceAsStream("/assets/necron/icon16x.png");
        return List.of(() -> Objects.requireNonNull(icon32x), () -> Objects.requireNonNull(icon16x));
    }
}
