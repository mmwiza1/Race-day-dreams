package org.racedaydreams.coach

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.DayOfWeek
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.application.ProfileOperations
import org.racedaydreams.data.AndroidSqlConnection
import org.racedaydreams.data.createAndroidSchemaFixture
import org.racedaydreams.domain.*
import org.racedaydreams.storage.*

/** Shared application policy over real persisted inputs; no fabricated baseline or workout. */
@RunWith(AndroidJUnit4::class)
class SharedProfileOperationsTest {
    private fun profile(strategy: TrainingStrategy, eligible: Boolean = false) =
        UserProfile(
            RaceGoal(RaceDistance.GENERAL_FITNESS, "", null, GoalMode.FINISH, null),
            TrainingPreferences(
                strategy,
                setOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY),
                2,
                DayOfWeek.FRIDAY,
                emptySet(),
                ScheduleType.FLEXIBLE,
                UnitSystem.METRIC,
            ),
            eligible,
        )

    private fun database(block: (SqlDatabase, SqliteAthleteProfile, ProfileOperations) -> Unit) {
        val original = SQLiteDatabase.create(null)
        try {
            createAndroidSchemaFixture(original)
            val db = AndroidSqlConnection { original }
            val profiles = SqliteAthleteProfile(db)
            block(db, profiles, ProfileOperations(profiles, SqliteSettings(db)))
        } finally {
            original.close()
        }
    }

    @Test
    fun aNewUnsupportedPerformanceSelectionRetainsTheSavedGoalAndExactDraft() =
        database { db, profiles, operations ->
            val original = profile(TrainingStrategy.BALANCED)
            profiles.save(original)
            val draft = """ {"step":2,"target":"Original input"} """
            profiles.saveDraft(draft)
            try {
                operations.save(profile(TrainingStrategy.PERFORMANCE), false)
                fail("A new Performance choice needs supporting eligibility.")
            } catch (expected: IllegalArgumentException) {
                assertTrue(
                    expected.message!!.contains("does not support a new Performance selection")
                )
            }
            assertEquals(original, profiles.load())
            assertEquals(draft, profiles.loadDraft())
            assertNull(SqliteSettings(db).getSetting("baseline.reviewed"))
            assertTrue(db.query("SELECT * FROM coach_plan_versions").isEmpty())
            assertTrue(db.query("SELECT * FROM activities").isEmpty())
        }

    @Test
    fun supportedSelectionCanBeRetainedWithoutInventingNewEligibilityOrChangingHistory() =
        database { db, profiles, operations ->
            db.execute(
                "INSERT INTO activities VALUES('retained','rdd','source',NULL,10,20,'original history')"
            )
            profiles.saveDraft("""{"step":2}""")
            operations.save(profile(TrainingStrategy.PERFORMANCE), true)
            assertEquals(profile(TrainingStrategy.PERFORMANCE, true), profiles.load())
            assertNull(profiles.loadDraft())
            assertEquals("true", SqliteSettings(db).getSetting("baseline.reviewed"))
            operations.save(profile(TrainingStrategy.PERFORMANCE), false)
            assertEquals(profile(TrainingStrategy.PERFORMANCE, false), profiles.load())
            operations.save(profile(TrainingStrategy.BALANCED), false)
            assertEquals(profile(TrainingStrategy.BALANCED), profiles.load())
            assertEquals(
                listOf(listOf(SqlValue.Text("original history"))),
                db.query("SELECT payload FROM activities WHERE id='retained'"),
            )
            assertTrue(db.query("SELECT * FROM coach_plan_versions").isEmpty())
        }

    @Test
    fun failedProfileReplacementRollsBackItsDraftAndDoesNotMarkBaselineReviewed() =
        database { db, profiles, operations ->
            val original = profile(TrainingStrategy.BALANCED)
            profiles.save(original)
            val draft = """ {"step":3,"retained":"Athlete's input"} """
            operations.saveDraft(draft)
            db.execute(
                "CREATE TRIGGER reject_goal BEFORE INSERT ON race_goal BEGIN SELECT RAISE(ABORT,'reject profile'); END"
            )
            try {
                operations.save(profile(TrainingStrategy.PERFORMANCE), true)
                fail("The injected storage failure must not be accepted.")
            } catch (expected: Exception) {
                assertTrue(expected.message!!.contains("reject profile"))
            }
            assertEquals(original, profiles.load())
            assertEquals(draft, profiles.loadDraft())
            assertNull(SqliteSettings(db).getSetting("baseline.reviewed"))
            operations.clearDraft()
            assertNull(profiles.loadDraft())
            assertEquals(original, profiles.load())
            assertTrue(db.query("PRAGMA foreign_key_check").isEmpty())
        }
}
