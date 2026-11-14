package org.racedaydreams.domain.strength

import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.strength.ExerciseMetadata.Role.*
import org.racedaydreams.domain.strength.StrengthProgramme.*
import org.racedaydreams.domain.strength.StrengthProgrammeTemplates.Split
import org.racedaydreams.domain.time.*

/**
 * Original bounded programming heuristics. No load is invented and no programme is applied here.
 */
class StrengthProgrammeGenerator {
    data class Request(
        private val id: String?,
        private val at: Instant?,
        private val name: String?,
        private val settings: Settings?,
        private val start: LocalDate?,
        private val weeks: Int,
        private val templateId: String?,
        private val focus: PrimaryFocus?,
        private var demandingRuns: Set<LocalDate>?,
        private val raceDate: LocalDate?,
        private var exclusions: Set<String>?,
        private var favourites: Set<String>?,
        private val recoveryAssessment: Boolean,
    ) {
        init {
            GymWorkoutDefinition.requireId(id, "programme")
            if (at == null) throw NullPointerException()
            StrengthProgramme.text(name, 120, false, "programme name")
            if (settings == null || start == null || focus == null) throw NullPointerException()
            require(weeks in 1..26) { "Generate 1–26 weeks." }
            demandingRuns = Immutable.copySet(demandingRuns ?: throw NullPointerException())
            exclusions = Immutable.copySet(exclusions ?: throw NullPointerException())
            favourites = Immutable.copySet(favourites ?: throw NullPointerException())
            require(
                templateId == null ||
                    StrengthProgrammeTemplates.require(templateId).frequency() ==
                        settings.frequency()
            ) {
                "Review the template frequency before generating."
            }
        }

        fun id(): String = id!!

        fun at(): Instant = at!!

        fun name(): String = name!!

        fun settings(): Settings = settings!!

        fun start(): LocalDate = start!!

        fun weeks(): Int = weeks

        fun templateId(): String? = templateId

        fun focus(): PrimaryFocus = focus!!

        fun demandingRuns(): Set<LocalDate> = demandingRuns!!

        fun raceDate(): LocalDate? = raceDate

        fun exclusions(): Set<String> = exclusions!!

        fun favourites(): Set<String> = favourites!!

        fun recoveryAssessment(): Boolean = recoveryAssessment

        override fun toString(): String =
            "Request[id=$id, at=$at, name=$name, settings=$settings, start=$start, weeks=$weeks, templateId=$templateId, focus=$focus, demandingRuns=$demandingRuns, raceDate=$raceDate, exclusions=$exclusions, favourites=$favourites, recoveryAssessment=$recoveryAssessment]"
    }

