package cn.boop.necron.utils.script

import cn.boop.necron.Necron.scriptDir
import cn.boop.necron.utils.modMessage
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

object ScriptManager {
    private val scripts = mutableListOf<ScriptConfigJson>()
    val triggerMap = ConcurrentHashMap<Int, MutableList<ScriptConfigJson>>()
    val lastFrameKeys = mutableSetOf<Int>()
    val lastFrameKeyStates = mutableMapOf<Int, Boolean>()

    init {
        loadAllScripts()
    }

    fun loadAllScripts() {
        scripts.clear()
        triggerMap.clear()

        try {
            Files.walk(scriptDir.toPath())
                .filter { it.toString().endsWith(".json") }
                .forEach { path ->
                    val config = ScriptParser.parse(path.toFile())
                    if (config != null) {
                        scripts.add(config)
                        if (config.enabled) {
                            val keyCode = ScriptParser.resolveKeyCode(config.triggerKeyStr)
                            if (keyCode != -1) {
                                triggerMap.computeIfAbsent(keyCode) { mutableListOf() }.add(config)
                            }
                        }
                    }
                }
            modMessage("Loaded §a${scripts.size} §7scripts.")
        } catch (e: Exception) {
            modMessage("§cFailed to load scripts: ${e.message}")
            e.printStackTrace()
        }
    }

    fun reloadScripts() {
        loadAllScripts()
        modMessage("Scripts reloaded.")
    }

    fun triggerScripts(keyCode: Int) {
        val targetScripts = triggerMap[keyCode] ?: return
        targetScripts.toList().forEach { script ->
            if (script.enabled) {
                ScriptEngine.execute(script)
            }
        }
    }

    fun getScriptByName(name: String): ScriptConfigJson? {
        return scripts.find { it.name == name }
    }
}