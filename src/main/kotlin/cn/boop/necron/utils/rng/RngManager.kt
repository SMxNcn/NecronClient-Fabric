package cn.boop.necron.utils.rng

import cn.boop.necron.Necron
import cn.boop.necron.events.RngEvent
import cn.boop.necron.utils.modMessage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

open class RngManager(private val storageKey: String) {
    protected val gson = GsonBuilder().setPrettyPrinting().create()
    protected val meters: MutableMap<String, RngMeterUserData> = mutableMapOf()
    private val dataFile = File(Necron.configDir, "necron/data/data.json")

    protected fun loadPersistedData() {
        synchronized(RngManager::class.java) {
            modMessage("§d[RNG Debug] $storageKey loadPersistedData: path=${dataFile.absolutePath}, exists=${dataFile.exists()}")
            if (!dataFile.exists()) {
                modMessage("§d[RNG Debug] $storageKey data file not found, skipping load")
                return
            }
            try {
                val json = dataFile.readText()
                modMessage("§d[RNG Debug] $storageKey raw json=${json.take(200)}")
                val saveData = gson.fromJson(json, RngMeterSaveData::class.java)
                modMessage("§d[RNG Debug] $storageKey gson parsed: saveData=${saveData != null}")
                if (saveData == null) {
                    modMessage("§d[RNG Debug] $storageKey gson returned null")
                    return
                }
                val dataMap = when (storageKey) {
                    "dungeon" -> saveData.dungeonData
                    "slayer" -> saveData.slayerData
                    else -> return
                }
                modMessage("§d[RNG Debug] $storageKey dataMap size=${dataMap.size}, entries=${dataMap.entries}")
                meters.clear()
                meters.putAll(dataMap)
                for ((key, data) in meters) {
                    modMessage("§d[RNG Debug] $storageKey loaded key=$key score=${data.score} item=${data.item} needed=${data.needed}")
                    if (data.item != null && data.needed == null) {
                        data.needed = RngMeterValues.getNeeded(key, data.item!!)
                        modMessage("§d[RNG Debug] $storageKey backfilled needed for $key: ${data.needed}")
                    }
                }
            } catch (e: Exception) {
                modMessage("§d[RNG Debug] $storageKey load error: ${e.message}")
            }
        }
    }

    protected fun persistData() {
        synchronized(RngManager::class.java) {
            dataFile.parentFile?.mkdirs()
            val existingJson = if (dataFile.exists()) {
                try { dataFile.readText() } catch (_: Exception) { "{}" }
            } else {
                "{}"
            }

            val saveData = try {
                gson.fromJson(existingJson, RngMeterSaveData::class.java) ?: RngMeterSaveData()
            } catch (_: Exception) {
                RngMeterSaveData()
            }

            val targetMap = when (storageKey) {
                "dungeon" -> saveData.dungeonData
                "slayer" -> saveData.slayerData
                else -> return
            }

            targetMap.clear()
            targetMap.putAll(meters)

            val outJson = gson.toJson(saveData)
            dataFile.writeText(outJson)
            modMessage("§d[RNG Debug] $storageKey persistData: wrote ${meters.size} entries, json=${outJson.take(200)}")
        }
    }

    fun checkDataExists(key: String) {
        meters.getOrPut(key) {
            RngMeterUserData()
        }
    }

    fun setScore(key: String, score: Int) {
        checkDataExists(key)
        meters[key]?.score = score
        modMessage("§d[RNG Debug] $storageKey setScore: key=$key score=$score")
        persistData()
        postScoreUpdate(key)
    }

    fun addScore(key: String, delta: Int) {
        checkDataExists(key)
        meters[key]?.let {
            val before = it.score
            it.score += delta
            modMessage("§d[RNG Debug] $storageKey addScore: key=$key delta=$delta before=$before after=${it.score}")
        }
        persistData()
        postScoreUpdate(key)
    }

    fun setItem(key: String, item: String?) {
        checkDataExists(key)
        val data = meters[key] ?: return
        if (item.isNullOrEmpty() || item == "null") {
            modMessage("§d[RNG Debug] $storageKey setItem: key=$key item=null (clear)")
            data.item = null
            data.needed = null
        } else {
            data.item = item
            data.needed = RngMeterValues.getNeeded(key, item)
            modMessage("§d[RNG Debug] $storageKey setItem: key=$key item=$item needed=${data.needed}")
        }
        persistData()
    }

    fun getMeterPercentage(key: String): Double {
        val data = meters[key] ?: return 0.0
        val needed = data.needed ?: return 0.0
        if (needed <= 0) return 0.0
        return (data.score.toDouble() / needed * 100.0).coerceAtMost(100.0)
    }

    fun getCurrentScore(key: String): Int = meters[key]?.score ?: 0
    fun getCurrentNeeded(key: String): Int? = meters[key]?.needed
    fun getCurrentItem(key: String): String? = meters[key]?.item

    private fun postScoreUpdate(key: String) {
        val data = meters[key] ?: return
        val type = if (storageKey == "dungeon") MeterType.DUNGEON else MeterType.SLAYER
        RngEvent.ScoreUpdate(type, key, data.item, data.score, data.needed).postAndCatch()
    }
}
