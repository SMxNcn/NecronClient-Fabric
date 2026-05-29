package cn.boop.necron.utils.waypoints

import com.google.gson.annotations.SerializedName
import net.minecraft.core.BlockPos

data class IslandWaypoints(
    @SerializedName("name") val name: String,
    @SerializedName("x") val x: Int,
    @SerializedName("y") val y: Int,
    @SerializedName("z") val z: Int,
    @SerializedName("type") val type: NodeType,
    @SerializedName("island") val island: String = ""
) {
    fun toBlockPos(): BlockPos = BlockPos(x, y, z)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IslandWaypoints) return false
        return x == other.x && y == other.y && z == other.z && island == other.island
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        result = 31 * result + island.hashCode()
        return result
    }
}

enum class NodeType(val displayName: String) {
    @SerializedName("WALK") WALK("Walk"),
    @SerializedName("TP") TP("Teleport"),
    @SerializedName("WARP") WARP("Warp")
}
