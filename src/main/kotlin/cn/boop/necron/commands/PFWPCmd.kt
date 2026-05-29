package cn.boop.necron.commands
/*
import cn.boop.necron.utils.modMessage
import cn.boop.necron.utils.path.IslandWaypoint
import cn.boop.necron.utils.path.NodeType
import cn.boop.necron.utils.path.PathManager
import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.utils.GreedyString
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

val pfwp = Commodore("pp") {
    literal("add").literal("walk").runs { name: GreedyString? ->
        addWaypoint(NodeType.WALK, name?.string?.trim())
    }

    literal("add").literal("tp").runs { name: GreedyString? ->
        addWaypoint(NodeType.TP, name?.string?.trim())
    }

    literal("add").literal("warp").runs { name: GreedyString? ->
        addWaypoint(NodeType.WARP, name?.string?.trim())
    }

    literal("remove").runs {
        val island = LocationUtils.currentArea
        val waypoint = getHoveredWaypoint() ?: return@runs
        val wpPos = getTargetedBlockPos()
        wpPos?.let {
            if (PathManager.removeWaypointAt(island, it)) {
                modMessage("§cRemoved waypoint at §e${waypoint.x}, ${waypoint.y}, ${waypoint.z}")
            }
        }
    }

    literal("reload").runs {
        PathManager.loadPaths()
        modMessage("§aWaypoints reloaded from config.")
    }

    // === LIST COMMAND ===
    literal("list").runs {
        val island = LocationUtils.currentArea
        val waypoints = PathManager.getWaypoints(island)

        if (waypoints.isEmpty()) {
            return@runs modMessage("§7No waypoints found for §f${island.displayName}")
        }

        val chunkedList = waypoints.chunked(10)
        val output = chunkedList.joinToString("\n") { chunk ->
            chunk.joinToString(", ") { wp ->
                "#${waypoints.indexOf(wp) + 1} ${wp.type.displayName} (${wp.x}, ${wp.y}, ${wp.z})"
            }
        }
        modMessage("§6=== Waypoints for ${island.displayName} ===\n$output")
    }


}

private fun addWaypoint(type: NodeType, name: String?) {
    val player = mc.player ?: return modMessage("§cPlayer not found!")
    val pos: BlockPos = player.blockPosition()
    val island = LocationUtils.currentArea

    // 检查是否已存在
    if (PathManager.getWaypointAt(island, pos) != null) {
        return modMessage("§cWaypoint already exists at this position!")
    }

    val waypoint = IslandWaypoint.fromBlockPos(pos, type, name ?: "")
    PathManager.addWaypoint(island, waypoint)

    val nameStr = if (!name.isNullOrBlank()) " named '§f$name§r'" else ""
    modMessage("§aAdded §f${type.displayName} §awaypoint$nameStr at §e${pos.x}, ${pos.y}, ${pos.z}")
}

private fun getHoveredWaypoint(): IslandWaypoint? {
    val island = LocationUtils.currentArea
    val waypoints = PathManager.getWaypoints(island)

    if (waypoints.isEmpty()) return null

    val targetedPos = getTargetedBlockPos() ?: return null
    return PathManager.getWaypointAt(island, targetedPos)
}

private fun getTargetedBlockPos(): BlockPos? {
    val hitResult = mc.hitResult ?: return null

    if (hitResult.type == HitResult.Type.BLOCK) {
        val blockHit = hitResult as BlockHitResult
        return blockHit.blockPos
    }

    return null
}*/
