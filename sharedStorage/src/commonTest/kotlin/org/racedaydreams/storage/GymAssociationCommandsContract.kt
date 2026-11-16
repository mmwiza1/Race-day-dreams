package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.GymWorkoutAssociation.Choice
import org.racedaydreams.domain.time.*

abstract class GymAssociationCommandsContract {
    protected abstract fun database(): SqlDatabase

    private val at = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")
    private val day = CalendarDates.of(2026, 11, 16)
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
    private val legacySession =
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
            "Preserved explanation",
        )

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
                setOf(DayOfWeek.MONDAY),
                1,
                setOf("full gym"),
                45,
                GymProgramme.Effort.RPE,
            ),
            day,
            day.plusDays(41),
            listOf(legacySession),
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
        val w =
            StrengthProgramme.Workout(
                "session",
                day,
                StrengthWorkoutBudget.workoutMinutes(slots),
                GymWorkoutDefinition("session", 1, at, "Upper", slots),
                null,
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
                setOf(DayOfWeek.MONDAY),
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
                    listOf(w),
                    "Practice",
                )
            ),
            day,
            "Athlete chose",
            emptyList(),
        )
    }

    private fun tick(ms: Long) =
        GymExecution.Tick(
            "boot",
            ms,
            Instants.ofEpochSecond(at.getEpochSecond() + ms / 1000, at.getNano().toLong()),
        )

    private fun finished(id: String = "actual", warm: Boolean = false): GymExecution {
        val e =
            GymExecutionEngine.start(
                id,
                GymExecution.Origin.FREE,
                null,
                null,
                Offsets.ofTotalSeconds(0),
                tick(0),
            )
        e.addExercises(listOf(GymWorkoutDefinition.Slot("slot", bench, null, null)), tick(10))
        e.saveSet(
            "slot",
            StrengthSession.SetEntry("working", 6, 80.0, null, warm, null, null, 2.5),
            tick(1000),
        )
        return e.finish(tick(2000), null, "Recorded note 🏋️\u0000", true)
    }

    /** Test-only seeding of original normalized rows; no production partial finalizer. */
    private fun seed(db: SqlDatabase, e: GymExecution) {
        val root = GymExecutionSnapshotJson.element(e)
        val header =
            if (e.completedSession() == null) JsonObject(root - setOf("actualSets", "events"))
            else
                JsonObject(
                    (root - setOf("actualSets", "events")) +
                        ("completedSession" to
                            JsonObject(root.getValue("completedSession").jsonObject - "exercises"))
                )
        db.execute(
            "INSERT INTO strength_executions(id,revision,origin,state,started_at,title,recorded_elapsed,active_millis,set_count,event_count,header,accepted_actual) VALUES(?,1,'FREE',?,?,'Free workout',?,?,?,?,?,?)",
            listOf(
                SqlValue.Text(e.id()),
                SqlValue.Text(e.status().name),
                SqlValue.Text(e.startedAt().toString()),
                SqlValue.Integer(e.recordedElapsedMillis()),
                SqlValue.Integer(e.activeMillis()),
                SqlValue.Integer(e.actualSets().size.toLong()),
                SqlValue.Integer(e.events().size.toLong()),
                SqlValue.Text(header.toString()),
                e.completedSession()?.let {
                    SqlValue.Text(StrengthEvidenceJson.encodeAcceptedSession(it))
                } ?: SqlValue.Null,
            ),
        )
        for ((i, v) in root.getValue("actualSets").jsonArray.withIndex()) db.execute(
            "INSERT INTO strength_execution_sets(execution_id,sequence,actual_set_id,payload) VALUES(?,?,?,?)",
            listOf(
                SqlValue.Text(e.id()),
                SqlValue.Integer(i.toLong()),
                SqlValue.Text(
                    v.jsonObject.getValue("actual").jsonObject.getValue("id").jsonPrimitive.content
                ),
                SqlValue.Text(v.toString()),
            ),
        )
        for ((i, v) in root.getValue("events").jsonArray.withIndex()) db.execute(
            "INSERT INTO strength_execution_events(execution_id,sequence,payload) VALUES(?,?,?)",
            listOf(SqlValue.Text(e.id()), SqlValue.Integer(i.toLong()), SqlValue.Text(v.toString())),
        )
        e.completedSession()?.let { actual(db, it) }
    }

    private fun actual(db: SqlDatabase, a: StrengthSession) =
        db.execute(
            "INSERT OR REPLACE INTO strength_sessions(id,started_at,payload) VALUES(?,?,?)",
            listOf(
                SqlValue.Text(a.id()),
                SqlValue.Text(a.startedAt().toString()),
                SqlValue.Text(StrengthEvidenceJson.encodeSession(a)),
            ),
        )

    private fun <T> storage(
        authored: Boolean = false,
        block: (SqlDatabase, SqliteGymAssociations) -> T,
    ): T {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "test-install")
            }
            if (authored)
                SqliteStrengthProgramming(db, { at }, { day })
                    .activate(authored(), null, "activate", emptyList())
            else SqliteLegacyGymProgrammes(db, { at }, { "unused" }).save(legacy())
            seed(db, finished())
            return block(db, SqliteGymAssociations(db) { tick(10000).wallTime() })
        } finally {
            db.close()
        }
    }

    private fun choice(id: String, p: GymWorkoutAssociation.Proposal, count: Boolean = true) =
        GymWorkoutAssociation.Decision(
            id,
            p,
            if (count) Choice.COUNT_TOWARD_SESSION else Choice.KEEP_EXTRA,
            tick(5000).wallTime(),
        )

    private fun proposal(r: SqliteGymAssociations, id: String = "actual") =
        r.proposals(id, tick(3000).wallTime()).single()

    private fun links(db: SqlDatabase) =
        db.query("SELECT id,payload FROM coach_evidence WHERE kind='STRENGTH_LINK'")

    private fun validateArchive(db: SqlDatabase, archive: GymAssociationArchiveJson.Archive) {
        val executions =
            db.query("SELECT id FROM strength_executions").associate {
                val id = it.single().sqlText()
                id to SqliteGymExecutionRecords(db).load(id)!!.execution()
            }
        val actuals =
            db.query("SELECT payload FROM strength_sessions")
                .map { StrengthEvidenceJson.decodeSession(it.single().sqlText()) }
                .associateBy { it.id() }
        val plans =
            SqliteLegacyGymProgrammes(db, { at }, { "unused" }).versions().associateBy {
                "${it.id()}:v${it.version()}"
            }
        val authored = SqliteStrengthProgrammingRecords(db).programme("authored", 1)
        val links =
            links(db).associate { row ->
                val value = GymStoredJson.read(row[1].sqlText())
                row[0].sqlText() to
                    GymAssociationArchiveJson.Link(
                        GymStoredJson.text(value, "id", 256),
                        GymStoredJson.text(value, "gymVersion", 256),
                        GymStoredJson.text(value, "gymSession", 256),
                        GymStoredJson.text(value, "plannedDate", 64),
                    )
            }
        GymAssociationArchiveJson.validate(
            archive,
            executions,
            actuals,
            plans,
            links,
            if (authored == null) emptyMap() else mapOf("authored:v1" to authored),
        )
    }

    @Test
    fun archiveRoundTripRetainsBothFamiliesAndHistoricalActualBeforeExplicitEdit(): Unit {
        for (authored in listOf(false, true)) storage(authored) { db, r ->
            val p = proposal(r)
            val count = choice("count", p)
            r.decide(count)
            actual(db, p.actual().copy(notes = "Current edited actual"))
            val archive = r.archive()
            assertEquals(
                archive,
                GymAssociationArchiveJson.decode(GymAssociationArchiveJson.encode(archive)),
            )
            validateArchive(db, archive)
            assertEquals(p.actual(), archive.decisions.single().proposal().actual())
            r.decide(choice("keep", r.reviewCurrent("actual", tick(4000).wallTime())!!, false))
            validateArchive(db, r.archive())
            assertEquals(2, r.archive().decisions.size)
            assertEquals(mapOf("actual" to "keep"), r.archive().current)
        }
    }

    @Test
    fun archiveRejectsFutureMalformedDuplicateAndMissingFields(): Unit = storage { _, r ->
        r.decide(choice("count", proposal(r)))
        val payload = GymAssociationArchiveJson.encode(r.archive())
        val root = GymStoredJson.read(payload)
        val decision = root.getValue("decisions").jsonArray.single()
        val current = root.getValue("current").jsonArray.single()
        for (invalid in
            listOf(
                JsonObject(root + ("version" to JsonPrimitive(2))),
                JsonObject(root - "current"),
                JsonObject(root + ("extra" to JsonNull)),
                JsonObject(root + ("decisions" to JsonArray(listOf(decision, decision)))),
                JsonObject(root + ("current" to JsonArray(listOf(current, current)))),
            )) assertFailsWith<IllegalArgumentException> {
            GymAssociationArchiveJson.decode(invalid.toString())
        }
        assertFailsWith<IllegalArgumentException> {
            GymAssociationArchiveJson.decode(payload.dropLast(1))
        }
    }

    @Test
    fun archiveRejectsMissingActualExecutionProgrammeAndChangedOrigin(): Unit = storage { db, r ->
        val p = proposal(r)
        r.decide(choice("count", p))
        val archive = r.archive()
        val e = SqliteGymExecutionRecords(db).load("actual")!!.execution()
        val link = GymAssociationArchiveJson.Link("actual", "legacy:v1", "session", day.toString())
        val plans = mapOf("legacy:v1" to legacy())
        val actuals = mapOf("actual" to p.actual())
        for ((execs, acts, programmes) in
            listOf<
                Triple<
                    Map<String, GymExecution>,
                    Map<String, StrengthSession>,
                    Map<String, GymProgramme>,
                >
            >(
                Triple(emptyMap(), actuals, plans),
                Triple(mapOf("actual" to e), emptyMap(), plans),
                Triple(mapOf("actual" to e), actuals, emptyMap()),
            )) assertFailsWith<IllegalArgumentException> {
            GymAssociationArchiveJson.validate(
                archive,
                execs,
                acts,
                programmes,
                mapOf("actual" to link),
            )
        }
        val forged =
            archive.copy(
                decisions =
                    listOf(
                        archive.decisions
                            .single()
                            .copy(proposal = p.copy(origin = GymExecution.Origin.CUSTOM))
                    )
            )
        assertFailsWith<IllegalArgumentException> { validateArchive(db, forged) }
        assertEquals(archive, r.archive())
    }

    @Test
    fun archiveRejectsChangedFrozenPrescriptionAndNonComparableHistoricalCount(): Unit =
        storage(true) { db, r ->
            val p = proposal(r)
            r.decide(choice("count", p))
            val original = r.archive()
            val w = p.authoredPrescription()!!
            val forged =
                p.copy(
                    authoredPrescription =
                        w.copy(definition = w.definition().copy(title = "Forged"))
                )
            assertFailsWith<IllegalArgumentException> {
                validateArchive(
                    db,
                    original.copy(
                        decisions = listOf(original.decisions.single().copy(proposal = forged))
                    ),
                )
            }
            val noWorking =
                p.actual()
                    .copy(
                        exercises =
                            p.actual().exercises().map { x ->
                                x.copy(sets = x.sets().map { it.copy(warmUp = true) })
                            }
                    )
            assertFailsWith<IllegalArgumentException> {
                validateArchive(
                    db,
                    original.copy(
                        decisions =
                            listOf(
                                original.decisions
                                    .single()
                                    .copy(proposal = p.copy(actual = noWorking))
                            )
                    ),
                )
            }
            validateArchive(db, original)
        }

    @Test
    fun archiveCurrentChoiceMustMatchItsActualAndCoachingProjection(): Unit = storage { db, r ->
        val p = proposal(r)
        r.decide(choice("count", p))
        val a = r.archive()
        for (invalid in
            listOf(
                a.copy(current = emptyMap()),
                a.copy(current = mapOf("actual" to "missing")),
                a.copy(current = mapOf("other" to "count")),
                a.copy(decisions = listOf(choice("count", p, false))),
            )) {
            assertFailsWith<IllegalArgumentException> { validateArchive(db, invalid) }
        }
        db.execute("DELETE FROM coach_evidence WHERE kind='STRENGTH_LINK'")
        assertFailsWith<IllegalArgumentException> { validateArchive(db, a) }
    }

    @Test
    fun archiveCannotClaimOneTargetFromTwoActuals(): Unit = storage { db, r ->
        seed(db, finished("other"))
        val first = proposal(r)
        val second = proposal(r, "other")
        r.decide(choice("count", first))
        val archive =
            r.archive().let {
                it.copy(
                    decisions = it.decisions + choice("compete", second),
                    current = it.current + ("other" to "compete"),
                )
            }
        val e = SqliteGymExecutionRecords(db)
        assertFailsWith<IllegalArgumentException> {
            GymAssociationArchiveJson.validate(
                archive,
                mapOf(
                    "actual" to e.load("actual")!!.execution(),
                    "other" to e.load("other")!!.execution(),
                ),
                mapOf("actual" to first.actual(), "other" to second.actual()),
                mapOf("legacy:v1" to legacy()),
                mapOf(
                    "actual" to
                        GymAssociationArchiveJson.Link(
                            "actual",
                            "legacy:v1",
                            "session",
                            day.toString(),
                        ),
                    "other" to
                        GymAssociationArchiveJson.Link(
                            "other",
                            "legacy:v1",
                            "session",
                            day.toString(),
                        ),
                ),
            )
        }
        assertEquals(mapOf("actual" to "count"), r.archive().current)
    }

    @Test
    fun archiveNeverInventsAthleteDecisionsForLegacyProjections(): Unit = storage { db, r ->
        db.execute(
            "INSERT INTO strength_association_targets(actual_id,programme_key,session_id,planned_date,decision_id) VALUES('actual','legacy:v1','session','2026-11-16',NULL)"
        )
        assertEquals(GymAssociationArchiveJson.Archive(emptyList(), emptyMap()), r.archive())
        validateArchive(db, r.archive())
    }

    @Test
    fun legacyCountKeepAndRetryPreserveActualAndDecisionHistory(): Unit = storage { db, r ->
        val original = SqliteGymExecutionRecords(db).evidence("actual")
        val p = proposal(r)
        assertFalse(p.authored())
        val count = choice("count", p)
        assertEquals(count, r.decide(count))
        assertEquals(count, r.decide(count))
        assertEquals(count, r.current("actual"))
        assertEquals(1, links(db).size)
        assertTrue(r.proposals("actual", tick(3000).wallTime()).isEmpty())
        r.decide(choice("keep", r.reviewCurrent("actual", tick(4000).wallTime())!!, false))
        assertTrue(links(db).isEmpty())
        assertEquals(2, db.query("SELECT id FROM strength_association_decisions").size)
        assertFails { r.decide(count) }
        assertEquals(original, SqliteGymExecutionRecords(db).evidence("actual"))
        assertEquals(legacy(), SqliteLegacyGymProgrammes(db, { at }, { "unused" }).current())
    }

    @Test
    fun authoredCountContributesOnlyExplicitCompletionAndKeepsReferenceSeparate(): Unit =
        storage(true) { db, r ->
            val p = proposal(r)
            assertTrue(p.authored())
            assertNull(p.prescription())
            assertEquals(
                82.25,
                p.authoredPrescription()!!
                    .definition()
                    .slots()
                    .single()
                    .target()!!
                    .referenceLoadKg(),
            )
            assertEquals(80.0, p.actual().exercises().single().sets().single().kilograms())
            val records = SqliteStrengthProgrammingRecords(db)
            val ref = records.currentReference()!!
            assertTrue(records.reviewRevision(ref).completedWorkoutIds().isEmpty())
            r.decide(choice("count", p))
            assertEquals(setOf("session"), records.reviewRevision(ref).completedWorkoutIds())
            r.decide(choice("keep", r.reviewCurrent("actual", tick(4000).wallTime())!!, false))
            assertTrue(records.reviewRevision(ref).completedWorkoutIds().isEmpty())
            assertEquals(authored(), records.programme("authored", 1))
        }

    @Test
    fun competingActualsCannotBothClaimOneTargetAndKeepReleasesIt(): Unit = storage { db, r ->
        seed(db, finished("other"))
        val first = proposal(r)
        val other = proposal(r, "other")
        r.decide(choice("first", first))
        assertFails { r.decide(choice("compete", other)) }
        assertNull(r.current("other"))
        r.decide(choice("keep", first, false))
        r.decide(choice("second", other))
        assertEquals("other", links(db).single()[0].sqlText())
        assertEquals(2, db.query("SELECT id FROM strength_sessions").size)
    }

    @Test
    fun changedCurrentVersionRejectsStaleCountButHistoricalKeepStillWorks(): Unit =
        storage { db, r ->
            val p = proposal(r)
            val plans = SqliteLegacyGymProgrammes(db, { at }, { "unused" })
            plans.save(legacy(2))
            assertFails { r.decide(choice("stale", p)) }
            r.decide(choice("keep", p, false))
            assertEquals(1, r.current("actual")!!.proposal().programmeRevision())
            assertEquals(2, plans.current()!!.version())
            assertTrue(links(db).isEmpty())
        }

    @Test
    fun authoredStopRejectsStaleCountButRetainedReviewRemainsAvailable(): Unit =
        storage(true) { db, r ->
            val p = proposal(r)
            r.decide(choice("count", p))
            val plans = SqliteStrengthProgramming(db, { at }, { day })
            plans.stop(plans.currentReference()!!, "stop")
            val review = r.reviewCurrent("actual", tick(4000).wallTime())!!
            assertEquals(p.authoredPrescription(), review.authoredPrescription())
            r.decide(choice("keep", review, false))
            assertFails { r.decide(choice("stale", p)) }
            assertTrue(links(db).isEmpty())
        }

    @Test
    fun actualEditRequiresFreshReviewAndNeverChangesHistoricalChoices(): Unit =
        storage(true) { db, r ->
            val p = proposal(r)
            val count = choice("count", p)
            r.decide(count)
            val edited = p.actual().copy(notes = "Explicit actual edit")
            actual(db, edited)
            assertFails { r.decide(choice("stale-keep", p, false)) }
            val review = r.reviewCurrent("actual", tick(4000).wallTime())!!
            assertEquals(edited, review.actual())
            r.decide(choice("keep", review, false))
            val historical =
                GymAssociationDecisionJson.decode(
                    db.query("SELECT payload FROM strength_association_decisions WHERE id='count'")
                        .single()
                        .single()
                        .sqlText()
                )
            assertEquals(count, historical)
            assertEquals(
                p.actual(),
                SqliteGymExecutionRecords(db).load("actual")!!.execution().completedSession(),
            )
            assertTrue(links(db).isEmpty())
        }

    @Test
    fun legacyLinkBlocksDiscoveryAndCannotBeRemovedUsingAnotherTarget(): Unit = storage { db, r ->
        val p = proposal(r)
        db.execute(
            "INSERT INTO strength_association_targets(actual_id,programme_key,session_id,planned_date,decision_id) VALUES('actual','other:v1','other','2026-11-16',NULL)"
        )
        assertTrue(r.proposals("actual", tick(3000).wallTime()).isEmpty())
        assertNull(r.current("actual"))
        assertFails { r.decide(choice("count", p)) }
        assertFails { r.decide(choice("keep", p, false)) }
        assertTrue(db.query("SELECT id FROM strength_association_decisions").isEmpty())
    }

    @Test
    fun missingDeletedWarmupOnlyAndUnfinishedEvidenceNeverClaimsCompletion(): Unit =
        storage { db, r ->
            assertTrue(r.proposals("missing", tick(3000).wallTime()).isEmpty())
            assertNull(r.current("missing"))
            assertNull(r.reviewCurrent("missing", tick(3000).wallTime()))
            seed(db, finished("warm", true))
            assertTrue(r.proposals("warm", tick(3000).wallTime()).isEmpty())
            val p = proposal(r)
            db.execute("DELETE FROM strength_sessions WHERE id='actual'")
            assertFails { r.decide(choice("deleted", p)) }
            db.execute("DELETE FROM strength_executions WHERE id='actual'")
            seed(
                db,
                GymExecutionEngine.start(
                        "actual",
                        GymExecution.Origin.FREE,
                        null,
                        null,
                        Offsets.ofTotalSeconds(0),
                        tick(0),
                    )
                    .snapshot(),
            )
            assertTrue(r.proposals("actual", tick(3000).wallTime()).isEmpty())
            assertFails { r.decide(choice("unfinished", p)) }
            assertTrue(links(db).isEmpty())
        }

    @Test
    fun futureChoiceAndReusedDecisionIdentityRejectWithoutPartialHistory(): Unit =
        storage { db, r ->
            val p = proposal(r)
            assertFails {
                r.decide(
                    GymWorkoutAssociation.Decision(
                        "future",
                        p,
                        Choice.COUNT_TOWARD_SESSION,
                        tick(11000).wallTime(),
                    )
                )
            }
            val keep = choice("keep", p, false)
            r.decide(keep)
            assertFails { r.decide(choice("keep", p)) }
            assertEquals(keep, r.current("actual"))
            assertEquals(1, db.query("SELECT id FROM strength_association_decisions").size)
            assertTrue(links(db).isEmpty())
        }

    @Test
    fun targetAndProjectionFailuresRollbackDecisionAndExistingLink(): Unit = storage { db, r ->
        val p = proposal(r)
        for ((table, trigger) in
            listOf("coach_evidence" to "projection", "strength_association_targets" to "target")) {
            db.execute(
                "CREATE TRIGGER $trigger BEFORE INSERT ON $table BEGIN SELECT RAISE(ABORT,'failed'); END"
            )
            assertFails { r.decide(choice("failed", p)) }
            assertTrue(db.query("SELECT id FROM strength_association_decisions").isEmpty())
            assertTrue(links(db).isEmpty())
            db.execute("DROP TRIGGER $trigger")
        }
        val count = choice("count", p)
        r.decide(count)
        db.execute(
            "CREATE TRIGGER keep_target BEFORE INSERT ON strength_association_targets BEGIN SELECT RAISE(ABORT,'failed'); END"
        )
        assertFails { r.decide(choice("keep", p, false)) }
        assertEquals(count, r.current("actual"))
        assertEquals(1, links(db).size)
        assertEquals(1, db.query("SELECT id FROM strength_association_decisions").size)
    }

    @Test
    fun outerTransactionRollbackPreservesActualAndAllowsCleanRetry(): Unit =
        storage(true) { db, r ->
            val p = proposal(r)
            val d = choice("count", p)
            val before = SqliteGymExecutionRecords(db).evidence("actual")
            assertFails {
                db.transaction {
                    r.decide(d)
                    error("outer rollback")
                }
            }
            assertNull(r.current("actual"))
            assertTrue(links(db).isEmpty())
            assertEquals(before, SqliteGymExecutionRecords(db).evidence("actual"))
            assertEquals(d, r.decide(d))
        }
}
