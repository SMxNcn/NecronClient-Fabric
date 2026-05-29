package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import cn.boop.necron.utils.randomDelay
import cn.boop.necron.utils.rightClick
import com.odtheking.odin.OdinMod
import com.odtheking.odin.events.InputEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.isEtherwarpItem
import com.odtheking.odin.utils.skyblock.LocationUtils.isInSkyblock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Etherwarp : Module(
    name = "Left Etherwarp",
    description = "Left click etherwarp.",
    category = NCategory.NECRON
) {
    private var lastLeftClickTime: Long = 0

    init {
        on<InputEvent> {
            if (key.value != 0) return@on

            val mainHandItem = mc.player!!.mainHandItem
            val canEtherwarp = mainHandItem.isEtherwarpItem()
            if (canEtherwarp != null && canEtherwarp.contains("ethermerge")) {
                useEtherwarp()
                cancel()
            }
        }
    }

    private fun useEtherwarp() {
        if (!isInSkyblock || mc.player == null || mc.screen != null) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastLeftClickTime < 150) return

        lastLeftClickTime = currentTime

        OdinMod.scope.launch {
            mc.options.keyShift.isDown = true
            delay(randomDelay(100, 50))
            rightClick()
            delay(randomDelay(50, 25))
            mc.options.keyShift.isDown = false
        }
    }
}
