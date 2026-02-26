package cn.boop.necron.commands

import cn.boop.necron.utils.modMessage
import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.utils.GreedyString
import com.odtheking.odin.OdinMod.mc
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import java.net.URI

val necronCommand = Commodore("necron") {
    literal("help").runs {
        val helpMsg =
            "§8§m-------------------------------------\n" +
            "§b             NecronClient §7v0.0.1\n" +
            "§r \n" +
            "§b/necron profile <player> §f§l»§r§7 获取玩家资料链接\n" +
            "§r§8§m-------------------------------------"

        val text = Component.literal(helpMsg)
        mc.execute { mc.gui?.chat?.addMessage(text) }
    }

    literal("profile").runs { name: GreedyString ->
        val username = mc.user?.name ?: return@runs modMessage("Invalid username!")
        val chat = mc.gui?.chat ?: return@runs

        val title = Component.empty().append(Component.literal("§7Stats/Website of §a$username§7:").withStyle(ChatFormatting.GRAY))

        val skyCrypt: MutableComponent = Component.literal("   §7[§aSky§fCrypt§7]")
            .withStyle { it.withClickEvent(ClickEvent.OpenUrl(URI("https://sky.shiiyu.moe/stats/$username"))) }
            .withStyle { it.withHoverEvent(HoverEvent.ShowText(Component.literal("Click to open SkyCrypt!").withStyle(ChatFormatting.YELLOW))) }

        val dungeonCrypt: MutableComponent = Component.literal("   §7[§8Dungeon§fCrypt§7]")
            .withStyle { it.withClickEvent(ClickEvent.OpenUrl(URI("https://dungeoncrypts.vercel.app/stats/$username"))) }
            .withStyle { it.withHoverEvent(HoverEvent.ShowText(Component.literal("Click to open DungeonCrypt!").withStyle(ChatFormatting.YELLOW))) }

        val ahHistory: MutableComponent = Component.literal("   §7[§eSky§6Cofl§7]")
            .withStyle { it.withClickEvent(ClickEvent.OpenUrl(URI("https://sky.coflnet.com/player/$username"))) }
            .withStyle { it.withHoverEvent(HoverEvent.ShowText(Component.literal("Click to open SkyCoflnet!").withStyle(ChatFormatting.YELLOW))) }

        val eliteFarmer: MutableComponent = Component.literal("   §7[§6El§3ite§7]")
            .withStyle { it.withClickEvent(ClickEvent.OpenUrl(URI("https://elitebot.dev/@$username"))) }
             .withStyle { it.withHoverEvent(HoverEvent.ShowText(Component.literal("Click to open Elite Farmer!").withStyle(ChatFormatting.YELLOW))) }

        chat.addMessage(Component.empty())
        chat.addMessage(title)
        chat.addMessage(skyCrypt)
        chat.addMessage(dungeonCrypt)
        chat.addMessage(ahHistory)
        chat.addMessage(eliteFarmer)
        chat.addMessage(Component.empty())
    }
}