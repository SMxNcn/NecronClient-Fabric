package cn.boop.necron.utils

import cn.boop.necron.mixin.KeyMappingAccessor
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.customData
import net.minecraft.client.KeyMapping
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.item.ItemStack

fun modMessage(message: Any?, prefix: String = "§bNecron §8»§r ", chatStyle: Style? = null) {
    val text = Component.literal("$prefix$message")
    chatStyle?.let { text.setStyle(chatStyle) }
    if (mc.isSameThread) mc.gui?.chat?.addMessage(text)
    else mc.execute { mc.gui?.chat?.addMessage(text) }
}

fun String.removeFormatting(): String {
    return this.replace(Regex("§[0-9a-fk-or]"), "")
}

fun Component.removeFormattingToString(): String {
    return this.string.replace(Regex("§[0-9a-fk-or]"), "")
}

inline val ItemStack.itemUpgradeLevel: Int
    get() = customData.getInt("upgrade_level").orElse(0)

fun rightClick() {
    val key = mc.options.keyUse
    val actualKey = (key as KeyMappingAccessor).boundKey
    KeyMapping.set(actualKey, true)
    KeyMapping.click(actualKey)
    KeyMapping.set(actualKey, false)
}

fun leftClick() {
    val key = mc.options.keyAttack
    val actualKey = (key as KeyMappingAccessor).boundKey
    KeyMapping.set(actualKey, true)
    KeyMapping.click(actualKey)
    KeyMapping.set(actualKey, false)
}
