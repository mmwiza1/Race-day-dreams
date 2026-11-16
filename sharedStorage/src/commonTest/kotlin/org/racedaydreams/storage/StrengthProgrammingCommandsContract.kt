package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.*
import org.racedaydreams.domain.time.*

abstract class StrengthProgrammingCommandsContract {
    protected abstract fun database(): SqlDatabase

    private val at = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")
    private val today = CalendarDates.of(2026, 11, 16)
    private val start = today.plusDays(1)
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
    private val replacement = ExerciseCatalog.definition("dumbbell-bench-press")!!.exercise()

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

    private fun programme(
        id: String = "programme",
        exercise: StrengthExercise = bench,
    ): StrengthProgramme {
        val slots = listOf(GymWorkoutDefinition.Slot("slot", exercise, target(), null))
        val workouts =
            List(2) { i ->
                val wid = "workout-$i"
                StrengthProgramme.Workout(
                    wid,
                    start.plusWeeks(i.toLong()),
                    StrengthWorkoutBudget.workoutMinutes(slots),
                    GymWorkoutDefinition(wid, 1, at, "Upper", slots),
                    null,
                )
            }
        return StrengthProgramme(
            id,
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

    private fun revised(p: StrengthProgramme = programme(), index: Int = 1): StrengthProgramme {
        val old = p.workouts()[index]
        val slots =
            old.definition().slots().map {
                it.copy(target = it.target()!!.copy(referenceLoadKg = 85.0))
            }
        val changed =
            old.copy(
                definition = old.definition().copy(revision = 2, slots = slots),
                expectedMinutes = StrengthWorkoutBudget.workoutMinutes(slots),
            )
        return p.copy(
            revision = 2,
            parentRevision = 1,
            effectiveFrom = old.date(),
            blocks =
                listOf(
                    p.blocks()
                        .single()
                        .copy(
                            workouts = p.workouts().map { if (it.id() == old.id()) changed else it }
                        )
                ),
            reason = "Athlete accepted future change",
        )
    }

    private fun draft() =
        StrengthProgrammeDraft(
            "draft",
            1,
            "programme",
            0,
            "Manual",
            settings(),
            start,
            listOf(
                StrengthProgrammeDraft.WeekBlock(
                    "block",
                    StrengthProgramme.Phase.DEVELOPMENT,
                    "Development",
                    2,
                    listOf(
                        StrengthProgrammeDraft.Day(
                            "day",
                            DayOfWeek.TUESDAY,
                            "Upper",
                            listOf(GymWorkoutDefinition.Slot("slot", bench, target(), null)),
                            null,
                        )
                    ),
                )
            ),
            StrengthProgramme.Origin.CUSTOM,
            null,
        )

    private fun texts(vararg values: String) = values.map { SqlValue.Text(it) }

    private fun <T> storage(
        block: (SqlDatabase, SqliteStrengthProgramming, SqliteConcreteStrengthProgrammes) -> T
    ): T {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "test-install")
            }
            return block(
                db,
                SqliteStrengthProgramming(db, { at }, { today }),
                SqliteConcreteStrengthProgrammes(db, { at }, { today }),
            )
        } finally {
            db.close()
        }
    }

    private fun accept(
        repo: SqliteStrengthProgramming,
        p: StrengthProgramme = programme(),
    ): Reference = repo.activate(p, null, "activate", emptyList()).after()!!

    private fun completed(db: SqlDatabase, session: String) {
        val actual =
            StrengthSession(
                "actual",
                at,
                Offsets.ofTotalSeconds(0),
                Durations.ofSeconds(1800, 0),
                null,
                "Recorded",
                listOf(
                    StrengthSession.ExerciseSets(
                        bench,
                        listOf(StrengthSession.SetEntry("set", 6, 80.0, 7.0, false, null, null)),
                    )
                ),
            )
        db.execute(
            "INSERT INTO strength_sessions VALUES('actual',?,?)",
            texts(at.toString(), StrengthEvidenceJson.encodeAcceptedSession(actual)),
        )
        db.execute(
            "INSERT INTO strength_association_targets(actual_id,programme_key,session_id,planned_date) VALUES('actual','programme:v1',?,?)",
            texts(session, start.toString()),
        )
    }

    private fun unfinished(db: SqlDatabase, p: StrengthProgramme, planned: Boolean = true) {
        val prepared =
            if (planned) GymWorkoutAdapter.fromAuthoredProgramme(p, "workout-0") else null
        val execution =
            GymExecutionEngine.start(
                    "live",
                    if (planned) GymExecution.Origin.PLANNED else GymExecution.Origin.FREE,
                    prepared?.definition(),
                    prepared?.source(),
                    Offsets.ofTotalSeconds(0),
                    GymExecution.Tick("boot", 1000, at),
                )
                .snapshot()
        val prescription = execution.prescription()
        if (prescription != null)
            db.execute(
                "INSERT INTO strength_workout_versions(workout_id,revision,source_kind,source_id,payload) VALUES(?,?, 'PROGRAMME',?,?)",
                listOf(
                    SqlValue.Text(prescription.id()),
                    SqlValue.Integer(prescription.revision().toLong()),
                ) +
                    texts(
                        p.id(),
                        FrozenGymWorkoutRecordJson.encode(
                            FrozenGymWorkoutRecordJson.Record(
                                prescription,
                                prepared!!.source(),
                                null,
                            )
                        ),
                    ),
            )
        val root = GymExecutionSnapshotJson.element(execution)
        db.execute(
            "INSERT INTO strength_executions(id,revision,origin,state,started_at,title,recorded_elapsed,active_millis,set_count,event_count,workout_id,workout_revision,header) VALUES('live',1,?,'ACTIVE',?,?,0,0,0,1,?,?,?)",
            texts(execution.origin().name, at.toString(), prescription?.title() ?: "Free workout") +
                listOf(
                    sqlText(prescription?.id()),
                    prescription?.revision()?.let { SqlValue.Integer(it.toLong()) }
                        ?: SqlValue.Null,
                    SqlValue.Text(JsonObject(root - setOf("events", "actualSets")).toString()),
                ),
        )
        db.execute(
            "INSERT INTO strength_execution_events VALUES('live',0,?)",
            texts(root.getValue("events").jsonArray.single().toString()),
        )
    }

    @Test
    fun initialAcceptanceAndIdenticalRetryAreAtomicAndDoNotCreateActuals(): Unit =
        storage { db, repo, _ ->
            val p = programme()
            val ref = accept(repo, p)
            assertEquals(Reference(Owner.AUTHORED, p.id(), 1), ref)
            assertEquals(ref, repo.currentReference())
            assertEquals(p, repo.programme(p.id(), 1))
            val choice = repo.decisions().single()
            assertEquals(at, choice.at())
            assertEquals(choice, repo.activate(p, null, "activate", emptyList()))
            assertEquals(1, repo.versions(100, 0).size)
            assertEquals(1, repo.decisions().size)
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
        }

    @Test
    fun staleExpectedSkippedRevisionAndReusedDecisionCannotReplaceHistory(): Unit =
        storage { _, repo, _ ->
            val ref = accept(repo)
            assertFailsWith<IllegalArgumentException> {
                repo.activate(revised(), null, "stale", emptyList())
            }
            assertFailsWith<IllegalArgumentException> {
                repo.activate(
                    revised().copy(revision = 3, parentRevision = 2),
                    ref,
                    "skip",
                    emptyList(),
                )
            }
            assertFailsWith<IllegalArgumentException> {
                repo.activate(
                    programme().copy(name = "Changed retry"),
                    null,
                    "activate",
                    emptyList(),
                )
            }
            assertFailsWith<IllegalArgumentException> { repo.stop(ref, "activate") }
            assertEquals(programme(), repo.programme("programme", 1))
            assertEquals(ref, repo.currentReference())
            assertEquals(1, repo.decisions().size)
        }

    @Test
    fun stoppingRetainsVersionsAndChoicesAndLostAcknowledgementIsHarmless(): Unit =
        storage { _, repo, _ ->
            val ref = accept(repo)
            val stopped = repo.stop(ref, "stop")
            assertNull(repo.currentReference())
            assertEquals(stopped, repo.stop(ref, "stop"))
            assertEquals(2, repo.decisions().size)
            assertEquals(programme(), repo.programme("programme", 1))
            assertFailsWith<IllegalArgumentException> { repo.stop(ref, "new-stop") }
        }

    @Test
    fun decisionAndPointerFailuresRollBackNewVersionsAndChildren(): Unit = storage { db, repo, _ ->
        db.execute(
            "CREATE TRIGGER fail_decision BEFORE INSERT ON strength_programme_decisions BEGIN SELECT RAISE(ABORT,'test failure'); END"
        )
        assertFails { accept(repo) }
        assertTrue(repo.versions(100, 0).isEmpty())
        assertTrue(db.query("SELECT block_id FROM strength_programme_blocks").isEmpty())
        assertNull(repo.currentReference())
        db.execute("DROP TRIGGER fail_decision")
        val ref = accept(repo)
        db.execute(
            "CREATE TRIGGER fail_pointer BEFORE INSERT ON strength_programme_current BEGIN SELECT RAISE(ABORT,'test failure'); END"
        )
        assertFails { repo.activate(revised(), ref, "future", emptyList()) }
        assertEquals(ref, repo.currentReference())
        assertEquals(1, repo.decisions().size)
        assertNull(repo.programme("programme", 2))
    }

    @Test
    fun fullAcceptanceAndStopShareTheExistingOuterTransaction(): Unit = storage { db, repo, _ ->
        assertFails {
            db.transaction {
                val ref = accept(repo)
                repo.stop(ref, "stop")
                error("Outer failure")
            }
        }
        assertNull(repo.currentReference())
        assertTrue(repo.decisions().isEmpty())
        assertTrue(repo.versions(100, 0).isEmpty())
    }

    @Test
    fun freshCompletionAfterReviewProtectsRecordedWorkoutWithoutRewritingActuals(): Unit =
        storage { db, repo, _ ->
            val ref = accept(repo)
            val reviewed = repo.reviewRevision(ref)
            val proposed = revised(index = 0)
            assertTrue(reviewed.completedWorkoutIds().isEmpty())
            completed(db, "workout-0")
            val payload =
                db.query("SELECT payload FROM strength_sessions").single().single().sqlText()
            assertFailsWith<IllegalArgumentException> {
                repo.activate(proposed, ref, "future", emptyList())
            }
            assertEquals(setOf("workout-0"), repo.reviewRevision(ref).completedWorkoutIds())
            assertEquals(
                payload,
                db.query("SELECT payload FROM strength_sessions").single().single().sqlText(),
            )
            assertEquals(programme(), repo.programme("programme", 1))
        }

    @Test
    fun pastPrescriptionCannotBeChangedEvenWithoutACompletedLink(): Unit = storage { _, repo, _ ->
        val ref = accept(repo)
        val future = revised()
        val old = future.workouts()[0]
        val forged =
            future.copy(
                blocks =
                    listOf(
                        future
                            .blocks()
                            .single()
                            .copy(
                                workouts =
                                    listOf(
                                        old.copy(notes = "Changed past prescription"),
                                        future.workouts()[1],
                                    )
                            )
                    )
            )
        assertFailsWith<IllegalArgumentException> {
            repo.activate(forged, ref, "past", emptyList())
        }
        assertNull(repo.programme("programme", 2))
    }

    @Test
    fun activeProgrammeExecutionBlocksItsRevisionButUnrelatedFreeWorkoutDoesNot(): Unit =
        storage { db, repo, _ ->
            val ref = accept(repo)
            unfinished(db, programme())
            assertFailsWith<IllegalArgumentException> {
                repo.activate(revised(), ref, "future", emptyList())
            }
            assertEquals(
                1L,
                db.query("SELECT revision FROM strength_executions").single().single().sqlLong(),
            )
            db.execute("DELETE FROM strength_executions")
            unfinished(db, programme(), false)
            repo.activate(revised(), ref, "future", emptyList())
            assertEquals(Reference(Owner.AUTHORED, "programme", 2), repo.currentReference())
            assertEquals(
                1L,
                db.query("SELECT revision FROM strength_executions").single().single().sqlLong(),
            )
        }

    @Test
    fun exclusionsArchivedChoicesAndUnavailableEquipmentPreventNewProgramming(): Unit =
        storage { db, repo, _ ->
            SqliteExerciseExclusions(db)
                .exclude(
                    ExerciseExclusionRepository.Exclusion(
                        bench.id(),
                        ExerciseExclusionRepository.Reason.PREFERENCE,
                        at,
                    )
                )
            assertFailsWith<IllegalArgumentException> { accept(repo) }
            SqliteExerciseExclusions(db).restoreExercise(bench.id())
            SqliteStrengthChoices(db).save(SqliteStrengthChoices.Choice(bench.id(), false, true))
            assertFailsWith<IllegalArgumentException> { accept(repo) }
            SqliteStrengthChoices(db).save(SqliteStrengthChoices.Choice(bench.id(), false, false))
            assertFailsWith<IllegalArgumentException> {
                accept(repo, programme().copy(settings = settings().copy(equipment = emptySet())))
            }
            assertTrue(repo.versions(100, 0).isEmpty())
        }

    @Test
    fun canonicalDefinitionTargetsBudgetAndFutureClockMustBeReviewed(): Unit =
        storage { _, repo, _ ->
            val p = programme()
            val w = p.workouts()[0]
            for (bad in
                listOf(
                    w.copy(expectedMinutes = w.expectedMinutes() + 1),
                    w.copy(
                        definition =
                            w.definition()
                                .copy(
                                    slots =
                                        listOf(w.definition().slots().single().copy(target = null))
                                )
                    ),
                    w.copy(
                        definition =
                            w.definition()
                                .copy(
                                    slots =
                                        listOf(
                                            w.definition()
                                                .slots()
                                                .single()
                                                .copy(
                                                    exercise =
                                                        bench.copy(name = "Stale canonical name")
                                                )
                                        )
                                )
                    ),
                )) {
                val candidate =
                    p.copy(
                        blocks =
                            listOf(
                                p.blocks().single().copy(workouts = listOf(bad, p.workouts()[1]))
                            )
                    )
                assertFailsWith<IllegalArgumentException> { accept(repo, candidate) }
            }
            assertFailsWith<IllegalArgumentException> {
                accept(
                    repo,
                    p.copy(
                        createdAt =
                            Instants.ofEpochSecond(at.getEpochSecond() + 1, at.getNano().toLong())
                    ),
                )
            }
            assertFailsWith<IllegalArgumentException> {
                accept(repo, p.copy(effectiveFrom = today.plusDays(-1)))
            }
            assertTrue(repo.decisions().isEmpty())
        }

    @Test
    fun permanentSubstitutionUsesTheExistingEngineAndClearsIncompatibleReference(): Unit =
        storage { db, repo, _ ->
            val p = programme()
            val ref = accept(repo)
            completed(db, "workout-0")
            val context =
                ExerciseSubstitutionEngine.Context(
                    bench,
                    p.settings().goal(),
                    p.settings().experience(),
                    p.settings().equipment(),
                    emptySet(),
                    emptySet(),
                    emptySet(),
                    ExerciseSubstitutionEngine.Reason.EQUIPMENT_BUSY,
                )
            val proposal =
                StrengthProgrammeEditor.substitute(
                    p,
                    "workout-1",
                    "slot",
                    context,
                    replacement.id(),
                    null,
                    p.workouts()[1].date(),
                    at,
                    setOf("workout-0"),
                )
            val accepted = repo.activate(proposal.proposed(), ref, "swap", proposal.substitutions())
            assertEquals(listOf("workout-0"), accepted.protectedWorkoutIds())
            assertEquals(proposal.substitutions(), accepted.substitutions())
            assertNull(
                repo
                    .programme("programme", 2)!!
                    .workouts()[1]
                    .definition()
                    .slots()
                    .single()
                    .target()!!
                    .referenceLoadKg()
            )
            assertEquals(p, repo.programme("programme", 1))
        }

    @Test
    fun substitutionExclusionIsRecheckedAtAcceptance(): Unit = storage { db, repo, _ ->
        val p = programme()
        val ref = accept(repo)
        val context =
            ExerciseSubstitutionEngine.Context(
                bench,
                p.settings().goal(),
                p.settings().experience(),
                p.settings().equipment(),
                emptySet(),
                emptySet(),
                emptySet(),
                ExerciseSubstitutionEngine.Reason.ALTERNATIVES,
            )
        val proposal =
            StrengthProgrammeEditor.substitute(
                p,
                "workout-1",
                "slot",
                context,
                replacement.id(),
                null,
                p.workouts()[1].date(),
                at,
                emptySet(),
            )
        SqliteExerciseExclusions(db)
            .exclude(
                ExerciseExclusionRepository.Exclusion(
                    replacement.id(),
                    ExerciseExclusionRepository.Reason.EQUIPMENT_UNAVAILABLE,
                    at,
                )
            )
        assertFailsWith<IllegalArgumentException> {
            repo.activate(proposal.proposed(), ref, "swap", proposal.substitutions())
        }
        assertEquals(ref, repo.currentReference())
    }

    @Test
    fun weeklyReviewedDraftMustMatchExactlyAndSavingNeverActivates(): Unit = storage { _, repo, _ ->
        val d = draft()
        repo.saveDraft(d, 0)
        val preview = d.preview(at, emptySet(), emptyMap())
        assertNull(repo.currentReference())
        repo.saveDraft(d.copy(revision = 2, name = "Another edit"), 1)
        assertFailsWith<IllegalArgumentException> { repo.activateDraft(preview, null, "accept", d) }
        assertNull(repo.currentReference())
        val current = repo.draft(d.id())!!
        val proposed = current.preview(at, emptySet(), emptyMap())
        val accepted = repo.activateDraft(proposed, null, "accept", current)
        repo.deleteDraft(current.id(), 2)
        assertEquals(accepted, repo.activateDraft(proposed, null, "accept", current))
    }

    @Test
    fun concretePreviewRequiresSavedExactReviewAndUsesTheSameActivationHistory(): Unit =
        storage { _, repo, concrete ->
            val d = ConcreteStrengthProgrammeDraft("preview", 1, null, programme(), emptyList())
            assertFailsWith<IllegalArgumentException> { concrete.activate(d, "accept") }
            concrete.save(d, 0)
            val changed =
                d.copy(revision = 2, programme = programme().copy(name = "Edited preview"))
            concrete.save(changed, 1)
            assertFailsWith<IllegalArgumentException> { concrete.activate(d, "accept") }
            val accepted = concrete.activate(changed, "accept")
            assertEquals(accepted, concrete.activate(changed, "accept"))
            assertEquals(accepted.after(), repo.currentReference())
            assertEquals(changed.programme(), repo.programme("programme", 1))
            assertTrue(concrete.list(10, 0).single().revisionExists())
            assertEquals(1, repo.decisions().size)
        }

    @Test
    fun customDefinitionAndEquipmentAreRecheckedBeforeActivation(): Unit = storage { _, repo, _ ->
        val custom =
            CustomStrengthExercise(
                StrengthExercise(
                    "custom-press",
                    "My press",
                    StrengthExercise.Region.UPPER,
                    StrengthExercise.Mode.WEIGHTED,
                    false,
                    StrengthExercise.MeasurementProfile.LOAD_REPS,
                ),
                ExerciseMetadata.BodyArea.CHEST,
                ExerciseMetadata.Role.HORIZONTAL_PUSH,
                ExerciseMetadata.EquipmentRequirement(
                    listOf(setOf(ExerciseMetadata.Equipment.DUMBBELLS))
                ),
                "Athlete notes",
            )
        val p = programme(exercise = custom.exercise())
        assertFailsWith<IllegalArgumentException> { accept(repo, p) }
        repo.saveCustom(custom, null)
        repo.saveCustom(
            custom.copy(exercise = custom.exercise().copy(name = "Edited name")),
            custom,
        )
        assertFailsWith<IllegalArgumentException> { accept(repo, p) }
        assertNull(repo.currentReference())
    }

    @Test
    fun legacyReplacementAndIdentityCollisionsKeepEarlierProgrammingReadable(): Unit =
        storage { db, repo, _ ->
            db.execute("INSERT INTO gym_plan_versions VALUES('legacy:v1','legacy',1,'{}')")
            db.execute("INSERT INTO gym_current VALUES(1,'legacy:v1')")
            val legacy = Reference(Owner.LEGACY, "legacy", 1)
            assertFailsWith<IllegalArgumentException> {
                repo.activate(programme("legacy"), legacy, "collision", emptyList())
            }
            val accepted = repo.activate(programme(), legacy, "replace", emptyList())
            assertEquals(legacy, accepted.before())
            assertTrue(db.query("SELECT version_id FROM gym_current").isEmpty())
            assertEquals(
                1L,
                db.query("SELECT COUNT(*) FROM gym_plan_versions").single().single().sqlLong(),
            )
            assertEquals(accepted.after(), repo.currentReference())
        }
}
