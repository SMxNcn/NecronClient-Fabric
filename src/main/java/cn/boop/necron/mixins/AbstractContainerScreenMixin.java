package cn.boop.necron.mixins;

import cn.boop.necron.utils.ItemRarity;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static cn.boop.necron.utils.ItemUtils.*;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

    @Shadow @Nullable protected Slot hoveredSlot;

    @ModifyArg(method = "renderTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;)V"), index = 5)
    private Identifier applyRarityTooltipStyle(Identifier originalStyle) {
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            ItemStack item = hoveredSlot.getItem();
            if (!isSkyBlockItem(item)) return originalStyle;
            ItemRarity rarity = getItemRarity(item);
            if (rarity != null) return getTooltipStyle(rarity);
        }
        return originalStyle;
    }
}
