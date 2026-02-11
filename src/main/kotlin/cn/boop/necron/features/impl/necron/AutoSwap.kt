package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import cn.boop.necron.utils.clickPlayerInventorySlot
import cn.boop.necron.utils.findItemByID
import cn.boop.necron.utils.isNormalRod
import cn.boop.necron.utils.rightClick
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
    private val rodSlot by NumberSetting("Rod Slot", 0, 1, 8, 1, desc = "Not include SkyBlock Menu slot")

    private val bonzoRegex = Regex("^Your (?:. )?Bonzo's Mask saved your life!$")
    private val spiritRegex = Regex("^Second Wind Activated! Your Spirit Mask saved your life!$")
    private val actionExecutor = Executors.newSingleThreadExecutor()
    private var calledFromAS = false

    init {
        on<ChatPacketEvent> {
            if (!DungeonUtils.inDungeons) return@on
            when{
                value.matches(bonzoRegex) -> {
                    actionExecutor.submit {
                        try {
                            Thread.sleep(2000 + (0L..199L).random())
                            sendCommand("equipment")
                            calledFromAS = true
                        } catch (_: InterruptedException) {
                            Thread.currentThread().interrupt()
                        }
                    }
                }

                value.matches(spiritRegex) -> {
                    val lastSlot = mc.player?.inventory?.selectedSlot ?: return@on
                    if (!isNormalRod(rodSlot - 1)) return@on
                    actionExecutor.submit {
                        try {
                            Thread.sleep(2000 + (0L..199L).random())
                            mc.player?.inventory?.selectedSlot = rodSlot - 1
                            Thread.sleep(160 + (0L..40L).random())
                            rightClick()
                            Thread.sleep(160 + (0L..40L).random())
                            mc.player?.inventory?.selectedSlot = lastSlot
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
                    schedule(5) { mc.player?.closeContainer() }
                }
            }
        }
    }
}