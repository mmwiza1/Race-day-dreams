package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.GymWorkoutAssociation.Choice
import org.racedaydreams.domain.time.*

abstract class GymBackupRestoreRepositoryContract {
    abstract fun database(): SqlDatabase

    private val at = GymEngineArchiveFixtures.at
    private val day = CalendarDates.of(2026, 11, 16)
    private val mono = 9007199254741017L
    private val bench = GymEngineArchiveFixtures.bench
    private val row = ExerciseCatalog.definition("dumbbell-row")!!.exercise()

    private fun tick(ms: Long, epoch: String = "source-boot") =
        GymExecution.Tick(
            epoch,
            mono + ms,
            Instants.ofEpochSecond(at.getEpochSecond() + ms / 1000, at.getNano().toLong()),
        )

    private fun repository(db: SqlDatabase) =
        SqliteGymExecutions(db, { tick(100000).wallTime() }, PortableLegacyStrengthFingerprint)

    private fun restorer(db: SqlDatabase) =
        SqliteGymExecutions(
            db,
            { error("Restore cannot create an observation") },
            LegacyStrengthFingerprint { error("Restore cannot regenerate source fingerprints") },
        )

    private fun storage(block: (SqlDatabase, SqlDatabase) -> Unit) {
        val source = database()
        val target = database()
        try {
            for (db in listOf(source, target)) ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "gym-restore-test")
            }
            block(source, target)
        } finally {
            source.close()
            target.close()
        }
    }

    private fun free(db: SqlDatabase, id: String): Pair<GymExecutionEngine, Long> {
        val e =
            GymExecutionEngine.start(
                id,
                GymExecution.Origin.FREE,
                null,
                null,
                Offsets.ofTotalSeconds(19815),
                tick(0),
            )
        val saved = repository(db).start(e.snapshot())
        e.addExercises(
            listOf(
                GymWorkoutDefinition.Slot("bench", bench, null, "pair"),
                GymWorkoutDefinition.Slot("row", row, null, "pair"),
            ),
            tick(10),
        )
        return e to saved.revision()
    }

    private fun finished(db: SqlDatabase, id: String = "actual"): GymExecution {
        val (e, revision) = free(db, id)
        e.saveSet("bench", StrengthSession.SetEntry("warm", 8, 20.0, null, true), tick(100))
        e.saveSet(
            "bench",
            StrengthSession.SetEntry("work", 6, 80.0, null, false, null, null, 2.5),
            tick(1000),
        )
        e.saveSet("row", StrengthSession.SetEntry("row-work", 10, 25.0, 7.5, false), tick(2000))
        val ended = e.finish(tick(3000), null, "Retained performed evidence 🏋️\u0000", true)
        repository(db).finish(ended, revision)
        return ended
    }

    private fun context(target: SqlDatabase, parent: UserBackupDataJson.Snapshot) {
        SqliteCoreBackupRestore(target).populate(parent.core)
        SqliteArchiveBackupRestore(target).populate(parent)
        SqliteStrengthProgramming(target, { error("No new time") }, { error("No current date") })
            .populateArchive(parent)
        SqliteBodyAwardsBackupRestore(target).populate(parent)
    }

    private fun compare(parent: UserBackupDataJson.Snapshot, target: SqlDatabase) {
        val actual = SqliteApplicationBackup(target).snapshot()
        assertEquals(
            GymExecutionArchiveJson.element(parent.engine!!),
            GymExecutionArchiveJson.element(actual.engine!!),
        )
        assertEquals(
            GymToolsPayloadJson.element(parent.tools),
            GymToolsPayloadJson.element(actual.tools),
        )
        assertEquals(parent.coaching!!.sessions, actual.coaching!!.sessions)
        assertTrue(target.query("PRAGMA foreign_key_check").isEmpty())
    }

    private fun legacy(db: SqlDatabase): GymProgramme {
        val p = LegacyGymArchiveFixtures.programme()
        val plan =
            p.copy(
                start = day,
                end = day.plusDays(41),
                settings = p.settings().copy(days = setOf(DayOfWeek.MONDAY)),
                sessions = p.sessions().map { it.copy(date = day) },
            )
        SqliteLegacyGymProgrammes(db, { at }, { "unused" }).save(plan)
        return plan
    }

    @Test
    fun editedActualFrozenRepeatSupersetsWarmupsAndLongRevisionsRestoreWithoutNewEvidence() =
        storage { source, target ->
            val original = finished(source)
            val repeat = GymWorkoutAdapter.repeatExecution("repeat", 1, at, "Repeat", original)
            repository(source).saveWorkout(repeat.definition(), repeat.source())
            repository(source).savePreference(GymExecutionRepository.Preference("repeat", 1, true))
            val edited = original.completedSession()!!.copy(notes = "Explicit later correction")
            repository(source).saveActual(edited)
            source.execute("UPDATE strength_executions SET revision=9007199254740993")
            val parent = SqliteApplicationBackup(source).snapshot()
            context(target, parent)
            val actualRows = target.query("SELECT * FROM strength_sessions")
            restorer(target).populateArchive(parent)
            compare(parent, target)
            val evidence = SqliteGymExecutionRecords(target).evidence("actual")!!
            assertEquals(original, evidence.original.execution())
            assertEquals(edited, evidence.currentActual)
            assertNotNull(evidence.actualEditedAt)
            assertEquals(9007199254740993L, evidence.original.revision())
            assertEquals("pair", original.slots().first().supersetGroup())
            assertTrue(evidence.original.execution().actualSets().first().actual().warmUp())
            assertEquals(actualRows, target.query("SELECT * FROM strength_sessions"))
            assertEquals(
                parent.engine!!.workouts.values.single(),
                SqliteGymWorkouts(target).record("repeat", 1),
            )
            assertEquals(listOf(repeat.definition()), restorer(target).favourites(1))
        }

    @Test
    fun deletedRepeatSourceKeepsOriginalFingerprintAndDeletionReceiptWithoutResurrectingActual() =
        storage { source, target ->
            val original = finished(source)
            val repeat =
                GymWorkoutAdapter.repeatActual(
                    "repeat",
                    1,
                    at,
                    "Repeat",
                    original.completedSession()!!,
                )
            repository(source).saveWorkout(repeat.definition(), repeat.source())
            repository(source).deleteActual("actual")
            val parent = SqliteApplicationBackup(source).snapshot()
            val proof = parent.engine!!.workouts.values.single().sourceSnapshot!!
            assertNotNull(proof.sourceDeletedAt)
            context(target, parent)
            restorer(target).populateArchive(parent)
            compare(parent, target)
            assertEquals(proof, SqliteGymWorkouts(target).record("repeat", 1)!!.sourceSnapshot)
            assertNull(restorer(target).load("actual"))
            assertTrue(target.query("SELECT * FROM strength_sessions").isEmpty())
        }

    @Test
    fun unfinishedStatesAndTimerClocksStayHonestAcrossDeviceEpochChanges() {
        for (state in
            listOf(
                GymExecution.Status.ACTIVE,
                GymExecution.Status.PAUSED,
                GymExecution.Status.RECOVERY_REQUIRED,
            )) storage { source, target ->
            val (started, revision) = free(source, "unfinished")
            started.saveSet(
                "bench",
                StrengthSession.SetEntry("warm", 8, 20.0, null, true),
                tick(100),
            )
            val e =
                if (state == GymExecution.Status.RECOVERY_REQUIRED)
                    GymExecutionEngine.recover(started.snapshot(), tick(200, "recovered-boot"))
                else started
            if (state == GymExecution.Status.PAUSED) e.pause(tick(200))
            repository(source).save(e.snapshot(), revision)
            val tools = SqliteGymTools(source)
            tools.preferences(GymRestTimer.Preferences(180, false, true, true, true))
            val timer =
                tools.change("unfinished") {
                    GymRestTimer.start(
                        "unfinished",
                        "saved-working-trigger",
                        180,
                        GymRestTimer.Clock("timer-boot", mono),
                    )
                }
            val parent = SqliteApplicationBackup(source).snapshot()
            context(target, parent)
            restorer(target).populateArchive(parent)
            compare(parent, target)
            assertEquals(e.snapshot(), restorer(target).load("unfinished")!!.execution())
            assertEquals(state, restorer(target).unfinished().single().status())
            val restored = SqliteGymTools(target).timer("unfinished")!!
            assertEquals(timer, restored)
            assertEquals(
                GymRestTimer.Phase.INTERRUPTED,
                restored.observe(GymRestTimer.Clock("new-device-boot", 0)).phase(),
            )
            assertEquals(180000L, restored.remainingMillis())
            assertTrue(target.query("SELECT * FROM strength_sessions").isEmpty())
        }
    }

    @Test
    fun plannedLegacyLinkRestoresAsProjectionWithoutInventingAnAssociationChoice() =
        storage { source, target ->
            val p = legacy(source)
            val prepared = GymWorkoutAdapter.fromProgramme(p, p.sessions().single().id())
            val e =
                GymExecutionEngine.start(
                    "planned",
                    GymExecution.Origin.PLANNED,
                    prepared.definition(),
                    prepared.source(),
                    Offsets.ofTotalSeconds(0),
                    tick(0),
                )
            val saved = repository(source).start(e.snapshot())
            e.saveSet(
                e.snapshot().slots().first().id(),
                StrengthSession.SetEntry("working", 6, 80.0, 7.5, false),
                tick(1000),
            )
            repository(source)
                .finish(e.finish(tick(2000), null, "Planned actual", true), saved.revision())
            val parent = SqliteApplicationBackup(source).snapshot()
            context(target, parent)
            restorer(target).populateArchive(parent)
            compare(parent, target)
            assertEquals(
                listOf(SqlValue.Text("programme:v1"), SqlValue.Text("session"), SqlValue.Null),
                target
                    .query(
                        "SELECT programme_key,session_id,decision_id FROM strength_association_targets"
                    )
                    .single(),
            )
            assertTrue(target.query("SELECT * FROM strength_association_decisions").isEmpty())
        }

    @Test
    fun explicitCountAndKeepExtraDecisionsRestoreTheirOriginalTargetsAndDecisionTimes() {
        for (choice in listOf(Choice.COUNT_TOWARD_SESSION, Choice.KEEP_EXTRA)) storage {
            source,
            target ->
            legacy(source)
            finished(source)
            val associations = SqliteGymAssociations(source) { tick(10000).wallTime() }
            val proposal = associations.proposals("actual", tick(5000).wallTime()).single()
            val decision =
                GymWorkoutAssociation.Decision(
                    "original-choice",
                    proposal,
                    choice,
                    tick(10000).wallTime(),
                )
            associations.decide(decision)
            val parent = SqliteApplicationBackup(source).snapshot()
            context(target, parent)
            restorer(target).populateArchive(parent)
            compare(parent, target)
            assertEquals(
                decision,
                SqliteGymAssociations(target) { error("No review date") }.current("actual"),
            )
            val link =
                target
                    .query(
                        "SELECT programme_key,session_id,planned_date,decision_id FROM strength_association_targets"
                    )
                    .single()
            assertEquals(SqlValue.Text("original-choice"), link[3])
            assertEquals(choice == Choice.COUNT_TOWARD_SESSION, link[0] != SqlValue.Null)
        }
    }

    @Test
    fun missingOldEngineStillRestoresLegacyLinkWithoutManufacturingExecutionsOrDecisions() =
        storage { source, target ->
            legacy(source)
            finished(source)
            source.execute(
                "INSERT INTO coach_evidence VALUES('STRENGTH_LINK','actual',?)",
                listOf(
                    SqlValue.Text(
                        """{"id":"actual","plannedDate":"2026-11-16","gymVersion":"programme:v1","gymSession":"session"}"""
                    )
                ),
            )
            val current = SqliteApplicationBackup(source).snapshot()
            val old =
                UserBackupDataJson.decode(
                    JsonObject(UserBackupDataJson.element(current) - "strengthEngine" - "gymTools")
                )
            context(target, old)
            restorer(target).populateArchive(old)
            assertEquals(1, target.query("SELECT * FROM strength_association_targets").size)
            assertEquals(1, target.query("SELECT * FROM strength_sessions").size)
            assertTrue(target.query("SELECT * FROM strength_executions").isEmpty())
            assertTrue(target.query("SELECT * FROM strength_association_decisions").isEmpty())
        }

    @Test
    fun occupiedLateTimerPreferenceRejectsBeforeAnyEngineOrAssociationWrite() =
        storage { source, target ->
            finished(source)
            val parent = SqliteApplicationBackup(source).snapshot()
            context(target, parent)
            SqliteGymTools(target).preferences(GymRestTimer.Preferences.defaults())
            val before = target.query("SELECT total_changes()")
            assertFails { restorer(target).populateArchive(parent) }
            assertEquals(before, target.query("SELECT total_changes()"))
            assertTrue(target.query("SELECT * FROM strength_executions").isEmpty())
            assertEquals(
                parent.coaching!!.sessions,
                SqliteCoachingArchive(target).snapshot().sessions,
            )
        }

    @Test
    fun lastPreferenceFailureAndCallerRollbackKeepCompleteGymRestoreAtomic() =
        storage { source, target ->
            finished(source)
            val parent = SqliteApplicationBackup(source).snapshot()
            context(target, parent)
            val actuals = target.query("SELECT * FROM strength_sessions")
            val failed =
                object : SqlDatabase by target {
                    override fun execute(sql: String, arguments: List<SqlValue>) {
                        if (sql.startsWith("INSERT OR REPLACE INTO gym_rest_preferences"))
                            error("Simulated final timer preference failure")
                        target.execute(sql, arguments)
                    }
                }
            assertFails { restorer(failed).populateArchive(parent) }
            assertTrue(target.query("SELECT * FROM strength_executions").isEmpty())
            assertTrue(target.query("SELECT * FROM strength_execution_sets").isEmpty())
            assertEquals(actuals, target.query("SELECT * FROM strength_sessions"))
            assertFails {
                target.transaction {
                    restorer(target).populateArchive(parent)
                    compare(parent, target)
                    error("Reject whole app replacement")
                }
            }
            assertTrue(target.query("SELECT * FROM strength_executions").isEmpty())
            restorer(target).populateArchive(parent)
            compare(parent, target)
            val before = target.query("SELECT total_changes()")
            assertFails { restorer(target).populateArchive(parent) }
            assertEquals(before, target.query("SELECT total_changes()"))
        }
}
