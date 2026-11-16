package org.racedaydreams.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.*
import org.racedaydreams.storage.*

@RunWith(AndroidJUnit4::class)
class SharedProgrammingArchiveTest {
    private val at = Instant.parse("2026-11-16T09:00:00.123456789Z")
    private val start = LocalDate.of(2026, 11, 17)
    private val ref = Reference(Owner.AUTHORED, "programme", 1)

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
                        listOf(
                            GymWorkoutDefinition.Slot(
                                "slot",
                                ExerciseCatalog.definition("bench-press")!!.exercise(),
                                target,
                                null,
                            )
                        ),
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

    private fun decision() =
        Decision("activate", null, ref, at, "Athlete chose", emptyList(), emptyList())

    private fun snapshot() =
        StrengthProgrammingArchiveJson.Snapshot(
            mapOf(ref.key() to programme()),
            emptyList(),
            emptyList(),
            emptyList(),
            listOf(decision()),
            ref,
        )

    private fun forward(): ConcreteStrengthProgrammeDraft {
        val p = programme()
        val old = p.workouts()[1]
        val slot = old.definition().slots().single()
        val replacement = ExerciseCatalog.definition("dumbbell-bench-press")!!.exercise()
        val updated =
            old.copy(
                definition =
                    old.definition()
                        .copy(
                            revision = 2,
                            slots =
                                listOf(
                                    slot.copy(
                                        exercise = replacement,
                                        target = slot.target()!!.withoutLoadReference(),
                                    )
                                ),
                        )
            )
        return ConcreteStrengthProgrammeDraft(
            "draft",
            5,
            ref,
            p.copy(
                revision = 2,
                parentRevision = 1,
                blocks =
                    listOf(p.blocks().single().copy(workouts = listOf(p.workouts()[0], updated))),
                effectiveFrom = old.date(),
                reason = "Athlete chose an alternative",
            ),
            listOf(
                StrengthProgrammeEditor.Change(
                    old.id(),
                    slot.id(),
                    slot.exercise().id(),
                    replacement.id(),
                    "Equipment unavailable",
                )
            ),
        )
    }

    private fun compare(expected: StrengthProgrammingArchiveJson.Snapshot) {
        val payload = StrengthProgrammingArchiveJson.encode(expected)
        val old = StrengthProgrammingArchive.validate(JSONObject(payload), null)
        assertEquals(expected.programmes, old.programmes())
        assertEquals(expected.drafts, old.drafts())
        assertEquals(expected.concreteDrafts, old.concreteDrafts())
        assertEquals(expected.custom, old.custom())
        assertEquals(expected.decisions, old.decisions())
        assertEquals(expected.current, old.current())
        assertEquals(expected, StrengthProgrammingArchiveJson.decode(payload))
        for (d in expected.decisions) {
            assertEquals(
                d,
                StrengthProgrammingDecisionJson.decode(
                    StrengthProgrammingArchive.decision(d).toString()
                ),
            )
            assertEquals(
                d,
                StrengthProgrammingArchive.decision(
                    JSONObject(StrengthProgrammingDecisionJson.encode(d))
                ),
            )
        }
    }

    @Test
    fun bothArchiveVersionsAndIndependentConcretePreviewsPreserveTheSameGraph() {
        compare(snapshot())
        val oldVersion = JSONObject(StrengthProgrammingArchiveJson.encode(snapshot()))
        oldVersion.remove("concreteDrafts")
        oldVersion.put("version", 1)
        val v1 = oldVersion.toString()
        assertEquals(snapshot(), StrengthProgrammingArchiveJson.decode(v1))
        assertEquals(
            snapshot().programmes,
            StrengthProgrammingArchive.validate(JSONObject(v1), null).programmes(),
        )
        compare(snapshot().copy(concreteDrafts = listOf(forward())))
        val initial =
            snapshot()
                .copy(
                    programmes = emptyMap(),
                    decisions = emptyList(),
                    current = null,
                    concreteDrafts =
                        listOf(
                            ConcreteStrengthProgrammeDraft("new", 3, null, programme(), emptyList())
                        ),
                )
        compare(initial)
    }

    @Test
    fun actualDecisionProvenanceAndProtectedWorkoutSubstitutionsRemainSeparateFromPreviews() {
        val d = forward()
        val after = Reference(Owner.AUTHORED, "programme", 2)
        val choice =
            Decision(
                "swap",
                ref,
                after,
                at,
                "Athlete accepted",
                listOf("workout-0"),
                d.substitutions(),
            )
        compare(
            snapshot()
                .copy(
                    programmes =
                        linkedMapOf(ref.key() to programme(), after.key() to d.programme()),
                    decisions = listOf(decision(), choice),
                    current = after,
                    concreteDrafts =
                        listOf(
                            d,
                            d.copy(
                                id = "competing",
                                programme = d.programme().copy(name = "Competing preview"),
                            ),
                        ),
                )
        )
        compare(
            snapshot()
                .copy(
                    decisions =
                        listOf(
                            decision(),
                            Decision(
                                "stop",
                                ref,
                                null,
                                at,
                                "Athlete stopped",
                                emptyList(),
                                emptyList(),
                            ),
                        ),
                    current = null,
                )
        )
    }

