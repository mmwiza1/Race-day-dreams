package org.racedaydreams.domain.strength

import org.racedaydreams.domain.time.Instant

/** Completed evidence choices; UI success must follow the acknowledged transaction. */
interface GymAssociationRepository {
    fun proposals(actualId: String, reviewedAt: Instant): List<GymWorkoutAssociation.Proposal>

    fun current(actualId: String): GymWorkoutAssociation.Decision?

    fun reviewCurrent(actualId: String, reviewedAt: Instant): GymWorkoutAssociation.Proposal?

    fun decide(decision: GymWorkoutAssociation.Decision): GymWorkoutAssociation.Decision?
}
