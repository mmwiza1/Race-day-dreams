package org.racedaydreams.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.storage.*

/** Uses the unchanged independent Android exporter/validator/restorer to check shared writes. */
@RunWith(AndroidJUnit4::class)
class SharedCoachingContextTest {
    private val now = Instant.parse("2026-11-17T09:00:00.123456789Z")
    private val day = LocalDate.of(2026, 11, 17)

    private fun storage(
        block: (SQLiteDatabase, SqliteCoachingContext, CoachingRepository) -> Unit
    ) {
        val db = SQLiteDatabase.create(null)
        try {
            val connection = AndroidSqlConnection { db }
            ApplicationSchema.open(connection) {
                ApplicationSchema.NewInstallation(now.toString(), "context-interop")
            }
            val coaching =
                SqliteCoachingRepository(
                    connection,
                    { now },
                    { "athlete-approved-v2" },
                    PortableLegacyStrengthFingerprint,
                )
            block(
                db,
                SqliteCoachingContext(connection, { now }, { "athlete-approved-v2" }),
                coaching,
            )
        } finally {
            db.close()
        }
    }

    private fun version(): PlanVersion {
        val profile =
            UserProfile(
                RaceGoal(RaceDistance.GENERAL_FITNESS, "", null, GoalMode.FINISH, null),
                TrainingPreferences(
                    TrainingStrategy.BALANCED,
                    setOf(DayOfWeek.TUESDAY),
                    1,
                    DayOfWeek.TUESDAY,
                    emptySet(),
                    ScheduleType.FLEXIBLE,
                    UnitSystem.METRIC,
                ),
                false,
            )
        val workout =
            PlannedWorkout(
                "future",
                day.plusDays(1),
                WorkoutCategory.EASY_RUN,
                5.0,
                Duration.ofMinutes(30),
                emptyList(),
                "Original prescription / café 🏃",
            )
        return PlanVersion(
            "retained-v1",
            "plan",
            1,
            now,
            "Athlete chose",
            profile,
            "Actual evidence",
            TrainingPlan("plan", day, day.plusDays(28), emptyList(), listOf(workout)),
        )
    }

    private fun review(v: PlanVersion) =
        WeeklyCoaching.Review(
            "review",
            v.id(),
            day.minusDays(1),
            2,
            1,
            5.0,
            0,
            null,
            listOf("Unknown effort remains unknown", "Retained / café 🏃"),
            listOf(
                WeeklyCoaching.Channel(
                    "Running",
                    WeeklyCoaching.State.INSUFFICIENT_EVIDENCE,
                    "Limited evidence",
                )
            ),
            "Keep or make a smaller change",
            listOf(
                WeeklyCoaching.Choice("keep", "Keep the programme", emptyList()),
                WeeklyCoaching.Choice(
                    "shorter",
                    "Shorter future run",
                    listOf(v.plan().workouts().single().copy(distanceKm = 3.0)),
                ),
            ),
        )

    @Test
    fun sharedReviewAndExplicitRevisionRemainReadableByOriginalAndroidBackupCode() =
        storage { db, context, coaching ->
            val original = version()
            coaching.saveCurrent(original)
            val review = review(original)
            context.saveReview(review)
            assertEquals(original, coaching.current())
            context.decide(review, "shorter", coaching, emptySet())
            val exported = DevelopmentStore.exportData(db)
            DevelopmentStore.validate(exported)
            val rows = exported.getJSONArray("records")
            assertEquals(2, rows.length())
            val stored =
                (0 until rows.length())
                    .map { rows.getJSONObject(it) }
                    .single { it.getString("kind") == "WEEKLY_REVIEW" }
                    .getJSONObject("payload")
            assertEquals(
                review.observations(),
                (0 until stored.getJSONArray("observations").length()).map {
                    stored.getJSONArray("observations").getString(it)
                },
            )
            assertTrue(stored.isNull("longest"))
            assertEquals(
                "INSUFFICIENT_EVIDENCE",
                stored.getJSONArray("channels").getJSONObject(0).getString("state"),
            )
            assertEquals(
                3.0,
                stored
                    .getJSONArray("choices")
                    .getJSONObject(1)
                    .getJSONArray("replacements")
                    .getJSONObject(0)
                    .getDouble("distanceKm"),
                0.0,
            )
            assertEquals(2, coaching.current()?.version())
            assertEquals(original, coaching.versions().single { it.id() == original.id() })
            val before = exported.toString()
            DevelopmentStore.clear(db)
            DevelopmentStore.restore(db, JSONObject(before))
            assertEquals("shorter", context.reviewDecision("review"))
            assertEquals("shorter", context.decisionForReview(review))
            assertEquals(before, DevelopmentStore.exportData(db).toString())
            assertEquals(2, coaching.versions().size)
        }

    @Test
    fun originalImportedWeightsAreVisibleLocallyButStillExcludedFromUserOwnedExports() =
        storage { db, context, coaching ->
            for ((id, provider) in listOf("manual" to "manual", "cached" to "health-connect")) {
                val payload =
                    JSONObject()
                        .put("id", id)
                        .put("kg", 87.25)
                        .put("at", now.toString())
                        .put("offset", "+05:30:15")
                        .put("provider", provider)
                        .put("sourceId", id)
                        .put("origin", JSONObject.NULL)
                        .put("retainedExtra", "Original metadata")
                db.execSQL(
                    "INSERT INTO coach_evidence(kind,id,payload) VALUES('WEIGHT',?,?)",
                    arrayOf(id, payload.toString()),
                )
            }
            assertEquals(setOf("manual", "cached"), context.weights().map { it.id() }.toSet())
            assertEquals(ZoneOffset.of("+05:30:15"), context.weights().first().offset())
            val exported = DevelopmentStore.exportData(db)
            DevelopmentStore.validate(exported)
            assertEquals(1, exported.getJSONArray("records").length())
            assertEquals(
                "manual",
                exported.getJSONArray("records").getJSONObject(0).getString("id"),
            )
            assertEquals(2, context.weights().size)
            assertTrue(coaching.strength().isEmpty())
            assertTrue(coaching.responses().isEmpty())
            assertNull(coaching.current())
        }
}
