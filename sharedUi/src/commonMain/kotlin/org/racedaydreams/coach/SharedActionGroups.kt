@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Low-emphasis navigation: plain rows on one borderless tonal panel instead of a stack of buttons,
 * so secondary destinations stay one tap away without competing with the screen's primary action.
 * With a glyph, the title becomes an expressive section label in that hue.
 */
@Composable
fun ActionGroup(
    title: String?,
    emphasised: Boolean = false,
    glyph: Glyph? = null,
    hue: ChartHue = ChartHue.BLUE,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CoachSpace.Related)) {
        if (title != null && glyph != null) SectionLabel(title, glyph, hue)
        else if (title != null)
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
        Surface(
            shape = RoundedCornerShape(28.dp),
            color =
                if (emphasised) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 8.dp), content = content)
        }
    }
}

/** A full-width row: title, optional one-line context and a chevron. At least 56dp tall. */
@Composable
fun ActionRow(
    title: String,
    supporting: String? = null,
    value: String? = null,
    tag: String? = null,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().let { if (tag != null) it.testTag(tag) else it },
    ) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (supporting != null)
                    Text(
                        supporting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
            }
            if (value != null) Text(value, style = MaterialTheme.typography.titleMedium)
            Text(
                "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

/**
 * Two equal secondary actions side by side (stacked at very large text), for spontaneous training
 * that must stay reachable without competing with the day's planned primary action.
 */
@Composable
fun QuickStartRow(actions: List<Pair<String, () -> Unit>>) {
    if (actions.isEmpty()) return
    val stacked = LocalDensity.current.fontScale >= 1.5f || actions.size == 1
    if (stacked)
        Column(verticalArrangement = Arrangement.spacedBy(CoachSpace.Related)) {
            actions.forEach { (label, action) ->
                FilledTonalButton(action, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(label)
                }
            }
        }
    else
        Row(horizontalArrangement = Arrangement.spacedBy(CoachSpace.Card)) {
            actions.forEach { (label, action) ->
                FilledTonalButton(action, Modifier.weight(1f).heightIn(min = 56.dp)) { Text(label) }
            }
        }
}

/**
 * A collapsible group of settings or detail. The header states what is inside, so people can find a
 * control without reading every disclosure; content is composed only when expanded.
 */
@Composable
fun ExpandableSection(
    title: String,
    summary: String,
    tag: String,
    initiallyExpanded: Boolean = false,
    lastOpened: MutableState<String?>? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // [lastOpened] is held by the host for the current app session only (never saved to disk),
    // so returning to the screen reopens the section the person last opened.
    var expanded by
        rememberSaveable(tag) { mutableStateOf(initiallyExpanded || lastOpened?.value == tag) }
    Column(verticalArrangement = Arrangement.spacedBy(CoachSpace.Card)) {
        Surface(
            onClick = {
                expanded = !expanded
                lastOpened?.let {
                    if (expanded) it.value = tag else if (it.value == tag) it.value = null
                }
            },
            shape = MaterialTheme.shapes.large,
            color =
                if (expanded) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor =
                if (expanded) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier.fillMaxWidth().testTag(tag).semantics {
                    stateDescription = if (expanded) "Expanded" else "Collapsed"
                },
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(summary, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    if (expanded) "▴" else "▾",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
        if (expanded)
            Column(
                Modifier.fillMaxWidth().padding(start = 8.dp),
                verticalArrangement = Arrangement.spacedBy(CoachSpace.Card),
                content = content,
            )
    }
}

/** Short facts on one wrapping line, e.g. "12.8 km · Light effort · 6:10–6:40/km". */
@Composable
fun MetaLine(items: List<String>) {
    if (items.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0)
                Text(
                    "·",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            Text(item, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
