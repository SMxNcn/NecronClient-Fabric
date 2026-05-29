package cn.boop.necron.events

import cn.boop.necron.utils.clean
import cn.boop.necron.utils.getScoreboard
import cn.boop.necron.utils.getScoreboardLines
import cn.boop.necron.utils.modMessage
import cn.boop.necron.utils.rng.Slayer
import cn.boop.necron.utils.rng.SlayerRngManager
import cn.boop.necron.utils.rng.SlayerState
import cn.boop.necron.utils.rng.getSlayerXPMultiplier
import com.odtheking.odin.utils.romanToInt
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import kotlin.math.roundToInt

object SlayerEventHandler {
    var currentSlayer: Slayer = Slayer.Unknown
        private set
    var currentSlayerState: SlayerState = SlayerState.NOT_IN_SLAYER
        private set

    private var inCombat = false
    private var startTime = 0L
    private var tickCounter = 0

    private val slayerZoneMapping: Map<Island, Map<String, Slayer>> = mapOf(
        Island.Hub to mapOf(
            "Ruins" to Slayer.Sven,
            "Graveyard" to Slayer.Revenant,
            "Crypts" to Slayer.Revenant
        ),
        Island.ThePark to mapOf(
            "Dark Thicket" to Slayer.Sven,
            "Savanna Woodland" to Slayer.Sven,
            "Jungle Island" to Slayer.Sven,
        ),
        Island.SpiderDen to mapOf(
            "Arachne's Burrow" to Slayer.Tarantula
        ),
        Island.CrimsonIsle to mapOf(
            "Stronghold" to Slayer.Inferno,
            "The Wasteland" to Slayer.Inferno,
            "Smoldering Tomb" to Slayer.Inferno,
            "Burning Desert" to Slayer.Tarantula
        ),
        Island.TheEnd to mapOf(
            "Zealot Bruiser Hideout" to Slayer.Voidgloom,
            "Void Sepulture" to Slayer.Voidgloom
        ),
        Island.Rift to mapOf(
            "Stillgore Château" to Slayer.Riftstalker
        )
    )

    fun tick() {
        if (!LocationUtils.isInSkyblock) {
            currentSlayer = Slayer.Unknown
            currentSlayerState = SlayerState.NOT_IN_SLAYER
            inCombat = false
            return
        }

        if (++tickCounter % 10 == 0) {
            updateSlayer()
            tickCounter = 0
        }
        updateSlayerState()
    }

    fun getKillTime(): String {
        if (startTime == 0L) return "0.00s"
        val seconds = (System.currentTimeMillis() - startTime) / 1000.0
        return "${"%.2f".format(seconds - 0.5)}s"
    }

    private fun updateSlayer() {
        val island = LocationUtils.currentArea
        if (island == Island.Unknown) {
            currentSlayer = Slayer.Unknown
            return
        }

        val zone = getCurrentZone() ?: run {
            currentSlayer = Slayer.Unknown
            return
        }
        val newSlayer = slayerZoneMapping[island]?.get(zone) ?: Slayer.Unknown
        currentSlayer = newSlayer
    }

    private fun updateSlayerState() {
        val currentlyInCombat = scoreboardContains("Slay the boss!")

        if (currentlyInCombat && !inCombat) {
            modMessage("§d[RNG Debug] Slayer combat STARTED")
            inCombat = true
            startTime = System.currentTimeMillis()
        } else if (!currentlyInCombat && inCombat) {
            inCombat = false
            modMessage("Slayer took §6${getKillTime()}§7 to kill!")
            addSlayerXPToRNGMeter()
            startTime = 0L
        }

        val newState = when {
            scoreboardContains("Boss slain!") -> SlayerState.BOSS_SLAIN
            currentlyInCombat -> SlayerState.IN_COMBAT
            scoreboardContains("Slayer Quest") && hasCombatXPLine() -> SlayerState.SUMMONING_BOSS
            !scoreboardContains("Slayer Quest") -> SlayerState.NOT_IN_SLAYER
            else -> currentSlayerState
        }
        if (newState != currentSlayerState) {
            modMessage("§d[RNG Debug] Slayer state: $currentSlayerState -> $newState")
        }
        currentSlayerState = newState
    }

    private fun addSlayerXPToRNGMeter() {
        val slayer = currentSlayer
        if (slayer == Slayer.Unknown) return

        val level = getSlayerLevelFromScoreboard()
        val baseXP = getSlayerXPRequirement(slayer, level)
        modMessage("§d[RNG Debug] Slayer XP: slayer=$slayer level=$level baseXP=$baseXP")
        if (baseXP <= 0) {
            modMessage("§d[RNG Debug] Slayer XP: baseXP=0, skipping (level detection failed)")
            return
        }

        val actualXP = (baseXP * getSlayerXPMultiplier()).roundToInt()
        val slayerName = slayer.displayName

        SlayerRngManager.addScore(slayerName, actualXP)
        val currentScore = SlayerRngManager.getCurrentScore(slayerName)
        modMessage("§dRNG Meter §7gained §6${"%,d".format(actualXP)} §7XP! (§6${"%,d".format(currentScore)} §bScore§7)")
    }

    private fun hasCombatXPLine(): Boolean {
        val lines = getScoreboardLines()?.lines ?: return false
        val pattern = Regex("\\(\\d{1,3}(?:,\\d{3})*/\\d{1,3}(?:,\\d{3})*k?\\) (?:Combat|Kills)")
        return lines.any { pattern.containsMatchIn(it) }
    }

    private fun getSlayerLevelFromScoreboard(): Int {
        val scoreboard = getScoreboard()
        val bossNames = setOf(
            "Revenant Horror", "Sven Packmaster", "Tarantula Broodfather",
            "Voidgloom Seraph", "Riftstalker Bloodfiend", "Inferno Demonlord"
        )

        for (line in scoreboard) {
            val cleaned = line.clean
            if (bossNames.any { cleaned.contains(it) }) {
                val parts = cleaned.split(" ")
                if (parts.isNotEmpty()) return romanToInt(parts.last())
            }
        }
        return 0
    }

    private fun getSlayerXPRequirement(slayer: Slayer, level: Int): Int {
        if (slayer == Slayer.Unknown) return 0
        if (slayer == Slayer.Riftstalker) {
            return when (level) {
                1 -> 10; 2 -> 25; 3 -> 60; 4 -> 120; 5 -> 160
                else -> 0
            }
        }
        return when (level) {
            1 -> 5; 2 -> 25; 3 -> 100; 4 -> 500; 5 -> 1500
            else -> 0
        }
    }

    private fun getCurrentZone(): String? {
        val scoreboard = getScoreboard()
        if (scoreboard.isEmpty()) return null
        for (line in scoreboard) {
            Regex("⏣\\s*(.+)").find(line.clean)?.let {
                return it.groupValues[1].trim()
            }
        }
        return null
    }

    private fun scoreboardContains(text: String): Boolean {
        return getScoreboardLines()?.lines?.any { it.contains(text) } ?: false
    }
}
