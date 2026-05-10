package cn.boop.necron.commands

import cn.boop.necron.features.impl.necron.CropNuker
import cn.boop.necron.utils.modMessage
import cn.boop.necron.utils.waypoints.FarmingWaypoints
import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.utils.GreedyString
import com.odtheking.odin.OdinMod.mc
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

val nwpCommand = Commodore("nwp") {
    literal("list").runs {
        val files = FarmingWaypoints.listFiles()
        if (files.isEmpty()) {
            modMessage("§cNo waypoint files found in config/necron/waypoints")
            return@runs
        }
        val active = FarmingWaypoints.activeFile
        modMessage("§7Available waypoint files:")
        files.forEach { file ->
            val prefix = if (file == active) " §a■" else " §8■"
            val msg = Component.literal("$prefix §7$file.json").withStyle {
                it.withHoverEvent(HoverEvent.ShowText(Component.literal("§7Click to load waypoint!")))
                    .withClickEvent(ClickEvent.RunCommand("/nwp load $file"))
            }
            mc.execute { mc.gui.chat.addMessage(msg) }
        }
    }

    literal("load").runs { file: GreedyString? ->
        val fileName = file?.string?.trim()?.takeIf { it.isNotBlank() }
            ?: return@runs modMessage("§7Usage: §7/nwp load <filename>")
        FarmingWaypoints.load(fileName)
    }

    literal("reload").runs {
        FarmingWaypoints.reload()
    }

    literal("unload").runs {
        FarmingWaypoints.unload()
    }

    literal("setIndex").runs { index: Int ->
        if (index < 1) {
            modMessage("§cIndex must be >= 1.")
            return@runs
        }
        CropNuker.setCurrentActionId(index)
        modMessage("§6Crop Nuker §7target set to waypoint §b#$index.")
    }
}