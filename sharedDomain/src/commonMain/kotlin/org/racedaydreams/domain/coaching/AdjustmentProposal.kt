package org.racedaydreams.domain.coaching

import org.racedaydreams.domain.*
import org.racedaydreams.domain.time.*

/**
 * Retained prescription/response snapshot; actual activity and explicit choices remain separate.
 */
data class AdjustmentProposal(
    private val id: String?,
    private val baseVersionId: String?,
    private val workoutId: String?,
    private val createdAt: Instant?,
    private val reason: String?,
    private var evidence: List<String>?,
    private val impact: String?,
    private val confidence: String?,
    private var options: List<Option>?,
) {
    enum class Action {
        KEEP,
        REPLACE,
        REST,
    }

    data class Option(
        private val id: String?,
        private val label: String?,
        private val action: Action?,
        private val replacement: PlannedWorkout?,
    ) {
        init {
            if (id == null || label == null || action == null) throw NullPointerException()
            require((action == Action.REPLACE) == (replacement != null)) {
                "Replacement does not match its action."
            }
        }

        fun id(): String = id!!

        fun label(): String = label!!

        fun action(): Action = action!!

        fun replacement(): PlannedWorkout? = replacement

        override fun toString(): String =
            "Option[id=$id, label=$label, action=$action, replacement=$replacement]"
    }

    init {
        require(
            id != null &&
                id.trim { it <= ' ' }.isNotEmpty() &&
                baseVersionId != null &&
                workoutId != null &&
                createdAt != null &&
                reason != null &&
                impact != null &&
                confidence != null
        ) {
            "Proposal identity, reasoning and timestamps are required."
        }
        val rows = options ?: throw NullPointerException()
        val ids = mutableSetOf<String>()
        for (option in rows) {
            val replacement = option.replacement()
            require(
                ids.add(option.id()) && (replacement == null || replacement.id() == workoutId)
            ) {
                "Proposal alternatives need unique IDs and must preserve workout identity."
            }
        }
        require(rows.any { it.action() == Action.KEEP }) {
            "The runner must be allowed to keep the original."
        }
        evidence = Immutable.copyList(evidence ?: throw NullPointerException())
        options = Immutable.copyList(rows)
    }

    fun id(): String = id!!

    fun baseVersionId(): String = baseVersionId!!

    fun workoutId(): String = workoutId!!

    fun createdAt(): Instant = createdAt!!

    fun reason(): String = reason!!

    fun evidence(): List<String> = evidence!!

    fun impact(): String = impact!!

    fun confidence(): String = confidence!!

    fun options(): List<Option> = options!!

    override fun toString(): String =
        "AdjustmentProposal[id=$id, baseVersionId=$baseVersionId, workoutId=$workoutId, createdAt=$createdAt, reason=$reason, evidence=$evidence, impact=$impact, confidence=$confidence, options=$options]"
}
