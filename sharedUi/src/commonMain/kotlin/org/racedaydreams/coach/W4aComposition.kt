package org.racedaydreams.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * W4a: shared presentation pieces for recorded workouts (activity history and run detail). Layout
 * only; every value is supplied by the caller from existing records and formatters. Public because
 * the Android run-detail adapter lives in the app module; candidates for promotion into the shared
 * expressive design system.
 */

/** A discipline's glyph in a filled disc, decorative and hidden from screen readers. */
@Composable
fun DisciplineGlyphDisc(discipline: Discipline, glyph: Glyph, size: Dp = 40.dp) {
    Box(
        Modifier.size(size)
            .clip(CircleShape)
            .background(discipline.container())
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, discipline.onContainer(), size * 0.55f)
    }
}

/**
 * One recorded figure led by its value: a small hue dot and label, then the value in a bold, large
 * style. The dot marks which measure it is (distance, time, pace, heart rate); it never means a
 * result is good or bad. Label and value are read together.
 */
@Composable
fun MeasureFigure(
    label: String,
    value: String,
    hue: ChartHue,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    val tones = chartAccent(hue)
    Column(
        modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(8.dp)
                    .clip(CircleShape)
                    .background(tones.strong)
                    .clearAndSetSemantics {}
            )
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            value,
            style =
                if (large) MaterialTheme.typography.displaySmall
                else MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Figures side by side with equal width; stacked at 150% text and above so nothing truncates. */
@Composable
fun MeasureFigureRow(figures: List<Triple<String, String, ChartHue>>) {
    if (figures.isEmpty()) return
    if (LocalDensity.current.fontScale >= 1.5f)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            figures.forEach { (label, value, hue) -> MeasureFigure(label, value, hue) }
        }
    else
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            figures.forEach { (label, value, hue) ->
                MeasureFigure(label, value, hue, Modifier.weight(1f))
            }
        }
}

/** A short, plain-text tag such as "Fastest" beside a figure; meaning is in the words. */
@Composable
fun FigureTag(text: String, hue: ChartHue) {
    val tones = chartAccent(hue)
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
                .background(tones.soft.copy(alpha = 0.22f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/**
 * A borderless, tappable history row: a discipline glyph disc, the row's title and figures, and a
 * chevron. The whole row is one target (at least 64 dp) and is read as one item.
 */
@Composable
fun RecordedWorkoutRow(
    discipline: Discipline,
    glyph: Glyph,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DisciplineGlyphDisc(discipline, glyph)
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content,
            )
            Box(Modifier.align(Alignment.CenterVertically)) {
                GlyphIcon(Glyph.CHEVRON, MaterialTheme.colorScheme.onSurfaceVariant, 18.dp)
            }
        }
    }
}

/** A non-interactive history row (for example a strength workout listed without a detail link). */
@Composable
fun RecordedWorkoutSummaryRow(
    discipline: Discipline,
    glyph: Glyph,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp).semantics(
                mergeDescendants = true
            ) {},
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DisciplineGlyphDisc(discipline, glyph)
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content,
            )
        }
    }
}

/** A history group's date: a small bold heading (for screen readers too) above its rows. */
@Composable
fun HistoryDateLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 8.dp, start = 4.dp).semantics { heading() },
    )
}

/** Secondary row text: smaller and in the variant colour. */
@Composable
fun RowDetail(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
}
