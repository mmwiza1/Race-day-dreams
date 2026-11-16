package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.*
import org.racedaydreams.domain.time.*

abstract class ProgrammingRecordsRepositoryContract {
    protected abstract fun database(): SqlDatabase

    private val at = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")
    private val start = CalendarDates.of(2026, 11, 17)

    private fun programme(): StrengthProgramme {
        val settings =
            StrengthProgramme.Settings(
                GymProgramme.Goal.STRENGTH,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(DayOfWeek.TUESDAY),
                1,
                60,
                ExerciseMetadata.Equipment.entries.toSet(),
            )
        val target =
            GymWorkoutDefinition.Target(
                2,
                4,
                6,
                null,
                null,
                GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RIR, 2.5, 4.5),
                180,
                82.25,
            )
        val exercise =
            ExerciseCatalog.definition("bench-press")!!.exercise()
                .copy(name = "Retained historical name")
        val workouts =
            List(2) { i ->
                val id = "workout-$i"
                StrengthProgramme.Workout(
                    id,
                    start.plusWeeks(i.toLong()),
                    30,
                    GymWorkoutDefinition(
                        id,
                        1,
                        at,
                        "Upper",
                        listOf(GymWorkoutDefinition.Slot("slot", exercise, target, null)),
                    ),
                    null,
                )
            }
        return StrengthProgramme(
            "programme",
            1,
            0,
            at,
            "Original",
            StrengthProgramme.Origin.CUSTOM,
            null,
            settings,
            listOf(
                StrengthProgramme.Block(
                    "block",
                    StrengthProgramme.Phase.DEVELOPMENT,
                    "Development",
                    start,
                    2,
                    workouts,
                    "Original explanation",
                )
            ),
            start,
            "Athlete authored",
            emptyList(),
        )
    }

    private fun <T> storage(block: (SqlDatabase, SqliteStrengthProgrammingRecords) -> T): T {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "test-install")
            }
            return block(db, SqliteStrengthProgrammingRecords(db))
        } finally {
            db.close()
        }
    }

    private fun values(vararg text: String) = text.map { SqlValue.Text(it) }

    private fun seedProgramme(db: SqlDatabase, p: StrengthProgramme = programme()) {
        val root = StrengthProgrammingPayloadJson.element(p)
        db.execute(
            "INSERT INTO strength_programme_revisions(programme_id,revision,parent_revision,name,start_date,end_date,header) VALUES(?,?,?,?,?,?,?)",
            listOf(
                SqlValue.Text(p.id()),
                SqlValue.Integer(p.revision().toLong()),
                SqlValue.Integer(p.parentRevision().toLong()),
            ) +
                values(
                    p.name(),
                    p.start().toString(),
                    p.end().toString(),
                    JsonObject(root - "blocks").toString(),
                ),
        )
        for ((i, b) in root.getValue("blocks").jsonArray.withIndex()) {
            val block = b.jsonObject
            db.execute(
                "INSERT INTO strength_programme_blocks(programme_id,revision,position,block_id,header) VALUES(?,?,?,?,?)",
                listOf(
                    SqlValue.Text(p.id()),
                    SqlValue.Integer(p.revision().toLong()),
                    SqlValue.Integer(i.toLong()),
                ) +
                    values(
                        block.getValue("id").jsonPrimitive.content,
                        JsonObject(block - "workouts").toString(),
                    ),
            )
            for ((n, w) in block.getValue("workouts").jsonArray.withIndex()) {
                val workout = w.jsonObject
                db.execute(
                    "INSERT INTO strength_programme_sessions(programme_id,revision,block_id,position,session_id,session_date,payload) VALUES(?,?,?,?,?,?,?)",
                    listOf(
                        SqlValue.Text(p.id()),
                        SqlValue.Integer(p.revision().toLong()),
                        SqlValue.Text(block.getValue("id").jsonPrimitive.content),
                        SqlValue.Integer(n.toLong()),
                    ) +
                        values(
                            workout.getValue("id").jsonPrimitive.content,
                            workout.getValue("date").jsonPrimitive.content,
                            workout.toString(),
                        ),
                )
            }
        }
    }

    private fun seedDraft(db: SqlDatabase): StrengthProgrammeDraft {
        val d =
            StrengthProgrammeDraft(
                "draft",
                3,
                "programme",
                0,
                "",
                programme().settings(),
                start,
                listOf(
                    StrengthProgrammeDraft.WeekBlock(
                        "b",
                        StrengthProgramme.Phase.FOUNDATION,
                        "Foundation",
                        3,
                        emptyList(),
                    )
                ),
                StrengthProgramme.Origin.CUSTOM,
                null,
            )
        val root = Json.parseToJsonElement(StrengthProgrammingPayloadJson.encodeDraft(d)).jsonObject
        db.execute(
            "INSERT INTO strength_programme_drafts(id,revision,programme_id,base_revision,name,start_date,header) VALUES('draft',3,'programme',0,'',?,?)",
            values(start.toString(), JsonObject(root - "blocks").toString()),
        )
        db.execute(
            "INSERT INTO strength_programme_draft_blocks(draft_id,position,payload) VALUES('draft',0,?)",
            values(root.getValue("blocks").jsonArray.single().toString()),
        )
        return d
    }

    private fun seedConcrete(db: SqlDatabase): ConcreteStrengthProgrammeDraft {
        val d = ConcreteStrengthProgrammeDraft("concrete", 5, null, programme(), emptyList())
        val root = Json.parseToJsonElement(ConcreteStrengthDraftPayloadJson.encode(d)).jsonObject
        val p = root.getValue("programme").jsonObject
        val header = JsonObject(root + ("programme" to JsonObject(p - "blocks")))
        db.execute(
            "INSERT INTO strength_concrete_drafts(id,revision,programme_id,programme_revision,name,start_date,end_date,header) VALUES('concrete',5,'programme',1,'Original',?,?,?)",
            values(
                d.programme().start().toString(),
                d.programme().end().toString(),
                header.toString(),
            ),
        )
        val b = p.getValue("blocks").jsonArray.single().jsonObject
        db.execute(
            "INSERT INTO strength_concrete_draft_blocks(draft_id,block_id,position,header) VALUES('concrete','block',0,?)",
            values(JsonObject(b - "workouts").toString()),
        )
        for ((n, w) in b.getValue("workouts").jsonArray.withIndex()) {
            val workout = w.jsonObject
            db.execute(
                "INSERT INTO strength_concrete_draft_workouts(draft_id,block_id,position,workout_id,workout_date,payload) VALUES('concrete','block',?,?,?,?)",
                listOf(SqlValue.Integer(n.toLong())) +
                    values(
                        workout.getValue("id").jsonPrimitive.content,
                        workout.getValue("date").jsonPrimitive.content,
                        workout.toString(),
                    ),
            )
        }
        return d
    }

    @Test
    fun normalizedProgrammeKeepsHistoricalDefinitionsAndExactOptionalTargets(): Unit =
        storage { db, records ->
            assertNull(records.programme("programme", 1))
            seedProgramme(db)
            assertEquals(programme(), records.programme("programme", 1))
            assertEquals(
                "Retained historical name",
                records
                    .programme("programme", 1)!!
                    .workouts()[0]
                    .definition()
                    .slots()
                    .single()
                    .exercise()
                    .name(),
            )
            assertEquals(
                listOf(Summary("programme", 1, "Original", start, programme().end())),
                records.versions(100, 0),
            )
            assertTrue(records.versions(1, 1).isEmpty())
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
            assertNull(records.currentReference())
        }

    @Test
    fun indexedProgrammeIdentityAndSummaryMismatchFailWithoutChangingRows(): Unit =
        storage { db, records ->
            seedProgramme(db)
            db.execute("UPDATE strength_programme_revisions SET name='Corrupt indexed name'")
            assertFailsWith<IllegalArgumentException> { records.programme("programme", 1) }
            assertEquals(
                "Corrupt indexed name",
                db.query("SELECT name FROM strength_programme_revisions")
                    .single()
                    .single()
                    .sqlText(),
            )
            db.execute(
                "UPDATE strength_programme_revisions SET name='Original',start_date='2026-12-31'"
            )
            assertFailsWith<IllegalArgumentException> { records.programme("programme", 1) }
            assertEquals(2, db.query("SELECT session_id FROM strength_programme_sessions").size)
        }

    @Test
    fun programmeBlockAndWorkoutOrderAndIdentityAreVerified(): Unit = storage { db, records ->
        seedProgramme(db)
        db.execute("UPDATE strength_programme_blocks SET position=2")
        assertFailsWith<IllegalArgumentException> { records.programme("programme", 1) }
        db.execute("UPDATE strength_programme_blocks SET position=0")
        db.execute(
            "UPDATE strength_programme_sessions SET session_date='2026-12-31' WHERE position=0"
        )
        assertFailsWith<IllegalArgumentException> { records.programme("programme", 1) }
        db.execute("DELETE FROM strength_programme_sessions WHERE position=0")
        assertFailsWith<IllegalArgumentException> { records.programme("programme", 1) }
        assertEquals(1, db.query("SELECT session_id FROM strength_programme_sessions").size)
    }

    @Test
    fun incompleteWeeklyDraftsStaySeparateAndCorruptIndexesReject(): Unit = storage { db, records ->
        assertNull(records.draft("draft"))
        val d = seedDraft(db)
        assertEquals(d, records.draft("draft"))
        assertEquals(listOf(DraftSummary("draft", 3, "", start)), records.drafts(10, 0))
        assertNull(records.currentReference())
        db.execute("UPDATE strength_programme_drafts SET revision=4")
        assertFailsWith<IllegalArgumentException> { records.draft("draft") }
        db.execute("UPDATE strength_programme_drafts SET revision=3")
        db.execute("UPDATE strength_programme_draft_blocks SET position=2")
        assertFailsWith<IllegalArgumentException> { records.draft("draft") }
    }

    @Test
    fun concretePreviewHasIndependentRevisionAndNeverBecomesCurrent(): Unit =
        storage { db, records ->
            assertNull(records.concreteDraft("concrete"))
            val d = seedConcrete(db)
            assertEquals(d, records.concreteDraft("concrete"))
            assertEquals(5, records.concreteDrafts(10, 0).single().revision())
            assertFalse(records.concreteDrafts(10, 0).single().revisionExists())
            seedProgramme(db)
            assertTrue(records.concreteDrafts(10, 0).single().revisionExists())
            assertEquals(d, records.concreteDraft("concrete"))
            assertNull(records.currentReference())
            db.execute("UPDATE strength_concrete_drafts SET programme_revision=2")
            assertFailsWith<IllegalArgumentException> { records.concreteDraft("concrete") }
            db.execute("UPDATE strength_concrete_drafts SET programme_revision=1")
            db.execute(
                "UPDATE strength_concrete_draft_workouts SET workout_id='corrupt' WHERE position=0"
            )
            assertFailsWith<IllegalArgumentException> { records.concreteDraft("concrete") }
        }

    @Test
    fun explicitCurrentReferencesKeepOwnersAndRejectTwoActivePointers(): Unit =
        storage { db, records ->
            seedProgramme(db)
            db.execute(
                "INSERT INTO gym_plan_versions(version_id,plan_id,version,payload) VALUES('legacy:v1','legacy',1,'{}')"
            )
            db.execute("INSERT INTO gym_current(id,version_id) VALUES(1,'legacy:v1')")
            assertEquals(Reference(Owner.LEGACY, "legacy", 1), records.currentReference())
            db.execute(
                "INSERT INTO strength_programme_current(id,programme_id,revision) VALUES(1,'programme',1)"
            )
            assertFailsWith<IllegalArgumentException> { records.currentReference() }
            db.execute("DELETE FROM gym_current")
            assertEquals(Reference(Owner.AUTHORED, "programme", 1), records.currentReference())
            assertEquals(1, db.query("SELECT version_id FROM gym_plan_versions").size)
        }

    @Test
    fun customMetadataAndDecisionHistoryUseRetainedIdentitiesAndOrder(): Unit =
        storage { db, records ->
            val e =
                StrengthExercise(
                    "custom-press",
                    "My press",
                    StrengthExercise.Region.UPPER,
                    StrengthExercise.Mode.WEIGHTED,
                    false,
                    StrengthExercise.MeasurementProfile.LOAD_REPS,
                )
            val c =
                CustomStrengthExercise(
                    e,
                    ExerciseMetadata.BodyArea.CHEST,
                    ExerciseMetadata.Role.HORIZONTAL_PUSH,
                    ExerciseMetadata.EquipmentRequirement(
                        listOf(setOf(ExerciseMetadata.Equipment.DUMBBELLS))
                    ),
                    "Athlete notes",
                )
            db.execute(
                "INSERT INTO strength_custom_exercises(id,payload) VALUES(?,?)",
                values(e.id(), StrengthProgrammingPayloadJson.encodeCustom(c)),
            )
            assertEquals(c, records.customExercise(e.id()))
            assertEquals(listOf(c), records.customExercises())
            assertNull(records.customExercise("missing"))
            val ref = Reference(Owner.AUTHORED, "programme", 1)
            val activate = Decision("first", null, ref, at, "Accepted", emptyList(), emptyList())
            val stop = Decision("second", ref, null, at, "Stopped", emptyList(), emptyList())
            for ((i, d) in listOf(activate, stop).withIndex()) db.execute(
                "INSERT INTO strength_programme_decisions(id,position,payload) VALUES(?,?,?)",
                listOf(SqlValue.Text(d.id()), SqlValue.Integer((i + 1).toLong())) +
                    values(StrengthProgrammingDecisionJson.encode(d)),
            )
            assertEquals(listOf(activate, stop), records.decisions())
            db.execute("UPDATE strength_custom_exercises SET id='custom-other'")
            assertFailsWith<IllegalArgumentException> { records.customExercises() }
            assertFailsWith<IllegalArgumentException> { records.customExercise("custom-other") }
        }

    @Test
    fun pagesAndIdentitiesAreBoundedAndReadsNeverActivateProgramming(): Unit =
        storage { _, records ->
            for ((limit, offset) in listOf(0 to 0, 101 to 0, 1 to -1)) {
                assertFailsWith<IllegalArgumentException> { records.versions(limit, offset) }
                assertFailsWith<IllegalArgumentException> { records.drafts(limit, offset) }
                assertFailsWith<IllegalArgumentException> { records.concreteDrafts(limit, offset) }
            }
            assertFailsWith<IllegalArgumentException> { records.concreteDrafts(1, 1000001) }
            assertFailsWith<IllegalArgumentException> { records.programme("programme", 0) }
            for (id in listOf("", " ", "x".repeat(257))) {
                assertFailsWith<IllegalArgumentException> { records.draft(id) }
                assertFailsWith<IllegalArgumentException> { records.customExercise(id) }
                assertFailsWith<IllegalArgumentException> { records.concreteDraft(id) }
            }
            assertNull(records.currentReference())
        }
}
