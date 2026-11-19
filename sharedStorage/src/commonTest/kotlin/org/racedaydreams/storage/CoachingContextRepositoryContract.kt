package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

abstract class CoachingContextRepositoryContract {
    abstract fun database(): SqlDatabase

    private val now = StoredInstant.parse("2026-11-17T09:00:00.123456789Z")

    private fun storage(
        block: (SqlDatabase, SqliteCoachingContext, CoachingRepository, () -> Int) -> Unit
    ) {
        val db = database()
        var ids = 0
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(now.toString(), "context-test")
            }
            val next = { "athlete-choice-${++ids}" }
            block(
                db,
                SqliteCoachingContext(db, { now }, next),
                SqliteCoachingRepository(db, { now }, next, PortableLegacyStrengthFingerprint),
                { ids },
            )
        } finally {
            db.close()
        }
    }

    private fun insert(db: SqlDatabase, kind: String, payload: JsonObject) =
        db.execute(
            "INSERT INTO coach_evidence(kind,id,payload) VALUES(?,?,?)",
            listOf(
                SqlValue.Text(kind),
                SqlValue.Text(payload.getValue("id").jsonPrimitive.content),
                SqlValue.Text(payload.toString()),
            ),
        )

    private fun weight(id: String, at: Instant, provider: String) = buildJsonObject {
        put("id", id)
        put("kg", 87.25)
        put("at", at.toString())
        put("offset", "+05:30:15")
        put("provider", provider)
        put("sourceId", id)
        put("origin", "Actual provider")
        put("retainedUnknown", "Original metadata")
    }

    private fun review(version: PlanVersion = CoachingArchiveFixtures.version()) =
        WeeklyCoaching.Review(
            "weekly-review",
            version.id(),
            StoredDate.parse("2026-11-09"),
            2,
            1,
            5.0,
            1,
            5.0,
            listOf("Retained observation / café 🏃", "Missing effort remains unknown"),
            listOf(
                WeeklyCoaching.Channel(
                    "Running",
                    WeeklyCoaching.State.INSUFFICIENT_EVIDENCE,
                    "Limited recorded evidence",
                )
            ),
            "Review the optional change",
            listOf(
                WeeklyCoaching.Choice("keep", "Keep the programme", emptyList()),
                WeeklyCoaching.Choice(
                    "shorter",
                    "Shorter future run",
                    listOf(version.plan().workouts().last().copy(distanceKm = 3.0)),
                ),
            ),
        )

    @Test
    fun manualWeightCommandsKeepProviderProvenanceAndNeverCreateTrainingEvidence() =
        storage { db, context, _, ids ->
            val imported = weight("retained-import", now, "health-kit")
            insert(db, "WEIGHT", imported)
            val before = db.query("SELECT payload FROM coach_evidence WHERE id='retained-import'")
            val manual =
                org.racedaydreams.domain.body.BodyWeight(
                    "manual-weight",
                    87.125,
                    now,
                    Offsets.ofTotalSeconds(19815),
                    "manual",
                    "manual-weight",
                    null,
                )
            context.saveWeight(manual)
            assertEquals(manual, context.weights().single { it.id() == manual.id() })
            assertEquals(
                listOf("manual-weight"),
                SqliteDevelopmentArchive(db).snapshot().records.map { it.id },
            )
            val edited =
                org.racedaydreams.domain.body.BodyWeight(
                    manual.id(),
                    86.75,
                    manual.at(),
                    manual.offset(),
                    manual.provider(),
                    manual.sourceId(),
                    manual.origin(),
                )
            context.saveWeight(edited)
            assertEquals(2, context.weights().size)
            assertEquals(edited, context.weights().single { it.id() == manual.id() })
            context.deleteWeight(manual.id())
            context.deleteWeight(manual.id())
            assertEquals(
                before,
                db.query("SELECT payload FROM coach_evidence WHERE id='retained-import'"),
            )
            assertEquals(listOf("retained-import"), context.weights().map { it.id() })
            assertTrue(db.query("SELECT * FROM strength_sessions").isEmpty())
            assertTrue(db.query("SELECT * FROM activities").isEmpty())
            assertEquals(0, ids())
        }

    @Test
    fun authoritativeWeightWindowIsHalfOpenScopedTransactionalAndRepeatable() =
        storage { db, context, _, _ ->
            val until = StoredInstant.parse("2026-11-18T09:00:00Z")
            insert(db, "WEIGHT", weight("manual", now, "manual"))
            insert(db, "WEIGHT", weight("other-provider", now, "health-kit"))
            insert(db, "WEIGHT", weight("old-at-start", now, "health-connect"))
            insert(db, "WEIGHT", weight("keep-at-end", until, "health-connect"))
            fun snapshot() = db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id")
            val before = snapshot()
            val replacement =
                org.racedaydreams.domain.body.BodyWeight(
                    "replacement",
                    87.5,
                    now,
                    Offsets.ofTotalSeconds(0),
                    "health-connect",
                    "actual-source",
                    "provider-origin",
                )
            val foreign =
                org.racedaydreams.domain.body.BodyWeight(
                    "foreign",
                    90.0,
                    now,
                    Offsets.ofTotalSeconds(0),
                    "manual",
                    "foreign",
                    null,
                )
            assertFailsWith<IllegalArgumentException> {
                context.replaceWeightWindow(
                    "health-connect",
                    now,
                    until,
                    listOf(replacement, foreign),
                )
            }
            assertEquals(
                before,
                snapshot(),
                "A late scope failure must roll back deletions AND earlier insertions",
            )
            context.replaceWeightWindow("health-connect", now, until, listOf(replacement))
            assertEquals(
                setOf("manual", "other-provider", "keep-at-end", "replacement"),
                context.weights().map { it.id() }.toSet(),
            )
            assertEquals(replacement, context.weights().single { it.id() == replacement.id() })
            val after = snapshot()
            context.replaceWeightWindow("health-connect", now, until, listOf(replacement))
            assertEquals(after, snapshot())
            assertEquals(
                before.filter { it[1].sqlText() != "old-at-start" },
                after.filter { it[1].sqlText() != "replacement" },
            )
        }

    @Test
    fun historicalCommandsRetainFrozenDefinitionsUnknownDatesAndMeasurementModalities() =
        storage { db, context, coaching, ids ->
            val exercise = LegacyFingerprintFixtures.session().exercises().first().exercise()
            val old =
                HistoricalStrengthRecord(
                    "old-load",
                    exercise,
                    82.25,
                    6,
                    null,
                    HistoricalStrengthRecord.DatePrecision.UNKNOWN,
                    "Original note",
                    null,
                    null,
                )
            val plank = ExerciseCatalog.definition("plank")!!.exercise()
            val timed =
                HistoricalStrengthRecord(
                    "old-time",
                    plank,
                    0.0,
                    0,
                    StoredDate.parse("2026-11-16"),
                    HistoricalStrengthRecord.DatePrecision.APPROXIMATE,
                    "Retained timed effort",
                    75,
                    null,
                )
            context.saveHistorical(old)
            context.saveHistorical(timed)
            assertEquals(listOf(old, timed), context.historical())
            assertTrue(coaching.strength().isEmpty())
            val before = db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id")
            assertFailsWith<IllegalStateException> {
                db.transaction {
                    context.deleteHistorical(old.id())
                    error("Reject enclosing edit")
                }
            }
            assertEquals(
                before,
                db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id"),
            )
            context.deleteHistorical(timed.id())
            context.deleteHistorical(timed.id())
            assertEquals(listOf(old), context.historical())
            assertEquals(0, ids())
        }

    @Test
    fun legacyLinksProjectGenericAndProgrammeTargetsButCannotRewriteExplicitChoices() =
        storage { db, context, _, ids ->
            val date = StoredDate.parse("2026-11-16")
            context.linkStrength("actual", date)
            assertEquals(date, context.strengthLinks()["actual"])
            assertTrue(context.gymLinks().isEmpty())
            assertEquals(
                listOf(SqlValue.Null, SqlValue.Null, SqlValue.Text(date.toString()), SqlValue.Null),
                db.query(
                        "SELECT programme_key,session_id,planned_date,decision_id FROM strength_association_targets"
                    )
                    .single(),
            )
            context.linkGym("actual", date, "programme:v2", "prescription")
            assertEquals("programme:v2", context.gymLinks().getValue("actual").versionKey())
            // Projection protects an existing decision identity without decoding/revising its
            // payload.
            db.execute(
                "INSERT INTO strength_association_decisions VALUES('explicit','actual','2026-11-17T09:00:00Z','{\"retained\":true}')"
            )
            db.execute(
                "UPDATE strength_association_targets SET decision_id='explicit' WHERE actual_id='actual'"
            )
            context.linkGym("actual", date, "programme:v2", "prescription")
            val before = db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id")
            val target = db.query("SELECT * FROM strength_association_targets")
            assertFailsWith<IllegalArgumentException> {
                context.linkStrength("actual", date.plusDays(1))
            }
            assertFailsWith<IllegalArgumentException> { context.deleteStrengthLink("actual") }
            assertEquals(
                before,
                db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id"),
            )
            assertEquals(target, db.query("SELECT * FROM strength_association_targets"))
            assertEquals(0, ids())
            assertTrue(db.query("PRAGMA foreign_key_check").isEmpty())
        }

    @Test
    fun failedLegacyProjectionAndOuterRollbackRetainExactLinkEvidence() =
        storage { db, context, _, _ ->
            val date = StoredDate.parse("2026-11-16")
            context.linkGym("actual", date, "programme:v2", "prescription")
            val before = db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id")
            val targets = db.query("SELECT * FROM strength_association_targets")
            db.execute(
                "CREATE TRIGGER reject_projection BEFORE INSERT ON strength_association_targets BEGIN SELECT RAISE(ABORT,'injected projection failure'); END"
            )
            assertFails { context.linkStrength("actual", date.plusDays(1)) }
            assertEquals(
                before,
                db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id"),
            )
            assertEquals(targets, db.query("SELECT * FROM strength_association_targets"))
            db.execute("DROP TRIGGER reject_projection")
            assertFailsWith<IllegalStateException> {
                db.transaction {
                    context.linkStrength("new", date)
                    error("Reject enclosing save")
                }
            }
            assertEquals(
                before,
                db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id"),
            )
            assertEquals(targets, db.query("SELECT * FROM strength_association_targets"))
            context.deleteStrengthLink("actual")
            context.deleteStrengthLink("actual")
            assertTrue(context.strengthLinks().isEmpty())
            assertTrue(db.query("SELECT * FROM strength_association_targets").isEmpty())
        }

    @Test
    fun importedAndManualWeightsRemainVisibleInTimeOrderWithoutBecomingUserOwnedExports() =
        storage { db, context, _, _ ->
            val recent = weight("a-later", now, "manual")
            val earlier = weight("z-earlier", CoachingArchiveFixtures.at, "health-kit")
            insert(db, "WEIGHT", recent)
            insert(db, "WEIGHT", earlier)
            val before = db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id")
            assertEquals(listOf("z-earlier", "a-later"), context.weights().map { it.id() })
            assertEquals(listOf("health-kit", "manual"), context.weights().map { it.provider() })
            assertEquals(Offsets.ofTotalSeconds(19815), context.weights().first().offset())
            assertEquals(
                listOf("a-later"),
                SqliteDevelopmentArchive(db).snapshot().records.map { it.id },
            )
            assertEquals(
                before,
                db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id"),
            )
            assertTrue(db.query("SELECT * FROM strength_sessions").isEmpty())
        }

    @Test
    fun unknownHistoricalDateAndOriginalGenericVersusGymLinksRemainDistinct() =
        storage { db, context, _, _ ->
            val exercise = LegacyFingerprintFixtures.session().exercises().first().exercise()
            insert(
                db,
                "HISTORICAL_PB",
                buildJsonObject {
                    put("id", "old-pb")
                    put("exercise", StrengthEvidenceJson.exercise(exercise))
                    put("kg", 82.25)
                    put("reps", 6)
                    put("date", JsonNull)
                    put("precision", "UNKNOWN")
                    put("notes", "An old lift, not a fabricated workout")
                    put("seconds", JsonNull)
                    put("metres", JsonNull)
                },
            )
            for (id in listOf("generic", "gym")) insert(
                db,
                "STRENGTH_LINK",
                buildJsonObject {
                    put("id", id)
                    put("plannedDate", "2026-11-16")
                    if (id == "gym") {
                        put("gymVersion", "programme:v2")
                        put("gymSession", "session")
                    }
                },
            )
            val before = db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id")
            assertNull(context.historical().single().date())
            assertEquals(
                HistoricalStrengthRecord.DatePrecision.UNKNOWN,
                context.historical().single().precision(),
            )
            assertEquals(setOf("generic", "gym"), context.strengthLinks().keys)
            assertEquals(setOf("gym"), context.gymLinks().keys)
            assertEquals("programme:v2", context.gymLinks().getValue("gym").versionKey())
            assertEquals(
                before,
                db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id"),
            )
            assertTrue(db.query("SELECT * FROM strength_sessions").isEmpty())
        }

    @Test
    fun retainedInterruptionAndExplicitSessionDecisionsKeepDatesNanosecondsAndOrder() =
        storage { db, context, _, _ ->
            val interruption =
                WeeklyCoaching.Interruption(
                    StoredDate.parse("2026-10-01"),
                    StoredDate.parse("2026-10-20"),
                    "Time away",
                )
            context.saveInterruption(interruption)
            val earlier =
                WorkoutDecision(
                    "z-first",
                    "done",
                    "v1",
                    CoachingArchiveFixtures.start,
                    null,
                    WorkoutDecision.Kind.SKIPPED,
                    CoachingArchiveFixtures.at,
                )
            val later =
                WorkoutDecision(
                    "a-later",
                    "future",
                    "v1",
                    CoachingArchiveFixtures.start,
                    CoachingArchiveFixtures.start.plusDays(2),
                    WorkoutDecision.Kind.MOVED,
                    now,
                )
            context.saveSessionDecision(later)
            context.saveSessionDecision(earlier)
            assertEquals(interruption, context.interruption())
            assertEquals(listOf(earlier, later), context.sessionDecisions())
            assertEquals(now, context.sessionDecisions().last().at())
            assertNull(context.reviewDecision("unknown"))
            assertFalse(context.reviewKnown("unknown"))
            assertEquals(3, SqliteDevelopmentArchive(db).snapshot().records.size)
            assertTrue(db.query("SELECT * FROM coach_plan_versions").isEmpty())
        }

    @Test
    fun savingAnOptionalReviewKeepsCompleteContextButDoesNotActivateAnyChoice() =
        storage { db, context, coaching, ids ->
            val original = CoachingArchiveFixtures.version()
            coaching.saveCurrent(original)
            val review = review(original)
            context.saveReview(review)
            assertTrue(context.reviewKnown(requireNotNull(review.id())))
            assertNull(context.reviewDecision(requireNotNull(review.id())))
            val stored =
                StoredJson.objectValue(
                    db.query("SELECT payload FROM coach_evidence WHERE kind='WEEKLY_REVIEW'")
                        .single()
                        .single()
                        .sqlText()
                )
            assertEquals(
                JsonArray(review.observations().map { JsonPrimitive(it) }),
                stored["observations"],
            )
            assertEquals(
                "INSUFFICIENT_EVIDENCE",
                stored
                    .getValue("channels")
                    .jsonArray
                    .single()
                    .jsonObject
                    .getValue("state")
                    .jsonPrimitive
                    .content,
            )
            val replacement =
                stored
                    .getValue("choices")
                    .jsonArray
                    .last()
                    .jsonObject
                    .getValue("replacements")
                    .jsonArray
                    .single()
                    .jsonObject
            assertEquals(
                review.choices().last().replacements().single(),
                PlannedWorkoutJson.read(replacement),
            )
            assertEquals(original, coaching.current())
            assertEquals(listOf(original), coaching.versions())
            assertEquals(0, ids())
            assertEquals(1, SqliteDevelopmentArchive(db).snapshot().records.size)
        }

    @Test
    fun keepIsIdempotentlyRecordedWithoutRevisionAndContextMatchingUsesActualObservations() =
        storage { _, context, coaching, ids ->
            val original = CoachingArchiveFixtures.version()
            coaching.saveCurrent(original)
            val review = review(original)
            context.saveReview(review)
            context.decide(review, "keep", coaching, emptySet())
            assertEquals("keep", context.reviewDecision(requireNotNull(review.id())))
            assertEquals(
                "keep",
                context.decisionForReview(
                    review.copy(id = "new-review", baseVersionId = "another-base")
                ),
            )
            assertNull(
                context.decisionForReview(review.copy(recommendation = "Changed recommendation"))
            )
            assertNull(
                context.decisionForReview(
                    review.copy(observations = review.observations().reversed())
                )
            )
            assertFailsWith<IllegalArgumentException> {
                context.decide(review, "keep", coaching, emptySet())
            }
            assertEquals(listOf(original), coaching.versions())
            assertEquals(0, ids())
        }

    @Test
    fun anAcceptedChangeCreatesOneRevisionAndPreservesCompletedPrescriptionsAndActualResponse() =
        storage { db, context, coaching, ids ->
            val original = CoachingArchiveFixtures.version()
            coaching.saveCurrent(original)
            val actual =
                RunExecution(
                    "recorded-run",
                    "done",
                    original.id(),
                    null,
                    null,
                    "Actual remains retained",
                )
            coaching.saveResponse(actual)
            val review = review(original)
            context.saveReview(review)
            context.decide(review, "shorter", coaching, setOf("done"))
            val current = requireNotNull(coaching.current())
            assertEquals(2, current.version())
            assertEquals("athlete-choice-1", current.id())
            assertEquals(now, current.createdAt())
            assertEquals(original, coaching.versions().single { it.id() == original.id() })
            assertEquals(original.plan().workouts().first(), current.plan().workouts().first())
            assertEquals(3.0, current.plan().workouts().last().distanceKm())
            assertEquals(listOf(actual), coaching.responses())
            assertEquals("shorter", context.reviewDecision(requireNotNull(review.id())))
            assertEquals(1, ids())
            assertTrue(db.query("PRAGMA foreign_key_check").isEmpty())
        }

    @Test
    fun staleUnknownAndCompletedChangesFailBeforeReplacingHistoryOrSavingAReceipt() =
        storage { db, context, coaching, ids ->
            val original = CoachingArchiveFixtures.version()
            coaching.saveCurrent(original)
            val review = review(original)
            context.saveReview(review)
            val before = db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id")
            assertFailsWith<IllegalArgumentException> {
                context.decide(
                    review.copy(baseVersionId = "stale"),
                    "shorter",
                    coaching,
                    emptySet(),
                )
            }
            assertFailsWith<IllegalArgumentException> {
                context.decide(review, "unknown", coaching, emptySet())
            }
            assertFailsWith<IllegalArgumentException> {
                context.decide(review, "shorter", coaching, setOf("future"))
            }
            assertEquals(original, coaching.current())
            assertEquals(listOf(original), coaching.versions())
            assertEquals(
                before,
                db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id"),
            )
            assertEquals(0, ids())
            assertNull(context.reviewDecision(requireNotNull(review.id())))
        }

    @Test
    fun outerRejectionRollsBackTheChosenRevisionAndReviewReceiptTogether() =
        storage { db, context, coaching, _ ->
            val original = CoachingArchiveFixtures.version()
            coaching.saveCurrent(original)
            val review = review(original)
            context.saveReview(review)
            val before = db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id")
            assertFailsWith<IllegalStateException> {
                db.transaction {
                    context.decide(review, "shorter", coaching, emptySet())
                    assertEquals(2, coaching.current()?.version())
                    assertEquals("shorter", context.reviewDecision(requireNotNull(review.id())))
                    error("Reject the enclosing operation")
                }
            }
            assertEquals(original, coaching.current())
            assertEquals(listOf(original), coaching.versions())
            assertEquals(
                before,
                db.query("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id"),
            )
            assertNull(context.reviewDecision(requireNotNull(review.id())))
        }
}
