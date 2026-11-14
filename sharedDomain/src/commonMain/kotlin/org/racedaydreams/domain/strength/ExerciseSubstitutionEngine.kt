package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import kotlin.math.abs
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.strength.ExerciseMetadata.Equipment
import org.racedaydreams.domain.strength.ExerciseMetadata.Role
import org.racedaydreams.domain.strength.ExerciseMetadata.Suitability

/**
 * Original intent-ranked options, never equivalent loading, form assessment or medical clearance.
 */
object ExerciseSubstitutionEngine {
    private val addedLoadPairs =
        listOf(
            "push-up" to "weighted-push-up",
            "pull-up" to "weighted-pull-up",
            "chin-up" to "weighted-chin-up",
            "dip" to "weighted-dip",
            "plank" to "weighted-plank",
            "side-plank" to "weighted-side-plank",
            "bodyweight-squat" to "goblet-squat",
            "bodyweight-squat" to "kettlebell-goblet-squat",
            "single-leg-calf-raise-bodyweight" to "single-leg-calf-raise",
        )
    private val supportPairs =
        listOf(
            "wall-push-up" to "incline-push-up",
            "incline-push-up" to "push-up",
            "wall-push-up" to "knee-push-up",
            "wall-push-up" to "push-up",
            "knee-push-up" to "push-up",
            "chair-sit-to-stand" to "bodyweight-squat",
        )

    enum class Reason {
        EQUIPMENT_BUSY,
        EQUIPMENT_UNAVAILABLE,
        UNCOMFORTABLE,
        EASIER,
        HARDER,
        ALTERNATIVES,
    }

    /** Available equipment means explicit capabilities, never a generic machines wildcard. */
    data class Context(
        private val original: StrengthExercise?,
        private val goal: GymProgramme.Goal?,
        private val experience: GymProgramme.Experience?,
        private var availableEquipment: Set<Equipment?>?,
        private var excludedIds: Set<String?>?,
        private var favouriteIds: Set<String?>?,
        private var historyIds: Set<String?>?,
        private val reason: Reason?,
    ) {
        init {
            if (original == null) throw NullPointerException("original exercise")
            if (goal == null) throw NullPointerException("programme goal")
            if (experience == null) throw NullPointerException("experience")
            if (reason == null) throw NullPointerException("substitution reason")
            require(availableEquipment != null && availableEquipment!!.none { it == null }) {
                "Available equipment must be explicit."
            }
            availableEquipment =
                Immutable.copyOrderedSet(Equipment.entries.filter { it in availableEquipment!! })
            excludedIds = identities(excludedIds)
            favouriteIds = identities(favouriteIds)
            historyIds = identities(historyIds)
        }

        fun original(): StrengthExercise = original!!

        fun goal(): GymProgramme.Goal = goal!!

        fun experience(): GymProgramme.Experience = experience!!

        // Constructor validation and the immutable copies remove nulls before publication.
        @Suppress("UNCHECKED_CAST")
        fun availableEquipment(): Set<Equipment> = availableEquipment!! as Set<Equipment>

        @Suppress("UNCHECKED_CAST") fun excludedIds(): Set<String> = excludedIds!! as Set<String>

        @Suppress("UNCHECKED_CAST") fun favouriteIds(): Set<String> = favouriteIds!! as Set<String>

        @Suppress("UNCHECKED_CAST") fun historyIds(): Set<String> = historyIds!! as Set<String>

        fun reason(): Reason = reason!!

        override fun toString(): String =
            "Context[original=$original, goal=$goal, experience=$experience, availableEquipment=$availableEquipment, excludedIds=$excludedIds, favouriteIds=$favouriteIds, historyIds=$historyIds, reason=$reason]"
    }

    /** A changed task requires a fresh target. Every swap requires a fresh load reference. */
    data class Suggestion(
        private val exercise: StrengthExercise?,
        private val explanation: String?,
        private val measurementChanged: Boolean,
    ) {
        init {
            if (exercise == null) throw NullPointerException("suggested exercise")
            require(explanation != null && explanation.trim { it <= ' ' }.isNotEmpty()) {
                "Explain the substitution."
            }
        }

        fun exercise(): StrengthExercise = exercise!!

        fun explanation(): String = explanation!!

        fun measurementChanged(): Boolean = measurementChanged

        override fun toString(): String =
            "Suggestion[exercise=$exercise, explanation=$explanation, measurementChanged=$measurementChanged]"
    }

    @JvmStatic
    fun discomfortGuidance(): String =
        "Avoid movements that reproduce pain. These alternatives change the setup or movement pattern, but Race Day Dreams cannot determine which exercise is medically appropriate for an injury."

