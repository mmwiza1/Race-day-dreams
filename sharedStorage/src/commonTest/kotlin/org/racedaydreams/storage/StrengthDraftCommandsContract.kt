package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.*
import org.racedaydreams.domain.time.*

abstract class StrengthDraftCommandsContract {
    protected abstract fun database(): SqlDatabase

    private val at = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")
    private val start = CalendarDates.of(2026, 11, 17)
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()

    private fun settings() =
        StrengthProgramme.Settings(
            GymProgramme.Goal.STRENGTH,
            GymProgramme.Priority.BALANCED,
            GymProgramme.Experience.INTERMEDIATE,
            setOf(DayOfWeek.TUESDAY),
            1,
            60,
            ExerciseMetadata.Equipment.entries.toSet(),
        )

    private fun target() =
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

    private fun draft(id: String = "draft", revision: Int = 1, exercise: StrengthExercise = bench) =
        StrengthProgrammeDraft(
            id,
            revision,
            "programme",
            0,
            "",
            settings(),
            start,
            listOf(
                StrengthProgrammeDraft.WeekBlock(
                    "b",
                    StrengthProgramme.Phase.FOUNDATION,
                    "Foundation",
                    2,
                    listOf(
                        StrengthProgrammeDraft.Day(
                            "day",
                            DayOfWeek.TUESDAY,
                            "Upper",
                            listOf(GymWorkoutDefinition.Slot("slot", exercise, null, null)),
                            null,
                        )
                    ),
                )
            ),
            StrengthProgramme.Origin.CUSTOM,
            null,
        )

