package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.polish.StartingLoad
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.StrengthProgressionAdvisor.Action
import org.racedaydreams.domain.time.*

class StrengthRecommendationContractTest {
    private val now = Instants.ofEpochSecond(1793188800, 500000000)
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()

    private fun at(days: Long, nano: Long = 500000000) =
        Instants.ofEpochSecond(now.getEpochSecond() - days * 86400, nano)

    private fun target(
        e: StrengthExercise = bench,
        effort: GymWorkoutDefinition.EffortRange? =
            GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RPE, 6.0, 8.0),
    ) =
        GymWorkoutDefinition.Target(
            2,
            if (e.measurement().usesReps()) 8 else null,
            if (e.measurement().usesReps()) 12 else null,
            if (e.measurement().usesDuration()) 30 else null,
            if (e.measurement().usesDistance()) 20.0 else null,
            effort,
            90,
            null,
        )

    private fun p() =
        GymProgramme.Prescription(bench.id(), 2, 8, 12, null, null, 6, 8, 90, "Retained")

    private fun session(
        id: String,
        e: StrengthExercise = bench,
        at: Instant = at(1),
        load: Double = if (e.measurement().usesLoad()) 80.0 else 0.0,
        effort: Double? = 7.0,
        warmUp: Boolean = false,
        success: Boolean = true,
        rir: Double? = null,
    ): StrengthSession {
        val sets =
            (0..1).map { n ->
                StrengthSession.SetEntry(
                    "$id-$n",
                    if (e.measurement().usesReps()) if (success) 12 else 5 else 0,
                    load,
                    effort,
                    warmUp,
                    if (e.measurement().usesDuration()) if (success) 30 else 10 else null,
                    if (e.measurement().usesDistance()) if (success) 20.0 else 10.0 else null,
                    rir,
                )
            }
        return StrengthSession(
            id,
            at,
            Offsets.ofTotalSeconds(0),
            Durations.ofMinutes(30),
            null,
            "",
            listOf(StrengthSession.ExerciseSets(e, sets)),
        )
    }

    private fun review(
        history: List<StrengthSession>,
        t: GymWorkoutDefinition.Target = target(),
        e: StrengthExercise = bench,
    ) =
        StrengthTargetProgression.review(
            GymWorkoutDefinition.Slot("slot", e, t, null),
            history,
            now,
        )

    @Test
    fun startingReferenceRequiresActualRecentExactWeightedSetsWithEffort() {
        val history = listOf(session("one"))
        val before = history.toString()
        val reference = StartingLoad.suggest(p(), history, now)!!
        assertEquals(76.0, reference.lowKg())
        assertEquals(80.0, reference.highKg())
        assertTrue(reference.reason()!!.startsWith("Optional reference"))
        assertEquals(before, history.toString())
        for (h in
            listOf(
                emptyList(),
                listOf(session("missing", effort = null)),
                listOf(session("warm", warmUp = true)),
                listOf(session("hard", effort = 9.0)),
                listOf(session("future", at = at(-1))),
                listOf(session("stale", at = at(29))),
            )) assertNull(StartingLoad.suggest(p(), h, now))
        val other = p().copy(exerciseId = "deadlift")
        assertNull(StartingLoad.suggest(other, history, now))
        val bodyweight = ExerciseCatalog.definition("weighted-pull-up")!!.exercise()
        assertNull(
            StartingLoad.suggest(
                p().copy(exerciseId = bodyweight.id()),
                listOf(session("bw", bodyweight)),
                now,
            )
        )
    }

    @Test
    fun fractionalFreshnessBoundaryAppliesToEachSupportingSession() {
        val recent = session("recent")
        val boundary = session("boundary", at = at(28))
        val stale = session("stale", at = at(28, 499999999))
        assertNotNull(StartingLoad.suggest(p(), listOf(boundary), now))
        assertNull(StartingLoad.suggest(p(), listOf(stale), now))
        assertEquals(Action.CONSIDER_INCREASE, review(listOf(recent, boundary)).action())
        assertEquals(Action.HOLD, review(listOf(recent, stale)).action())
        assertEquals(
            Action.HOLD,
            StrengthProgressionAdvisor.review(p(), listOf(stale), now).action(),
        )
        assertEquals(
            Action.GATHER_EVIDENCE,
            StrengthProgressionAdvisor.review(p(), emptyList(), now).action(),
        )
    }

    @Test
    fun allSupportedModalitiesUseProvenTaskAndNeverInventAddedLoad() {
        for (id in
            listOf(
                "bench-press",
                "push-up",
                "weighted-pull-up",
                "plank",
                "weighted-plank",
                "farmers-carry",
            )) {
            val e = ExerciseCatalog.definition(id)!!.exercise()
            val t = target(e)
            val h = listOf(session("recent", e), session("older", e, at(3)))
            val before = h.toString()
            val a = review(h, t, e)
            assertEquals(Action.CONSIDER_INCREASE, a.action(), id)
            assertEquals(listOf("recent", "older"), a.supportingSessionIds())
            val proposed = a.proposedTarget()!!
            if (e.measurement().usesReps() && e.measurement().usesLoad())
                assertEquals(82.0, proposed.referenceLoadKg())
            else if (e.measurement().usesReps()) {
                assertEquals(13, proposed.maximumReps())
                assertNull(proposed.referenceLoadKg())
            } else if (e.measurement().usesDuration()) assertEquals(35L, proposed.seconds())
            else assertEquals(25.0, proposed.metres())
            if (!e.measurement().usesReps() && e.measurement().usesLoad())
                assertEquals(80.0, proposed.referenceLoadKg())
            assertEquals(before, h.toString())
            assertNull(t.referenceLoadKg())
            assertEquals(t.effort(), proposed.effort())
            assertEquals(t.restSeconds(), proposed.restSeconds())
        }
    }

    @Test
    fun missingEffortAndPrescribedReserveStaySeparateAndConservative() {
        val noEffort = listOf(session("a", effort = null), session("b", at = at(2), effort = null))
        assertEquals(Action.HOLD, review(noEffort).action())
        assertEquals(Action.HOLD, StrengthProgressionAdvisor.review(p(), noEffort, now).action())
        assertEquals(
            Action.HOLD,
            review(listOf(session("a"), session("b", at = at(2))), target(effort = null)).action(),
        )
        val rir = GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RIR, 2.0, 4.0)
        val h =
            listOf(
                session("a", effort = null, rir = 3.0),
                session("b", at = at(2), effort = null, rir = 3.0),
            )
        val advice = review(h, target(effort = rir))
        assertEquals(Action.CONSIDER_INCREASE, advice.action())
        assertEquals(rir, advice.proposedTarget()!!.effort())
        assertNull(h.first().exercises().single().sets().first().rpe())
        assertEquals(3.0, h.first().exercises().single().sets().first().repetitionsInReserve())
    }

    @Test
    fun duplicateIdentityAndUnknownOrChangedTasksCannotCreateTaskProgression() {
        val one = session("one")
        assertEquals(Action.HOLD, review(listOf(one, one)).action())
        assertEquals(Action.HOLD, review(listOf(one)).action())
        assertEquals(
            Action.GATHER_EVIDENCE,
            review(listOf(session("warm", warmUp = true))).action(),
        )
        val custom = bench.copy(id = "custom")
        assertEquals(Action.GATHER_EVIDENCE, review(listOf(one), target(), custom).action())
        val changed = bench.copy(region = StrengthExercise.Region.LOWER)
        assertEquals(Action.GATHER_EVIDENCE, review(listOf(one), target(), changed).action())
        // The retained legacy overload has no duplicate-ID rule. Keep that old API behaviour;
        // authored task advice deliberately has its separate, explicit identity safeguard.
        assertEquals(
            Action.CONSIDER_INCREASE,
            StrengthProgressionAdvisor.review(p(), listOf(one, one), now).action(),
        )
    }

    @Test
    fun underperformanceLoadMismatchAndNewerReferenceCannotBeMasked() {
        assertEquals(
            Action.REDUCE_OR_SIMPLIFY,
            review(listOf(session("hard", effort = 9.0))).action(),
        )
        assertEquals(
            Action.REDUCE_OR_SIMPLIFY,
            review(listOf(session("short", success = false))).action(),
        )
        assertEquals(
            Action.HOLD,
            review(listOf(session("a"), session("b", at = at(2), load = 75.0))).action(),
        )
        val history = listOf(session("a"), session("b", at = at(2)))
        assertEquals(Action.HOLD, review(history, target().copy(referenceLoadKg = 82.5)).action())
        assertNull(review(history, target().copy(referenceLoadKg = 82.5)).proposedTarget())
    }

    @Test
    fun upperBoundsAndOriginalHalfKilogramLoadIncrementsRemainIntact() {
        for ((load, expected) in listOf(20.0 to 20.5, 40.0 to 41.0, 80.0 to 82.0, 1500.0 to null)) {
            val h = listOf(session("a", load = load), session("b", at = at(2), load = load))
            val a = StrengthProgressionAdvisor.review(p(), h, now)
            assertEquals(
                if (expected == null) Action.HOLD else Action.CONSIDER_INCREASE,
                a.action(),
            )
            assertEquals(expected, a.suggestedKilograms())
        }
        val e = ExerciseCatalog.definition("plank")!!.exercise()
        val h = listOf(session("a", e), session("b", e, at(2)))
        // A boundary-sized target cannot be justified by the recorded 30-second holds.
        assertEquals(
            Action.REDUCE_OR_SIMPLIFY,
            review(h, target(e).copy(seconds = 43200), e).action(),
        )
    }

    @Test
    fun evidenceSnapshotsAreFrozenAndRawLegacyRecordContractIsUnchanged() {
        val ids = mutableListOf("a", "b")
        val advice = StrengthTargetProgression.Advice(Action.HOLD, "Keep current", null, ids)
        ids.clear()
        assertEquals(listOf("a", "b"), advice.supportingSessionIds())
        assertFails { (advice.supportingSessionIds() as MutableList<String>).clear() }
        assertFailsWith<IllegalArgumentException> {
            StrengthTargetProgression.Advice(Action.HOLD, "Invalid", target(), emptyList())
        }
        val options = mutableListOf("Raw original DTO")
        val raw = StrengthProgressionAdvisor.Advice(null, null, null, null, options)
        options.clear()
        assertTrue(raw.options()!!.isEmpty())
        assertNull(raw.reason())
        val generated = StrengthProgressionAdvisor.review(p(), emptyList(), now)
        assertFails { (generated.options() as MutableList<String>).clear() }
    }
}
