package org.racedaydreams.domain.coaching

import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import org.racedaydreams.domain.RaceDistance
import org.racedaydreams.domain.TrainingPhase
import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.WorkoutSegment
import org.racedaydreams.domain.baseline.RunnerBaseline
import org.racedaydreams.domain.time.Durations

/**
 * Race-specific quality workouts (training Piece 4).
 *
 * Sessions are built from the existing segment model: warm-up, work repetitions with recovery jogs,
 * and cool-down. Intensity comes from current fitness evidence (recent race results), never from a
 * goal time alone; without a result, sessions are prescribed by effort. Most running stays easy: at
 * most one of these sessions a week, chosen by the plan generator.
 */
internal object QualityWorkouts {
    /** How much structured intensity current evidence supports, from least to most. */
    enum class Readiness {
        /** Easy running only (no usable history, aerobic strategy or returning from a break). */
        NONE,
        /** A few short, relaxed strides at the end of one easy run a week. */
        STRIDES,
        /** One controlled quality session a week, with modest doses. */
        CONTROLLED,
        /** The same sessions with more repetitions, for runners the Performance gate supports. */
        PERFORMANCE,
    }

    enum class Template {
        THRESHOLD,
        INTERVALS_5K,
        INTERVALS_10K,
        HALF_PACE,
        MARATHON_PACE,
    }

    /** Training paces in seconds per km, from the closest recent race result; null without one. */
    class Paces(
        val fiveK: Double?,
        val tenK: Double?,
        val half: Double?,
        val marathon: Double?,
        val threshold: Double?,
        /** Plain-language source, e.g. "your recent 10K (48:30)". */
        val source: String?,
    )

    class Session(
        val template: Template,
        val category: WorkoutCategory,
        val segments: List<WorkoutSegment>,
        val explanation: String,
    )

    /** Riegel-style estimates from the closest recent result. A guide, not a prediction. */
    fun paces(baseline: RunnerBaseline?): Paces {
        val results =
            listOfNotNull(
                baseline?.recent5k()?.let { Triple(5.0, it.getSeconds().toDouble(), "5K") },
                baseline?.recent10k()?.let { Triple(10.0, it.getSeconds().toDouble(), "10K") },
                baseline?.recentHalf()?.let {
                    Triple(21.0975, it.getSeconds().toDouble(), "half marathon")
                },
            )
        if (results.isEmpty()) return Paces(null, null, null, null, null, null)
        fun closest(km: Double) = results.minBy { kotlin.math.abs(ln(it.first / km)) }
        fun pace(km: Double): Double {
            val (d, t, _) = closest(km)
            return t * (km / d).pow(1.06) / km
        }
        // Threshold: roughly the pace that could be held for about an hour.
        val (d, t, _) = closest(15.0)
        val hourKm = d * (3600 / t).pow(1 / 1.06)
        val source =
            results.joinToString(" and ") { "your recent ${it.third} (${clock(it.second)})" }
        return Paces(pace(5.0), pace(10.0), pace(21.0975), pace(42.195), 3600 / hourKm, source)
    }

    /**
     * Which session a race distance uses in a phase: base phases build threshold first;
     * race-specific and peak phases use the race-specific session two weeks in three, threshold in
     * the third.
     */
    fun template(distance: RaceDistance, phase: TrainingPhase.Kind, week: Int): Template {
        val specific = phase == TrainingPhase.Kind.RACE_SPECIFIC || phase == TrainingPhase.Kind.PEAK
        if (!specific || week % 3 == 2) return Template.THRESHOLD
        return when (distance) {
            RaceDistance.FIVE_K -> Template.INTERVALS_5K
            RaceDistance.TEN_K -> Template.INTERVALS_10K
            RaceDistance.HALF_MARATHON -> Template.HALF_PACE
            RaceDistance.MARATHON -> Template.MARATHON_PACE
            else -> Template.THRESHOLD
        }
    }

    private class Shape(
        val repKm: Double,
        val recoverySeconds: Long,
        val minReps: Int,
        val controlledReps: Int,
        val performanceReps: Int,
        val rpe: Double,
        val category: WorkoutCategory,
    )

