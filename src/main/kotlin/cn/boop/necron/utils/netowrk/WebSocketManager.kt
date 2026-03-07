package cn.boop.necron.utils.netowrk

class WebSocketManager {
    interface WSMessage {
        val type: String
        val timestamp: Long
    }

    // - C2S -
    data class ClientMessage(
        override val type: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val uuid: String,
        val ign: String,
        val serverIp: String? = null,
        val message: String? = null,
        val data: EventData? = null
    ) : WSMessage {
        companion object {
            fun connect(uuid: String, ign: String, ip: String) = ClientMessage(
                "connect", System.currentTimeMillis(), uuid, ign, serverIp = ip
            )
            fun disconnect(uuid: String, ign: String) = ClientMessage(
                "disconnect", System.currentTimeMillis(), uuid, ign
            )
            fun chat(uuid: String, ign: String, content: String) = ClientMessage(
                "chat", System.currentTimeMillis(), uuid, ign, message = content
            )
            fun event(uuid: String, ign: String, eventData: EventData) = ClientMessage(
                "event", System.currentTimeMillis(), uuid, ign, data = eventData
            )
            fun ping(uuid: String, ign: String) = ClientMessage(
                "ping", System.currentTimeMillis(), uuid, ign
            )
        }
    }

    data class EventData(
        val eventType: String,
        val details: Map<String, String?>
    )

    // - S2C -
    data class ServerMessage(
        override val type: String,
        override val timestamp: Long,
        val from: UserInfo? = null,
        val message: String? = null,
        val data: EventData? = null
    ) : WSMessage

    data class UserInfo(
        val uuid: String,
        val ign: String
    )
}