    fun generate(request: Request): StrengthProgramme {
        val settings = request.settings()
        val split =
            request.templateId()?.let {
                requireNotNull(StrengthProgrammeTemplates.require(it).split())
            }
                ?: when {
                    settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING -> Split.FULL_BODY
                    settings.frequency() >= 5 -> Split.PUSH_PULL_LEGS
                    settings.frequency() == 4 -> Split.UPPER_LOWER
                    else -> Split.FULL_BODY
                }
        val tradeoffs = linkedSetOf<String>()
        request.templateId()?.let {
            tradeoffs.add(requireNotNull(StrengthProgrammeTemplates.require(it).tradeoff()))
        }
        tradeoffs.add(
            "Original planning heuristics: adjust effort and volume to your experience; prescriptions are not actual sets."
        )
        if (settings.goal() == GymProgramme.Goal.POWER) {
            tradeoffs.add(
                "Power sets prioritise speed and control. Stop when either deteriorates; no maximal testing is required."
            )
            if (settings.experience() == GymProgramme.Experience.BEGINNER)
                tradeoffs.add(
                    "The current catalogue has no beginner-classified ballistic option for this setup. This preview builds stable strength and movement practice instead of inventing power exposure."
                )
        }
        val blocks = mutableListOf<Block>()
        var start = request.start()
        var blockIndex = 0
        for (phase in phases(request)) {
            val blockId = "${request.id()}-block-${blockIndex++}"
            val workouts = mutableListOf<Workout>()
            for (week in 0 until phase.weeks) {
                val first = start.plusWeeks(week.toLong())
                val days = selectDays(first, request, split)
                for (day in days.indices) {
                    val date = days[day]
                    val layout = layout(split, day, settings.goal())
                    var roles = layout.roles.toMutableList()
                    if (
                        settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING &&
                            settings.frequency() == 1
                    )
                        roles = mutableListOf(SQUAT, HINGE, UNILATERAL, CALF, CORE, HORIZONTAL_PULL)
                    if (
                        settings.goal() == GymProgramme.Goal.POWER &&
                            settings.experience() != GymProgramme.Experience.BEGINNER
                    )
                        roles.add(0, POWER)
                    var limit =
                        when (settings.experience()) {
                            GymProgramme.Experience.BEGINNER -> 4
                            GymProgramme.Experience.INTERMEDIATE -> 6
                            GymProgramme.Experience.EXPERIENCED -> 7
                        }
                    if (settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING)
                        limit = maxOf(5, limit)
                    if (settings.sessionMinutes() <= 30)
                        limit =
                            minOf(
                                limit,
                                if (settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING) 5 else 4,
                            )
                    if (settings.goal() == GymProgramme.Goal.POWER) limit = minOf(limit, 5)
                    if (
                        settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING &&
                            settings.frequency() == 1
                    )
                        limit = 6
                    val slots = mutableListOf<GymWorkoutDefinition.Slot>()
                    val seen = mutableSetOf<String>()
                    var minutes = 4
                    val race = request.raceDate()
                    val raceWeek =
                        request.focus() != PrimaryFocus.STRENGTH &&
                            race != null &&
                            date.toEpochDay() <= race.toEpochDay() &&
                            date.toEpochDay() >= race.minusDays(7).toEpochDay()
                    if (raceWeek)
                        tradeoffs.add(
                            "Race-week previews use fewer working sets; activation is your choice and the running plan stays unchanged."
                        )
                    val lowerSession =
                        split == Split.UPPER_LOWER && day % 2 == 1 ||
                            split == Split.PUSH_PULL_LEGS && day % 3 == 2
                    var required =
                        if (split == Split.FULL_BODY) {
                            if (settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING) {
                                if (settings.frequency() == 1) 6 else 5
                            } else 3
                        } else if (lowerSession) 4 else 2
                    if (
                        settings.goal() == GymProgramme.Goal.POWER &&
                            settings.experience() != GymProgramme.Experience.BEGINNER
                    )
                        required++
                    required = minOf(required, minOf(limit, roles.size))
                    var roleIndex = 0
                    for (role in roles) {
                        val reservedMinutes = maxOf(0, required - roleIndex++ - 1) * 2
                        if (slots.size >= limit) break
                        val exercise =
                            select(
                                role,
                                seen,
                                settings,
                                request,
                                day,
                                layout.title.startsWith("Push"),
                                tradeoffs,
                            ) ?: continue
                        var target =
                            target(exercise, role, slots.isEmpty(), phase.phase, settings, raceWeek)
                        var estimate = StrengthWorkoutBudget.exerciseMinutes(target)
                        val initialSets = target.sets()
                        while (
                            minutes + estimate + reservedMinutes > settings.sessionMinutes() &&
                                target.sets() > 1
                        ) {
                            target = sets(target, target.sets() - 1)
                            estimate = StrengthWorkoutBudget.exerciseMinutes(target)
                        }
                        if (target.sets() < initialSets)
                            tradeoffs.add(
                                "Working sets are reduced to fit your time budget while retaining key movement exposure. Review the preview before activation."
                            )
                        if (minutes + estimate > settings.sessionMinutes()) {
                            tradeoffs.add(
                                "The session time budget limits accessory exposure; review weekly balance before activation."
                            )
                            continue
                        }
                        seen.add(exercise.id())
                        minutes += estimate
                        slots.add(
                            GymWorkoutDefinition.Slot("slot-${slots.size}", exercise, target, null)
                        )
                    }
                    require(slots.size >= 2) {
                        "Not enough eligible exercises for a balanced session. Review equipment, exclusions or template."
                    }
                    val id = "${request.id()}-$date"
                    var sessionContext = ""
                    if (
                        request.focus() != PrimaryFocus.STRENGTH &&
                            lowerBody(slots) &&
                            conflict(date, request.demandingRuns()) > 0
                    ) {
                        val placement =
                            if (date in request.demandingRuns()) "on a demanding run day"
                            else if (date.plusDays(1) in request.demandingRuns())
                                "before a demanding run"
                            else "after a demanding run"
                        sessionContext =
                            "$date: lower-body work is $placement; consider moving it or keeping more effort reserve."
                        tradeoffs.add(sessionContext)
                    }
                    workouts.add(
                        Workout(
                            id,
                            date,
                            minutes,
                            GymWorkoutDefinition(id, 1, request.at(), layout.title, slots),
                            "Use manageable working loads. ${phaseExplanation(phase.phase)}${if (sessionContext.isEmpty()) "" else " $sessionContext"}",
                        )
                    )
                }
            }
            blocks.add(
                Block(
                    blockId,
                    phase.phase,
                    title(phase.phase),
                    start,
                    phase.weeks,
                    workouts,
                    phaseExplanation(phase.phase),
                )
            )
            start = start.plusWeeks(phase.weeks.toLong())
        }
        var summary = tradeoffs.toMutableList()
        if (summary.size > 100) {
            val remaining = summary.size - 99
            summary = summary.take(99).toMutableList()
            summary.add("$remaining additional trade-offs are retained in the dated workout notes.")
        }
        return StrengthProgramme(
            request.id(),
            1,
            0,
            request.at(),
            request.name(),
            if (request.templateId() == null) Origin.GENERATED else Origin.TEMPLATE,
            request.templateId(),
            settings,
            blocks,
            request.start(),
            "Athlete requested a preview; nothing is activated until explicitly accepted.",
            summary,
        )
    }

