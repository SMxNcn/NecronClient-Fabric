package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.EquipmentUtils.swapEquipment
import cn.boop.necron.utils.NCategory
import cn.boop.necron.utils.findRodSlot
import cn.boop.necron.utils.rightClick
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AutoSwap : Module(
    name = "Auto Swap",
    description = "Auto swap spirit/bonzo",
    category = NCategory.NECRON
) {
    private val useCustomDelay by BooleanSetting("Custom Swap Delay", false, desc = "Customize delay before swapping items.")
    private val custom by DropdownSetting("Delay", false, desc = "Delay settings.").withDependency { useCustomDelay }
    private val spiritDelay by NumberSetting("Spirit Swap Delay", 200f, 100f, 2000f, 50f, desc = "Delay before equipping Spirit Mask.", unit = "ms").withDependency { custom }
    private val phoenixDelay by NumberSetting("Phoenix Swap Delay", 200f, 100f, 2000f, 50f, desc = "Delay before switching to fishing rod.", unit = "ms").withDependency { custom }

    private val bonzoRegex = Regex("^Your (?:. )?Bonzo's Mask saved your life!$")
    private val spiritRegex = Regex("^Second Wind Activated! Your Spirit Mask saved your life!$")

    init {
        on<ChatPacketEvent> {
            if (!DungeonUtils.inDungeons) return@on

            when {
                value.matches(bonzoRegex) -> OdinMod.scope.launch { handleBonzo() }
                value.matches(spiritRegex) -> OdinMod.scope.launch { handleSpirit() }
            }
        }
    }

    private suspend fun handleBonzo() {
        val delayTime = if (useCustomDelay) spiritDelay.toInt() else 250
        delay(delayTime + (0..99).random().toLong())

        if (Auto4.isDeviceIncomplete()) Auto4.pauseShooting()
        delay(100)

        if (swapEquipment(listOf("SPIRIT_MASK"))) {
            delay(50)
            if (Auto4.isDeviceIncomplete()) Auto4.resumeShooting()
        }
    }

    private suspend fun handleSpirit() {
        val lastSlot = mc.player?.inventory?.selectedSlot ?: return
        val rodSlot = findRodSlot()
        if (rodSlot == -1) return

        val delayTime = if (useCustomDelay) phoenixDelay.toInt() else 250
        delay(delayTime + (0..99).random().toLong())

        if (Auto4.isDeviceIncomplete()) Auto4.pauseShooting()
        delay(100)

        mc.player?.inventory?.selectedSlot = rodSlot
        delay(160 + (0..40).random().toLong())
        rightClick()
        delay(160 + (0..40).random().toLong())
        mc.player?.inventory?.selectedSlot = lastSlot
        delay(50)

        if (Auto4.isDeviceIncomplete()) Auto4.resumeShooting()
    }
}