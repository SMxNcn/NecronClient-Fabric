package cn.boop.necron.utils.rng

enum class Slayer(val displayName: String) {
    Unknown("Unknown"),
    Revenant("Zombie Slayer"),
    Tarantula("Spider Slayer"),
    Sven("Wolf Slayer"),
    Voidgloom("Enderman Slayer"),
    Riftstalker("Vampire Slayer"),
    Inferno("Blaze Slayer")
}

enum class SlayerState {
    NOT_IN_SLAYER,
    SUMMONING_BOSS,
    IN_COMBAT,
    BOSS_SLAIN
}

data class RngMeterSaveData(
    val dungeonData: MutableMap<String, RngMeterUserData> = mutableMapOf(),
    val slayerData: MutableMap<String, RngMeterUserData> = mutableMapOf()
)

data class RngMeterUserData(
    var score: Int = 0,
    var item: String? = null,
    var needed: Int? = null
)

enum class MeterType {
    DUNGEON, SLAYER
}