    private data class PhaseLength(val phase: Phase, val weeks: Int)

    private fun phases(request: Request): List<PhaseLength> {
        val weeks = request.weeks()
        val settings = request.settings()
        val result = mutableListOf<PhaseLength>()
        var remaining = weeks
        val recoveryWeeks = if (request.recoveryAssessment() && weeks >= 4) 1 else 0
        val foundation =
            if (weeks >= 8)
                when (settings.experience()) {
                    GymProgramme.Experience.BEGINNER -> 3
                    GymProgramme.Experience.EXPERIENCED -> 1
                    else -> 2
                }
            else 1
        result.add(PhaseLength(Phase.FOUNDATION, foundation))
        remaining -= foundation + recoveryWeeks
        val appropriate =
            settings.frequency() >= 2 &&
                (settings.experience() != GymProgramme.Experience.BEGINNER ||
                    settings.goal() == GymProgramme.Goal.STRENGTH && weeks >= 10) &&
                !(settings.goal() == GymProgramme.Goal.SUPPORT_RUNNING &&
                    settings.priority() == GymProgramme.Priority.RUNNING_FIRST)
        val intensify = if (weeks >= 8 && appropriate) 2 else 0
        if (remaining > intensify) result.add(PhaseLength(Phase.DEVELOPMENT, remaining - intensify))
        if (intensify > 0) result.add(PhaseLength(Phase.INTENSIFICATION, intensify))
        if (recoveryWeeks > 0) result.add(PhaseLength(Phase.RECOVERY_ASSESSMENT, 1))
        return result
    }

    private data class Layout(val title: String, val roles: List<ExerciseMetadata.Role>)

    private fun layout(split: Split, index: Int, goal: GymProgramme.Goal): Layout {
        if (split == Split.UPPER_LOWER) {
            if (index % 2 == 0)
                return Layout(
                    if (index == 0) "Upper · horizontal emphasis" else "Upper · vertical emphasis",
                    if (index == 0)
                        listOf(
                            HORIZONTAL_PUSH,
                            HORIZONTAL_PULL,
                            VERTICAL_PUSH,
                            VERTICAL_PULL,
                            ISOLATION,
                            CORE,
                        )
                    else
                        listOf(
                            VERTICAL_PUSH,
                            VERTICAL_PULL,
                            HORIZONTAL_PUSH,
                            HORIZONTAL_PULL,
                            ISOLATION,
                            CORE,
                        ),
                )
            return Layout(
                if (index == 1) "Lower · squat emphasis" else "Lower · hinge emphasis",
                listOf(
                    if (index == 1) SQUAT else HINGE,
                    UNILATERAL,
                    CALF,
                    CORE,
                    if (index == 1) HAMSTRING else SQUAT,
                ),
            )
        }
        if (split == Split.PUSH_PULL_LEGS)
            return when (index % 3) {
                0 ->
                    Layout(
                        if (index < 3) "Push A" else "Push B",
                        if (index < 3) listOf(HORIZONTAL_PUSH, VERTICAL_PUSH, ISOLATION, CORE)
                        else listOf(VERTICAL_PUSH, HORIZONTAL_PUSH, ISOLATION, CORE),
                    )
                1 ->
                    Layout(
                        if (index < 3) "Pull A" else "Pull B",
                        if (index < 3) listOf(HORIZONTAL_PULL, VERTICAL_PULL, CORE, CARRY)
                        else listOf(VERTICAL_PULL, HORIZONTAL_PULL, CARRY, CORE),
                    )
                else ->
                    Layout(
                        if (index < 3) "Legs · squat emphasis" else "Legs · hinge emphasis",
                        listOf(if (index < 3) SQUAT else HINGE, UNILATERAL, CALF, CORE, HAMSTRING),
                    )
            }
        if (goal == GymProgramme.Goal.SUPPORT_RUNNING)
            return Layout(
                "Running support ${index + 1}",
                listOf(
                    if (index % 2 == 0) SQUAT else HINGE,
                    UNILATERAL,
                    CALF,
                    CORE,
                    if (index % 2 == 0) HORIZONTAL_PUSH else HORIZONTAL_PULL,
                    if (index % 2 == 0) HINGE else SQUAT,
                ),
            )
        return Layout(
            "Full body ${index + 1}",
            listOf(
                if (index % 2 == 0) SQUAT else HINGE,
                if (index % 2 == 0) HORIZONTAL_PUSH else VERTICAL_PUSH,
                if (index % 2 == 0) HORIZONTAL_PULL else VERTICAL_PULL,
                CORE,
                UNILATERAL,
                CALF,
                ISOLATION,
            ),
        )
    }

