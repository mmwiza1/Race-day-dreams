package org.racedaydreams.domain.strength

import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.Instant

/** Existing reviewed-choice protocol. The calling application serializes operations on IO. */
class GymAssociationCoordinator(
    repository: GymAssociationRepository?,
    clock: TimeSource?,
    ids: IdSource?,
) {
    fun interface TimeSource {
        fun instant(): Instant
    }

    fun interface IdSource {
        fun get(): String?
    }

    data class View(
        private val actualId: String?,
        private var proposals: List<GymWorkoutAssociation.Proposal>?,
        private val current: GymWorkoutAssociation.Decision?,
        private val currentReview: GymWorkoutAssociation.Proposal?,
        private val pending: GymWorkoutAssociation.Decision?,
    ) {
        init {
            proposals = Immutable.copyList(proposals ?: throw NullPointerException())
        }

        fun actualId(): String? = actualId

        fun proposals(): List<GymWorkoutAssociation.Proposal> = proposals!!

        fun current(): GymWorkoutAssociation.Decision? = current

        fun currentReview(): GymWorkoutAssociation.Proposal? = currentReview

        fun pending(): GymWorkoutAssociation.Decision? = pending

        override fun toString(): String =
            "View[actualId=$actualId, proposals=$proposals, current=$current, currentReview=$currentReview, pending=$pending]"
    }

    private val repository = repository ?: throw NullPointerException()
    private val clock = clock ?: throw NullPointerException()
    private val ids = ids ?: throw NullPointerException()
    private var view = View(null, Immutable.list(), null, null, null)

    fun view(): View = view

    fun review(actualId: String) {
        require(view.pending() == null) {
            "Retry the pending choice or explicitly reload saved evidence first."
        }
        load(actualId)
    }

    private fun load(actualId: String) {
        val at = clock.instant()
        val current = repository.current(actualId)
        val currentReview = if (current == null) null else repository.reviewCurrent(actualId, at)
        val proposals = repository.proposals(actualId, at)
        view = View(actualId, proposals, current, currentReview, null)
    }

    fun choose(proposal: GymWorkoutAssociation.Proposal?, choice: GymWorkoutAssociation.Choice?) {
        require(view.pending() == null) { "Retry the pending choice before making another." }
        val listed = proposal in view.proposals()
        val reversing =
            choice == GymWorkoutAssociation.Choice.KEEP_EXTRA && view.currentReview() == proposal
        require(listed || reversing) { "Review the current workout and target before choosing." }
        val decision = GymWorkoutAssociation.Decision(ids.get(), proposal, choice, clock.instant())
        view =
            View(view.actualId(), view.proposals(), view.current(), view.currentReview(), decision)
        retry()
    }

    fun retry() {
        val pending =
            view.pending()
                ?: throw IllegalArgumentException("There is no pending association choice.")
        val accepted = repository.decide(pending)
        require(pending == accepted) { "Saved choice did not match the reviewed action." }
        // If the post-commit read fails, retain the exact pending decision for an idempotent retry.
        load(pending.proposal().actual().id())
    }

    /** Abandon an unacknowledged request only after durable evidence is successfully reloaded. */
    fun reloadSaved() {
        val id =
            view.actualId() ?: throw IllegalArgumentException("Choose a completed workout first.")
        load(id)
    }

    fun clear() {
        require(view.pending() == null) {
            "Review the pending association before starting another workout."
        }
        view = View(null, Immutable.list(), null, null, null)
    }
}
