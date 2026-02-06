package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import cn.boop.necron.utils.removeFormatting
import cn.boop.necron.utils.toLegacyString
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.render.drawText
import com.odtheking.odin.utils.renderBoundingBox
import com.odtheking.odin.utils.renderX
import com.odtheking.odin.utils.renderY
import com.odtheking.odin.utils.renderZ
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

object Nametags : Module(
    name = "Nametags",
    description = "Render a nametag above players.",
    category = NCategory.NECRON
) {
    private val dropdown by DropdownSetting("Show Settings")
    private val renderDistance by BooleanSetting("Distance", true, desc = "Render distance string.").withDependency { dropdown }
    private val teammateESP by BooleanSetting("Teammate ESP", false, desc = "Show ESP box for dungeon teammates.").withDependency { dropdown }
    private val removeGlowing by BooleanSetting("Disable Glowing effect", true, desc = "Removes glowing effect by Hypixel.").withDependency { teammateESP }
    private val forceSkyBlock by BooleanSetting("Force SkyBlock", false, desc = "Force render other players.")

    init {
        on<RenderEvent.Extract> {
            if (!canDisplayNametags()) return@on
            val level = mc.level ?: return@on
            val player = mc.player ?: return@on

            for (entity in level.players()) {
                if (entity == player) continue
                if (!isValidSkyBlockPlayer(entity)) continue
                if (entity.isRemoved || !entity.isAlive) continue

                val distance = player.distanceTo(entity)

                val nametagText = buildNametagText(entity, distance.toInt())
                val scale = calculateScale(distance)
                val yOffset = if (entity.isCrouching) 0.6 + scale / 5f else 0.8 + scale / 5f
                val renderPos = Vec3(entity.renderX, entity.renderY + entity.eyeHeight + yOffset, entity.renderZ)

                drawText(text = nametagText, pos = renderPos, scale = scale, depth = false)

                if (teammateESP && DungeonUtils.inDungeons && entity != mc.player) {
                    val playerName = entity.name.string
                    val dungeonPlayer = DungeonUtils.dungeonTeammates.find { it.name == playerName }

                    dungeonPlayer?.let { dp ->
                        val color = getDungeonClassColor(dp.clazz)
                        drawStyledBox(aabb = entity.renderBoundingBox, color = color, style = 1, depth = false)
                    }
                }
            }
        }
    }

    private fun calculateScale(distance: Float): Float {
        var size = distance / 10.0f
        if (size < 1.1f) size = 1.1f
        return size * 2f / 1.6f
    }

    private fun buildNametagText(entity: Player, distance: Int): String {
        val playerName = entity.name.string

        return if (LocationUtils.isCurrentArea(Island.Dungeon)) {
            val dungeonPlayer = DungeonUtils.dungeonTeammates.find { it.name == playerName }

            if (dungeonPlayer != null) {
                val clazz = dungeonPlayer.clazz
                val classInitial = clazz.name.first().uppercase()
                val classColor = getClassColorCode(clazz)

                "$classColor[$classInitial] $playerName"
            } else {
                "§7[?] $playerName"
            }
        } else {
            val displayName = entity.displayName?.toLegacyString()
            val strippedName = displayName!!

            return if (renderDistance) "$strippedName §7${distance}m" else strippedName
        }
    }

    fun isValidSkyBlockPlayer(entity: Player): Boolean {
        val name = entity.displayName?.string?.removeFormatting()
        if (name != null) {
            return if (forceSkyBlock) !name.contains("[NPC]") && !name.contains("CIT-")
            else name.matches(Regex("^\\[\\d{1,3}]\\s[a-zA-Z0-9_]{1,16}.*"))
        }
        return false
    }

    private fun getDungeonClassColor(clazz: DungeonClass): Color {
        return when (clazz) {
            DungeonClass.Archer -> Color(255, 85, 85)
            DungeonClass.Berserk -> Color(255, 170, 0)
            DungeonClass.Healer -> Color(255, 85, 255)
            DungeonClass.Mage -> Color(85, 255, 255)
            DungeonClass.Tank -> Color(0, 170, 0)
            else -> Color(170, 170, 170)
        }
    }

    private fun getClassColorCode(clazz: DungeonClass): String {
        return when (clazz) {
            DungeonClass.Archer -> "§c"
            DungeonClass.Berserk -> "§6"
            DungeonClass.Healer -> "§d"
            DungeonClass.Mage -> "§b"
            DungeonClass.Tank -> "§a"
            else -> "§7"
        }
    }

    fun isDungeonTeammate(player: Player): Boolean {
        if (!DungeonUtils.inDungeons) return false

        val playerName = player.name.string
        return DungeonUtils.dungeonTeammates.any { it.name == playerName }
    }

    fun shouldRemoveGlowing(): Boolean {
        return enabled && teammateESP && removeGlowing && DungeonUtils.inDungeons
    }

    fun canDisplayNametags(): Boolean {
        return enabled && (forceSkyBlock || LocationUtils.isInSkyblock)
    }
}