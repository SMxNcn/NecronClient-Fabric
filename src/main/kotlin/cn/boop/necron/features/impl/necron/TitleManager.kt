package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import org.lwjgl.glfw.GLFW

object TitleManager : Module (
    name = "Title Manager",
    description = "Manage your window title.",
    category = NCategory.NECRON
) {
    private val titleText by StringSetting("Title", "Minecraft 1.21.10", desc = "Title")
    private val dropdown by DropdownSetting("Show Settings")
    private val displayLocation by BooleanSetting("Location", false, desc = "Island name").withDependency { dropdown }
    private val displayPlayer by BooleanSetting("Player Name", false, desc = "Player name").withDependency { dropdown }
    private val windowIcon by BooleanSetting("Window Icon", false, desc = "Restart the game to apply icon changes").withDependency { dropdown }

    fun updateTitle() {
        val window = OdinMod.mc.window ?: return
        val handler = window.handle()
        val newTitle = buildTitle()
        if (handler != 0L) {
            try {
                GLFW.glfwSetWindowTitle(window.handle(), newTitle)
            } catch (_: Exception) {}
        }
    }

    fun buildTitle(): String {
        val sb = StringBuilder(titleText)
        var locationText = LocationUtils.currentArea.displayName
        val playerName = OdinMod.mc.player?.name?.string
        if (LocationUtils.currentArea == Island.Dungeon) {
            locationText += " ${DungeonUtils.floor?.name}"
        }
        if (displayLocation && !locationText.contains("Unknown")) sb.append(" | ").append(locationText)
        if (displayPlayer && playerName != null) sb.append(" | ").append(playerName)
        return sb.toString()
    }

    fun isCustomIcon() = enabled && windowIcon
}