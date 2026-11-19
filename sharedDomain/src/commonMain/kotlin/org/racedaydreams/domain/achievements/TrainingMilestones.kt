package org.racedaydreams.domain.achievements

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.strength.StrengthSession
import org.racedaydreams.domain.time.*

/** Original restrained milestones from accepted actuals. No streaks or invented completion. */
object TrainingMilestones {
    enum class Source {
        RUN,
        GYM,
    }

    enum class Kind(private val count: Int, private val title: String) {
        FIRST_GYM(1, "First recorded strength workout"),
        FIVE_GYM(5, "Five recorded strength workouts"),
        FIFTY_GYM(50, "Fifty recorded strength workouts"),
        FIRST_RUN(1, "First recorded run"),
        TEN_RUNS(10, "Ten recorded runs"),
        FIRST_FIVE_KM_RUN(1, "A recorded run of at least 5 km"),
        TWELVE_TRAINING_WEEKS(12, "Training recorded across twelve weeks");

        fun count(): Int = count

        fun title(): String = title

        fun explanation(): String =
            when (this) {
                FIRST_GYM,
                FIVE_GYM,
                FIFTY_GYM ->
                    "Based on completed recorded strength workouts with at least one working set. Warm-up-only sessions and manual PB entries do not count."
                FIRST_RUN,
                TEN_RUNS ->
                    "Based on retained completed running records. These are recorded activities, not a claim of independently verified physical performance."
                FIRST_FIVE_KM_RUN ->
                    "A completed running record has at least 5,000 metres of recorded distance. This does not assert an official race result or universal GPS accuracy."
                TWELVE_TRAINING_WEEKS ->
                    "Training appears in twelve distinct Monday-based weeks. The weeks need not be consecutive; rest, breaks and missed days carry no penalty."
            }
    }

    data class Proof(
        private val source: Source?,
        private val recordId: String?,
        private val at: Instant?,
        private val offset: ZoneOffset?,
        private val distanceMetres: Double?,
    ) {
        init {
            if (source == null || at == null) throw NullPointerException()
            require(
                recordId != null &&
                    recordId.trim { it <= ' ' }.isNotEmpty() &&
                    recordId.length <= 256
            ) {
                "Milestone source identity required."
            }
            require(distanceMetres == null || distanceMetres.isFinite() && distanceMetres >= 0) {
                "Invalid retained distance."
            }
            require(source != Source.GYM || distanceMetres == null) {
                "Gym workouts are not running distance evidence."
            }
        }

        fun source(): Source = source!!

        fun recordId(): String = recordId!!

        fun at(): Instant = at!!

        fun offset(): ZoneOffset? = offset

        fun distanceMetres(): Double? = distanceMetres

        fun week(): LocalDate? {
            val zone = offset ?: return null
            val date = CalendarDates.atOffset(at(), zone)
            return date.minusDays((date.getDayOfWeek().getValue() - 1).toLong())
        }

        override fun toString(): String =
            "Proof[source=$source, recordId=$recordId, at=$at, offset=$offset, distanceMetres=$distanceMetres]"
    }

    data class Award(
        private val kind: Kind?,
        private val earnedAt: Instant?,
        private var evidence: List<Proof>?,
    ) {
        init {
            if (kind == null || earnedAt == null) throw NullPointerException()
            evidence = Immutable.copyList(evidence!!)
            require(evidence!!.size == kind.count()) { "Milestone evidence count differs." }
            val sources = mutableSetOf<String>()
            val weeks = mutableSetOf<LocalDate?>()
            var last: Instant? = null
            for (proof in evidence!!) {
                require(sources.add(proof.source().name + ":" + proof.recordId())) {
                    "Duplicate milestone source."
                }
                require(last == null || !proof.at().isBefore(last)) {
                    "Milestone evidence must be chronological."
                }
                last = proof.at()
                weeks.add(proof.week())
                require(
                    kind !in listOf(Kind.FIRST_GYM, Kind.FIVE_GYM, Kind.FIFTY_GYM) ||
                        proof.source() == Source.GYM
                ) {
                    "Strength milestone requires strength evidence."
                }
                require(
                    kind !in listOf(Kind.FIRST_RUN, Kind.TEN_RUNS, Kind.FIRST_FIVE_KM_RUN) ||
                        proof.source() == Source.RUN
                ) {
                    "Running milestone requires running evidence."
                }
                require(
                    kind != Kind.FIRST_FIVE_KM_RUN ||
                        proof.distanceMetres()?.let { it >= 5000 } == true
                ) {
                    "Distance milestone requires retained distance."
                }
            }
            require(
                earnedAt == last &&
                    (kind != Kind.TWELVE_TRAINING_WEEKS || weeks.size == 12 && null !in weeks)
            ) {
                "Milestone chronology differs."
            }
        }

        fun kind(): Kind = kind!!

        fun earnedAt(): Instant = earnedAt!!

        fun evidence(): List<Proof> = evidence!!

        fun id(): String = kind!!.name

        override fun toString(): String =
            "Award[kind=$kind, earnedAt=$earnedAt, evidence=$evidence]"
    }

    @JvmStatic
    fun eligible(
        strength: List<StrengthSession>,
        runs: List<ActivityRecord>,
        now: Instant,
    ): List<Award> {
        val gym = mutableListOf<Proof>()
        val running = mutableListOf<Proof>()
        val seenGym = mutableMapOf<String, StrengthSession>()
        val seenRuns = mutableMapOf<String, ActivityRecord>()
        for (session in strength) {
            val old = seenGym[session.id()]
            if (old != null) {
                require(old == session) { "Conflicting actuals." }
                continue
            }
            seenGym[session.id()] = session
            if (
                !session.startedAt().isAfter(now) &&
                    session.exercises().any { e -> e.sets().any { !it.warmUp() } }
            )
                gym.add(
                    Proof(Source.GYM, session.id(), session.startedAt(), session.offset(), null)
                )
        }
        for (run in runs) {
            val old = seenRuns[run.id()]
            if (old != null) {
                require(old == run) { "Conflicting running evidence." }
                continue
            }
            seenRuns[run.id()] = run
            if (!run.end().isAfter(now))
                running.add(
                    Proof(
                        Source.RUN,
                        run.id(),
                        run.start(),
                        run.startOffset(),
                        run.distanceMetres(),
                    )
                )
        }
        val order =
            compareBy<Proof> { it.at() }.thenBy { it.source().name }.thenBy { it.recordId() }
        gym.sortWith(order)
        running.sortWith(order)
        val result = mutableListOf<Award>()
        for (kind in Kind.entries) {
            val source: List<Proof> =
                when (kind) {
                    Kind.TWELVE_TRAINING_WEEKS -> {
                        val byWeek = linkedMapOf<LocalDate, Proof>()
                        for (proof in (gym + running).sortedWith(order)) {
                            val week = proof.week()
                            if (week != null && week !in byWeek) byWeek[week] = proof
                        }
                        byWeek.values.toList()
                    }
                    Kind.FIRST_FIVE_KM_RUN ->
                        running.filter { it.distanceMetres()?.let { v -> v >= 5000 } == true }
                    else -> if (kind.name.contains("GYM")) gym else running
                }
            if (source.size >= kind.count()) {
                val proof = Immutable.copyList(source.subList(0, kind.count()))
                result.add(Award(kind, proof.last().at(), proof))
            }
        }
        return Immutable.copyList(result)
    }
}
