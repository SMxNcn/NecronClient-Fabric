package cn.boop.necron.utils

import cn.boop.necron.mixin.KeyMappingAccessor
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.containsOneOf
import com.odtheking.odin.utils.customData
import com.odtheking.odin.utils.itemId
import net.minecraft.client.KeyMapping
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

fun modMessage(message: Any?, prefix: String = "§bNecron §8»§r ", chatStyle: Style? = null) {
    val text = Component.literal("$prefix$message")
    chatStyle?.let { text.setStyle(chatStyle) }
    if (mc.isSameThread) mc.gui?.chat?.addMessage(text)
    else mc.execute { mc.gui?.chat?.addMessage(text) }
}

fun String.removeFormatting(): String {
    return this.replace(Regex("§[0-9a-fk-or]"), "")
}

fun Component.removeFormattingToString(): String {
    return this.string.replace(Regex("§[0-9a-fk-or]"), "")
}

inline val ItemStack.itemUpgradeLevel: Int
    get() = customData.getInt("upgrade_level").orElse(0)

fun rightClick() {
    val key = mc.options.keyUse
    val actualKey = (key as KeyMappingAccessor).boundKey
    KeyMapping.set(actualKey, true)
    KeyMapping.click(actualKey)
    KeyMapping.set(actualKey, false)
}

fun leftClick() {
    val key = mc.options.keyAttack
    val actualKey = (key as KeyMappingAccessor).boundKey
    KeyMapping.set(actualKey, true)
    KeyMapping.click(actualKey)
    KeyMapping.set(actualKey, false)
}

fun findItemByID(itemID: String?): Int {
    if (itemID.isNullOrEmpty()) return -1
    val player = mc.player ?: return -1

    return (0 until 36)
        .firstOrNull { slot ->
            val stack = player.inventory.getItem(slot)
            !stack.isEmpty && stack.itemId.contains(itemID, ignoreCase = true)
        } ?: -1
}

fun clickInventorySlot(slot: Int, containerId: Int, rightClick: Boolean = false) {
    val player = mc.player ?: return

    mc.execute {
        mc.gameMode?.handleInventoryMouseClick(containerId, slot, if (rightClick) 1 else 0, ClickType.PICKUP, player)
    }
}

fun clickPlayerInventorySlot(slot: Int, containerId: Int) {
    val player = mc.player ?: return
    val container = player.containerMenu

    val containerSlots = container.slots.size
    val actualSlot: Int

    when (slot) {
        in 0..8 -> {
            actualSlot = containerSlots - 9 + slot
        }
        in 9..35 -> {
            val containerBaseSlots = containerSlots - 36
            if (containerBaseSlots < 0) return
            actualSlot = containerBaseSlots + (slot - 9)
        }
        else -> return
    }

    if (actualSlot !in 0 until containerSlots) return

    mc.execute {
        mc.gameMode?.handleInventoryMouseClick(containerId, actualSlot, 0, ClickType.PICKUP, player)
    }
}

fun isNormalRod(slot: Int): Boolean =
    mc.player?.let { player ->
        val stack = player.inventory.getItem(slot)
        !stack.isEmpty && stack.item == Items.FISHING_ROD && !stack.itemId.containsOneOf("SOUL_WHIP", "FLAMING_FLAY", ignoreCase = true)
    } ?: false
