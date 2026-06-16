package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import cn.boop.necron.utils.RotationUtils.exponentialSmooth
import cn.boop.necron.utils.RotationUtils.vec3ToRotation
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onSend
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.equalsOneOf
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3

object RelicHelper : Module(
    name = "Relic Helper",
    description = "Useful features for Wither King relics.",
    category = NCategory.NECRON
) {
    private val relicLook by BooleanSetting("Relic Look", false, desc = "Auto-look at the target cauldron after picking it up.")

    private var currentYaw = 0f
    private var currentPitch = 0f
    private var currentRelic :Relic? = null
    private var currentClass: DungeonClass? = null
    private var lookingCauldron = false
    private var targetVec: Vec3? = null

    init {
        onSend<ServerboundUseItemOnPacket> {
            if (DungeonUtils.getF7Phase() != M7Phases.P5 || hand == InteractionHand.OFF_HAND) return@onSend

            val block = mc.level?.getBlockState(hitResult.blockPos)?.block
            if (!block.equalsOneOf(Blocks.CAULDRON, Blocks.ANVIL)) return@onSend

            Relic.entries.find { it.id == currentRelic?.id }?.let {
                lookingCauldron = false
            }
        }

        on<WorldEvent.Load> {
            lookingCauldron = false
            currentRelic = null
            targetVec = null
        }

        on<TickEvent.Server> {
            if (DungeonUtils.getF7Phase() != M7Phases.P5) return@on
            currentRelic = Relic.entries.find { mc.player?.inventory?.find { item -> item.itemId == it.id } != null }
            targetVec = currentRelic?.cauldronVec3
            lookingCauldron = currentRelic != null
        }

        on<RenderEvent.Extract> {
            if (DungeonUtils.getF7Phase() != M7Phases.P5 || !lookingCauldron || !relicLook) return@on
            val player = mc.player ?: return@on
            if (targetVec != null && ((currentClass == DungeonClass.ARCHER || currentClass == DungeonClass.BERSERK))) {
                val targetRot = vec3ToRotation(targetVec!!)
                currentYaw = exponentialSmooth(currentYaw, targetRot.yaw, 0.5f)
                currentPitch = exponentialSmooth(currentPitch, targetRot.pitch, 0.5f)
                player.yRot = currentYaw
                player.xRot = currentPitch
            }
        }
    }

    private enum class Relic(
        val id: String,
        val cauldronVec3: Vec3
    ) {
        Green("GREEN_KING_RELIC", Vec3(49.0, 7.0, 44.0)),
        Purple("PURPLE_KING_RELIC", Vec3(54.0, 7.0, 41.0)),
        Blue("BLUE_KING_RELIC", Vec3(59.0, 7.0, 44.0)),
        Orange("ORANGE_KING_RELIC", Vec3(57.0, 7.0, 42.0)),
        Red("RED_KING_RELIC", Vec3(51.0, 7.0, 42.0))
    }
}