    private fun shape(template: Template) =
        when (template) {
            Template.THRESHOLD -> Shape(1.6, 120, 2, 3, 5, 6.0, WorkoutCategory.TEMPO_THRESHOLD_RUN)
            Template.INTERVALS_5K -> Shape(.8, 120, 4, 5, 7, 8.0, WorkoutCategory.INTERVALS)
            Template.INTERVALS_10K -> Shape(1.0, 90, 4, 5, 7, 7.5, WorkoutCategory.INTERVALS)
            Template.HALF_PACE -> Shape(2.0, 120, 2, 3, 4, 7.0, WorkoutCategory.RACE_PACE_RUN)
            // One steady block: the marathon is the one distance practised continuously.
            Template.MARATHON_PACE -> Shape(1.0, 0, 5, 8, 14, 6.0, WorkoutCategory.RACE_PACE_RUN)
        }

    /** About how far a recovery jog covers, for fitting a session into its planned distance. */
    private const val RECOVERY_KM_PER_SECOND = .18 / 60

    /** Planning allowance only; the live recovery ends by active time. */
    fun estimatedRecoveryKm(segments: List<WorkoutSegment>): Double =
        segments.sumOf {
            if (it.kind() == WorkoutSegment.Kind.RECOVERY && it.distanceKm() == null)
                (it.duration()?.getSeconds() ?: 0) * RECOVERY_KM_PER_SECOND
            else 0.0
        }

