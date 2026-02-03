package cn.boop.necron.mixin;

import cn.boop.necron.features.impl.necron.ItemStarDisplay;
import cn.boop.necron.utils.UtilsKt;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public class MixinGuiGraphics {
    @Unique ItemStack itemStack;

    @Unique
    private static boolean isSkyBlockItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;
        customData.copyTag();
        return customData.copyTag().contains("id");
    }

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemCount(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"))
    private void getStack(Font font, ItemStack itemStackIn, int i, int j, String string, CallbackInfo ci) {
        itemStack = itemStackIn;
    }

    @ModifyArg(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemCount(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"), index = 4)
    private String modifyCountText(String string) {
        if (itemStack == null || itemStack.isEmpty() || !isSkyBlockItem(itemStack)) return string;

        var customData = itemStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return string;

        int upgradeLevel = UtilsKt.getItemUpgradeLevel(itemStack);
        if (upgradeLevel >= 1 && ItemStarDisplay.INSTANCE.getEnabled()) return String.valueOf(upgradeLevel);

        return string;
    }
}
