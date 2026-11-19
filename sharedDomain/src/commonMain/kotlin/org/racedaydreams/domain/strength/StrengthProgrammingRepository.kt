package org.racedaydreams.domain.strength

import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.*

/** Authoring owns prescriptions/choices, never actual sets or a second execution engine. */
interface StrengthProgrammingRepository {
    enum class Owner {
        LEGACY,
        AUTHORED,
    }

    data class Reference(
        private val owner: Owner?,
        private val id: String?,
        private val revision: Int,
    ) {
        init {
            if (owner == null) throw NullPointerException()
            GymWorkoutDefinition.requireId(id, "programme")
            require(revision >= 1) { "Invalid programme revision." }
        }

        fun owner(): Owner = owner!!

        fun id(): String = id!!

        fun revision(): Int = revision

        fun key(): String = "$id:v$revision"

        override fun toString(): String = "Reference[owner=$owner, id=$id, revision=$revision]"
    }

    data class Decision(
        private val id: String?,
        private val before: Reference?,
        private val after: Reference?,
        private val at: Instant?,
        private val reason: String?,
        private var protectedWorkoutIds: List<String>?,
        private var substitutions: List<StrengthProgrammeEditor.Change>?,
    ) {
        init {
            GymWorkoutDefinition.requireId(id, "programme decision")
            if (at == null) throw NullPointerException()
            StrengthProgramme.text(reason, 2000, false, "decision reason")
            substitutions = Immutable.copyList(substitutions ?: throw NullPointerException())
            protectedWorkoutIds =
                Immutable.copyList(protectedWorkoutIds ?: throw NullPointerException())
            require(
                !(before == null && after == null) &&
                    (after == null || after.owner() == Owner.AUTHORED) &&
                    substitutions!!.size <= 312 * 12 &&
                    protectedWorkoutIds!!.size <= 312 &&
                    protectedWorkoutIds!!.toSet().size == protectedWorkoutIds!!.size
            ) {
                "Invalid programme decision."
            }
            require(
                substitutions!!.isEmpty() ||
                    before != null &&
                        after != null &&
                        before.owner() == Owner.AUTHORED &&
                        before.id() == after.id() &&
                        after.revision() == before.revision() + 1
            ) {
                "Substitutions need an explicit forward revision."
            }
        }

        fun id(): String = id!!

        fun before(): Reference? = before

        fun after(): Reference? = after

        fun at(): Instant = at!!

        fun reason(): String = reason!!

        fun protectedWorkoutIds(): List<String> = protectedWorkoutIds!!

        fun substitutions(): List<StrengthProgrammeEditor.Change> = substitutions!!

        override fun toString(): String =
            "Decision[id=$id, before=$before, after=$after, at=$at, reason=$reason, protectedWorkoutIds=$protectedWorkoutIds, substitutions=$substitutions]"
    }

    /** One read-transaction snapshot. Acceptance must still recheck fresh completions. */
    data class RevisionReview(
        private val programme: StrengthProgramme?,
        private var completedWorkoutIds: Set<String>?,
    ) {
        init {
            if (programme == null) throw NullPointerException()
            completedWorkoutIds =
                Immutable.copySet(completedWorkoutIds ?: throw NullPointerException())
            require(
                programme.workouts().map { it.id() }.toSet().containsAll(completedWorkoutIds!!)
            ) {
                "Completed evidence references an unknown workout."
            }
        }

        fun programme(): StrengthProgramme = programme!!

        fun completedWorkoutIds(): Set<String> = completedWorkoutIds!!

        fun reference(): Reference =
            Reference(Owner.AUTHORED, programme!!.id(), programme.revision())

        override fun toString(): String =
            "RevisionReview[programme=$programme, completedWorkoutIds=$completedWorkoutIds]"
    }

    data class Summary(
        private val id: String?,
        private val revision: Int,
        private val name: String?,
        private val start: LocalDate?,
        private val end: LocalDate?,
    ) {
        fun id(): String? = id

        fun revision(): Int = revision

        fun name(): String? = name

        fun start(): LocalDate? = start

        fun end(): LocalDate? = end

        override fun toString(): String =
            "Summary[id=$id, revision=$revision, name=$name, start=$start, end=$end]"
    }

    data class DraftSummary(
        private val id: String?,
        private val revision: Int,
        private val name: String?,
        private val start: LocalDate?,
    ) {
        fun id(): String? = id

        fun revision(): Int = revision

        fun name(): String? = name

        fun start(): LocalDate? = start

        override fun toString(): String =
            "DraftSummary[id=$id, revision=$revision, name=$name, start=$start]"
    }

    fun currentReference(): Reference?

    fun programme(id: String, revision: Int): StrengthProgramme?

    fun reviewRevision(reference: Reference): RevisionReview

    fun versions(limit: Int, offset: Int): List<Summary>

    fun draft(id: String): StrengthProgrammeDraft?

    fun drafts(limit: Int, offset: Int): List<DraftSummary>

    fun saveDraft(draft: StrengthProgrammeDraft, expectedRevision: Int)

    fun deleteDraft(id: String, expectedRevision: Int)

    fun customExercise(id: String): CustomStrengthExercise?

    fun customExercises(): List<CustomStrengthExercise>

    fun saveCustom(exercise: CustomStrengthExercise, expected: CustomStrengthExercise?)

    /** Compare-and-activate; fresh completed evidence is checked inside the write transaction. */
    fun activate(
        proposed: StrengthProgramme,
        expected: Reference?,
        decisionId: String,
        substitutions: List<StrengthProgrammeEditor.Change>,
    ): Decision

    /** Confirm the exact reviewed draft AND pointer in the same transaction. */
    fun activateDraft(
        proposed: StrengthProgramme,
        expected: Reference?,
        decisionId: String,
        reviewed: StrengthProgrammeDraft,
    ): Decision

    fun stop(expected: Reference, decisionId: String): Decision

    fun decisions(): List<Decision>
}
