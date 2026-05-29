package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.*
import cn.boop.necron.utils.RotationUtils.vec3ToRotation
import cn.boop.necron.utils.dungeon.P3Stages
import cn.boop.necron.utils.dungeon.getP3Stage
import cn.boop.necron.utils.dungeon.leapTo
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.*
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.equalsOneOf
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils.dungeonTeammates
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

object Auto4 : Module (
    name = "Auto 4",
    description = "Auto complete Arrow Sharp device on F7/M7.",
    category = NCategory.NECRON
) {
    private val leapTarget by SelectorSetting("Leap Target", "Tank", listOf("Archer", "Berserk", "Healer", "Mage", "Tank"), desc = "Class to leap to after completing device.")
    private val aimSpeed by NumberSetting("Aim Speed", 0.25f, 0.05f, 0.5f, 0.05f, desc = "Smooth aiming transition speed.")

    private data class AimPoint(
        val index: Int,
        val position: Vec3,
        val coveredBlocks: Set<BlockPos>
    )

    private val deviceCompleteRegex = Regex("^(.{1,16}) completed a device! \\((\\d)/(\\d)\\)$")
    private val markedPositions = mutableSetOf<BlockPos>()
    private var targetPosition: BlockPos? = null
    private var optimalAimPoints: List<AimPoint> = emptyList()
    private var currentYaw = 0f
    private var currentPitch = 0f
    private var isShooting = false
    private var hitCount = 0
    private var lastClickTime = 0L
    private var lastRenderUpdate = 0L
    private var isDeviceComplete = false
    private var isI4Leap = false

    private val TARGET_POSITIONS = listOf(
        BlockPos(68, 130, 50), BlockPos(66, 130, 50), BlockPos(64, 130, 50),
        BlockPos(68, 128, 50), BlockPos(66, 128, 50), BlockPos(64, 128, 50),
        BlockPos(68, 126, 50), BlockPos(66, 126, 50), BlockPos(64, 126, 50)
    )
    private val AIMING_POSITIONS = listOf(
        Vec3(67.3, 131.1, 48.8),
        Vec3(65.5, 131.1, 48.7),
        Vec3(67.3, 129.1, 48.8),
        Vec3(65.4, 129.1, 48.7),
        Vec3(67.3, 127.5, 48.8),
        Vec3(65.5, 127.3, 48.7)
    )

    private val ADJACENT_PAIRS = listOf(
        Pair(BlockPos(66, 130, 50), BlockPos(68, 130, 50)) to 0,
        Pair(BlockPos(64, 130, 50), BlockPos(66, 130, 50)) to 1,
        Pair(BlockPos(66, 128, 50), BlockPos(68, 128, 50)) to 2,
        Pair(BlockPos(64, 128, 50), BlockPos(66, 128, 50)) to 3,
        Pair(BlockPos(66, 126, 50), BlockPos(68, 126, 50)) to 4,
        Pair(BlockPos(64, 126, 50), BlockPos(66, 126, 50)) to 5
    )

    private var isPaused = false

    fun pauseShooting() {
        isPaused = true
        isShooting = false
    }

    fun resumeShooting() {
        isPaused = false
    }

    fun isDeviceIncomplete() = enabled && isPlayerAtDevice() && !isDeviceComplete

    private fun resetState() {
        markedPositions.clear()
        targetPosition = null
        optimalAimPoints = emptyList()
        hitCount = 0
        isDeviceComplete = false
        isShooting = false
        isPaused = false
        isI4Leap = false
    }

    init {
        on<BlockUpdateEvent> {
            if (getP3Stage() != P3Stages.S4 || !TARGET_POSITIONS.contains(pos)) return@on
            when (old.block) {
                Blocks.EMERALD_BLOCK if updated.block == Blocks.BLUE_TERRACOTTA -> {
                    markedPositions.add(pos.immutable())
                    if (targetPosition == pos) {
                        targetPosition = null
                        isShooting = false
                    }
                    targetPosition?.let { optimalAimPoints = selectOptimalAimPoints(it) }
                }
                Blocks.BLUE_TERRACOTTA if updated.block == Blocks.EMERALD_BLOCK -> {
                    markedPositions.remove(pos)
                    targetPosition = pos.immutable()
                    optimalAimPoints = selectOptimalAimPoints(pos)
                    mc.player?.let {
                        currentYaw = it.yRot
                        currentPitch = it.xRot
                    }
                }
            }
        }

        on<RenderEvent.Extract> {
            val player = mc.player ?: return@on

            val now = System.currentTimeMillis()
            if (now - lastRenderUpdate < 7L) return@on
            lastRenderUpdate = now

            val shouldAim = getP3Stage() == P3Stages.S4 && isPlayerAtDevice() && !isDeviceComplete && !isPaused
                    && targetPosition != null && !isShooting && optimalAimPoints.isNotEmpty()

            val targetVec = if (shouldAim) optimalAimPoints[0].position else null
            RotationUtils.update(player.yRot, player.xRot, targetVec, aimSpeed, true)
        }

        on<TickEvent.End> {
            if (!isPlayerAtDevice() || isDeviceComplete || isPaused || optimalAimPoints.isEmpty()) return@on
            val aimVec = optimalAimPoints[0].position
            if (isRotationAligned(vec3ToRotation(aimVec))) {
                if (System.currentTimeMillis() - lastClickTime > 100 + (0..40).random()) {
                    leftClick()
                    lastClickTime = System.currentTimeMillis()
                }
            }
        }

        on<ChatPacketEvent> {
            if (getP3Stage() != P3Stages.S4 || isDeviceComplete) return@on
            val message = value.clean
            println(message)
            val matcher = deviceCompleteRegex.find(message)
            if (matcher != null) {
                val player = matcher.groupValues[1]
                if (player == mc.player?.name?.legacy) {
                    schedule(4) { onComplete() }
                }
            }
        }

        on<GuiEvent.Open> {
            val chest = (screen as? AbstractContainerScreen<*>) ?: return@on
            if (!isI4Leap) return@on
            val inLeapGui = chest.title.string.equalsOneOf("Spirit Leap", "Teleport to Player")
            if (!DungeonUtils.inDungeons || !inLeapGui) return@on
            schedule(4) { leapBack(chest) }
        }

        on<WorldEvent.Load> {
            resetState()
        }
    }

    private fun selectOptimalAimPoints(target: BlockPos): List<AimPoint> {
        val unmarked = TARGET_POSITIONS.filterNot { it in markedPositions }

        val candidates = ADJACENT_PAIRS.filter { (pair, _) ->
            target in listOf(pair.first, pair.second)
        }.mapNotNull { (pair, index) ->
            val covered = setOf(pair.first, pair.second).filter { it in unmarked }.toSet()
            AimPoint(
                index = index,
                position = AIMING_POSITIONS[index],
                coveredBlocks = covered
            ).takeIf { it.coveredBlocks.isNotEmpty() }
        }

        val green = candidates.maxByOrNull { it.coveredBlocks.size } ?: return emptyList()

        val remaining = ADJACENT_PAIRS.filterNot { (pair, _) ->
            target in listOf(pair.first, pair.second)
        }.mapNotNull { (pair, index) ->
            val covered = setOf(pair.first, pair.second).filter { it in unmarked }.toSet()
            AimPoint(
                index = index,
                position = AIMING_POSITIONS[index],
                coveredBlocks = covered
            ).takeIf { it.coveredBlocks.isNotEmpty() }
        }

        return findBestCombination(green, remaining)
    }

    private fun findBestCombination(green: AimPoint, candidates: List<AimPoint>): List<AimPoint> {
        val result = mutableListOf(green)
        val covered = green.coveredBlocks.toMutableSet()

        repeat(2) {
            val best = candidates.filterNot { it in result }
                .maxWithOrNull(
                    compareBy(
                        { it.coveredBlocks.count { block -> block !in covered } },
                        { it.coveredBlocks.size },
                        { -result.last().position.distanceTo(it.position) }
                    )
                ) ?: return@repeat

            result.add(best)
            covered.addAll(best.coveredBlocks)
        }

        return result
    }

    private fun isPlayerAtDevice(): Boolean {
        val player = mc.player ?: return false
        return player.x in 62.0..64.0 && player.y in 127.0..129.0 && player.z in 34.0..36.0
    }

    private fun isRotationAligned(target: RotationUtils.Rotation): Boolean {
        return abs(Mth.wrapDegrees(currentYaw - target.yaw)) < 0.5f &&
                abs(Mth.wrapDegrees(currentPitch - target.pitch)) < 0.5f
    }

    private fun leapBack(screen: AbstractContainerScreen<*>) {
        val targetClass = dungeonTeammates.find { !it.isDead && it.clazz == when(leapTarget) {
            0 -> DungeonClass.Archer
            1 -> DungeonClass.Berserk
            2 -> DungeonClass.Healer
            3 -> DungeonClass.Mage
            else -> DungeonClass.Tank
        } }

        targetClass?.name?.let { leapTo(it, screen) }
    }

    private fun onComplete() {
        isDeviceComplete = true
        isShooting = false
        resetState()

        if (findLeapSlot() == -1) return
        mc.player?.inventory?.selectedSlot = findLeapSlot()
        schedule(4) {
            rightClick()
            isI4Leap = true
        }
    }
}