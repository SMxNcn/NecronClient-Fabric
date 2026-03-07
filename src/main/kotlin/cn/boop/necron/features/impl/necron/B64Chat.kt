package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.B64Utils
import cn.boop.necron.utils.NCategory
import cn.boop.necron.utils.cleanString
import cn.boop.necron.utils.legacy
import cn.boop.necron.utils.modMessage
import cn.boop.necron.utils.netowrk.WebSocketMessageHandler.handleRareDrop
import cn.boop.necron.utils.netowrk.WSClient
import cn.boop.necron.utils.netowrk.WebSocketManager
import cn.boop.necron.utils.netowrk.WebSocketMessageHandler.handleWsJoin
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.network.chat.Component

object B64Chat : Module(
    name = "B64 Chat",
    description = "Base 64 chatting.",
    category = NCategory.NECRON
) {
    private val hideMessage by BooleanSetting("Hide Message", true, desc = "Remove chat line that contains origin message.")
    private val enableWS by BooleanSetting("Enable WebSocket", false, desc = "Send messages via WebSocket server.")

    var lastIsland: String = ""
    var lastLobbyId: String = ""

    init {
        on<ChatPacketEvent> {
            var clean = component.cleanString
            var message = component.legacy
            if (clean.startsWith("Odin »") || clean.startsWith("N »") || clean.startsWith("WS »")) return@on

            handleRareDrop(message)

            var startIndex = value.indexOf("::")
            if (startIndex != -1) {
                var endIndex = value.indexOf("%]")
                if (endIndex != -1) {
                    var encodePart = value.substring(startIndex, endIndex + 2)
                    val decodedResult = B64Utils.decodeWithOffset(encodePart) ?: return@on
                    val text = Component.literal("§bN §8»§r " + message.replace(encodePart, decodedResult, true))
                    OdinMod.mc.execute { OdinMod.mc.gui?.chat?.addMessage(text) }
                }
            }
        }

        on<WorldEvent.Load> {
            if (WSClient.isConnected) return@on
            schedule(40) {
                handleWsJoin(lastIsland, lastLobbyId)
            }
        }

        on<WorldEvent.Unload> {
            if (!WSClient.isConnected) return@on
            val eventData = WebSocketManager.EventData(
                eventType = "player_leave",
                details = mapOf(
                    "island" to lastIsland,
                    "lobby" to lastLobbyId
                )
            )
            WSClient.sendEvent(eventData)
            WSClient.disconnect()
        }

        WSClient.onBroadcast = { serverMsg ->
            val decoded = serverMsg.message?.let { B64Utils.decodeWithOffset(it) } ?: ""
            val from = serverMsg.from?.ign ?: "Unknown"
            val text = Component.literal("§f$from§r: §f$decoded")
            modMessage(text.string, WSClient.PREFIX)
        }

        WSClient.onEvent = onEvent@{ serverMsg ->
            val from = serverMsg.from?.ign ?: "Unknown"
            val eventData = serverMsg.data ?: return@onEvent

            val displayText = when (eventData.eventType) {
                "rare_drop" -> {
                    val itemName = eventData.details["itemName"] ?: "Unknown Item"
                    "§f$from§r §7got $itemName"
                }

                "reward" -> {
                    val rewardName = eventData.details["rewardName"] ?: "Unknown Reward"
                    val chestName = eventData.details["chestName"] ?: "Unknown Chest"
                    "§f$from§r §7unlocked a $chestName §a§lREWARD!§r ($rewardName§7)"
                }

                "server_change" -> {
                    val toIsland = eventData.details["toIsland"] ?: "Unknown"
                    val toLobby = eventData.details["toLobby"] ?: "unknown"
                    "§f$from§r §7switched to §f$toIsland §8($toLobby)"
                }

                else -> return@onEvent
            }

            val text = Component.literal(displayText)
            modMessage(text.string, WSClient.PREFIX)
        }

        WSClient.onPlayerJoin = { serverMsg ->
            val from = serverMsg.from?.ign ?: "Unknown"
            val island = serverMsg.data?.details?.get("island") ?: "Unknown"
            val text = Component.literal("§a$from§e joined §7($island)")
            modMessage(text.string, WSClient.PREFIX)
        }

        WSClient.onPlayerLeave = { serverMsg ->
            val from = serverMsg.from?.ign ?: "Unknown"
            val text = Component.literal("§c$from§e left")
            modMessage(text.string, WSClient.PREFIX)
        }
    }

    override fun onEnable() {
        super.onEnable()
        if (enableWS) {
            val session = mc.player?.stringUUID!!
            WSClient.initialize(session, mc.player?.name?.string!!)
            WSClient.connect()

            lastIsland = LocationUtils.currentArea.name
            lastLobbyId = LocationUtils.lobbyId ?: "unknown"
        }
    }

    override fun onDisable() {
        super.onDisable()
        if (WSClient.isConnected) {
            val eventData = WebSocketManager.EventData(
                eventType = "player_leave",
                details = mapOf(
                    "island" to lastIsland,
                    "lobby" to lastLobbyId
                )
            )
            WSClient.sendEvent(eventData)
            WSClient.disconnect()
        }
    }

    fun isWSEnabled(): Boolean {
        return enabled && enableWS
    }

    fun shouldHideMessage(): Boolean {
        return enabled && hideMessage
    }
}