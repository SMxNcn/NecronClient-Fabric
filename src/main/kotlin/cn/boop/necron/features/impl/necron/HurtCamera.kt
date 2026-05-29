package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import com.odtheking.odin.OdinMod
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.render.hollowFill
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.Identifier

object HurtCamera : Module(
    name = "Hurt Camera",
    description = "Renders hurt overlay effect.",
    category = NCategory.NECRON
) {
    private val HUD_LAYER: Identifier = Identifier.fromNamespaceAndPath(OdinMod.MOD_ID, "overlay")

    init {
        HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR, HUD_LAYER, this::render)
    }

    fun render(guiGraphics: GuiGraphics, deltaTicks: DeltaTracker) {
        val hurtTime = mc.player?.hurtTime ?: return
        if (hurtTime <= 0 || !enabled || mc.options.hideGui) return

        guiGraphics.hollowFill(0, 0, mc.window.guiScaledWidth, mc.window.guiScaledHeight, 2, Color(255, 0, 0, hurtTime * 0.1f))
    }
}