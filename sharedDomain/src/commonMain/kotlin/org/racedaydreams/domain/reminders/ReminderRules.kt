package org.racedaydreams.domain.reminders

import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.coaching.coachingReadOnlyList
import org.racedaydreams.domain.coaching.performanceDecimal
import org.racedaydreams.domain.time.*

/** Typed wall-clock facts, independent of Android/Apple notification and time-zone APIs. */
data class ReminderSettings(
    val mode: Mode,
    val eveningNanoOfDay: Long,
    val quietHours: QuietWindow?,
    val zoneId: String,
) {
    enum class Mode {
        DISABLED,
        ALL,
        LONG_HARD_ONLY,
    }

    init {
        require(eveningNanoOfDay in 0 until NANOS_PER_DAY)
        require(zoneId.isNotEmpty())
    }

    companion object {
        const val NANOS_PER_DAY = 86400000000000L

        fun defaults(zoneId: String) =
            ReminderSettings(
                Mode.DISABLED,
                19 * 3600 * 1000000000L,
                QuietWindow(22 * 3600 * 1000000000L, 7 * 3600 * 1000000000L),
                zoneId,
            )
    }
}

data class QuietWindow(val start: Long, val end: Long) {
    init {
        require(
            start in 0 until ReminderSettings.NANOS_PER_DAY &&
                end in 0 until ReminderSettings.NANOS_PER_DAY
        )
        require(start != end) { "Quiet hours need different start and end times." }
    }

    fun contains(nanoOfDay: Long): Boolean =
        if (start < end) nanoOfDay >= start && nanoOfDay < end
        else nanoOfDay >= start || nanoOfDay < end
}

/** Resolution must shift gaps forward and select the earlier occurrence of an overlap. */
interface ReminderCalendar {
    fun resolve(date: LocalDate, nanoOfDay: Long, zoneId: String): LocalReminderTime

    fun at(instant: Instant, zoneId: String): LocalReminderTime
}

data class LocalReminderTime(val instant: Instant, val date: LocalDate, val nanoOfDay: Long) {
    init {
        require(nanoOfDay in 0 until ReminderSettings.NANOS_PER_DAY)
    }
}

/** The original opt-in schedule and delivery rules. No alarm, permission request or plan edit. */
object ReminderRules {
    fun schedule(
        entries: List<ReminderPlanEntry>,
        preferences: ReminderSettings?,
        clock: () -> Instant,
        notificationPermission: Boolean,
        calendar: ReminderCalendar,
    ): List<EveningReminder> {
        if (!notificationPermission || preferences!!.mode == ReminderSettings.Mode.DISABLED)
            return emptyList()
        val reminders = mutableListOf<EveningReminder>()
        for (entry in entries) {
            val reminder = candidate(entry, preferences, calendar)
            if (reminder != null && reminder.triggerAt().isAfter(clock())) reminders.add(reminder)
        }
        return coachingReadOnlyList(reminders)
    }

    fun candidate(
        entry: ReminderPlanEntry?,
        preferences: ReminderSettings?,
        calendar: ReminderCalendar,
    ): EveningReminder? {
        if (entry == null) throw NullPointerException("planned workout")
        if (preferences == null) throw NullPointerException("preferences")
        if (
            entry.status() != ReminderPlanEntry.Status.PLANNED ||
                preferences.mode == ReminderSettings.Mode.DISABLED ||
                preferences.mode == ReminderSettings.Mode.LONG_HARD_ONLY &&
                    !longOrHard(entry.workout().category())
        )
            return null
        val local =
            calendar.resolve(
                entry.workout().date().minusDays(1),
                preferences.eveningNanoOfDay,
                preferences.zoneId,
            )
        if (preferences.quietHours?.contains(local.nanoOfDay) == true) return null
        val label =
            when (entry.workout().category()) {
                WorkoutCategory.RECOVERY_RUN -> "Recovery run"
                WorkoutCategory.EASY_RUN -> "Easy run"
                WorkoutCategory.ZONE_2_RUN -> "Zone 2 run"
                WorkoutCategory.STEADY_RUN -> "Steady run"
                WorkoutCategory.LONG_RUN -> "Long run"
                WorkoutCategory.PROGRESSION_RUN -> "Progression run"
                WorkoutCategory.TEMPO_THRESHOLD_RUN -> "Tempo run"
                WorkoutCategory.INTERVALS -> "Intervals"
                WorkoutCategory.RACE_PACE_RUN -> "Race-pace run"
                WorkoutCategory.TIME_TRIAL -> "Time trial"
                WorkoutCategory.RACE -> "Race"
                WorkoutCategory.CUSTOM_RUN -> "Custom run"
            }
        val prescription =
            entry.workout().distanceKm()?.let { performanceDecimal(it) + " km" } ?: "Your workout"
        return EveningReminder(
            entry.workout().id(),
            entry.revision(),
            entry.workout().date(),
            local.instant,
            label + " tomorrow",
            prescription +
                " is planned tomorrow. Hydrate this evening, eat normally, prepare your kit and aim for adequate sleep.",
        )
    }

    fun canDeliver(
        scheduled: EveningReminder?,
        current: ReminderPlanEntry?,
        preferences: ReminderSettings?,
        clock: () -> Instant,
        notificationPermission: Boolean,
        calendar: ReminderCalendar,
    ): Boolean {
        if (!notificationPermission || current == null) return false
        val expected = candidate(current, preferences, calendar)
        if (expected == null || expected != scheduled) return false
        val now = clock()
        val expiry =
            Instants.ofEpochSecond(
                checkedAdd(scheduled!!.triggerAt().getEpochSecond(), 7200),
                scheduled.triggerAt().getNano().toLong(),
            )
        if (now.isBefore(scheduled.triggerAt()) || !now.isBefore(expiry)) return false
        val local = calendar.at(now, preferences!!.zoneId)
        return local.date == scheduled.workoutDate().minusDays(1) &&
            preferences.quietHours?.contains(local.nanoOfDay) != true
    }

    private fun longOrHard(category: WorkoutCategory): Boolean =
        category in
            listOf(
                WorkoutCategory.LONG_RUN,
                WorkoutCategory.INTERVALS,
                WorkoutCategory.TEMPO_THRESHOLD_RUN,
                WorkoutCategory.RACE_PACE_RUN,
                WorkoutCategory.TIME_TRIAL,
                WorkoutCategory.RACE,
                WorkoutCategory.PROGRESSION_RUN,
            )
}
