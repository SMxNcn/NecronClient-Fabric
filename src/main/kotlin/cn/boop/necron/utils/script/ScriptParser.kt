package cn.boop.necron.utils.script

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.lwjgl.glfw.GLFW
import java.io.File
import java.nio.file.Files

object ScriptParser {
    fun parse(file: File): ScriptConfigJson? {
        return try {
            if (!file.exists() || !file.name.endsWith(".json")) return null
            val content = Files.readString(file.toPath())
            val config = Gson().fromJson(content, ScriptConfigJson::class.java)

            if (config == null) {
                println("Invalid JSON in ${file.name}")
                return null
            }
            config
        } catch (e: JsonSyntaxException) {
            println("Syntax error in ${file.name}: ${e.message}")
            null
        } catch (e: Exception) {
            println("Error reading ${file.name}: ${e.message}")
            null
        }
    }

    fun resolveKeyCode(keyStr: String): Int {
        if (keyStr.startsWith("KEY_")) {
            return when (keyStr) {
                "KEY_A" -> GLFW.GLFW_KEY_A
                "KEY_B" -> GLFW.GLFW_KEY_B
                "KEY_C" -> GLFW.GLFW_KEY_C
                "KEY_D" -> GLFW.GLFW_KEY_D
                "KEY_E" -> GLFW.GLFW_KEY_E
                "KEY_F" -> GLFW.GLFW_KEY_F
                "KEY_G" -> GLFW.GLFW_KEY_G
                "KEY_H" -> GLFW.GLFW_KEY_H
                "KEY_I" -> GLFW.GLFW_KEY_I
                "KEY_J" -> GLFW.GLFW_KEY_J
                "KEY_K" -> GLFW.GLFW_KEY_K
                "KEY_L" -> GLFW.GLFW_KEY_L
                "KEY_M" -> GLFW.GLFW_KEY_M
                "KEY_N" -> GLFW.GLFW_KEY_N
                "KEY_O" -> GLFW.GLFW_KEY_O
                "KEY_P" -> GLFW.GLFW_KEY_P
                "KEY_Q" -> GLFW.GLFW_KEY_Q
                "KEY_R" -> GLFW.GLFW_KEY_R
                "KEY_S" -> GLFW.GLFW_KEY_S
                "KEY_T" -> GLFW.GLFW_KEY_T
                "KEY_U" -> GLFW.GLFW_KEY_U
                "KEY_V" -> GLFW.GLFW_KEY_V
                "KEY_W" -> GLFW.GLFW_KEY_W
                "KEY_X" -> GLFW.GLFW_KEY_X
                "KEY_Y" -> GLFW.GLFW_KEY_Y
                "KEY_Z" -> GLFW.GLFW_KEY_Z
                "KEY_0" -> GLFW.GLFW_KEY_0
                "KEY_1" -> GLFW.GLFW_KEY_1
                "KEY_2" -> GLFW.GLFW_KEY_2
                "KEY_3" -> GLFW.GLFW_KEY_3
                "KEY_4" -> GLFW.GLFW_KEY_4
                "KEY_5" -> GLFW.GLFW_KEY_5
                "KEY_6" -> GLFW.GLFW_KEY_6
                "KEY_7" -> GLFW.GLFW_KEY_7
                "KEY_8" -> GLFW.GLFW_KEY_8
                "KEY_9" -> GLFW.GLFW_KEY_9
                "KEY_ENTER" -> GLFW.GLFW_KEY_ENTER
                "KEY_ESCAPE" -> GLFW.GLFW_KEY_ESCAPE
                "KEY_SPACE" -> GLFW.GLFW_KEY_SPACE
                "KEY_LSHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT
                "KEY_RSHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT
                "KEY_LCONTROL" -> GLFW.GLFW_KEY_LEFT_CONTROL
                "KEY_RCONTROL" -> GLFW.GLFW_KEY_RIGHT_CONTROL
                else -> {
                    println("Unknown key name: $keyStr")
                    GLFW.GLFW_KEY_UNKNOWN
                }
            }
        }
        return keyStr.toIntOrNull() ?: GLFW.GLFW_KEY_UNKNOWN
    }
}