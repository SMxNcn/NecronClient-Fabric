package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import cn.boop.necron.utils.script.ScriptManager
import cn.boop.necron.utils.script.ScriptManager.lastFrameKeyStates
import cn.boop.necron.utils.script.ScriptManager.lastFrameKeys
import cn.boop.necron.utils.script.ScriptManager.triggerMap
import cn.boop.necron.utils.script.ScriptManager.triggerScripts
import com.mojang.blaze3d.platform.InputConstants.isKeyDown
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.impl.ActionSetting
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module

object Script : Module(
    name = "Script",
    description = "what",
    category = NCategory.NECRON,
) {
    private val keyTriggerType by SelectorSetting("Trigger Type", "Press", listOf("Release", "Press"), desc = "Key Trigger type")
    private val showTriggerMsg by BooleanSetting("Script Trigger Message", true, desc = "Show Trigger message")
    private val reloadSetting by ActionSetting("Reload Scripts", "Re-scan the scripts folder") { ScriptManager.reloadScripts() }

    init {
        on<TickEvent.End> {
            if (OdinMod.mc.player == null || OdinMod.mc.screen != null) {
                lastFrameKeys.clear()
                return@on
            }

            val currentFrameStates = mutableMapOf<Int, Boolean>()

            for (keyCode in triggerMap.keys) {
                val isPressed = isKeyDown(OdinMod.mc.window, keyCode)
                currentFrameStates[keyCode] = isPressed
                val wasPressed = lastFrameKeyStates[keyCode] ?: false
                val canTrigger = when (keyTriggerType) {
                    0 -> wasPressed && !isPressed
                    1 -> !wasPressed && isPressed
                    else -> false
                }

                if (canTrigger) triggerScripts(keyCode)
            }

            lastFrameKeyStates.clear()
            lastFrameKeyStates.putAll(currentFrameStates)
        }
    }

    fun canTriggerMsg() = showTriggerMsg
}