package org.racedaydreams.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.*

@RunWith(AndroidJUnit4::class)
class SharedRunningCoachingTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val day = LocalDate.of(2026, 11, 16)
    private val at = Instant.parse("2026-11-15T09:00:00.123456789Z")
    private lateinit var db: SQLiteDatabase
    private lateinit var original: CoachingStore
    private lateinit var shared: SqliteRunningCoaching
    private lateinit var mirror: SqlitePlannedWorkouts

    @Test
    fun completeCoachingArchiveMatchesOriginalExportValidatorAndImmutablePayloads() {
        original.saveCurrent(version())
        original.saveResponse(RunExecution("run", "done", "v1", null, null, "Recorded evidence"))
        original.saveProposal(proposal())
        original.saveCurrent(version(2))
        val exercise =
            StrengthExercise(
                "press",
                "Press / reference",
                StrengthExercise.Region.UPPER,
                StrengthExercise.Mode.WEIGHTED,
                true,
            )
        val actual =
            StrengthSession(
                "actual",
                at,
                ZoneOffset.UTC,
                Duration.ofMinutes(30),
                7.5,
                "Recorded / 🏋️",
                listOf(
                    StrengthSession.ExerciseSets(
                        exercise,
                        listOf(StrengthSession.SetEntry("working", 6, 82.25, 7.5, false)),
                    )
                ),
            )
        original.saveStrength(actual)
        val feedback =
            """ {"ruleVersion":1,"score":null,"components":["historic component"],"extra":"retain"} """
        db.execSQL(
            "INSERT INTO run_feedback(activity_id,plan_version_id,payload) VALUES(?,?,?)",
            arrayOf("run", "v1", feedback),
        )
        val before = CoachingStore.exportData(db)
        val snapshot = SqliteCoachingArchive(AndroidSqlTestConnection(db)).snapshot()
        assertEquals(actual, snapshot.sessions["actual"])
        assertEquals("v2", snapshot.currentVersionId)
        assertNull(snapshot.proposals.single().decision)
        assertEquals(
            feedback,
            snapshot.rows.getValue(CoachingArchiveJson.Table.FEEDBACK).single()["payload"],
        )
        val out = CoachingArchiveJson.element(snapshot)
        assertEquals(Json.parseToJsonElement(before.toString()), out)
        CoachingStore.validate(JSONObject(out.toString()))
        assertEquals(
            out,
            CoachingArchiveJson.element(
                CoachingArchiveJson.decode(Json.parseToJsonElement(before.toString()) as JsonObject)
            ),
        )
        assertEquals(before.toString(), CoachingStore.exportData(db).toString())
        assertEquals("v2", original.current()?.id())
    }

    @Test
    fun corruptCoachingArchiveGraphsFailBothValidatorsBeforeAnyReplacement() {
        original.saveCurrent(version())
        original.saveProposal(proposal())
        val before = CoachingStore.exportData(db)
        val row = before.getJSONArray("coach_proposals").getJSONObject(0)
        val corrupt =
            listOf(
                JSONObject(before.toString()).put("version", 2),
                JSONObject(before.toString()).put("coach_plan_versions", JSONArray()),
                JSONObject(before.toString()).put("coach_proposals", JSONArray().put(row).put(row)),
                JSONObject(before.toString())
                    .put(
                        "coach_proposals",
                        JSONArray()
                            .put(
                                JSONObject(row.toString())
                                    .put("decision", "unknown")
                                    .put("result_version_id", "v1")
                            ),
                    ),
                JSONObject(before.toString())
                    .put(
                        "coach_proposals",
                        JSONArray().put(JSONObject(row.toString()).put("unexpected", true)),
                    ),
            )
        for (bad in corrupt) {
            assertTrue(runCatching { CoachingStore.validate(bad) }.isFailure)
            assertTrue(
                runCatching {
                        CoachingArchiveJson.decode(
                            Json.parseToJsonElement(bad.toString()) as JsonObject
                        )
                    }
                    .isFailure
            )
        }
        assertEquals(before.toString(), CoachingStore.exportData(db).toString())
    }

    @Before
    fun prepare() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
        db = ProfileDatabase.get(context).writableDatabase
        val bridge = AndroidSqlTestConnection(db)
        original = CoachingStore(context)
        shared = SqliteRunningCoaching(bridge, { at }, { "shared-next" })
        mirror = SqlitePlannedWorkouts(bridge)
    }

    @After
    fun cleanup() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
    }

    private fun workout(id: String = "done", date: LocalDate = day) =
        PlannedWorkout(
            id,
            date,
            WorkoutCategory.EASY_RUN,
            5.0,
            Duration.ofMinutes(30),
            listOf(
                WorkoutSegment(
                    WorkoutSegment.Kind.RUN,
                    null,
                    Duration.ofMinutes(30),
                    300.0,
                    360.0,
                    null,
                    4.5,
                    "Talk comfortably",
                )
            ),
            "retained 🏃\u0000 note",
        )

    private fun profile() =
        UserProfile(
            RaceGoal(RaceDistance.GENERAL_FITNESS, "", null, GoalMode.FINISH, null),
            TrainingPreferences(
                TrainingStrategy.BALANCED,
                setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                3,
                DayOfWeek.FRIDAY,
                setOf(DayOfWeek.TUESDAY),
                ScheduleType.FLEXIBLE,
                UnitSystem.METRIC,
            ),
            false,
        )

    private fun version(
        number: Int = 1,
        workouts: List<PlannedWorkout> = listOf(workout(), workout("future", day.plusDays(1))),
    ) =
        PlanVersion(
            "v$number",
            "plan",
            number,
            at,
            "Accepted",
            profile(),
            "snapshot",
            TrainingPlan(
                "plan",
                day,
                day.plusDays(7),
                listOf(
                    TrainingPhase(
                        TrainingPhase.Kind.FOUNDATION,
                        day,
                        day.plusDays(7),
                        "Build a routine",
                    )
                ),
                workouts,
            ),
        )

    private fun proposal(id: String = "proposal") =
        AdjustmentProposal(
            id,
            "v1",
            "future",
            at,
            "Recorded context",
            listOf("Actual evidence"),
            "Keep or rest",
            "Limited",
            listOf(
                AdjustmentProposal.Option("keep", "Keep", AdjustmentProposal.Action.KEEP, null),
                AdjustmentProposal.Option("rest", "Rest", AdjustmentProposal.Action.REST, null),
            ),
        )

    @Test
    fun originalProfilePrescriptionAndVersionCodecsReadSharedOutputBothWays() {
        val workout = workout()
        val profile = profile()
        val version = version()
        assertEquals(workout, PlannedWorkoutJson.decode(FeatureJson.planned(workout).toString()))
        assertEquals(workout, FeatureJson.planned(JSONObject(PlannedWorkoutJson.encode(workout))))
        assertEquals(profile, ProfileJson.decode(FeatureJson.profile(profile).toString()))
        assertEquals(profile, FeatureJson.profile(JSONObject(ProfileJson.encode(profile))))
        assertEquals(
            version,
            RunningCoachingJson.decodeVersion(CoachingJson.version(version).toString()),
        )
        assertEquals(
            version,
            CoachingJson.version(JSONObject(RunningCoachingJson.encodeVersion(version))),
        )
    }

    @Test
    fun unchangedVersionsAcrossBothWritersDoNotDuplicateReminderRevisions() {
        original.saveCurrent(version())
        assertEquals(1L, mirror.plannedWorkoutRevision("done"))
        shared.saveCurrent(version(2))
        assertEquals(1L, mirror.plannedWorkoutRevision("done"))
        original.saveCurrent(version(3))
        assertEquals(1L, mirror.plannedWorkoutRevision("done"))
        val changed =
            listOf(workout(), workout("future", day.plusDays(1)).copy(note = "Future change"))
        shared.saveCurrent(version(4, changed))
        assertEquals(2L, mirror.plannedWorkoutRevision("future"))
        original.saveCurrent(version(5, changed))
        assertEquals(2L, mirror.plannedWorkoutRevision("future"))
        assertEquals(original.current(), shared.current())
        assertEquals(original.versions(), shared.versions())
        val legacyMirror = SqlitePlannedWorkoutRepository(context)
        assertEquals(legacyMirror.plannedWorkouts(), mirror.plannedWorkouts())
        legacyMirror.savePlannedWorkout(workout())
        assertEquals(2L, mirror.plannedWorkoutRevision("done"))
    }

    @Test
    fun bothStoresPreserveCompletedPrescriptionAndOriginalResponse() {
        original.saveCurrent(version())
        val response = RunExecution("actual", "done", "v1", 0, false, "entered")
        original.saveResponse(response)
        assertEquals(
            response,
            RunningCoachingJson.decodeResponse(CoachingJson.response(response).toString()),
        )
        assertEquals(
            response,
            CoachingJson.response(JSONObject(RunningCoachingJson.encodeResponse(response))),
        )
        val changed = listOf(workout().copy(distanceKm = 8.0), workout("future", day.plusDays(1)))
        assertThrows(IllegalArgumentException::class.java) {
            shared.saveCurrent(version(2, changed))
        }
        assertThrows(IllegalArgumentException::class.java) {
            original.saveCurrent(version(2, changed))
        }
        shared.saveCurrent(version(2))
        assertEquals(original.current(), shared.current())
        assertEquals(listOf(response), original.responses())
        assertEquals(original.responses(), shared.responses())
    }

    @Test
    fun explicitDecisionsInteroperateWithoutRewritingOriginalVersion() {
        original.saveCurrent(version())
        val first = proposal()
        shared.saveProposal(first)
        assertEquals(original.proposals(), shared.proposals())
        assertEquals(
            first,
            CoachingJson.proposal(JSONObject(RunningCoachingJson.encodeProposal(first))),
        )
        assertEquals(
            first,
            RunningCoachingJson.decodeProposal(CoachingJson.proposal(first).toString()),
        )
        shared.decide(first, "keep", emptySet())
        assertTrue(original.proposals().isEmpty())
        assertTrue(original.proposalKnown(first.id()))
        val second = proposal("second")
        original.saveProposal(second)
        original.decide(second, "rest", setOf("done"))
        assertEquals(original.current(), shared.current())
        assertEquals(2, shared.current()!!.version())
        assertEquals(version(), shared.versions().last())
        assertEquals(listOf("done"), shared.current()!!.plan().workouts().map { it.id() })
    }

    @Test
    fun allSharedAndOriginalCoachingWritesJoinOuterRollback() {
        db.beginTransaction()
        try {
            shared.saveCurrent(version())
            original.saveProposal(proposal())
            mirror.savePlannedWorkout(workout())
        } finally {
            db.endTransaction()
        }
        assertNull(original.current())
        assertNull(shared.current())
        assertTrue(original.versions().isEmpty())
        assertTrue(mirror.plannedWorkouts().isEmpty())
    }

    @Test
    fun integerAndDecimalNegativeZeroMatchActualAndroidJsonReader() {
        val source =
            """{"id":"shoe","model":"Daily","startingDistanceMetres":-0,"retired":false,"defaultShoe":false}"""
        for (text in listOf(source, source.replace(":-0", ":-0.0"))) {
            val legacy = FeatureJson.shoe(JSONObject(text))
            val portable = ShoeJson.decode(text)
            assertEquals(
                legacy.startingDistanceMetres()!!.toBits(),
                portable.startingDistanceMetres()!!.toBits(),
            )
        }
    }
}
