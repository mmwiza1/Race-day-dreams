package org.racedaydreams.coach

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.application.*
import org.racedaydreams.data.AndroidSqlConnection
import org.racedaydreams.data.createAndroidSchemaFixture
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.*

/** Real common operations and SQLite actual/projection boundary; no prescription becomes a set. */
@RunWith(AndroidJUnit4::class)
class SharedStrengthCoordinatorTest {
    private val now = Instant.parse("2026-11-17T09:00:00Z")
    private val today = LocalDate.of(2026, 11, 17)
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val exercise =
        StrengthExercise(
            "custom-original",
            "Original exercise",
            StrengthExercise.Region.UPPER,
            StrengthExercise.Mode.WEIGHTED,
            false,
            StrengthExercise.MeasurementProfile.LOAD_REPS,
        )

    private fun historical(id: String, date: LocalDate? = null) =
        HistoricalStrengthRecord(
            id,
            exercise,
            82.25,
            6,
            date,
            if (date == null) HistoricalStrengthRecord.DatePrecision.UNKNOWN
            else HistoricalStrengthRecord.DatePrecision.EXACT,
            "Actual prior evidence",
            null,
            null,
        )

    private fun session() =
        StrengthSession(
            "actual-session",
            now.minusSeconds(3600),
            ZoneOffset.UTC,
            Duration.ofMinutes(30),
            null,
            "User-entered session",
            listOf(
                StrengthSession.ExerciseSets(
                    exercise,
                    listOf(StrengthSession.SetEntry("actual-set", 6, 82.25, null, false)),
                )
            ),
        )

    private class Fixture(
        val db: SqlDatabase,
        val service: StrengthCoordinator,
        val runner: CoachOperationRunner,
        val coaching: SqliteCoachingRepository,
        val context: SqliteCoachingContext,
        val choices: SqliteStrengthChoices,
        val exclusions: SqliteExerciseExclusions,
    )

    private fun fixture(block: (Fixture) -> Unit) {
        val raw = SQLiteDatabase.create(null)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            createAndroidSchemaFixture(raw)
            val db = AndroidSqlConnection { raw }
            var identity = 0
            val next = { "explicit-choice-${++identity}" }
            val context = SqliteCoachingContext(db, { now }, next)
            val coaching =
                SqliteCoachingRepository(db, { now }, next, PortableLegacyStrengthFingerprint)
            val choices = SqliteStrengthChoices(db)
            val exclusions = SqliteExerciseExclusions(db)
            coaching.saveExercise(exercise)
            context.saveHistorical(historical("retained-pb"))
            val runner =
                CoachOperationRunner(
                    scope,
                    {
                        withContext(Dispatchers.IO) {
                            CoachUiState(
                                loading = false,
                                exercises = coaching.exercises(),
                                strengthSessions = coaching.strength(),
                                historicalStrength = context.historical(),
                                strengthPlannedDates = context.strengthLinks(),
                                exerciseChoices = choices.choices(),
                                exerciseExclusions = exclusions.exclusions(),
                            )
                        }
                    },
                )
            val programmes =
                SharedGymProgrammeOperations(
                    {
                        object : GymProgrammeActionClock {
                            override fun instant() = now

                            override fun today() = this@SharedStrengthCoordinatorTest.today
                        }
                    },
                    next,
                )
            val service =
                StrengthCoordinator(
                    SqliteLegacyGymProgrammes(db, { now }, next),
                    choices,
                    exclusions,
                    runner,
                    coaching,
                    context,
                    programmes,
                    { now },
                    { today },
                )
            block(Fixture(db, service, runner, coaching, context, choices, exclusions))
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
    fun savedActualAndGenericLinkRemainFrozenThroughRenameArchiveExclusionAndRestore() =
        fixture { f ->
            val actual = session()
            assertNull(execute(f) { f.service.saveStrength(actual, plannedDate = today) }.error)
            assertEquals(actual, f.coaching.strength().single())
            assertEquals(today, f.context.strengthLinks()[actual.id()])
            assertTrue(f.context.gymLinks().isEmpty())
            assertNull(
                execute(f) { f.service.saveExercise(exercise.copy(name = "Current exercise name")) }
                    .error
            )
            assertEquals(
                "Current exercise name",
                f.coaching.exercises().single { it.id() == exercise.id() }.name(),
            )
            assertNull(execute(f) { f.service.exerciseChoice(exercise.id(), true, true) }.error)
            assertNull(
                execute(f) {
                        f.service.exerciseExclusion(
                            exercise.id(),
                            ExerciseExclusionRepository.Reason.UNCOMFORTABLE,
                        )
                    }
                    .error
            )
            assertEquals(
                ExerciseExclusionRepository.Reason.UNCOMFORTABLE,
                f.exclusions.exclusions().single().reason(),
            )
            assertNull(execute(f) { f.service.exerciseExclusion(exercise.id(), null) }.error)
            assertTrue(f.exclusions.exclusions().isEmpty())
            assertEquals(actual, f.coaching.strength().single())
            assertEquals(historical("retained-pb"), f.context.historical().single())
            assertTrue(f.choices.choices().single().favourite())
            assertTrue(f.choices.choices().single().archived())
            assertNull(execute(f) { f.service.deleteStrength(actual.id()) }.error)
            assertTrue(f.coaching.strength().isEmpty())
            assertTrue(f.context.strengthLinks().isEmpty())
            assertEquals(historical("retained-pb"), f.context.historical().single())
        }

    @Test
    fun failedAssociationRollsBackActualAndLinkWhileRetainingHistoricalEvidence() = fixture { f ->
        f.db.execute(
            "CREATE TRIGGER reject_strength_projection BEFORE INSERT ON strength_association_targets BEGIN SELECT RAISE(ABORT,'injected association failure'); END"
        )
        assertTrue(
            execute(f) { f.service.saveStrength(session(), plannedDate = today) }
                .error!!
                .contains("left in place")
        )
        assertTrue(f.coaching.strength().isEmpty())
        assertTrue(f.context.strengthLinks().isEmpty())
        assertTrue(f.db.query("SELECT * FROM strength_association_targets").isEmpty())
        assertEquals(historical("retained-pb"), f.context.historical().single())
        assertTrue(f.db.query("PRAGMA foreign_key_check").isEmpty())
    }

    @Test
    fun historicalFutureGuardPreservesHistoryAndUnknownDateNeverCreatesAWorkout() = fixture { f ->
        assertTrue(
            execute(f) { f.service.saveHistorical(historical("future", today.plusDays(1))) }
                .error!!
                .contains("cannot be in the future")
        )
        assertEquals(listOf(historical("retained-pb")), f.context.historical())
        assertNull(execute(f) { f.service.saveHistorical(historical("unknown")) }.error)
        assertNull(f.context.historical().single { it.id() == "unknown" }.date())
        assertTrue(f.coaching.strength().isEmpty())
        assertTrue(f.context.strengthLinks().isEmpty())
        assertNull(execute(f) { f.service.deleteHistorical("unknown") }.error)
        assertEquals(listOf(historical("retained-pb")), f.context.historical())
    }
}
