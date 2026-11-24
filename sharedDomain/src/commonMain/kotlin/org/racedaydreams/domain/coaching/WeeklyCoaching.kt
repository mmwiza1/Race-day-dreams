package org.racedaydreams.domain.coaching

import kotlin.jvm.JvmStatic
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import org.racedaydreams.domain.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

/** Original raw-evidence review. No score/body-mass input and no silent programme change. */
object WeeklyCoaching {
    enum class State {
        IMPROVING,
        STABLE,
        NEEDS_ATTENTION,
        INSUFFICIENT_EVIDENCE,
    }

    data class Channel(
        private val name: String?,
        private val state: State?,
        private val reason: String?,
    ) {
        fun name(): String? = name

        fun state(): State? = state

        fun reason(): String? = reason

        override fun toString(): String = "Channel[name=$name, state=$state, reason=$reason]"
    }

    data class Choice(
        private val id: String?,
        private val label: String?,
        private var replacements: List<PlannedWorkout>?,
    ) {
        init {
            replacements = Immutable.copyList(replacements ?: throw NullPointerException())
        }

        fun id(): String? = id

        fun label(): String? = label

        fun replacements(): List<PlannedWorkout> = replacements!!

        override fun toString(): String = "Choice[id=$id, label=$label, replacements=$replacements]"
    }

    data class Review(
        private val id: String?,
        private val baseVersionId: String?,
        private val week: LocalDate?,
        private val planned: Int,
        private val completed: Int,
        private val kilometres: Double,
        private val strengthSessions: Int,
        private val longestKm: Double?,
        private var observations: List<String>?,
        private var channels: List<Channel>?,
        private val recommendation: String?,
        private var choices: List<Choice>?,
    ) {
        init {
            observations = Immutable.copyList(observations ?: throw NullPointerException())
            channels = Immutable.copyList(channels ?: throw NullPointerException())
            choices = Immutable.copyList(choices ?: throw NullPointerException())
        }

        fun id(): String? = id

        fun baseVersionId(): String? = baseVersionId

        fun week(): LocalDate? = week

        fun planned(): Int = planned

        fun completed(): Int = completed

        fun kilometres(): Double = kilometres

        fun strengthSessions(): Int = strengthSessions

        fun longestKm(): Double? = longestKm

        fun observations(): List<String> = observations!!

        fun channels(): List<Channel> = channels!!

        fun recommendation(): String? = recommendation

        fun choices(): List<Choice> = choices!!

        override fun toString(): String =
            "Review[id=$id, baseVersionId=$baseVersionId, week=$week, planned=$planned, completed=$completed, kilometres=$kilometres, strengthSessions=$strengthSessions, longestKm=$longestKm, observations=$observations, channels=$channels, recommendation=$recommendation, choices=$choices]"
    }

    data class Interruption(
        private val from: LocalDate?,
        private val returned: LocalDate?,
        private val reason: String?,
    ) {
        init {
            require(
                from != null &&
                    returned != null &&
                    !coachingBefore(returned, from) &&
                    reason != null
            ) {
                "Check interruption dates and reason."
            }
        }

        fun from(): LocalDate = from!!

        fun returned(): LocalDate = returned!!

        fun reason(): String = reason!!

        override fun toString(): String =
            "Interruption[from=$from, returned=$returned, reason=$reason]"
    }

    @JvmStatic
    fun review(
        version: PlanVersion,
        history: List<PlanVersion>,
        runs: List<ActivityRecord>,
        links: List<RunExecution>,
        strength: List<StrengthSession>,
        results: List<RecordedResult>,
        today: LocalDate,
        interruption: Interruption?,
    ): Review =
        review(
            version,
            history,
            runs,
            links,
            strength,
            results,
            today,
            interruption,
            Immutable.list(),
        )

    @JvmStatic
    fun review(
        version: PlanVersion,
        history: List<PlanVersion>,
        runs: List<ActivityRecord>,
        links: List<RunExecution>,
        strength: List<StrengthSession>,
        results: List<RecordedResult>,
        today: LocalDate,
        interruption: Interruption?,
        decisions: List<WorkoutDecision>,
    ): Review =
        review(
            version,
            history,
            runs,
            links,
            strength,
            results,
            today,
            interruption,
            decisions,
            BaselineConfidence.STRONG,
            Immutable.list(),
        )

