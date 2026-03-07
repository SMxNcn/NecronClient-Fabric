package cn.boop.necron.utils.netowrk

import cn.boop.necron.features.impl.necron.B64Chat.lastIsland
import cn.boop.necron.features.impl.necron.B64Chat.lastLobbyId
import cn.boop.necron.utils.clean
import cn.boop.necron.utils.modMessage
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.skyblock.LocationUtils

object WebSocketMessageHandler {
    private val DROP_REGEX = Regex("§[569acd]§l[A-Z ]+ [CD]ROP! (§[0-9a-f][^§].+?)(?:.(§[0-9a-f].+))?$") // https://regex101.com/r/gjcmZ4/5
    private val INFO_REGEX = Regex("(§[6b])([\\d,]+).*?([☘✯])") // https://regex101.com/r/l7v5P2/2
    private val DYE_REGEX = Regex("§r§d§lWOW! §r(.*?) §r§6found an? §r(§[0-9a-f])([^§]+?)Dye.*")
    private val PET_REGEX = Regex("§eWow! §r(.*?) §r§efound a §r§cPhoenix §r§epet!")

    fun handleRareDrop(message: String) {
        if (!WSClient.isConnected) return
        val playerName = mc.player?.name?.string ?: return

        DROP_REGEX.find(message)?.let { dropMatcher ->
            val itemName = dropMatcher.groupValues[1].trim()
                .replace(Regex("^§7\\("), "")
                .replace(Regex("§7\\)"), "")
            val dropInfo = dropMatcher.groupValues[2].trim()
                .replace(Regex("§6\\+"), "+§6")
            sendDrops("$itemName ${parseExtraInfo(dropInfo)}")
            return
        }

        PET_REGEX.find(message)?.let { petMatcher ->
            if (petMatcher.groupValues[1].clean == playerName) {
                sendDrops("§cPhoenix Pet")
            }
            return
        }

        DYE_REGEX.find(message)?.let { dyeMatcher ->
            if (dyeMatcher.groupValues[1].clean == playerName) {
                sendDrops("${dyeMatcher.groupValues[2]}${dyeMatcher.groupValues[3]}Dye")
            }
            return
        }
    }

    fun handleChestReward(itemName: String, chestName: String) {
        if (!WSClient.isConnected) return
        val eventData = WebSocketManager.EventData(
            eventType = "reward",
            details = mapOf(
                "rewardName" to itemName,
                "chestName" to chestName
            )
        )

        WSClient.sendEvent(eventData)
    }

    fun handleWsJoin(isName: String, lobbyId: String) {
        if (WSClient.isConnected) return
        val session = mc.player?.stringUUID!!
        WSClient.initialize(session, mc.player?.name?.string!!)
        WSClient.connect()

        val currentIsland = LocationUtils.currentArea.name
        val currentLobby = LocationUtils.lobbyId ?: "null"

        if (currentIsland != isName || currentLobby != lobbyId) {
            val eventData = WebSocketManager.EventData(
                eventType = "server_change",
                details = mapOf(
                    "fromIsland" to isName,
                    "toIsland" to currentIsland,
                    "fromLobby" to lobbyId,
                    "toLobby" to currentLobby
                )
            )

            if (WSClient.sendEvent(eventData)) {
                modMessage("§7Location: §f$isName §8($lobbyId) §7→ §f$currentIsland §8($currentLobby)", WSClient.PREFIX)
            }

            lastIsland = currentIsland
            lastLobbyId = currentLobby
        }
    }

    private fun parseExtraInfo(info: String): String {
        val infoMatcher = INFO_REGEX.find(info) ?: return ""
        val countMatcher = Regex("(§8x[\\d,]+)").find(info)
        val statsPart = "${infoMatcher.groupValues[1].trim()}(${infoMatcher.groupValues[2].trim()}${infoMatcher.groupValues[3].trim()})"

        return if (countMatcher != null) {
            "${countMatcher.groupValues[1].trim()} $statsPart"
        } else {
            statsPart
        }
    }

    private fun sendDrops(itemName: String) {
        val eventData = WebSocketManager.EventData(
            eventType = "rare_drop",
            details = mapOf("itemName" to itemName)
        )
        WSClient.sendEvent(eventData)
    }
}