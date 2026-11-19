package org.racedaydreams.coach

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
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
import org.racedaydreams.domain.baseline.BaselineEngine
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.storage.*

/**
 * Existing user-controlled plan commands over real persisted prescriptions, actuals and decisions.
 */
@RunWith(AndroidJUnit4::class)
class SharedRunningPlanCoordinatorTest {
    private val now = Instant.parse("2026-11-17T09:00:00Z")
    private val today = LocalDate.of(2026, 11, 17)
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private class Fixture(
        val db: SqlDatabase,
        val runner: CoachOperationRunner,
        val service: RunningPlanCoordinator,
        val coaching: SqliteCoachingRepository,
        val context: SqliteCoachingContext,
        val profiles: SqliteAthleteProfile,
        val activities: SqliteActivities,
        val original: PlanVersion,
        val actual: ActivityRecord,
        val ids: AtomicInteger,
    )

    private fun fixture(forGeneratedRebuild: Boolean = false, block: (Fixture) -> Unit) {
        val raw = SQLiteDatabase.create(null)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            createAndroidSchemaFixture(raw)
            val db = AndroidSqlConnection { raw }
            val ids = AtomicInteger()
            val next = { "explicit-choice-${ids.incrementAndGet()}" }
            val profiles = SqliteAthleteProfile(db)
            val context = SqliteCoachingContext(db, { now }, next)
            val activities = SqliteActivities(db) { now }
            val coaching =
                SqliteCoachingRepository(db, { now }, next, PortableLegacyStrengthFingerprint)
            val profile =
                UserProfile(
                    RaceGoal(RaceDistance.GENERAL_FITNESS, "", null, GoalMode.FINISH, null),
                    TrainingPreferences(
                        TrainingStrategy.BALANCED,
                        if (forGeneratedRebuild)
                            setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY)
                        else DayOfWeek.values().toSet(),
                        3,
                        DayOfWeek.SUNDAY,
                        emptySet(),
                        ScheduleType.FLEXIBLE,
                        UnitSystem.METRIC,
                    ),
                    false,
                )
            profiles.save(profile)
            profiles.saveDraft(""" {"step":2,"input":"Retained draft"} """)
            // Same-day preservation applies to matching generator identities, not arbitrary IDs
            // absent from a rebuilt programme. Seed this case through the real production
            // generator.
            val generatedSeed =
                if (forGeneratedRebuild)
                    OriginalTrainingPlanGenerator()
                        .generate(
                            PlanRequest(
                                profile,
                                org.racedaydreams.domain.baseline.RunnerBaseline.empty(
                                    now.minusSeconds(84L * 86400),
                                    now,
                                ),
                                today,
                                setOf(today.plusDays(3)),
                                emptyList(),
                                45,
                                "MORE_SHORTER",
                            )
                        )
                else null
            val completedId =
                generatedSeed?.workouts()?.single { it.date() == today }?.id() ?: "done"
            val planId = generatedSeed?.id() ?: "original-plan"
            val workouts =
                listOf(
                    PlannedWorkout(
                        completedId,
                        today,
                        WorkoutCategory.EASY_RUN,
                        3.0,
                        null,
                        emptyList(),
                        "Original completed prescription",
                    ),
                    PlannedWorkout(
                        "move",
                        today.plusDays(2),
                        WorkoutCategory.EASY_RUN,
                        4.0,
                        null,
                        emptyList(),
                        "Original future prescription",
                    ),
                    PlannedWorkout(
                        "other",
                        today.plusDays(4),
                        WorkoutCategory.LONG_RUN,
                        5.0,
                        null,
                        emptyList(),
                        "Other future work",
                    ),
                )
            val original =
                PlanVersion(
                    "original-version",
                    planId,
                    1,
                    now.minusSeconds(60),
                    "Original athlete choice",
                    profile,
                    "Original evidence",
                    TrainingPlan(planId, today, today.plusWeeks(2), emptyList(), workouts),
                )
            coaching.saveCurrent(original)
            val actual =
                ActivityRecord(
                    "actual",
                    SourceIdentity("manual", "actual", null),
                    now.minusSeconds(3600),
                    now.minusSeconds(1800),
                    ZoneOffset.UTC,
                    ZoneOffset.UTC,
                    3000.0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "Actual entered run",
                    emptyList(),
                    emptyList(),
                    completedId,
                    null,
                )
            activities.upsertManualActivity(actual)
            coaching.saveResponse(
                RunExecution(
                    actual.id(),
                    completedId,
                    original.id(),
                    null,
                    null,
                    "Original explicit association",
                )
            )
            suspend fun read() =
                withContext(Dispatchers.IO) {
                    val evidence = activities.activities()
                    CoachUiState(
                        loading = false,
                        profile = profiles.load(),
                        currentPlan = coaching.current(),
                        activities = evidence,
                        baseline =
                            BaselineEngine.derive(evidence, now.minusSeconds(84L * 86400), now),
                    )
                }
            val runner = CoachOperationRunner(scope, ::read)
            runner.update {
                it.copy(
                    baseline =
                        BaselineEngine.derive(
                            activities.activities(),
                            now.minusSeconds(84L * 86400),
                            now,
                        )
                )
            }
            val service =
                RunningPlanCoordinator(
                    SqliteRunningSupportArchives(db),
                    runner,
                    activities,
                    profiles,
                    coaching,
                    context,
                    { now },
                    { today },
                    next,
                    ::shortDate,
                )
            block(
                Fixture(
                    db,
                    runner,
                    service,
                    coaching,
                    context,
                    profiles,
                    activities,
                    original,
                    actual,
                    ids,
                )
            )
        } finally {
            instrumentation.runOnMainSync { scope.cancel() }
            raw.close()
        }
    }

    private fun execute(f: Fixture, action: () -> Unit): CoachUiState {
        instrumentation.runOnMainSync { action() }
        return runBlocking { withTimeout(20_000) { f.runner.state.first { !it.busy } } }
    }

    @Test
    fun rebuildUsesStoredScheduleAndPreservesCompletedPrescriptionActualSourceAndDraft() =
        fixture(forGeneratedRebuild = true) { f ->
            val draft = f.profiles.loadDraft()
            val response = f.coaching.responses().single()
            f.db.execute(
                "INSERT INTO release08(kind,id,payload) VALUES('config','schedule',?)",
                listOf(
                    SqlValue.Text(
                        """{"version":1,"type":"schedule","maximumMinutes":45,"distribution":"MORE_SHORTER","unavailable":["2026-11-20"]}"""
                    )
                ),
            )
            val result = execute(f) { f.service.generatePlan() }
            assertNull(result.error)
            val current = f.coaching.current()!!
            assertNotEquals(f.original.id(), current.id())
            assertEquals(2, f.coaching.versions().size)
            assertEquals(f.original, f.coaching.versions().single { it.id() == f.original.id() })
            assertEquals(
                f.original.plan().workouts().first(),
                current.plan().workouts().single { it.id() == f.actual.plannedWorkoutId() },
            )
            assertTrue(current.plan().workouts().none { it.date() == today.plusDays(3) })
            assertTrue(current.evidenceSnapshot().contains("45 minute usual budget; more shorter"))
            assertTrue(current.evidenceSnapshot().contains("20 Nov 2026"))
            assertEquals(response, f.coaching.responses().single())
            assertEquals(f.actual, f.activities.activities().single())
            assertEquals(draft, f.profiles.loadDraft())
        }

    @Test
    fun explicitMoveCreatesRevisionAndReceiptWhileCompletedWorkoutCannotBeMoved() = fixture { f ->
        assertNull(execute(f) { f.service.changeWorkout("move", today.plusDays(3)) }.error)
        val current = f.coaching.current()!!
        assertEquals(2, current.version())
        assertEquals(
            today.plusDays(3),
            current.plan().workouts().single { it.id() == "move" }.date(),
        )
        assertEquals(
            today.plusDays(2),
            f.original.plan().workouts().single { it.id() == "move" }.date(),
        )
        val decision = f.context.sessionDecisions().single()
        assertEquals(f.original.id(), decision.versionId())
        assertEquals(today.plusDays(2), decision.originalDate())
        assertEquals(today.plusDays(3), decision.movedTo())
        assertEquals(WorkoutDecision.Kind.MOVED, decision.kind())
        val allocated = f.ids.get()
        assertTrue(
            execute(f) { f.service.changeWorkout("done", today.plusDays(1)) }
                .error!!
                .contains("Completed sessions cannot be changed")
        )
        assertEquals(current, f.coaching.current())
        assertEquals(2, f.coaching.versions().size)
        assertEquals(listOf(decision), f.context.sessionDecisions())
        assertEquals(allocated, f.ids.get())
        assertEquals(f.actual, f.activities.activities().single())
    }

    @Test
    fun decisionFailureRollsBackTheFutureRevisionWithoutChangingHistoricalEvidence() =
        fixture { f ->
            val draft = f.profiles.loadDraft()
            val response = f.coaching.responses().single()
            f.db.execute(
                "CREATE TRIGGER reject_plan_decision BEFORE INSERT ON coach_evidence WHEN NEW.kind='SESSION_DECISION' BEGIN SELECT RAISE(ABORT,'injected decision failure'); END"
            )
            assertTrue(
                execute(f) { f.service.changeWorkout("move", today.plusDays(3)) }
                    .error!!
                    .contains("left in place")
            )
            assertEquals(f.original, f.coaching.current())
            assertEquals(listOf(f.original), f.coaching.versions())
            assertTrue(f.context.sessionDecisions().isEmpty())
            assertEquals(response, f.coaching.responses().single())
            assertEquals(f.actual, f.activities.activities().single())
            assertEquals(draft, f.profiles.loadDraft())
            assertTrue(f.db.query("PRAGMA foreign_key_check").isEmpty())
        }
}
