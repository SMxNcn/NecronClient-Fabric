package cn.boop.necron.utils.rng

import cn.boop.necron.features.impl.necron.RngMeterHUD
import cn.boop.necron.utils.network.MayorData.aatroxMultiplier
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.odtheking.odin.OdinMod
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CompletableFuture

object RngMeterValues {
    private const val REMOTE_URL = "https://nckim.top/api/rng-meter"

    @Volatile
    private var values: Map<String, Map<String, Int>> = emptyMap()

    /** 从远程服务器异步加载 RNG 表到内存 */
    private fun fetchRemote() {
        CompletableFuture.runAsync {
            try {
                val url = URI.create(REMOTE_URL).toURL()
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                connection.disconnect()

                val type = object : TypeToken<Map<String, Map<String, Int>>>() {}.type
                val remoteData: Map<String, Map<String, Int>> = Gson().fromJson(response, type) ?: emptyMap()
                if (remoteData.isNotEmpty()) {
                    values = remoteData
                    OdinMod.logger.info("Successfully fetched RNG meter data from remote (${remoteData.size} categories)")
                }
            } catch (e: Exception) {
                OdinMod.logger.error("Failed to fetch RNG meter data from remote, meter lookups will be unavailable", e)
            }
        }
    }

    init {
        fetchRemote()
    }

    fun getNeeded(key: String, item: String): Int? = values[key]?.get(item)
}

val RNG_RESET_PATTERN = Regex("§d§lRNG METER! §aReselected the (.+?) §afor §c(.+?)§a! §e§lCLICK HERE §r§ato select a new drop!")

val FLOOR_KEY_PATTERN = Regex("""Catacombs \((\w{1,2})\)""")

val daemonMultiplier: Double
    get() = if (RngMeterHUD.hasDaemon) 1.0 + (RngMeterHUD.daemonLevel * 0.01) else 1.0

fun getSlayerXPMultiplier(): Double = aatroxMultiplier * daemonMultiplier

fun generateMeterBar(score: Int, needed: Int): String {
    val percentage = if (needed <= 0) 0.0 else (score.toDouble() / needed).coerceAtMost(1.0)
    val progress = (percentage * 15).toInt().coerceIn(0, 15)
    return "§d${"%,d".format(score)} §a§m§l${" ".repeat(progress)}§7§m§l${" ".repeat(15 - progress)}§r §d${"%,d".format(needed)}"
}
