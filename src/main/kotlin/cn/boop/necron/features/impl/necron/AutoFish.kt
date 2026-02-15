package cn.boop.necron.features.impl.necron

import cn.boop.necron.utils.NCategory
import cn.boop.necron.utils.removeFormatting
import cn.boop.necron.utils.rightClick
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
// import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.containsOneOf
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.modMessage
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.FishingRodItem
import net.minecraft.world.item.ItemStack

object AutoFish : Module(
    name = "Auto Fish",
    description = "Automatically reels in and casts fishing rods.",
    category = NCategory.NECRON
) {
    private val autoKill by BooleanSetting("Auto kill", false, desc = "Automatically kill sea creatures. (does NOT include rare mobs)")
    private val killWeapon by SelectorSetting("Kill Weapon", "Flaming Flay", listOf("Hyperion", "Soul Whip", "Flaming Flay"), desc = "Weapon used for auto kill.")
    private val rethrowDelay by NumberSetting("Rethrow Delay", 500, 100, 500, 10, desc = "Delay before rethrowing fishing hook.", unit = "ms")
    private val waitTime by NumberSetting("Max Wait Time", 20, 20, 60, desc = "Maximum time to wait for bite before rethrowing.", unit = "s")

    enum class FishingState() {
        IDLE, THROW, WAIT, CAST, KILL
    }

    private var currentState = FishingState.IDLE
    private var fishBitten = false
    // private var hasRareMob = false // TD: Auto disable Auto Fish when rare creature appeared.
    private var throwTick = 0L
    private var waitStartTick = 0L
    private var killStartTime = 0L
    private var hookUpTick = 0L

    init {
        on<TickEvent.Start> {
            handleFishing()
        }

//        on<ChatPacketEvent> {
//
//        }
    }

    private fun handleFishing() {
        val player = mc.player ?: return
        val world = mc.level ?: return
        val currentTime = world.gameTime

        if (!isValidRod(player.mainHandItem)) {
            currentState = FishingState.IDLE
            return
        }

        when (currentState) {
            FishingState.IDLE -> {
                currentState = FishingState.THROW
                println(currentState.name)
            }
            FishingState.THROW -> {
                if (player.fishing == null) {
                    rightClick()
                    throwTick = currentTime
                    currentState = FishingState.WAIT
                    waitStartTick = currentTime
                }
            }
            FishingState.WAIT -> {
                val hook = player.fishing
                if (hook != null) {
                    if (currentTime - waitStartTick >= waitTime * 20) {
                        rightClick()
                        modMessage("Max wait time reached! Reset. §c(${waitTime}s)")
                        currentState = FishingState.IDLE
                    }
                    checkHookArmorStand()
                    if (fishBitten) currentState = FishingState.CAST
                }
            }
            FishingState.CAST -> {
                if (hookUpTick == 0L) {
                    rightClick()
                    hookUpTick = currentTime
                }

                if (currentTime - hookUpTick > rethrowDelay / 50) {
//                        if (autoKill) {
//                            fishBitten = false
//                            killStartTime = currentTime
//                            currentState = FishingState.KILL
//                        } else {
                    fishBitten = false
                    currentState = FishingState.THROW
                    hookUpTick = 0L
//                        }
                }
            }

            FishingState.KILL -> {
                val rodSlot = mc.player?.inventory?.selectedSlot!!
                val weaponSlot = when (killWeapon) {
                    0 -> findHotbarWeapon("HYPERION")
                    1 -> findHotbarWeapon("SOUL_WHIP")
                    else -> findHotbarWeapon("FLAMING_FLAY")
                }

                if (killStartTime - currentTime == 10L && weaponSlot != -1) {
                    mc.player?.inventory?.selectedSlot = weaponSlot
                    schedule(2) { // Adjust delay as needed for attack timing
                        rightClick() // Attack with weapon
                    }
                    modMessage("Attacking with weapon!") // Add feedback
                }

                if (killStartTime - currentTime > rethrowDelay / 100) {
                    if (rodSlot in 0..8) mc.player?.inventory?.selectedSlot = rodSlot
                    currentState = FishingState.THROW
                    hookUpTick = 0L
                    killStartTime = 0L
                }
            }
        }
    }

    private fun checkHookArmorStand() {
        val hook = mc.player?.fishing ?: return
        val armorStand = mc.level?.getEntitiesOfClass(
            ArmorStand::class.java, hook.boundingBox.inflate(1.0), { entity: ArmorStand ->
                entity.isInvisible && entity.hasCustomName() && entity.customName?.string?.removeFormatting()?.contains("!!!") == true
            }
        )?.firstOrNull()

        armorStand?.let { fishBitten = true }
    }

    private fun isValidRod(item: ItemStack): Boolean {
        return item.item is FishingRodItem && !item.itemId.containsOneOf("FLAY", "WHIP")
    }

    private fun findHotbarWeapon(targetID: String): Int {
        val inventory = mc.player?.inventory ?: return -1
        for (slotIndex in 0..8) {
            val itemStack = inventory.getItem(slotIndex)
            if (!itemStack.isEmpty && itemStack.itemId.equals(targetID, ignoreCase = true)) return slotIndex
        }
        return -1
    }

    private fun reset() {
        currentState = FishingState.IDLE
        fishBitten = false
        throwTick = 0L
        waitStartTick = 0L
        killStartTime = 0L
        hookUpTick = 0L
    }

    override fun onEnable() {
        super.onEnable()
        reset()
    }

    override fun onDisable() {
        super.onDisable()
        if (mc.player?.fishing != null) rightClick()
        reset()
    }
}