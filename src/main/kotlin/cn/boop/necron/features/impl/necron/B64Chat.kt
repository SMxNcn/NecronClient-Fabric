package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.B64Utils
import cn.boop.necron.utils.NCategory
import cn.boop.necron.utils.cleanString
import cn.boop.necron.utils.legacy
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import net.minecraft.network.chat.Component

object B64Chat : Module(
    name = "B64 Chat",
    description = "Base 64 chatting.",
    category = NCategory.NECRON
) {
    private val hideMessage by BooleanSetting("Hide Message", true, desc = "Remove chat line that contains origin message.")

    init {
        on<ChatPacketEvent> {
            if (clean.startsWith("Odin »") || clean.startsWith("N »")) return@on
            var clean = component.cleanString
            var message = component.legacy
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
    }

    fun shouldHideMessage(): Boolean {
        return enabled && hideMessage
    }
}