    @Test
    fun bothReadersRejectOrphanCurrentRevisionsAndInvalidDecisionHistory() {
        val cases =
            listOf(
                snapshot().copy(decisions = emptyList()),
                snapshot().copy(current = null),
                snapshot().copy(decisions = listOf(decision(), decision())),
                snapshot().copy(decisions = listOf(decision().copy(at = at.minusSeconds(1)))),
            )
        for (s in cases) {
            val payload = StrengthProgrammingArchiveJson.encode(s)
            assertThrows(IllegalArgumentException::class.java) {
                StrengthProgrammingArchiveJson.decode(payload)
            }
            assertThrows(IllegalArgumentException::class.java) {
                StrengthProgrammingArchive.validate(JSONObject(payload), null)
            }
        }
        val d =
            Decision(
                "swap",
                ref,
                Reference(Owner.AUTHORED, "programme", 2),
                at,
                "Reviewed",
                listOf("\u00a0"),
                forward().substitutions(),
            )
        assertEquals(
            d,
            StrengthProgrammingArchive.decision(
                JSONObject(StrengthProgrammingDecisionJson.encode(d))
            ),
        )
        val bad = StrengthProgrammingDecisionJson.encode(d).replace("\u00a0", "\u2003")
        assertThrows(IllegalArgumentException::class.java) {
            StrengthProgrammingDecisionJson.decode(bad)
        }
        assertThrows(IllegalArgumentException::class.java) {
            StrengthProgrammingArchive.decision(JSONObject(bad))
        }
    }

    private fun database(
        block:
            (
                android.database.sqlite.SQLiteDatabase,
                SqliteStrengthProgrammingRecords,
                SqliteStrengthProgrammingRepository,
            ) -> Unit
    ) {
        val context =
            androidx.test.core.app.ApplicationProvider.getApplicationContext<
                android.content.Context
            >()
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
        try {
            val db = ProfileDatabase.get(context).writableDatabase
            block(
                db,
                SqliteStrengthProgrammingRecords(AndroidSqlTestConnection(db)),
                SqliteStrengthProgrammingRepository(context),
            )
        } finally {
            ProfileDatabase.get(context).close()
            context.deleteDatabase(ProfileDatabase.NAME)
        }
    }

