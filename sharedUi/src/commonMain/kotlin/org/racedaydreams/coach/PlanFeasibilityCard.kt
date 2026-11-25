package org.racedaydreams.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.coaching.PlanFeasibility

/**
 * Race-readiness notice for the Plan screen (training Piece 1). Shown only when the plan gives
 * limited or no realistic preparation for the chosen race; a supported plan shows nothing extra.
 * Meaning is carried by the text, not the container colour.
 */
@Composable
internal fun PlanFeasibilityCard(
    assessment: PlanFeasibility.Assessment,
    editGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (assessment.level() == PlanFeasibility.Level.SUPPORTED) return
    var open by rememberSaveable { mutableStateOf(false) }
    val serious = assessment.level() == PlanFeasibility.Level.NOT_SUPPORTED
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth().testTag("plan-feasibility"),
        colors =
            CardDefaults.cardColors(
                containerColor = if (serious) scheme.errorContainer else scheme.secondaryContainer,
                contentColor = if (serious) scheme.onErrorContainer else scheme.onSecondaryContainer,
            ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (serious) "Race readiness" else "Preparation check",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                assessment.headline(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            assessment.reasons().firstOrNull()?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            if (open) {
                assessment.reasons().drop(1).forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                if (assessment.alternatives().isNotEmpty()) {
                    Text(
                        "What you can do",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp).semantics { heading() },
                    )
                    assessment.alternatives().forEach {
                        Text("• $it", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(assessment.caveat(), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = editGoal, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Review goal and routine")
                }
            }
            TextButton(
                onClick = { open = !open },
                modifier =
                    Modifier.heightIn(min = 48.dp).semantics {
                        stateDescription = if (open) "Expanded" else "Collapsed"
                    },
            ) {
                Text(if (open) "Hide details" else "Why and your options")
            }
        }
    }
}
