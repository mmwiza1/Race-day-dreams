package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.health.HealthIntelligence
import org.racedaydreams.domain.recovery.*
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class TrainingRecoveryContractTest {
    private val now = Instants.ofEpochSecond(2000000000)
    private val utc = Offsets.ofTotalSeconds(0)
    private val today = CalendarDates.atOffset(now, utc)

    private fun gym(
        id: String,
        days: Long,
        rpe: Double? = 9.0,
        rir: Double? = null,
        warm: Boolean = false,
    ): StrengthSession {
        val sets =
            listOf(
                StrengthSession.SetEntry("a", 5, 60.0, rpe, warm, null, null, rir),
                StrengthSession.SetEntry("b", 5, 60.0, rpe, warm, null, null, rir),
            )
        return StrengthSession(
            id,
            Instants.ofEpochSecond(now.getEpochSecond() - days * 86400),
            utc,
            Durations.ofMinutes(30),
            null,
            "",
            listOf(
                StrengthSession.ExerciseSets(
                    requireNotNull(ExerciseCatalog.definition("bench-press")).exercise(),
                    sets,
                )
            ),
        )
    }

    private fun review(
        gym: List<StrengthSession> = emptyList(),
        runs: List<ActivityRecord> = emptyList(),
        health: List<HealthIntelligence.Observation> = emptyList(),
    ) = TrainingRecovery.review(gym, runs, health, now, utc)

    private fun programme(
        priority: GymProgramme.Priority = GymProgramme.Priority.BALANCED
    ): StrengthProgramme {
        val settings =
            StrengthProgramme.Settings(
                GymProgramme.Goal.STRENGTH,
                priority,
                GymProgramme.Experience.INTERMEDIATE,
                DayOfWeek.entries.toSet(),
                3,
                60,
                ExerciseMetadata.Equipment.entries.toSet(),
            )
        return StrengthProgrammeGenerator()
            .generate(
                StrengthProgrammeGenerator.Request(
                    "recovery",
                    Instants.ofEpochSecond(now.getEpochSecond() - 10 * 86400),
                    "Original",
                    settings,
                    today.minusDays(7),
                    6,
                    null,
                    PrimaryFocus.HYBRID,
                    emptySet(),
                    null,
                    emptySet(),
                    emptySet(),
                    false,
                )
            )
    }

    @Test
    fun missingEffortAndWarmUpsCannotBecomeMeasuredFatigueOrADeload() {
        val unknown = review(listOf(gym("unknown", 1, null)))
        assertFalse(unknown.lighterWeek())
        assertFalse(unknown.reviewToday())
        assertTrue(unknown.evidence().isEmpty())
        assertTrue(
            unknown.limitations().any {
                it.startsWith("0 recent strength workouts have some effort evidence; 1 do not")
            }
        )
        assertFalse(review(listOf(gym("warm", 1, warm = true))).reviewToday())
    }

    @Test
    fun lighterWeekRequiresRepeatedDistinctDemandingDaysAcrossAtLeastSixDays() {
        assertTrue(review(listOf(gym("a", 1), gym("b", 4), gym("c", 7))).lighterWeek())
        assertFalse(review(listOf(gym("a", 1), gym("b", 2), gym("c", 3))).lighterWeek())
        assertFalse(review(listOf(gym("a", 1), gym("b", 1), gym("c", 1))).lighterWeek())
        assertFalse(review(listOf(gym("old", 15))).reviewToday())
        assertTrue(review(listOf(gym("ongoing", 0))).evidence().isEmpty())
    }

    @Test
    fun duplicateEvidenceIsIdempotentButConflictingActualsAreRejected() {
        val a = gym("a", 1)
        assertEquals(review(listOf(a)), review(listOf(a, a)))
        assertFailsWith<IllegalArgumentException> { review(listOf(a, gym("a", 1, null))) }
    }

    @Test
    fun recordedRepetitionsInReserveRemainDistinctFromPerceivedEffort() {
        assertTrue(review(listOf(gym("rir", 1, null, 1.0))).reviewToday())
        assertTrue(review(listOf(gym("rpe", 1, 9.0))).reviewToday())
        assertFalse(review(listOf(gym("lower", 1, 8.0))).reviewToday())
        assertFalse(review(listOf(gym("unknown", 1, null, null))).reviewToday())
    }

    @Test
    fun runningContextRequiresCompletedRecentActualEffort() {
        fun run(id: String, days: Long, effort: Double?): ActivityRecord {
            val at = Instants.ofEpochSecond(now.getEpochSecond() - days * 86400)
            return ActivityRecord(
                id,
                SourceIdentity("local", id, null),
                at,
                Instants.ofEpochSecond(at.getEpochSecond() + 3600),
                utc,
                utc,
                10000.0,
                null,
                null,
                null,
                null,
                null,
                effort,
                null,
                emptyList(),
                emptyList(),
                null,
                null,
            )
        }
        assertTrue(review(runs = listOf(run("hard", 2, 8.0))).reviewToday())
        assertFalse(review(runs = listOf(run("unknown", 1, null))).reviewToday())
        assertFalse(review(runs = listOf(run("unfinished", 0, 9.0))).reviewToday())
        assertFalse(review(runs = listOf(run("old", 3, 9.0))).reviewToday())
    }

    @Test
    fun healthTrendsAloneCannotEstablishADeloadAndFutureImportsStayOut() {
        val health =
            (0..34).flatMap { day ->
                listOf(HealthIntelligence.Metric.RESTING_HR, HealthIntelligence.Metric.HRV_RMSSD)
                    .map { metric ->
                        val at = Instants.ofEpochSecond(now.getEpochSecond() - day * 86400L - 3600)
                        HealthIntelligence.Observation(
                            "$metric-$day",
                            metric,
                            today.minusDays(day.toLong()),
                            at,
                            Instants.ofEpochSecond(at.getEpochSecond() + 60),
                            if (metric == HealthIntelligence.Metric.RESTING_HR) {
                                if (day < 7) 80.0 else 60.0
                            } else {
                                if (day < 7) 35.0 else 50.0
                            },
                            "same",
                            now,
                        )
                    }
            }
        assertFalse(review(health = health).lighterWeek())
        assertFalse(review(health = health).reviewToday())
        val combined = review(listOf(gym("old", 10)), health = health)
        assertTrue(combined.reviewToday())
        assertFalse(combined.lighterWeek())
        assertTrue(combined.evidence().any { it.kind() == "health" })
        val future =
            health.map {
                HealthIntelligence.Observation(
                    it.id(),
                    it.metric(),
                    it.date(),
                    it.start(),
                    it.end(),
                    it.value(),
                    it.source(),
                    Instants.ofEpochSecond(now.getEpochSecond() + 1),
                )
            }
        assertFalse(review(listOf(gym("old", 10)), health = future).reviewToday())
    }

    @Test
    fun hybridConflictsAreContextAndRespectProtectedActualsAndPriorities() {
        for (priority in GymProgramme.Priority.entries) {
            val p = programme(priority)
            val w =
                p.workouts().first {
                    it.date().toEpochDay() >= today.toEpochDay() &&
                        TrainingRecovery.meaningfulLower(it)
                }
            val run =
                PlannedWorkout(
                    "hard",
                    w.date().plusDays(1),
                    WorkoutCategory.LONG_RUN,
                    15.0,
                    null,
                    emptyList(),
                    "Original",
                )
            val conflicts = TrainingRecovery.conflicts(p, listOf(run), today, emptySet())
            assertTrue(
                conflicts.any {
                    it.workoutId() == w.id() && it.timing() == TrainingRecovery.Timing.BEFORE
                }
            )
            assertFalse(
                TrainingRecovery.conflicts(p, listOf(run), today, setOf(w.id())).any {
                    it.workoutId() == w.id()
                }
            )
            assertEquals(w, p.workouts().first { it.id() == w.id() })
        }
        assertFalse(
            TrainingRecovery.demanding(
                PlannedWorkout(
                    "progression",
                    today,
                    WorkoutCategory.PROGRESSION_RUN,
                    5.0,
                    null,
                    emptyList(),
                    null,
                )
            )
        )
    }

    @Test
    fun lighterWeekIsANewPreviewAndKeepsProtectedHistoryTasksAndReferences() {
        val p = programme()
        val supported = review(listOf(gym("a", 1), gym("b", 4), gym("c", 7)))
        val protected = p.workouts().first { it.date().toEpochDay() >= today.toEpochDay() }.id()
        val proposal =
            RecoveryProgrammeProposal.lighterWeek(p, supported, now, utc, setOf(protected))
        assertSame(p, proposal.previous())
        assertEquals(p.revision() + 1, proposal.proposed().revision())
        for (old in p.workouts()) {
            val next = proposal.proposed().workouts().first { it.id() == old.id() }
            if (
                old.date().toEpochDay() < today.toEpochDay() ||
                    old.date().toEpochDay() > today.plusDays(6).toEpochDay() ||
                    old.id() == protected
            )
                assertEquals(old, next)
            else
                for ((a, b) in old.definition().slots().zip(next.definition().slots())) {
                    assertEquals(a.exercise(), b.exercise())
                    assertEquals(a.supersetGroup(), b.supersetGroup())
                    val x = a.target()
                    val y = b.target()
                    if (x != null) {
                        assertEquals(maxOf(1, x.sets() - 1), y!!.sets())
                        assertEquals(x.referenceLoadKg(), y.referenceLoadKg())
                        assertEquals(x.effort(), y.effort())
                        assertEquals(x.restSeconds(), y.restSeconds())
                        assertEquals(x.minimumReps(), y.minimumReps())
                        assertEquals(x.maximumReps(), y.maximumReps())
                    }
                }
        }
        assertFailsWith<IllegalArgumentException> {
            RecoveryProgrammeProposal.lighterWeek(p, review(), now, utc, emptySet())
        }
    }

    @Test
    fun alternateDatesStayInsideTheAnchoredWeekAndKeepTheOriginalUntilReview() {
        val p = programme()
        val w = p.workouts().first { it.date().toEpochDay() >= today.toEpochDay() }
        val run =
            PlannedWorkout(
                "hard",
                w.date(),
                WorkoutCategory.LONG_RUN,
                15.0,
                null,
                emptyList(),
                null,
            )
        val choices =
            RecoveryProgrammeProposal.alternatives(p, w.id(), listOf(run), today, emptySet())
        val block = p.blocks().first { b -> b.workouts().any { it.id() == w.id() } }
        val week = (w.date().toEpochDay() - block.start().toEpochDay()) / 7
        assertTrue(choices.isNotEmpty())
        for (date in choices) assertEquals(
            week,
            (date.toEpochDay() - block.start().toEpochDay()) / 7,
        )
        val moved =
            RecoveryProgrammeProposal.move(
                p,
                w.id(),
                choices.first(),
                listOf(run),
                today,
                now,
                emptySet(),
            )
        assertSame(p, moved.previous())
        val next = moved.proposed().workouts().first { it.id() == w.id() }
        assertEquals(w.definition(), next.definition())
        assertEquals(choices.first(), next.date())
        assertNotEquals(choices.first(), w.date())
        assertTrue(
            RecoveryProgrammeProposal.alternatives(p, w.id(), listOf(run), today, setOf(w.id()))
                .isEmpty()
        )
    }
}
