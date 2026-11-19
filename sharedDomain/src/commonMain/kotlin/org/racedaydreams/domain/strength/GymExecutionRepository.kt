package org.racedaydreams.domain.strength

import org.racedaydreams.domain.time.Instant

/** Typed local durability. Unfinished actuals never count as completed training evidence. */
interface GymExecutionRepository {
    data class Stored(private val execution: GymExecution?, private val revision: Long) {
        init {
            require(execution != null && revision >= 1) {
                "A stored execution needs a positive revision."
            }
        }

        fun execution(): GymExecution = execution!!

        fun revision(): Long = revision

        override fun toString(): String = "Stored[execution=$execution, revision=$revision]"
    }

    /** Summary queries do not hydrate actual sets, event histories or workout definitions. */
    data class Summary(
        private val id: String?,
        private val origin: GymExecution.Origin?,
        private val status: GymExecution.Status?,
        private val title: String?,
        private val startedAt: Instant?,
        private val recordedElapsedMillis: Long,
        private val activeMillis: Long,
        private val actualSetCount: Int,
        private val revision: Long,
    ) {
        fun id(): String? = id

        fun origin(): GymExecution.Origin? = origin

        fun status(): GymExecution.Status? = status

        fun title(): String? = title

        fun startedAt(): Instant? = startedAt

        fun recordedElapsedMillis(): Long = recordedElapsedMillis

        fun activeMillis(): Long = activeMillis

        fun actualSetCount(): Int = actualSetCount

        fun revision(): Long = revision

        override fun toString(): String =
            "Summary[id=$id, origin=$origin, status=$status, title=$title, startedAt=$startedAt, recordedElapsedMillis=$recordedElapsedMillis, activeMillis=$activeMillis, actualSetCount=$actualSetCount, revision=$revision]"
    }

    data class Preference(
        private val workoutId: String?,
        private val currentRevision: Int,
        private val favourite: Boolean,
    ) {
        init {
            GymWorkoutDefinition.requireId(workoutId, "workout")
            require(currentRevision >= 1) { "Choose a saved revision." }
        }

        fun workoutId(): String = workoutId!!

        fun currentRevision(): Int = currentRevision

        fun favourite(): Boolean = favourite

        override fun toString(): String =
            "Preference[workoutId=$workoutId, currentRevision=$currentRevision, favourite=$favourite]"
    }

    // Nullable acknowledgements are retained so the coordinator rejects malformed platform stores.
    fun start(initial: GymExecution): Stored?

    fun load(id: String): Stored?

    fun unfinished(): List<Summary>

    /** Append atomically. An identical lost-acknowledgement retry must be harmless. */
    fun save(snapshot: GymExecution, expectedRevision: Long): Stored?

    /** Final actuals and the exact explicit planned association commit together. */
    fun finish(finished: GymExecution, expectedRevision: Long): Stored?

    fun saveWorkout(workout: GymWorkoutDefinition, source: GymExecution.SourceReference?)

    fun workout(id: String, revision: Int): GymWorkoutDefinition?

    fun preferences(): List<Preference>

    fun savePreference(preference: Preference)
}
