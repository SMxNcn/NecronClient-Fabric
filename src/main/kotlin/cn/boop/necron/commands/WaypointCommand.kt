package cn.boop.necron.commands

import cn.boop.necron.utils.modMessage
import cn.boop.necron.utils.waypoints.FarmingWaypoints
import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.utils.GreedyString

val nwpCommand = Commodore("nwp") {
    literal("list").runs {
        val files = FarmingWaypoints.listFiles()
        if (files.isEmpty()) {
            modMessage("§cNo waypoint files found in config/necron/waypoints")
            return@runs
        }
        val active = FarmingWaypoints.activeFile
        val formatted = files.joinToString("\n") { if (it == active) "§a■ §7$it.json" else "§8■ §7$it.json" }
        modMessage("§7Available Waypoint Files:\n$formatted")
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
}