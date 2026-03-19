package cn.boop.necron.utils.script

import com.google.gson.annotations.SerializedName

enum class ActionType {
    CLICK_SLOT,
//    SEND_CHAT,
    SEND_COMMAND,
    USE_KEY,
    DELAY,
    SEND_CLIENT
}

data class ScriptActionJson(
    val type: ActionType,

    @SerializedName("slot") val slot: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("keyCode") val keyCodeStr: String? = null,
    @SerializedName("duration") val duration: Long? = null,
    @SerializedName("delayAfter") val delayAfter: Long = 0
)

data class ScriptConfigJson(
    val name: String,
    @SerializedName("triggerKey") val triggerKeyStr: String, // such as "KEY_G"
    val enabled: Boolean = true,
    @SerializedName("initialDelay") val initialDelay: Long = 0,
    val actions: List<ScriptActionJson> = emptyList()
)