package org.racedaydreams.coach

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.reminders.ReminderPreferences

@RunWith(AndroidJUnit4::class)
class SharedCoachStateBindingsTest {
    @Test
    fun sharedReminderStateRoundTripsEveryModeAndExactZoneTimeAndQuietBoundary() {
        for (mode in ReminderPreferences.Mode.entries) for (zone in
            listOf("Europe/London", "Asia/Kathmandu", "Pacific/Chatham", "UTC")) {
            for (quiet in
                listOf(
                    null,
                    ReminderPreferences.QuietHours(
                        LocalTime.of(22, 45, 10, 987654321),
                        LocalTime.of(7, 15, 5, 123456789),
                    ),
                )) {
                val original =
                    ReminderPreferences(
                        mode,
                        LocalTime.of(19, 30, 0, 123456789),
                        quiet,
                        ZoneId.of(zone),
                    )
                val shared = original.shared()
                assertEquals(zone, shared.zoneId)
                assertEquals(original.eveningTime().toNanoOfDay(), shared.eveningNanoOfDay)
                assertEquals(original, ReminderPreferences.fromShared(shared))
            }
        }
    }

    @Test
    fun sharedInitialPresentationKeepsOriginalLocalReminderDefaultsAndUnknownTrainingEvidence() {
        val state = CoachUiState()
        assertEquals(
            ReminderPreferences.defaults(ZoneId.systemDefault()),
            ReminderPreferences.fromShared(state.reminderPreferences),
        )
        assertNull(state.profile)
        assertNull(state.primaryFocus)
        assertNull(state.baseline.weeklyDistanceKm())
        assertNull(state.baseline.easyPaceSecondsPerKm())
        assertNull(state.baseline.recent5k())
        assertTrue(state.baseline.provenance().isEmpty())
        assertTrue(state.activities.isEmpty())
        assertFalse(state.performanceEligible)
    }
}
