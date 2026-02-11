package cn.boop.necron

import cn.boop.necron.commands.necronChatCommand
import cn.boop.necron.features.impl.necron.AutoClicker
import cn.boop.necron.features.impl.necron.AutoCloseChest
import cn.boop.necron.features.impl.necron.AutoGFS
import cn.boop.necron.features.impl.necron.AutoLeap
import cn.boop.necron.features.impl.necron.AutoSwap
import cn.boop.necron.features.impl.necron.B64Chat
import cn.boop.necron.features.impl.necron.DungeonESP
import cn.boop.necron.features.impl.necron.Etherwarp
import cn.boop.necron.features.impl.necron.FuckDiorite
import cn.boop.necron.features.impl.necron.ItemStarDisplay
import cn.boop.necron.features.impl.necron.RerollProtector
import cn.boop.necron.features.impl.necron.Nametags
import cn.boop.necron.features.impl.necron.TitleManager
import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.ModuleManager
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

object Necron : ClientModInitializer {
    override fun onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            arrayOf(necronChatCommand).forEach { commodore -> commodore.register(dispatcher) }
        }

        listOf(this).forEach { EventBus.subscribe(it) }

        ModuleManager.registerModules(ModuleConfig("necron.json"),
            AutoClicker, AutoCloseChest, AutoGFS, AutoLeap, AutoSwap, B64Chat, DungeonESP, Etherwarp, FuckDiorite, ItemStarDisplay,
            Nametags, RerollProtector, TitleManager)

        ClientTickEvents.START_CLIENT_TICK.register { _ ->
            if (TitleManager.enabled) {
                TitleManager.updateTitle()
            }
        }
    }
}
