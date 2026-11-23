package org.racedaydreams.coach.strength

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.ChartHue
import org.racedaydreams.coach.Discipline
import org.racedaydreams.coach.Eyebrow
import org.racedaydreams.coach.Glyph
import org.racedaydreams.coach.GlyphIcon
import org.racedaydreams.coach.accent
import org.racedaydreams.coach.chartAccent
import org.racedaydreams.coach.onAccent

/*
 * W4b gym composition: small, internal building blocks shared by Gym Now and the programme
 * screens. Strength colour (tertiary / plum) only; glyphs are decorative and hidden from screen
 * readers, so every meaning stays in text. Candidates for promotion to the shared design system.
 */

/** A decorative plum glyph disc, as used by [org.racedaydreams.coach.SectionLabel]. */
@Composable
internal fun GymGlyphDisc(glyph: Glyph, size: Dp = 40.dp) {
    val tones = chartAccent(ChartHue.PLUM)
    Box(
        Modifier.size(size)
            .clip(CircleShape)
            .background(tones.soft.copy(alpha = 0.22f))
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, tones.strong, size * 0.55f)
    }
}

/**
 * The one prominent start action of a gym region: full width, strength accent, play glyph. Unlike
 * the frozen hero action it can be disabled while saving or while a workout is unfinished.
 */
@Composable
internal fun GymStartAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    val strength = Discipline.STRENGTH
    Button(
        onClick = onClick,
        enabled = enabled,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = strength.accent(),
                contentColor = strength.onAccent(),
            ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        GlyphIcon(Glyph.PLAY, androidx.compose.material3.LocalContentColor.current, 18.dp)
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

/** A quieter, strength-tinted start for one row among several (e.g. one programme session). */
@Composable
internal fun GymRowAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Text(label)
    }
}

/**
 * A glyph-led choice: a borderless tonal tile with a plum glyph disc, a title, one line of detail
 * and a chevron. The whole tile is one 72 dp touch target whose label is its text.
 */
@Composable
internal fun GymOptionTile(
    title: String,
    detail: String,
    glyph: Glyph,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                .alpha(if (enabled) 1f else 0.38f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GymGlyphDisc(glyph)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GlyphIcon(Glyph.CHEVRON, MaterialTheme.colorScheme.onSurfaceVariant, 18.dp)
        }
    }
}

/** Option tiles stacked with consistent spacing. */
@Composable
internal fun GymOptionTiles(content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

/**
 * One programme session as a readable row: the date as a plum eyebrow, the session title, then a
 * short detail line (exercise count, expected time). Optional [content] holds the row's action.
 */
@Composable
internal fun GymSessionRow(
    date: String,
    title: String,
    detail: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Eyebrow(date, MaterialTheme.typography.labelMedium, chartAccent(ChartHue.PLUM).strong)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (detail != null)
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        content()
    }
}

/** A [GymSessionRow] that opens the session: the whole row is one touch target with a chevron. */
@Composable
internal fun GymSessionLink(date: String, title: String, detail: String?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Row(
            Modifier.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GymSessionRow(date, title, detail, Modifier.weight(1f))
            GlyphIcon(Glyph.CHEVRON, MaterialTheme.colorScheme.onSurfaceVariant, 18.dp)
        }
    }
}

/** A one-line session link whose label is fixed text (kept exactly for existing journeys). */
@Composable
internal fun GymTextLink(text: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Row(
            Modifier.padding(vertical = 8.dp).alpha(if (enabled) 1f else 0.38f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            GlyphIcon(Glyph.CHEVRON, MaterialTheme.colorScheme.onSurfaceVariant, 18.dp)
        }
    }
}

/** Exercise count with correct plural, e.g. "1 exercise", "5 exercises". */
internal fun exerciseCount(count: Int) = if (count == 1) "1 exercise" else "$count exercises"
