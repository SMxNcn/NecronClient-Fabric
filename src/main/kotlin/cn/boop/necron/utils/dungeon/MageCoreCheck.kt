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

        return magePos.x in 51..57 &&
                magePos.y in 114..120 &&
                magePos.z in 51..53
    }
}