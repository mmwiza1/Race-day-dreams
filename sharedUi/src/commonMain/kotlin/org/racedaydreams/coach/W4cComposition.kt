package org.racedaydreams.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * W4c composition helpers (race day, recovery, milestones, bouldering): small presentation pieces
 * built only from the shared expressive design system. Candidates for promotion to the shared
 * layout once another screen needs them.
 */

/** One stage of a [StageTimeline]: a range title, an optional strong figure and guidance. */
internal class TimelineStage(val title: String, val figure: String?, val detail: String?)

/**
 * Stages as a vertical timeline: a numbered accent disc per stage joined by a quiet rail, so the
 * order of the race reads at a glance. Numbers and the rail are decorative; the text carries the
 * meaning and each stage is read as one item.
 */
@Composable
internal fun StageTimeline(stages: List<TimelineStage>, discipline: Discipline) {
    val rail = MaterialTheme.colorScheme.outlineVariant
    Column {
        stages.forEachIndexed { index, stage ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min).semantics(
                    mergeDescendants = true
                ) {},
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(
                    Modifier.width(32.dp).fillMaxHeight().clearAndSetSemantics {},
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(discipline.accent()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = discipline.onAccent(),
                        )
                    }
                    if (index < stages.lastIndex)
                        Box(Modifier.width(2.dp).weight(1f).background(rail))
                }
                Column(
                    Modifier.weight(1f)
                        .padding(bottom = if (index < stages.lastIndex) 18.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        stage.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    stage.figure?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    stage.detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }
}

/** A clean, full-width checkbox row (at least 48 dp) for checklists and options in a panel. */
@Composable
internal fun CheckRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked, onCheckedChange = null, enabled = enabled)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

/** A quiet supporting sentence (disclaimers, captions) in the receding text colour. */
@Composable
internal fun QuietNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** A decorative discipline badge: the discipline's glyph on its container colour. */
@Composable
internal fun DisciplineBadge(glyph: Glyph, discipline: Discipline, size: Dp = 40.dp) {
    Box(
        Modifier.size(size)
            .clip(CircleShape)
            .background(discipline.container())
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, discipline.accent(), size * 0.5f)
    }
}
