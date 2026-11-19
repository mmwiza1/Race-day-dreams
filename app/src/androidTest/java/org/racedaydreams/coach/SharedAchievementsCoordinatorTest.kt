package org.racedaydreams.coach

import android.database.sqlite.SQLiteDatabase
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.strength.*
import org.racedaydreams.data.AndroidSqlConnection
import org.racedaydreams.data.createAndroidSchemaFixture
import org.racedaydreams.domain.achievements.TrainingMilestones
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.*

/** Actual shared recognition and SQL, retained history and summary-only activity reads. */
@RunWith(AndroidJUnit4::class)
class SharedAchievementsCoordinatorTest {
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val now = Instant.parse("2026-11-17T18:00:00Z")

    private class Fixture(
        val model: SharedAchievementsCoordinator,
        val db: SqlDatabase,
        val clock: AtomicReference<Instant>,
        val reads: AtomicInteger,
        val fail: AtomicBoolean,
    )

    private fun fixture(block: (Fixture) -> Unit) {
        val raw = SQLiteDatabase.create(null)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            createAndroidSchemaFixture(raw)
            val calls = AtomicInteger()
            val db = AndroidSqlConnection {
                check(Looper.myLooper() != Looper.getMainLooper()) {
                    "Milestone SQL must stay off Main."
                }
                calls.incrementAndGet()
                raw
            }
            val identity = AtomicInteger()
            val clock = AtomicReference(now)
            val coaching =
                SqliteCoachingRepository(
                    db,
                    clock::get,
                    { "milestone-event-${identity.incrementAndGet()}" },
                    PortableLegacyStrengthFingerprint,
                )
            repeat(5) { index ->
                coaching.saveStrength(
                    StrengthSession(
                        "actual-$index",
                        now.minusSeconds(86400L * (index + 1)),
                        ZoneOffset.UTC,
                        Duration.ofMinutes(30),
                        null,
                        "Retained actual",
                        listOf(
                            StrengthSession.ExerciseSets(
                                requireNotNull(ExerciseCatalog.definition("push-up")).exercise(),
                                listOf(StrengthSession.SetEntry("set-$index", 6, 0.0, null, false)),
                            )
                        ),
                    )
                )
            }
            val actual = SqliteActivities(db, clock::get)
            val reads = AtomicInteger()
            val fail = AtomicBoolean(false)
            val summaries =
                object : ActivityRepository by actual {
                    override fun activities(): List<ActivityRecord> =
                        error("Recognition must not hydrate routes.")

                    override fun activityDetail(id: String): ActivityRecord? =
                        error("No route detail requested.")

                    override fun activitySummaries(): List<ActivityRecord> {
                        reads.incrementAndGet()
                        if (fail.get()) throw IllegalStateException("Unavailable summary source")
                        return actual.activitySummaries()
                    }
                }
            val reference = AtomicReference<SharedAchievementsCoordinator>()
            calls.set(0)
            instrumentation.runOnMainSync {
                reference.set(
                    SharedAchievementsCoordinator(db, scope, coaching, summaries, clock::get)
                )
            }
            assertEquals("Construction must not read the database", 0, calls.get())
            block(Fixture(reference.get(), db, clock, reads, fail))
        } finally {
            instrumentation.runOnMainSync { scope.cancel() }
            raw.close()
        }
    }

    private fun refresh(f: Fixture, enabled: Boolean? = null): AchievementsState {
        instrumentation.runOnMainSync { f.model.refresh(enabled) }
        runBlocking { withTimeout(20000) { f.model.state.first { !it.loading } } }
        instrumentation.waitForIdleSync()
        return f.model.state.value
    }

    @Test
    fun retainedRecognitionUsesSummariesAndNeverRedatesOrRewritesActualEvidence() = fixture { f ->
        val originals = f.db.query("SELECT * FROM strength_sessions ORDER BY id")
        val first = refresh(f, true)
        assertNull(first.error)
        assertEquals(2, first.awards.size)
        assertEquals(5, first.availableSources.size)
        assertTrue(first.availableSources.all { it.first == TrainingMilestones.Source.GYM })
        assertTrue(first.celebrations)
        assertNotNull(first.notice)
        val retained = f.db.query("SELECT * FROM training_milestones ORDER BY kind")
        f.clock.set(now.plusSeconds(86400))
        val second = refresh(f, false)
        assertNull(second.error)
        assertNull(second.notice)
        assertFalse(second.celebrations)
        assertEquals(first.awards, second.awards)
        assertEquals(retained, f.db.query("SELECT * FROM training_milestones ORDER BY kind"))
        assertEquals(originals, f.db.query("SELECT * FROM strength_sessions ORDER BY id"))
        assertEquals(2, f.reads.get())
    }

    @Test
    fun sourceFailureRetainsKnownAwardsAndHistoryThenAllowsAnExplicitRetry() = fixture { f ->
        val original = refresh(f, true)
        val actuals = f.db.query("SELECT * FROM strength_sessions ORDER BY id")
        val retained = f.db.query("SELECT * FROM training_milestones ORDER BY kind")
        f.fail.set(true)
        val failed = refresh(f)
        assertFalse(failed.loading)
        assertEquals(
            "Milestones could not be updated. Your workout history is unchanged.",
            failed.error,
        )
        assertEquals(original.awards, failed.awards)
        assertEquals(retained, f.db.query("SELECT * FROM training_milestones ORDER BY kind"))
        assertEquals(actuals, f.db.query("SELECT * FROM strength_sessions ORDER BY id"))
        f.fail.set(false)
        val retried = refresh(f)
        assertNull(retried.error)
        assertEquals(original.awards, retried.awards)
        assertEquals(retained, f.db.query("SELECT * FROM training_milestones ORDER BY kind"))
        instrumentation.runOnMainSync { f.model.dismiss() }
        assertNull(f.model.state.value.notice)
    }
}
