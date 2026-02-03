package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.GuiEvent
import com.odtheking.odin.events.PacketEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.equalsOneOf
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils.inDungeons
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket

object AutoCloseChest : Module(
    name ="Close Chest",
    description = "Allows you to instantly close chests with any key or automatically.",
    category = NCategory.NECRON
) {
    private val mode by SelectorSetting("Mode", "Auto", arrayListOf("Auto", "Any Key"), desc = "The mode to use, auto will automatically close the chest, any key will make any key input close the chest.")

    init {
        on<PacketEvent.Receive> {
            if (!inDungeons) return@on
            val packet = packet as? ClientboundOpenScreenPacket ?: return@on

            val title = packet.title.string
            val isSecretChest = title.equalsOneOf("Chest", "Large Chest")

            if (mode == 0 && isSecretChest) {
                mc.connection?.send(ServerboundContainerClosePacket(packet.containerId))
                cancel()
            }
        }

        on <GuiEvent.MouseClick> {
            if (mode != 1 || !inDungeons) return@on

            var title = screen.title.string
            val isSecretChest = title.equalsOneOf("Chest", "Large Chest")

            if (isSecretChest) mc.player?.closeContainer()
        }

        on <GuiEvent.KeyPress> {
            if (mode != 1 || !inDungeons) return@on

            var title = screen.title.string
            val isSecretChest = title.equalsOneOf("Chest", "Large Chest")

            if (isSecretChest) mc.player?.closeContainer()
        }
    }
}