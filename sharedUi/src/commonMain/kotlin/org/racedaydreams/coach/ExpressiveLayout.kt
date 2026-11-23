@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

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
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Expressive screen composition (owner-requested visual pass): a tonal hero header that gives each
 * tab its identity and primary action, section labels with a small coloured glyph above
 * borderless, large-radius panels, and generous section spacing. Layout only; no data.
 */

/** Small decorative glyphs drawn from paths (no icon dependency). Hidden from screen readers. */
enum class Glyph(internal val path: String) {
    DUMBBELL(
        "M1,9 L3,9 L3,6 L6,6 L6,10 L18,10 L18,6 L21,6 L21,9 L23,9 L23,15 L21,15 L21,18 L18,18 L18,14 L6,14 L6,18 L3,18 L3,15 L1,15 Z"
    ),
    TREND(
        "M3,3 L5,3 L5,19 L22,19 L22,21 L3,21 Z M7,15 L11,10 L15,12 L20,5 L22,6 L16,15 L12,13 L9,17 Z"
    ),
    VOLUME("M3,20 L3,13 L7,13 L7,20 Z M10,20 L10,6 L14,6 L14,20 Z M17,20 L17,9 L21,9 L21,20 Z"),
    BODY(
        "M9.5,1.5 L14.5,1.5 L14.5,6.5 L9.5,6.5 Z M5,8 L19,8 L19,10.5 L15,10.5 L15,22 L12.8,22 L12.8,15 L11.2,15 L11.2,22 L9,22 L9,10.5 L5,10.5 Z"
    ),
    LIST(
        "M3,4 L21,4 L21,6.5 L3,6.5 Z M3,10.75 L21,10.75 L21,13.25 L3,13.25 Z M3,17.5 L15,17.5 L15,20 L3,20 Z"
    ),
    PLAY("M7,4 L20,12 L7,20 Z"),
    CHEVRON("M9,5 L16,12 L9,19 L7.5,17.5 L13,12 L7.5,6.5 Z");

    internal val vector: ImageVector by lazy {
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .apply {
                addPath(
                    pathData = PathParser().parsePathString(path).toNodes(),
                    fill = SolidColor(Color.White),
                    pathFillType = PathFillType.EvenOdd,
                )
            }
            .build()
    }
}

@Composable
fun GlyphIcon(glyph: Glyph, tint: Color, size: Dp = 20.dp) {
    Icon(glyph.vector, contentDescription = null, tint = tint, modifier = Modifier.size(size))
}

@Composable
private fun GlyphDisc(glyph: Glyph, background: Color, tint: Color, size: Dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(background).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, tint, size * 0.55f)
    }
}

/** Text colour that reads on a discipline accent fill. */
@Composable
fun Discipline.onAccent(): Color =
    when (this) {
        Discipline.RUNNING -> MaterialTheme.colorScheme.onPrimary
        Discipline.STRENGTH -> MaterialTheme.colorScheme.onTertiary
        Discipline.RECOVERY -> MaterialTheme.colorScheme.onSecondary
    }

/**
 * A tab's hero: tonal discipline surface with an eyebrow, a large title, one line of context and,
 * where the tab has one, a prominent primary action with up to two quieter secondary actions.
 */
@Composable
fun ScreenHero(
    discipline: Discipline,
    glyph: Glyph,
    eyebrow: String,
    title: String,
    supporting: String?,
    primary: Pair<String, () -> Unit>? = null,
    secondary: List<Pair<String, () -> Unit>> = emptyList(),
) {
    val onContainer = discipline.onContainer()
    val large = LocalDensity.current.fontScale >= 1.5f
    Surface(
        color = discipline.container(),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth().testTag("screen-hero"),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlyphDisc(glyph, discipline.accent(), discipline.onAccent(), 36.dp)
                Text(
                    eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.2.sp),
                    color = onContainer,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = onContainer,
                    modifier = Modifier.semantics { heading() },
                )
                if (supporting != null)
                    Text(
                        supporting,
                        style = MaterialTheme.typography.bodyLarge,
                        color = onContainer.copy(alpha = 0.85f),
                    )
            }
            if (primary != null)
                Button(
                    onClick = primary.second,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = discipline.accent(),
                            contentColor = discipline.onAccent(),
                        ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    GlyphIcon(Glyph.PLAY, discipline.onAccent(), 18.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(primary.first, style = MaterialTheme.typography.titleMedium)
                }
            if (secondary.isNotEmpty()) {
                val content: @Composable (Pair<String, () -> Unit>, Modifier) -> Unit =
                    { (label, action), modifier ->
                        Surface(
                            onClick = action,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = modifier.heightIn(min = 52.dp),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                GlyphIcon(
                                    Glyph.CHEVRON,
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                    16.dp,
                                )
                            }
                        }
                    }
                if (large || secondary.size == 1)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        secondary.forEach { content(it, Modifier.fillMaxWidth()) }
                    }
                else
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        secondary.forEach { content(it, Modifier.weight(1f)) }
                    }
            }
        }
    }
}

/**
 * A compact tonal header for screens whose content, not an action, is the point (Progress): the
 * same discipline colour, glyph and bold title as [ScreenHero] in about a third of the height.
 */
@Composable
fun ScreenHeader(
    discipline: Discipline,
    glyph: Glyph,
    eyebrow: String,
    title: String,
    trailing: (@Composable () -> Unit)? = null,
) {
    val onContainer = discipline.onContainer()
    Surface(
        color = discipline.container(),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().testTag("screen-header"),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GlyphDisc(glyph, discipline.accent(), discipline.onAccent(), 44.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                    color = onContainer.copy(alpha = 0.85f),
                )
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = onContainer,
                    modifier = Modifier.semantics { heading() },
                )
            }
            trailing?.invoke()
        }
    }
}

/**
 * A section's label above its panel: a small glyph disc in the section's hue, a bold title (the
 * accessibility heading) and an optional one-line caption, with an optional trailing action.
 */
@Composable
fun SectionLabel(
    title: String,
    glyph: Glyph,
    hue: ChartHue,
    caption: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val tones = chartAccent(hue)
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GlyphDisc(glyph, tones.soft.copy(alpha = 0.22f), tones.strong, 32.dp)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            if (caption != null)
                Text(
                    caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
        }
        trailing?.invoke()
    }
}

/** A borderless, large-radius tonal panel that holds one section's content. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** Label plus panel: the standard section of an expressive tab. */
@Composable
fun ExpressiveSection(
    title: String,
    glyph: Glyph,
    hue: ChartHue,
    caption: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel(title, glyph, hue, caption, trailing)
        Panel(content = content)
    }
}
