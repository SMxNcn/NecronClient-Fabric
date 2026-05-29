package cn.boop.necron.utils.rng

import cn.boop.necron.events.RngEvent
import cn.boop.necron.utils.clean
import cn.boop.necron.utils.cleanString
import cn.boop.necron.utils.legacy
import cn.boop.necron.utils.modMessage
import com.odtheking.odin.utils.lore
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.ChestMenu

object DungeonRngEventHandler {
    private val SCORE_PATTERN = Regex("Team Score:\\s*(\\d+)\\s*\\((S\\+?)\\)$")
    private val DESELECT_PATTERN = Regex("§aYou reset your selected drop for your §dCatacombs \\((\\w{1,2})\\) RNG Meter§a!")
    private val SET_PATTERN = Regex("§aYou set your §dCatacombs \\((\\w{1,2})\\) RNG Meter §ato drop (.+)§a!")

    private val lastProcessedTime = mutableMapOf<String, Long>()

    fun handleChat(message: String): Boolean {
        println(message)
        val clean = message.clean

        SCORE_PATTERN.find(clean)?.let { match ->
            val score = match.groupValues[1].toIntOrNull() ?: return false
            val rank = match.groupValues[2]
            val floor = DungeonRngManager.getCurrentFloorKey() ?: return false

            val now = System.currentTimeMillis()
            if (now - (lastProcessedTime[floor] ?: 0L) < 60_000) return true
            lastProcessedTime[floor] = now

            val rankMultiplier = if (rank == "S") 0.7 else 1.0
            val actualScore = (score * rankMultiplier * daemonMultiplier).toInt()

            modMessage("§d[RNG Debug] Dungeon SCORE: raw=$score rank=$rank floor=$floor actualScore=$actualScore (rankMult=$rankMultiplier daemonMult=$daemonMultiplier)")
            DungeonRngManager.addScore(floor, actualScore)
            return true
        }

        DESELECT_PATTERN.find(message)?.let { match ->
            val floor = match.groupValues[1]
            modMessage("§d[RNG Debug] Dungeon DESELECT: floor=$floor")
            DungeonRngManager.setItem(floor, null)
            return true
        }

        RNG_RESET_PATTERN.find(message)?.let { match ->
            val item = match.groupValues[1]
            val meterName = match.groupValues[2]
            val floor = FLOOR_KEY_PATTERN.find(meterName)?.groupValues?.get(1) ?: return false
            modMessage("§d[RNG Debug] Dungeon RESET: floor=$floor item=$item")
            val currentItem = DungeonRngManager.getCurrentItem(floor)
            if (currentItem != null && currentItem == item) {
                DungeonRngManager.setScore(floor, 0)
            }
            return true
        }

        SET_PATTERN.find(message)?.let { match ->
            handleSetMatch(match.groupValues[1], match.groupValues[2])
            return true
        }

        return false
    }

    fun handleGui(screen: AbstractContainerScreen<*>): Boolean {
        if (screen.title.cleanString != "Catacombs RNG Meters") return false

        val menu = screen.menu as? ChestMenu ?: return false
        val slots = menu.slots

        for (i in 19..34) {
            if (i >= slots.size) break
            val stack = slots[i].item
            val displayRaw = stack.displayName.cleanString
            if (stack.isEmpty) continue

            val floorMatch = FLOOR_KEY_PATTERN.find(displayRaw) ?: continue
            val floor = floorMatch.groupValues[1]

            val lore = stack.lore.map { it.legacy }
            parseMeterLore(lore)?.let { (item, score, needed) ->
                if (item != null) {
                    DungeonRngManager.setItem(floor, item)
                }
                if (score != null) {
                    DungeonRngManager.setScore(floor, score)
                }
                if (needed != null) {
                    DungeonRngManager.checkDataExists(floor)
                    val data = DungeonRngManager.getCurrentFloorMeter()
                    if (data != null && data.item != null) {
                        data.needed = needed
                        modMessage("§d[RNG Debug] Dungeon GUI: set needed=$needed for floor=$floor")
                    }
                }
            } ?: modMessage("§d[RNG Debug] Dungeon GUI: parseMeterLore returned null for slot $i")
        }

        return true
    }

    private fun parseMeterLore(lore: List<String>): MeterLoreResult? {
        var item: String? = null
        var score: Int? = null
        var needed: Int? = null
        var foundSelectedDrop = false

        for (line in lore) {
            val cleanLine = line.clean

            if (cleanLine.contains("Selected Drop")) {
                foundSelectedDrop = true
                continue
            }

            if (foundSelectedDrop && item == null && cleanLine.isNotBlank()) {
                item = line
                foundSelectedDrop = false
            }

            // score/needed format: "----------440,000/3,500,000" (可能前导删除线进度条)
            Regex("""(\d[\d,]*)\s*/\s*(\d[\d.,]*)""").find(cleanLine)?.let {
                score = it.groupValues[1].replace(",", "").toIntOrNull()
                needed = it.groupValues[2].replace(",", "").toIntOrNull()
            }

            // Stored Dungeon Score: "Stored Dungeon Score: 1,234"
            Regex("^Stored Dungeon Score:\\s*([\\d,]+)$").find(cleanLine)?.let {
                score = it.groupValues[1].replace(",", "").toIntOrNull()
            }
        }

        // floor is obtained from displayName, not from lore
        return if (item != null || score != null || needed != null) MeterLoreResult(item, score, needed) else null
    }

    private fun handleSetMatch(floor: String, rawItem: String) {
        val item = rawItem.replace("&", "§")
        modMessage("§d[RNG Debug] Dungeon SET: floor=$floor item=${item.clean}")
        DungeonRngManager.setItem(floor, item)
        RngEvent.ItemSelected(MeterType.DUNGEON, floor, item).postAndCatch()
    }

    fun resetDedup() {
        lastProcessedTime.clear()
    }

    private data class MeterLoreResult(
        val item: String?,
        val score: Int?,
        val needed: Int?
    )
}