    private fun select(
        role: ExerciseMetadata.Role,
        seen: Set<String>,
        settings: Settings,
        request: Request,
        variation: Int,
        pushDay: Boolean,
        tradeoffs: MutableSet<String>,
    ): StrengthExercise? {
        val options =
            ExerciseCatalog.exercises()
                .filter { e ->
                    val m = ExerciseMetadataCatalog.require(e.id())
                    m.primaryRole() == role &&
                        (role != ISOLATION ||
                            !pushDay ||
                            ExerciseMetadata.BodyArea.TRICEPS in m.primaryAreas()) &&
                        e.id() !in seen &&
                        e.id() !in request.exclusions() &&
                        m.suitability().appropriateFor(settings.experience()) &&
                        m.equipment().availableWith(settings.equipment())
                }
                .sortedWith(
                    compareBy<StrengthExercise> { preferred(it, role, settings, variation) }
                        .thenBy { if (it.id() in request.favourites()) 0 else 1 }
                        .thenBy { it.id() }
                )
        if (options.isEmpty()) {
            tradeoffs.add(
                "No eligible ${role.label().lowercase()} exercise for the selected equipment, experience and exclusions; this exposure is missing."
            )
            return null
        }
        return options[0]
    }

    private fun preferred(
        e: StrengthExercise,
        role: ExerciseMetadata.Role,
        settings: Settings,
        variation: Int,
    ): Int {
        if (settings.experience() == GymProgramme.Experience.BEGINNER)
            return if (e.mode() == StrengthExercise.Mode.BODYWEIGHT) 0 else 1
        val preferred =
            when (role) {
                SQUAT -> "back-squat"
                HINGE -> "romanian-deadlift"
                HORIZONTAL_PUSH -> "bench-press"
                HORIZONTAL_PULL -> "dumbbell-row"
                VERTICAL_PUSH -> "overhead-press"
                VERTICAL_PULL -> "lat-pulldown"
                POWER ->
                    if (settings.experience() == GymProgramme.Experience.EXPERIENCED) "high-pull"
                    else "kettlebell-swing"
                else -> ""
            }
        return if (e.id() == preferred) 0 else 1
    }

