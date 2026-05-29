package cn.boop.necron.commands

import cn.boop.necron.events.GardenEvent
import cn.boop.necron.utils.cleanString
import cn.boop.necron.utils.getScoreboardLines
import cn.boop.necron.utils.legacy
import cn.boop.necron.utils.modMessage
import cn.boop.necron.utils.network.MayorData
import com.github.stivais.commodore.Commodore
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.setClipboardContent

val devCommand = Commodore("ncd") {
    literal("copyTabList").runs { formatted: Boolean ->
        val connection = mc.connection ?: return@runs
        val result = connection.listedOnlinePlayers.sortedBy { it.tabListOrder }.mapNotNull { playerInfo ->
            if (formatted) playerInfo.tabListDisplayName?.legacy else playerInfo.tabListDisplayName?.cleanString
        }
        setClipboardContent(result.joinToString("\n"))
        modMessage("§aCopied ${if (formatted) "formatted " else ""}Tab List entries to clipboard.")
    }

    literal("copySidebar").runs { formatted: Boolean ->
        val data = getScoreboardLines(formatted) ?: return@runs
        val text = data.lines.joinToString("\n", "${data.title}\n", "\n")
        setClipboardContent(text)
        modMessage("§aCopied ${if (formatted) "formatted " else ""}Sidebar lines to clipboard.")
    }

    literal("pestKilled").runs {
        modMessage("§7Posting test PestKilled event.")
        GardenEvent.PestKilled().postAndCatch()
    }

    literal("pestReady").runs {
        modMessage("§7Posting test PestReady (Time: ${MayorData.pestSpawnCooldown}) event.")
        GardenEvent.PestReady().postAndCatch()
    }

    literal("pestSpawn").runs { plot: Int ->
        modMessage("§7Posting test PestSpawn event.")
        GardenEvent.PestSpawned(plot).postAndCatch()
    }
}