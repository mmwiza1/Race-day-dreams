package org.racedaydreams.domain.strength

import org.racedaydreams.domain.coaching.Immutable

/** A saved, unaccepted concrete preview. It preserves dated variation and all explanations. */
data class ConcreteStrengthProgrammeDraft(
    private val id: String?,
    private val revision: Int,
    private val before: StrengthProgrammingRepository.Reference?,
    private val programme: StrengthProgramme?,
    private var substitutions: List<StrengthProgrammeEditor.Change>?,
) {
    init {
        GymWorkoutDefinition.requireId(id, "concrete programme draft")
        require(revision >= 1) { "Invalid concrete draft revision." }
        if (programme == null) throw NullPointerException()
        substitutions = Immutable.copyList(substitutions ?: throw NullPointerException())
        val forward =
            before != null &&
                before.owner() == StrengthProgrammingRepository.Owner.AUTHORED &&
                before.id() == programme.id() &&
                before.revision() == programme.parentRevision()
        require(programme.parentRevision() <= 0 || forward) {
            "A revision draft must identify its preserved base."
        }
        require(
            !(programme.parentRevision() == 0 && before != null && before.id() == programme.id())
        ) {
            "A replacement programme needs a distinct identity."
        }
        require(substitutions!!.isEmpty() || forward) {
            "Permanent substitutions need a forward revision."
        }
        require(substitutions!!.size <= 312 * 12) { "Too many substitution decisions." }
        val changed = mutableSetOf<List<String>>()
        for (change in substitutions!!) {
            require(changed.add(listOf(change.workoutId(), change.slotId()))) {
                "Review the final substitution for each slot."
            }
        }
        for (workout in programme.workouts()) for (slot in workout.definition().slots()) {
            require(slot.target() != null) { "Review a target for every programme exercise." }
        }
    }

    fun id(): String = id!!

    fun revision(): Int = revision

    fun before(): StrengthProgrammingRepository.Reference? = before

    fun programme(): StrengthProgramme = programme!!

    fun substitutions(): List<StrengthProgrammeEditor.Change> = substitutions!!

    /** Structural validation never infers completion; acceptance independently checks actuals. */
    fun validateAgainst(preserved: StrengthProgramme) {
        require(
            before != null &&
                before.owner() == StrengthProgrammingRepository.Owner.AUTHORED &&
                before.id() == preserved.id() &&
                before.revision() == preserved.revision() &&
                programme().parentRevision() != 0
        ) {
            "Review the preserved base of this revision."
        }
        val proposed = programme().workouts().associateBy { it.id() }
        val unchanged =
            preserved.workouts().filter { it == proposed[it.id()] }.map { it.id() }.toSet()
        StrengthProgrammeEditor.Proposal(preserved, programme(), unchanged, substitutions())
    }

    /** Retain programme lineage while saving a new athlete-edited draft revision. */
    fun withProgramme(
        replacement: StrengthProgramme,
        changes: List<StrengthProgrammeEditor.Change>?,
    ): ConcreteStrengthProgrammeDraft {
        val current = programme()
        require(
            current.id() == replacement.id() &&
                current.revision() == replacement.revision() &&
                current.parentRevision() == replacement.parentRevision() &&
                !replacement.createdAt().isBefore(current.createdAt())
        ) {
            "Draft edits must retain programme identity and lineage."
        }
        return ConcreteStrengthProgrammeDraft(
            id,
            nextConcreteRevision(revision),
            before,
            replacement,
            changes,
        )
    }

    override fun toString(): String =
        "ConcreteStrengthProgrammeDraft[id=$id, revision=$revision, before=$before, programme=$programme, substitutions=$substitutions]"
}

/**
 * Match the original positive revision's Math.addExact, rather than wrapping on either platform.
 */
internal fun nextConcreteRevision(revision: Int): Int {
    if (revision == Int.MAX_VALUE) throw ArithmeticException("integer overflow")
    return revision + 1
}
