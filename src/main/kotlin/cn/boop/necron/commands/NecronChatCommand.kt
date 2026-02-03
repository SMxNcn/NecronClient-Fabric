package cn.boop.necron.commands

import cn.boop.necron.utils.B64Utils
import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.utils.GreedyString
import com.odtheking.odin.OdinMod
import com.odtheking.odin.utils.sendChatMessage
import net.minecraft.network.chat.Component

val necronChatCommand = Commodore("ncc", "nchat") {

    runs { greedy: GreedyString ->
        sendChatMessage(B64Utils.encodeWithOffset(greedy.string.replace("\\&", "§")))
    }

    literal("help").runs {
        val helpMsg =
            "§8§m-------------------------------------\n" +
            "§b              NecronClient Chat\n" +
            "§r \n" +
            "§b用法： \n" +
            "§b /ncc <message> §f§l»§r§7 发送Base64加密消息\n" +
            "§7 使用 '\\&' 替代Minecraft颜色代码\n" +
            "§r \n" +
            "§b颜色示例及对应颜色代码：\n" +
            " §bNecron§8Client  §f§l»§r§7  \\&bNecron\\&8Client\n" +
            "§r§8§m-------------------------------------"

        val text = Component.literal(helpMsg)
        OdinMod.mc.execute { OdinMod.mc.gui?.chat?.addMessage(text) }
    }
}