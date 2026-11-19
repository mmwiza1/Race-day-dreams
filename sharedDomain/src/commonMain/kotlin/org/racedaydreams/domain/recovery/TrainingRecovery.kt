package org.racedaydreams.domain.recovery

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.*
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.health.HealthIntelligence
import org.racedaydreams.domain.health.RecoveryCoaching
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

/** Original conservative coaching rules. No readiness score, diagnosis or automatic plan edit. */
object TrainingRecovery {
    data class Evidence(
        private val kind: String?,
        private val id: String?,
        private val at: Instant?,
        private val explanation: String?,
    ) {
        fun kind(): String? = kind

        fun id(): String? = id

        fun at(): Instant? = at

        fun explanation(): String? = explanation

        override fun toString(): String =
            "Evidence[kind=$kind, id=$id, at=$at, explanation=$explanation]"
    }

    data class Review(
        private val date: LocalDate?,
        private val heading: String?,
        private val instruction: String?,
        private val lighterWeek: Boolean,
        private val reviewToday: Boolean,
        private var evidence: List<Evidence>,
        private var limitations: List<String>,
    ) {
        init {
            evidence = Immutable.copyList(evidence)
            limitations = Immutable.copyList(limitations)
        }

        fun date(): LocalDate? = date

        fun heading(): String? = heading

        fun instruction(): String? = instruction

        fun lighterWeek(): Boolean = lighterWeek

        fun reviewToday(): Boolean = reviewToday

        fun evidence(): List<Evidence> = evidence

        fun limitations(): List<String> = limitations

        override fun toString(): String =
            "Review[date=$date, heading=$heading, instruction=$instruction, lighterWeek=$lighterWeek, reviewToday=$reviewToday, evidence=$evidence, limitations=$limitations]"
    }

