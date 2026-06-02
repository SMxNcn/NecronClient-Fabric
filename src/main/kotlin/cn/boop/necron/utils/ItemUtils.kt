@file:JvmName("ItemUtils")
package cn.boop.necron.utils

import com.google.gson.JsonParser
import com.odtheking.odin.utils.customData
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.loreString
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack

inline val ItemStack.itemUpgradeLevel: Int
    get() = customData.getInt("upgrade_level").orElse(0)!!

inline val ItemStack.petInfo: String
    get() = customData.getString("petInfo").orElse("")!!

fun getItemRarity(itemStack: ItemStack): ItemRarity? {
    if (itemStack.itemId == "PET") {
        val petInfo = itemStack.petInfo
        if (petInfo.isNotEmpty()) {
            try {
                val json = JsonParser.parseString(petInfo).asJsonObject
                val tier = json.get("tier")?.asString
                if (tier != null) return ItemRarity.entries.find { it.name == tier }
            } catch (_: Exception) {}
        }
    }

    for (i in itemStack.loreString.indices.reversed()) {
        val rarity = rarityRegex.find(itemStack.loreString[i])?.groups?.get(1)?.value ?: continue
        return ItemRarity.entries.find { it.loreName == rarity }
    }
    return null
}

fun getTooltipStyle(rarity: ItemRarity): Identifier = Identifier.withDefaultNamespace(rarity.name.lowercase())

fun isSkyBlockItem(stack: ItemStack): Boolean {
    if (stack.isEmpty) return false
    val customData = stack.customData
    return customData.copy().contains("id")
}