package org.racedaydreams.coach

import android.database.sqlite.SQLiteDatabase
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.application.CoachSnapshotSources
import org.racedaydreams.coach.application.SharedCoachSnapshotReader
import org.racedaydreams.data.AndroidSqlConnection
import org.racedaydreams.data.createAndroidSchemaFixture
import org.racedaydreams.domain.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.presentation.StrengthTodaySummary
import org.racedaydreams.domain.reminders.ReminderSettings
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.strength.*
import org.racedaydreams.health.HealthAvailability
import org.racedaydreams.storage.*

/**
 * Real original SQLite schema/repositories, with deterministic host permissions/reminder inputs.
 */
@RunWith(AndroidJUnit4::class)
class CoachSnapshotReaderTest {
    private val now = Instant.parse("2026-11-17T09:00:00.123456789Z")
    private val day = LocalDate.of(2026, 11, 17)

    private class Sources : CoachSnapshotSources {
        var selectedFocus: PrimaryFocus? = PrimaryFocus.STRENGTH
        var savedProfile: UserProfile? = null
        var draft: String? = "Persisted onboarding draft"
        var covered: Pair<Instant, Instant>? = null
        var availability = HealthAvailability.UNAVAILABLE
        var permissions: Set<String> = emptySet()
        var permissionFailure: Exception? = null
        var permissionReads = 0
        var legacyReads = 0
        val reviews = linkedMapOf<String, WeeklyCoaching.Review>()
        val events = mutableListOf<String>()
        val links =
            mapOf("actual" to StrengthTodaySummary.Association("actual", "retained:v1", "session"))

        override fun primaryFocus() = selectedFocus

        override fun profile() = savedProfile

        override fun profileDraft() = draft

        override fun importWindow() = covered

        override fun shoeDiscomfort() = setOf("retained-run")

        override fun interruption(): WeeklyCoaching.Interruption? = null

        override fun sessionDecisions() = emptyList<WorkoutDecision>()

        override fun reviewKnown(id: String) = reviews.containsKey(id)

        override fun saveReview(review: WeeklyCoaching.Review) {
            assertNotSame(Looper.getMainLooper(), Looper.myLooper())
            assertNull(reviews.put(requireNotNull(review.id()), review))
            events += "save-review"
        }

        override fun decisionForReview(review: WeeklyCoaching.Review): String? = null

        override fun reconcileReminders() {
            assertNotSame(Looper.getMainLooper(), Looper.myLooper())
            events += "reminders"
        }

        override fun reminderPreferences() = ReminderSettings.defaults("Europe/London")

        override fun weights() = emptyList<org.racedaydreams.domain.body.BodyWeight>()

        override fun historicalStrength() = emptyList<HistoricalStrengthRecord>()

        override fun strengthLinks() = mapOf("actual" to LocalDate.of(2026, 11, 16))

        override fun gymLinks() = links

        override fun gymProgramme(): GymProgramme? {
            legacyReads++
            return null
        }

        override fun gymVersions() = emptyList<GymProgramme>()

        override fun gymDecisions() = emptyList<LegacyGymProgrammePayloadJson.Decision>()

        override fun exerciseChoices() = listOf(SqliteStrengthChoices.Choice("plank", true, false))

        override fun exerciseExclusions() =
            listOf(
                ExerciseExclusionRepository.Exclusion(
                    "bench-press",
                    ExerciseExclusionRepository.Reason.PREFERENCE,
                    Instant.parse("2026-11-16T00:00:00Z"),
                )
            )

        override fun acknowledgedPbs() = setOf("retained-pb")

        override fun healthAvailability() = availability

        override suspend fun grantedHealthPermissions(): Set<String> {
            permissionReads++
            permissionFailure?.let { throw it }
            return permissions
        }
    }

    private fun database(
        block: (SqlDatabase, Sources, org.racedaydreams.domain.coaching.CoachingRepository) -> Unit
    ) {
        val original = SQLiteDatabase.create(null)
        val db = AndroidSqlConnection { original }
        try {
            createAndroidSchemaFixture(original)
            ApplicationSchema.open(db) {
                error("The original Android fixture is already versioned.")
            }
            var ids = 0
            block(
                db,
                Sources(),
                SqliteCoachingRepository(
                    db,
                    { now },
                    { "chosen-${++ids}" },
                    PortableLegacyStrengthFingerprint,
                ),
            )
        } finally {
            original.close()
        }
    }

    private fun reader(
        db: SqlDatabase,
        sources: Sources,
        coaching: CoachingRepository,
        savedDraft: () -> String? = { null },
    ): SharedCoachSnapshotReader =
        SharedCoachSnapshotReader(
            SqliteActivities(db) { now },
            SqliteResults(db),
            SqliteShoes(db),
            SqliteBaselines(db) { now },
            SqliteSettings(db),
            SqlitePlannedWorkouts(db),
            sources,
            coaching,
            savedDraft,
            null,
            { now },
            { day },
        )

    @Test
    fun strengthOnlySnapshotNeedsNoRaceOrHealthSetupAndDoesNotCreateTrainingEvidence() =
        database { db, sources, coaching ->
            val before = db.query("SELECT total_changes()")
            val state = runBlocking { reader(db, sources, coaching).read() }
            assertFalse(state.loading)
            assertNull(state.profile)
            assertEquals(PrimaryFocus.STRENGTH, state.primaryFocus)
            assertNull(state.currentPlan)
            assertTrue(state.activities.isEmpty())
            assertTrue(state.strengthSessions.isEmpty())
            assertNull(state.baseline.easyPaceSecondsPerKm())
            assertFalse(state.performanceEligible)
            assertEquals(0, sources.permissionReads)
            assertEquals(before, db.query("SELECT total_changes()"))
            assertEquals(listOf("reminders"), sources.events)
        }

