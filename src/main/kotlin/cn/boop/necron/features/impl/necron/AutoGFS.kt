package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.fillItemFromSack
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.skyblock.KuudraUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils

object AutoGFS : Module(
    name = "Auto GFS",
    description = "Automatically refills certain items from your sacks.",
    category = NCategory.NECRON
) {
    private val inKuudra by BooleanSetting("In Kuudra", true, desc = "Only gfs in Kuudra.")
    private val inDungeon by BooleanSetting("In Dungeon", true, desc = "Only gfs in Dungeons.")
    private val refillOnDungeonStart by BooleanSetting("Refill on Dungeon Start", true, desc = "Refill when a dungeon starts.")
    private val refillPearl by BooleanSetting("Refill Pearl", true, desc = "Refill ender pearls.")
    private val refillJerry by BooleanSetting("Refill Jerry", true, desc = "Refill inflatable jerrys.")
    private val refillTNT by BooleanSetting("Refill TNT", true, desc = "Refill superboom tnt.")
    private val refillOnTimer by BooleanSetting("Refill on Timer", true, desc = "Refill on a 5s intervals.")
    private val timerIncrements by NumberSetting("Timer Increments", 5, 1, 60, desc = "The interval in which to refill.", unit = "s")

    private val startRegex = Regex("\\[NPC] Mort: Here, I found this map when I first entered the dungeon\\.|\\[NPC] Mort: Right-click the Orb for spells, and Left-click \\(or Drop\\) to use your Ultimate!")

    init {
        scheduleRefill()
        on<ChatPacketEvent> {
            when {
                value.matches(startRegex) -> {
                    if (refillOnDungeonStart) refill()
                }
            }
        }
    }

    private fun scheduleRefill() {
        val delayTicks = timerIncrements * 20
        schedule(delayTicks) {
            if (enabled && refillOnTimer) refill()
            scheduleRefill()
        }
    }

    private fun refill() {
        if (!((inKuudra && KuudraUtils.inKuudra) || (inDungeon && DungeonUtils.inDungeons))) return
        val inventory = mc.player?.inventory ?: return

        inventory.find { it?.itemId == "ENDER_PEARL" }?.takeIf { refillPearl }?.also { fillItemFromSack(16, "ENDER_PEARL", "ender_pearl", false) }

        inventory.find { it?.itemId == "INFLATABLE_JERRY" }?.takeIf { refillJerry }?.also { fillItemFromSack(64, "INFLATABLE_JERRY", "inflatable_jerry", false) }

        inventory.find { it?.itemId == "SUPERBOOM_TNT" }.takeIf { refillTNT }?.also { fillItemFromSack(64, "SUPERBOOM_TNT", "superboom_tnt", false) }
    }
}