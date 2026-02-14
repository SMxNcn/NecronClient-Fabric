package cn.boop.necron.commands

import cn.boop.necron.Necron
import cn.boop.necron.features.impl.necron.AutoSell.sellList
import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.utils.GreedyString
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes

val autoSellCommand = Commodore("autosell") {
    literal("add").runs { item: GreedyString? ->
        val lowercase =
            item?.string?.lowercase() ?: mc.player?.mainHandItem?.hoverName?.string?.noControlCodes?.lowercase()
            ?: return@runs modMessage("Either hold an item or write an item name to be added to autosell.")
        if (lowercase in sellList) return@runs modMessage("$lowercase is already in the Auto sell list.")

        modMessage("Added \"$lowercase\" to the Auto sell list.")
        sellList.add(lowercase)
        Necron.config.save()
    }

    literal("remove").runs { item: GreedyString ->
        val lowercase = item.string.lowercase()
        if (lowercase !in sellList) return@runs modMessage("$item isn't in the Auto sell list.")

        modMessage("Removed \"$item\" from the Auto sell list.")
        sellList.remove(lowercase)
        Necron.config.save()
    }

    literal("clear").runs {
        modMessage("Auto sell list cleared.")
        sellList.clear()
        Necron.config.save()
    }

    literal("list").runs {
        if (sellList.isEmpty()) return@runs modMessage("Auto sell list is empty")
        val chunkedList = sellList.chunked(10)
        modMessage("Auto sell list:\n${chunkedList.joinToString("\n")}")
    }
}