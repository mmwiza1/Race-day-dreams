package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic

/** Extensible definitions. Actual sessions retain the definition used when recorded. */
data class StrengthExercise(
    private val id: String?,
    private val name: String?,
    private val region: Region?,
    private val mode: Mode?,
    private val estimateOneRepMax: Boolean,
    private val measurement: MeasurementProfile?,
) {
    enum class MeasurementProfile {
        LOAD_REPS,
        REPS_ONLY,
        DURATION,
        LOAD_DURATION,
        DISTANCE,
        LOAD_DISTANCE;

        fun usesLoad(): Boolean =
            this == LOAD_REPS || this == LOAD_DURATION || this == LOAD_DISTANCE

        fun usesReps(): Boolean = this == LOAD_REPS || this == REPS_ONLY

        fun usesDuration(): Boolean = this == DURATION || this == LOAD_DURATION

        fun usesDistance(): Boolean = this == DISTANCE || this == LOAD_DISTANCE
    }

    enum class Region {
        UPPER,
        LOWER,
        FULL,
    }

    enum class Mode {
        WEIGHTED,
        BODYWEIGHT,
        WEIGHTED_BODYWEIGHT,
    }

    constructor(
        id: String?,
        name: String?,
        region: Region?,
        mode: Mode?,
        estimateOneRepMax: Boolean,
    ) : this(
        id,
        name,
        region,
        mode,
        estimateOneRepMax,
        if (mode == Mode.BODYWEIGHT) MeasurementProfile.REPS_ONLY else MeasurementProfile.LOAD_REPS,
    )

    init {
        require(
            id != null &&
                id.trim { it <= ' ' }.isNotEmpty() &&
                name != null &&
                name.trim { it <= ' ' }.isNotEmpty() &&
                name.length <= 120
        ) {
            "Exercise needs an ID and a name of up to 120 characters."
        }
        if (region == null) throw NullPointerException()
        if (mode == null) throw NullPointerException()
        if (measurement == null) throw NullPointerException()
        require(!estimateOneRepMax || measurement == MeasurementProfile.LOAD_REPS) {
            "Estimated 1RM requires load and reps."
        }
        require(mode == Mode.WEIGHTED || !estimateOneRepMax) {
            "Estimated 1RM needs a suitable weighted exercise."
        }
    }

    fun id(): String = id!!

    fun name(): String = name!!

    fun region(): Region = region!!

    fun mode(): Mode = mode!!

    fun estimateOneRepMax(): Boolean = estimateOneRepMax

    fun measurement(): MeasurementProfile = measurement!!

    override fun toString(): String =
        "StrengthExercise[id=$id, name=$name, region=$region, mode=$mode, estimateOneRepMax=$estimateOneRepMax, measurement=$measurement]"

    companion object {
        @JvmStatic fun starters(): List<StrengthExercise> = ExerciseCatalog.exercises()
    }
}
