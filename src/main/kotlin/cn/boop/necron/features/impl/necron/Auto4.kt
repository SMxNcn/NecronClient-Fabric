package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import cn.boop.necron.utils.RotationUtils
import cn.boop.necron.utils.RotationUtils.exponentialSmooth
import cn.boop.necron.utils.RotationUtils.vec3ToRotation
import cn.boop.necron.utils.clean
import cn.boop.necron.utils.dungeon.P3Stages
import cn.boop.necron.utils.dungeon.getP3Stage
import cn.boop.necron.utils.dungeon.leapTo
import cn.boop.necron.utils.leftClick
import cn.boop.necron.utils.legacy
import cn.boop.necron.utils.rightClick
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.BlockUpdateEvent
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
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
    private val leapItem by SelectorSetting("Leap Type", "Infinite", listOf("Infinite Leap", "Spirit Leap"), desc = "Which leap item used for auto leap.")
    private val aimSpeed by NumberSetting("Aim Speed", 0.25f, 0.05f, 1.0f, 0.05f, desc = "Smooth aiming transition speed.")

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

    var isPaused = false
        private set

    fun pauseShooting() {
        isPaused = true
        isShooting = false
    }

    fun resumeShooting() {
        isPaused = false
    }

    fun isDeviceIncomplete(): Boolean {
        return enabled && isPlayerAtDevice() && !isDeviceComplete
    }

    private fun resetState() {
        markedPositions.clear()
        targetPosition = null
        optimalAimPoints = emptyList()
        hitCount = 0
        isDeviceComplete = false
        isShooting = false
        isPaused = false
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

            if (getP3Stage() != P3Stages.S4 || !isPlayerAtDevice() || isDeviceComplete || isPaused) return@on
            if (targetPosition != null && !isShooting && optimalAimPoints.isNotEmpty()) {
                val aimVec = optimalAimPoints[0].position
                val targetRot = vec3ToRotation(aimVec)

                currentYaw = exponentialSmooth(currentYaw, targetRot.yaw, aimSpeed)
                currentPitch = exponentialSmooth(currentPitch, targetRot.pitch, aimSpeed)
                player.yRot = currentYaw
                player.xRot = currentPitch
            }
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

    private fun onComplete() {
        isDeviceComplete = true
        isShooting = false
        resetState()

        val tank = dungeonTeammates.find { it.clazz == DungeonClass.Tank && !it.isDead }
        var leapSlot = -1
        val id = when (leapItem) {
            0 -> "INFINITE_SPIRIT_LEAP"
            else -> "SPIRIT_LEAP"
        }

        for (slot in 0..8) {
            val item = mc.player?.inventory?.getItem(slot) ?: return

            if (item.itemId.contains(id)) {
                leapSlot = slot
                break
            }
        }

        mc.player?.inventory?.selectedSlot = leapSlot
        schedule(4) {
            rightClick()
            schedule(4) {
                val screen = mc.screen as? AbstractContainerScreen<*> ?: return@schedule
                schedule(4) {
                    tank?.name?.let { leapTo(it, screen) }
                }
            }
        }
    }
}