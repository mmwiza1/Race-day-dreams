package org.racedaydreams.coach

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/** Shared 4/8dp rhythm. Keep text and control heights intrinsic at large font sizes. */
object CoachSpace {
    val Tiny = 4.dp
    val Chip = 8.dp
    val Related = 8.dp
    val Card = 12.dp
    val Inset = 16.dp
    val Screen = 16.dp
    val Section = 24.dp
    val Touch = 48.dp
}

/** Official selectable chip; selection is both semantic and visible without colour. */
@Composable
fun CoachFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier.heightIn(min = CoachSpace.Touch),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        leadingIcon =
            if (selected) {
                { Text("✓", modifier = Modifier.clearAndSetSemantics {}) }
            } else null,
    )
}

/** Scrollable Material tabs retain full labels at 200% without a cramped segmented grid. */
@Composable
fun CoachSectionTabs(options: List<String>, selected: String, choose: (String) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = options.indexOf(selected).coerceAtLeast(0),
        edgePadding = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        divider = {},
    ) {
        options.forEach { option ->
            Tab(
                selected = selected == option,
                onClick = { choose(option) },
                modifier = Modifier.heightIn(min = CoachSpace.Touch),
                text = { Text(option) },
            )
        }
    }
}
