package cn.boop.necron.utils.rng

import cn.boop.necron.events.RngEvent
import cn.boop.necron.utils.clean
import cn.boop.necron.utils.cleanString
import cn.boop.necron.utils.legacy
import com.odtheking.odin.utils.lore
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.ChestMenu

object SlayerRngEventHandler {
    private val SLAYER_DROP_PATTERN = Regex("§.§l[A-Z ]+DROP!? §7\\((?:§.\\d+x\\s+)?(§.[^§(]+?)§7\\)")  // https://regex101.com/r/ctsJ16/1
    private val STORED_XP_PATTERN = Regex("§dRNG Meter §f- §d([\\d,]+) Stored XP")
    private val SET_PATTERN = Regex("§aYou set your §d(.+) RNG Meter §ato drop (.+)§a!")

    fun handleChat(message: String): Boolean {
        println(message)
        SLAYER_DROP_PATTERN.find(message)?.let { match ->
            val dropName = match.groupValues[1]
            val slayerKey = SlayerRngManager.getCurrentSlayerKey() ?: return false

            val data = SlayerRngManager.getCurrentSlayerMeter() ?: return true
            val selectedItem = data.item ?: return true
            if (selectedItem.clean == dropName.clean) {
                RngEvent.MeterReset(MeterType.SLAYER, slayerKey, selectedItem, data.score, data.needed).postAndCatch()
            }
            return true
        }

        RNG_RESET_PATTERN.find(message)?.let { match ->
            val item = match.groupValues[1]
            val slayerName = match.groupValues[2]
            val currentItem = SlayerRngManager.getCurrentItem(slayerName)
            if (currentItem != null && currentItem == item) {
                val oldScore = SlayerRngManager.getCurrentScore(slayerName)
                val needed = SlayerRngManager.getCurrentNeeded(slayerName)
                SlayerRngManager.setScore(slayerName, 0)
                RngEvent.MeterReset(MeterType.SLAYER, slayerName, item, oldScore, needed).postAndCatch()
            }
            return true
        }

        STORED_XP_PATTERN.find(message)?.let { match ->
            val xp = match.groupValues[1].replace(",", "").toIntOrNull() ?: return false
            val slayerKey = SlayerRngManager.getCurrentSlayerKey() ?: return false
            SlayerRngManager.setScore(slayerKey, xp)
            return true
        }

        SET_PATTERN.find(message)?.let { match ->
            handleSetMatch(match.groupValues[1], match.groupValues[2])
            return true
        }

        return false
    }

    fun handleGui(screen: AbstractContainerScreen<*>): Boolean {
        if (screen.title.cleanString != "Slayer RNG Meters") return false

        val menu = screen.menu as? ChestMenu ?: return false
        val slots = menu.slots

        for (i in 19..34) {
            if (i >= slots.size) break
            val stack = slots[i].item
            val itemType = stack.item
            val displayRaw = stack.displayName.cleanString
            if (stack.isEmpty) continue
            if (displayRaw != "RNG Meter") continue

            val lore = stack.lore.map { it.legacy }
            parseMeterLore(lore)?.let { (slayerKey, item, score, needed) ->
                if (item != null) SlayerRngManager.setItem(slayerKey, item)
                if (score != null) SlayerRngManager.setScore(slayerKey, score)
                if (needed != null) {
                    SlayerRngManager.checkDataExists(slayerKey)
                    val data = SlayerRngManager.getCurrentSlayerMeter()
                    if (data != null && data.item != null) {
                        data.needed = needed
                    }
                }
            }
        }

        return true
    }

    private fun parseMeterLore(lore: List<String>): MeterLoreResult? {
        var slayerKey: String? = null
        var item: String? = null
        var score: Int? = null
        var needed: Int? = null
        var foundSelectedDrop = false

        for (line in lore) {
            val cleanLine = line.clean

            for ((guiName, key) in SlayerRngManager.SLAYER_GUI_TO_KEY) {
                if (cleanLine.contains(guiName)) {
                    slayerKey = key
                    break
                }
            }

            // STORED_XP_PATTERN in lore: "RNG Meter - 2500 Stored XP"
            Regex("RNG Meter - ([\\d,]+) Stored XP").find(cleanLine)?.let {
                score = it.groupValues[1].replace(",", "").toIntOrNull()
            }

            // Score / Needed format: "----------440,000/3,500,000"
            Regex("""(\d[\d,]*)\s*/\s*(\d[\d.,]*)""").find(cleanLine)?.let {
                score = it.groupValues[1].replace(",", "").toIntOrNull()
                needed = it.groupValues[2].replace(",", "").toIntOrNull()
            }

            if (cleanLine.contains("Selected Drop")) {
                foundSelectedDrop = true
                continue
            }

            if (foundSelectedDrop && item == null && cleanLine.isNotBlank()) {
                item = line
                foundSelectedDrop = false
            }
        }

        slayerKey ?: return null
        return MeterLoreResult(slayerKey, item, score, needed)
    }

    private fun handleSetMatch(guiName: String, rawItem: String) {
        val slayerKey = SlayerRngManager.SLAYER_GUI_TO_KEY[guiName]
        if (slayerKey != null) {
            val cleanItem = rawItem.clean
            if (cleanItem.isNotBlank()) {
                SlayerRngManager.setItem(slayerKey, cleanItem)
                RngEvent.ItemSelected(MeterType.SLAYER, slayerKey, cleanItem).postAndCatch()
            }
        }
    }

    private data class MeterLoreResult(
        val key: String,
        val item: String?,
        val score: Int?,
        val needed: Int?
    )
}
