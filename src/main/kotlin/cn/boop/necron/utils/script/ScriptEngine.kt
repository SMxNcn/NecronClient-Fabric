package cn.boop.necron.utils.script

import cn.boop.necron.features.impl.necron.Script.canTriggerMsg
import cn.boop.necron.utils.modMessage
import com.odtheking.odin.OdinMod.mc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ScriptEngine {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun execute(config: ScriptConfigJson) {
        if (!config.enabled) return

        scope.launch {
            if (config.enabled && canTriggerMsg()) {
                modMessage("§7Executing script §a[${config.name}] §7on §8[${config.triggerKeyStr}]")
            }

            if (config.initialDelay > 0) {
                delay(config.initialDelay)
            }

            for (actionJson in config.actions) {
                if (!config.enabled) break

                try {
                    val action = createActionInstance(actionJson)

                    action.execute(mc)

                    if (actionJson.delayAfter > 0) {
                        delay(actionJson.delayAfter)
                    }
                } catch (e: Exception) {
                    modMessage("§cError in script '${config.name}': ${e.message}")
                    e.printStackTrace()
                    break
                }
            }
        }
    }

    private fun createActionInstance(json: ScriptActionJson): ScriptAction {
        return when (json.type) {
            ActionType.CLICK_SLOT -> {
                requireNotNull(json.slot) { "ClickSlot action requires 'slot' parameter" }
                ClickSlotAction(json.slot, false)
            }
            ActionType.SEND_COMMAND -> {
                requireNotNull(json.message) { "SendCommand action requires 'command' parameter" }
                SendCommandAction(json.message)
            }
            ActionType.USE_KEY -> {
                requireNotNull(json.keyCodeStr) { "UseKey action requires 'keyCode' parameter" }
                val code = ScriptParser.resolveKeyCode(json.keyCodeStr)
                UseKeyAction(code)
            }
            ActionType.DELAY -> {
                val duration = json.duration ?: 0L
                DelayAction(duration)
            }
            ActionType.SEND_CLIENT -> {
                requireNotNull(json.message) { "SendClient action requires 'message' parameter" }
                SendClientAction(json.message)
            }
        }
    }
}