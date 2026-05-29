package cn.boop.necron.utils.screen

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.Component
import java.awt.Desktop
import java.io.File

class CrashScreen(
    private val guiTitle: String,
    private val message: String
)  : Screen(Component.literal("Crash Patch")) {
    override fun init() {
        super.init()
        val centerX = width / 2
        val centerY = height / 2

        addRenderableWidget(Button.Builder(Component.literal("Back to game")) {
            try {
                val logDir = File(minecraft.gameDirectory, "logs")
                if (logDir.exists() && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(logDir)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.bounds(centerX - 140 - 15, height - 50, 160, 20).build())

        addRenderableWidget(Button.Builder(Component.literal("Back to main menu")) {
            minecraft.setScreen(TitleScreen())
        }.bounds(centerX + 15, height - 50, 160, 20).build())
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        val centerX = width / 2
        val font = minecraft.font!!

        val mainTitle = "Uh-oh, your game crashed!"
        guiGraphics.drawString(
            font, mainTitle,
            centerX - font.width(mainTitle) / 2, 40,
            0xFF6666, true
        )

        guiGraphics.drawString(
            font, guiTitle,
            centerX - font.width(guiTitle) / 2, 140,
            0xFFAA00, true
        )

        guiGraphics.drawString(
            font, message,
            centerX - font.width(message) / 2, 180,
            0xFFFF55, true
        )

        val boxYStart = 100
        val boxHeight = 180
        val boxWidth = 380
        val boxX = centerX - boxWidth / 2
        guiGraphics.fill(boxX, boxYStart, boxX + boxWidth, boxYStart + boxHeight, 0xC0101010.toInt())
        // 绘制边框 (简单实现)
        guiGraphics.fill(boxX, boxYStart, boxX + boxWidth, boxYStart + 1, 0xFF666666.toInt())
        guiGraphics.fill(boxX, boxYStart + boxHeight - 1, boxX + boxWidth, boxYStart + boxHeight, 0xFF666666.toInt())
        guiGraphics.fill(boxX, boxYStart, boxX + 1, boxYStart + boxHeight, 0xFF666666.toInt())
        guiGraphics.fill(boxX + boxWidth - 1, boxYStart, boxX + boxWidth, boxYStart + boxHeight, 0xFF666666.toInt())

        // 绘制堆栈行
        var yOffset = boxYStart + 10
        val lines = message.split('\n')
        for (line in lines) {
            if (yOffset > boxYStart + boxHeight - 10) break
            guiGraphics.drawString(font, line, centerX - 190, yOffset, 0xAAAAAA, false)
            yOffset += 9
        }

        if (lines.size > 8) {
            guiGraphics.drawString(font, "... (Click button to copy full logs)", centerX - 190, yOffset - 5, 0x555555, false)
        }
    }

    override fun isPauseScreen(): Boolean = false
    override fun shouldCloseOnEsc(): Boolean = false
}