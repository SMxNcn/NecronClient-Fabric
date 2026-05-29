package cn.boop.necron.utils.waypoints

import cn.boop.necron.Necron.configDir
import cn.boop.necron.utils.modMessage
import com.google.common.reflect.TypeToken
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.odtheking.odin.OdinMod
import com.odtheking.odin.utils.skyblock.Island
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.core.BlockPos
import java.io.File
import java.io.FileReader
import java.io.FileWriter

data class IslandData(
    @SerializedName("island") val island: String,
    @SerializedName("waypoints") val waypoints: List<IslandWaypoints>
)

object PathManager {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val islandFile = File(configDir, "necron/nc-island.json")

    val waypoints: MutableMap<String, MutableList<IslandWaypoints>> = mutableMapOf()
    private var isLoaded = false

    fun ensureLoaded() {
        if (!isLoaded) {
            loadPaths()
            isLoaded = true
        }
    }

    fun loadPaths() {
        OdinMod.scope.launch(Dispatchers.IO) {
            if (!islandFile.exists()) {
                configDir.mkdirs()
                islandFile.createNewFile()
                waypoints.clear()
                modMessage("§eCreated empty waypoint file: §bwaypoints.json")
                return@launch
            }

            try {
                val type = object : TypeToken<List<IslandData>>() {}.type
                val loaded: List<IslandData> = gson.fromJson(FileReader(islandFile), type)

                waypoints.clear()
                for (entry in loaded) {
                    waypoints[entry.island] = entry.waypoints.toMutableList()
                }

                val totalCount = waypoints.values.sumOf { it.size }
                modMessage("§aLoaded $totalCount waypoints from §bwaypoints.json")
            } catch (e: Exception) {
                modMessage("§cFailed to load waypoints: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun savePaths() {
        OdinMod.scope.launch(Dispatchers.IO) {
            configDir.mkdirs()

            val data = waypoints.map { (islandName, waypointList) ->
                IslandData(islandName, waypointList)
            }.toList()

            try {
                FileWriter(islandFile).use { gson.toJson(data, it) }
            } catch (e: Exception) {
                modMessage("§cFailed to save waypoints: ${e.message}")
            }
        }
    }

    fun addWaypoint(island: Island, name: String, pos: BlockPos, type: NodeType): Boolean {
        ensureLoaded()
        val islandName = island.name

        // 检查该位置是否已存在路径点
        if (getWaypointAt(island, pos) != null) {
            modMessage("§cWaypoint already exists at this position!")
            return false
        }

        // 检查该名称是否已存在（同岛屿）
        if (getWaypointByName(island, name) != null) {
            modMessage("§cWaypoint name '$name' already exists on this island!")
            return false
        }

        if (!waypoints.containsKey(islandName)) {
            waypoints[islandName] = mutableListOf()
        }

        val waypoint = IslandWaypoints(name, pos.x, pos.y, pos.z, type, islandName)
        waypoints[islandName]!!.add(waypoint)
        savePaths()

        modMessage("§aAdded §f${type.displayName} §awaypoint '§f$name§a' at §e${pos.x}, ${pos.y}, ${pos.z}")
        return true
    }

    fun removeWaypoint(island: Island, pos: BlockPos): Boolean {
        ensureLoaded()
        val list = waypoints[island.name] ?: return false
        val index = list.indexOfFirst { it.x == pos.x && it.y == pos.y && it.z == pos.z }

        return if (index >= 0) {
            val removed = list.removeAt(index)
            savePaths()
            modMessage("§cRemoved waypoint '§f${removed.name}§c' at §e${pos.x}, ${pos.y}, ${pos.z}")
            true
        } else false
    }

    fun removeWaypointByName(island: Island, name: String): Boolean {
        ensureLoaded()
        val list = waypoints[island.name] ?: return false
        val index = list.indexOfFirst { it.name.equals(name, ignoreCase = true) }

        return if (index >= 0) {
            val removed = list.removeAt(index)
            savePaths()
            modMessage("§cRemoved waypoint '§f${removed.name}§c'")
            true
        } else false
    }

    fun updateWaypoint(island: Island, pos: BlockPos, newName: String?, newType: NodeType?): Boolean {
        ensureLoaded()
        val list = waypoints[island.name] ?: return false
        val index = list.indexOfFirst { it.x == pos.x && it.y == pos.y && it.z == pos.z }

        return if (index >= 0) {
            val old = list[index]
            val newWaypoint = IslandWaypoints(newName ?: old.name, old.x,old.y,old.z, newType ?: old.type, old.island)
            list[index] = newWaypoint
            savePaths()
            modMessage("§eUpdated waypoint at §b${pos.x}, ${pos.y}, ${pos.z}")
            true
        } else false
    }

    fun clearPath(island: Island) {
        ensureLoaded()
        waypoints[island.name] = mutableListOf()
        savePaths()
        modMessage("§cCleared all waypoints for §f${island.displayName}")
    }

    fun getWaypoints(island: Island): List<IslandWaypoints> {
        ensureLoaded()
        return waypoints[island.name] ?: emptyList()
    }

    fun getWaypointAt(island: Island, pos: BlockPos): IslandWaypoints? {
        ensureLoaded()
        return waypoints[island.name]?.find { it.x == pos.x && it.y == pos.y && it.z == pos.z }
    }

    fun getWaypointByName(island: Island, name: String): IslandWaypoints? {
        ensureLoaded()
        return waypoints[island.name]?.find { it.name.equals(name, ignoreCase = true) }
    }

    fun getAllWaypoints(): List<IslandWaypoints> {
        ensureLoaded()
        return waypoints.values.flatten()
    }
}