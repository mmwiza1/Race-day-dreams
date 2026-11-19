package org.racedaydreams.domain.coaching

import org.racedaydreams.domain.*
import org.racedaydreams.domain.time.*

/**
 * Retained prescription/response snapshot; actual activity and explicit choices remain separate.
 */
data class PlanVersion(
    private val id: String?,
    private val planId: String?,
    private val version: Int,
    private val createdAt: Instant?,
    private val reason: String?,
    private val goalSnapshot: UserProfile?,
    private val evidenceSnapshot: String?,
    private val plan: TrainingPlan?,
) {

    init {
        require(
            id != null &&
                id.trim { it <= ' ' }.isNotEmpty() &&
                planId != null &&
                planId.trim { it <= ' ' }.isNotEmpty() &&
                version >= 1
        ) {
            "Check plan identity/version."
        }
        if (
            createdAt == null ||
                reason == null ||
                goalSnapshot == null ||
                evidenceSnapshot == null ||
                plan == null
        )
            throw NullPointerException()
        require(planId == plan.id()) { "Plan identity differs from version identity." }
    }

    fun id(): String = id!!

    fun planId(): String = planId!!

    fun version(): Int = version

    fun createdAt(): Instant = createdAt!!

    fun reason(): String = reason!!

    fun goalSnapshot(): UserProfile = goalSnapshot!!

    fun evidenceSnapshot(): String = evidenceSnapshot!!

    fun plan(): TrainingPlan = plan!!

    override fun toString(): String =
        "PlanVersion[id=$id, planId=$planId, version=$version, createdAt=$createdAt, reason=$reason, goalSnapshot=$goalSnapshot, evidenceSnapshot=$evidenceSnapshot, plan=$plan]"
}
