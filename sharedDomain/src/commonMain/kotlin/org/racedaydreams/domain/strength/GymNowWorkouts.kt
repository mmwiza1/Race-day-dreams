package org.racedaydreams.domain.strength

import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.strength.ExerciseMetadata.BodyArea
import org.racedaydreams.domain.strength.ExerciseMetadata.Equipment
import org.racedaydreams.domain.strength.ExerciseMetadata.Role
import org.racedaydreams.domain.time.*

/** Date-free one-off intentions. Selection reuses the planner; no programme or actual is saved. */
object GymNowWorkouts {
    private val REFERENCE_START = CalendarDates.of(2020, 1, 6)

    enum class Focus(private val displayTitle: String) {
        FULL_BODY("Full body workout"),
        UPPER("Upper body workout"),
        LOWER("Lower body workout");

        fun title(): String = displayTitle
    }

    enum class MuscleGroup(val label: String, private vararg val areas: BodyArea) {
        CHEST("Chest", BodyArea.CHEST),
        BACK("Back", BodyArea.UPPER_BACK, BodyArea.LATS),
        SHOULDERS("Shoulders", BodyArea.SHOULDERS),
        ARMS("Arms", BodyArea.BICEPS, BodyArea.TRICEPS, BodyArea.FOREARMS),
        LEGS(
            "Legs",
            BodyArea.QUADS,
            BodyArea.HAMSTRINGS,
            BodyArea.GLUTES,
            BodyArea.CALVES,
            BodyArea.LOWER_LEG,
            BodyArea.HIP_ADDUCTORS,
            BodyArea.HIP_FLEXORS,
        ),
        CORE("Core", BodyArea.TRUNK);

        fun matches(metadata: ExerciseMetadata): Boolean =
            areas.any { it in metadata.primaryAreas() }
    }

    data class Request
    @JvmOverloads
    constructor(
        private val minutes: Int,
        private var equipment: Set<String>?,
        private val focus: Focus?,
        private val goal: GymProgramme.Goal?,
        private val experience: GymProgramme.Experience?,
        private val priority: GymProgramme.Priority?,
        private val effort: GymProgramme.Effort?,
        private var muscleGroups: Set<MuscleGroup> = emptySet(),
        val variant: Int = 0,
    ) {
        init {
            require(!equipment.isNullOrEmpty()) {
                "Choose the equipment available, including bodyweight if appropriate."
            }
            require(
                focus != null &&
                    goal != null &&
                    experience != null &&
                    priority != null &&
                    effort != null
            ) {
                "Choose a focus, goal, experience, priority and effort scale."
            }
            equipment = Immutable.copySet(equipment!!)
            muscleGroups = Immutable.copySet(muscleGroups)
            require(variant >= 0)
            GymProgramme.Settings(
                goal,
                priority,
                experience,
                Immutable.set(DayOfWeek.MONDAY),
                1,
                equipment,
                minutes,
                effort,
            )
        }

        fun muscleGroups(): Set<MuscleGroup> = muscleGroups

        fun minutes(): Int = minutes

        fun equipment(): Set<String> = equipment!!

        fun focus(): Focus = focus!!

        fun goal(): GymProgramme.Goal = goal!!

        fun experience(): GymProgramme.Experience = experience!!

        fun priority(): GymProgramme.Priority = priority!!

        fun effort(): GymProgramme.Effort = effort!!

        override fun toString(): String =
            "Request[minutes=$minutes, equipment=$equipment, focus=$focus, goal=$goal, experience=$experience, priority=$priority, effort=$effort]"
    }

    data class Suggestion(
        private val definition: GymWorkoutDefinition?,
        private val explanation: String?,
        private var limitations: List<String>?,
    ) {
        init {
            if (definition == null) throw NullPointerException("one-off definition")
            require(explanation != null && explanation.trim { it <= ' ' }.isNotEmpty()) {
                "Explain the proposed one-off workout."
            }
            limitations = Immutable.copyList(limitations ?: throw NullPointerException())
        }

        fun definition(): GymWorkoutDefinition = definition!!

        fun explanation(): String = explanation!!

        fun limitations(): List<String> = limitations!!

        override fun toString(): String =
            "Suggestion[definition=$definition, explanation=$explanation, limitations=$limitations]"
    }

