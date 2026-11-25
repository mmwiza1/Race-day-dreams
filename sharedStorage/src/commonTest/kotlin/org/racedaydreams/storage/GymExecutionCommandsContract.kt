package org.racedaydreams.storage

import kotlin.test.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

abstract class GymExecutionCommandsContract {
    protected abstract fun database(): SqlDatabase

    private val at = StoredInstant.parse("2026-11-15T09:00:00.123456789Z")
    private val day = CalendarDates.of(2026, 11, 15)
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
    private val row = ExerciseCatalog.definition("dumbbell-row")!!.exercise()

    private fun tick(ms: Long, epoch: String = "boot") =
        GymExecution.Tick(
            epoch,
            9007199254741017L + ms,
            Instants.ofEpochSecond(at.getEpochSecond() + ms / 1000, at.getNano().toLong()),
        )

    private fun set(id: String, warm: Boolean = false) =
        StrengthSession.SetEntry(id, 6, 80.0, null, warm, null, null, 2.5)

    private fun free(id: String = "execution") =
        GymExecutionEngine.start(
            id,
            GymExecution.Origin.FREE,
            null,
            null,
            Offsets.ofTotalSeconds(19815),
            tick(0),
        )

    private fun add(e: GymExecutionEngine, grouped: Boolean = false) =
        e.addExercises(
            if (grouped)
                listOf(
                    GymWorkoutDefinition.Slot("a", bench, null, "pair"),
                    GymWorkoutDefinition.Slot("b", row, null, "pair"),
                )
            else listOf(GymWorkoutDefinition.Slot("a", bench, null, null)),
            tick(10),
        )

    /** Every contract case uses the production portable byte renderer and platform SHA-256. */
    private class ActualFingerprints :
        LegacyStrengthFingerprint by PortableLegacyStrengthFingerprint {
        fun hash(s: StrengthSession): String = fingerprint(s)
    }

