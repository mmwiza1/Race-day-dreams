package org.racedaydreams.domain.strength

import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.math.abs
import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.*

/**
 * Original deterministic six-week blocks; scheduling penalties are inspectable product heuristics.
 */
class GymProgrammeGenerator {
    @JvmOverloads
    fun generate(
        id: String?,
        version: Int,
        createdAt: Instant?,
        reason: String?,
        settings: GymProgramme.Settings,
        start: LocalDate,
        runs: List<PlannedWorkout>,
        actual: List<StrengthSession>,
        favourites: Set<String> = emptySet(),
        excluded: Set<String>? = emptySet(),
    ): GymProgramme {
        if (excluded == null) throw NullPointerException("exercise exclusions")
        val sessions = mutableListOf<GymProgramme.Session>()
        val tradeoffs = linkedSetOf<String>()
        val end = start.plusWeeks(6).minusDays(1)
        val hardActual = StrengthAnalytics.hardLowerBodyDates(actual)
        val plannedLowerDates = linkedSetOf<LocalDate>()
        val plans = mutableListOf<SessionPlan>()
        var lowerSessionOrdinal = 0
        for (i in 0 until settings.frequency()) {
            val template = template(settings.frequency(), i)
            val lowerTemplate = template == "Lower" || template == "Full body"
            val exercises =
                prescriptions(settings, template, lowerSessionOrdinal, favourites, excluded)
            if (lowerTemplate) lowerSessionOrdinal++
            plans.add(SessionPlan(template, exercises, hasLowerBodyWork(exercises)))
        }
        for (programmeWeek in 0 until 6) {
            val week = start.plusWeeks(programmeWeek.toLong())
            val candidates =
                (0..6)
                    .map { week.plusDays(it.toLong()) }
                    .filter { it.getDayOfWeek() in settings.days() }
            val best = Search(settings, runs, hardActual, plannedLowerDates, plans, week)
            best.permute(candidates, mutableListOf(), mutableSetOf())
            for (i in best.chosen.indices) {
                val day = best.chosen[i]
                val plan = plans[i]
                tradeoffs.addAll(conflicts(day, plan.lower, runs, hardActual))
                if (plan.lower) plannedLowerDates.add(day)
                sessions.add(
                    GymProgramme.Session(
                        "$id-v$version-$day",
                        day,
                        "${plan.template} ${i + 1}",
                        plan.exercises,
                        explanation(settings, plan.exercises),
                    )
                )
            }
        }
        sessions.sortBy { it.date().toEpochDay() }
        val lowerDates = plannedLowerDates.sortedBy { it.toEpochDay() }
        for (i in 1 until lowerDates.size) {
            val previous = lowerDates[i - 1]
            val day = lowerDates[i]
            if (day.toEpochDay() - previous.toEpochDay() == 1L)
                tradeoffs.add(
                    "Planned lower-body strength falls on consecutive days, ${formatGymProgrammeDate(previous)} and ${formatGymProgrammeDate(day)}. Consider moving a session or reducing gym effort; nothing has been removed."
                )
        }
        if (settings.frequency() > 3 && settings.priority() == GymProgramme.Priority.RUNNING_FIRST)
            tradeoffs.add(
                "You selected frequent gym training with running first. Working sets are deliberately limited; more days are not inherently better."
            )
        if (
            "full gym" !in settings.equipment() &&
                listOf("dumbbells", "barbell", "cables", "pull-up bar").none {
                    it in settings.equipment()
                }
        ) {
            val trunkPrescribed =
                plans.any { p ->
                    p.exercises.any {
                        ExerciseCatalog.definition(it.exerciseId())!!.category() == "Core"
                    }
                }
            tradeoffs.add(
                if (trunkPrescribed)
                    "No pulling equipment was selected. Trunk work fills a practical slot; it is not equivalent to rows or pull-ups."
                else
                    "No pulling equipment was selected. This block does not prescribe rows or pull-ups."
            )
        }
        if (
            settings.goal() == GymProgramme.Goal.POWER &&
                settings.experience() == GymProgramme.Experience.BEGINNER
        )
            tradeoffs.add(
                "Beginner power preparation uses controlled fundamentals; technical Olympic lifts are not automatically prescribed."
            )
        return GymProgramme(
            id,
            version,
            createdAt,
            reason,
            settings,
            start,
            end,
            sessions,
            Immutable.copyList(tradeoffs),
        )
    }

    private data class SessionPlan(
        val template: String,
        val exercises: List<GymProgramme.Prescription>,
        val lower: Boolean,
    )