    @JvmStatic
    @JvmOverloads
    fun generate(
        workoutId: String?,
        createdAt: Instant?,
        request: Request,
        favourites: Set<String> = emptySet(),
        excluded: Set<String> = emptySet(),
    ): Suggestion {
        if (request.muscleGroups().isNotEmpty())
            return focusedWorkout(workoutId, createdAt, request, favourites, excluded)
        val frequency = if (request.focus() == Focus.FULL_BODY) 1 else 4
        val days =
            if (frequency == 1) Immutable.set(DayOfWeek.MONDAY)
            else
                Immutable.set(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                )
        val settings =
            GymProgramme.Settings(
                request.goal(),
                request.priority(),
                request.experience(),
                days,
                frequency,
                request.equipment(),
                request.minutes(),
                request.effort(),
            )
        val reference =
            GymProgrammeGenerator(capabilities(request.equipment()))
                .generate(
                    "one-off-reference",
                    1,
                    createdAt,
                    "Temporary prescription selection, not an athlete programme",
                    settings,
                    REFERENCE_START,
                    emptyList(),
                    emptyList(),
                    Immutable.copySet(favourites),
                    Immutable.copySet(excluded),
                )
        val prefix =
            when (request.focus()) {
                Focus.FULL_BODY -> "Full body"
                Focus.UPPER -> "Upper"
                Focus.LOWER -> "Lower"
            }
        val selected = reference.sessions().firstOrNull { it.title().startsWith("$prefix ") }
        require(selected != null && selected.exercises().isNotEmpty()) {
            "No matching workout fits these choices. Review focus, equipment and time."
        }
        val lower = GymProgrammeGenerator.hasLowerBodyWork(selected.exercises())
        var upper = false
        var pull = false
        for (prescription in selected.exercises()) {
            val metadata = ExerciseMetadataCatalog.require(prescription.exerciseId())
            upper =
                upper ||
                    metadata.roles().any {
                        it == ExerciseMetadata.Role.HORIZONTAL_PUSH ||
                            it == ExerciseMetadata.Role.VERTICAL_PUSH ||
                            it == ExerciseMetadata.Role.HORIZONTAL_PULL ||
                            it == ExerciseMetadata.Role.VERTICAL_PULL
                    }
            pull =
                pull ||
                    metadata.roles().any {
                        it == ExerciseMetadata.Role.HORIZONTAL_PULL ||
                            it == ExerciseMetadata.Role.VERTICAL_PULL
                    }
        }
        require(request.focus() != Focus.FULL_BODY || lower && upper) {
            "These choices do not leave room for both upper- and lower-body work. Choose more time or a different focus."
        }
        val prepared = GymWorkoutAdapter.fromProgramme(reference, selected.id())
        val slots =
            prepared.definition().slots().map {
                GymWorkoutDefinition.Slot(
                    it.id(),
                    it.exercise(),
                    it.target()?.withoutLoadReference(),
                    it.supersetGroup(),
                )
            }
        val definition =
            GymWorkoutDefinition(workoutId, 1, createdAt, request.focus().title(), slots)
        val limitations = mutableListOf<String>()
        if (request.focus() != Focus.LOWER && !pull)
            limitations.add(
                "No upper-body pulling exercise fits this selection. Core work is not a replacement for rows or pull-ups; review equipment or allow more time if pulling is wanted today."
            )
        if (request.goal() == GymProgramme.Goal.POWER)
            limitations.add(
                "Power preparation uses the planner's controlled strength movements and movement-speed intent. It does not add Olympic lifts or maximal testing."
            )
        for (slot in slots) {
            val metadata = ExerciseMetadataCatalog.require(slot.exercise().id())
            if (!metadata.suitability().appropriateFor(request.experience()))
                limitations.add(
                    slot.exercise().name() +
                        " uses a more complex variation than your selected experience. Review its technique guide and choose another variation if preferred."
                )
        }
        var explanation =
            "One-off ${request.focus().title().lowercase()} for ${goalLabel(request.goal())}, using ${request.experience().name.lowercase()} experience and ${request.priority().name.lowercase().replace('_', ' ')} priority. Targets follow the gym planner's current rules. Choose manageable loads; no starting weight has been inferred. This does not change a programme."
        if (slots.any { it.exercise().id() in favourites })
            explanation +=
                " Suitable favourite exercises were preferred within the required movement roles."
        return Suggestion(definition, explanation, limitations)
    }

