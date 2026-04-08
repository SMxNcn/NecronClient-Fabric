package cn.boop.necron.commands

import cn.boop.necron.utils.EquipmentUtils.swapEquipment
import cn.boop.necron.utils.WardrobeUtils.swapArmorTo
import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.utils.GreedyString
import com.odtheking.odin.OdinMod
import com.odtheking.odin.OdinMod.mc
import kotlinx.coroutines.launch
import net.minecraft.network.chat.Component

val necronCommand = Commodore("necron", "nc") {
    literal("help").runs {
        val helpMsg =
            "§8§m-------------------------------------\n" +
            "§b             NecronClient §7v0.0.2\n" +
            "§r \n" +
            "§b/necron swapArmor <index> [page]\n   §f§l»§r§7 Swap to Wardrobe Slot <index> on [page]\n" +
            "§b/necron swapEquipment <itemIds...>\n   §f§l»§r§7 Switch equipment by item IDs. Split with \" \" or \",\"\n" +
            "§r§8§m-------------------------------------"

        val text = Component.literal(helpMsg)
        mc.execute { mc.gui?.chat?.addMessage(text) }
    }

    literal("swapArmor").runs { index: Int, page: Int? ->
        OdinMod.scope.launch {
            if (page != null) swapArmorTo(index, page)
            else swapArmorTo(index)
        }
    }

    literal("swapEquipment").runs { inputId: GreedyString ->
        val itemIds = inputId.toString().split(Regex("[\\s,]+")).filter { it.isNotBlank() }
        OdinMod.scope.launch {
            swapEquipment(itemIds)
        }
    }
}