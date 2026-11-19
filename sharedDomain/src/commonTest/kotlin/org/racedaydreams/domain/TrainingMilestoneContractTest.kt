package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.achievements.TrainingMilestones
import org.racedaydreams.domain.achievements.TrainingMilestones.Award
import org.racedaydreams.domain.achievements.TrainingMilestones.Kind
import org.racedaydreams.domain.achievements.TrainingMilestones.Proof
import org.racedaydreams.domain.achievements.TrainingMilestones.Source
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class TrainingMilestoneContractTest {
    private val now = Instants.ofEpochSecond(2000000000)
    private val utc = Offsets.ofTotalSeconds(0)

    private fun run(
        id: String,
        days: Long = 1,
        distance: Double? = null,
        offset: ZoneOffset? = utc,
    ): ActivityRecord {
        val start = Instants.ofEpochSecond(now.getEpochSecond() - days * 86400)
        return ActivityRecord(
            id,
            SourceIdentity("local", id, null),
            start,
            Instants.ofEpochSecond(start.getEpochSecond() + 1800),
            offset,
            offset,
            distance,
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
    }

    private fun gym(id: String, warm: Boolean = false, days: Long = 1): StrengthSession =
        StrengthSession(
            id,
            Instants.ofEpochSecond(now.getEpochSecond() - days * 86400),
            utc,
            Durations.ofMinutes(30),
            null,
            "",
            listOf(
                StrengthSession.ExerciseSets(
                    requireNotNull(ExerciseCatalog.definition("push-up")).exercise(),
                    listOf(StrengthSession.SetEntry("set", 6, 0.0, null, warm)),
                )
            ),
        )

    @Test
    fun noTrainingAndWarmUpOnlyTrainingCannotBecomeAnAchievement() {
        assertTrue(TrainingMilestones.eligible(emptyList(), emptyList(), now).isEmpty())
        assertTrue(
            TrainingMilestones.eligible(listOf(gym("warm", true)), emptyList(), now).isEmpty()
        )
    }

    @Test
    fun futureAndUnfinishedEvidenceCannotEarnAwards() {
        assertTrue(
            TrainingMilestones.eligible(
                    listOf(gym("future", days = -1)),
                    listOf(run("ongoing", 0, 6000.0)),
                    now,
                )
                .isEmpty()
        )
    }

    @Test
    fun originalThresholdsSortChronologicallyAndRepeatedEvaluationIsIdempotent() {
        val strength = (1..50).map { gym("g$it", days = it.toLong() * 2) }
        val runs = (1..20).map { run("r$it", it.toLong() * 7, if (it == 5) 5000.0 else null) }
        val awards = TrainingMilestones.eligible(strength, runs, now)
        assertEquals(7, awards.size)
        assertEquals(
            awards,
            TrainingMilestones.eligible(
                strength.reversed() + strength.first(),
                runs.reversed() + runs.first(),
                now,
            ),
        )
        for (award in awards) {
            assertEquals(award.kind().count(), award.evidence().size)
            assertEquals(award.evidence().last().at(), award.earnedAt())
            assertEquals(award.kind().name, award.id())
        }
    }

    @Test
    fun duplicatedIdentityWithConflictingActualsCannotSilentlyEarnAnAward() {
        assertFailsWith<IllegalArgumentException> {
            TrainingMilestones.eligible(listOf(gym("g"), gym("g", true)), emptyList(), now)
        }
        assertFailsWith<IllegalArgumentException> {
            TrainingMilestones.eligible(
                emptyList(),
                listOf(run("r", distance = 5000.0), run("r", distance = 5001.0)),
                now,
            )
        }
    }

    @Test
    fun absentOffsetsDoNotInventTwelveCalendarWeeks() {
        val runs = (1..20).map { run("r$it", it.toLong() * 7, offset = null) }
        val awards = TrainingMilestones.eligible(emptyList(), runs, now)
        assertTrue(awards.any { it.kind() == Kind.TEN_RUNS })
        assertFalse(awards.any { it.kind() == Kind.TWELVE_TRAINING_WEEKS })
    }

    @Test
    fun recordedOffsetDeterminesMondayWeekRatherThanCurrentDeviceZone() {
        val at =
            Instants.ofEpochSecond(
                4 * 86400 - 1800
            ) // Sunday 23:30 UTC at the epoch's first weekend
        val western = Proof(Source.RUN, "r", at, utc, null)
        val eastern = Proof(Source.RUN, "r", at, Offsets.ofTotalSeconds(3600), null)
        assertEquals(CalendarDates.of(1969, 12, 29), western.week())
        assertEquals(CalendarDates.of(1970, 1, 5), eastern.week())
        assertNull(Proof(Source.RUN, "r", at, null, null).week())
    }

    @Test
    fun distanceAwardRequiresActualRecordedDistanceAndCorrectSource() {
        assertFalse(
            TrainingMilestones.eligible(emptyList(), listOf(run("r", distance = 4999.9)), now).any {
                it.kind() == Kind.FIRST_FIVE_KM_RUN
            }
        )
        assertTrue(
            TrainingMilestones.eligible(emptyList(), listOf(run("r", distance = 5000.0)), now).any {
                it.kind() == Kind.FIRST_FIVE_KM_RUN
            }
        )
        assertFailsWith<IllegalArgumentException> { Proof(Source.GYM, "g", now, utc, 5000.0) }
        assertFailsWith<IllegalArgumentException> {
            Award(Kind.FIRST_FIVE_KM_RUN, now, listOf(Proof(Source.RUN, "r", now, utc, null)))
        }
    }

    @Test
    fun awardCannotClaimAnUnsupportedDateOrIncorrectEvidenceCount() {
        val proof = Proof(Source.RUN, "r", now, utc, null)
        assertFailsWith<IllegalArgumentException> { Award(Kind.TEN_RUNS, now, listOf(proof)) }
        assertFailsWith<IllegalArgumentException> {
            Award(Kind.FIRST_RUN, Instants.ofEpochSecond(now.getEpochSecond() + 1), listOf(proof))
        }
        val evidence = mutableListOf(proof)
        val award = Award(Kind.FIRST_RUN, now, evidence)
        evidence.clear()
        assertEquals(listOf(proof), award.evidence())
    }
}
