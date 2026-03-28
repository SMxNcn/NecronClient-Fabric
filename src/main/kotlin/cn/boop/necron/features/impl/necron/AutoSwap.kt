package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import cn.boop.necron.utils.clickPlayerInventorySlot
import cn.boop.necron.utils.findItemByID
import cn.boop.necron.utils.findRodSlot
import cn.boop.necron.utils.rightClick
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.GuiEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.sendCommand
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import java.util.concurrent.Executors

object AutoSwap : Module(
    name = "Auto Swap",
    description = "Auto swap spirit/bonzo",
    category = NCategory.NECRON
) {
    private val useCustomDelay by BooleanSetting("Custom Swap Delay", false, desc = "Customize delay before swapping items.")
    private val custom by DropdownSetting("Delay", false, desc = "Delay settings.").withDependency { useCustomDelay }
    private val spiritDelay by NumberSetting("Spirit Swap Delay", 200f, 100f, 2000f, 50f, desc = "Delay before equipping Spirit Mask.").withDependency { custom }
    private val phoenixDelay by NumberSetting("Phoenix Swap Delay", 200f, 100f, 2000f, 50f, desc = "Delay before switching to fishing rod.").withDependency { custom }

    private val bonzoRegex = Regex("^Your (?:. )?Bonzo's Mask saved your life!$")
    private val spiritRegex = Regex("^Second Wind Activated! Your Spirit Mask saved your life!$")
    private val actionExecutor = Executors.newSingleThreadExecutor()
    private var calledFromAS = false

    init {
        on<ChatPacketEvent> {
            if (!DungeonUtils.inDungeons) return@on
            val delayTime = if (useCustomDelay) spiritDelay.toInt() else 100
            when{
                value.matches(bonzoRegex) -> {
                    actionExecutor.submit {
                        try {
                            Thread.sleep(delayTime + (0L..99L).random())
                            if (Auto4.isDeviceIncomplete()) Auto4.pauseShooting()
                            Thread.sleep(100)
                            sendCommand("equipment")
                            calledFromAS = true
                        } catch (_: InterruptedException) {
                            Thread.currentThread().interrupt()
                        }
                    }
                }

                value.matches(spiritRegex) -> {
                    val lastSlot = mc.player?.inventory?.selectedSlot ?: return@on
                    val delayTime = if (useCustomDelay) phoenixDelay.toInt() else 100
                    if (findRodSlot() == -1) return@on
                    actionExecutor.submit {
                        try {
                            Thread.sleep(delayTime + (0L..99L).random())
                            if (Auto4.isDeviceIncomplete()) Auto4.pauseShooting()
                            Thread.sleep(100)
                            mc.player?.inventory?.selectedSlot = findRodSlot()
                            Thread.sleep(160 + (0L..40L).random())
                            rightClick()
                            Thread.sleep(160 + (0L..40L).random())
                            mc.player?.inventory?.selectedSlot = lastSlot
                            Thread.sleep(50)
                            if (Auto4.isDeviceIncomplete()) Auto4.resumeShooting()
                        } catch (_: InterruptedException) {
                            Thread.currentThread().interrupt()
                        }
                    }
                }
            }
        }

        on<GuiEvent.Open> {
            val chest = (screen as? AbstractContainerScreen<*>) ?: return@on
            if (!calledFromAS) return@on
            val isEquipmentGui = chest.title.string.contains("Equipment")
            val spiritSlot = findItemByID("SPIRIT_MASK")
            val id = mc.player?.containerMenu?.containerId

            if (isEquipmentGui) {
                schedule(6) {
                    id?.let { clickPlayerInventorySlot(spiritSlot, it) }
                    calledFromAS = false
                    schedule(5) {
                        mc.player?.closeContainer()
                        Thread.sleep(50)
                        if (Auto4.isDeviceIncomplete()) Auto4.resumeShooting()
                    }
                }
            }
        }
    }
}