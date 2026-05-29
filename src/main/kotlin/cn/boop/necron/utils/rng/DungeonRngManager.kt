package cn.boop.necron.utils.rng

import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils

object DungeonRngManager : RngManager("dungeon") {

    init {
        loadPersistedData()
    }

    fun getCurrentFloorKey(): String? {
        if (!DungeonUtils.inDungeons) {
            return null
        }
        val floor = DungeonUtils.floor
        val key = floor?.name?.replace(Regex("[()]"), "")
        return key
    }

    fun getCurrentFloorMeter(): RngMeterUserData? {
        val key = getCurrentFloorKey() ?: return null
        checkDataExists(key)
        return meters[key]
    }

    fun getCurrentFloorMeterPercentage(): Double {
        val key = getCurrentFloorKey() ?: return 0.0
        return getMeterPercentage(key)
    }

    fun getCurrentFloorMeterBar(): String {
        val key = getCurrentFloorKey() ?: return ""
        val data = meters[key] ?: return ""
        return generateMeterBar(data.score, data.needed ?: 0)
    }
}