    @Test
    fun permissionsAreReadOnlyWhenAvailableAndFailuresRemainUnknown() =
        database { db, sources, coaching ->
            sources.availability = HealthAvailability.AVAILABLE
            sources.permissionFailure = SecurityException("Permission revoked")
            val state = runBlocking { reader(db, sources, coaching).read() }
            assertEquals(1, sources.permissionReads)
            assertEquals(HealthAvailability.AVAILABLE, state.healthAvailability)
            assertTrue(state.healthPermissions.isEmpty())
            assertTrue(state.weights.isEmpty())
            assertFalse(state.performanceEligible)
        }

    @Test
    fun grantedPermissionsAreCopiedWithoutInferringActivityOrEligibility() =
        database { db, sources, coaching ->
            sources.availability = HealthAvailability.AVAILABLE
            sources.permissions = setOf("actual-granted-read")
            val state = runBlocking { reader(db, sources, coaching).read() }
            assertEquals(sources.permissions, state.healthPermissions)
            assertTrue(state.activities.isEmpty())
            assertFalse(state.performanceEligible)
            assertEquals(1, sources.permissionReads)
        }

    @Test
    fun baselineUsesActualImportCoverageWithinTheOriginalEightyFourDayWindow() =
        database { db, sources, coaching ->
            val reader = reader(db, sources, coaching)
            val recent = now.minusSeconds(10 * 86400L)
            sources.covered = recent to now
            assertEquals(recent, runBlocking { reader.read() }.baseline.windowStart())
            sources.covered = now.minusSeconds(100 * 86400L) to now
            val older = runBlocking { reader.read() }.baseline
            assertEquals(now.minusSeconds(84 * 86400L), older.windowStart())
            assertEquals(now, older.windowEnd())
            sources.covered = null
            assertEquals(older.windowStart(), runBlocking { reader.read() }.baseline.windowStart())
        }

    @Test
    fun snapshotRetainsActualChoicesProvenanceUnitsAndSavedInputPriority() =
        database { db, sources, coaching ->
            val settings = SqliteSettings(db)
            settings.putSetting("mass.unit", "LB")
            settings.putSetting("calendar.view", "Month")
            settings.putSetting("milestones.enabled", "true")
            settings.putSetting("baseline.reviewed", "true")
            val state = runBlocking { reader(db, sources, coaching) { "In-progress input" }.read() }
            assertEquals("In-progress input", state.draft)
            assertEquals("Month", state.calendarView)
            assertEquals(org.racedaydreams.domain.body.MassUnit.LB, state.massUnit)
            assertTrue(state.milestonesEnabled)
            assertTrue(state.baselineReviewed)
            assertEquals(sources.links, state.gymLinks)
            assertEquals(sources.strengthLinks(), state.strengthPlannedDates)
            assertEquals(sources.exerciseChoices(), state.exerciseChoices)
            assertEquals(sources.exerciseExclusions(), state.exerciseExclusions)
            assertEquals(sources.acknowledgedPbs(), state.acknowledgedPbs)
            assertEquals(sources.shoeDiscomfort(), state.shoeDiscomfort)
            assertEquals(sources.reminderPreferences(), state.reminderPreferences)
            assertEquals(
                "Persisted onboarding draft",
                runBlocking { reader(db, sources, coaching).read() }.draft,
            )
            settings.putSetting("mass.unit", "future-value")
            assertEquals(
                org.racedaydreams.domain.body.MassUnit.KG,
                runBlocking { reader(db, sources, coaching).read() }.massUnit,
            )
        }

    @Test
    fun repeatedRefreshStoresOneOptionalWeeklyReviewBeforeRemindersAndNeverActivatesIt() =
        database { db, sources, coaching ->
            val profile =
                UserProfile(
                    RaceGoal(RaceDistance.GENERAL_FITNESS, "", null, GoalMode.FINISH, null),
                    TrainingPreferences(
                        TrainingStrategy.BALANCED,
                        setOf(java.time.DayOfWeek.TUESDAY),
                        1,
                        java.time.DayOfWeek.TUESDAY,
                        emptySet(),
                        ScheduleType.FLEXIBLE,
                        UnitSystem.METRIC,
                    ),
                    false,
                )
            val original =
                PlanVersion(
                    "retained",
                    "plan",
                    1,
                    now,
                    "Athlete chose",
                    profile,
                    "Actual evidence",
                    TrainingPlan("plan", day, day.plusDays(28), emptyList(), emptyList()),
                )
            coaching.saveCurrent(original)
            sources.savedProfile = profile
            val reader = reader(db, sources, coaching)
            val first = runBlocking { reader.read() }
            val second = runBlocking { reader.read() }
            assertEquals(listOf("save-review", "reminders", "reminders"), sources.events)
            assertEquals(1, sources.reviews.size)
            assertEquals(first.weeklyReview, second.weeklyReview)
            assertNotNull(first.weeklyReview)
            assertNull(second.weeklyDecision)
            assertEquals(original, second.currentPlan)
            assertEquals(listOf(original), coaching.versions())
            assertTrue(coaching.responses().isEmpty())
            assertTrue(db.query("PRAGMA foreign_key_check").isEmpty())
        }
}
