package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/**
 * Existing first-run focus choice, as four equal choices rather than four filled buttons. Saving
 * and routing happen in the real host after success.
 */
@Composable
fun SharedStartingPath(
    record: () -> Unit,
    race: () -> Unit,
    strength: () -> Unit,
    hybrid: () -> Unit,
) = CoachPage {
    CoachHeading("Make today yours")
    Text(
        "Choose where to start. You can change it in Settings, and every feature stays available.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    StartingChoice(
        "Train for a race",
        "5K to marathon. A plan built around your goal and your week.",
        race,
    )
    StartingChoice(
        "Record my runs",
        "Run when you like. No plan, race or health access needed.",
        record,
    )
    StartingChoice(
        "Focus on strength",
        "Set up optional strength programming or log your own workouts. No race goal or running profile required.",
        strength,
    )
    StartingChoice(
        "Running + strength",
        "Start with your running goal and add gym days when you choose.",
        hybrid,
    )
}

@Composable
private fun StartingChoice(title: String, description: String, choose: () -> Unit) {
    Card(
        onClick = choose,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(CoachSpace.Inset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "›",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}
