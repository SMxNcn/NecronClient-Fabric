package cn.boop.necron.utils.crash

import net.minecraft.ChatFormatting
import net.minecraft.CrashReport
import net.minecraft.ReportType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.Component
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date

object CrashPatch {
    val logger: Logger = LogManager.getLogger(CrashPatch.javaClass)
    @JvmStatic
    var exceptionCounter = 0
    const val MAX_EXCEPTION_COUNTER = 10

    @JvmStatic
    fun incrementCounter() {
        exceptionCounter++
    }

    @JvmStatic
    fun decrementCounter() {
        if (exceptionCounter > 0) exceptionCounter--
    }

    @JvmStatic
    fun triggerSave(mc: Minecraft) {
        try {
            logger.warn("Triggering emergency cleanup...")

            if (mc.player != null && mc.level != null) {
                if (mc.isSingleplayer) {
                    try {
                        mc.singleplayerServer?.saveEverything(true, true, true)
                        logger.info("Singleplayer world saved successfully.")
                    } catch (e: Exception) {
                        logger.error("Failed to save singleplayer world", e)
                    }

                    mc.level?.disconnect(Component.literal("Oh no, your game crashed!").withStyle(ChatFormatting.RED))
                } else {
                    try {
                        mc.connection?.close()
                        mc.level?.disconnect(Component.literal("Oh no, your game crashed!").withStyle(ChatFormatting.RED))
                        mc.level = null
                        mc.player = null
                    } catch (e: Exception) {
                        mc.level = null
                        mc.player = null
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed during emergency cleanup", e)
        }
    }

    @JvmStatic
    fun saveReport(gameDir: File?, report: CrashReport) {
        if (gameDir == null) return
        try {
            val crashDir = File(gameDir, "crash-reports")
            if (!crashDir.exists()) crashDir.mkdirs()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")
            val fileName = "crash-${dateFormat.format(Date())}-intercepted.txt"
            val reportFile = File(crashDir, fileName)

            FileWriter(reportFile).use { writer ->
                writer.write(report.getFriendlyReport(ReportType.CRASH))
            }
            logger.info("Native report saved to: ${reportFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to save native report", e)
        }
    }

    @JvmStatic
    fun showCrashScreen(mc: Minecraft, throwable: Throwable, report: CrashReport) {
        if (!mc.isRunning) return
        try {
            val crashScreen = CrashScreen(
                report.title,
                report.toString(),
                throwable.stackTraceToString().split("\n").take(8).joinToString("\n"),
                throwable.stackTraceToString()
            )
            mc.setScreen(crashScreen)
        } catch (_: Exception) {
            mc.setScreen(TitleScreen())
        }
    }
}