    private class Search(
        val settings: GymProgramme.Settings,
        val runs: List<PlannedWorkout>,
        val actual: Set<LocalDate>,
        plannedLowerDates: Set<LocalDate>,
        val plans: List<SessionPlan>,
        val week: LocalDate,
    ) {
        val plannedLowerDates = Immutable.copySet(plannedLowerDates)
        var chosen: List<LocalDate> = emptyList()
        var best = Long.MAX_VALUE

        fun permute(
            days: List<LocalDate>,
            current: MutableList<LocalDate>,
            used: MutableSet<LocalDate>,
        ) {
            if (current.size == plans.size) {
                var score = 0L
                val conflictWeight =
                    when (settings.priority()) {
                        GymProgramme.Priority.RUNNING_FIRST -> 100
                        GymProgramme.Priority.BALANCED -> 60
                        else -> 30
                    }
                for (i in current.indices) {
                    val lower = plans[i].lower
                    score += schedulingPenalty(current[i], lower, runs, actual, conflictWeight)
                    if (lower && current[i].minusDays(1) in plannedLowerDates) score += 40
                    for (j in 0 until i) if (
                        lower &&
                            plans[j].lower &&
                            abs(current[i].toEpochDay() - current[j].toEpochDay()) == 1L
                    )
                        score += 40
                    score += abs(current[i].toEpochDay() - week.toEpochDay() - i * 7 / plans.size)
                }
                if (score < best) {
                    best = score
                    chosen = current.toList()
                }
                return
            }
            for (day in days) if (used.add(day)) {
                current.add(day)
                permute(days, current, used)
                current.removeAt(current.lastIndex)
                used.remove(day)
            }
        }
    }

