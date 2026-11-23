package org.racedaydreams.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

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
