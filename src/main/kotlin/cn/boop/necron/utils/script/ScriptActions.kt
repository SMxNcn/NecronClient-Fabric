package cn.boop.necron.utils.script

import cn.boop.necron.utils.clickInventorySlot
import com.odtheking.odin.utils.sendCommand
import net.minecraft.client.Minecraft

interface ScriptAction {
    suspend fun execute(mc: Minecraft)
}

class ClickSlotAction(private val slot: Int, private val rightClick: Boolean) : ScriptAction {
    override suspend fun execute(mc: Minecraft) {
        val id = mc.player?.containerMenu?.containerId ?: return
        clickInventorySlot(slot, id, rightClick)
    }
}

class SendCommandAction(private val command: String) : ScriptAction {
    override suspend fun execute(mc: Minecraft) {
        sendCommand(command)
    }
}

class UseKeyAction(private val keyCode: Int) : ScriptAction {
    override suspend fun execute(mc: Minecraft) {
        TODO()
    }
}

class DelayAction(private val duration: Long) : ScriptAction {
    override suspend fun execute(mc: Minecraft) {
        if (duration > 0) kotlinx.coroutines.delay(duration)
    }
}

class SendClientAction(private val message: String) : ScriptAction {
    override suspend fun execute(mc: Minecraft) {
        TODO()
    }
}