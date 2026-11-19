package org.racedaydreams.domain.coaching

import org.racedaydreams.domain.strength.*

/** Platform storage must preserve immutable versions and atomically commit explicit decisions. */
interface CoachingRepository {
    fun current(): PlanVersion?

    fun versions(): List<PlanVersion>

    fun exercises(): List<StrengthExercise>

    fun strength(): List<StrengthSession>

    fun responses(): List<RunExecution>

    fun proposals(): List<AdjustmentProposal>

    fun proposalKnown(id: String): Boolean

    fun saveCurrent(version: PlanVersion)

    fun saveExercise(exercise: StrengthExercise)

    fun saveStrength(session: StrengthSession)

    fun deleteStrength(id: String)

    fun saveResponse(response: RunExecution)

    fun saveFeedback(activityId: String, versionId: String?, feedback: RunFeedback)

    fun saveProposal(proposal: AdjustmentProposal)

    fun decide(proposal: AdjustmentProposal, optionId: String, completedWorkoutIds: Set<String>)

    fun transaction(action: CoachingTransaction)
}
