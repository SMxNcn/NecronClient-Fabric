package cn.boop.necron.utils.dungeon

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.clickSlot
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos

enum class P3Stages(
    val corner1: BlockPos,
    val corner2: BlockPos
) {
    Unknown(BlockPos(-7, 160, -7), BlockPos(7, 160, 7)),
    Tunnel(BlockPos(39, 160, 54), BlockPos(69, 112, 118)),
    S1(BlockPos(89, 153, 51), BlockPos(111, 105, 121)),
    S2(BlockPos(89, 153, 121), BlockPos(19, 105, 143)),
    S3(BlockPos(19, 153, 121), BlockPos(-3, 105, 51)),
    S4(BlockPos(19, 153, 51), BlockPos(89, 105, 29));

    companion object {
        fun getP3Stage(): P3Stages {
            if (DungeonUtils.getF7Phase() != M7Phases.P3 || mc.player == null)
                return Unknown

            val player = mc.player!!
            val playerPos = BlockPos(
                player.x.toInt(),
                player.y.toInt(),
                player.z.toInt()
            )

            for (stage in entries) {
                if (stage == Unknown) continue

                if (isPlayerInArea(stage.corner1, stage.corner2, playerPos)) {
                    return stage
                }
            }

            return Unknown
        }

        private fun isPlayerInArea(corner1: BlockPos, corner2: BlockPos, playerPos: BlockPos): Boolean {
            val minX = minOf(corner1.x, corner2.x)
            val maxX = maxOf(corner1.x, corner2.x)
            val minY = minOf(corner1.y, corner2.y)
            val maxY = maxOf(corner1.y, corner2.y)
            val minZ = minOf(corner1.z, corner2.z)
            val maxZ = maxOf(corner1.z, corner2.z)

            return playerPos.x in minX..maxX &&
                    playerPos.y in minY..maxY &&
                    playerPos.z in minZ..maxZ
        }
    }
}

fun getP3Stage(): P3Stages = P3Stages.getP3Stage()

fun leapTo(name: String, screenHandler: AbstractContainerScreen<*>) {
    val index = screenHandler.menu.slots.subList(11, 16).firstOrNull {
        it.item?.hoverName?.string?.substringAfter(' ').equals(name.noControlCodes, ignoreCase = true)
    }?.index ?: return
    mc.player?.clickSlot(screenHandler.menu.containerId, index)
}