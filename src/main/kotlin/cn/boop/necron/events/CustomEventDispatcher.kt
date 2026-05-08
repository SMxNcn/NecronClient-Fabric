package cn.boop.necron.events

import cn.boop.necron.features.impl.necron.FarmingHelper.specialItemList
import cn.boop.necron.utils.getCurrentPestCount
import cn.boop.necron.utils.getCurrentPlot
import cn.boop.necron.utils.network.MayorData
import cn.boop.necron.utils.network.MayorData.pestSpawnCooldown
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.utils.containsOneOf
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket
import net.minecraft.world.entity.Relative
import java.util.*

object CustomEventDispatcher {
    private val visitRegex = Regex("\\[SkyBlock] (?:\\[.*?] )?(.*?) is visiting Your Garden!")
    private val pestSpawnRegex = Regex("^YUCK! \\d ൠ Pest have spawned in Plot - (\\d{1,2})!")
    private var activePestPlot = -1
    private var lastPestCount = -1

    init {
        on<TickEvent.End> {
            if (activePestPlot == -1) return@on

            val currentPlot = getCurrentPlot() ?: return@on
            if (currentPlot != activePestPlot) return@on

            val currentPestCount = getCurrentPestCount(activePestPlot)

            if (lastPestCount > 0 && currentPestCount == 0) {
                GardenEvent.PestKilled().postAndCatch()
                activePestPlot = -1
            }

            lastPestCount = currentPestCount
        }

        on<ChatPacketEvent> {
            visitRegex.find(value)?.let { visitMatcher ->
                val playerName = visitMatcher.groupValues[1].trim()
                GardenEvent.GuestVisit(playerName).postAndCatch()
            }

            pestSpawnRegex.find(value)?.let { pestMatcher ->
                activePestPlot = pestMatcher.groupValues[1].toInt()
                GardenEvent.PestSpawned(activePestPlot).postAndCatch()

                schedule((pestSpawnCooldown - 10) * 20, true) {
                    GardenEvent.PestReady().postAndCatch()
                }
            }

            if (value.contains("Everybody unlocks exclusive perks!")) MayorData.fetchData()
        }

        on<WorldEvent.Load> {
            GardenEvent.FailSafe("World Change").postAndCatch()
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
}