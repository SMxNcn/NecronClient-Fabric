package cn.boop.necron

import cn.boop.necron.commands.*
import cn.boop.necron.events.CustomEventDispatcher
import cn.boop.necron.features.impl.necron.*
import cn.boop.necron.utils.EquipmentUtils
import cn.boop.necron.utils.WardrobeUtils
import cn.boop.necron.utils.network.MayorData
import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.ModuleManager
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.loader.api.FabricLoader
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File

object Necron : ClientModInitializer {
    val logger: Logger = LogManager.getLogger(Necron.javaClass)
    val config = ModuleConfig("necron.json")
    val configDir : File = FabricLoader.getInstance().configDir.toFile()

    override fun onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            arrayOf(autoSellCommand, necronChatCommand, necronCommand, nwpCommand).forEach { commodore -> commodore.register(dispatcher) }
        }

        listOf(this, CustomEventDispatcher, EquipmentUtils, WardrobeUtils).forEach { EventBus.subscribe(it) }

        MayorData.fetchData()
        ModuleManager.registerModules(config,
            Auto4, AutoClicker, AutoCloseChest, AutoExperiments,
            AutoFish, AutoGFS, AutoLeap, AutoSell, AutoTerms, AutoSwap,
            B64Chat, DungeonESP, Etherwarp, FuckDiorite, FarmingHelper,
            HurtCamera, ItemStarDisplay, Nametags, RelicHelper, RerollProtector,
            TitleManager
        )
    }
}
