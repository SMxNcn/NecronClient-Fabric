package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import cn.boop.necron.utils.clean
import cn.boop.necron.utils.legacy
import cn.boop.necron.utils.modMessage
import cn.boop.necron.utils.network.WebSocketMessageHandler.handleChestReward
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.events.GuiEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.equalsOneOf
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.sendCommand
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.KuudraUtils
import com.odtheking.odin.utils.skyblock.LocationUtils
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
            if (!(DungeonUtils.inDungeons || KuudraUtils.inKuudra) || LocationUtils.currentArea == Island.DungeonHub) return@on
            val chest = (screen as? AbstractContainerScreen<*>) ?: return@on
            if (lastCheckedChest != chest.title.string) {
                hasShownMessage = false
                lastCheckedChest = getChestColor(chest.title.string)
            }

            if (!isRewardChest(chest)) return@on
            val menu = chest.menu as? ChestMenu ?: return@on
            val container: Container = menu.container
            schedule(4) {
                hasRareItems = hasRareLoot(container)
                if (hasRareItems && !hasShownMessage && lastRareItemName != null) {
                    hasShownMessage = true
                    sendMessage(lastRawItemName!!, lastCheckedChest!!)
                    handleChestReward(lastRawItemName!!, lastCheckedChest!!)
                }
            }
        }

        on<GuiEvent.SlotClick> {
            if (!hasRareItems || slotId != REROLL_BUTTON_ID || !(DungeonUtils.inDungeons || KuudraUtils.inKuudra)) return@on
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

    private fun isRewardChest(chest: AbstractContainerScreen<*>) =
        chest.title.string.equalsOneOf("Wood", "Gold", "Diamond", "Emerald", "Obsidian", "Bedrock", "Free", "Paid")

    private fun hasRareLoot(container: Container): Boolean {
        val containerSize = container.containerSize

        for (i in 9..26) {
            if (i >= containerSize) break
            val stack = container.getItem(i)

            if (!stack.isEmpty) {
                val rawDisplayName = stack.displayName?.string ?: continue
                var cleanName = rawDisplayName.clean
                cleanName = cleanName.replace("[", "").replace("]", "").trim()

                if (RARE_ITEMS.contains(cleanName)) {
                    rareItemSlot = i
                    lastRareItemName = cleanName
                    lastRawItemName = stack.displayName.legacy.replace("[", "").replace("]", "")
                    return true
                }
            }
        }

        return false
    }

    private fun sendMessage(itemName: String, chestName: String) {
        val chatMessage = message.replace("%i", itemName).replace("%c", chestName)
        if (sendRngMessage) sendCommand("pc NC » ${chatMessage.clean}")
        modMessage("§dRng Item §7in $chestName§7! ($itemName§7)")
    }

    private fun resetState() {
        hasRareItems = false
        rareItemSlot = -1
        hasShownMessage = false
        lastRareItemName = null
    }

    private fun getChestColor(chestName: String): String {
        return when (chestName.clean) {
            "Bedrock" -> "§8$chestName Chest"
            "Obsidian" -> "§5$chestName Chest"
            "Emerald" -> "§2$chestName Chest"
            "Diamond" -> "§b$chestName Chest"
            "Gold" -> "§6$chestName Chest"
            "Wood" -> "§f$chestName Chest"
            else -> "§f$chestName Chest"
        }
    }
}