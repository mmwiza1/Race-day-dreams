package org.racedaydreams.domain.strength

import org.racedaydreams.domain.time.LocalDate

/** Typed proposal storage; saving never changes the current programme or creates actual sets. */
interface ConcreteStrengthProgrammeDraftRepository {
    data class Summary(
        private val id: String?,
        private val revision: Int,
        private val programmeId: String?,
        private val programmeRevision: Int,
        private val name: String?,
        private val start: LocalDate?,
        private val end: LocalDate?,
        private val revisionExists: Boolean,
    ) {
        fun id(): String? = id

        fun revision(): Int = revision

        fun programmeId(): String? = programmeId

        fun programmeRevision(): Int = programmeRevision

        fun name(): String? = name

        fun start(): LocalDate? = start

        fun end(): LocalDate? = end

        fun revisionExists(): Boolean = revisionExists

        override fun toString(): String =
            "Summary[id=$id, revision=$revision, programmeId=$programmeId, programmeRevision=$programmeRevision, name=$name, start=$start, end=$end, revisionExists=$revisionExists]"
    }

    fun get(id: String): ConcreteStrengthProgrammeDraft?

    fun list(limit: Int, offset: Int): List<Summary>

    fun save(draft: ConcreteStrengthProgrammeDraft, expectedRevision: Int)

    fun delete(id: String, expectedRevision: Int)

    fun activate(
        reviewed: ConcreteStrengthProgrammeDraft,
        decisionId: String,
    ): StrengthProgrammingRepository.Decision
}
