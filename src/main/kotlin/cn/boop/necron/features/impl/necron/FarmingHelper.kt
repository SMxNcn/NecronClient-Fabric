package cn.boop.necron.features.impl.necron

import cn.boop.necron.events.GardenEvent
import cn.boop.necron.utils.*
import cn.boop.necron.utils.EquipmentUtils.swapEquipment
import cn.boop.necron.utils.WardrobeUtils.swapArmorTo
import cn.boop.necron.utils.screen.ActionInputScreen
import cn.boop.necron.utils.waypoints.FarmingWaypoints
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.*
import com.odtheking.odin.events.GuiEvent
import com.odtheking.odin.events.InputEvent
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.containsOneOf
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.render.drawText
import com.odtheking.odin.utils.sendCommand
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
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
    private val renderWps by BooleanSetting("Render Waypoints", true, desc = "Render waypoints.")
    private val renderOnFarming by BooleanSetting("Render on Farming", false, desc = "Render waypoints when CropNuker is disabled.")

    private val armorDropdown by DropdownSetting("Armor")
    private val mossyArmorSlot by NumberSetting("Mossy Slot", 1, 1, 9, desc = "Mossy armor wardrobe slot.").withDependency { armorDropdown }
    private val mantidArmorSlot by NumberSetting("Mantid Slot", 2, 1, 9, desc = "Mantid armor wardrobe slot.").withDependency { armorDropdown }

    private val otherDropdown by DropdownSetting("Others")
    private val autoKick by BooleanSetting("Auto Kick", true, desc = "Auto Kick player who visiting your garden.").withDependency { otherDropdown }
    private val ignorePests by BooleanSetting("Ignore Pests", false, desc = "CropNuker will not respond to pest ready/spawned/killed events.").withDependency { otherDropdown }
    private val equipZorro by BooleanSetting("Zorro's Cape", false, desc = "Equip Zorro's Cape in farming contests.")
    private val killAtDisco by BooleanSetting("Pest Disco", false, desc = "Kill pest around Disco.").withDependency { otherDropdown }
    private val maxVacuumTime by NumberSetting("Max Vacuum Time", 1000, 500, 2000, 100, desc = "How long to hold right-click to use the vacuum.", "ms").withDependency { otherDropdown && killAtDisco }
    private val changeTimeOnPest by BooleanSetting("Change Time (Fireflies)", false, desc = "Set garden time to day for Sunset's Overbloom bonus.").withDependency { otherDropdown }

    private val nukerKeybind by KeybindSetting("Nuker Keybind", GLFW.GLFW_KEY_X, desc = "Keybind to toggle nuker.").onPress {
        CropNuker.toggleNuker()
    }

    private val isJacobActive: Boolean
        get() = getScoreboard().any { it.contains("Jacob's Contest") }

    private val blossomIds: List<String>
        get() =
            if (isJacobActive && equipZorro) listOf("BLOSSOM_NECKLACE", "ZORROS_CAPE", "BLOSSOM_BELT", "BLOSSOM_BRACELET")
            else listOf("BLOSSOM_NECKLACE", "BLOSSOM_CLOAK", "BLOSSOM_BELT", "BLOSSOM_BRACELET")

    private val pestIds = listOf("PESTHUNTERS_NECKLACE", "PEST_VEST", "PESTHUNTERS_BELT", "PESTHUNTERS_GLOVES")
    val specialItemList = listOf("SQUEAKY_MOUSEMAT", "ASPECT_OF_THE_VOID", "INFINI_VACUUM_HOOVERIUS")

    private var lastHeldSlot: Int = -1
    private var containerId = -1

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
                if (mc.player?.isCrouching == true && mc.screen == null) {
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
            if (ignorePests) return@on
            val player = mc.player ?: return@on
            lastHeldSlot = player.inventory.selectedSlot

            if (!CropNuker.enabled) return@on

            OdinMod.scope.launch {
                CropNuker.stop()
                if (swapArmorTo(mantidArmorSlot)) {
                    delay(randomDelay(400, 100))
                    if (swapEquipment(pestIds)) {
                        delay(randomDelay(100, 50))
                        CropNuker.start()
                    } else {
                        modMessage("§cMissing equipments!")
                    }
                } else {
                    modMessage("§cFailed to equip armor from Wardrobe #$mantidArmorSlot!")
                }
            }
        }

        on<GardenEvent.PestSpawned> {
            if (!CropNuker.enabled || ignorePests) return@on
            val player = mc.player ?: return@on

            OdinMod.scope.launch {
                delay(randomDelay(200, 100))
                CropNuker.stop()
                sendCommand("setspawn")
                delay(randomDelay(250, 50))
                if (changeTimeOnPest) changeGardenTime(false) // Change time to Day

                if (swapArmorTo(mossyArmorSlot)) {
                    delay(randomDelay(400, 100))
                    if (swapEquipment(blossomIds)) {
                        delay(randomDelay(100, 50))
                        sendCommand("tptoplot $plot")
                        if (killAtDisco) {
                            delay(randomDelay(1200, 400))
                            val slot = findItemByID(specialItemList[3], true)
                            if (slot != -1 && slot in 0..9) {
                                player.inventory.selectedSlot = slot
                                delay(randomDelay(100, 100))
                                mc.options.keyUse.isDown = true
                                delay(randomDelay(maxVacuumTime, 100))
                                mc.options.keyUse.isDown = false
                                delay(randomDelay(500, 50))
                                GardenEvent.PestKilled().postAndCatch()
                            }
                        }
                    } else {
                        modMessage("§cMissing equipments!")
                    }
                } else {
                    modMessage("§cFailed to equip armor from Wardrobe #$mossyArmorSlot!")
                }
            }
        }

        on<GardenEvent.PestKilled> {
            if (CropNuker.enabled || ignorePests) return@on
            val player = mc.player ?: return@on

            OdinMod.scope.launch {
                delay(100)
                mc.options.keyShift.isDown = true
                sendCommand("warp garden")
                delay(randomDelay(200, 100))
                mc.options.keyShift.isDown = false
                if (changeTimeOnPest) changeGardenTime(true) // Change time to Night

                delay(randomDelay(550, 100))
                player.inventory.selectedSlot = if (lastHeldSlot == -1) 0 else lastHeldSlot
                delay(randomDelay(450, 50))
                CropNuker.start()
            }
        }

        on<GardenEvent.GuestVisit> {
            if (autoKick) schedule(2) { sendCommand("sbkick $player") }
        }

        on<GuiEvent.Open> {
            if (CropNuker.enabled || ignorePests || !changeTimeOnPest) return@on
            val chest = (screen as? AbstractContainerScreen<*>) ?: return@on
            if (!chest.title.cleanString.containsOneOf("Desk", "Garden Time", "Pesthunter")) return@on
            containerId = mc.player?.containerMenu?.containerId ?: return@on
        }
    }

    private inline val reachPosition: BlockPos?
        get() {
            val hitResult = mc.hitResult
            if (hitResult !is BlockHitResult) return null

            val blockPos = hitResult.blockPos
            val blockState = mc.level?.getBlockState(blockPos) ?: return null
            return if (blockState.isSolidRender) blockPos else null
        }

    suspend fun changeGardenTime(toNight: Boolean) {
        delay(randomDelay(200, 100))
        sendCommand("desk")
        delay(randomDelay(600, 100))
        clickInventorySlot(50, containerId)
        delay(randomDelay(600, 100))
        val slot = if (toNight) 13 else 11
        clickInventorySlot(slot, containerId)
        delay(randomDelay(600, 100))
        clickInventorySlot(31, containerId)
    }
}