package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.reminders.*
import org.racedaydreams.domain.time.*

class ReminderRulesContractTest {
    private val day = CalendarDates.of(2030, 4, 10)
    private val trigger = Instants.ofEpochSecond(day.minusDays(1).toEpochDay() * 86400 + 19 * 3600)
    private val settings =
        ReminderSettings(ReminderSettings.Mode.ALL, 19 * 3600 * 1000000000L, null, "UTC")
    private val utc =
        object : ReminderCalendar {
            override fun resolve(date: LocalDate, nanoOfDay: Long, zoneId: String) =
                LocalReminderTime(
                    Instants.ofEpochSecond(
                        date.toEpochDay() * 86400 + nanoOfDay / 1000000000,
                        nanoOfDay % 1000000000,
                    ),
                    date,
                    nanoOfDay,
                )

            override fun at(instant: Instant, zoneId: String) =
                LocalReminderTime(
                    instant,
                    CalendarDates.ofEpochDay(instant.getEpochSecond().floorDiv(86400)),
                    instant.getEpochSecond().mod(86400) * 1000000000L + instant.getNano(),
                )
        }

    private fun entry(
        id: String = "r",
        revision: Long = 1,
        status: ReminderPlanEntry.Status = ReminderPlanEntry.Status.PLANNED,
        category: WorkoutCategory = WorkoutCategory.EASY_RUN,
        distance: Double? = 5.0,
    ) =
        ReminderPlanEntry(
            PlannedWorkout(
                id,
                day,
                category,
                distance,
                Durations.ofMinutes(30),
                emptyList(),
                "Original",
            ),
            revision,
            status,
        )

    @Test
    fun reminderDefaultsAreDisabledAndPermissionNeverBecomesImplicit() {
        val defaults = ReminderSettings.defaults("UTC")
        assertEquals(ReminderSettings.Mode.DISABLED, defaults.mode)
        assertNull(ReminderRules.candidate(entry(), defaults, utc))
        assertTrue(
            ReminderRules.schedule(
                    listOf(entry()),
                    settings,
                    { error("No clock read without permission") },
                    false,
                    utc,
                )
                .isEmpty()
        )
    }

    @Test
    fun permissionOrMissingCurrentPrescriptionShortCircuitsBeforeCalendarAndClock() {
        assertFalse(ReminderRules.canDeliver(null, null, null, { error("No clock") }, false, utc))
        assertFalse(
            ReminderRules.canDeliver(null, null, settings, { error("No clock") }, true, utc)
        )
    }

    @Test
    fun versionStatusCategoryAndContentsAreRevalidatedAtDelivery() {
        val original = entry()
        val alarm = assertNotNull(ReminderRules.candidate(original, settings, utc))
        for (current in
            listOf(
                entry(revision = 2),
                entry(status = ReminderPlanEntry.Status.COMPLETED),
                entry(status = ReminderPlanEntry.Status.CANCELLED),
                entry(status = ReminderPlanEntry.Status.SKIPPED),
                entry(category = WorkoutCategory.LONG_RUN),
                entry(distance = 6.0),
            )) assertFalse(
            ReminderRules.canDeliver(
                alarm,
                current,
                settings,
                { error("Stale proposals need no clock") },
                true,
                utc,
            )
        )
        assertTrue(ReminderRules.canDeliver(alarm, original, settings, { trigger }, true, utc))
    }

    @Test
    fun exactDeliveryWindowIncludesTheTriggerButExcludesTwoHours() {
        val e = entry()
        val alarm = assertNotNull(ReminderRules.candidate(e, settings, utc))
        fun at(seconds: Long, nanos: Long = 0) =
            Instants.ofEpochSecond(trigger.getEpochSecond() + seconds, nanos)
        assertFalse(ReminderRules.canDeliver(alarm, e, settings, { at(-1, 999999999) }, true, utc))
        assertTrue(ReminderRules.canDeliver(alarm, e, settings, { trigger }, true, utc))
        assertTrue(ReminderRules.canDeliver(alarm, e, settings, { at(7199, 999999999) }, true, utc))
        assertFalse(ReminderRules.canDeliver(alarm, e, settings, { at(7200) }, true, utc))
    }

    @Test
    fun quietIntervalsKeepInclusiveStartsAndExclusiveEndsAcrossMidnight() {
        val night = QuietWindow(22 * 3600 * 1000000000L, 7 * 3600 * 1000000000L)
        assertTrue(night.contains(night.start))
        assertTrue(night.contains(0))
        assertFalse(night.contains(night.end))
        val daytime = QuietWindow(12 * 3600 * 1000000000L, 13 * 3600 * 1000000000L)
        assertTrue(daytime.contains(daytime.start))
        assertFalse(daytime.contains(daytime.end))
        assertFalse(daytime.contains(0))
        assertFailsWith<IllegalArgumentException> { QuietWindow(0, 0) }
        assertFailsWith<IllegalArgumentException> {
            ReminderSettings(ReminderSettings.Mode.ALL, ReminderSettings.NANOS_PER_DAY, null, "UTC")
        }
    }

    @Test
    fun longHardPreferenceIncludesOriginalCategoriesWithoutReclassifyingTheWorkout() {
        val selected = settings.copy(mode = ReminderSettings.Mode.LONG_HARD_ONLY)
        for (category in WorkoutCategory.entries) {
            val expected =
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
            assertEquals(
                expected,
                ReminderRules.candidate(entry(category = category), selected, utc) != null,
                category.name,
            )
        }
    }

    @Test
    fun scheduleRetainsOriginalOrderAndReadsOnlyTheClockForEligibleEntries() {
        var reads = 0
        val rows =
            ReminderRules.schedule(
                listOf(
                    entry("b"),
                    entry("a"),
                    entry("skipped", status = ReminderPlanEntry.Status.SKIPPED),
                ),
                settings,
                {
                    reads++
                    Instants.ofEpochSecond(trigger.getEpochSecond() - 1)
                },
                true,
                utc,
            )
        assertEquals(listOf("b", "a"), rows.map { it.workoutId() })
        assertEquals(2, reads)
        assertTrue(
            ReminderRules.schedule(listOf(entry()), settings, { trigger }, true, utc).isEmpty()
        )
    }

    @Test
    fun messagesUseTheActualPrescriptionAndDoNotInventMissingDistance() {
        val unknown = assertNotNull(ReminderRules.candidate(entry(distance = null), settings, utc))
        assertTrue(unknown.message().startsWith("Your workout is planned tomorrow."))
        val known = assertNotNull(ReminderRules.candidate(entry(distance = 5.25), settings, utc))
        assertTrue(known.message().startsWith("5.3 km is planned tomorrow."))
        assertEquals("Easy run tomorrow", known.title())
    }
}