    companion object {
        @JvmStatic
        fun template(frequency: Int, index: Int): String =
            when {
                frequency <= 3 -> "Full body"
                frequency == 4 -> if (index % 2 == 0) "Upper" else "Lower"
                frequency == 5 -> listOf("Upper", "Lower", "Push", "Pull", "Lower")[index]
                else -> listOf("Push", "Pull", "Lower")[index % 3]
            }

        private fun schedulingPenalty(
            day: LocalDate,
            lower: Boolean,
            runs: List<PlannedWorkout>,
            actual: Set<LocalDate>,
            conflictWeight: Int,
        ): Int {
            if (!lower) return 0
            var penalty = if (day.minusDays(1) in actual) conflictWeight else 0
            for (run in runs) {
                if (!demanding(run.category())) continue
                if (run.date() == day || run.date() == day.plusDays(1)) penalty += conflictWeight
                else if (run.date() == day.minusDays(1)) penalty += conflictWeight / 3
            }
            return penalty
        }

        @JvmStatic
        fun hasLowerBodyWork(exercises: List<GymProgramme.Prescription>): Boolean =
            exercises.any {
                val definition = ExerciseCatalog.definition(it.exerciseId())
                definition != null &&
                    definition.exercise().region() != StrengthExercise.Region.UPPER &&
                    definition.category() != "Core"
            }

        @JvmStatic
        fun conflicts(
            day: LocalDate,
            lower: Boolean,
            runs: List<PlannedWorkout>,
            actual: Set<LocalDate>,
        ): List<String> {
            val out = mutableListOf<String>()
            if (!lower) return out
            for (run in runs) {
                if (!demanding(run.category())) continue
                val relation =
                    when (run.date()) {
                        day -> "the same day as"
                        day.plusDays(1) -> "the day before"
                        day.minusDays(1) -> "the day after"
                        else -> continue
                    }
                val advice =
                    if (run.date() == day.minusDays(1))
                        "Review recovery and reduce gym effort if needed; nothing has been removed."
                    else
                        "Consider separating sessions or reducing gym effort; nothing has been removed."
                out.add(
                    "Lower-body strength on ${formatGymProgrammeDate(day)} is $relation ${run.category().name.lowercase().replace('_', ' ')} on ${formatGymProgrammeDate(run.date())}. $advice"
                )
            }
            if (day.minusDays(1) in actual)
                out.add(
                    "Hard lower-body work was actually recorded before ${formatGymProgrammeDate(day)}. Review the proposed session before training."
                )
            return Immutable.copyList(out)
        }

        @JvmStatic
        fun demanding(category: WorkoutCategory?): Boolean =
            category == WorkoutCategory.LONG_RUN ||
                category == WorkoutCategory.INTERVALS ||
                category == WorkoutCategory.TEMPO_THRESHOLD_RUN ||
                category == WorkoutCategory.RACE_PACE_RUN ||
                category == WorkoutCategory.PROGRESSION_RUN ||
                category == WorkoutCategory.RACE ||
                category == WorkoutCategory.TIME_TRIAL

        @JvmStatic
        fun available(id: String?, equipment: Set<String>): Boolean {
            if ("full gym" in equipment) return ExerciseCatalog.definition(id) != null
            val definition = ExerciseCatalog.definition(id) ?: return false
            val known = id!!
            val freeLoad =
                "dumbbells" in equipment || "kettlebells" in equipment || "barbell" in equipment
            if (known == "goblet-squat")
                return "dumbbells" in equipment || "kettlebells" in equipment
            if (known == "split-squat") return "bench" in equipment && freeLoad
            if (known == "hip-thrust")
                return "bench" in equipment && ("barbell" in equipment || "dumbbells" in equipment)
            if (
                known.contains("pull-up") ||
                    known.contains("chin-up") ||
                    known.startsWith("hanging-") ||
                    known == "toes-to-bar" ||
                    known == "dead-hang"
            )
                return "pull-up bar" in equipment && (!known.startsWith("weighted-") || freeLoad)
            if (known == "inverted-row")
                return "pull-up bar" in equipment || "squat rack" in equipment
            if (known == "bench-dip") return "bench" in equipment
            if (
                known in
                    listOf(
                        "dip",
                        "weighted-dip",
                        "ab-wheel",
                        "nordic-hamstring-curl",
                        "swiss-ball-leg-curl",
                        "sliding-leg-curl",
                    )
            )
                return false
            if (known in listOf("step-up", "box-step-up", "high-step-up"))
                return "bench" in equipment
            val required = definition.equipment()
            if (required == "bodyweight") return true
            if (required !in equipment) return false
            if (known == "back-squat" || known == "front-squat") return "squat rack" in equipment
            if (known.contains("bench-press"))
                return "bench" in equipment &&
                    (required == "dumbbells" || "squat rack" in equipment)
            return true
        }

        private fun choose(
            equipment: Set<String>,
            favourites: Set<String>,
            excluded: Set<String>,
            vararg ids: String,
        ): String {
            for (id in ids) {
                if (id !in excluded && available(id, equipment) && id in favourites) {
                    val guide = TechniqueGuide.forExercise(id)
                    val first = TechniqueGuide.forExercise(ids[0])
                    if (
                        guide != null &&
                            first != null &&
                            guide.pattern()!! == first.pattern() &&
                            ExerciseCatalog.definition(id)!!.exercise().measurement() ==
                                ExerciseCatalog.definition(ids[0])!!.exercise().measurement()
                    )
                        return id
                }
            }
            for (id in ids) if (id !in excluded && available(id, equipment)) return id
            throw IllegalArgumentException(
                "No suitable exercise fits your equipment and exclusions. Review these preferences; existing programmes are unchanged."
            )
        }

        private fun prescriptions(
            settings: GymProgramme.Settings,
            template: String,
            lowerSessionOrdinal: Int,
            favourites: Set<String>,
            excluded: Set<String>,
        ): List<GymProgramme.Prescription> {
            val equipment = settings.equipment()
            val ids = mutableListOf<String>()
            val lower = template == "Lower" || template == "Full body"
            val upper = template != "Lower"
            if (lower) {
                ids.add(
                    choose(
                        equipment,
                        favourites,
                        excluded,
                        if (lowerSessionOrdinal % 2 == 0) "back-squat" else "romanian-deadlift",
                        "goblet-squat",
                        "deadlift",
                        "bodyweight-squat",
                    )
                )
                ids.add(
                    choose(
                        equipment,
                        favourites,
                        excluded,
                        if (settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING) "split-squat"
                        else "hip-thrust",
                        "reverse-lunge-bodyweight",
                        "glute-bridge",
                    )
                )
            }
            if (upper) {
                if (template != "Pull")
                    ids.add(
                        choose(
                            equipment,
                            favourites,
                            excluded,
                            "bench-press",
                            "dumbbell-bench-press",
                            "push-up",
                        )
                    )
                if (template != "Push")
                    ids.add(
                        choose(
                            equipment,
                            favourites,
                            excluded,
                            "seated-cable-row",
                            "dumbbell-row",
                            "barbell-row",
                            "bird-dog",
                        )
                    )
            }
            if (settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING && lower)
                ids.add(
                    choose(
                        equipment,
                        favourites,
                        excluded,
                        "calf-raise",
                        "single-leg-calf-raise-bodyweight",
                    )
                )
            if (settings.minutes() >= 45 && ids.size < 5 && "plank" !in excluded) ids.add("plank")
            if (settings.minutes() >= 60 && upper)
                ids.add(
                    choose(
                        equipment,
                        favourites,
                        excluded,
                        if (template == "Pull") "barbell-curl" else "overhead-press",
                        "dumbbell-shoulder-press",
                        "dead-bug",
                    )
                )
            return prescriptionsForIds(settings, ids, favourites)
        }

        /** The same target and time budget also serve an explicitly focused one-off workout. */
        internal fun prescriptionsForIds(
            settings: GymProgramme.Settings,
            ids: List<String>,
            favourites: Set<String>,
            conservativeTimeBudget: Boolean = false,
        ): List<GymProgramme.Prescription> {
            var sets =
                if (
                    settings.experience() == GymProgramme.Experience.BEGINNER ||
                        settings.priority() == GymProgramme.Priority.RUNNING_FIRST
                )
                    2
                else 3
            if (
                settings.frequency() >= 4 &&
                    settings.priority() == GymProgramme.Priority.RUNNING_FIRST
            )
                sets = 2
            val minimum =
                when (settings.goal()) {
                    GymProgramme.Goal.STRENGTH -> 4
                    GymProgramme.Goal.POWER -> 3
                    GymProgramme.Goal.MUSCLE -> 8
                    else -> 6
                }
            val maximum =
                when (settings.goal()) {
                    GymProgramme.Goal.STRENGTH -> 6
                    GymProgramme.Goal.POWER -> 5
                    GymProgramme.Goal.MUSCLE -> 12
                    else -> 10
                }
            val minRpe = if (settings.experience() == GymProgramme.Experience.BEGINNER) 5 else 6
            val maxRpe = if (settings.priority() == GymProgramme.Priority.RUNNING_FIRST) 7 else 8
            val rest =
                if (
                    settings.goal() == GymProgramme.Goal.STRENGTH ||
                        settings.goal() == GymProgramme.Goal.POWER
                )
                    150
                else 90
            val out = mutableListOf<GymProgramme.Prescription>()
            var budget = settings.minutes() - 6
            for (id in ids.toSet()) {
                val cost =
                    if (conservativeTimeBudget) (sets * (rest + 60) + 119) / 60
                    else sets * (rest / 60 + 1) + 1
                if (budget < cost && out.isNotEmpty()) break
                budget -= cost
                val profile = ExerciseCatalog.definition(id)!!.exercise().measurement()
                val intent =
                    if (settings.goal() == GymProgramme.Goal.POWER) {
                        if (settings.experience() == GymProgramme.Experience.BEGINNER)
                            "Develop controlled fundamentals before adding power work. No maximal testing."
                        else
                            "Use a manageable moderate load, controlled lowering and an intentionally fast upward phase. Stop before speed or control deteriorates."
                    } else
                        "Choose a manageable load or variation within the effort range; progression is optional." +
                            if (id in favourites)
                                " This suitable exercise is one of your favourites."
                            else ""
                out.add(
                    GymProgramme.Prescription(
                        id,
                        sets,
                        if (profile.usesReps()) minimum else 0,
                        if (profile.usesReps()) maximum else 0,
                        if (profile.usesDuration()) 30 else null,
                        if (profile.usesDistance()) 30.0 else null,
                        minRpe,
                        maxRpe,
                        rest,
                        intent,
                    )
                )
            }
            return Immutable.copyList(out)
        }

        private fun explanation(
            settings: GymProgramme.Settings,
            exercises: List<GymProgramme.Prescription>,
        ): String {
            val movements = linkedSetOf<String>()
            for (prescription in exercises) {
                val guide = TechniqueGuide.forExercise(prescription.exerciseId()) ?: continue
                val pattern = guide.pattern() ?: throw NullPointerException()
                movements.add(
                    when (pattern) {
                        "split" -> "unilateral"
                        "bridge" -> "hip extension"
                        "press" -> "horizontal push"
                        "overhead" -> "overhead push"
                        "row",
                        "pull" -> "upper-body pull"
                        "core",
                        "plank" -> "trunk"
                        else -> pattern
                    }
                )
            }
            return "Stable six-week block. This session includes " +
                movements.joinToString(", ") +
                " work. " +
                (if (settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING)
                    "Selected work supports your running plan."
                else "Exercise and rep ranges reflect your selected strength goal.") +
                " Scheduling uses your available days and " +
                settings.priority().name.lowercase().replace('_', ' ') +
                " priority."
        }
    }
}

internal expect fun formatGymProgrammeDate(day: LocalDate): String