    @JvmStatic
    fun review(
        strength: List<StrengthSession>,
        runs: List<ActivityRecord>,
        health: List<HealthIntelligence.Observation>,
        now: Instant,
        offset: ZoneOffset,
    ): Review {
        val today = CalendarDates.atOffset(now, offset)
        val evidence = mutableListOf<Evidence>()
        val highDates = mutableSetOf<LocalDate>()
        var knownEffort = 0
        var unknownEffort = 0
        var recentDemand = false
        val seen = mutableMapOf<String, StrengthSession>()
        for (session in strength) {
            val duplicate = seen[session.id()]
            if (duplicate != null) {
                require(duplicate == session) { "Conflicting strength evidence identity." }
                continue
            }
            seen[session.id()] = session
            if (
                completion(session).isAfter(now) ||
                    session.startedAt().isBefore(before(now, 14 * 86400L))
            )
                continue
            var working = 0
            var effort = 0
            var demanding = 0
            for (block in session.exercises()) for (set in block.sets()) if (!set.warmUp()) {
                working++
                if (set.rpe() != null || set.repetitionsInReserve() != null) effort++
                if (
                    set.rpe()?.let { it >= 9 } == true ||
                        set.repetitionsInReserve()?.let { it <= 1 } == true
                )
                    demanding++
            }
            if (working == 0) continue
            if (session.rpe() != null || effort > 0) knownEffort++ else unknownEffort++
            val high =
                session.rpe()?.let { it >= 8 } == true ||
                    effort >= 2 && demanding >= 2 && demanding * 2 >= effort
            if (high) {
                highDates.add(CalendarDates.atOffset(session.startedAt(), session.offset()))
                val detail =
                    if (session.rpe()?.let { it >= 8 } == true)
                        "Strength workout recorded with perceived effort ${session.rpe()} out of 10."
                    else
                        "Several working sets recorded very high perceived effort or no more than one repetition left before failure. These are separate recorded effort scales, not a measured fatigue score."
                evidence.add(Evidence("strength", session.id(), session.startedAt(), detail))
                if (!session.startedAt().isBefore(before(now, 48 * 3600L))) recentDemand = true
            }
        }
        val seenRuns = mutableMapOf<String, ActivityRecord>()
        for (run in runs) {
            val duplicate = seenRuns[run.id()]
            if (duplicate != null) {
                require(duplicate == run) { "Conflicting running evidence identity." }
                continue
            }
            seenRuns[run.id()] = run
            if (
                !run.end().isAfter(now) &&
                    !run.start().isBefore(before(now, 48 * 3600L)) &&
                    run.rpe()?.let { it >= 8 } == true
            ) {
                recentDemand = true
                evidence.add(
                    Evidence(
                        "run",
                        run.id(),
                        run.start(),
                        "Run recorded with perceived effort ${run.rpe()} out of 10 in the last two days.",
                    )
                )
            }
        }
        val validHealth =
            health.filter {
                !it.start().isAfter(now) && !it.end().isAfter(now) && !it.importedAt().isAfter(now)
            }
        var corroborating = 0
        for (trend in HealthIntelligence.evaluate(validHealth, today)) {
            if (
                trend.recentDays() < 4 ||
                    trend.referenceDays() < 14 ||
                    trend.recent() == null ||
                    trend.baseline() == null
            )
                continue
            if (!RecoveryCoaching.adversePersonalTrend(trend)) continue
            val latest =
                validHealth
                    .filter { it.metric() == trend.metric() && it.source() == trend.source() }
                    .maxByOrNull { it.end() }
            if (latest == null || latest.end().isBefore(before(now, 3 * 86400L))) continue
            corroborating++
            evidence.add(
                Evidence(
                    "health",
                    latest.id(),
                    latest.end(),
                    "${trend.metric()!!.name.replace('_',' ')}: ${trend.interpretation()} from comparable source ${trend.source()} (${trend.recentDays()} recent and ${trend.referenceDays()} reference days). Supporting context only, not a diagnosis.",
                )
            )
        }
        val lighter =
            highDates.size >= 3 &&
                highDates.maxOf { it.toEpochDay() } - highDates.minOf { it.toEpochDay() } >= 6
        val todayReview = recentDemand || corroborating >= 2 && highDates.isNotEmpty()
        val limitations =
            mutableListOf(
                "Recorded effort is subjective. Unlogged training and missing measurements remain unknown; this does not diagnose recovery, illness or injury.",
                "$knownEffort recent strength workouts have some effort evidence; $unknownEffort do not. Warm-up-only sessions are excluded.",
            )
        if (corroborating < 2)
            limitations.add(
                "There are not two fresh comparable health trends supporting a recovery review. Health Connect is optional."
            )
        evidence.sortWith(
            compareByDescending<Evidence> { it.at()!! }.thenBy { it.kind()!! }.thenBy { it.id()!! }
        )
        return Review(
            today,
            if (lighter) "An optional lighter week is available for review"
            else if (todayReview) "Consider a lighter approach today"
            else "No programme change is suggested from these records",
            if (lighter)
                "Your recent logs include several very demanding strength workouts across at least a week. You can keep your programme or review one week with fewer working sets."
            else if (todayReview)
                "You recently recorded demanding training. You may use fewer optional working sets or keep more effort in reserve today. Log what you actually perform; your programme has not changed."
            else
                "Keep your current programme if it feels appropriate. There is not enough supported evidence here to recommend a lighter week; you can still choose an easier session yourself.",
            lighter,
            todayReview,
            evidence,
            limitations,
        )
    }

    enum class Timing {
        SAME_DAY,
        BEFORE,
        AFTER,
        RACE_WEEK,
    }

    data class Conflict(
        private val workoutId: String?,
        private val gymDate: LocalDate?,
        private val runId: String?,
        private val runDate: LocalDate?,
        private val category: WorkoutCategory?,
        private val timing: Timing?,
        private val explanation: String?,
    ) {
        fun workoutId(): String? = workoutId

        fun gymDate(): LocalDate? = gymDate

        fun runId(): String? = runId

        fun runDate(): LocalDate? = runDate

        fun category(): WorkoutCategory? = category

        fun timing(): Timing? = timing

        fun explanation(): String? = explanation

        override fun toString(): String =
            "Conflict[workoutId=$workoutId, gymDate=$gymDate, runId=$runId, runDate=$runDate, category=$category, timing=$timing, explanation=$explanation]"
    }

