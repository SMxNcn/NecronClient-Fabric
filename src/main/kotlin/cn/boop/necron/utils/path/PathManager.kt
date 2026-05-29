package cn.boop.necron.utils.path

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.core.BlockPos
import java.io.File
import java.io.FileReader
import java.io.FileWriter

data class WaypointData(
    @SerializedName("island") val island: String,
    @SerializedName("waypoints") val waypoints: List<IslandWaypoint>
)

object PathManager {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configDir = File("config/necron")
    private val pathFile = File(configDir, "nc-island.json")

    private var isLoaded = false
    val waypoints: MutableMap<String, MutableList<IslandWaypoint>> = mutableMapOf()

    fun ensureLoaded() {
        if (!isLoaded) {
            loadPaths()
            isLoaded = true
        }
    }

    fun loadPaths() {
        if (!pathFile.exists()) {
            waypoints.clear()
            return
        }

        try {
            val reader = FileReader(pathFile)
            val data: List<WaypointData> = gson.fromJson(reader, Array<WaypointData>::class.java).toList()
            reader.close()

            waypoints.clear()
            for (entry in data) {
                waypoints[entry.island] = entry.waypoints.toMutableList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun savePaths() {
        // 在 save 时检测/创建目录和文件
        if (!configDir.exists()) {
            configDir.mkdirs()
        }

        val data = waypoints.map { (islandName, waypointList) ->
            WaypointData(islandName, waypointList)
        }.toList()

        val writer = FileWriter(pathFile)
        gson.toJson(data, writer)
        writer.close()
    }

    fun addWaypoint(island: Island, waypoint: IslandWaypoint) {
        ensureLoaded()
        val islandName = island.name
        if (!waypoints.containsKey(islandName)) {
            waypoints[islandName] = mutableListOf()
        }
        waypoints[islandName]!!.add(waypoint)
        savePaths()
    }

    fun removeWaypoint(island: Island, waypoint: IslandWaypoint): Boolean {
        ensureLoaded()
        val list = waypoints[island.name] ?: return false
        if (list.remove(waypoint)) {
            savePaths()
            return true
        }
        return false
    }

    fun removeWaypointAt(island: Island, pos: BlockPos): Boolean {
        ensureLoaded()
        val list = waypoints[island.name] ?: return false
        val index = list.indexOfFirst { it.x == pos.x && it.y == pos.y && it.z == pos.z }
        if (index >= 0) {
            list.removeAt(index)
            savePaths()
            return true
        }
        return false
    }

    fun clearPath(island: Island) {
        ensureLoaded()
        waypoints[island.name] = mutableListOf()
        savePaths()
    }

    fun getWaypoints(island: Island): List<IslandWaypoint> {
        ensureLoaded()
        return waypoints[island.name] ?: emptyList()
    }

    fun getWaypointAt(island: Island, pos: BlockPos): IslandWaypoint? {
        ensureLoaded()
        val list = waypoints[island.name] ?: return null
        return list.find { it.x == pos.x && it.y == pos.y && it.z == pos.z }
    }

    fun getCurrentIslandWaypoints(): List<IslandWaypoint> {
        ensureLoaded()
        return getWaypoints(LocationUtils.currentArea)
    }
}