    private fun <T> storage(block: (SqlDatabase, SqliteGymExecutions, ActualFingerprints) -> T): T {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "test-install")
            }
            val fingerprints = ActualFingerprints()
            return block(
                db,
                SqliteGymExecutions(db, { tick(10000000).wallTime() }, fingerprints),
                fingerprints,
            )
        } finally {
            db.close()
        }
    }

    private fun finish(r: SqliteGymExecutions, id: String = "execution"): GymExecution {
        val e = free(id)
        val start = r.start(e.snapshot())
        add(e)
        e.saveSet("a", set("working"), tick(1000))
        val ended = e.finish(tick(2000), null, "Recorded 🏋️\u0000", true)
        r.finish(ended, start.revision())
        return ended
    }

    private fun legacy() =
        GymProgramme(
            "legacy",
            1,
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
                    "Retained",
                )
            ),
            emptyList(),
        )

    private fun authored(): StrengthProgramme {
        val slots =
            listOf(
                GymWorkoutDefinition.Slot(
                    "a",
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
            "Upper",
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
                            GymWorkoutDefinition("workout", 1, at, "Upper", slots),
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

    private fun plan(db: SqlDatabase, authored: Boolean): GymWorkoutAdapter.PreparedWorkout =
        if (authored) {
            val p = authored()
            SqliteStrengthProgramming(db, { at }, { day })
                .activate(p, null, "activate", emptyList())
            GymWorkoutAdapter.fromAuthoredProgramme(p, "session")
        } else {
            val p = legacy()
            SqliteLegacyGymProgrammes(db, { at }, { "unused" }).save(p)
            GymWorkoutAdapter.fromProgramme(p, "session")
        }

    @Test
    fun startsRetriesOneUnfinishedAndCompletedIdentityStayIndependent(): Unit =
        storage { db, r, _ ->
            val initial = free().snapshot()
            assertEquals(r.start(initial), r.start(initial))
            assertEquals(1L, r.load("execution")!!.revision())
            assertFails { r.start(free("other").snapshot()) }
            assertEquals(1, r.unfinished().size)
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
            r.save(
                GymExecutionEngine.recover(initial, tick(100, "new")).discard(tick(200, "new")),
                1,
            )
            val actual = finish(r, "other").completedSession()!!
            assertFails { r.start(free(actual.id()).snapshot()) }
            assertEquals(
                setOf("other"),
                r.finishedActualIds(listOf("other", "execution", "missing")),
            )
        }

    @Test
    fun appendOnlySetsRecoveryPauseAndExactRevisionsSurviveReload(): Unit = storage { db, r, _ ->
        val e = free()
        r.start(e.snapshot())
        add(e, true)
        e.saveSet("a", set("warm", true), tick(100))
        val saved = r.save(e.snapshot(), 1)
        assertEquals(saved, r.save(e.snapshot(), 1))
        e.saveSet("a", set("working"), tick(1000))
        e.replaceExercise("a", row, tick(1100))
        e.saveSet("a", set("replacement"), tick(1200))
        e.breakSuperset("b", tick(1300))
        r.save(e.pause(tick(2000)), saved.revision())
        val previous = r.load("execution")!!
        val recovered = GymExecutionEngine.recover(previous.execution(), tick(900000, "new"))
        val resumed = recovered.resume(tick(900100, "new"))
        assertEquals(GymExecution.Status.ACTIVE, resumed.status())
        val latest = r.save(resumed, previous.revision())
        assertEquals(resumed, r.load("execution")!!.execution())
        assertEquals(9007199255641117L, latest.execution().lastTick().monotonicMillis())
        assertEquals(
            listOf(bench, bench, row),
            latest.execution().actualSets().map { it.exercise() },
        )
        assertEquals(3, db.query("SELECT sequence FROM strength_execution_sets").size)
        assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
    }

    @Test
    fun staleAndDivergingPrefixesCannotRewriteSavedSetsOrTerminalEvidence(): Unit =
        storage { _, r, _ ->
            val e = free()
            val initial = e.snapshot()
            r.start(initial)
            add(e)
            e.saveSet("a", set("first"), tick(100))
            val saved = r.save(e.snapshot(), 1)
            val branch = GymExecutionEngine.recover(initial, tick(0, "branch"))
            branch.resume(tick(1, "branch"))
            branch.addExercises(
                listOf(GymWorkoutDefinition.Slot("a", bench, null, null)),
                tick(10, "branch"),
            )
            branch.saveSet("a", set("different"), tick(100, "branch"))
            assertFails { r.save(branch.snapshot(), saved.revision()) }
            e.saveSet("a", set("next"), tick(200))
            assertFails { r.save(e.snapshot(), 1) }
            for (revision in listOf(0L, Long.MAX_VALUE)) assertFails {
                r.save(e.snapshot(), revision)
            }
            assertEquals(saved, r.load("execution"))
            val ended = e.finish(tick(1000), null, "Actual", false)
            val finished = r.finish(ended, saved.revision())
            assertEquals(finished, r.finish(ended, 1))
            assertFails { r.save(e.snapshot(), finished.revision()) }
            assertFails { r.finish(initial, finished.revision()) }
        }

    @Test
    fun actualsAreCreatedOnlyByAtomicFinishAndNeverFromPrescriptions(): Unit = storage { db, r, _ ->
        val e = free()
        r.start(e.snapshot())
        add(e)
        e.saveSet("a", set("warm", true), tick(100))
        e.saveSet("a", set("working"), tick(200))
        r.save(e.snapshot(), 1)
        assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
        val ended = e.finish(tick(1000), null, "Actual", true)
        assertFails { r.save(ended, 2) }
        val saved = r.finish(ended, 2)
        assertEquals(saved, r.finish(ended, 1))
        val evidence = SqliteGymExecutionRecords(db).evidence("execution")!!
        assertEquals(ended.completedSession(), evidence.currentActual)
        assertEquals(ended, evidence.original.execution())
        assertTrue(evidence.currentActual!!.exercises().single().sets().first().warmUp())
        assertTrue(db.query("SELECT id FROM coach_evidence WHERE kind='STRENGTH_LINK'").isEmpty())
    }

    @Test
    fun plannedBothFamiliesBindExactFrozenRevisionAndProjectWithoutInventedChoice(): Unit {
        for (authored in listOf(false, true)) storage { db, r, _ ->
            val p = plan(db, authored)
            val e =
                GymExecutionEngine.start(
                    "execution",
                    GymExecution.Origin.PLANNED,
                    p.definition(),
                    p.source(),
                    Offsets.ofTotalSeconds(0),
                    tick(0),
                )
            r.start(e.snapshot())
            e.saveSet(p.definition().slots().single().id(), set("working"), tick(100))
            val ended = e.finish(tick(1000), null, "Actual", true)
            r.finish(ended, 1)
            assertEquals(
                1,
                db.query("SELECT id FROM coach_evidence WHERE kind='STRENGTH_LINK'").size,
            )
            val link =
                db.query(
                        "SELECT programme_key,session_id,decision_id FROM strength_association_targets"
                    )
                    .single()
            assertEquals(
                listOf(
                    SqlValue.Text(if (authored) "authored:v1" else "legacy:v1"),
                    SqlValue.Text("session"),
                    SqlValue.Null,
                ),
                link,
            )
            assertTrue(db.query("SELECT id FROM strength_association_decisions").isEmpty())
            assertEquals(ended, r.load("execution")!!.execution())
            val w = p.definition()
            assertFails { r.saveWorkout(w.copy(title = "Changed"), p.source()) }
            assertEquals(w, r.workout(w.id(), w.revision()))
            assertEquals(ended, r.finish(ended, 1).execution())
            val retryAsAnotherWorkout =
                GymExecutionEngine.start(
                    "duplicate",
                    GymExecution.Origin.PLANNED,
                    p.definition(),
                    p.source(),
                    Offsets.ofTotalSeconds(0),
                    tick(2000),
                )
            assertTrue(
                assertFails { r.start(retryAsAnotherWorkout.snapshot()) }
                    .message!!
                    .contains("already completed")
            )
            assertNull(r.load("duplicate"))
            assertEquals(1, db.query("SELECT id FROM strength_sessions").size)
        }
    }

    @Test
    fun programmeSourceIsRecheckedAtFinishAndMissingOrChangedSourceRollsBack(): Unit =
        storage { db, r, _ ->
            val p = plan(db, false)
            val e =
                GymExecutionEngine.start(
                    "execution",
                    GymExecution.Origin.PLANNED,
                    p.definition(),
                    p.source(),
                    Offsets.ofTotalSeconds(0),
                    tick(0),
                )
            r.start(e.snapshot())
            e.saveSet("slot-0", set("working"), tick(100))
            val ended = e.finish(tick(1000), null, "Actual", true)
            db.execute(
                "UPDATE gym_plan_versions SET payload=?",
                listOf(
                    SqlValue.Text(
                        LegacyGymProgrammePayloadJson.encode(
                            legacy()
                                .copy(
                                    sessions =
                                        listOf(
                                            legacy()
                                                .sessions()
                                                .single()
                                                .copy(title = "Changed source")
                                        )
                                )
                        )
                    )
                ),
            )
            assertFails { r.finish(ended, 1) }
            assertEquals(GymExecution.Status.ACTIVE, r.load("execution")!!.execution().status())
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
            assertTrue(db.query("SELECT actual_id FROM strength_association_targets").isEmpty())
        }

    @Test
    fun savedWorkoutMustAlreadyExistAndRetainItsUnderlyingSourceValidation(): Unit =
        storage { db, r, _ ->
            val w =
                GymWorkoutDefinition(
                    "saved",
                    1,
                    at,
                    "Saved",
                    listOf(GymWorkoutDefinition.Slot("a", bench, null, null)),
                )
            val source =
                GymExecution.SourceReference(
                    GymExecution.SourceKind.SAVED_WORKOUT,
                    "saved",
                    1,
                    null,
                )
            assertFails { r.saveWorkout(w, source) }
            r.saveWorkout(w, null)
            r.saveWorkout(w, source)
            r.savePreference(GymExecutionRepository.Preference("saved", 1, true))
            assertEquals(listOf(w), r.favourites(1))
            val p = plan(db, false)
            r.saveWorkout(p.definition(), p.source())
            SqliteLegacyGymProgrammes(db, { at }, { "unused" }).disable()
            db.execute("DELETE FROM gym_plan_versions")
            val wrapped =
                GymExecution.SourceReference(
                    GymExecution.SourceKind.SAVED_WORKOUT,
                    p.definition().id(),
                    1,
                    null,
                )
            assertFails { r.saveWorkout(p.definition(), wrapped) }
            assertEquals(2, db.query("SELECT workout_id FROM strength_workout_versions").size)
        }

    @Test
    fun repeatedActualReceiptsRequireCurrentExactEvidenceAndExplicitEditAcknowledgement(): Unit =
        storage { db, r, hashes ->
            val original = finish(r)
            val actual = original.completedSession()!!
            val firstHash = hashes.hash(actual)
            val repeat = GymWorkoutAdapter.repeatActual("repeat", 1, at, "Repeat", actual)
            r.saveWorkout(repeat.definition(), repeat.source())
            val library = SqliteGymWorkouts(db)
            val before = library.record("repeat", 1)!!
            assertEquals(
                FrozenGymWorkoutRecordJson.TemplateKind.ACTUAL,
                before.sourceSnapshot!!.templateKind,
            )
            assertEquals(firstHash, before.sourceSnapshot!!.originalSourceFingerprint)
            val edited = actual.copy(notes = "Accepted explicit edit")
            val secondHash = hashes.hash(edited)
            r.saveActual(edited)
            val changed = library.record("repeat", 1)!!
            assertEquals(before.definition, changed.definition)
            assertEquals(firstHash, changed.sourceSnapshot!!.originalSourceFingerprint)
            assertEquals(secondHash, changed.sourceSnapshot!!.currentSourceFingerprint)
            assertNotNull(changed.sourceSnapshot!!.sourceEditedAt)
            r.saveWorkout(repeat.definition(), repeat.source())
            val evidence = SqliteGymExecutionRecords(db).evidence("execution")!!
            assertEquals(original, evidence.original.execution())
            assertEquals(edited, evidence.currentActual)
            assertFails { r.finish(original, 1) }
        }

    @Test
    fun repeatedExecutionRetainsSupersetsAndClearsLoadReferencesWithoutTransferringActualWeights():
        Unit = storage { db, r, hashes ->
        val p = plan(db, true)
        val e =
            GymExecutionEngine.start(
                "execution",
                GymExecution.Origin.PLANNED,
                p.definition(),
                p.source(),
                Offsets.ofTotalSeconds(0),
                tick(0),
            )
        r.start(e.snapshot())
        e.saveSet("a", set("working"), tick(100))
        val ended = e.finish(tick(1000), null, "Actual", false)
        r.finish(ended, 1)
        hashes.hash(ended.completedSession()!!)
        val repeat = GymWorkoutAdapter.repeatExecution("repeat", 1, at, "Repeat", ended)
        r.saveWorkout(repeat.definition(), repeat.source())
        val proof = SqliteGymWorkouts(db).record("repeat", 1)!!.sourceSnapshot!!
        assertEquals(FrozenGymWorkoutRecordJson.TemplateKind.EXECUTION, proof.templateKind)
        assertEquals(82.25, proof.originalSourceSlots.single().target()!!.referenceLoadKg())
        assertNull(repeat.definition().slots().single().target()!!.referenceLoadKg())
        val edited = ended.completedSession()!!.copy(notes = "Edited actual")
        hashes.hash(edited)
        r.saveActual(edited)
        r.saveWorkout(repeat.definition(), repeat.source())
        assertEquals(ended.slots(), proof.originalSourceSlots)
    }

    @Test
    fun unacknowledgedActualChangesAndInvalidFingerprintNeverCreateNewReceipts(): Unit =
        storage { db, r, hashes ->
            val actual = finish(r).completedSession()!!
            hashes.hash(actual)
            val repeat = GymWorkoutAdapter.repeatActual("repeat", 1, at, "Repeat", actual)
            r.saveWorkout(repeat.definition(), repeat.source())
            val edited = actual.copy(notes = "Unacknowledged change")
            hashes.hash(edited)
            db.execute(
                "UPDATE strength_sessions SET payload=?",
                listOf(SqlValue.Text(StrengthEvidenceJson.encodeSession(edited))),
            )
            assertFails { r.saveWorkout(repeat.definition(), repeat.source()) }
            assertFails { r.saveActual(edited) }
            assertFails { r.deleteActual(actual.id()) }
            db.execute(
                "UPDATE strength_sessions SET payload=?",
                listOf(SqlValue.Text(StrengthEvidenceJson.encodeSession(actual))),
            )
            val bad = SqliteGymExecutions(db, { at }, LegacyStrengthFingerprint { "not-a-sha256" })
            val second = GymWorkoutAdapter.repeatActual("second", 1, at, "Repeat", actual)
            assertFails { bad.saveWorkout(second.definition(), second.source()) }
            assertNull(r.workout("second", 1))
            assertEquals(1, db.query("SELECT id FROM strength_sessions").size)
        }

    @Test
    fun explicitDeletionAcknowledgesRepeatSourceAndRemovesOnlyOwnedActualAndChoices(): Unit =
        storage { db, r, hashes ->
            val original = finish(r)
            val actual = original.completedSession()!!
            hashes.hash(actual)
            val repeat = GymWorkoutAdapter.repeatActual("repeat", 1, at, "Repeat", actual)
            r.saveWorkout(repeat.definition(), repeat.source())
            r.savePreference(GymExecutionRepository.Preference("repeat", 1, true))
            plan(db, false)
            val associations = SqliteGymAssociations(db) { tick(100000).wallTime() }
            val proposal = associations.proposals(actual.id(), tick(3000).wallTime()).single()
            associations.decide(
                GymWorkoutAssociation.Decision(
                    "count",
                    proposal,
                    GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION,
                    tick(4000).wallTime(),
                )
            )
            r.deleteActual(actual.id())
            r.deleteActual(actual.id())
            assertNull(r.load(actual.id()))
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
            assertTrue(db.query("SELECT id FROM strength_association_decisions").isEmpty())
            assertTrue(db.query("SELECT actual_id FROM strength_association_targets").isEmpty())
            val receipt = SqliteGymWorkouts(db).record("repeat", 1)!!.sourceSnapshot!!
            assertNotNull(receipt.sourceDeletedAt)
            assertNull(receipt.currentSourceFingerprint)
            assertEquals(listOf(repeat.definition()), r.favourites(1))
            r.saveWorkout(repeat.definition(), repeat.source())
            // An identity reused for new evidence cannot silently resurrect a deleted repeat
            // source.
            assertFails { r.saveActual(actual) }
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
            assertEquals(receipt, SqliteGymWorkouts(db).record("repeat", 1)!!.sourceSnapshot)
            val next =
                GymExecutionEngine.start(
                    "next",
                    GymExecution.Origin.REPEATED,
                    repeat.definition(),
                    repeat.source(),
                    Offsets.ofTotalSeconds(0),
                    tick(0),
                )
            r.start(next.snapshot())
            next.saveSet("slot-0", set("new-working"), tick(1000))
            r.finish(
                next.finish(tick(2000), null, "New actual, deleted source stays deleted", true),
                1,
            )
            assertNull(r.load(actual.id()))
            assertEquals("next", r.recentActual(1).single().id())
        }

    @Test
    fun activeExecutionCannotBeDeletedOrReplacedByCompletedActual(): Unit = storage { db, r, _ ->
        val e = free()
        r.start(e.snapshot())
        add(e)
        e.saveSet("a", set("working"), tick(100))
        val ended = e.finish(tick(1000), null, "Actual", true)
        assertFails { r.deleteActual("execution") }
        assertFails { r.saveActual(ended.completedSession()!!) }
        assertEquals(GymExecution.Status.ACTIVE, r.load("execution")!!.execution().status())
        assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
    }

    @Test
    fun rowAppendFailureRollsBackHeaderActualAndAssociationTogether(): Unit = storage { db, r, _ ->
        val p = plan(db, false)
        val e =
            GymExecutionEngine.start(
                "execution",
                GymExecution.Origin.PLANNED,
                p.definition(),
                p.source(),
                Offsets.ofTotalSeconds(0),
                tick(0),
            )
        val initial = r.start(e.snapshot())
        e.saveSet("slot-0", set("working"), tick(100))
        val ended = e.finish(tick(1000), null, "Actual", true)
        for (table in
            listOf(
                "strength_sessions",
                "coach_evidence",
                "strength_association_targets",
                "strength_execution_sets",
                "strength_execution_events",
            )) {
            db.execute(
                "CREATE TRIGGER fail_write BEFORE INSERT ON $table BEGIN SELECT RAISE(ABORT,'failed'); END"
            )
            assertFails { r.finish(ended, 1) }
            assertEquals(initial, r.load("execution"))
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
            assertTrue(db.query("SELECT actual_id FROM strength_association_targets").isEmpty())
            db.execute("DROP TRIGGER fail_write")
        }
        assertEquals(ended, r.finish(ended, 1).execution())
    }

    @Test
    fun outerTransactionsRollbackStartsFinishesActualEditsAndDeletionReceipts(): Unit =
        storage { db, r, hashes ->
            assertFails {
                db.transaction {
                    r.start(free().snapshot())
                    error("rollback")
                }
            }
            assertNull(r.load("execution"))
            val original = finish(r)
            val actual = original.completedSession()!!
            hashes.hash(actual)
            val repeat = GymWorkoutAdapter.repeatActual("repeat", 1, at, "Repeat", actual)
            r.saveWorkout(repeat.definition(), repeat.source())
            val before = SqliteGymWorkouts(db).record("repeat", 1)
            val edited = actual.copy(notes = "Edited")
            hashes.hash(edited)
            assertFails {
                db.transaction {
                    r.saveActual(edited)
                    error("rollback")
                }
            }
            assertEquals(
                actual,
                SqliteGymExecutionRecords(db).evidence("execution")!!.currentActual,
            )
            assertEquals(before, SqliteGymWorkouts(db).record("repeat", 1))
            assertFails {
                db.transaction {
                    r.deleteActual("execution")
                    error("rollback")
                }
            }
            assertEquals(original, r.load("execution")!!.execution())
            assertEquals(before, SqliteGymWorkouts(db).record("repeat", 1))
        }

    @Test
    fun currentAcceptedActualConflictAndReceiptWriteFailureRollbackExplicitEdit(): Unit =
        storage { db, r, hashes ->
            val original = finish(r)
            val actual = original.completedSession()!!
            hashes.hash(actual)
            val repeat = GymWorkoutAdapter.repeatActual("repeat", 1, at, "Repeat", actual)
            r.saveWorkout(repeat.definition(), repeat.source())
            val edited = actual.copy(notes = "Edited")
            hashes.hash(edited)
            for (trigger in
                listOf(
                    "BEFORE UPDATE ON strength_workout_versions",
                    "BEFORE UPDATE ON strength_executions",
                    "BEFORE UPDATE ON strength_sessions",
                )) {
                db.execute(
                    "CREATE TRIGGER fail_edit $trigger BEGIN SELECT RAISE(ABORT,'failed'); END"
                )
                assertFails { r.saveActual(edited) }
                assertEquals(
                    actual,
                    SqliteGymExecutionRecords(db).evidence("execution")!!.currentActual,
                )
                assertNull(
                    SqliteGymWorkouts(db).record("repeat", 1)!!.sourceSnapshot!!.sourceEditedAt
                )
                db.execute("DROP TRIGGER fail_edit")
            }
        }

    @Test
    fun startRejectsNonemptyAndSelfRepeatAndFinishRejectsFutureEvidence(): Unit =
        storage { db, r, _ ->
            val e = free()
            add(e)
            assertFails { r.start(e.snapshot()) }
            val definition =
                GymWorkoutDefinition(
                    "repeat",
                    1,
                    at,
                    "Repeat",
                    listOf(GymWorkoutDefinition.Slot("slot-0", bench, null, null)),
                )
            assertFails {
                r.start(
                    GymExecutionEngine.start(
                            "self",
                            GymExecution.Origin.REPEATED,
                            definition,
                            GymExecution.SourceReference(
                                GymExecution.SourceKind.ACTUAL_SESSION,
                                "self",
                                0,
                                null,
                            ),
                            Offsets.ofTotalSeconds(0),
                            tick(0),
                        )
                        .snapshot()
                )
            }
            assertTrue(db.query("SELECT id FROM strength_executions").isEmpty())
            val future =
                GymExecutionEngine.start(
                    "future",
                    GymExecution.Origin.FREE,
                    null,
                    null,
                    Offsets.ofTotalSeconds(0),
                    tick(20000000),
                )
            r.start(future.snapshot())
            future.addExercises(
                listOf(GymWorkoutDefinition.Slot("a", bench, null, null)),
                tick(20000010),
            )
            future.saveSet("a", set("working"), tick(20001000))
            val ended = future.finish(tick(20002000), null, "Actual", true)
            assertFails { r.finish(ended, 1) }
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
            assertEquals(GymExecution.Status.ACTIVE, r.load("future")!!.execution().status())
        }

    @Test
    fun ignoredConditionalHeaderWriteRollsBackAlreadyPreparedActual(): Unit = storage { db, r, _ ->
        val e = free()
        val initial = r.start(e.snapshot())
        add(e)
        e.saveSet("a", set("working"), tick(1000))
        val ended = e.finish(tick(2000), null, "Actual", true)
        db.execute(
            "CREATE TRIGGER ignore_header BEFORE UPDATE ON strength_executions BEGIN SELECT RAISE(IGNORE); END"
        )
        assertFails { r.finish(ended, 1) }
        assertEquals(initial, r.load("execution"))
        assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
        db.execute("DROP TRIGGER ignore_header")
        assertEquals(ended, r.finish(ended, 1).execution())
    }

    @Test
    fun sourceContentIsCheckedBeforeAnyWorkoutOrExecutionIsInserted(): Unit = storage { db, r, _ ->
        val p = plan(db, false)
        val changed = p.definition().copy(title = "Unsupported source content")
        val e =
            GymExecutionEngine.start(
                "execution",
                GymExecution.Origin.PLANNED,
                changed,
                p.source(),
                Offsets.ofTotalSeconds(0),
                tick(0),
            )
        assertFails { r.start(e.snapshot()) }
        assertTrue(db.query("SELECT id FROM strength_executions").isEmpty())
        assertTrue(db.query("SELECT workout_id FROM strength_workout_versions").isEmpty())
        val correct =
            GymExecutionEngine.start(
                "execution",
                GymExecution.Origin.PLANNED,
                p.definition(),
                p.source(),
                Offsets.ofTotalSeconds(0),
                tick(0),
            )
        r.start(correct.snapshot())
        assertEquals(p.definition(), r.workout(p.definition().id(), 1))
    }

    @Test
    fun oversizedEditedActualCannotChangeAcceptedEvidenceOrRepeatReceipts(): Unit =
        storage { db, r, hashes ->
            val ended = finish(r)
            val actual = ended.completedSession()!!
            hashes.hash(actual)
            val repeat = GymWorkoutAdapter.repeatActual("repeat", 1, at, "Repeat", actual)
            r.saveWorkout(repeat.definition(), repeat.source())
            val before = SqliteGymWorkouts(db).record("repeat", 1)
            val huge =
                actual.copy(
                    exercises =
                        List(100) { x ->
                            StrengthSession.ExerciseSets(
                                bench,
                                List(100) { n -> set("working-$x-$n") },
                            )
                        }
                )
            hashes.hash(huge)
            assertTrue(StrengthEvidenceJson.encodeSession(huge).encodeToByteArray().size > 262144)
            assertFails { r.saveActual(huge) }
            assertEquals(
                actual,
                SqliteGymExecutionRecords(db).evidence("execution")!!.currentActual,
            )
            assertEquals(before, SqliteGymWorkouts(db).record("repeat", 1))
        }

    @Test
    fun recentActualsAreBoundedExcludeFutureAndDoNotHydrateUnrelatedCorruptHistory(): Unit =
        storage { db, r, _ ->
            val actual = finish(r).completedSession()!!
            val older =
                actual.copy(
                    id = "older",
                    startedAt = Instants.ofEpochSecond(at.getEpochSecond() - 86400),
                )
            r.saveActual(older)
            db.execute(
                "INSERT INTO strength_sessions(id,started_at,payload) VALUES('future','2027-01-01T00:00:00Z','corrupt future payload')"
            )
            db.execute(
                "UPDATE strength_sessions SET payload='corrupt older payload' WHERE id='older'"
            )
            assertEquals(listOf(actual), r.recentActual(1))
            assertFails { r.recentActual(0) }
            assertFails { r.recentActual(21) }
            assertFails { r.recentActual(2) }
            assertEquals(3, db.query("SELECT id FROM strength_sessions").size)
        }

    @Test
    fun ignoredReceiptAcceptedHeaderAndActualUpdatesCannotAcknowledgeAnUncommittedEdit(): Unit =
        storage { db, r, hashes ->
            val ended = finish(r)
            val actual = ended.completedSession()!!
            hashes.hash(actual)
            val repeat = GymWorkoutAdapter.repeatActual("repeat", 1, at, "Repeat", actual)
            r.saveWorkout(repeat.definition(), repeat.source())
            val edited = actual.copy(notes = "Explicit edit")
            hashes.hash(edited)
            val before = SqliteGymWorkouts(db).record("repeat", 1)
            for (table in
                listOf("strength_workout_versions", "strength_executions", "strength_sessions")) {
                db.execute(
                    "CREATE TRIGGER ignore_edit BEFORE UPDATE ON $table BEGIN SELECT RAISE(IGNORE); END"
                )
                assertFails { r.saveActual(edited) }
                assertEquals(before, SqliteGymWorkouts(db).record("repeat", 1))
                assertEquals(
                    actual,
                    SqliteGymExecutionRecords(db).evidence("execution")!!.currentActual,
                )
                db.execute("DROP TRIGGER ignore_edit")
            }
        }
}
