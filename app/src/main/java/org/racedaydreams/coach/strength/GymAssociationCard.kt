package org.racedaydreams.coach.strength

import androidx.compose.runtime.Composable
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.racedaydreams.domain.strength.GymAssociationCoordinator
import org.racedaydreams.domain.strength.GymWorkoutAssociation

/** Original UK presentation adapter; choices still enter the one reviewed IO protocol. */
@Composable
fun GymAssociationCard(
    view: GymAssociationCoordinator.View?,
    enabled: Boolean,
    onChoose: (GymWorkoutAssociation.Proposal, GymWorkoutAssociation.Choice) -> Unit,
    onRefresh: () -> Unit,
) =
    SharedGymAssociationCard(
        view = view,
        enabled = enabled,
        onChoose = onChoose,
        onRefresh = onRefresh,
        formatDate = { it.format(ASSOCIATION_DAY) },
        formatTarget = ::gymTargetDescription,
    )

private val ASSOCIATION_DAY = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.UK)
