package org.racedaydreams.domain.coaching

import org.racedaydreams.domain.*
import org.racedaydreams.domain.time.*

/**
 * Retained prescription/response snapshot; actual activity and explicit choices remain separate.
 */
data class WorkoutDecision(
    private val id: String?,
    private val workoutId: String?,
    private val versionId: String?,
    private val originalDate: LocalDate?,
    private val movedTo: LocalDate?,
    private val kind: Kind?,
    private val at: Instant?,
) {
    enum class Kind {
        MOVED,
        SKIPPED,
    }

    init {
        if (originalDate == null || kind == null || at == null) throw NullPointerException()
        require(
            id != null &&
                workoutId != null &&
                versionId != null &&
                (kind == Kind.MOVED) == (movedTo != null)
        ) {
            "Invalid workout decision."
        }
    }

    fun id(): String = id!!

    fun workoutId(): String = workoutId!!

    fun versionId(): String = versionId!!

    fun originalDate(): LocalDate = originalDate!!

    fun movedTo(): LocalDate? = movedTo

    fun kind(): Kind = kind!!

    fun at(): Instant = at!!

    override fun toString(): String =
        "WorkoutDecision[id=$id, workoutId=$workoutId, versionId=$versionId, originalDate=$originalDate, movedTo=$movedTo, kind=$kind, at=$at]"
}
