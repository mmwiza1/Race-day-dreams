package org.racedaydreams.domain

import org.racedaydreams.domain.time.*

/** Original evidence/choice contract, shared without changing validation or history. */
data class PlanAdjustment(
    private var id: String?,
    private var planId: String?,
    private val proposedAt: Instant?,
    private var recommendation: String?,
    private var reason: String?,
    private var options: List<AdjustmentOption>?,
    private val selectedOptionId: String?,
) {
    init {

        id = DomainChecks.id(id, "adjustment id")
        planId = DomainChecks.id(planId, "plan id")
        if (proposedAt == null) throw NullPointerException("proposedAt")
        recommendation = DomainChecks.id(recommendation, "recommendation")
        reason = DomainChecks.id(reason, "recommendation reason")
        options = DomainChecks.immutableList(options)
        require(
            options!!.size >= 2 &&
                options!!.any { it.action() == AdjustmentOption.Action.KEEP_ORIGINAL }
        ) {
            "Adjustments must offer choices including keeping the original"
        }
        val ids = mutableSetOf<String>()
        for (option in options!!) require(ids.add(option.id())) { "Option ids must be unique" }
        require(selectedOptionId == null || selectedOptionId in ids) {
            "Selected option is not among the offered choices"
        }
    }

    fun id(): String = id!!

    fun planId(): String = planId!!

    fun proposedAt(): Instant = proposedAt!!

    fun recommendation(): String = recommendation!!

    fun reason(): String = reason!!

    fun options(): List<AdjustmentOption> = options!!

    fun selectedOptionId(): String? = selectedOptionId

    override fun toString(): String =
        "PlanAdjustment[id=$id, planId=$planId, proposedAt=$proposedAt, recommendation=$recommendation, reason=$reason, options=$options, selectedOptionId=$selectedOptionId]"
}
