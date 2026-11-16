package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.time.*

/** Same real SQLite tests run on JVM and Apple. No recommendation is automatically activated. */
abstract class RunningPlanRepositoryContract {
    protected abstract fun database(): SqlDatabase

    protected val start = CalendarDates.of(2026, 11, 16)
    protected val at = StoredInstant.parse("2026-11-15T09:00:00.123456789Z")

    @Test
    fun coachingArchiveReadsOneConsistentGraphWithoutRewritingActualPayloadOrFeedback() =
        storage { db, r, _ ->
            r.saveCurrent(version())
            r.saveResponse(RunExecution("run", "done", "v1", null, null, "Retained actual"))
            r.saveProposal(proposal())
            r.saveCurrent(version(2))
            val actual = LegacyFingerprintFixtures.session()
            val payload = " \n" + StrengthEvidenceJson.encodeSession(actual)
            db.execute(
                "INSERT INTO strength_sessions(id,started_at,payload) VALUES(?,?,?)",
                listOf(
                    SqlValue.Text(actual.id()),
                    SqlValue.Text(actual.startedAt().toString()),
                    SqlValue.Text(payload),
                ),
            )
            val feedback =
                """ {"ruleVersion":1,"score":null,"components":["historic component"],"extra":"retain"} """
            db.execute(
                "INSERT INTO run_feedback(activity_id,plan_version_id,payload) VALUES(?,?,?)",
                listOf(SqlValue.Text("run"), SqlValue.Text("v1"), SqlValue.Text(feedback)),
            )
            val changes = db.query("SELECT total_changes()")
            val snapshot = SqliteCoachingArchive(db).snapshot()
            assertEquals("v2", snapshot.currentVersionId)
            assertEquals(2, snapshot.versions.size)
            assertEquals(actual, snapshot.sessions[actual.id()])
            assertEquals(
                payload,
                snapshot.rows.getValue(CoachingArchiveJson.Table.SESSIONS).single()["payload"],
            )
            assertEquals(
                feedback,
                snapshot.rows.getValue(CoachingArchiveJson.Table.FEEDBACK).single()["payload"],
            )
            assertNull(snapshot.proposals.single().decision)
            assertEquals(
                CoachingArchiveJson.element(snapshot),
                CoachingArchiveJson.element(
                    CoachingArchiveJson.decode(CoachingArchiveJson.element(snapshot))
                ),
            )
            assertEquals(changes, db.query("SELECT total_changes()"))
        }

    @Test
    fun coachingExportRejectsImpossibleSizeBeforeLoadingPayloadsAndKeepsDatabaseIntact() =
        storage { db, r, _ ->
            r.saveCurrent(version())
            db.execute(
                "INSERT INTO run_feedback(activity_id,plan_version_id,payload) VALUES('huge','v1',CAST(zeroblob(33554433) AS TEXT))"
            )
            val changes = db.query("SELECT total_changes()")
            val guarded =
                object : SqlDatabase by db {
                    override fun query(
                        sql: String,
                        arguments: List<SqlValue>,
                    ): List<List<SqlValue>> {
                        check(!sql.contains("ORDER BY rowid")) {
                            "Payloads must not be loaded before the size check."
                        }
                        return db.query(sql, arguments)
                    }
                }
            val error =
                assertFailsWith<IllegalArgumentException> {
                    SqliteCoachingArchive(guarded).snapshot()
                }
            assertTrue(error.message!!.contains("whole-backup budget"))
            assertEquals(
                listOf(SqlValue.Integer(1)),
                db.query("SELECT COUNT(*) FROM run_feedback").single(),
            )
            assertEquals(changes, db.query("SELECT total_changes()"))
        }

    protected fun workout(id: String = "done", date: LocalDate = start): PlannedWorkout =
        PlannedWorkout(
            id,
            date,
            WorkoutCategory.EASY_RUN,
            5.0,
            Durations.ofMinutes(30),
            listOf(
                WorkoutSegment(
                    WorkoutSegment.Kind.RUN,
                    null,
                    Durations.ofMinutes(30),
                    300.0,
                    360.0,
                    null,
                    4.5,
                    "Talk comfortably",
                )
            ),
            "retained 🏃\u0000 note",
        )

