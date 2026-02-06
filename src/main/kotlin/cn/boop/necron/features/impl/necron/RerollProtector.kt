package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import cn.boop.necron.utils.modMessage
import cn.boop.necron.utils.removeFormatting
import cn.boop.necron.utils.toLegacyString
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.events.GuiEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.equalsOneOf
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.sendCommand
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.Container
import net.minecraft.world.inventory.ChestMenu

object RerollProtector : Module(
    name = "Reroll Protector",
    description = "Prevent reroll when rare rewards appear in reward chests.",
    category = NCategory.NECRON
) {
    private val sendRngMessage by BooleanSetting("Send RNG Message", true, desc = "Send rare item name to party.")
    private val message by StringSetting("Rng Message", "%i in %c!", desc = "Use %i for rng item name, %c for chest type.")

    private const val REROLL_BUTTON_ID = 50
    private var hasRareItems = false
    private var hasShownMessage = false
    private var rareItemSlot = -1
    private var lastRareItemName: String? = null
    private var lastCheckedChest: String? = null
    private var lastRawItemName: String? = null

    private val RARE_ITEMS = setOf(
        "Shiny Necron's Handle",
        "Necron's Handle",
        "Implosion",
        "Wither Shield",
        "Shadow Warp",
        "Dark Claymore",
        "Giant's Sword",
        "Shadow Fury",
        "Necron Dye",
        "Livid Dye",
        "Master Skull - Tier 5",
        "Fifth Master Star",
        "Fourth Master Star",
        "Third Master Star",
        "Second Master Star",
        "First Master Star",
        "Tentacle Dye",
        "Hellstorm Wand",
        "Tormentor"/*,
        "Enchanted Book (Fatal Tempo I)" // Need confirm*/
    )

    init {
        on<GuiEvent.Open> {
            if (!enabled || !DungeonUtils.inDungeons) return@on
            val chest = (screen as? AbstractContainerScreen<*>) ?: return@on
            if (lastCheckedChest != chest.title.string) {
                hasShownMessage = false
                lastCheckedChest = chest.title.string
            }

            if (!isRewardChest(chest)) return@on
            val menu = chest.menu as? ChestMenu ?: return@on
            val container: Container = menu.container
            schedule(4) {
                hasRareItems = hasRareLoot(container)
                if (hasRareItems && !hasShownMessage && lastRareItemName != null) {
                    hasShownMessage = true
                    sendMessage(lastRawItemName!!, lastCheckedChest!!)
                }
            }
        }

        on<GuiEvent.SlotClick> {
            if (!enabled || !hasRareItems || slotId != REROLL_BUTTON_ID && !DungeonUtils.inDungeons) return@on
            if (button == 0 || button == 1) {
                cancel()
                modMessage("§cReroll button has been §lDISABLED§r§c!")
            }
        }

        on<GuiEvent.Close> {
            resetState()
            lastCheckedChest = null
        }

        on<WorldEvent.Unload> {
            resetState()
        }
    }

    private fun isRewardChest(chest: AbstractContainerScreen<*>): Boolean {
        return chest.title.string.equalsOneOf("Wood Chest", "Gold Chest", "Diamond Chest", "Emerald Chest", "Obsidian Chest", "Bedrock Chest", "Free Chest", "Paid Chest")
    }

    private fun hasRareLoot(container: Container): Boolean {
        val containerSize = container.containerSize

        for (i in 9..26) {
            if (i >= containerSize) break
            val stack = container.getItem(i)

            if (!stack.isEmpty) {
                val rawDisplayName = stack.displayName?.string ?: continue
                var cleanName = rawDisplayName.removeFormatting()
                cleanName = cleanName.replace("[", "").replace("]", "").trim()

                if (isRareItem(cleanName)) {
                    rareItemSlot = i
                    lastRareItemName = cleanName
                    lastRawItemName = stack.displayName.toLegacyString()
                    return true
                }
            }
        }

        return false
    }

    private fun sendMessage(itemName: String, chestName: String) {
        val rawItemName = itemName.replace("[", "").replace("]", "").trim()
        val chatMessage = message.replace("%i", rawItemName).replace("%c", chestName)

        val formattedChestName = getChestColor(chestName)

        if (sendRngMessage) sendCommand("pc NC » ${chatMessage.removeFormatting()}")
        modMessage("§dRng Item §7in $formattedChestName§7! ($rawItemName§7)")
    }

    private fun isRareItem(itemName: String): Boolean {
        return RARE_ITEMS.contains(itemName)
    }

    private fun resetState() {
        hasRareItems = false
        rareItemSlot = -1
        hasShownMessage = false
        lastRareItemName = null
    }

    private fun getChestColor(chestName: String): String {
        return when (chestName.removeFormatting()) {
            "Bedrock Chest" -> "§8$chestName"
            "Obsidian Chest" -> "§5$chestName"
            "Emerald Chest" -> "§2$chestName"
            "Diamond Chest" -> "§b$chestName"
            "Gold Chest" -> "§6$chestName"
            "Wood Chest" -> "§f$chestName"
            else -> "§f$chestName"
        }
    }
}