    @JvmStatic
    fun review(
        version: PlanVersion,
        history: List<PlanVersion>,
        runs: List<ActivityRecord>,
        links: List<RunExecution>,
        strength: List<StrengthSession>,
        results: List<RecordedResult>,
        today: LocalDate,
        interruption: Interruption?,
        decisions: List<WorkoutDecision>,
        confidence: BaselineConfidence,
        assessments: List<AssessmentResult>,
    ): Review {
        val end = coachingMonday(today)
        val from = end.minusDays(7)
        val window = end.minusDays(28)
        val original = HashMap<String, PlannedWorkout>()
        for (v in history.sortedBy { it.createdAt() }) for (w in v.plan().workouts()) if (
            w.id() !in original
        )
            original[w.id()] = w
        for (w in version.plan().workouts()) if (w.id() !in original) original[w.id()] = w
        val linked = HashMap<String, String>()
        for (l in links) l.plannedWorkoutId()?.let { linked[l.activityId()] = it }
        val actual = HashMap<String, ActivityRecord>()
        // Local midnight, so a run finished late today west of UTC still counts (audit B-R5).
        val tomorrow =
            org.racedaydreams.domain.activity.ActivityDates.startOfDeviceDay(today.plusDays(1))
        for (a in runs) {
            val key = if (linked.containsKey(a.id())) linked[a.id()] else a.plannedWorkoutId()
            if (key != null && !a.end().isAfter(tomorrow)) actual[key] = a
        }
        var planned = 0
        var completed = 0
        var recentCount = 0
        var priorCount = 0
        var shortened = 0
        var hard = 0
        var km = 0.0
        var longest = 0.0
        var recentEffort = 0.0
        var priorEffort = 0.0
        val observations = mutableListOf<String>()
        val channels = mutableListOf<Channel>()
        var faded = 0
        var durable = 0
        val countedRuns = mutableSetOf<String>()
        // One physical run reported by two sources (a native recording and a watch import, say)
        // counts once towards volume, as on Progress (audit B-02).
        for (a in org.racedaydreams.domain.activity.PhysicalRunSessions.counted(runs)) {
            val date = coachingDate(a)
            val metres = a.distanceMetres()
            if (
                !coachingBefore(date, from) &&
                    coachingBefore(date, end) &&
                    countedRuns.add(a.id()) &&
                    metres != null
            ) {
                km += metres / 1000
                longest = max(longest, metres / 1000)
            }
        }
        val difficultTypes = mutableMapOf<WorkoutCategory, Int>()
        var longShortened = 0
        var longDurationMisses = 0
        var strengthPrecededMisses = 0
        val recordedHeavy = StrengthAnalytics.hardLowerBodyDates(strength)
        val difficult = mutableMapOf<DayOfWeek, MutableSet<LocalDate>>()
        val moved = mutableMapOf<DayOfWeek, Int>()
        val reviewedWorkouts = version.plan().workouts().toMutableList()
        for (d in decisions) if (
            d.kind() == WorkoutDecision.Kind.SKIPPED &&
                d.workoutId() in original &&
                reviewedWorkouts.none { it.id() == d.workoutId() }
        )
            reviewedWorkouts.add(original.getValue(d.workoutId()))
        for (w in reviewedWorkouts) {
            if (w.category() == WorkoutCategory.RACE) continue
            val a = actual[w.id()]
            if (!coachingBefore(w.date(), from) && coachingBefore(w.date(), end)) {
                planned++
                if (a != null && coachingBefore(coachingDate(a), end)) completed++
            }
            if (coachingBefore(w.date(), window) || !coachingBefore(w.date(), end)) continue
            val first = original[w.id()] ?: w
            val wasMoved = first.date() != w.date() || a != null && coachingDate(a) != first.date()
            if (a == null) {
                difficultTypes[w.category()] = (difficultTypes[w.category()] ?: 0) + 1
                if (w.duration()?.let { it.getSeconds() / 60 >= 60 } == true) longDurationMisses++
                if (first.date().minusDays(1) in recordedHeavy) strengthPrecededMisses++
            }
            if (a == null || wasMoved) {
                val day = first.date().getDayOfWeek()
                difficult.getOrPut(day) { mutableSetOf() }.add(coachingMonday(first.date()))
                if (wasMoved && a != null) moved[day] = (moved[day] ?: 0) + 1
            }
            if (a == null || !coachingBefore(coachingDate(a), end)) continue
            val rpe = a.rpe()
            if (rpe != null) {
                if (coachingBefore(w.date(), end.minusDays(14))) {
                    priorEffort += rpe
                    priorCount++
                } else {
                    recentEffort += rpe
                    recentCount++
                }
                if (rpe > if (AdaptiveCoaching.isQuality(w.category())) 8 else 5) hard++
            }
            val distance = w.distanceKm()
            val metres = a.distanceMetres()
            if (distance != null && metres != null && metres < distance * 800) {
                shortened++
                if (w.category() == WorkoutCategory.LONG_RUN) longShortened++
            }
            if (w.category() == WorkoutCategory.LONG_RUN) {
                val d = LongRunDurability.analyse(a)
                if (d.sufficient()) {
                    durable++
                    if (requireNotNull(d.paceFadePercent()) > 8) faded++
                }
            }
        }
        for ((day, weeks) in difficult.entries.sortedBy { it.key.getValue() }) if (weeks.size >= 3)
            observations.add(
                "$day was missed or moved in ${weeks.size} of the last four weeks (${moved[day] ?: 0} moved and completed). Consider editing your regular days; no catch-up mileage is suggested."
            )
        for ((type, count) in difficultTypes.entries.sortedBy { it.key.ordinal }) if (count >= 3)
            observations.add(
                "$count ${type.name.lowercase().replace('_', ' ')} sessions were not recorded recently. Consider whether this session type fits your week."
            )
        if (longDurationMisses >= 3)
            observations.add(
                "Several prescribed sessions of at least an hour were not recorded. Shorter sessions may fit your routine better."
            )
        if (strengthPrecededMisses >= 3)
            observations.add(
                "At least three unrecorded runs followed actual hard lower-body work. Consider separating those sessions."
            )
        if (longShortened >= 3)
            observations.add(
                "At least three long runs were shortened. Repeating the current endurance step may fit better than extending it."
            )
        val rising =
            recentCount >= 3 &&
                priorCount >= 2 &&
                recentEffort / recentCount - priorEffort / priorCount >= 1.5
        val strain = rising && shortened >= 2 || hard >= 3 || faded >= 2
        val consistent = planned >= 2 && completed == planned
        val explicitSkips =
            decisions
                .count {
                    it.kind() == WorkoutDecision.Kind.SKIPPED &&
                        !coachingBefore(it.originalDate(), window) &&
                        coachingBefore(it.originalDate(), end)
                }
                .toLong()
        if (explicitSkips > 0)
            observations.add(
                "$explicitSkips sessions were explicitly skipped; this is distinct from an unrecorded workout."
            )
        observations.add("$completed / $planned planned runs completed in the reviewed week.")
        if (shortened >= 2)
            observations.add("$shortened recent sessions were below 80% of their planned distance.")
        if (rising)
            observations.add(
                "Reported effort increased across several sessions. Workout mix and conditions may contribute."
            )
        if (faded >= 2)
            observations.add(
                "At least two long runs slowed by over 8% in the second half. Review terrain, conditions and effort before progressing duration."
            )
        channels.add(
            Channel(
                "Volume tolerance",
                if (strain) State.NEEDS_ATTENTION
                else if (completed >= 2) State.STABLE else State.INSUFFICIENT_EVIDENCE,
                "Completion and reported response considered together; distance alone does not establish tolerance.",
            )
        )
        channels.add(
            Channel(
                "Intensity tolerance",
                if (hard >= 3) State.NEEDS_ATTENTION
                else if (recentCount >= 3) State.STABLE else State.INSUFFICIENT_EVIDENCE,
                "Uses session-specific RPE context, not Run Score.",
            )
        )
        channels.add(
            Channel(
                "Endurance durability",
                if (faded >= 2) State.NEEDS_ATTENTION
                else if (durable >= 2) State.STABLE else State.INSUFFICIENT_EVIDENCE,
                "$durable recent long runs had qualified continuous samples.",
            )
        )
        channels.add(
            Channel(
                "Consistency",
                if (consistent) State.STABLE
                else if (planned >= 2) State.NEEDS_ATTENTION else State.INSUFFICIENT_EVIDENCE,
                "Moved and later completed sessions count as completed.",
            )
        )
        val gyms =
            strength.count {
                !coachingBefore(CalendarDates.atOffset(it.startedAt(), it.offset()), from) &&
                    coachingBefore(CalendarDates.atOffset(it.startedAt(), it.offset()), end)
            }
        val lower = StrengthAnalytics.hardLowerBodyDates(strength)
        val interference =
            version.plan().workouts().any {
                !coachingBefore(it.date(), end) &&
                    coachingBefore(it.date(), end.plusWeeks(1)) &&
                    AdaptiveCoaching.isDemanding(it.category()) &&
                    it.date().minusDays(1) in lower
            }
        channels.add(
            Channel(
                "Strength interference",
                if (interference) State.NEEDS_ATTENTION
                else if (strength.isEmpty()) State.INSUFFICIENT_EVIDENCE else State.STABLE,
                "Only logged exercises/sets and effort count as completed strength work.",
            )
        )
        channels.add(
            Channel(
                "Subjective response",
                if (rising) State.NEEDS_ATTENTION
                else if (recentCount >= 3) State.STABLE else State.INSUFFICIENT_EVIDENCE,
                "RPE is optional; missing entries remain unknown.",
            )
        )
        val performance =
            PerformanceReassessment.review(
                results,
                assessments,
                end,
                confidence,
                if (planned == 0) 0.0 else completed / planned.toDouble(),
                recentCount,
                rising || hard > 0 || faded >= 2,
            )
        val major = performance.propose()
        channels.add(
            Channel(
                "Performance trend",
                if (major) State.IMPROVING else State.INSUFFICIENT_EVIDENCE,
                "Confidence: ${requireNotNull(performance.confidence()).name.lowercase()}. ${requireNotNull(performance.reasons()).joinToString(" ")}",
            )
        )
        val choices = mutableListOf(Choice("keep", "Keep original plan", Immutable.list()))
        var recommendation = "No adjustment required. Continue the planned progression."
        if (strain) {
            recommendation =
                "Hold the next two weeks rather than increasing workload; reducing remains an option."
            // Without recorded distance there is no ceiling: hold keeps the planned volume and
            // only reduce cuts it (audit B-R4).
            choices.add(
                Choice("hold", "Hold volume for two weeks", scaled(version, end, 2, km, 1.0))
            )
            choices.add(
                Choice("reduce", "Reduce the next two weeks", scaled(version, end, 2, km * .8, .8))
            )
        }
        val last =
            runs
                .map(::coachingDate)
                .filter { !coachingAfter(it, today) }
                .maxByOrNull { it.toEpochDay() }
        var gap = if (last == null) 0L else today.toEpochDay() - last.toEpochDay()
        if (
            interruption != null &&
                coachingBefore(interruption.returned(), today.plusDays(1)) &&
                !coachingBefore(interruption.returned(), today.minusDays(21))
        )
            gap = max(gap, interruption.returned().toEpochDay() - interruption.from().toEpochDay())
        if (gap >= 14) {
            recommendation =
                "Consider a three-week return bridge: reduced easy running, rebuild frequency, then reassess quality. Previous fitness still matters; this is not medical clearance."
            choices.add(
                Choice("return", "Use a three-week return bridge", bridge(version, today, gap))
            )
        }
        if (major) {
            observations.add(
                "Recent performance may support reviewing quality paces. Easy/recovery effort and your race target remain unchanged."
            )
            val pace =
                qualityPaces(
                    version,
                    end,
                    performance.fasterFraction(),
                    performance.supportingMetres(),
                )
            if (pace.isNotEmpty())
                choices.add(Choice("paces", "Review proposed quality paces", pace))
        }
        val signature = version.id() + from + observations + channels + choices
        return Review(
            StrengthProgrammeEditor.nameUuid(signature),
            version.id(),
            from,
            planned,
            completed,
            km,
            gyms,
            if (longest == 0.0) null else longest,
            observations,
            channels,
            recommendation,
            choices,
        )
    }

