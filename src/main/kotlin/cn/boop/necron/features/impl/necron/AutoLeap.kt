package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import cn.boop.necron.utils.dungeon.MageCoreCheck
import cn.boop.necron.utils.dungeon.P3Stages
import cn.boop.necron.utils.dungeon.getP3Stage
import cn.boop.necron.utils.dungeon.leapTo
import cn.boop.necron.utils.rightClick
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.GuiEvent
import com.odtheking.odin.events.InputEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.equalsOneOf
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.sendCommand
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

object AutoLeap : Module(
    name = "Auto Leap",
    description = "Auto leap to players based on predefined rules.",
    category = NCategory.NECRON
) {
    private val leapAnnounce by BooleanSetting("Leap Announce", false, desc = "Announces when you leap to a player.")
    private val announceMessage by StringSetting("Announce Message", "Leaped to %p! (%c)", desc = "Message to announce in party after leaping. Use %p for player name, %c for class.").withDependency { leapAnnounce }

    private var inLeapGui = false
    private var shouldAutoLeap = false
    private var targetClass: DungeonClass? = null
    private val leapedRegex = Regex("You have teleported to (\\w{1,16})!")

    init {
        on<InputEvent>{
            if (!DungeonUtils.inDungeons || key.value != 0) return@on
            if (mc.player?.mainHandItem?.itemId.equalsOneOf("INFINITE_SPIRIT_LEAP", "SPIRIT_LEAP")) {
                targetClass = selectLeapTarget()
                shouldAutoLeap = true
                cancel()
                rightClick()
            }
        }

        on<GuiEvent.Open> {
            val chest = (screen as? AbstractContainerScreen<*>) ?: return@on
            inLeapGui = chest.title.string.equalsOneOf("Spirit Leap", "Teleport to Player")
            if (!DungeonUtils.inDungeons || !inLeapGui || !shouldAutoLeap) return@on
            if (enabled) {
                schedule(5) {
                    performAutoLeap(chest)
                }
            } else {
                inLeapGui = false
            }
        }

        on<GuiEvent.Close> {
            if (!DungeonUtils.inDungeons) return@on
            shouldAutoLeap = false
            inLeapGui = false
        }

        on<ChatPacketEvent> {
            if (!DungeonUtils.inDungeons || !leapAnnounce) return@on

            leapedRegex.find(value)?.groupValues?.get(1)?.let { playerName ->
                val targetPlayer = DungeonUtils.dungeonTeammates.find { it.name == playerName }
                val className = targetPlayer?.clazz?.name ?: "null"

                val message = announceMessage
                    .replace("%p", playerName)
                    .replace("%c", className)

                sendCommand("pc $message")
            }
        }
    }

    private fun performAutoLeap(screen: AbstractContainerScreen<*>) {
        if (targetClass == null) {
            shouldAutoLeap = false
            return
        }

        val targetPlayer = DungeonUtils.dungeonTeammates.find {
            it.clazz == targetClass && !it.isDead
        }

        if (targetPlayer == null) {
            shouldAutoLeap = false
            return
        }

        try {
            leapTo(targetPlayer.name, screen)
            modMessage("Teleporting to ${targetPlayer.name} (${targetClass})")
        } catch (_: Exception) {} finally {
            shouldAutoLeap = false
        }
    }

    private fun selectLeapTarget(): DungeonClass? {
        if (!DungeonUtils.inDungeons) return null

        return if (DungeonUtils.inBoss) {
            handleM7Phase()
        } else {
            handlePreBossPhase()
        }
    }

    private fun handlePreBossPhase(): DungeonClass? {
        val myPlayer = DungeonUtils.currentDungeonPlayer
        val myClass = myPlayer.clazz

        if (myClass != DungeonClass.Mage && myClass != DungeonClass.Archer) return null

        val doorOpenerName = DungeonUtils.doorOpener
        if (doorOpenerName.isNotBlank()) {
            val doorOpener = DungeonUtils.dungeonTeammates.find { it.name == doorOpenerName }
            if (doorOpener != null && !doorOpener.name.equals(mc.player?.name)) {
                return doorOpener.clazz
            }
        }

        val teammateToLeap = when (myClass) {
            DungeonClass.Archer -> DungeonUtils.dungeonTeammates.find { it.clazz == DungeonClass.Mage }
            DungeonClass.Mage -> DungeonUtils.dungeonTeammates.find { it.clazz == DungeonClass.Archer }
            else -> null
        }

        return teammateToLeap?.clazz
    }

    private fun handleM7Phase(): DungeonClass? {
        val floor = DungeonUtils.floor ?: return null
        if (floor.floorNumber != 7) return null

        val myClass = DungeonUtils.currentDungeonPlayer.clazz
        val phase = DungeonUtils.getF7Phase()
        val p3Stage = getP3Stage()

        if (phase == M7Phases.Unknown) return null

        val isCore = p3Stage == P3Stages.S3 && MageCoreCheck.checkIfMageIsCore()

        return queryRule(myClass, phase, p3Stage, isCore)
    }

    private fun queryRule(sourceClass: DungeonClass, phase: M7Phases, p3Stage: P3Stages, isCore: Boolean = false): DungeonClass? {
        return when (sourceClass) {
            // Archer
            DungeonClass.Archer -> when (phase) {
                M7Phases.P1 -> DungeonClass.Berserk
                M7Phases.P2 -> DungeonClass.Healer
                M7Phases.P3 -> when (p3Stage) {
                    P3Stages.S1 -> null
                    P3Stages.S2 -> DungeonClass.Healer
                    P3Stages.S3 -> {
                        if (isCore) DungeonClass.Mage
                        else null
                    }
                    P3Stages.S4 -> DungeonClass.Mage
                    P3Stages.Tunnel -> DungeonClass.Healer
                    else -> null
                }
                M7Phases.P4 -> DungeonClass.Healer
                M7Phases.P5 -> null
                else -> null
            }

            // Berserk
            DungeonClass.Berserk -> when (phase) {
                M7Phases.P1 -> null
                M7Phases.P2 -> DungeonClass.Healer
                M7Phases.P3 -> when (p3Stage) {
                    P3Stages.S1 -> DungeonClass.Archer
                    P3Stages.S2 -> DungeonClass.Healer
                    P3Stages.S3 -> {
                        if (isCore) DungeonClass.Mage
                        else DungeonClass.Archer
                    }
                    P3Stages.S4 -> DungeonClass.Mage
                    P3Stages.Tunnel -> DungeonClass.Healer
                    else -> null
                }
                M7Phases.P4 -> DungeonClass.Healer
                M7Phases.P5 -> null
                else -> null
            }

            // Healer
            DungeonClass.Healer -> when (phase) {
                M7Phases.P1 -> null
                M7Phases.P2 -> DungeonClass.Archer
                M7Phases.P3 -> when (p3Stage) {
                    P3Stages.S1 -> DungeonClass.Archer
                    P3Stages.S2 -> null
                    P3Stages.S3 -> {
                        if (isCore) DungeonClass.Mage
                        else DungeonClass.Archer
                    }
                    P3Stages.S4 -> DungeonClass.Mage
                    P3Stages.Tunnel -> null
                    else -> null
                }
                M7Phases.P4 -> null
                M7Phases.P5 -> DungeonClass.Berserk
                else -> null
            }

            // Mage
            DungeonClass.Mage -> when (phase) {
                M7Phases.P1 -> DungeonClass.Berserk
                M7Phases.P2 -> DungeonClass.Healer
                M7Phases.P3 -> when (p3Stage) {
                    P3Stages.S1 -> DungeonClass.Archer
                    P3Stages.S2 -> DungeonClass.Healer
                    P3Stages.S3 -> null
                    P3Stages.S4 -> null
                    P3Stages.Tunnel -> DungeonClass.Healer
                    else -> null
                }
                M7Phases.P4 -> DungeonClass.Healer
                M7Phases.P5 -> DungeonClass.Berserk
                else -> null
            }

            // Tank
            DungeonClass.Tank -> when (phase) {
                M7Phases.P1 -> DungeonClass.Berserk
                M7Phases.P2 -> DungeonClass.Healer
                M7Phases.P3 -> when (p3Stage) {
                    P3Stages.S1 -> DungeonClass.Archer
                    P3Stages.S2 -> DungeonClass.Healer
                    P3Stages.S3 -> {
                        if (isCore) DungeonClass.Mage
                        else DungeonClass.Archer
                    }
                    P3Stages.S4 -> DungeonClass.Mage
                    P3Stages.Tunnel -> DungeonClass.Healer
                    else -> null
                }
                M7Phases.P4 -> DungeonClass.Healer
                M7Phases.P5 -> DungeonClass.Archer
                else -> null
            }

            DungeonClass.Unknown -> null
        }
    }
}