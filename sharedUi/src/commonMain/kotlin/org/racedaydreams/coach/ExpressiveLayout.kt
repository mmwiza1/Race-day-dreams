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
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
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
    /** Running: motion lines and a forward chevron. */
    RUN(
        "M2,7 L9,7 L9,9 L2,9 Z M4,11 L11,11 L11,13 L4,13 Z M2,15 L9,15 L9,17 L2,17 Z M13,5 L20,12 L13,19 L11.5,17.5 L17,12 L11.5,6.5 Z"
    ),
    CALENDAR(
        "M3,5 L21,5 L21,21 L3,21 Z M5,10 L5,19 L19,19 L19,10 Z M7,2 L9,2 L9,6 L7,6 Z M15,2 L17,2 L17,6 L15,6 Z M7,12 L10,12 L10,15 L7,15 Z"
    ),
    FLAG("M5,2 L7,2 L7,22 L5,22 Z M8,3 L20,3 L17,7.5 L20,12 L8,12 Z"),
    SPARK("M12,2 L14.2,9.8 L22,12 L14.2,14.2 L12,22 L9.8,14.2 L2,12 L9.8,9.8 Z"),
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

/**
 * A letter-spaced capitalised label. Screen readers and tests get the natural-case words, so
 * "STRENGTH TRAINING" is read as "Strength training", not spelt out.
 */
@Composable
fun Eyebrow(text: String, style: TextStyle, color: Color) {
    Text(
        text.uppercase(),
        style = style.copy(letterSpacing = 1.2.sp),
        color = color,
        modifier = Modifier.clearAndSetSemantics { this.text = AnnotatedString(text) },
    )
}

/** Text colour that reads on a discipline accent fill. */
@Composable
fun Discipline.onAccent(): Color =
    customAccent()?.onPrimary
        ?: when (this) {
            Discipline.RUNNING -> org.racedaydreams.coach.theme.disciplineBaseScheme().onPrimary
            Discipline.STRENGTH,
            Discipline.BOULDERING -> org.racedaydreams.coach.theme.disciplineBaseScheme().onTertiary
            Discipline.PROGRESS,
            Discipline.RECOVERY -> org.racedaydreams.coach.theme.disciplineBaseScheme().onSecondary
        }

/**
 * The tonal surface every hero shares: discipline container, 32 dp corners, a glyph disc and a
 * letter-spaced eyebrow, then the caller's content.
 */
@Composable
fun HeroSurface(
    discipline: Discipline,
    glyph: Glyph,
    eyebrow: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = discipline.container(),
        contentColor = discipline.onContainer(),
        shape = RoundedCornerShape(32.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlyphDisc(glyph, discipline.accent(), discipline.onAccent(), 36.dp)
                Eyebrow(eyebrow, MaterialTheme.typography.labelLarge, LocalContentColor.current)
            }
            content()
        }
    }
}

/** A hero's display title; the accessibility heading of its region. */
@Composable
fun HeroTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() },
    )
}

/** The one prominent action of a hero: full-width, in the discipline accent, with a play glyph. */
@Composable
fun HeroPrimaryAction(discipline: Discipline, label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = discipline.accent(),
                contentColor = discipline.onAccent(),
            ),
        contentPadding = PaddingValues(horizontal = CoachSpace.Inset, vertical = CoachSpace.Card),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        GlyphIcon(Glyph.PLAY, discipline.onAccent(), 18.dp)
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Quieter hero actions: translucent rows with a chevron, side by side when there are two and the
 * text is normal size, otherwise stacked full width.
 */
@Composable
fun HeroSecondaryActions(actions: List<Pair<String, () -> Unit>>) {
    if (actions.isEmpty()) return
    val large = LocalDensity.current.fontScale >= 1.5f
    val content: @Composable (Pair<String, () -> Unit>, Modifier) -> Unit = { (label, action), m ->
        Surface(
            onClick = action,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            shape = RoundedCornerShape(20.dp),
            modifier = m.heightIn(min = CoachSpace.Touch),
        ) {
            Row(
                Modifier.padding(horizontal = CoachSpace.Inset, vertical = CoachSpace.Card),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                GlyphIcon(Glyph.CHEVRON, MaterialTheme.colorScheme.onSurfaceVariant, 16.dp)
            }
        }
    }
    // Side by side only when both labels are short enough not to wrap in half the width.
    if (large || actions.size != 2 || actions.any { it.first.length > 14 })
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            actions.forEach { content(it, Modifier.fillMaxWidth()) }
        }
    else
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            actions.forEach { content(it, Modifier.weight(1f)) }
        }
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
) =
    HeroSurface(discipline, glyph, eyebrow, Modifier.testTag("screen-hero")) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HeroTitle(title)
            if (supporting != null)
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyLarge,
                    color = LocalContentColor.current.copy(alpha = 0.85f),
                )
        }
        if (primary != null) HeroPrimaryAction(discipline, primary.first, primary.second)
        HeroSecondaryActions(secondary)
    }

/**
 * A second session on the same day, nested inside the day's hero (hybrid days): a translucent panel
 * led by its own discipline glyph, so running and strength read as one day's training.
 */
@Composable
fun CompanionSession(
    discipline: Discipline,
    glyph: Glyph,
    label: String,
    title: String,
    detail: String?,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().testTag("companion-session"),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlyphDisc(glyph, discipline.accent(), discipline.onAccent(), 36.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (detail != null)
                        Text(
                            detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                }
            }
            content()
        }
    }
}

/**
 * Unplanned starts as two tonal tiles, each with its discipline's glyph and colour, so "run" and
 * "gym" are recognisable at a glance; stacked full width at large text or when there is one.
 */
@Composable
fun QuickStartTiles(actions: List<QuickStart>) {
    if (actions.isEmpty()) return
    val large = LocalDensity.current.fontScale >= 1.5f
    val tile: @Composable (QuickStart, Modifier) -> Unit = { action, m ->
        Surface(
            onClick = action.onClick,
            color = action.discipline.container(),
            contentColor = action.discipline.onContainer(),
            shape = RoundedCornerShape(24.dp),
            modifier = m.heightIn(min = 64.dp),
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlyphDisc(
                    action.glyph,
                    action.discipline.accent(),
                    action.discipline.onAccent(),
                    36.dp,
                )
                Text(
                    action.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
    if (large || actions.size == 1)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            actions.forEach { tile(it, Modifier.fillMaxWidth()) }
        }
    else
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            actions.forEach { tile(it, Modifier.weight(1f)) }
        }
}

class QuickStart(
    val label: String,
    val glyph: Glyph,
    val discipline: Discipline,
    val onClick: () -> Unit,
)

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
        contentColor = onContainer,
        shape = RoundedCornerShape(CoachSpace.Section),
        modifier = Modifier.fillMaxWidth().testTag("screen-header"),
    ) {
        Row(
            Modifier.padding(horizontal = CoachSpace.Inset, vertical = CoachSpace.Card),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CoachSpace.Card),
        ) {
            GlyphDisc(glyph, discipline.accent(), discipline.onAccent(), 44.dp)
            Column(Modifier.weight(1f)) {
                Eyebrow(
                    eyebrow,
                    MaterialTheme.typography.labelMedium,
                    onContainer.copy(alpha = 0.85f),
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
fun Panel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(CoachSpace.Inset),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(CoachSpace.Card),
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
    contentPadding: PaddingValues = PaddingValues(CoachSpace.Inset),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel(title, glyph, hue, caption, trailing)
        Panel(contentPadding = contentPadding, content = content)
    }
}
