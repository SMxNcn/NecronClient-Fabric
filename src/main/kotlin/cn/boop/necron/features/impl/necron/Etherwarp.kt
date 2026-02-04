package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import cn.boop.necron.utils.rightClick
import com.odtheking.odin.events.InputEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.isEtherwarpItem
import com.odtheking.odin.utils.skyblock.LocationUtils.isInSkyblock
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

object Etherwarp : Module(
    name = "Left Etherwarp",
    description = "Left click etherwarp.",
    category = NCategory.NECRON
) {
    var lastLeftClickTime: Long = 0
    val executor: ExecutorService = Executors.newFixedThreadPool(1, object : ThreadFactory {
        private val counter = AtomicInteger(0)

        override fun newThread(r: Runnable): Thread {
            val thread = Thread(r, "Etherwarp-" + counter.getAndIncrement())
            thread.isDaemon = true
            return thread
        }
    })

    init {
        on<InputEvent>{
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

        executor.submit {
            try {
                lastLeftClickTime = System.currentTimeMillis()
                mc.options.keyShift.isDown = true
                Thread.sleep(100)
                rightClick()
                Thread.sleep(50)
                mc.options.keyShift.isDown = false
            } catch (_: Exception) {}
        }
    }
}
