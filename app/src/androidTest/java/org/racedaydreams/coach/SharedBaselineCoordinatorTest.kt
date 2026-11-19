package org.racedaydreams.coach

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.application.*
import org.racedaydreams.data.AndroidSqlConnection
import org.racedaydreams.data.createAndroidSchemaFixture
import org.racedaydreams.domain.baseline.ManualBaselineInput
import org.racedaydreams.storage.*

/**
 * Common application policy with the real runner and transactional SQLite baseline/activity store.
 */
@RunWith(AndroidJUnit4::class)
class SharedBaselineCoordinatorTest {
    private val now = Instant.parse("2026-11-17T09:00:00Z")
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private class Fixture(
        val db: SqlDatabase,
        val baseline: SqliteBaselines,
        val runner: CoachOperationRunner,
        val service: SharedBaselineCoordinator,
        val ids: AtomicInteger,
    )

    private fun fixture(block: (Fixture) -> Unit) {
        val raw = SQLiteDatabase.create(null)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            createAndroidSchemaFixture(raw)
            val db = AndroidSqlConnection { raw }
            val baseline = SqliteBaselines(db) { now }
            val runner = CoachOperationRunner(scope, { CoachUiState(loading = false) })
            val ids = AtomicInteger()
            val service =
                SharedBaselineCoordinator(
                    runner,
                    baseline,
                    SqliteSettings(db),
                    { now },
                    { "assessment-${ids.incrementAndGet()}" },
                )
            block(Fixture(db, baseline, runner, service, ids))
        } finally {
            instrumentation.runOnMainSync { scope.cancel() }
            raw.close()
        }
    }

    private fun awaitFailure(runner: CoachOperationRunner): String = runBlocking {
        withTimeout(20_000) { runner.state.first { !it.busy && it.error != null }.error!! }
    }

    @Test
    fun actualAssessmentKeepsBothDaylightSavingOffsetsAndUnknownEffortWithoutCreatingAPlan() =
        fixture { f ->
            val start = LocalDateTime.of(2026, 10, 25, 1, 55).atZone(ZoneId.of("Europe/London"))
            val duration = Duration.ofMinutes(30)
            val end = start.plus(duration)
            assertNotEquals(start.offset, end.offset)
            val completed = CountDownLatch(1)
            instrumentation.runOnMainSync {
                f.service.saveAssessment(
                    3,
                    duration,
                    {
                        AssessmentTiming(start.toInstant(), start.offset) {
                            end.toInstant() to end.offset
                        }
                    },
                    false,
                    null,
                    "Original manual note",
                    { completed.countDown() },
                )
            }
            assertTrue(completed.await(20, TimeUnit.SECONDS))
            val actual = SqliteActivities(f.db) { now }.activities().single()
            val assessment = f.baseline.assessments().single()
            assertEquals("assessment-1", actual.id())
            assertEquals("assessment", actual.source().provider())
            assertEquals(start.toInstant(), actual.start())
            assertEquals(end.toInstant(), actual.end())
            assertEquals(start.offset, actual.startOffset())
            assertEquals(end.offset, actual.endOffset())
            assertEquals(3000.0, actual.distanceMetres()!!, 0.0)
            assertNull(actual.rpe())
            assertNull(assessment.rpe())
            assertEquals(actual.id(), assessment.activityId())
            assertEquals(duration, assessment.duration())
            assertEquals(end.toInstant(), assessment.completedAt())
            assertTrue(actual.samples().isEmpty())
            assertTrue(actual.laps().isEmpty())
            assertEquals(
                "Controlled assessment. Manually entered. Original manual note",
                actual.notes(),
            )
            assertNull(SqliteSettings(f.db).getSetting("baseline.reviewed"))
            assertTrue(f.db.query("SELECT * FROM coach_plan_versions").isEmpty())
            assertEquals(1, f.ids.get())
        }

    @Test
    fun futureStartIsRejectedBeforeFinishAndInconsistentHostTimingCannotInventEvidence() =
        fixture { f ->
            val finishCalled = AtomicBoolean(false)
            instrumentation.runOnMainSync {
                f.service.saveAssessment(
                    3,
                    Duration.ofMinutes(30),
                    {
                        AssessmentTiming(now.plusSeconds(1), ZoneOffset.UTC) {
                            finishCalled.set(true)
                            error("Future-start check must run first")
                        }
                    },
                    true,
                    7.5,
                    "",
                )
            }
            assertTrue(awaitFailure(f.runner).contains("cannot be in the future"))
            assertFalse(finishCalled.get())
            instrumentation.runOnMainSync {
                f.service.saveAssessment(
                    3,
                    Duration.ofMinutes(30),
                    {
                        AssessmentTiming(now.minusSeconds(3600), ZoneOffset.UTC) {
                            now.minusSeconds(3599) to ZoneOffset.UTC
                        }
                    },
                    true,
                    7.5,
                    "",
                )
            }
            assertTrue(awaitFailure(f.runner).contains("timing does not match"))
            assertTrue(f.baseline.assessments().isEmpty())
            assertTrue(f.db.query("SELECT * FROM activities").isEmpty())
            assertEquals(0, f.ids.get())
        }

    @Test
    fun lateAssessmentFailureRollsBackItsActivityAndRetainsTheManualBaseline() = fixture { f ->
        val manual = ManualBaselineInput.empty()
        f.baseline.saveManualBaseline(manual)
        f.db.execute(
            "CREATE TRIGGER reject_assessment BEFORE INSERT ON assessment_results BEGIN SELECT RAISE(ABORT,'injected assessment failure'); END"
        )
        val completed = AtomicBoolean(false)
        instrumentation.runOnMainSync {
            f.service.saveAssessment(
                3,
                Duration.ofMinutes(30),
                {
                    AssessmentTiming(now.minusSeconds(3600), ZoneOffset.UTC) {
                        now.minusSeconds(1800) to ZoneOffset.UTC
                    }
                },
                false,
                null,
                "",
                { completed.set(true) },
            )
        }
        assertTrue(awaitFailure(f.runner).contains("left in place"))
        assertFalse(completed.get())
        assertTrue(f.baseline.assessments().isEmpty())
        assertTrue(f.db.query("SELECT * FROM activities").isEmpty())
        assertEquals(manual, f.baseline.savedManualBaseline())
        assertEquals(now, f.baseline.manualBaselineRecordedAt())
        assertEquals(1, f.ids.get())
    }
}
