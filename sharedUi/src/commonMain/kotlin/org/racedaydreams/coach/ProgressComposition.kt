@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

/*
 * W3 — Progress (Running, Overview) expressive composition. Presentation only: these group the
 * existing Progress destinations; no destination, route or figure is added or changed.
 */

/** A labelled group of Progress drill-downs: what it is about, its glyph and its measure hue. */
internal enum class ProgressLinkGroup(val title: String, val glyph: Glyph, val hue: ChartHue) {
    RUNNING("Running detail", Glyph.RUN, ChartHue.BLUE),
    BODY("Milestones & body", Glyph.BODY, ChartHue.TEAL),
    STRENGTH("Strength detail", Glyph.DUMBBELL, ChartHue.PLUM),
}

/**
 * Progress drill-downs as glyph-labelled, borderless [ActionGroup]s (one per group) instead of one
 * outlined card of stacked links. Empty groups are left out by the caller. Each row keeps the
 * destination's existing title and summary.
 */
@Composable
internal fun ProgressDrillDowns(
    groups: List<Pair<ProgressLinkGroup, List<ProgressLink>>>,
    open: (ProgressLink) -> Unit,
) {
    if (groups.isEmpty()) return
    Column(
        Modifier.testTag("progress-drill-downs"),
        verticalArrangement = Arrangement.spacedBy(CoachSpace.Card),
    ) {
        for ((group, links) in groups) ActionGroup(
            group.title,
            glyph = group.glyph,
            hue = group.hue,
        ) {
            for (link in links) ActionRow(link.title, link.summary) { open(link) }
        }
    }
}

/**
 * Single-choice tonal pills (W3): borderless capsules, the selected one filled with
 * [selectedContainer] and marked with a checkmark so the choice never relies on colour alone. Below
 * 150% text, when every pill's natural width fits the row, they share ONE row (each weighted by its
 * natural width, so no label is ever squeezed); otherwise, and always at 150%+ text, they wrap at
 * natural width. Every pill is at least 48 dp tall and exposes selection.
 */
@Composable
internal fun <T> TonalPills(
    options: List<Pair<T, String>>,
    selected: T,
    choose: (T) -> Unit,
    selectedContainer: Color,
    selectedContent: Color,
    unselectedContainer: Color,
    unselectedContent: Color,
    tag: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    val density = LocalDensity.current
    val gap = 6.dp
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val labels =
            options.map { (_, label) ->
                with(density) { measurer.measure(label, style).size.width.toDp() }
            }
        val check = with(density) { measurer.measure("✓", style).size.width.toDp() } + PillCheckGap
        fun natural(index: Int) =
            labels[index] + PillPadding * 2 + if (options[index].first == selected) check else 0.dp
        // Reserve the checkmark once, so changing the selection never changes the layout mode.
        val needed =
            labels.fold(0.dp) { sum, it -> sum + it + PillPadding * 2 } +
                check +
                gap * (options.size - 1) +
                2.dp
        val oneRow = density.fontScale < 1.5f && needed <= maxWidth
        val pill: @Composable (Int, Modifier) -> Unit = { index, pillModifier ->
            val (value, label) = options[index]
            TonalPill(
                label,
                value == selected,
                { choose(value) },
                if (value == selected) selectedContainer else unselectedContainer,
                if (value == selected) selectedContent else unselectedContent,
                pillModifier.testTag(tag(value)),
            )
        }
        if (oneRow)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                options.indices.forEach { index ->
                    pill(index, Modifier.weight(natural(index).value))
                }
            }
        else
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                options.indices.forEach { index -> pill(index, Modifier) }
            }
    }
}

private val PillPadding = 10.dp
private val PillCheckGap = 4.dp

@Composable
private fun TonalPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    modifier: Modifier,
) {
    Surface(
        color = container,
        contentColor = content,
        shape = CircleShape,
        modifier =
            modifier
                .heightIn(min = 48.dp)
                .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        Row(
            Modifier.heightIn(min = 48.dp).padding(horizontal = PillPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (selected) {
                Text(
                    "✓",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clearAndSetSemantics {},
                )
                Spacer(Modifier.width(PillCheckGap))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
