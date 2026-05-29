package cn.boop.necron.utils.rng

import cn.boop.necron.events.SlayerEventHandler
import cn.boop.necron.utils.modMessage

object SlayerRngManager : RngManager("slayer") {
    val SLAYER_GUI_TO_KEY = mapOf(
        "Revenant Horror" to "Zombie Slayer",
        "Tarantula Broodfather" to "Spider Slayer",
        "Sven Packmaster" to "Wolf Slayer",
        "Voidgloom Seraph" to "Enderman Slayer",
        "Riftstalker Bloodfiend" to "Vampire Slayer",
        "Inferno Demonlord" to "Blaze Slayer"
    )

    init {
        loadPersistedData()
    }

    fun getCurrentSlayerKey(): String? {
        val slayer = SlayerEventHandler.currentSlayer
        if (slayer == Slayer.Unknown) return null
        return slayer.displayName
    }

    fun getCurrentSlayerMeter(): RngMeterUserData? {
        val key = getCurrentSlayerKey() ?: return null
        checkDataExists(key)
        return meters[key]
    }

    fun getCurrentSlayerMeterPercentage(): Double {
        val key = getCurrentSlayerKey() ?: return 0.0
        return getMeterPercentage(key)
    }

    fun getCurrentSlayerMeterBar(): String {
        val key = getCurrentSlayerKey() ?: return ""
        val data = meters[key] ?: return ""
        return generateMeterBar(data.score, data.needed ?: 0)
    }
}
