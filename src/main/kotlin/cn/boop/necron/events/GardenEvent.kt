package cn.boop.necron.events

import com.odtheking.odin.events.core.Event

abstract class GardenEvent : Event {
    class PestReady : GardenEvent()

    class PestSpawned(val plot: Int) : GardenEvent()

    class PestKilled : GardenEvent()

    class GuestVisit(val player: String) : GardenEvent()

    class FailSafe(val reason: String) : GardenEvent()
}