    protected fun profile(): UserProfile =
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

    protected fun version(
        number: Int = 1,
        workouts: List<PlannedWorkout> = listOf(workout(), workout("future", start.plusDays(1))),
        id: String = "v$number",
    ): PlanVersion =
        PlanVersion(
            id,
            "plan",
            number,
            at,
            "Athlete accepted",
            profile(),
            "retained evidence",
            TrainingPlan(
                "plan",
                start,
                start.plusDays(7),
                listOf(
                    TrainingPhase(
                        TrainingPhase.Kind.FOUNDATION,
                        start,
                        start.plusDays(7),
                        "Build a routine",
                    )
                ),
                workouts,
            ),
        )

    private fun proposal(id: String = "proposal", versionId: String = "v1"): AdjustmentProposal =
        AdjustmentProposal(
            id,
            versionId,
            "future",
            at,
            "Recorded context",
            listOf("Actual recorded evidence"),
            "Keep or rest",
            "Limited",
            listOf(
                AdjustmentProposal.Option(
                    "keep",
                    "Keep plan",
                    AdjustmentProposal.Action.KEEP,
                    null,
                ),
                AdjustmentProposal.Option(
                    "rest",
                    "Rest today",
                    AdjustmentProposal.Action.REST,
                    null,
                ),
                AdjustmentProposal.Option(
                    "replace",
                    "Shorter",
                    AdjustmentProposal.Action.REPLACE,
                    workout("future", start.plusDays(1)).copy(distanceKm = 3.0),
                ),
            ),
        )

