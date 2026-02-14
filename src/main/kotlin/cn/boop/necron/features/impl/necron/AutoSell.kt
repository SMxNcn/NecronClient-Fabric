package cn.boop.necron.features.impl.necron

import cn.boop.necron.Necron
import cn.boop.necron.utils.NCategory
import cn.boop.necron.utils.removeFormatting
import com.odtheking.odin.clickgui.settings.impl.ActionSetting
import com.odtheking.odin.clickgui.settings.impl.ListSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.containsOneOf
import com.odtheking.odin.utils.equalsOneOf
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ClickType

object AutoSell : Module(
    name = "Auto Sell",
    description = "Automatically sell items in trades and cookie menus. (/autosell)",
    category = NCategory.NECRON
) {
    val sellList by ListSetting("Sell list", mutableSetOf<String>())
    private val delay by NumberSetting("Delay", 100L, 75L, 300L, 5L, desc = "The delay between each sell action.", unit = "ms")
    private val clickType by SelectorSetting("Click Type", "Shift", arrayListOf("Shift", "Middle", "Left"), desc = "The type of click to use when selling items.")
    private val addDefaults by ActionSetting("Add defaults", desc = "Add default dungeon items to the auto sell list.") {
        sellList.addAll(defaultItems)
        modMessage("§aAdded default items to auto sell list")
        Necron.config.save()
    }
    private var lastClickTime = 0L

    init {
        on<TickEvent.Start> {
            schedule((delay + (0..50).random()).toInt()) {
                if (!enabled || sellList.isEmpty()) return@schedule
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime < 50) return@schedule
                val container = mc.screen as? AbstractContainerScreen<*> ?: return@schedule
                val player = mc.player ?: return@schedule
                val title = container.title?.string ?: return@schedule

                if (!title.removeFormatting().equalsOneOf("Trades", "Booster Cookie", "Farm Merchant", "Ophelia")) return@schedule

                val index = container.menu.slots.filter { it.container is Inventory }.firstOrNull {
                    val stack = it.item ?: return@firstOrNull false
                    if (stack.isEmpty) return@firstOrNull false
                    stack.hoverName?.string?.noControlCodes?.containsOneOf(sellList, ignoreCase = true) == true
                }?.index ?: return@schedule

                val ct = when (clickType) {
                    1 -> ClickType.QUICK_MOVE
                    2 -> ClickType.CLONE
                    3 -> ClickType.PICKUP
                    else -> ClickType.QUICK_MOVE
                }
                mc.gameMode?.handleInventoryMouseClick(container.menu.containerId, index, 0, ct, player)
                lastClickTime = currentTime
            }
        }
    }

    private val defaultItems = arrayOf(
        "enchanted ice", "superboom tnt", "rotten", "skeleton grunt", "cutlass",
        "skeleton lord", "skeleton soldier", "zombie soldier", "zombie knight", "zombie commander", "zombie lord",
        "skeletor", "super heavy", "heavy", "sniper helmet", "dreadlord", "earth shard", "zombie commander whip",
        "machine gun", "sniper bow", "soulstealer bow", "silent death", "training weight",
        "beating heart", "premium flesh", "mimic fragment", "enchanted rotten flesh", "sign",
        "enchanted bone", "defuse kit", "optical lens", "tripwire hook", "button", "carpet", "lever", "diamond atom",
        "healing viii splash potion", "healing 8 splash potion", "candycomb"
    )
}