package cn.boop.necron.utils.network

class WebSocketManager {
    data class ServerMessage(
        val type: String,
        val timestamp: Long,
        val from: UserInfo? = null,
        val message: String? = null,
        val data: EventData? = null
    )

    data class UserInfo(
        val uuid: String,
        val ign: String
    )

    data class EventData(
        val eventType: String,
        val details: Map<String, String?>
    )
}
