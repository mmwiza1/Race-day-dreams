package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.CoachSpace
import org.racedaydreams.coach.Panel

/** The selected exercise, its most recent performance and any actions for that exercise. */
@Composable
fun StrengthExerciseHeader(
    exercise: String,
    previous: String?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Panel {
        Text(
            exercise,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
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
        if (load != null && maxWidth >= 280.dp && LocalDensity.current.fontScale < 1.5f) {
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

/** Compact evidence row. The caller labels the group according to its persistence state. */
@Composable
fun StrengthSetRow(
    number: Int,
    value: String,
    detail: String? = null,
    editing: Boolean = false,
    actions: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (editing) scheme.primaryContainer else scheme.surfaceContainerHigh,
        contentColor = if (editing) scheme.onPrimaryContainer else scheme.onSurface,
    ) {
        Row(
            Modifier.heightIn(min = CoachSpace.Touch)
                .padding(
                    start = CoachSpace.Card,
                    end = if (actions == null) CoachSpace.Card else 0.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(vertical = CoachSpace.Related)) {
                Text("$number: $value", style = MaterialTheme.typography.bodyLarge)
                detail?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
            }
            actions?.invoke()
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