    @JvmStatic
    fun conflicts(
        programme: StrengthProgramme,
        running: List<PlannedWorkout>,
        today: LocalDate,
        protectedWorkouts: Set<String>,
    ): List<Conflict> {
        val result = mutableListOf<Conflict>()
        for (gym in programme.workouts()) {
            if (
                gym.date().toEpochDay() < today.toEpochDay() ||
                    gym.date().toEpochDay() > today.plusDays(20).toEpochDay() ||
                    gym.id() in protectedWorkouts ||
                    !meaningfulLower(gym)
            )
                continue
            for (run in running) {
                if (!demanding(run) || run.date().toEpochDay() < today.toEpochDay()) continue
                val separation = run.date().toEpochDay() - gym.date().toEpochDay()
                val timing =
                    when {
                        separation == 0L -> Timing.SAME_DAY
                        separation == 1L -> Timing.BEFORE
                        separation == -1L -> Timing.AFTER
                        run.category() == WorkoutCategory.RACE && separation in 2L..6L ->
                            Timing.RACE_WEEK
                        else -> null
                    } ?: continue
                val context =
                    when (timing) {
                        Timing.SAME_DAY ->
                            "Lower-body gym work shares a day with a demanding planned run."
                        Timing.BEFORE ->
                            "Lower-body gym work is immediately before a demanding planned run."
                        Timing.AFTER ->
                            "Lower-body gym work is immediately after a demanding planned run; this is softer scheduling context, not proof of fatigue."
                        Timing.RACE_WEEK ->
                            "Lower-body gym work falls in the days before your planned race."
                    }
                val priority =
                    when (programme.settings().priority()) {
                        GymProgramme.Priority.STRENGTH_FIRST ->
                            "Strength First is selected. Keeping the gym session is a valid choice; neither discipline is silently sacrificed."
                        GymProgramme.Priority.RUNNING_FIRST ->
                            "Running First is selected. Moving gym work or reducing optional volume may leave more room for the run, if you choose."
                        GymProgramme.Priority.BALANCED ->
                            "Balanced is selected. Review the trade-off; both sessions may be kept."
                    }
                result.add(
                    Conflict(
                        gym.id(),
                        gym.date(),
                        run.id(),
                        run.date(),
                        run.category(),
                        timing,
                        "$context $priority",
                    )
                )
            }
        }
        result.sortWith(
            compareBy<Conflict> { it.gymDate()!!.toEpochDay() }
                .thenBy { it.workoutId()!! }
                .thenBy { it.runDate()!!.toEpochDay() }
                .thenBy { it.runId()!! }
        )
        return Immutable.copyList(result)
    }

    @JvmStatic
    fun meaningfulLower(workout: StrengthProgramme.Workout): Boolean {
        var sets = 0
        for (slot in workout.definition().slots()) {
            val canonical = ExerciseCatalog.definition(slot.exercise().id())
            val metadata =
                if (canonical != null && canonical.exercise() == slot.exercise())
                    ExerciseMetadataCatalog.find(slot.exercise().id())
                else null
            val lower =
                if (metadata != null)
                    metadata.primaryAreas().any {
                        it == ExerciseMetadata.BodyArea.QUADS ||
                            it == ExerciseMetadata.BodyArea.HAMSTRINGS ||
                            it == ExerciseMetadata.BodyArea.GLUTES ||
                            it == ExerciseMetadata.BodyArea.CALVES ||
                            it == ExerciseMetadata.BodyArea.LOWER_LEG
                    }
                else slot.exercise().region() == StrengthExercise.Region.LOWER
            if (lower && slot.target() != null) sets += slot.target()!!.sets()
        }
        return sets >= 2
    }

    @JvmStatic
    fun demanding(run: PlannedWorkout): Boolean =
        run.category() in
            listOf(
                WorkoutCategory.LONG_RUN,
                WorkoutCategory.TEMPO_THRESHOLD_RUN,
                WorkoutCategory.INTERVALS,
                WorkoutCategory.RACE_PACE_RUN,
                WorkoutCategory.TIME_TRIAL,
                WorkoutCategory.RACE,
            )

    private fun before(now: Instant, seconds: Long): Instant =
        Instants.ofEpochSecond(checkedAdd(now.getEpochSecond(), -seconds), now.getNano().toLong())

    private fun completion(session: StrengthSession): Instant =
        Instants.ofEpochSecond(
            checkedAdd(session.startedAt().getEpochSecond(), session.duration().getSeconds()),
            session.startedAt().getNano().toLong() + session.duration().getNano(),
        )
}
