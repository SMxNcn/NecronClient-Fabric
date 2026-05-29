package cn.boop.necron.utils.crash

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.Component
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class CrashScreen(
    private val guiTitle: String,
    private val message: String,
    private val stackTracePreview: String,
    private val fullLog: String
)  : Screen(Component.literal("Crash Patch")) {
    override fun init() {
        super.init()
        val centerX = width / 2
        val centerY = height / 2

        addRenderableWidget(Button.Builder(Component.literal("Copy full crash logs")) {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(fullLog), null)
        }.bounds(centerX, centerY + 20, 300, 20).build())

        addRenderableWidget(Button.Builder(Component.literal("Back to main menu")) {
            minecraft.setScreen(TitleScreen())
        }.bounds(centerX - 100, centerY + 50, 200, 20).build())
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.fillGradient(0, 0, width, height, -1072689136, -804253680)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        val centerX = width / 2
        val font = minecraft.font!!

        guiGraphics.drawString(
            font, "Your game crashed!",
            centerX - font.width("Your game crashed!") / 2, 30,
            0xFF0000, true
        )

        guiGraphics.drawString(
            font, guiTitle,
            centerX - font.width(guiTitle) / 2, 60,
            0xFFAA00, true
        )

        guiGraphics.drawString(
            font, message,
            centerX - font.width(message) / 2, 75,
            0xFFFF55, true
        )

        val boxYStart = 90
        val boxHeight = 140
        guiGraphics.fill(centerX - 200, boxYStart, centerX + 200, boxYStart + boxHeight, 0xA0000000.toInt())
        // 绘制边框 (简单实现)
        guiGraphics.fill(centerX - 200, boxYStart, centerX + 200, boxYStart + 1, 0xFF555555.toInt())
        guiGraphics.fill(centerX - 200, boxYStart + boxHeight - 1, centerX + 200, boxYStart + boxHeight, 0xFF555555.toInt())
        guiGraphics.fill(centerX - 200, boxYStart, centerX - 199, boxYStart + boxHeight, 0xFF555555.toInt())
        guiGraphics.fill(centerX + 199, boxYStart, centerX + 200, boxYStart + boxHeight, 0xFF555555.toInt())

        // 绘制堆栈行
        var yOffset = boxYStart + 10
        val lines = stackTracePreview.split("\n")
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