    /**
     * Missing metadata or reviewed progression relationships never produce invented alternatives.
     */
    @JvmStatic
    fun rank(context: Context?, limit: Int): List<Suggestion> {
        if (context == null) throw NullPointerException("substitution context")
        require(limit in 1..50) { "Choose between 1 and 50 alternatives." }
        val original =
            ExerciseMetadataCatalog.find(context.original().id()) ?: return Immutable.list()
        val canonical = ExerciseCatalog.definition(context.original().id())!!.exercise()
        if (
            context.original().measurement() != canonical.measurement() ||
                context.original().mode() != canonical.mode() ||
                context.original().region() != canonical.region()
        )
            return Immutable.list()
        val candidates = mutableListOf<Candidate>()
        for (exercise in ExerciseCatalog.exercises()) {
            if (exercise.id() == context.original().id() || exercise.id() in context.excludedIds())
                continue
            val metadata = ExerciseMetadataCatalog.require(exercise.id())
            if (
                metadata.primaryRole() != original.primaryRole() ||
                    overlap(metadata.primaryAreas(), original.primaryAreas()) == 0 ||
                    !compatibleTask(context.original().measurement(), exercise.measurement()) ||
                    !metadata.suitability().appropriateFor(context.experience()) ||
                    !metadata.equipment().availableWith(context.availableEquipment())
            )
                continue
            val progression = reviewedProgression(context.original().id(), exercise.id())
            val complexity = metadata.suitability().ordinal - original.suitability().ordinal
            if (context.reason() == Reason.EASIER && progression >= 0 && complexity >= 0) continue
            if (context.reason() == Reason.HARDER && progression <= 0 && complexity <= 0) continue
            if (
                context.reason() == Reason.EASIER && progression > 0 ||
                    context.reason() == Reason.HARDER && progression < 0
            )
                continue
            val equipment = bestSetup(metadata, original, context)
            candidates.add(
                Candidate(
                    exercise,
                    metadata,
                    equipment,
                    goalAlignment(original, metadata, context.goal()),
                    overlap(original.roles(), metadata.roles()),
                    overlap(original.primaryAreas(), metadata.primaryAreas()),
                    equipmentChangeCost(equipment, original, context.reason()),
                    if (context.original().measurement() == exercise.measurement()) 0 else 1,
                    abs(complexity),
                    if (exercise.id() in context.favouriteIds()) 0 else 1,
                    if (exercise.id() in context.historyIds()) 0 else 1,
                    progression,
                )
            )
        }
        candidates.sortWith(
            compareBy<Candidate> { it.equipmentCost }
                .thenByDescending { it.goalAlignment }
                .thenByDescending { it.sharedRoles }
                .thenByDescending { it.primaryOverlap }
                .thenBy { it.measurementCost }
                .thenBy { it.complexityDistance }
                .thenBy { it.favouriteCost }
                .thenBy { it.historyCost }
                .thenBy { it.exercise.id() }
        )
        return Immutable.copyList(
            candidates.take(limit).map {
                Suggestion(it.exercise, explanation(context, original, it), it.measurementCost != 0)
            }
        )
    }

    @JvmStatic
    fun compatibleTask(
        original: StrengthExercise.MeasurementProfile?,
        replacement: StrengthExercise.MeasurementProfile?,
    ): Boolean {
        if (original == null) throw NullPointerException("original measurement")
        if (replacement == null) throw NullPointerException("replacement measurement")
        return original.usesReps() && replacement.usesReps() ||
            original.usesDuration() && replacement.usesDuration() ||
            original.usesDistance() && replacement.usesDistance()
    }

    private data class Candidate(
        val exercise: StrengthExercise,
        val metadata: ExerciseMetadata,
        val equipment: Set<Equipment>,
        val goalAlignment: Int,
        val sharedRoles: Int,
        val primaryOverlap: Int,
        val equipmentCost: Int,
        val measurementCost: Int,
        val complexityDistance: Int,
        val favouriteCost: Int,
        val historyCost: Int,
        val progression: Int,
    )

    private fun goalAlignment(
        original: ExerciseMetadata,
        replacement: ExerciseMetadata,
        goal: GymProgramme.Goal,
    ): Int =
        when (goal) {
            GymProgramme.Goal.STRENGTH ->
                if (
                    Role.PRIMARY_STRENGTH in original.roles() &&
                        Role.PRIMARY_STRENGTH in replacement.roles()
                )
                    1
                else 0
            GymProgramme.Goal.POWER -> overlap(original.roles(), replacement.roles())
            GymProgramme.Goal.MUSCLE -> overlap(original.primaryAreas(), replacement.primaryAreas())
            GymProgramme.Goal.SUPPORT_RUNNING -> overlap(original.roles(), replacement.roles())
            GymProgramme.Goal.GENERAL ->
                if (replacement.suitability() == Suitability.BEGINNER_FRIENDLY) 1 else 0
        }

