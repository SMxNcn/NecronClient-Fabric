package cn.boop.necron.events

import cn.boop.necron.features.impl.necron.FarmingHelper.specialItemList
import cn.boop.necron.utils.*
import cn.boop.necron.utils.network.MayorData
import cn.boop.necron.utils.network.MayorData.pestSpawnCooldown
import cn.boop.necron.utils.network.WSClient
import cn.boop.necron.utils.rng.DungeonRngEventHandler
import cn.boop.necron.utils.rng.MeterType
import cn.boop.necron.utils.rng.SlayerRngEventHandler
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.GuiEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.utils.containsOneOf
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket
import net.minecraft.world.entity.Relative
import java.util.*

object CustomEventDispatcher {
    private val visitRegex = Regex("\\[SkyBlock] (?:\\[.*?] )?(.*?) is visiting Your Garden!")
    private val pestSpawnRegex = Regex("(?:A ൠ Pest has appeared|\\d+ ൠ Pest have spawned) in Plot - (\\d{1,2})!")
    private var activePestPlot = -1
    private var lastPestCount = -1

    init {
        on<TickEvent.End> {
            if (LocationUtils.currentArea != Island.Garden || activePestPlot == -1) return@on

            val currentPlot = getCurrentPlot() ?: return@on
            if (currentPlot != activePestPlot) return@on

            val currentPestCount = getCurrentPestCount(activePestPlot)

            if (lastPestCount > 0 && currentPestCount == 0) {
                GardenEvent.PestKilled().postAndCatch()
                activePestPlot = -1
            }

            lastPestCount = currentPestCount
        }

        on<TickEvent.End> {
            SlayerEventHandler.tick()
        }

        on<ChatPacketEvent> {
            if (LocationUtils.currentArea != Island.Garden) return@on

            visitRegex.find(value.clean)?.let { visitMatcher ->
                val playerName = visitMatcher.groupValues[1].trim()
                GardenEvent.GuestVisit(playerName).postAndCatch()
            }

            pestSpawnRegex.find(value.clean)?.let { pestMatcher ->
                activePestPlot = pestMatcher.groupValues[1].toInt()
                GardenEvent.PestSpawned(activePestPlot).postAndCatch()

                schedule((pestSpawnCooldown - 10) * 20, true) {
                    GardenEvent.PestReady().postAndCatch()
                }
            }

            if (value.clean.contains("Everybody unlocks exclusive perks!")) MayorData.fetchData()
        }

        on<ChatPacketEvent> {
            val msg = component.legacy
            if (LocationUtils.isInSkyblock) {
                DungeonRngEventHandler.handleChat(msg)
                SlayerRngEventHandler.handleChat(msg)
            }
        }

        on<GuiEvent.Open> {
            val chest = (screen as? AbstractContainerScreen<*>) ?: return@on
            schedule(3) {
                if (mc.screen != chest) return@schedule
                 if (LocationUtils.isInSkyblock) {
                    DungeonRngEventHandler.handleGui(chest)
                    SlayerRngEventHandler.handleGui(chest)
                }
            }
        }

        on<RngEvent.ItemSelected> {
            val displayName = when (type) {
                MeterType.DUNGEON -> "Catacombs §8(${key}§8)"
                MeterType.SLAYER -> key
            }
            modMessage("§aSet RNG Meter §ato drop §6${item}§a for §d$displayName§a!")
        }

        on<RngEvent.MeterReset> {
            val percentage = if (needed != null && needed > 0) score.toDouble() / needed * 100 else 0.0
            modMessage("§dRng Item §7reset! (§6${"%,d".format(score)} §bScore, §6${"%.2f".format(percentage)}§b%§7)")
        }

        on<WorldEvent.Unload> {
            GardenEvent.FailSafe("World Change").postAndCatch()
            DungeonRngEventHandler.resetDedup()
        }

        onReceive<ClientboundPlayerPositionPacket> {
            if (LocationUtils.currentArea != Island.Garden) return@onReceive
            if (mc.player?.mainHandItem?.itemId?.containsOneOf(specialItemList) == true) return@onReceive
            val isTeleport = relatives.any { it in EnumSet.of(Relative.X, Relative.Y, Relative.Z) }
            val isRotation = relatives.any { it == Relative.Y_ROT || it == Relative.X_ROT }
            if (isTeleport) GardenEvent.FailSafe("Teleport").postAndCatch()
            else if (isRotation) GardenEvent.FailSafe("Rotation").postAndCatch()
        }

        onReceive<ClientboundSetHeldSlotPacket> {
            if (LocationUtils.currentArea != Island.Garden) return@onReceive
            GardenEvent.FailSafe("Held Item Change").postAndCatch()
        }
    }

    fun postFabricEvents() {
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            if (WSClient.isConnected) {
                WSClient.disconnect()
            }
        }
    }
}