    private fun programme(): StrengthProgramme {
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
                        listOf(GymWorkoutDefinition.Slot("slot", bench, target(), null)),
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
            settings(),
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

    private fun concrete(revision: Int = 1) =
        ConcreteStrengthProgrammeDraft("concrete", revision, null, programme(), emptyList())

    private fun custom(
        profile: StrengthExercise.MeasurementProfile = StrengthExercise.MeasurementProfile.LOAD_REPS
    ) =
        CustomStrengthExercise(
            StrengthExercise(
                "custom-${profile.name.lowercase().replace('_','-')}",
                "My press",
                StrengthExercise.Region.UPPER,
                if (profile.usesLoad()) StrengthExercise.Mode.WEIGHTED
                else StrengthExercise.Mode.BODYWEIGHT,
                false,
                profile,
            ),
            ExerciseMetadata.BodyArea.CHEST,
            ExerciseMetadata.Role.HORIZONTAL_PUSH,
            ExerciseMetadata.EquipmentRequirement(
                listOf(setOf(ExerciseMetadata.Equipment.DUMBBELLS))
            ),
            "Athlete notes 🏋️\u0000",
        )

    private fun texts(vararg values: String) = values.map { SqlValue.Text(it) }

    private fun <T> storage(
        block: (SqlDatabase, SqliteStrengthDrafts, SqliteStrengthProgrammingRecords) -> T
    ): T {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "test-install")
            }
            return block(db, SqliteStrengthDrafts(db), SqliteStrengthProgrammingRecords(db))
        } finally {
            db.close()
        }
    }

    private fun seedAccepted(db: SqlDatabase) {
        val p = programme()
        val root = StrengthProgrammingPayloadJson.element(p)
        db.execute(
            "INSERT INTO strength_programme_revisions(programme_id,revision,parent_revision,name,start_date,end_date,header) VALUES('programme',1,0,?,?,?,?)",
            texts(
                p.name(),
                p.start().toString(),
                p.end().toString(),
                JsonObject(root - "blocks").toString(),
            ),
        )
        val b = root.getValue("blocks").jsonArray.single().jsonObject
        db.execute(
            "INSERT INTO strength_programme_blocks(programme_id,revision,position,block_id,header) VALUES('programme',1,0,'block',?)",
            texts(JsonObject(b - "workouts").toString()),
        )
        for ((i, item) in b.getValue("workouts").jsonArray.withIndex()) {
            val w = item.jsonObject
            db.execute(
                "INSERT INTO strength_programme_sessions(programme_id,revision,block_id,position,session_id,session_date,payload) VALUES('programme',1,'block',?,?,?,?)",
                listOf(SqlValue.Integer(i.toLong())) +
                    texts(
                        w.getValue("id").jsonPrimitive.content,
                        w.getValue("date").jsonPrimitive.content,
                        w.toString(),
                    ),
            )
        }
        db.execute("INSERT INTO strength_programme_current VALUES(1,'programme',1)")
    }

    private fun mirror(db: SqlDatabase, id: String): StrengthExercise =
        StrengthEvidenceJson.decodeExercise(
            db.query("SELECT payload FROM strength_exercises WHERE id=?", texts(id))
                .single()
                .single()
                .sqlText()
        )

    @Test
    fun incompleteWeeklyDraftsKeepTargetsUnknownAndNeverActivate(): Unit =
        storage { db, commands, records ->
            val d = draft()
            commands.saveDraft(d, 0)
            commands.saveDraft(d, -7) // Existing identical lost acknowledgement is harmless.
            assertEquals(d, records.draft(d.id()))
            assertNull(
                records.draft(d.id())!!.blocks().single().days().single().slots().single().target()
            )
            val changed = d.copy(revision = 4, name = "Athlete changed")
            commands.saveDraft(changed, 1) // Weekly drafts historically allow a forward jump.
            assertEquals(changed, records.draft(d.id()))
            assertNull(records.currentReference())
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
            assertTrue(db.query("SELECT programme_id FROM strength_programme_revisions").isEmpty())
        }

    @Test
    fun staleWeeklyUpdatesAndDeletesPreserveTheLatestDraft(): Unit =
        storage { _, commands, records ->
            commands.saveDraft(draft(), 0)
            assertFailsWith<IllegalArgumentException> { commands.saveDraft(draft(revision = 2), 0) }
            assertFailsWith<IllegalArgumentException> {
                commands.saveDraft(draft().copy(name = "Same revision changed"), 1)
            }
            assertFailsWith<IllegalArgumentException> { commands.deleteDraft("draft", 2) }
            assertEquals(draft(), records.draft("draft"))
            commands.deleteDraft("draft", 1)
            commands.deleteDraft("draft", 99)
            assertNull(records.draft("draft"))
        }

    @Test
    fun weeklyBaseMustBeKnownAuthoredAndNeverAmbiguousLegacy(): Unit =
        storage { db, commands, records ->
            val d = draft().copy(baseRevision = 1)
            assertFailsWith<IllegalArgumentException> { commands.saveDraft(d, 0) }
            seedAccepted(db)
            commands.saveDraft(d, 0)
            assertEquals(d, records.draft("draft"))
            assertFailsWith<IllegalArgumentException> {
                commands.saveDraft(d.copy(revision = 2, baseRevision = 2), 1)
            }
            db.execute("INSERT INTO gym_plan_versions VALUES('programme:v1','programme',1,'{}')")
            assertFailsWith<IllegalArgumentException> {
                commands.saveDraft(d.copy(revision = 2), 1)
            }
            assertEquals(programme(), records.programme("programme", 1))
        }

    @Test
    fun customMetadataMustExistBeforeDraftUsesIt(): Unit = storage { _, commands, records ->
        val c = custom()
        val d = draft(exercise = c.exercise())
        assertFailsWith<IllegalArgumentException> { commands.saveDraft(d, 0) }
        commands.saveCustom(c, null)
        commands.saveDraft(d, 0)
        assertEquals(d, records.draft("draft"))
        assertEquals(c, records.customExercise(c.exercise().id()))
    }

    @Test
    fun customModalitiesPersistWithTheirLoggingMirrorWithoutCreatingEvidence(): Unit =
        storage { db, commands, records ->
            for (profile in StrengthExercise.MeasurementProfile.entries) {
                val c = custom(profile)
                commands.saveCustom(c, null)
                commands.saveCustom(c, c.copy(notes = "Stale expected but identical value"))
                assertEquals(c, records.customExercise(c.exercise().id()))
                assertEquals(c.exercise(), mirror(db, c.exercise().id()))
            }
            assertEquals(6, records.customExercises().size)
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
        }

    @Test
    fun customCompareAndSaveAndMeasurementGuardsPreserveActualHistory(): Unit =
        storage { db, commands, records ->
            val c = custom()
            commands.saveCustom(c, null)
            val actual =
                StrengthSession(
                    "actual",
                    at,
                    Offsets.ofTotalSeconds(0),
                    Durations.ofSeconds(1800, 0),
                    null,
                    "Retained evidence",
                    listOf(
                        StrengthSession.ExerciseSets(
                            c.exercise(),
                            listOf(
                                StrengthSession.SetEntry("set", 6, 82.25, 7.0, false, null, null)
                            ),
                        )
                    ),
                )
            val payload = StrengthEvidenceJson.encodeAcceptedSession(actual)
            db.execute(
                "INSERT INTO strength_sessions(id,started_at,payload) VALUES('actual',?,?)",
                texts(at.toString(), payload),
            )
            val renamed = c.copy(exercise = c.exercise().copy(name = "New display name"))
            assertFailsWith<IllegalArgumentException> { commands.saveCustom(renamed, null) }
            commands.saveCustom(renamed, c)
            val changedTask =
                renamed.copy(
                    exercise =
                        renamed
                            .exercise()
                            .copy(measurement = StrengthExercise.MeasurementProfile.LOAD_DURATION)
                )
            assertFailsWith<IllegalArgumentException> { commands.saveCustom(changedTask, renamed) }
            assertFailsWith<IllegalArgumentException> {
                commands.saveCustom(
                    renamed.copy(
                        exercise =
                            renamed
                                .exercise()
                                .copy(mode = StrengthExercise.Mode.WEIGHTED_BODYWEIGHT)
                    ),
                    renamed,
                )
            }
            assertEquals(renamed, records.customExercise(c.exercise().id()))
            assertEquals(
                payload,
                db.query("SELECT payload FROM strength_sessions WHERE id='actual'")
                    .single()
                    .single()
                    .sqlText(),
            )
        }

    @Test
    fun loggingEditsKeepTypedMetadataAndRefuseConflictingMirrors(): Unit =
        storage { db, commands, records ->
            val c = custom()
            assertFalse(commands.updateLoggingDefinition(bench))
            commands.saveCustom(c, null)
            val renamed = c.exercise().copy(name = "Renamed through logging")
            assertTrue(commands.updateLoggingDefinition(renamed))
            assertEquals(c.copy(exercise = renamed), records.customExercise(c.exercise().id()))
            assertEquals(renamed, mirror(db, c.exercise().id()))
            db.execute(
                "UPDATE strength_exercises SET payload=? WHERE id=?",
                texts(
                    StrengthEvidenceJson.encodeExercise(renamed.copy(name = "Conflicting mirror")),
                    renamed.id(),
                ),
            )
            assertFailsWith<IllegalArgumentException> {
                commands.saveCustom(c, c.copy(exercise = renamed))
            }
            assertEquals("Conflicting mirror", mirror(db, renamed.id()).name())
            assertEquals(c.copy(exercise = renamed), records.customExercise(renamed.id()))
        }

    @Test
    fun customMirrorInsertFailureAndOuterRollbackRestoreEveryRecord(): Unit =
        storage { db, commands, records ->
            val c = custom()
            commands.saveCustom(c, null)
            db.execute(
                "CREATE TRIGGER fail_mirror BEFORE INSERT ON strength_exercises BEGIN SELECT RAISE(ABORT,'test failure'); END"
            )
            assertFails { commands.saveCustom(c.copy(notes = "Changed"), c) }
            assertEquals(c, records.customExercise(c.exercise().id()))
            assertEquals(c.exercise(), mirror(db, c.exercise().id()))
            db.execute("DROP TRIGGER fail_mirror")
            assertFails {
                db.transaction {
                    commands.saveCustom(c.copy(notes = "Changed"), c)
                    commands.saveDraft(draft(), 0)
                    error("Outer failure")
                }
            }
            assertEquals(c, records.customExercise(c.exercise().id()))
            assertNull(records.draft("draft"))
        }

    @Test
    fun weeklyChildReplacementRollsBackHeaderAndDeletedChildrenOnFailure(): Unit =
        storage { db, commands, records ->
            commands.saveDraft(draft(), 0)
            db.execute(
                "CREATE TRIGGER fail_weekly_block BEFORE INSERT ON strength_programme_draft_blocks BEGIN SELECT RAISE(ABORT,'test failure'); END"
            )
            assertFails { commands.saveDraft(draft(revision = 2).copy(name = "Changed"), 1) }
            assertEquals(draft(), records.draft("draft"))
        }

    @Test
    fun concreteDraftRevisionsAreSequentialAndSeparateFromCandidateProgramming(): Unit =
        storage { db, commands, records ->
            commands.saveConcrete(concrete(), 0)
            commands.saveConcrete(concrete(), -7)
            assertFailsWith<IllegalArgumentException> { commands.saveConcrete(concrete(3), 1) }
            val changed = concrete(2).copy(programme = programme().copy(name = "Edited preview"))
            commands.saveConcrete(changed, 1)
            assertEquals(changed, records.concreteDraft("concrete"))
            assertEquals(1, changed.programme().revision())
            assertNull(records.currentReference())
            assertTrue(db.query("SELECT programme_id FROM strength_programme_revisions").isEmpty())
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
        }

    @Test
    fun concreteIdentityAndStaleDeleteGuardsDoNotDiscardTheSavedPreview(): Unit =
        storage { _, commands, records ->
            commands.saveConcrete(concrete(), 0)
            assertFailsWith<IllegalArgumentException> {
                commands.saveConcrete(
                    concrete(2).copy(programme = programme().copy(id = "different")),
                    1,
                )
            }
            assertFailsWith<IllegalArgumentException> {
                commands.saveConcrete(
                    concrete(2).copy(before = Reference(Owner.LEGACY, "legacy", 1)),
                    1,
                )
            }
            assertFailsWith<IllegalArgumentException> { commands.deleteConcrete("concrete", 2) }
            assertEquals(concrete(), records.concreteDraft("concrete"))
            commands.deleteConcrete("concrete", 1)
            assertNull(records.concreteDraft("concrete"))
            assertFailsWith<IllegalArgumentException> { commands.deleteConcrete("concrete", 1) }
        }

    @Test
    fun acceptedRevisionsCannotBeRewrittenAsDraftsAndDeletingPreviewRetainsHistory(): Unit =
        storage { db, commands, records ->
            commands.saveConcrete(concrete(), 0)
            seedAccepted(db)
            commands.saveConcrete(concrete(), 0) // Retry is not a new activation or edit.
            assertFailsWith<IllegalArgumentException> {
                commands.saveConcrete(
                    concrete(2)
                        .copy(programme = programme().copy(name = "Changed accepted history")),
                    1,
                )
            }
            commands.deleteConcrete("concrete", 1)
            assertEquals(programme(), records.programme("programme", 1))
            assertEquals(Reference(Owner.AUTHORED, "programme", 1), records.currentReference())
        }

    @Test
    fun concreteBaseReferencesAndHistoricalPrescriptionsAreChecked(): Unit =
        storage { db, commands, records ->
            val base = programme()
            val ref = Reference(Owner.AUTHORED, "programme", 1)
            val forward =
                ConcreteStrengthProgrammeDraft(
                    "forward",
                    1,
                    ref,
                    base.copy(
                        revision = 2,
                        parentRevision = 1,
                        effectiveFrom = base.workouts()[1].date(),
                    ),
                    emptyList(),
                )
            assertFailsWith<IllegalArgumentException> { commands.saveConcrete(forward, 0) }
            seedAccepted(db)
            commands.saveConcrete(forward, 0)
            val past =
                base
                    .workouts()[0]
                    .copy(
                        definition =
                            base.workouts()[0].definition().copy(title = "Retrospective rewrite")
                    )
            val invalid =
                forward
                    .programme()
                    .copy(
                        blocks =
                            listOf(
                                base
                                    .blocks()
                                    .single()
                                    .copy(workouts = listOf(past, base.workouts()[1]))
                            )
                    )
            assertFailsWith<IllegalArgumentException> {
                commands.saveConcrete(forward.copy(revision = 2, programme = invalid), 1)
            }
            assertEquals(forward, records.concreteDraft("forward"))
            assertEquals(base, records.programme("programme", 1))
        }

    @Test
    fun legacyReplacementReferencesAndMissingCustomMetadataStayExplicit(): Unit =
        storage { db, commands, records ->
            val replacement = concrete().copy(before = Reference(Owner.LEGACY, "legacy", 1))
            assertFailsWith<IllegalArgumentException> { commands.saveConcrete(replacement, 0) }
            db.execute("INSERT INTO gym_plan_versions VALUES('legacy:v1','legacy',1,'{}')")
            db.execute("INSERT INTO gym_current VALUES(1,'legacy:v1')")
            commands.saveConcrete(replacement, 0)
            assertEquals(Reference(Owner.LEGACY, "legacy", 1), records.currentReference())
            val c = custom()
            val w = programme().workouts()[0]
            val changed =
                w.copy(
                    definition =
                        w.definition()
                            .copy(
                                slots =
                                    listOf(
                                        w.definition()
                                            .slots()
                                            .single()
                                            .copy(exercise = c.exercise())
                                    )
                            )
                )
            val p =
                programme()
                    .copy(
                        blocks =
                            listOf(
                                programme()
                                    .blocks()
                                    .single()
                                    .copy(workouts = listOf(changed, programme().workouts()[1]))
                            )
                    )
            val d = concrete(2).copy(before = replacement.before(), programme = p)
            assertFailsWith<IllegalArgumentException> { commands.saveConcrete(d, 1) }
            commands.saveCustom(c, null)
            commands.saveConcrete(d, 1)
            assertEquals(d, records.concreteDraft("concrete"))
        }

    @Test
    fun concreteChildFailureRollsBackDeletedRowsAndIndependentHeaderRevision(): Unit =
        storage { db, commands, records ->
            commands.saveConcrete(concrete(), 0)
            db.execute(
                "CREATE TRIGGER fail_concrete_workout BEFORE INSERT ON strength_concrete_draft_workouts BEGIN SELECT RAISE(ABORT,'test failure'); END"
            )
            assertFails {
                commands.saveConcrete(
                    concrete(2).copy(programme = programme().copy(name = "Changed")),
                    1,
                )
            }
            assertEquals(concrete(), records.concreteDraft("concrete"))
            assertEquals(
                2,
                db.query("SELECT workout_id FROM strength_concrete_draft_workouts").size,
            )
        }

    @Test
    fun exactDraftIdentitiesAndOuterTransactionArePreserved(): Unit =
        storage { db, commands, records ->
            val d = draft(id = "kept\u0000🏋️")
            commands.saveDraft(d, 0)
            assertEquals(d, records.draft(d.id()))
            assertFails {
                db.transaction {
                    commands.saveConcrete(concrete(), 0)
                    commands.saveDraft(d.copy(revision = 2), 1)
                    error("Outer failure")
                }
            }
            assertNull(records.concreteDraft("concrete"))
            assertEquals(d, records.draft(d.id()))
        }

    @Test
    fun concreteQuotaAllowsExistingEditsAndDeleteBeforeAnotherPreview(): Unit =
        storage { db, commands, records ->
            db.transaction {
                repeat(1000) { commands.saveConcrete(concrete().copy(id = "candidate-$it"), 0) }
            }
            assertFailsWith<IllegalArgumentException> { commands.saveConcrete(concrete(), 0) }
            val edited =
                concrete(2)
                    .copy(
                        id = "candidate-9",
                        programme = programme().copy(name = "Edited existing preview"),
                    )
            commands.saveConcrete(edited, 1)
            assertEquals(edited, records.concreteDraft(edited.id()))
            commands.deleteConcrete("candidate-0", 1)
            commands.saveConcrete(concrete(), 0)
            assertEquals(
                1000L,
                db.query("SELECT COUNT(*) FROM strength_concrete_drafts")
                    .single()
                    .single()
                    .sqlLong(),
            )
            assertNull(records.currentReference())
        }

    @Test
    fun concreteRevisionOverflowCannotWrapOrPartiallyRewriteAPreview(): Unit =
        storage { db, commands, records ->
            commands.saveConcrete(concrete(), 0)
            val root =
                Json.parseToJsonElement(
                        ConcreteStrengthDraftPayloadJson.encode(concrete(Int.MAX_VALUE))
                    )
                    .jsonObject
            val p = root.getValue("programme").jsonObject
            val header = JsonObject(root + ("programme" to JsonObject(p - "blocks")))
            db.execute(
                "UPDATE strength_concrete_drafts SET revision=?,header=? WHERE id='concrete'",
                listOf(SqlValue.Integer(Int.MAX_VALUE.toLong()), SqlValue.Text(header.toString())),
            )
            assertFailsWith<ArithmeticException> {
                commands.saveConcrete(
                    concrete(Int.MAX_VALUE)
                        .copy(programme = programme().copy(name = "Overflow attempt")),
                    Int.MAX_VALUE,
                )
            }
            assertEquals(concrete(Int.MAX_VALUE), records.concreteDraft("concrete"))
        }

    @Test
    fun concreteCandidateCannotReuseALegacyProgrammeIdentity(): Unit =
        storage { db, commands, records ->
            db.execute("INSERT INTO gym_plan_versions VALUES('programme:v1','programme',1,'{}')")
            assertFailsWith<IllegalArgumentException> { commands.saveConcrete(concrete(), 0) }
            assertNull(records.concreteDraft("concrete"))
            assertEquals(
                1L,
                db.query("SELECT COUNT(*) FROM gym_plan_versions").single().single().sqlLong(),
            )
        }
}
