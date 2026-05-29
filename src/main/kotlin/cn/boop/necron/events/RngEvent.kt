package cn.boop.necron.events

import cn.boop.necron.utils.rng.MeterType
import com.odtheking.odin.events.core.Event

abstract class RngEvent : Event {
    class ScoreUpdate(
        val type: MeterType,
        val key: String,
        val item: String?,
        val score: Int,
        val needed: Int?
    ) : RngEvent()

    class ItemSelected(
        val type: MeterType,
        val key: String,
        val item: String
    ) : RngEvent()

    class MeterReset(
        val type: MeterType,
        val key: String,
        val item: String?,
        val score: Int,
        val needed: Int?
    ) : RngEvent()
}
