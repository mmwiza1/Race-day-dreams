package org.racedaydreams.domain.reminders

import java.time.LocalTime
import java.time.ZoneId
import kotlin.jvm.JvmStatic

/** Original JVM time/zone preference API. Decision rules and quiet-window logic are shared. */
data class ReminderPreferences(
    private val mode: Mode?,
    private val eveningTime: LocalTime?,
    private val quietHours: QuietHours?,
    private val zone: ZoneId?,
) {
    enum class Mode {
        DISABLED,
        ALL,
        LONG_HARD_ONLY,
    }

    data class QuietHours(private val start: LocalTime?, private val end: LocalTime?) {
        private val window: QuietWindow

        init {
            if (start == null) throw NullPointerException("quiet-hours start")
            if (end == null) throw NullPointerException("quiet-hours end")
            window = QuietWindow(start.toNanoOfDay(), end.toNanoOfDay())
        }

        fun start(): LocalTime = start!!

        fun end(): LocalTime = end!!

        fun contains(time: LocalTime): Boolean = window.contains(time.toNanoOfDay())

        internal fun shared(): QuietWindow = window

        override fun toString(): String = "QuietHours[start=$start, end=$end]"
    }

    init {
        if (mode == null) throw NullPointerException("reminder mode")
        if (eveningTime == null) throw NullPointerException("reminder time")
        if (zone == null) throw NullPointerException("local time zone")
    }

    fun mode(): Mode = mode!!

    fun eveningTime(): LocalTime = eveningTime!!

    fun quietHours(): QuietHours? = quietHours

    fun zone(): ZoneId = zone!!

    fun shared(): ReminderSettings =
        ReminderSettings(
            ReminderSettings.Mode.valueOf(mode!!.name),
            eveningTime!!.toNanoOfDay(),
            quietHours?.shared(),
            zone!!.id,
        )

    override fun toString(): String =
        "ReminderPreferences[mode=$mode, eveningTime=$eveningTime, quietHours=$quietHours, zone=$zone]"

    companion object {
        /** Platform boundary only; no scheduling decisions or time-zone substitution. */
        @JvmStatic
        fun fromShared(value: ReminderSettings): ReminderPreferences =
            ReminderPreferences(
                Mode.valueOf(value.mode.name),
                LocalTime.ofNanoOfDay(value.eveningNanoOfDay),
                value.quietHours?.let {
                    QuietHours(LocalTime.ofNanoOfDay(it.start), LocalTime.ofNanoOfDay(it.end))
                },
                ZoneId.of(value.zoneId),
            )

        @JvmStatic
        fun defaults(zone: ZoneId?): ReminderPreferences {
            if (zone == null) throw NullPointerException("local time zone")
            val value = ReminderSettings.defaults(zone.id)
            return ReminderPreferences(
                Mode.valueOf(value.mode.name),
                LocalTime.ofNanoOfDay(value.eveningNanoOfDay),
                value.quietHours?.let {
                    QuietHours(LocalTime.ofNanoOfDay(it.start), LocalTime.ofNanoOfDay(it.end))
                },
                zone,
            )
        }
    }
}
