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
}
