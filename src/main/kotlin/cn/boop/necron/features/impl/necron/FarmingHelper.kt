package cn.boop.necron.features.impl.necron

import cn.boop.necron.events.GardenEvent
import cn.boop.necron.utils.EquipmentUtils.swapEquipment
import cn.boop.necron.utils.NCategory
import cn.boop.necron.utils.WardrobeUtils.swapArmorTo
import cn.boop.necron.utils.modMessage
import cn.boop.necron.utils.screen.ActionInputScreen
import cn.boop.necron.utils.waypoints.FarmingWaypoints
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.*
import com.odtheking.odin.events.InputEvent
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.render.drawText
import com.odtheking.odin.utils.sendCommand
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import org.lwjgl.glfw.GLFW

object FarmingHelper : Module(
    name = "Farming Helper",
    description = "Features for garden farming.",
    category = NCategory.NECRON
) {
    private val allowEdits by BooleanSetting("Allow Edits", false, desc = "Right-click blocks to add/remove waypoints.")
    private val allowKeyEdit by BooleanSetting("Allow Key Edit", false, desc = "Shift and Right-click blocks to edit waypoint keys.").withDependency { allowEdits }
    private val renderWps by BooleanSetting("Render Waypoints", true, desc = "Render waypoints.")
    private val renderOnFarming by BooleanSetting("Render on Farming", false, desc = "Stop render if CropNuker is enabled.")

    private val armorDropdown by DropdownSetting("Armor & Equipment")
    private val mossyArmorSlot by NumberSetting("Mossy Slot", 1, 1, 9, desc = "Mossy armor wardrobe slot.").withDependency { armorDropdown }
    private val mantidArmorSlot by NumberSetting("Mantid Slot", 2, 1, 9, desc = "Mantid armor wardrobe slot.").withDependency { armorDropdown }
    private val ffEquipmentType by SelectorSetting("Equipment Type", "Blossom", listOf("Lotus", "Blossom"), desc = "Equipment Type").withDependency { armorDropdown }

    private val autoKick by BooleanSetting("Auto Kick", false, desc = "Auto Kick player who visiting your garden.")

    private val nukerKeybind by KeybindSetting("Nuker Keybind", GLFW.GLFW_KEY_X, desc = "Keybind to toggle nuker.").onPress {
        CropNuker.toggleNuker()
    }

    private val lotusIds = listOf("LOTUS_NECKLACE", "LOTUS_CLOAK", "LOTUS_BELT", "LOTUS_BRACELET")
    private val blossomIds = listOf("BLOSSOM_NECKLACE", "BLOSSOM_CLOAK", "BLOSSOM_BELT", "BLOSSOM_BRACELET")
    private val pestIds = listOf("PESTHUNTERS_NECKLACE", "PEST_VEST", "PESTHUNTERS_BELT", "PESTHUNTERS_GLOVES")
    val specialItemList = listOf("SQUEAKY_MOUSEMAT", "ASPECT_OF_THE_END", "ASPECT_OF_THE_VOID")

    init {
        on<TickEvent.End> {
            if (LocationUtils.currentArea != Island.Garden || mc.screen != null) return@on
            CropNuker.onTick()
        }

        on<GardenEvent.FailSafe> {
            if (!CropNuker.enabled) return@on
            CropNuker.stop()
            when (reason) {
                "World Change" -> modMessage("§6Crop Nuker §7disabled due to world changed.")
                "Teleport" -> modMessage("§6Crop Nuker §7disabled due to position changed.")
                "Rotation" -> modMessage("§6Crop Nuker §7disabled due to rotation changed.")
                "Held Item Change" -> modMessage("§6Crop Nuker §7disabled due to held item changed.")
            }
        }

        on<InputEvent> {
            if (!allowEdits || key.value != GLFW.GLFW_MOUSE_BUTTON_RIGHT || mc.screen != null) return@on
            if (LocationUtils.currentArea == Island.Garden) {
                val pos = reachPosition ?: return@on
                if (allowKeyEdit && mc.player?.isCrouching == true && mc.screen == null) {
                    val currentAction = FarmingWaypoints.currentWaypoints.find { it.blockPos == pos }?.action ?: FarmingWaypoints.Action()
                    mc.setScreen(ActionInputScreen(currentAction) { newAction ->
                        FarmingWaypoints.updateAt(pos, newAction)
                    })
                } else if (!FarmingWaypoints.removeAt(pos)) {
                    FarmingWaypoints.addAt(pos)
                }
            }
        }

        on<RenderEvent.Extract> {
            if (!renderWps || LocationUtils.currentArea != Island.Garden || (!renderOnFarming && CropNuker.enabled)) return@on
            FarmingWaypoints.currentWaypoints.forEach { wp ->
                drawStyledBox(AABB(wp.blockPos), Colors.MINECRAFT_GRAY, 1, false)
                drawText("#${wp.id}", wp.blockPos.center.add(0.0, 1.1, 0.0), 1.2f, false)
            }
        }

        on<GardenEvent.PestReady> {
            if (!CropNuker.enabled) return@on
            CropNuker.stop()
            OdinMod.scope.launch {
                if (swapArmorTo(mantidArmorSlot)) {
                    delay(200 + (0..100).random().toLong())
                    if (swapEquipment(pestIds)) {
                        delay(100)
                        CropNuker.start()
                    }
                }
            }
        }

        on<GardenEvent.PestSpawned> {
            if (!CropNuker.enabled) return@on
            CropNuker.stop()

            val eqList = when (ffEquipmentType) {
                0 -> lotusIds.toList()
                else -> blossomIds.toList()
            }

            OdinMod.scope.launch {
                sendCommand("setspawn")
                delay(250 + (0..50).random().toLong())

                if (swapArmorTo(mossyArmorSlot)) {
                    delay(200 + (0..100).random().toLong())
                    if (swapEquipment(eqList)) {
                        delay(50)
                        sendCommand("tptoplot $plot")
                    }
                }
            }
        }

        on<GardenEvent.PestKilled> {
            if (CropNuker.enabled) return@on
            schedule(4) {
                sendCommand("warp garden")
                modMessage("Pest killed.")
                schedule(2) { CropNuker.start() }
            }
        }

        on<GardenEvent.GuestVisit> {
            if (autoKick) schedule(2) { sendCommand("sbkick $player") }
        }
    }

    private inline val reachPosition: BlockPos?
        get() {
            val hitResult = mc.hitResult
            if (hitResult !is BlockHitResult) return null

            val blockPos = hitResult.blockPos
            val blockState = mc.level?.getBlockState(blockPos) ?: return null
            return if (blockState.isSolid) blockPos else null
        }
}