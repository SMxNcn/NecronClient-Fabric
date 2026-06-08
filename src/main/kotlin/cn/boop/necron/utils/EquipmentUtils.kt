package cn.boop.necron.utils

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.GuiEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.sendCommand
import kotlinx.coroutines.suspendCancellableCoroutine
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import kotlin.coroutines.resume

object EquipmentUtils {
    private var callback: ((Boolean) -> Unit)? = null
    private var pendingSlots = listOf<Int>()
    private var isProcessing = false
    private var currentIndex = 0
    private var containerId = -1
    private var calledFromThis = false

    init {
        on<GuiEvent.Open> {
            containerId = mc.player?.containerMenu?.containerId ?: return@on
            if (!calledFromThis || isProcessing) return@on
            handleGuiOpen(screen)
        }
    }

    /**
     * Swaps multiple equipment items in sequence.
     * @param itemIds List of item IDs to be clicked in order.
     * @return True if all swaps are successful, false otherwise.
     */
    suspend fun swapEquipment(itemIds: List<String>): Boolean {
        if (calledFromThis || isProcessing) return false
        val slots = itemIds.mapNotNull { findItemByID(it).takeIf { slot -> slot != -1 } }
        if (slots.isEmpty()) return false

        return suspendCancellableCoroutine { cont ->
            callback = { result -> cont.resume(result) }
            pendingSlots = slots
            currentIndex = 0
            isProcessing = false
            calledFromThis = true
            sendCommand("equipment")

            schedule(200) {
                if (callback != null) {
                    callback?.invoke(false)
                    reset()
                }
            }
        }
    }

    private fun handleGuiOpen(screen: Screen?) {
        val chest = (screen as? AbstractContainerScreen<*>) ?: run { callback?.invoke(false); return }
        if (!chest.title.string.contains("Your Equipment")) run { callback?.invoke(false); return }

        schedule((8..10).random()) {
            if (!isProcessing) { processNextItem() }
        }
    }

    private fun processNextItem() {
        if (currentIndex >= pendingSlots.size) {
            schedule((6..8).random()) {
                mc.player?.connection?.send(ServerboundContainerClosePacket(containerId))
                mc.setScreen(null)
                callback?.invoke(true)
                reset()
            }
            return
        }

        isProcessing = true

        mc.player?.clickPlayerInventorySlot(pendingSlots[currentIndex], containerId)
        currentIndex++

        schedule((8..10).random()) { processNextItem() }
    }

    private fun reset() {
        callback = null
        pendingSlots = emptyList()
        isProcessing = false
        currentIndex = 0
        containerId = -1
        calledFromThis = false
    }
}