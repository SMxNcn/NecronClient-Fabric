package cn.boop.necron.utils.network

import cn.boop.necron.Necron
import com.google.gson.JsonObject
import com.odtheking.odin.OdinMod.mc
import top.nckim.ws.WsClient
import top.nckim.ws.WsConfig
import top.nckim.ws.WsConnectionState
import top.nckim.ws.WsListener

object WSClient {
    const val PREFIX = "§bWS §8»§r "

    private var wsClient: WsClient? = null

    var playerUUID: String = ""
    var playerIGN: String = ""

    val isConnected: Boolean
        get() = wsClient?.let {
            val state = it.getConnectionState()
            state == WsConnectionState.CONNECTED
        } ?: false

    var onBroadcast: ((WebSocketManager.ServerMessage) -> Unit)? = null
    var onEvent: ((WebSocketManager.ServerMessage) -> Unit)? = null
    var onPlayerJoin: ((WebSocketManager.ServerMessage) -> Unit)? = null
    var onPlayerLeave: ((WebSocketManager.ServerMessage) -> Unit)? = null

    fun initialize(uuid: String, ign: String) {
        playerUUID = uuid
        playerIGN = ign
    }

    fun connect() {
        if (isConnected) return

        // 清理旧的 wsClient，防止它的 reconnection loop 继续运行
        wsClient?.shutdown()
        wsClient = null

        val session = mc.player
        if (playerUUID.isEmpty() && session != null) {
            playerUUID = session.stringUUID
            playerIGN = session.name.string
        }

        val island = try {
            com.odtheking.odin.utils.skyblock.LocationUtils.currentArea.name
        } catch (_: Exception) {
            "Unknown"
        }

        val config = WsConfig(
            source = "necronclient:0.0.3",
            playerUuid = playerUUID,
            playerIgn = playerIGN,
            island = island
        )

        wsClient = WsClient(
            config, serverUrl = WsClient.SERVER_URL
        ).apply {
            addListener(object : WsListener {
                override fun onConnected(onlineCount: Int) {
                    Necron.logger.info("Connected to WebSocket server (online: $onlineCount).")
                }

                override fun onDisconnected() {
                    Necron.logger.info("WS Disconnected.")
                }

                override fun onChatReceived(ign: String, content: String) {
                    val msg = WebSocketManager.ServerMessage(
                        type = "broadcast",
                        timestamp = System.currentTimeMillis(),
                        from = WebSocketManager.UserInfo(uuid = "", ign = ign),
                        message = content
                    )
                    onBroadcast?.invoke(msg)
                }

                override fun onEventReceived(ign: String, eventType: String, data: JsonObject) {
                    val details = mutableMapOf<String, String?>()
                    data.keySet().forEach { key ->
                        val el = data.get(key)
                        details[key] = if (el.isJsonNull) null else el.asString
                    }
                    val msg = WebSocketManager.ServerMessage(
                        type = "event",
                        timestamp = System.currentTimeMillis(),
                        from = WebSocketManager.UserInfo(uuid = "", ign = ign),
                        data = WebSocketManager.EventData(eventType, details)
                    )
                    onEvent?.invoke(msg)
                }

                override fun onPlayerJoin(ign: String) {
                    val msg = WebSocketManager.ServerMessage(
                        type = "player_join",
                        timestamp = System.currentTimeMillis(),
                        from = WebSocketManager.UserInfo(uuid = "", ign = ign)
                    )
                    onPlayerJoin?.invoke(msg)
                }

                override fun onPlayerLeave(ign: String) {
                    val msg = WebSocketManager.ServerMessage(
                        type = "player_leave",
                        timestamp = System.currentTimeMillis(),
                        from = WebSocketManager.UserInfo(uuid = "", ign = ign)
                    )
                    onPlayerLeave?.invoke(msg)
                }

                override fun onErrorReceived(code: String, message: String) {
                    Necron.logger.error("WS Error: $code $message")
                }
            })
            connect()
        }
    }

    fun disconnect() {
        wsClient?.shutdown()
        wsClient = null
    }

    fun sendChat(content: String) {
        wsClient?.sendChat(content)
    }

    fun sendEvent(eventData: WebSocketManager.EventData): Boolean {
        val client = wsClient ?: return false
        val data = JsonObject()
        eventData.details.forEach { (key, value) ->
            value?.let { data.addProperty(key, it) }
        }
        client.sendEvent(eventData.eventType, data)
        return true
    }
}
