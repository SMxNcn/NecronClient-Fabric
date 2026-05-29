package cn.boop.necron.utils.path

import com.google.gson.annotations.SerializedName
import net.minecraft.core.BlockPos

data class IslandWaypoint(
    @SerializedName("x") val x: Int,
    @SerializedName("y") val y: Int,
    @SerializedName("z") val z: Int,
    @SerializedName("type") val type: NodeType,
    @SerializedName("name") val name: String = ""
) {
    fun toBlockPos(): BlockPos = BlockPos(x, y, z)

    companion object {
        fun fromBlockPos(pos: BlockPos, type: NodeType, name: String = ""): IslandWaypoint {
            return IslandWaypoint(pos.x, pos.y, pos.z, type, name)
        }
    }
}

enum class NodeType(val displayName: String) {
    @SerializedName("WALK") WALK("Walk"),
    @SerializedName("TP") TP("Teleport Item"),
    @SerializedName("WARP") WARP("Warp Command")
}