    private fun scaled(
        v: PlanVersion,
        start: LocalDate,
        weeks: Int,
        ceiling: Double,
        noEvidenceFactor: Double,
    ): List<PlannedWorkout> {
        val out = mutableListOf<PlannedWorkout>()
        for (i in 0 until weeks) {
            val from = start.plusWeeks(i.toLong())
            val to = from.plusWeeks(1)
            val total =
                coachingSum(
                    v.plan()
                        .workouts()
                        .filter {
                            !coachingBefore(it.date(), from) &&
                                coachingBefore(it.date(), to) &&
                                it.category() != WorkoutCategory.RACE
                        }
                        .mapNotNull { it.distanceKm() }
                )
            val factor =
                if (total > 0 && ceiling > 0) min(1.0, ceiling / total) else noEvidenceFactor
            for (w in v.plan().workouts()) if (
                !coachingBefore(w.date(), from) &&
                    coachingBefore(w.date(), to) &&
                    w.category() != WorkoutCategory.RACE
            )
                out.add(resize(w, factor, false))
        }
        return out
    }

    private fun bridge(v: PlanVersion, start: LocalDate, gap: Long): List<PlannedWorkout> {
        val out = mutableListOf<PlannedWorkout>()
        for (w in v.plan().workouts()) if (
            !coachingBefore(w.date(), start) &&
                coachingBefore(w.date(), start.plusWeeks(3)) &&
                w.category() != WorkoutCategory.RACE
        ) {
            val week = (w.date().toEpochDay() - start.toEpochDay()) / 7
            val factor =
                if (week == 0L) {
                    if (gap >= 28) .5 else .65
                } else if (week == 1L) .75 else .85
            out.add(resize(w, factor, true))
        }
        return out
    }

