package org.racedaydreams.coach

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.application.*
import org.racedaydreams.data.AndroidSqlConnection
import org.racedaydreams.data.createAndroidSchemaFixture
import org.racedaydreams.domain.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.storage.*

/** Real common command runner and one SQLite transaction for actual/response/receipt/feedback. */
@RunWith(AndroidJUnit4::class)
class SharedRunningEvidenceCoordinatorTest {
    private val now = Instant.parse("2026-11-17T09:00:00Z")
    private val today = LocalDate.of(2026, 11, 17)
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private class Fixture(
        val db: SqlDatabase,
        val service: RunningEvidenceCoordinator,
        val runner: CoachOperationRunner,
        val activities: SqliteActivities,
        val coaching: SqliteCoachingRepository,
        val support: SqliteRunningSupportArchives,
        val original: PlanVersion,
    )

    private fun fixture(block: (Fixture) -> Unit) {
        val raw = SQLiteDatabase.create(null)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            createAndroidSchemaFixture(raw)
            val db = AndroidSqlConnection { raw }
            var ids = 0
            val next = { "actual-${++ids}" }
            val activities = SqliteActivities(db) { now }
            val shoes = SqliteShoes(db)
            val coaching =
                SqliteCoachingRepository(db, { now }, next, PortableLegacyStrengthFingerprint)
            val support = SqliteRunningSupportArchives(db)
            val profile =
                UserProfile(
                    RaceGoal(RaceDistance.GENERAL_FITNESS, "", null, GoalMode.FINISH, null),
                    TrainingPreferences(
                        TrainingStrategy.BALANCED,
                        DayOfWeek.values().toSet(),
                        3,
                        DayOfWeek.SUNDAY,
                        emptySet(),
                        ScheduleType.FLEXIBLE,
                        UnitSystem.METRIC,
                    ),
                    false,
                )
            val original =
                PlanVersion(
                    "version",
                    "plan",
                    1,
                    now.minusSeconds(3600),
                    "Original choice",
                    profile,
                    "Retained prescription",
                    TrainingPlan(
                        "plan",
                        today.minusDays(30),
                        today.plusWeeks(1),
                        emptyList(),
                        listOf(
                            PlannedWorkout(
                                "done",
                                today.minusDays(23),
                                WorkoutCategory.EASY_RUN,
                                3.0,
                                Duration.ofMinutes(30),
                                emptyList(),
                                "Original",
                            ),
                            PlannedWorkout(
                                "future",
                                today.plusDays(1),
                                WorkoutCategory.EASY_RUN,
                                4.0,
                                null,
                                emptyList(),
                                "Future",
                            ),
                        ),
                    ),
                )
            coaching.saveCurrent(original)
            shoes.saveShoe(Shoe("shoe", "Brand", "Daily", null, null, false, "Retained", true))
            val runner =
                CoachOperationRunner(
                    scope,
                    {
                        withContext(Dispatchers.IO) {
                            CoachUiState(
                                loading = false,
                                currentPlan = coaching.current(),
                                activities = activities.activities(),
                                shoeDiscomfort = support.shoeDiscomfort(),
                            )
                        }
                    },
                )
            val service =
                RunningEvidenceCoordinator(
                    support,
                    runner,
                    activities,
                    SqliteResults(db),
                    shoes,
                    coaching,
                    { now },
                    { today },
                    next,
                    { date, time, duration ->
                        val start =
                            date.atTime(LocalTime.parse(time)).atZone(ZoneId.of("Europe/London"))
                        val end = start.plus(duration)
                        RunningEvidenceCoordinator.Timing(
                            start.toInstant(),
                            end.toInstant(),
                            start.offset,
                            end.offset,
                        )
                    },
                )
            block(Fixture(db, service, runner, activities, coaching, support, original))
        } finally {
            instrumentation.runOnMainSync { scope.cancel() }
            raw.close()
        }
    }

    private fun execute(f: Fixture, action: () -> Unit): CoachUiState {
        instrumentation.runOnMainSync { action() }
        val state = runBlocking { withTimeout(20_000) { f.runner.state.first { !it.busy } } }
        // Snapshot publication precedes the success callback on Main. Wait for that command
        // turn to finish before observing callback-owned state, without an arbitrary delay.
        instrumentation.waitForIdleSync()
        return state
    }

    @Test
    fun manualActualRetainsDstOffsetsSourceAndUnknownSamplesWithoutChangingPrescription() =
        fixture { f ->
            var saved: String? = null
            val state =
                execute(f) {
                    // The UK autumn offset changes during this actual elapsed hour.
                    f.service.completeRun(
                        "version",
                        "done",
                        5.125,
                        Duration.ofHours(1),
                        LocalDate.of(2026, 10, 25),
                        "01:30:00.123456789",
                        null,
                        "Actual user entry",
                    ) {
                        saved = it
                    }
                }
            assertNull(state.error)
            val actual = f.activities.activities().single()
            assertEquals(actual.id(), saved)
            assertEquals(SourceIdentity("manual", actual.id(), null), actual.source())
            assertEquals(Instant.parse("2026-10-25T00:30:00.123456789Z"), actual.start())
            assertEquals(Instant.parse("2026-10-25T01:30:00.123456789Z"), actual.end())
            assertEquals(ZoneOffset.ofHours(1), actual.startOffset())
            assertEquals(ZoneOffset.UTC, actual.endOffset())
            assertEquals(5125.0, actual.distanceMetres()!!, 0.0)
            assertNull(actual.rpe())
            assertNull(actual.movingTime())
            assertTrue(actual.samples().isEmpty())
            assertTrue(actual.laps().isEmpty())
            assertEquals("shoe", actual.shoeId())
            assertEquals("done", f.coaching.responses().single().plannedWorkoutId())
            assertEquals(f.original, f.coaching.current())
            assertTrue(f.db.query("SELECT id FROM release08 WHERE kind='association'").isEmpty())
            assertNotNull(
                execute(f) {
                        f.service.completeRun(
                            "version",
                            "done",
                            2.0,
                            Duration.ofMinutes(10),
                            today.minusDays(1),
                            "08:00",
                            5.0,
                            "Duplicate",
                        ) {}
                    }
                    .error
            )
            assertEquals(listOf(actual), f.activities.activities())
        }

    @Test
    fun explicitLinkPreservesImportedSourceAndReceiptAndShoeObservationRemainsOptional() =
        fixture { f ->
            val imported =
                ActivityRecord(
                    "imported",
                    SourceIdentity("health-connect", "source-id", "provider"),
                    now.minusSeconds(3600),
                    now.minusSeconds(1800),
                    ZoneOffset.UTC,
                    ZoneOffset.UTC,
                    3210.0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "Provider evidence",
                    emptyList(),
                    emptyList(),
                    null,
                    "shoe",
                )
            // Persist provider evidence through the real existing importer, never make it manual.
            f.activities.applyImport("health-connect", listOf(imported), emptyList(), "checkpoint")
            val frozen = f.activities.activities().single()
            val stored = f.db.query("SELECT payload FROM activities WHERE id='imported'")
            assertNull(execute(f) { f.service.linkRun("imported", "version", "done", 2) }.error)
            val linked = frozen.copy(plannedWorkoutId = "done")
            assertEquals(listOf(linked), f.activities.activities())
            assertEquals(stored, f.db.query("SELECT payload FROM activities WHERE id='imported'"))
            val response = f.coaching.responses().single()
            assertEquals("version", response.planVersionId())
            val receipt = f.support.snapshot().release08.records.single { it.id == "imported" }
            assertEquals(now.toString(), receipt.payload["at"].toString().trim('"'))
            assertNotNull(execute(f) { f.service.linkRun("imported", "version", "done") }.error)
            assertEquals(response, f.coaching.responses().single())
            assertNull(execute(f) { f.service.shoeDiscomfort("imported", true) }.error)
            assertEquals(setOf("imported"), f.support.shoeDiscomfort())
            assertNull(execute(f) { f.service.shoeDiscomfort("imported", false) }.error)
            assertTrue(f.support.shoeDiscomfort().isEmpty())
            assertEquals(listOf(linked), f.activities.activities())
            assertEquals(stored, f.db.query("SELECT payload FROM activities WHERE id='imported'"))
        }

    @Test
    fun lateFeedbackFailureRollsBackActualAndResponseAndFutureWorkCannotBeCompleted() =
        fixture { f ->
            f.db.execute(
                "CREATE TRIGGER reject_feedback BEFORE INSERT ON run_feedback BEGIN SELECT RAISE(ABORT,'Rejected feedback'); END"
            )
            var saved = false
            assertNotNull(
                execute(f) {
                        f.service.completeRun(
                            "version",
                            "done",
                            3.0,
                            Duration.ofMinutes(30),
                            today.minusDays(1),
                            "08:00",
                            null,
                            "Should roll back",
                        ) {
                            saved = true
                        }
                    }
                    .error
            )
            assertFalse(saved)
            assertTrue(f.activities.activities().isEmpty())
            assertTrue(f.coaching.responses().isEmpty())
            assertTrue(f.db.query("SELECT activity_id FROM run_feedback").isEmpty())
            assertEquals(f.original, f.coaching.current())
            assertNotNull(
                execute(f) {
                        f.service.completeRun(
                            "version",
                            "future",
                            3.0,
                            Duration.ofMinutes(30),
                            today.minusDays(1),
                            "08:00",
                            null,
                            "Future prescription",
                        ) {}
                    }
                    .error
            )
            assertTrue(f.activities.activities().isEmpty())
        }
}
