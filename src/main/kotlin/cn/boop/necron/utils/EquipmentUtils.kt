package cn.boop.necron.utils

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.GuiEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.sendCommand
import kotlinx.coroutines.future.await
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import java.util.concurrent.CompletableFuture

object EquipmentUtils {
    private var pendingFuture: CompletableFuture<Boolean>? = null
    private var pendingSlots = listOf<Int>()
    private var isProcessing = false
    private var currentIndex = 0
    private var containerId = -1
    private var calledFromThis = false

    init {
        on<GuiEvent.Open> {
            containerId = mc.player?.containerMenu?.containerId ?: return@on
            if (!calledFromThis) return@on
            handleGuiOpen(screen)
        }
    }

    /**
     * Swaps multiple equipment items in sequence.
     * @param itemIds List of item IDs to be clicked in order.
     * @return True if all swaps are successful, false otherwise.
     */
    suspend fun swapEquipment(itemIds: List<String>): Boolean {
        val slots = itemIds.mapNotNull { findItemByID(it).takeIf { slot -> slot != -1 } }
        if (slots.isEmpty()) return false

        val future = CompletableFuture<Boolean>()
        pendingFuture = future
        pendingSlots = slots
        currentIndex = 0
        isProcessing = false
        calledFromThis = true

        sendCommand("equipment")

        schedule(100) {
            val f = pendingFuture
            if (f != null && !f.isDone) {
                reset()
                f.complete(false)
            }
        }

        return future.await()
    }

    private fun handleGuiOpen(screen: Screen?) {
        if (pendingFuture == null) return
        val chest = (screen as? AbstractContainerScreen<*>) ?: return
        if (!chest.title.string.contains("Your Equipment")) return

        schedule((5..7).random()) {
            if (!isProcessing) {
                processNextItem()
            }
        }
    }

    private fun processNextItem() {
        if (currentIndex >= pendingSlots.size) {
            schedule((5..8).random()) {
                mc.player?.closeContainer()
                pendingFuture?.complete(true)
                reset()
            }
            return
        }

        isProcessing = true

        if (clickPlayerInventorySlot(pendingSlots[currentIndex], containerId)) {
            currentIndex++
        }

        schedule((6..8).random()) {
            processNextItem()
        }
    }

    private fun reset() {
        pendingFuture = null
        pendingSlots = emptyList()
        isProcessing = false
        currentIndex = 0
        containerId = -1
        calledFromThis = false
    }
}