    private fun resize(w: PlannedWorkout, factor: Double, easy: Boolean): PlannedWorkout {
        val km = w.distanceKm()?.let { max(.1, floor(it * factor * 10) / 10.0) }
        val d =
            w.duration()?.let { Durations.ofSeconds(max(60L, (it.getSeconds() * factor).toLong())) }
        val segments = mutableListOf<WorkoutSegment>()
        if (easy)
            segments.add(
                WorkoutSegment(
                    WorkoutSegment.Kind.RUN,
                    km,
                    d,
                    null,
                    null,
                    null,
                    3.0,
                    "Conversational effort; no catch-up mileage.",
                )
            )
        else
            for (s in w.segments()) segments.add(
                WorkoutSegment(
                    s.kind(),
                    s.distanceKm()?.times(factor),
                    s.duration()?.let {
                        Durations.ofSeconds(max(1L, (it.getSeconds() * factor).toLong()))
                    },
                    s.minimumSecondsPerKm(),
                    s.maximumSecondsPerKm(),
                    s.heartRateZone(),
                    s.rpe(),
                    s.guidance(),
                )
            )
        return PlannedWorkout(
            w.id(),
            w.date(),
            if (easy) WorkoutCategory.EASY_RUN else w.category(),
            km,
            d,
            segments,
            "Runner-selected reduced workload. Original prescription is retained in plan history.",
        )
    }

