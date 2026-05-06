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
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam

val ncPrefix: Component = coloredChar("N", 0x44aaf8)
    .append(coloredChar("e", 0x47bbf9))
    .append(coloredChar("c", 0x4bccfb))
    .append(coloredChar("r", 0x4eddfc))
    .append(coloredChar("o", 0x52eefe))
    .append(coloredChar("n",0x55ffff))

fun modMessage(message: Any?, prefix: String = "§bNecron §8»§r §7", chatStyle: Style? = null) {
    val text = Component.literal("$prefix$message")
    chatStyle?.let { text.setStyle(chatStyle) }
    if (mc.isSameThread) mc.gui?.chat?.addMessage(text)
    else mc.execute { mc.gui?.chat?.addMessage(text) }
}

fun modMessage(message: Any?) {
    val text = ncPrefix.copy().append(Component.literal(" §8»§r §7$message"))
    if (mc.isSameThread) mc.gui?.chat?.addMessage(text)
    else mc.execute { mc.gui?.chat?.addMessage(text) }
}

val String.clean: String
    get() = this.replace(Regex("§[0-9a-fk-or]"), "")

val Component.cleanString: String
    get() = this.string.replace(Regex("§[0-9a-fk-or]"), "")

inline val ItemStack.itemUpgradeLevel: Int
    get() = customData.getInt("upgrade_level").orElse(0)!!

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

fun clickKey(key: KeyMapping) {
    val actualKey = (key as KeyMappingAccessor).boundKey
    KeyMapping.set(actualKey, true)
    KeyMapping.click(actualKey)
    KeyMapping.set(actualKey, false)
}

fun holdKey(key: KeyMapping, holding: Boolean) {
    val actualKey = (key as KeyMappingAccessor).boundKey
    KeyMapping.set(actualKey, holding)
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
    if (mc.screen == null) return
    val player = mc.player ?: return

    mc.execute {
        mc.gameMode?.handleInventoryMouseClick(containerId, slot, if (rightClick) 1 else 0, ClickType.PICKUP, player)
    }
}

fun clickPlayerInventorySlot(slot: Int, containerId: Int): Boolean {
    if (mc.screen == null) return false
    val player = mc.player ?: return false
    val container = player.containerMenu

    val containerSlots = container.slots.size
    val actualSlot: Int

    when (slot) {
        in 0..8 -> {
            actualSlot = containerSlots - 9 + slot
        }
        in 9..35 -> {
            val containerBaseSlots = containerSlots - 36
            if (containerBaseSlots < 0) return false
            actualSlot = containerBaseSlots + (slot - 9)
        }
        else -> return false
    }

    if (actualSlot !in 0 until containerSlots) return false

    mc.execute {
        mc.gameMode?.handleInventoryMouseClick(containerId, actualSlot, 0, ClickType.PICKUP, player)
    }
    return true
}

fun getScoreboard(): List<String> {
    val scoreboard = mc.level?.scoreboard ?: return emptyList()
    val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return emptyList()

    val scores = scoreboard.listPlayerScores(objective)
    val title = objective.displayName.cleanString

    val lines = scores.sortedBy { it.value() }.reversed().mapNotNull { entry ->
            val owner = entry.owner
            val team = scoreboard.getPlayersTeam(owner)
            val formattedName = PlayerTeam.formatNameForTeam(team, entry.ownerName())
            formattedName.legacy
        }

    return listOf(title) + lines
}

fun getTabList(): List<String> {
    val connection = mc.connection ?: return emptyList()
    return connection.listedOnlinePlayers.sortedBy { it.tabListOrder }.mapNotNull { playerInfo -> playerInfo.tabListDisplayName?.legacy }
}

fun isNormalRod(slot: Int): Boolean =
    mc.player?.let { player ->
        val stack = player.inventory.getItem(slot)
        !stack.isEmpty && stack.item == Items.FISHING_ROD && !stack.itemId.containsOneOf("SOUL_WHIP", "FLAMING_FLAY", ignoreCase = true)
    } ?: false

fun isLeapItem(slot: Int): Boolean =
    mc.player?.let { player ->
        val stack = player.inventory.getItem(slot)
        !stack.isEmpty && stack.item == Items.PLAYER_HEAD && stack.itemId.containsOneOf("INFINITE_SPIRIT_LEAP", "SPIRIT_LEAP", ignoreCase = true)
    } ?: false

fun findRodSlot(): Int = (0..8).firstOrNull { isNormalRod(it) } ?: -1

fun findLeapSlot(): Int = (0..8).firstOrNull { isLeapItem(it) } ?: -1