    private fun focusedWorkout(
        workoutId: String?,
        createdAt: Instant?,
        request: Request,
        favourites: Set<String>,
        excluded: Set<String>,
    ): Suggestion {
        val selected = MuscleGroup.entries.filter { it in request.muscleGroups() }
        val equipment = capabilities(request.equipment())
        val candidates =
            ExerciseCatalog.definitions().mapNotNull { definition ->
                val id = definition.exercise().id()
                val metadata = ExerciseMetadataCatalog.find(id) ?: return@mapNotNull null
                metadata.takeIf {
                    id !in excluded &&
                        metadata.equipment().availableWith(equipment) &&
                        metadata.suitability().appropriateFor(request.experience()) &&
                        Role.CONDITIONING !in metadata.roles() &&
                        Role.POWER !in metadata.roles()
                }
            }
        val slotsByGroup =
            selected.associateWith { group ->
                candidates
                    .filter(group::matches)
                    .sortedWith(
                        compareByDescending<ExerciseMetadata> { it.exerciseId() in favourites }
                            .thenByDescending { Role.PRIMARY_STRENGTH in it.roles() }
                            .thenBy { it.exerciseId() }
                    )
            }
        val proposed = linkedSetOf<String>()
        val gained = mutableSetOf<MuscleGroup>()
        // Round-robin through the selected groups and fit the result once to the whole budget.
        repeat(if (selected.size == 1) 4 else 3) { round ->
            for (group in selected) {
                val ordered = slotsByGroup.getValue(group)
                if (round == 0 && group in gained || ordered.isEmpty()) continue
                val window = minOf(ordered.size, 3)
                val first = request.variant % window
                val rotated =
                    ordered.take(window).let { it.drop(first) + it.take(first) } +
                        ordered.drop(window)
                val chosen = rotated.firstOrNull { it.exerciseId() !in proposed } ?: continue
                proposed.add(chosen.exerciseId())
                gained.addAll(selected.filter { it.matches(chosen) })
            }
        }
        val settings =
            GymProgramme.Settings(
                request.goal(),
                request.priority(),
                request.experience(),
                Immutable.set(DayOfWeek.MONDAY),
                1,
                request.equipment(),
                request.minutes(),
                request.effort(),
            )
        val prescriptions =
            GymProgrammeGenerator.prescriptionsForIds(
                settings,
                proposed.toList(),
                favourites,
                conservativeTimeBudget = true,
            )
        require(prescriptions.isNotEmpty()) {
            "No exercise fits the selected muscle groups, equipment and experience."
        }
        val slots =
            prescriptions.mapIndexed { index, prescription ->
                val exercise = ExerciseCatalog.definition(prescription.exerciseId())!!.exercise()
                GymWorkoutAdapter.prescribedSlot(
                    "slot-$index",
                    exercise,
                    prescription,
                    request.effort(),
                )
            }
        val covered = slots.map { ExerciseMetadataCatalog.require(it.exercise().id()) }
        val missing = selected.filter { group -> covered.none(group::matches) }
        val limits =
            if (missing.isEmpty()) emptyList()
            else
                listOf(
                    "No main exercise for ${missing.joinToString { it.label }} fits this session. Try more time or equipment."
                )
        return Suggestion(
            GymWorkoutDefinition(
                workoutId,
                1,
                createdAt,
                selected.joinToString(" + ") { it.label },
                slots,
            ),
            "Focus on ${selected.joinToString { it.label.lowercase() }} in one ${request.minutes()}-minute workout. The targets use your goal and experience. Choose a manageable load.",
            limits,
        )
    }

    /** Uses the same explicit equipment capabilities as live substitution. */
    fun capabilities(labels: Set<String>): Set<Equipment> {
        if ("full gym" in labels) return Equipment.entries.toSet()
        return buildSet {
            for (label in labels) {
                when (label) {
                    "barbell" -> add(Equipment.BARBELL)
                    "dumbbells" -> add(Equipment.DUMBBELLS)
                    "kettlebells" -> add(Equipment.KETTLEBELLS)
                    "squat rack" -> add(Equipment.SQUAT_RACK)
                    "bench" -> add(Equipment.BENCH)
                    "machines",
                    "cables" -> {
                        val group =
                            if (label == "machines") ExerciseMetadata.EquipmentGroup.MACHINES
                            else ExerciseMetadata.EquipmentGroup.CABLES
                        Equipment.entries.filterTo(this) { it.group() == group }
                    }
                    "pull-up bar" -> add(Equipment.PULL_UP_BAR)
                    "trap bar" -> add(Equipment.TRAP_BAR)
                    "sled" -> add(Equipment.SLED)
                    "resistance bands" -> add(Equipment.RESISTANCE_BAND)
                    "band anchor" -> add(Equipment.SECURE_BAND_ANCHOR)
                    "stable step" -> add(Equipment.STABLE_STEP)
                }
            }
        }
    }

    fun alternatives(
        request: Request,
        definition: GymWorkoutDefinition,
        slot: GymWorkoutDefinition.Slot,
        excluded: Set<String>,
        favourites: Set<String>,
    ): List<ExerciseSubstitutionEngine.Suggestion> {
        val used = definition.slots().map { it.exercise().id() }.toSet()
        val original = ExerciseMetadataCatalog.find(slot.exercise().id()) ?: return emptyList()
        val groups = request.muscleGroups().filter { it.matches(original) }
        return ExerciseSubstitutionEngine.rank(
                ExerciseSubstitutionEngine.Context(
                    slot.exercise(),
                    request.goal(),
                    request.experience(),
                    capabilities(request.equipment()),
                    excluded + used,
                    favourites,
                    emptySet(),
                    ExerciseSubstitutionEngine.Reason.ALTERNATIVES,
                ),
                20,
            )
            .filter {
                val alternative = ExerciseMetadataCatalog.require(it.exercise().id())
                it.exercise().measurement() == slot.exercise().measurement() &&
                    groups.all { group -> group.matches(alternative) }
            }
            .take(6)
    }

    private fun goalLabel(goal: GymProgramme.Goal): String =
        when (goal) {
            GymProgramme.Goal.SUPPORT_RUNNING -> "running support"
            GymProgramme.Goal.STRENGTH -> "getting stronger"
            GymProgramme.Goal.MUSCLE -> "building muscle"
            GymProgramme.Goal.GENERAL -> "general strength and fitness"
            GymProgramme.Goal.POWER -> "power preparation"
        }
}