    private fun qualityPaces(
        v: PlanVersion,
        start: LocalDate,
        fraction: Double,
        evidenceMetres: Double,
    ): List<PlannedWorkout> {
        val out = mutableListOf<PlannedWorkout>()
        for (w in v.plan().workouts()) if (
            !coachingBefore(w.date(), start) &&
                coachingBefore(w.date(), start.plusWeeks(4)) &&
                AdaptiveCoaching.isQuality(w.category()) &&
                (w.category() != WorkoutCategory.RACE_PACE_RUN ||
                    evidenceMetres >=
                        min(10000.0, v.goalSnapshot().raceGoal().distance().kilometres() * 500))
        ) {
            val segments = mutableListOf<WorkoutSegment>()
            var changed = false
            for (s in w.segments()) {
                val fast = s.minimumSecondsPerKm()
                val slow = s.maximumSecondsPerKm()
                val rpe = s.rpe()
                val update =
                    s.kind() == WorkoutSegment.Kind.RUN &&
                        (rpe == null || rpe > 4) &&
                        fast != null &&
                        slow != null
                changed = changed || update
                segments.add(
                    if (update)
                        WorkoutSegment(
                            s.kind(),
                            s.distanceKm(),
                            s.duration(),
                            fast!! * (1 - fraction),
                            slow!! * (1 - fraction),
                            s.heartRateZone(),
                            rpe,
                            s.guidance(),
                        )
                    else s
                )
            }
            if (changed)
                out.add(
                    PlannedWorkout(
                        w.id(),
                        w.date(),
                        w.category(),
                        w.distanceKm(),
                        w.duration(),
                        segments,
                        "Runner-approved modest quality pace review; easy effort and race target unchanged.",
                    )
                )
        }
        return out
    }

    @JvmStatic
    fun apply(
        v: PlanVersion,
        review: Review,
        choice: Choice,
        completed: Set<String>,
    ): TrainingPlan {
        require(v.id() == review.baseVersionId() && choice in review.choices()) {
            "Review no longer matches the plan."
        }
        val changes = HashMap<String, PlannedWorkout>()
        for (w in choice.replacements()) {
            require(w.id() !in completed) { "Completed sessions cannot change." }
            changes[w.id()] = w
        }
        val out = v.plan().workouts().map { changes[it.id()] ?: it }
        return TrainingPlan(
            v.planId(),
            v.plan().startDate(),
            v.plan().endDate(),
            v.plan().phases(),
            out,
        )
    }
}
