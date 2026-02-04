package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.renderBoundingBox
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.boss.wither.WitherBoss

object DungeonESP : Module(
    name = "Dungeon ESP",
    description = "ESP for bats/withers.",
    category = NCategory.NECRON
) {
    private val witherESP by BooleanSetting("Wither ESP", true, desc = "Highlight Wither entities")
    private val batESP by BooleanSetting("Bat ESP", true, desc = "Highlight Bat entities")
    private val onlyInBoss by BooleanSetting("Only in Boss", false, desc = "Only show ESP in boss rooms")
    private val color by ColorSetting("Highlight color", Colors.MINECRAFT_GOLD, true, desc = "The color of the highlight.")
    private val renderStyle by SelectorSetting("Render Style", "Outline", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.")

    init {
        on<RenderEvent.Extract> {
            if (!DungeonUtils.inDungeons) return@on
            val level = mc.level ?: return@on

            for (entity in level.entitiesForRendering()) {
                when {
                    witherESP && entity is WitherBoss -> {
                        if (entity.isInvisible || (entity.yRot in 0.0f..10f && entity.xRot == 0.0f)) continue
                        if (onlyInBoss && !DungeonUtils.inBoss) continue

                        val floor = DungeonUtils.floor
                        if (floor?.floorNumber != 7) continue
                        drawStyledBox(entity.renderBoundingBox, color, renderStyle, false)
                    }

                    batESP && entity.type == EntityType.BAT -> {
                        if (entity.isInvisible) continue
                        drawStyledBox(entity.renderBoundingBox, color, renderStyle, false)
                    }
                }
            }
        }
    }
}