    private fun bestSetup(
        metadata: ExerciseMetadata,
        original: ExerciseMetadata,
        context: Context,
    ): Set<Equipment> {
        var best: Set<Equipment>? = null
        var cost = Int.MAX_VALUE
        for (setup in metadata.equipment().alternatives()) {
            if (!context.availableEquipment().containsAll(setup)) continue
            val candidateCost = equipmentChangeCost(setup, original, context.reason())
            if (
                best == null ||
                    candidateCost < cost ||
                    candidateCost == cost && signature(setup) < signature(best)
            ) {
                best = setup
                cost = candidateCost
            }
        }
        return best ?: throw NullPointerException("eligible complete setup")
    }

    private fun equipmentChangeCost(
        setup: Set<Equipment>,
        original: ExerciseMetadata,
        reason: Reason,
    ): Int {
        if (reason != Reason.EQUIPMENT_BUSY && reason != Reason.EQUIPMENT_UNAVAILABLE) return 0
        val originalEquipment = original.equipment().alternatives().flatten().toSet()
        return overlap(setup, originalEquipment)
    }

    private fun explanation(
        context: Context,
        original: ExerciseMetadata,
        candidate: Candidate,
    ): String = buildString {
        val areas =
            ExerciseMetadata.BodyArea.entries
                .filter { it in original.primaryAreas() && it in candidate.metadata.primaryAreas() }
                .map { it.label().lowercase() }
        append("Same ")
            .append(original.primaryRole().label().lowercase())
            .append(" role with shared ")
            .append(areas.joinToString(" / "))
            .append(" involvement. Uses ")
            .append(equipmentLabel(candidate.equipment))
            .append('.')
        if (context.reason() == Reason.EQUIPMENT_BUSY)
            append(" Check that this setup is free; shared equipment may still be busy.")
        if (context.reason() == Reason.EASIER || context.reason() == Reason.HARDER) {
            if (
                candidate.progression != 0 &&
                    context.original().mode() == StrengthExercise.Mode.BODYWEIGHT &&
                    candidate.exercise.mode() == StrengthExercise.Mode.BODYWEIGHT
            )
                append(
                    if (candidate.progression < 0)
                        " Uses more support or less demanding leverage; review your repetition target."
                    else
                        " Uses less support or more demanding leverage; review your repetition target."
                )
            else if (candidate.progression != 0)
                append(
                    if (candidate.progression < 0) " Removes the added-loading variation."
                    else " Offers an added-loading progression; choose a manageable load."
                )
            else
                append(
                    if (candidate.metadata.suitability().ordinal < original.suitability().ordinal)
                        " A less technically complex setup."
                    else " A more technically complex variation within your selected experience."
                )
            append(" Difficulty still depends on load, control and execution.")
        }
        if (candidate.measurementCost != 0)
            append(
                " Measurement changes: review a fresh target; previous targets and loads do not transfer."
            )
        else append(" Previous loads do not transfer; choose and record the actual load or task.")
        if (candidate.exercise.id() in context.favouriteIds()) append(" Also saved as a favourite.")
        if (candidate.exercise.id() in context.historyIds())
            append(
                " You have recorded this exercise before; that is not a starting-load recommendation."
            )
        if (context.reason() == Reason.UNCOMFORTABLE)
            append(" An alternative is not medical clearance; avoid movements that reproduce pain.")
    }

    private fun equipmentLabel(setup: Set<Equipment>): String =
        if (setup.isEmpty()) "no external equipment"
        else
            Equipment.entries
                .filter { it in setup }
                .joinToString(" + ") { it.name.lowercase().replace('_', ' ') }

    private fun signature(setup: Set<Equipment>): String =
        Equipment.entries.filter { it in setup }.joinToString("") { it.name + "/" }

    private fun reviewedProgression(original: String, replacement: String): Int {
        for (pairs in listOf(addedLoadPairs, supportPairs)) for ((first, second) in pairs) {
            if (first == original && second == replacement) return 1
            if (second == original && first == replacement) return -1
        }
        return 0
    }

    private fun identities(ids: Set<String?>?): Set<String> {
        require(ids != null && ids.none { it == null || it.trim { c -> c <= ' ' }.isEmpty() }) {
            "Exercise identity choices must be explicit."
        }
        return Immutable.copySet(ids.map { it!! }.toSet())
    }

    private fun overlap(left: Set<*>, right: Set<*>): Int = left.count { it in right }
}