    private fun target(
        e: StrengthExercise,
        role: ExerciseMetadata.Role,
        primary: Boolean,
        phase: Phase,
        s: Settings,
        raceWeek: Boolean,
    ): GymWorkoutDefinition.Target {
        val beginner = s.experience() == GymProgramme.Experience.BEGINNER
        var sets = if (beginner) 2 else 3
        if (
            s.goal() == GymProgramme.Goal.MUSCLE &&
                s.priority() == GymProgramme.Priority.STRENGTH_FIRST &&
                s.experience() == GymProgramme.Experience.EXPERIENCED
        )
            sets = 4
        if (
            s.priority() == GymProgramme.Priority.RUNNING_FIRST &&
                e.region() != StrengthExercise.Region.UPPER
        )
            sets = minOf(sets, 2)
        var low = 8
        var high = 12
        var rest = if (primary) 120 else 60
        if (s.goal() == GymProgramme.Goal.STRENGTH) {
            low = if (primary) 4 else 6
            high = if (primary) 6 else 10
            rest = if (primary) 180 else 90
        } else if (s.goal() == GymProgramme.Goal.SUPPORT_RUNNING) {
            low = if (primary && !beginner) 5 else 8
            high = if (primary && !beginner) 8 else 12
            if (s.priority() == GymProgramme.Priority.RUNNING_FIRST) sets = 2
        } else if (role == POWER) {
            low = 3
            high = 5
            sets = 3
            rest = 180
        }
        var minEffort = if (beginner) 5 else 6
        var maxEffort = if (beginner) 7 else 8
        if (s.goal() == GymProgramme.Goal.POWER && role != POWER) {
            sets = minOf(sets, 2)
            low = 6
            high = 8
        }
        if (phase == Phase.FOUNDATION) {
            sets = minOf(sets, if (beginner) 1 else 2)
            minEffort = if (beginner) 4 else 5
            maxEffort = if (beginner) 6 else 7
        }
        if (phase == Phase.INTENSIFICATION && role == POWER) {
            sets = 2
            low = 2
            high = 3
            rest = 210
        }
        if (phase == Phase.INTENSIFICATION && primary && role != POWER) {
            if (s.goal() == GymProgramme.Goal.STRENGTH) {
                low = 3
                high = 5
                rest = 180
            } else if (s.goal() == GymProgramme.Goal.MUSCLE) {
                low = 6
                high = 10
            }
            if (!beginner) {
                minEffort = 7
                maxEffort = 8
            }
        }
        if (phase == Phase.RECOVERY_ASSESSMENT || raceWeek) {
            sets = 1
            minEffort = 5
            maxEffort = 6
        }
        if (s.goal() == GymProgramme.Goal.POWER && phase != Phase.FOUNDATION) {
            minEffort = 5
            maxEffort = if (phase == Phase.RECOVERY_ASSESSMENT || raceWeek) 6 else 7
        }
        return GymWorkoutDefinition.Target(
            sets,
            if (e.measurement().usesReps()) low else null,
            if (e.measurement().usesReps()) high else null,
            if (e.measurement().usesDuration()) 30L else null,
            if (e.measurement().usesDistance()) 20.0 else null,
            GymWorkoutDefinition.EffortRange(
                GymWorkoutDefinition.EffortScale.RPE,
                minEffort.toDouble(),
                maxEffort.toDouble(),
            ),
            rest,
            null,
        )
    }

    private fun sets(t: GymWorkoutDefinition.Target, sets: Int): GymWorkoutDefinition.Target =
        GymWorkoutDefinition.Target(
            sets,
            t.minimumReps(),
            t.maximumReps(),
            t.seconds(),
            t.metres(),
            t.effort(),
            t.restSeconds(),
            null,
        )

    private fun lowerBody(slots: List<GymWorkoutDefinition.Slot>): Boolean =
        slots.any {
            it.exercise().region() != StrengthExercise.Region.UPPER &&
                ExerciseMetadataCatalog.require(it.exercise().id()).primaryRole() != CORE
        }

    private fun conflict(date: LocalDate, runs: Set<LocalDate>): Int =
        (if (date in runs) 5 else 0) +
            (if (date.plusDays(1) in runs) 4 else 0) +
            (if (date.minusDays(1) in runs) 2 else 0)

    private fun selectDays(first: LocalDate, r: Request, split: Split): List<LocalDate> {
        val days =
            (0..6)
                .map { first.plusDays(it.toLong()) }
                .filter { it.getDayOfWeek() in r.settings().days() }
        var best: List<LocalDate>? = null
        var bestCost = Int.MAX_VALUE
        for (mask in 1 until (1 shl days.size)) {
            if (mask.countOneBits() != r.settings().frequency()) continue
            val chosen = days.filterIndexed { i, _ -> (mask and (1 shl i)) != 0 }
            var cost = 0
            for (i in chosen.indices) {
                val lower =
                    split == Split.FULL_BODY ||
                        r.settings().goal() == GymProgramme.Goal.POWER &&
                            r.settings().experience() != GymProgramme.Experience.BEGINNER ||
                        split == Split.UPPER_LOWER && i % 2 == 1 ||
                        split == Split.PUSH_PULL_LEGS && i % 3 == 2
                if (lower && r.focus() != PrimaryFocus.STRENGTH)
                    cost += conflict(chosen[i], r.demandingRuns())
                if (i > 0 && chosen[i - 1].plusDays(1) == chosen[i]) cost++
            }
            if (cost < bestCost) {
                best = chosen
                bestCost = cost
            }
        }
        return best ?: throw NullPointerException()
    }

    private fun title(p: Phase): String = p.name.lowercase().replace('_', ' ')

    private fun phaseExplanation(p: Phase): String =
        when (p) {
            Phase.FOUNDATION ->
                "Practise stable movements, establish manageable working loads and keep effort in reserve."
            Phase.DEVELOPMENT ->
                "Repeat key movements with goal-specific working volume; load progression requires actual evidence."
            Phase.INTENSIFICATION ->
                "Use more specific primary work with controlled volume; accessories remain supportive."
            else ->
                "Optional lower-volume practice and reflection; no maximal assessment is required."
        }
}
