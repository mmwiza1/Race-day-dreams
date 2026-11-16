package org.racedaydreams.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.GymWorkoutAssociation.Choice
import org.racedaydreams.storage.*

@RunWith(AndroidJUnit4::class)
class SharedGymAssociationCommandsTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val at = Instant.parse("2026-11-15T09:00:00.123456789Z")
    private val day = LocalDate.of(2026, 11, 15)
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
    private lateinit var db: android.database.sqlite.SQLiteDatabase
    private lateinit var original: SqliteGymAssociationRepository
    private lateinit var shared: SqliteGymAssociations
    private lateinit var gym: SqliteGymExecutionRepository

    @Before
    fun prepare() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
        db = ProfileDatabase.get(context).writableDatabase
        original = SqliteGymAssociationRepository(context)
        shared = SqliteGymAssociations(AndroidSqlTestConnection(db)) { at.plusSeconds(10000) }
        gym = SqliteGymExecutionRepository(context)
    }

    @After
    fun cleanup() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
    }

    private fun legacy(version: Int = 1) =
        GymProgramme(
            "legacy",
            version,
            at,
            "Athlete chose",
            GymProgramme.Settings(
                GymProgramme.Goal.STRENGTH,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(DayOfWeek.SUNDAY),
                1,
                setOf("full gym"),
                45,
                GymProgramme.Effort.RPE,
            ),
            day,
            day.plusDays(41),
            listOf(
                GymProgramme.Session(
                    "session",
                    day,
                    "Upper",
                    listOf(
                        GymProgramme.Prescription(
                            "bench-press",
                            2,
                            4,
                            6,
                            null,
                            null,
                            6,
                            8,
                            180,
                            "Practice",
                            82.25,
                        )
                    ),
                    "Preserved",
                )
            ),
            emptyList(),
        )

    private fun authored(): StrengthProgramme {
        val slots =
            listOf(
                GymWorkoutDefinition.Slot(
                    "slot",
                    bench,
                    GymWorkoutDefinition.Target(
                        2,
                        4,
                        6,
                        null,
                        null,
                        GymWorkoutDefinition.EffortRange(
                            GymWorkoutDefinition.EffortScale.RIR,
                            2.5,
                            4.5,
                        ),
                        180,
                        82.25,
                    ),
                    null,
                )
            )
        return StrengthProgramme(
            "authored",
            1,
            0,
            at,
            "Authored",
            StrengthProgramme.Origin.CUSTOM,
            null,
            StrengthProgramme.Settings(
                GymProgramme.Goal.STRENGTH,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(DayOfWeek.SUNDAY),
                1,
                60,
                ExerciseMetadata.Equipment.entries.toSet(),
            ),
            listOf(
                StrengthProgramme.Block(
                    "block",
                    StrengthProgramme.Phase.DEVELOPMENT,
                    "Development",
                    day,
                    1,
                    listOf(
                        StrengthProgramme.Workout(
                            "session",
                            day,
                            StrengthWorkoutBudget.workoutMinutes(slots),
                            GymWorkoutDefinition("session", 1, at, "Upper", slots),
                            null,
                        )
                    ),
                    "Practice",
                )
            ),
            day,
            "Athlete chose",
            emptyList(),
        )
    }

    private fun install(authored: Boolean) {
        if (authored)
            SqliteStrengthProgrammingRepository(context, Clock.fixed(at, ZoneOffset.UTC))
                .activate(authored(), null, "activate", emptyList())
        else GymProgrammeStore(context).save(legacy())
    }

    private fun tick(ms: Long) = GymExecution.Tick("boot", ms, at.plusMillis(ms))

    private fun finish(id: String = "actual") {
        val e =
            GymExecutionEngine.start(
                id,
                GymExecution.Origin.FREE,
                null,
                null,
                ZoneOffset.UTC,
                tick(0),
            )
        val initial = gym.start(e.snapshot())!!
        e.addExercises(listOf(GymWorkoutDefinition.Slot("slot", bench, null, null)), tick(10))
        e.saveSet(
            "slot",
            StrengthSession.SetEntry("set-$id", 6, 80.0, null, false, null, null, 2.5),
            tick(1000),
        )
        gym.finish(e.finish(tick(2000), null, "Recorded 🏋️\u0000", true), initial.revision())
    }

    private fun choice(id: String, p: GymWorkoutAssociation.Proposal, count: Boolean = true) =
        GymWorkoutAssociation.Decision(
            id,
            p,
            if (count) Choice.COUNT_TOWARD_SESSION else Choice.KEEP_EXTRA,
            at.plusSeconds(5),
        )

    @Test
    fun originalAndSharedChoicesAreInterchangeableForBothProgrammeFamilies() {
        for (authored in listOf(false, true)) {
            if (authored) {
                cleanup()
                prepare()
            }
            install(authored)
            finish()
            val before = gym.load("actual")
            val originalActuals = CoachingStore(context).strength()
            val p = original.proposals("actual", at.plusSeconds(3)).single()
            assertEquals(listOf(p), shared.proposals("actual", at.plusSeconds(3)))
            assertEquals(authored, p.authored())
            val count = choice("count", p)
            assertEquals(count, shared.decide(count))
            assertEquals(count, original.decide(count))
            assertEquals(original.current("actual"), shared.current("actual"))
            assertEquals(
                original.reviewCurrent("actual", at.plusSeconds(4)),
                shared.reviewCurrent("actual", at.plusSeconds(4)),
            )
            assertEquals(1, DevelopmentStore(context).gymLinks().size)
            val keep = choice("keep", shared.reviewCurrent("actual", at.plusSeconds(4))!!, false)
            assertEquals(keep, original.decide(keep))
            assertEquals(keep, shared.decide(keep))
            assertTrue(DevelopmentStore(context).gymLinks().isEmpty())
            assertEquals(before, gym.load("actual"))
            assertEquals(originalActuals, CoachingStore(context).strength())
        }
    }

    @Test
    fun bothImplementationsRejectCompetingTargetAndStaleProgrammeReview() {
        install(false)
        finish()
        finish("other")
        val p = shared.proposals("actual", at.plusSeconds(3)).single()
        val other = shared.proposals("other", at.plusSeconds(3)).single()
        original.decide(choice("count", p))
        assertThrows(IllegalArgumentException::class.java) {
            shared.decide(choice("compete", other))
        }
        assertThrows(IllegalArgumentException::class.java) {
            original.decide(choice("compete", other))
        }
        shared.decide(choice("keep", p, false))
        GymProgrammeStore(context).save(legacy(2))
        assertThrows(IllegalArgumentException::class.java) { shared.decide(choice("stale", other)) }
        assertThrows(IllegalArgumentException::class.java) {
            original.decide(choice("stale", other))
        }
        assertNull(shared.current("other"))
        assertTrue(DevelopmentStore(context).gymLinks().isEmpty())
    }

    @Test
    fun editedActualNeedsNewReviewWhileOriginalWorkoutAndChoicesRemainImmutable() {
        install(true)
        finish()
        val before = gym.load("actual")
        val p = shared.proposals("actual", at.plusSeconds(3)).single()
        shared.decide(choice("count", p))
        val edit = p.actual().copy(notes = "Athlete edited note")
        CoachingStore(context).saveStrength(edit)
        assertThrows(IllegalArgumentException::class.java) {
            original.decide(choice("stale", p, false))
        }
        assertThrows(IllegalArgumentException::class.java) {
            shared.decide(choice("stale", p, false))
        }
        val review = shared.reviewCurrent("actual", at.plusSeconds(4))!!
        assertEquals(edit, review.actual())
        assertEquals(original.reviewCurrent("actual", at.plusSeconds(4)), review)
        original.decide(choice("keep", review, false))
        assertEquals(original.current("actual"), shared.current("actual"))
        assertEquals(before, gym.load("actual"))
        assertEquals(
            edit,
            SqliteGymExecutionRecords(AndroidSqlTestConnection(db))
                .evidence("actual")!!
                .currentActual,
        )
        db.rawQuery("SELECT payload FROM strength_association_decisions WHERE id='count'", null)
            .use {
                assertTrue(it.moveToFirst())
                assertEquals(p, GymAssociationJson.read(it.getString(0)).proposal())
            }
    }

    @Test
    fun sharedChoicesRoundTripThroughOriginalBackupAndSharedArchiveGraphGuards() {
        for (authored in listOf(false, true)) {
            if (authored) {
                cleanup()
                prepare()
            }
            install(authored)
            finish()
            val p = shared.proposals("actual", at.plusSeconds(3)).single()
            shared.decide(choice("count", p))
            val originalArchive =
                GymAssociationArchiveJson.decode(
                    SqliteGymAssociationRepository.exportData(db).toString()
                )
            assertEquals(originalArchive, shared.archive())
            val e = gym.load("actual")!!.execution()
            val link =
                GymAssociationArchiveJson.Link(
                    "actual",
                    if (authored) "authored:v1" else "legacy:v1",
                    "session",
                    day.toString(),
                )
            GymAssociationArchiveJson.validate(
                shared.archive(),
                mapOf("actual" to e),
                mapOf("actual" to p.actual()),
                if (authored) emptyMap() else mapOf("legacy:v1" to legacy()),
                mapOf("actual" to link),
                if (authored) mapOf("authored:v1" to authored()) else emptyMap(),
            )
            val backups = SqliteBackupRepository(context)
            val export = backups.exportUserData()
            backups.restoreUserData(export, false)
            assertEquals(originalArchive, shared.archive())
            assertEquals(original.current("actual"), shared.current("actual"))
            assertEquals(e, gym.load("actual")!!.execution())
            shared.decide(
                choice("keep", shared.reviewCurrent("actual", at.plusSeconds(4))!!, false)
            )
            val keepArchive = shared.archive()
            backups.restoreUserData(backups.exportUserData(), false)
            assertEquals(keepArchive, shared.archive())
            assertEquals(2, keepArchive.decisions.size)
            assertTrue(DevelopmentStore(context).gymLinks().isEmpty())
        }
    }

    @Test
    fun nestedHistoryAboveStandaloneLimitMatchesOriginalBackupGraphValidation() {
        install(false)
        finish()
        val p = original.proposals("actual", at.plusSeconds(3)).single()
        val historical = p.copy(actual = p.actual().copy(notes = "n".repeat(10000)))
        val choices = List(1800) { choice("historical-$it", historical) }
        val archive =
            GymAssociationArchiveJson.Archive(choices, mapOf("actual" to "historical-1799"))
        val nested = GymAssociationArchiveJson.element(archive)
        val payload = nested.toString()
        assertTrue(
            payload.toByteArray(Charsets.UTF_8).size in
                (16 * 1024 * 1024 + 1) until (32 * 1024 * 1024)
        )
        val link =
            org.json
                .JSONObject()
                .put("id", "actual")
                .put("gymVersion", "legacy:v1")
                .put("gymSession", "session")
                .put("plannedDate", day.toString())
        SqliteGymAssociationRepository.validate(
            org.json.JSONObject(payload),
            mapOf("actual" to gym.load("actual")!!.execution()),
            mapOf("actual" to p.actual()),
            mapOf("legacy:v1" to legacy()),
            mapOf("actual" to link),
        )
        assertEquals(
            choices.sortedBy { it.id() },
            GymAssociationArchiveJson.decode(nested).decisions,
        )
        assertNull(shared.current("actual"))
        assertTrue(DevelopmentStore(context).gymLinks().isEmpty())
    }

    @Test
    fun sharedTargetFailureRollsBackOriginalVisibleDecisionProjectionAndActuals() {
        install(true)
        finish()
        val p = original.proposals("actual", at.plusSeconds(3)).single()
        val before = gym.load("actual")
        db.execSQL(
            "CREATE TRIGGER fail_target BEFORE INSERT ON strength_association_targets BEGIN SELECT RAISE(ABORT,'injected'); END"
        )
        assertThrows(android.database.sqlite.SQLiteException::class.java) {
            shared.decide(choice("count", p))
        }
        assertNull(original.current("actual"))
        assertTrue(DevelopmentStore(context).gymLinks().isEmpty())
        assertEquals(before, gym.load("actual"))
        db.rawQuery("SELECT COUNT(*) FROM strength_association_decisions", null).use {
            assertTrue(it.moveToFirst())
            assertEquals(0L, it.getLong(0))
        }
        db.execSQL("DROP TRIGGER fail_target")
        assertEquals(choice("count", p), original.decide(choice("count", p)))
    }
}
