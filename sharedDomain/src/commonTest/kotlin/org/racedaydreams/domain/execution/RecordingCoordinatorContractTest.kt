package org.racedaydreams.domain.execution

import kotlin.test.*
import org.racedaydreams.domain.WorkoutSegment
import org.racedaydreams.domain.recording.RunMetricsEngine

class RecordingCoordinatorContractTest {
    private fun status(
        metres: Double = 0.0,
        active: Long = 0,
        elapsed: Long = active,
        pace: Double? = null,
        gps: String = "GPS ready",
        paused: Boolean = false,
    ) = RunMetricsEngine.Status(metres, elapsed, active, pace, gps, paused)

    private fun step(metres: Double? = 1000.0, millis: Long? = null) =
        WorkoutExecutionEngine.Step(
            "Run",
            WorkoutSegment.Kind.RUN,
            metres,
            millis,
            295.0,
            305.0,
            7.0,
        )

    @Test
    fun eventDeltasPublishOnceAndExternalManualActionsKeepTheirRecordedBoundaries() {
        val e = WorkoutExecutionCoordinator(listOf(step(null), step(400.0)))
        val initial = e.update(status())
        e.update(status(100.0, 30000))
        e.completeOpen()
        val changed = e.update(status(100.0, 30000))
        assertEquals(
            listOf("STEP_COMPLETED", "STEP_STARTED"),
            changed.newEvents().map { it.type() },
        )
        assertEquals(100.0, changed.newEvents().first().metres())
        assertEquals(30000L, changed.newEvents().first().activeMillis())
        assertTrue(e.update(status(100.0, 30000)).newEvents().isEmpty())
        e.skip()
        assertTrue(e.update(status(100.0, 30000)).state()!!.complete())
        assertEquals(2, initial.newEvents().size)
        assertEquals(6, e.events().size)
        assertFails { (changed.newEvents() as MutableList).clear() }
        val source = mutableListOf(changed.newEvents().first())
        val snapshot = WorkoutExecutionCoordinator.Update(changed.state(), source)
        source.clear()
        assertEquals(1, snapshot.newEvents().size)
        assertEquals(
            snapshot,
            WorkoutExecutionCoordinator.Update(changed.state(), snapshot.newEvents()),
        )
    }

    @Test
    fun gpsPrioritySuppressesTransitionWithoutCreatingAStaleSpeechBacklog() {
        val c = RecordingCueCoordinator()
        val e = WorkoutExecutionCoordinator(listOf(step()))
        val metrics = status(gps = "GPS temporarily lost · distance incomplete")
        val first = c.update(metrics, e.update(metrics), e.steps(), null, false, 10000, false, true)
        assertEquals("gps-lost", first.selected()!!.id())
        assertNull(
            c.update(
                    status(active = 1000, gps = "GPS temporarily lost · distance incomplete"),
                    null,
                    e.steps(),
                    null,
                    false,
                    10000,
                    false,
                    true,
                )
                .selected()
        )
        assertNull(
            c.update(status(active = 2000), null, e.steps(), null, false, 10000, false, true)
                .selected()
        )
        assertNull(
            c.update(
                    status(active = 3000, gps = "GPS temporarily lost"),
                    null,
                    e.steps(),
                    null,
                    false,
                    10000,
                    false,
                    true,
                )
                .selected()
        )
        c.update(status(active = 60000), null, e.steps(), null, false, 10000, false, true)
        assertEquals(
            "gps-lost",
            c.update(
                    status(active = 61000, gps = "GPS temporarily lost"),
                    null,
                    e.steps(),
                    null,
                    false,
                    10000,
                    false,
                    true,
                )
                .selected()!!
                .id(),
        )
    }

    @Test
    fun raceContextUsesElapsedIncludingPausesAndMilestonesAreNeverRepeated() {
        val c = RecordingCueCoordinator()
        val race = RecordingCueCoordinator.RaceTarget(21000.0, 6300L)
        val first =
            c.update(
                status(10500.0, 2800000, 3000000),
                null,
                emptyList(),
                race,
                false,
                10000,
                false,
                false,
            )
        assertTrue(first.raceContext()!!.startsWith("150 sec ahead of"))
        assertEquals("race-half", first.selected()!!.id())
        assertNull(
            c.update(
                    status(10500.0, 2800000, 3000000),
                    null,
                    emptyList(),
                    race,
                    false,
                    10000,
                    false,
                    false,
                )
                .selected()
        )
        assertEquals(
            "race-five",
            c.update(
                    status(16000.0, 4500000, 5000000),
                    null,
                    emptyList(),
                    race,
                    false,
                    10000,
                    false,
                    false,
                )
                .selected()!!
                .id(),
        )
        assertEquals(
            "Race elapsed time includes pauses.",
            c.update(
                    status(),
                    null,
                    emptyList(),
                    RecordingCueCoordinator.RaceTarget(1000.0, null),
                    false,
                    10000,
                    false,
                    false,
                )
                .raceContext(),
        )
    }

    @Test
    fun splitPreferencesDoNotReplayMissedAnnouncementsAndSensorLossRequiresAConnection() {
        val c = RecordingCueCoordinator()
        assertNull(
            c.update(status(1200.0, 360000), null, emptyList(), null, false, 10000, false, false)
                .selected()
        )
        assertNull(
            c.update(status(1300.0, 390000), null, emptyList(), null, false, 10000, false, true)
                .selected()
        )
        assertEquals(
            "split-2",
            c.update(status(2000.0, 600000), null, emptyList(), null, false, 10000, false, true)
                .selected()!!
                .id(),
        )
        assertFalse(c.heartRateConnection(false))
        assertFalse(c.heartRateConnection(true))
        assertFalse(c.heartRateConnection(true))
        assertTrue(c.heartRateConnection(false))
        assertFalse(c.heartRateConnection(false))
    }

    @Test
    fun paceCuesRequireFreshReliableSustainedEvidenceAndHillsSuppressThem() {
        fun cues(hilly: Boolean, gps: String): List<RecordingCueCoordinator.Update> {
            val c = RecordingCueCoordinator()
            val e = WorkoutExecutionCoordinator(listOf(step(10000.0)))
            return listOf(0L, 15000L, 25000L).map { t ->
                val s = status(t / 300.0, t, pace = 250.0, gps = gps)
                c.update(s, e.update(s), e.steps(), null, hilly, 10000, false, false)
            }
        }
        val qualified = cues(false, "GPS ready").last()
        assertTrue(qualified.selected()!!.id().startsWith("pace-"))
        assertEquals("TARGET_RANGE_EXITED", qualified.paceEvent())
        assertNull(cues(true, "GPS ready").last().selected())
        assertNull(cues(false, "GPS signal weak").last().selected())
    }

    @Test
    fun finishingEarlyDoesNotBecomeCompletedEvidenceAndNewEventsAreStillPublishedOnce() {
        val e = WorkoutExecutionCoordinator(listOf(step()))
        e.update(status())
        e.update(status(100.0, 30000))
        e.finishEarly()
        val first = e.update(status(100.0, 30000))
        assertTrue(first.state()!!.early())
        assertFalse(first.state()!!.complete())
        assertEquals(listOf("WORKOUT_FINISHED_EARLY"), first.newEvents().map { it.type() })
        assertTrue(e.update(status(2000.0, 600000)).newEvents().isEmpty())
    }
}
