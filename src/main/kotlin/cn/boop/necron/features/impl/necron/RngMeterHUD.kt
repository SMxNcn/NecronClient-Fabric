package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import cn.boop.necron.utils.rng.*
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.render.getStringWidth
import com.odtheking.odin.utils.render.text
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils

object RngMeterHUD : Module(
    name = "RNG Meter",
    description = "RNG Meter progress tracking for dungeons and slayers.",
    category = NCategory.NECRON
) {
    val hasDaemon by BooleanSetting("Has Daemon", false, desc = "Whether you have unlocked Daemon Shard.")
    val daemonLevel by NumberSetting("Daemon Level", 0, 0, 10, 1, desc = "Daemon shard level.")
        .withDependency { hasDaemon }
    val showBackground by BooleanSetting("Background", true, desc = "Show HUD background.")

    private data class HudRenderData(
        val key: String,
        val meter: RngMeterUserData,
        val type: MeterType
    )

    val hud by HUD(
        name = "RNG Meter",
        desc = "RNG Meter HUD overlay",
        toggleable = true,
        x = 10,
        y = 10
    ) { example ->
        val data: HudRenderData? = if (example) {
            HudRenderData("M7", RngMeterUserData(42069, "§6Necron's Handle", 231599), MeterType.DUNGEON)
        } else if (DungeonUtils.inDungeons) {
            val key = DungeonRngManager.getCurrentFloorKey()
            if (key != null) {
                val meter = DungeonRngManager.getCurrentFloorMeter()
                if (meter != null) HudRenderData(key, meter, MeterType.DUNGEON) else null
            } else null
        } else if (LocationUtils.isInSkyblock) {
            val key = SlayerRngManager.getCurrentSlayerKey()
            if (key != null) {
                val meter = SlayerRngManager.getCurrentSlayerMeter()
                if (meter != null) HudRenderData(key, meter, MeterType.SLAYER) else null
            } else null
        } else null

        val renderData: HudRenderData = data ?: return@HUD Pair(0, 0)
        val (key, meter, type) = renderData

        val paddingX = 4
        val paddingY = 4
        val lineHeight = 9

        // Title: §dRNG Meter §8- §aF7 §8- §d45.67%
        val meterTypeStr = if (type == MeterType.DUNGEON) key else key.replace(" Slayer", "")
        val floorColor = if (meterTypeStr.startsWith("M")) "§c" else "§a"
        val score = meter.score
        val needed = meter.needed ?: 0
        val percentage = if (needed > 0) (score.toDouble() / needed * 100).coerceAtMost(100.0) else 0.0
        val title = "§dRNG Meter §8- $floorColor$meterTypeStr §8- §d${"%.2f".format(percentage)}%"

        // Build content lines matching original Forge layout
        val lines = mutableListOf(title)

        when {
            score > 0 && needed <= 0 -> lines.add("§7Stored Score: §d${"%,d".format(score)}")
            needed > 0 -> {
                lines.add("§7Item: ${meter.item ?: "§cNone"}")
                lines.add(generateMeterBar(score, needed))
            }
            else -> lines.add("§cNo RNG Meter Data!")
        }

        // Calculate dimensions
        val maxWidth = lines.maxOf { getStringWidth(it) }
        val totalWidth = maxWidth + paddingX * 2
        val totalHeight = lines.size * lineHeight + paddingY * 2

        // Draw background (semi-transparent black, matching original alpha ~55%)
        if (showBackground) {
            fill(0, 0, totalWidth, totalHeight, 0x8C000000.toInt())
        }

        // Draw text lines with 9px line spacing
        var currentY = paddingY
        for (line in lines) {
            text(line, paddingX, currentY, shadow = false)
            currentY += lineHeight
        }

        Pair(totalWidth, totalHeight)
    }
}