    private fun <T> storage(
        block: (SqlDatabase, SqliteRunningCoaching, SqlitePlannedWorkouts) -> T
    ): T {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "test-install")
            }
            return block(
                db,
                SqliteRunningCoaching(db, { at }, { "decision-v2" }),
                SqlitePlannedWorkouts(db),
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun explicitReminderEditsIncrementButNeverCreateActualActivity(): Unit =
        storage { db, _, mirror ->
            assertTrue(mirror.plannedWorkouts().isEmpty())
            assertEquals(0L, mirror.plannedWorkoutRevision("missing"))
            mirror.savePlannedWorkout(workout())
            mirror.savePlannedWorkout(workout())
            assertEquals(2L, mirror.plannedWorkoutRevision("done"))
            assertEquals(listOf(workout()), mirror.plannedWorkouts())
            assertTrue(db.query("SELECT * FROM activities").isEmpty())
            mirror.deletePlannedWorkout("done")
            assertEquals(0L, mirror.plannedWorkoutRevision("done"))
        }

    @Test
    fun planSnapshotsAndCurrentPointerAreIndependentOfReminderEdits(): Unit =
        storage { _, plans, mirror ->
            val original = version()
            plans.saveCurrent(original)
            mirror.savePlannedWorkout(workout().copy(note = "Temporary mirror edit"))
            assertEquals(original, plans.current())
            assertEquals(listOf(original), plans.versions())
            mirror.deletePlannedWorkout("done")
            assertEquals(original, plans.current())
        }

    @Test
    fun unchangedLegacyJsonSpellingDoesNotBumpReminderRevision(): Unit =
        storage { db, plans, mirror ->
            plans.saveCurrent(version())
            db.execute(
                "UPDATE planned_workouts SET payload=? WHERE id='done'",
                listOf(
                    SqlValue.Text(
                        PlannedWorkoutJson.encode(workout())
                            .replace("\"distanceKm\":5.0", "\"distanceKm\":5")
                    )
                ),
            )
            plans.saveCurrent(version(2))
            assertEquals(1L, mirror.plannedWorkoutRevision("done"))
            assertEquals(1L, mirror.plannedWorkoutRevision("future"))
            plans.saveCurrent(
                version(
                    3,
                    listOf(
                        workout().copy(note = "Changed instruction"),
                        workout("future", start.plusDays(1)),
                    ),
                )
            )
            assertEquals(2L, mirror.plannedWorkoutRevision("done"))
            assertEquals(1L, mirror.plannedWorkoutRevision("future"))
        }

    @Test
    fun completedPrescriptionCannotBeChangedButFutureWorkCan(): Unit = storage { _, plans, mirror ->
        val original = version()
        plans.saveCurrent(original)
        plans.saveResponse(RunExecution("actual", "done", "v1", null, null, "recorded"))
        assertFailsWith<IllegalArgumentException> {
            plans.saveCurrent(
                version(
                    2,
                    listOf(workout().copy(distanceKm = 7.0), workout("future", start.plusDays(1))),
                )
            )
        }
        assertEquals(original, plans.current())
        assertEquals(1L, mirror.plannedWorkoutRevision("done"))
        val next =
            version(
                2,
                listOf(workout(), workout("future", start.plusDays(1)).copy(note = "Future edit")),
            )
        plans.saveCurrent(next)
        assertEquals(listOf(next, original), plans.versions())
        assertEquals(next, plans.current())
        assertEquals("v1", plans.responses().single().planVersionId())
    }

    @Test
    fun duplicateVersionOrFailedMirrorLeavesOldPointerAndHistory(): Unit =
        storage { db, plans, mirror ->
            val original = version()
            plans.saveCurrent(original)
            assertFails { plans.saveCurrent(original) }
            assertFails { plans.saveCurrent(version(1, id = "different-id")) }
            db.execute(
                "CREATE TRIGGER fail_mirror BEFORE INSERT ON planned_workouts BEGIN SELECT RAISE(ABORT,'test failure'); END"
            )
            assertFails { plans.saveCurrent(version(2)) }
            assertEquals(original, plans.current())
            assertEquals(listOf(original), plans.versions())
            assertEquals(2, mirror.plannedWorkouts().size)
        }

    @Test
    fun duplicateWorkoutIdentityRollsBackRatherThanReplacingReminder(): Unit =
        storage { _, plans, mirror ->
            val original = version()
            plans.saveCurrent(original)
            assertFails {
                plans.saveCurrent(version(2, listOf(workout(), workout("done", start.plusDays(1)))))
            }
            assertEquals(original, plans.current())
            assertEquals(2, mirror.plannedWorkouts().size)
        }

    @Test
    fun keepDecisionDoesNotReadClockGenerateIdOrCreateNewVersion(): Unit = storage { db, plans, _ ->
        plans.saveCurrent(version())
        val suggestion = proposal()
        plans.saveProposal(suggestion)
        val noClock =
            SqliteRunningCoaching(
                db,
                { error("Keep must not generate time") },
                { error("Keep must not generate ID") },
            )
        noClock.decide(suggestion, "keep", emptySet())
        assertEquals("v1", plans.current()!!.id())
        assertEquals(1, plans.versions().size)
        assertTrue(plans.proposals().isEmpty())
        assertTrue(plans.proposalKnown(suggestion.id()))
        assertFailsWith<IllegalArgumentException> { noClock.decide(suggestion, "keep", emptySet()) }
        assertEquals(
            listOf(SqlValue.Text("keep"), SqlValue.Text("v1")),
            db.query("SELECT decision,result_version_id FROM coach_proposals").single(),
        )
    }

    @Test
    fun acceptedRestUsesSharedEngineAndPreservesOriginalVersion(): Unit =
        storage { db, plans, mirror ->
            val original = version()
            plans.saveCurrent(original)
            val suggestion = proposal()
            plans.saveProposal(suggestion)
            assertEquals(original, plans.current())
            plans.decide(suggestion, "rest", setOf("done"))
            assertEquals("decision-v2", plans.current()!!.id())
            assertEquals(2, plans.current()!!.version())
            assertEquals(listOf("done"), plans.current()!!.plan().workouts().map { it.id() })
            assertEquals(original, plans.versions().last())
            assertEquals(0L, mirror.plannedWorkoutRevision("future"))
            assertEquals(1L, mirror.plannedWorkoutRevision("done"))
            assertTrue(db.query("SELECT * FROM activities").isEmpty())
        }

    @Test
    fun staleCompletedAndInvalidDecisionsCannotChangePlan(): Unit = storage { _, plans, _ ->
        val suggestion = proposal()
        plans.saveCurrent(version())
        plans.saveProposal(suggestion)
        assertFailsWith<IllegalArgumentException> {
            plans.decide(suggestion, "rest", setOf("future"))
        }
        assertFailsWith<IllegalArgumentException> { plans.decide(suggestion, "other", emptySet()) }
        plans.saveCurrent(version(2))
        assertFailsWith<IllegalArgumentException> { plans.decide(suggestion, "rest", emptySet()) }
        assertEquals("v2", plans.current()!!.id())
        assertEquals(listOf(suggestion), plans.proposals())
    }

    @Test
    fun decisionFailureRollsBackNewVersionMirrorAndDecision(): Unit = storage { db, plans, mirror ->
        val original = version()
        plans.saveCurrent(original)
        val suggestion = proposal()
        plans.saveProposal(suggestion)
        db.execute(
            "CREATE TRIGGER reject_decision BEFORE UPDATE ON coach_proposals BEGIN SELECT RAISE(ABORT,'test failure'); END"
        )
        assertFails { plans.decide(suggestion, "replace", emptySet()) }
        assertEquals(original, plans.current())
        assertEquals(listOf(original), plans.versions())
        assertEquals(listOf(suggestion), plans.proposals())
        assertEquals(2, mirror.plannedWorkouts().size)
    }

    @Test
    fun responsesValidateOriginalMembershipAndOverlayOnlyTheirLink(): Unit =
        storage { db, plans, _ ->
            plans.saveCurrent(version())
            assertFailsWith<IllegalArgumentException> {
                plans.saveResponse(RunExecution("actual", "missing", "v1", null, null, ""))
            }
            assertFailsWith<IllegalArgumentException> {
                plans.saveResponse(RunExecution("actual", "done", "missing", null, null, ""))
            }
            val activity =
                ActivityRecord(
                    "actual",
                    SourceIdentity("manual", "actual", null),
                    at,
                    Instants.ofEpochSecond(at.getEpochSecond() + 600, at.getNano().toLong()),
                    null,
                    null,
                    1000.0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    emptyList(),
                    emptyList(),
                    null,
                    null,
                )
            val activities = SqliteActivities(db) { at }
            activities.upsertManualActivity(activity)
            plans.saveResponse(RunExecution("actual", "done", "v1", 0, false, "entered"))
            assertEquals("done", activities.activityDetail("actual")!!.plannedWorkoutId())
            assertNull(
                ActivityJson.decode(
                        db.query("SELECT payload FROM activities").single().single().sqlText()
                    )
                    .plannedWorkoutId()
            )
        }

    @Test
    fun planProfileWorkoutResponseAndProposalCodecsRetainUnknownsAndEvidence(): Unit =
        storage { _, _, _ ->
            val original = version()
            assertEquals(
                original,
                RunningCoachingJson.decodeVersion(RunningCoachingJson.encodeVersion(original)),
            )
            assertEquals(profile(), ProfileJson.decode(ProfileJson.encode(profile())))
            val unknown =
                workout()
                    .copy(
                        distanceKm = null,
                        duration = null,
                        segments =
                            listOf(
                                WorkoutSegment(
                                    WorkoutSegment.Kind.RUN,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                )
                            ),
                    )
            assertEquals(unknown, PlannedWorkoutJson.decode(PlannedWorkoutJson.encode(unknown)))
            val response =
                RunExecution(
                    "actual",
                    "done",
                    "v1",
                    0,
                    false,
                    "notes",
                    listOf(
                        RunExecution.StepEvidence(
                            0,
                            WorkoutSegment.Kind.RUN,
                            123.0,
                            9007199254741017L,
                            null,
                            null,
                            true,
                            false,
                        )
                    ),
                )
            assertEquals(
                response,
                RunningCoachingJson.decodeResponse(RunningCoachingJson.encodeResponse(response)),
            )
            assertEquals(
                proposal(),
                RunningCoachingJson.decodeProposal(RunningCoachingJson.encodeProposal(proposal())),
            )
            val free = RunExecution("free", null, null, null, null, "")
            assertEquals(
                free,
                RunningCoachingJson.decodeResponse(
                    RunningCoachingJson.encodeResponse(free).replace(",\"steps\":[]", "")
                ),
            )
        }

    @Test
    fun malformedOversizedAndDuplicatePlanPayloadsAreRejected(): Unit = storage { _, _, _ ->
        val row = Json.parseToJsonElement(RunningCoachingJson.encodeVersion(version())).jsonObject
        val duplicate =
            JsonObject(
                row +
                    ("workouts" to
                        JsonArray(
                            listOf(
                                row.getValue("workouts").jsonArray.first(),
                                row.getValue("workouts").jsonArray.first(),
                            )
                        ))
            )
        assertFailsWith<IllegalArgumentException> {
            RunningCoachingJson.decodeVersion(duplicate.toString())
        }
        val oversized = JsonObject(row + ("phases" to JsonArray(List(1001) { JsonNull })))
        assertFailsWith<IllegalArgumentException> {
            RunningCoachingJson.decodeVersion(oversized.toString())
        }
        val workoutRow = Json.parseToJsonElement(PlannedWorkoutJson.encode(workout())).jsonObject
        assertFailsWith<IllegalArgumentException> {
            PlannedWorkoutJson.decode(
                JsonObject(workoutRow + ("segments" to JsonArray(List(1001) { JsonNull })))
                    .toString()
            )
        }
    }

    @Test
    fun existingIntegerNegativeZeroAndDecimalZeroKeepAndroidReadSemantics(): Unit =
        storage { _, _, _ ->
            val source =
                """{"id":"shoe","model":"Daily","startingDistanceMetres":-0,"retired":false,"defaultShoe":false}"""
            assertEquals(0L, ShoeJson.decode(source).startingDistanceMetres()!!.toBits())
            assertEquals(
                (-0.0).toBits(),
                ShoeJson.decode(source.replace(":-0", ":-0.0")).startingDistanceMetres()!!.toBits(),
            )
        }

    @Test
    fun feedbackRetainsRuleAndActualNullableScoreWithoutComputingNewScore(): Unit =
        storage { db, plans, _ ->
            plans.saveCurrent(version())
            val feedback =
                RunFeedback(
                    null,
                    "Not enough evidence",
                    listOf(RunFeedback.Component("Recorded", null, "Unknown")),
                    emptyList(),
                    emptyList(),
                    "Limited evidence",
                    "Keep recording",
                )
            plans.saveFeedback("actual", "v1", feedback)
            val row =
                StoredJson.objectValue(
                    db.query("SELECT payload FROM run_feedback").single().single().sqlText()
                )
            assertEquals(JsonNull, row["score"])
            assertEquals(JsonPrimitive(RunScoreEngine.RULE_VERSION), row["ruleVersion"])
            assertFails { plans.saveFeedback("other", null, feedback) }
        }

    @Test
    fun allRunningStoresJoinTheExistingOuterTransaction(): Unit = storage { db, plans, mirror ->
        assertFails {
            db.transaction {
                plans.saveCurrent(version())
                plans.saveProposal(proposal())
                mirror.savePlannedWorkout(workout())
                error("Reject entire batch")
            }
        }
        assertNull(plans.current())
        assertTrue(plans.versions().isEmpty())
        assertTrue(plans.proposals().isEmpty())
        assertTrue(mirror.plannedWorkouts().isEmpty())
    }
}
