package cn.boop.necron.utils

enum class ItemRarity(val loreName: String, val colorCode: String) {
    COMMON("COMMON", "§f"),
    UNCOMMON("UNCOMMON", "§2"),
    RARE("RARE", "§9"),
    EPIC("EPIC", "§5"),
    LEGENDARY("LEGENDARY", "§6"),
    MYTHIC("MYTHIC", "§d"),
    DIVINE("DIVINE", "§b"),
    SPECIAL("SPECIAL", "§c"),
    VERY_SPECIAL("VERY SPECIAL", "§c"),
    ULTIMATE("ULTIMATE", "§4");
}

val rarityRegex = Regex("(${ItemRarity.entries.joinToString("|") { it.loreName }}) ?([A-Z ]+)?")