    @Test
    fun normalizedSharedReadersOpenOriginalProgrammeDraftCustomAndConcreteRows() =
        database { db, shared, original ->
            val draft =
                StrengthProgrammeDraft(
                    "weekly",
                    3,
                    "programme",
                    1,
                    "",
                    programme().settings(),
                    start,
                    emptyList(),
                    StrengthProgramme.Origin.CUSTOM,
                    null,
                )
            val e =
                StrengthExercise(
                    "custom-press",
                    "My press",
                    StrengthExercise.Region.UPPER,
                    StrengthExercise.Mode.WEIGHTED,
                    false,
                    StrengthExercise.MeasurementProfile.LOAD_REPS,
                )
            val custom =
                CustomStrengthExercise(
                    e,
                    ExerciseMetadata.BodyArea.CHEST,
                    ExerciseMetadata.Role.HORIZONTAL_PUSH,
                    ExerciseMetadata.EquipmentRequirement(
                        listOf(setOf(ExerciseMetadata.Equipment.DUMBBELLS))
                    ),
                    "Athlete notes",
                )
            val data =
                snapshot()
                    .copy(
                        drafts = listOf(draft),
                        concreteDrafts = listOf(forward()),
                        custom = listOf(custom),
                    )
            val graph =
                StrengthProgrammingArchive.validate(
                    JSONObject(StrengthProgrammingArchiveJson.encode(data)),
                    null,
                )
            db.beginTransaction()
            try {
                SqliteStrengthProgrammingRepository.restore(db, graph)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            assertEquals(original.programme("programme", 1), shared.programme("programme", 1))
            assertEquals(original.currentReference(), shared.currentReference())
            assertEquals(original.versions(10, 0), shared.versions(10, 0))
            assertEquals(original.draft("weekly"), shared.draft("weekly"))
            assertEquals(original.drafts(10, 0), shared.drafts(10, 0))
            assertEquals(original.customExercises(), shared.customExercises())
            assertEquals(original.customExercise(e.id()), shared.customExercise(e.id()))
            assertEquals(original.decisions(), shared.decisions())
            val concrete =
                SqliteConcreteStrengthProgrammeDraftRepository(
                    androidx.test.core.app.ApplicationProvider.getApplicationContext()
                )
            assertEquals(concrete.get("draft"), shared.concreteDraft("draft"))
            assertEquals(concrete.list(10, 0), shared.concreteDrafts(10, 0))
            assertTrue(shared.versions(1, 1).isEmpty())
            assertEquals(ref, original.currentReference())
        }

    @Test
    fun bothNormalizedReadersRejectMismatchedSummaryAndMissingOrderWithoutRepairingHistory() =
        database { db, shared, original ->
            val graph =
                StrengthProgrammingArchive.validate(
                    JSONObject(StrengthProgrammingArchiveJson.encode(snapshot())),
                    null,
                )
            db.beginTransaction()
            try {
                SqliteStrengthProgrammingRepository.restore(db, graph)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            db.execSQL("UPDATE strength_programme_revisions SET name='Corrupt index'")
            assertThrows(IllegalArgumentException::class.java) { shared.programme("programme", 1) }
            assertThrows(IllegalArgumentException::class.java) {
                original.programme("programme", 1)
            }
            db.execSQL("UPDATE strength_programme_revisions SET name='Original'")
            db.execSQL("DELETE FROM strength_programme_sessions WHERE position=0")
            assertThrows(IllegalArgumentException::class.java) { shared.programme("programme", 1) }
            assertThrows(IllegalArgumentException::class.java) {
                original.programme("programme", 1)
            }
            db.rawQuery("SELECT COUNT(*) FROM strength_programme_sessions", null).use {
                assertTrue(it.moveToFirst())
                assertEquals(1L, it.getLong(0))
            }
        }

    @Test
    fun originalAndSharedCurrentReferenceReadersRejectCompetingOwners() =
        database { db, shared, original ->
            val graph =
                StrengthProgrammingArchive.validate(
                    JSONObject(StrengthProgrammingArchiveJson.encode(snapshot())),
                    null,
                )
            db.beginTransaction()
            try {
                SqliteStrengthProgrammingRepository.restore(db, graph)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            db.execSQL("INSERT INTO gym_plan_versions VALUES('legacy:v1','legacy',1,'{}')")
            db.execSQL("INSERT INTO gym_current VALUES(1,'legacy:v1')")
            assertThrows(IllegalArgumentException::class.java) { shared.currentReference() }
            assertThrows(IllegalArgumentException::class.java) { original.currentReference() }
            db.execSQL("DELETE FROM strength_programme_current")
            assertEquals(Reference(Owner.LEGACY, "legacy", 1), shared.currentReference())
            assertEquals(original.currentReference(), shared.currentReference())
            assertEquals(programme(), shared.programme("programme", 1))
        }

    @Test
    fun revisionReviewMatchesOriginalRetainedActualProjectionAndRefreshesAfterDeletion() =
        database { db, shared, original ->
            val graph =
                StrengthProgrammingArchive.validate(
                    JSONObject(StrengthProgrammingArchiveJson.encode(snapshot())),
                    null,
                )
            db.beginTransaction()
            try {
                SqliteStrengthProgrammingRepository.restore(db, graph)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            assertEquals(original.reviewRevision(ref), shared.reviewRevision(ref))
            val actual =
                StrengthSession(
                    "recorded",
                    at.minusSeconds(86400),
                    ZoneOffset.UTC,
                    Duration.ofMinutes(30),
                    null,
                    "Recorded actual",
                    listOf(
                        StrengthSession.ExerciseSets(
                            ExerciseCatalog.definition("bench-press")!!.exercise(),
                            listOf(StrengthSession.SetEntry("set", 6, 80.0, 7.0, false, null, null)),
                        )
                    ),
                )
            CoachingStore(androidx.test.core.app.ApplicationProvider.getApplicationContext())
                .saveStrength(actual)
            db.execSQL(
                "INSERT INTO strength_association_targets(actual_id,programme_key,session_id,planned_date) VALUES('recorded','programme:v1','workout-0','2026-11-17')"
            )
            db.execSQL(
                "INSERT INTO strength_association_targets(actual_id,programme_key,session_id,planned_date) VALUES('missing','programme:v1','workout-1','2026-11-17')"
            )
            val before = shared.reviewRevision(ref)
            assertEquals(setOf("workout-0"), before.completedWorkoutIds())
            assertEquals(original.reviewRevision(ref), before)
            db.execSQL("DELETE FROM strength_sessions WHERE id='recorded'")
            assertEquals(original.reviewRevision(ref), shared.reviewRevision(ref))
            assertTrue(shared.reviewRevision(ref).completedWorkoutIds().isEmpty())
            assertEquals(setOf("workout-0"), before.completedWorkoutIds())
            assertEquals(ref, original.currentReference())
        }
}
