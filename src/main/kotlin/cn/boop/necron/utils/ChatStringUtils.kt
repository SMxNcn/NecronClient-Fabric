package cn.boop.necron.utils

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.util.Optional

fun Component.toLegacyString(): String {
    val builder = StringBuilder()

    this.visit({ style: Style, text: String ->
        if (!style.isEmpty) {
            style.color?.let { color ->
                val colorCode = getLegacyColorCode(color.value)
                if (colorCode != null) {
                    builder.append(colorCode)
                }
            }

            if (style.isBold) builder.append("§l")
            if (style.isItalic) builder.append("§o")
            if (style.isUnderlined) builder.append("§n")
            if (style.isStrikethrough) builder.append("§m")
            if (style.isObfuscated) builder.append("§k")
        }

        builder.append(text)
        Optional.empty<Unit>()
    }, Style.EMPTY)

    return builder.toString()
}

private fun getLegacyColorCode(rgb: Int): String? = when (rgb) {
    0x000000 -> "§0"
    0x0000AA -> "§1"
    0x00AA00 -> "§2"
    0x00AAAA -> "§3"
    0xAA0000 -> "§4"
    0xAA00AA -> "§5"
    0xFFAA00 -> "§6"
    0xAAAAAA -> "§7"
    0x555555 -> "§8"
    0x5555FF -> "§9"
    0x55FF55 -> "§a"
    0x55FFFF -> "§b"
    0xFF5555 -> "§c"
    0xFF55FF -> "§d"
    0xFFFF55 -> "§e"
    0xFFFFFF -> "§f"
    else -> null
}