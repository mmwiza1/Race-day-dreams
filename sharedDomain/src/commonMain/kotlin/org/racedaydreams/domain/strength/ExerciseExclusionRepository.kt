package org.racedaydreams.domain.strength

import org.racedaydreams.domain.isJavaBlank
import org.racedaydreams.domain.time.Instant

/** Explicit reversible programming preference; never deletion of exercise evidence. */
interface ExerciseExclusionRepository {
    enum class Reason {
        PREFERENCE,
        EQUIPMENT_UNAVAILABLE,
        UNCOMFORTABLE,
        OTHER,
    }

    data class Exclusion(
        private val exerciseId: String?,
        private val reason: Reason?,
        private val excludedAt: Instant?,
    ) {
        init {
            require(exerciseId != null && !isJavaBlank(exerciseId) && exerciseId.length <= 200) {
                "Invalid excluded exercise."
            }
            if (reason == null) throw NullPointerException("exclusion reason")
            if (excludedAt == null) throw NullPointerException("exclusion date")
        }

        fun exerciseId(): String = exerciseId!!

        fun reason(): Reason = reason!!

        fun excludedAt(): Instant = excludedAt!!

        override fun toString(): String =
            "Exclusion[exerciseId=$exerciseId, reason=$reason, excludedAt=$excludedAt]"
    }

    fun exclusions(): List<Exclusion>

    fun exclude(exclusion: Exclusion)

    fun restoreExercise(exerciseId: String)
}
