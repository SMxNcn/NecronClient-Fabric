package cn.boop.necron.mixins;

import cn.boop.necron.features.impl.necron.ItemStarDisplay;
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

import static cn.boop.necron.utils.ItemUtils.getItemUpgradeLevel;
import static cn.boop.necron.utils.ItemUtils.isSkyBlockItem;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
    @Unique
    ItemStack itemStack;

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemCount(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"))
    private void getStack(Font font, ItemStack itemStack, int i, int j, String string, CallbackInfo ci) {
        this.itemStack = itemStack;
    }

    @ModifyArg(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemCount(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"), index = 4)
    private String modifyCountText(String string) {
        if (itemStack == null || itemStack.isEmpty() || !isSkyBlockItem(itemStack)) return string;

        var customData = itemStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return string;

        int upgradeLevel = getItemUpgradeLevel(itemStack);
        if (upgradeLevel >= 1 && ItemStarDisplay.INSTANCE.getEnabled()) return String.valueOf(upgradeLevel);

        return string;
    }
}
