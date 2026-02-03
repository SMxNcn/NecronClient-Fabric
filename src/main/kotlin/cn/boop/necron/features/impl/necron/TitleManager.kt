package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.LocationUtils
import org.lwjgl.glfw.GLFW

object TitleManager : Module (
    name = "Title Manager",
    description = "Manage your window title.",
    category = NCategory.NECRON
) {
    private var titleText by StringSetting("Title", "Minecraft 1.21.10", desc = "Title")
    private var dropdown by DropdownSetting("Show Settings")
    private var displayLocation by BooleanSetting("Location", false, desc = "Island name").withDependency { dropdown }
    private var displayPlayer by BooleanSetting("Player Name", false, desc = "Player name").withDependency { dropdown }

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
        val locationText = LocationUtils.currentArea.name
        val playerName = OdinMod.mc.player?.name?.string
        if (displayLocation && !locationText.contains("Unknown")) sb.append(" | ").append(locationText)
        if (displayPlayer && playerName != null) sb.append(" | ").append(playerName)
        return sb.toString()
    }
}