    /**
     * Builds [template] within [km] for [readiness]. [reduced] (taper or after hard legs) uses the
     * smallest dose. Returns null when the session does not fit, so the day stays easy.
     */
    fun build(
        template: Template,
        km: Double,
        readiness: Readiness,
        paces: Paces,
        reduced: Boolean,
    ): Session? {
        if (readiness < Readiness.CONTROLLED) return null
        val s = shape(template)
        val coolDown = 1.0
        // Faster repetitions need a fuller warm-up; threshold on a shorter day can start after 1
        // km.
        val minimumWarmUp = if (km >= 8) 2.0 else if (template == Template.THRESHOLD) 1.0 else 1.5
        val recoveryKm = s.recoverySeconds * RECOVERY_KM_PER_SECOND
        val maxReps =
            if (reduced) s.minReps
            else if (readiness == Readiness.PERFORMANCE) s.performanceReps else s.controlledReps
        val room = km - coolDown - minimumWarmUp
        var reps =
            if (template == Template.MARATHON_PACE) floor(room).toInt()
            else floor((room + recoveryKm) / (s.repKm + recoveryKm)).toInt()
        reps = minOf(reps, maxReps)
        if (reps < s.minReps) return null
        val work = reps * s.repKm
        val recoveries = if (template == Template.MARATHON_PACE) 0 else reps - 1
        val warmUp = km - coolDown - work - recoveries * recoveryKm
        val pace =
            when (template) {
                Template.THRESHOLD -> paces.threshold
                Template.INTERVALS_5K -> paces.fiveK
                Template.INTERVALS_10K -> paces.tenK
                Template.HALF_PACE -> paces.half
                Template.MARATHON_PACE -> paces.marathon
            }
        val effort =
            when (template) {
                Template.THRESHOLD ->
                    "Comfortably hard: you can speak a few words at a time but not chat."
                Template.INTERVALS_5K ->
                    "Strong 5K effort, controlled rather than all-out; every repetition should feel similar."
                Template.INTERVALS_10K ->
                    "10K effort: strong and steady; finish feeling you could have done one more."
                Template.HALF_PACE ->
                    "Half-marathon effort: steady and sustainable; slower if it feels like racing."
                Template.MARATHON_PACE ->
                    "Marathon effort: relaxed and steady; practise rhythm, drinks and fuelling."
            }
        val paceNote =
            if (pace != null) " Pace guide from ${paces.source}; effort comes first."
            else
                " Run by effort: there is no recent race result to set paces, and a goal time alone is not used."
        val segments = mutableListOf<WorkoutSegment>()
        segments +=
            WorkoutSegment(
                WorkoutSegment.Kind.WARM_UP,
                round1(warmUp),
                null,
                null,
                null,
                null,
                3.0,
                "Easy jog, then a few relaxed strides.",
            )
        if (template == Template.MARATHON_PACE)
            segments +=
                WorkoutSegment(
                    WorkoutSegment.Kind.RUN,
                    round1(work),
                    null,
                    pace?.times(.99),
                    pace?.times(1.04),
                    null,
                    s.rpe,
                    effort + paceNote,
                )
        else
            for (i in 1..reps) {
                segments +=
                    WorkoutSegment(
                        WorkoutSegment.Kind.RUN,
                        s.repKm,
                        null,
                        pace?.times(.98),
                        pace?.times(1.03),
                        null,
                        s.rpe,
                        "Repetition $i of $reps. " + effort + paceNote,
                    )
                if (i < reps)
                    segments +=
                        WorkoutSegment(
                            WorkoutSegment.Kind.RECOVERY,
                            null,
                            Durations.ofSeconds(s.recoverySeconds),
                            null,
                            null,
                            null,
                            2.0,
                            "Jog or walk gently until you feel ready again.",
                        )
            }
        segments +=
            WorkoutSegment(
                WorkoutSegment.Kind.COOL_DOWN,
                coolDown,
                null,
                null,
                null,
                null,
                2.0,
                "Easy jog to finish.",
            )
        val name =
            when (template) {
                Template.THRESHOLD ->
                    "Threshold: $reps × ${km(s.repKm)} with ${minutes(s.recoverySeconds)} easy jog"
                Template.INTERVALS_5K ->
                    "Intervals: $reps × 800 m at 5K effort with ${minutes(s.recoverySeconds)} jog"
                Template.INTERVALS_10K ->
                    "Intervals: $reps × 1 km at 10K effort with ${minutes(s.recoverySeconds)} jog"
                Template.HALF_PACE ->
                    "Half-marathon pace: $reps × 2 km with ${minutes(s.recoverySeconds)} jog"
                Template.MARATHON_PACE -> "Marathon pace: ${km(work)} steady inside an easy run"
            }
        val purpose =
            when (template) {
                Template.THRESHOLD -> "Raises the pace you can hold comfortably for longer."
                Template.INTERVALS_5K,
                Template.INTERVALS_10K ->
                    "Builds speed for your race in short pieces, with recoveries so it stays controlled."
                Template.HALF_PACE -> "Practises the steady rhythm you will hold on race day."
                Template.MARATHON_PACE -> "Practises race rhythm and fuelling on tired legs."
            }
        val dose =
            if (reduced) " A shorter, familiar version to stay sharp without tiring you." else ""
        return Session(template, s.category, segments, "$name. $purpose$dose")
    }

    /**
     * Six short strides at the end of an easy run, as a gentle first step towards faster running.
     */
    fun strides(km: Double): List<WorkoutSegment> =
        listOf(
            WorkoutSegment(
                WorkoutSegment.Kind.RUN,
                round1(km - .4),
                null,
                null,
                null,
                null,
                3.0,
                "Easy conversational running.",
            ),
            WorkoutSegment(
                WorkoutSegment.Kind.RUN,
                .4,
                null,
                null,
                null,
                null,
                5.0,
                "6 × 20-second strides: quick but relaxed, not sprints; walk or jog back to recover fully between them.",
            ),
        )

    private fun round1(value: Double) = kotlin.math.round(value * 10) / 10.0

    private fun km(value: Double): String =
        if (value < 1) "${(value * 1000).toInt()} m"
        else if (value == floor(value)) "${value.toInt()} km" else "${round1(value)} km"

    private fun minutes(seconds: Long): String =
        if (seconds % 60 == 0L) "${seconds / 60} min" else "${seconds} s"

    private fun clock(seconds: Double): String {
        val total = kotlin.math.round(seconds).toLong()
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
        else "$m:${s.toString().padStart(2, '0')}"
    }
}
