package cn.boop.necron.utils.dungeon

import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils.dungeonTeammates
import net.minecraft.core.BlockPos

object MageCoreCheck {
    fun checkIfMageIsCore(): Boolean {
        val mage = dungeonTeammates.find { it.clazz == DungeonClass.Mage && !it.isDead } ?: return false

        val mageEntity = mage.entity ?: return false
        val magePos = BlockPos(
            mageEntity.x.toInt(),
            mageEntity.y.toInt(),
            mageEntity.z.toInt()
        )

        val minX = minOf(57, 51)
        val maxX = maxOf(57, 51)
        val minY = minOf(120, 114)
        val maxY = maxOf(120, 114)
        val minZ = minOf(53, 51)
        val maxZ = maxOf(53, 51)

        return magePos.x in minX..maxX &&
                magePos.y in minY..maxY &&
                magePos.z in minZ..maxZ
    }
}