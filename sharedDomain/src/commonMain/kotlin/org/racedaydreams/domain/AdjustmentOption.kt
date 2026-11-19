package org.racedaydreams.domain

/** Offered choice, never a command; keeping the original remains an explicit option. */
data class AdjustmentOption(
    private var id: String?,
    private var label: String?,
    private val action: Action?,
    private var proposedWorkouts: List<PlannedWorkout>?,
    private var explanation: String?,
) {
    enum class Action {
        ACCEPT_RECOMMENDATION,
        KEEP_ORIGINAL,
        MOVE_WORKOUT,
        EASY_RUN,
        REST,
        CUSTOMISE,
    }

    init {
        id = DomainChecks.id(id, "option id")
        label = DomainChecks.id(label, "option label")
        require(action != null) { "Option action is required" }
        proposedWorkouts = DomainChecks.immutableList(proposedWorkouts)
        explanation = DomainChecks.id(explanation, "option explanation")
    }

    fun id(): String = id!!

    fun label(): String = label!!

    fun action(): Action = action!!

    fun proposedWorkouts(): List<PlannedWorkout> = proposedWorkouts!!

    fun explanation(): String = explanation!!

    override fun toString(): String =
        "AdjustmentOption[id=$id, label=$label, action=$action, proposedWorkouts=$proposedWorkouts, explanation=$explanation]"
}
