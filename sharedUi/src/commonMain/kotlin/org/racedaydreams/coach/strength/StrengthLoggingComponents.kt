package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.ActivityHeading
import org.racedaydreams.coach.CoachSpace
import org.racedaydreams.coach.Glyph

/** The selected exercise, its most recent performance and any actions for that exercise. */
@Composable
fun StrengthExerciseHeader(
    exercise: String,
    previous: String?,
    progress: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CoachSpace.Related)) {
        ActivityHeading(exercise, Glyph.DUMBBELL, context = progress)
        if (previous != null) {
            Column(verticalArrangement = Arrangement.spacedBy(CoachSpace.Tiny)) {
                Text("Last time", style = MaterialTheme.typography.labelMedium)
                Text(previous, style = MaterialTheme.typography.titleMedium)
            }
        }
        content()
    }
}

/** Fit adjacent inputs when their full labels have room. Keep both reachable with enlarged text. */
@Composable
fun StrengthSetFields(
    modifier: Modifier = Modifier,
    load: (@Composable (Modifier) -> Unit)?,
    amount: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (load != null && maxWidth >= 240.dp && LocalDensity.current.fontScale < 1.5f) {
            Row(horizontalArrangement = Arrangement.spacedBy(CoachSpace.Related)) {
                load(Modifier.weight(1f))
                amount(Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(CoachSpace.Related)) {
                load?.invoke(Modifier.fillMaxWidth())
                amount(Modifier.fillMaxWidth())
            }
        }
    }
}

/** A disclosure whose state can also be opened by editing an existing set. */
@Composable
fun StrengthDetailsToggle(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        modifier =
            Modifier.fillMaxWidth().semantics {
                role = Role.Button
                stateDescription = if (expanded) "Expanded" else "Collapsed"
            },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            Modifier.heightIn(min = CoachSpace.Touch).padding(CoachSpace.Card),
            horizontalArrangement = Arrangement.spacedBy(CoachSpace.Related),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            Text(if (expanded) "▴" else "▾", Modifier.clearAndSetSemantics {})
        }
    }
}
