package org.racedaydreams.domain.strength

/** Athlete-authored metadata; no fabricated technique, suitability or load-transfer evidence. */
data class CustomStrengthExercise(
    private val exercise: StrengthExercise?,
    private val bodyArea: ExerciseMetadata.BodyArea?,
    private val role: ExerciseMetadata.Role?,
    private val equipment: ExerciseMetadata.EquipmentRequirement?,
    private val notes: String?,
) {
    init {
        if (exercise == null) throw NullPointerException()
        if (bodyArea == null) throw NullPointerException()
        if (role == null) throw NullPointerException()
        if (equipment == null) throw NullPointerException()
        require(
            exercise.id().startsWith("custom-") &&
                ExerciseCatalog.definition(exercise.id()) == null &&
                Regex("[a-z0-9]+(?:-[a-z0-9]+)*").matches(exercise.id())
        ) {
            "Use a distinct stable custom exercise identity."
        }
        require(!exercise.estimateOneRepMax()) {
            "Custom exercises do not infer estimated strength or automatic progression."
        }
        StrengthProgramme.text(notes, 2000, true, "custom exercise notes")
    }

    fun exercise(): StrengthExercise = exercise!!

    fun bodyArea(): ExerciseMetadata.BodyArea = bodyArea!!

    fun role(): ExerciseMetadata.Role = role!!

    fun equipment(): ExerciseMetadata.EquipmentRequirement = equipment!!

    fun notes(): String? = notes

    override fun toString(): String =
        "CustomStrengthExercise[exercise=$exercise, bodyArea=$bodyArea, role=$role, equipment=$equipment, notes=$notes]"
}
