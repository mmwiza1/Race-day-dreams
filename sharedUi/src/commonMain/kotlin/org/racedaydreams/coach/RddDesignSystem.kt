@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.time.LocalDate

/*
 * Race Day Dreams design system (2026). Built only from Material 3 colour roles, the shared type
 * scale and the existing shapes, so Material You dynamic colour, dark mode and large text keep
 * working. Disciplines are told apart by colour role AND by label or shape, never colour alone.
 */

/** Training disciplines that share one visual language but stay distinguishable. */
enum class Discipline(val label: String) {
    RUNNING("Run"),
    STRENGTH("Strength"),
    RECOVERY("Recovery"),
}

/**
 * Chart identities: one restrained hue per measure, shared by every chart (running trends now,
 * strength progression later). Each hue has a strong tone for the selected value and a softer,
 * related tone for the rest, in light and dark variants. Both tones keep at least 3:1 contrast with
 * the app's light and dark surfaces, so marks stay readable; selection is also shown by a marker
 * and guide line, never by colour alone. Hues identify a measure only; they never mean a result is
 * good or bad. They are fixed tones rather than wallpaper-derived, so a measure looks the same with
 * dynamic colour on or off.
 */
enum class ChartHue(
    internal val lightStrong: Long,
    internal val lightSoft: Long,
    internal val darkStrong: Long,
    internal val darkSoft: Long,
) {
    /** Distance: rich blue. */
    BLUE(0xFF1D4ED8, 0xFF5F82DA, 0xFFA8C4FF, 0xFF5E7FC7),
    /** Training time: teal. */
    TEAL(0xFF00796B, 0xFF3F9A8E, 0xFF7FDACB, 0xFF3E8F85),
    /** Pace: indigo-violet. */
    VIOLET(0xFF5B3FC4, 0xFF8A73D9, 0xFFC9B8FF, 0xFF7C68C4),
    /** Heart rate: soft coral. */
    CORAL(0xFFC9483B, 0xFFD9685A, 0xFFFFB4A8, 0xFFB9655A),

    /** Strength (lifts, volume, workload): plum, distinct from the indigo-violet of pace. */
    PLUM(0xFF7B2D78, 0xFFA65BA2, 0xFFF0A6E8, 0xFFA8649F);

    fun strong(dark: Boolean): Color = Color(if (dark) darkStrong else lightStrong)

    fun soft(dark: Boolean): Color = Color(if (dark) darkSoft else lightSoft)
}

/** The strong and soft tones of [hue] for the current theme's surface. */
class ChartAccent(val strong: Color, val soft: Color)

@Composable
fun chartAccent(hue: ChartHue): ChartAccent {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return ChartAccent(hue.strong(dark), hue.soft(dark))
}

/** Accent (marks, figures) and container (hero surfaces) colours for a discipline. */
@Composable
fun Discipline.accent(): Color =
    when (this) {
        Discipline.RUNNING -> MaterialTheme.colorScheme.primary
        Discipline.STRENGTH -> MaterialTheme.colorScheme.tertiary
        Discipline.RECOVERY -> MaterialTheme.colorScheme.secondary
    }

@Composable
fun Discipline.container(): Color =
    when (this) {
        Discipline.RUNNING -> MaterialTheme.colorScheme.primaryContainer
        Discipline.STRENGTH -> MaterialTheme.colorScheme.tertiaryContainer
        Discipline.RECOVERY -> MaterialTheme.colorScheme.secondaryContainer
    }

@Composable
fun Discipline.onContainer(): Color =
    when (this) {
        Discipline.RUNNING -> MaterialTheme.colorScheme.onPrimaryContainer
        Discipline.STRENGTH -> MaterialTheme.colorScheme.onTertiaryContainer
        Discipline.RECOVERY -> MaterialTheme.colorScheme.onSecondaryContainer
    }

/**
 * The single most important thing on a screen: a discipline-tinted surface with generous padding.
 * One per screen region, so it never competes with another hero.
 */
@Composable
fun HeroCard(
    discipline: Discipline,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = discipline.container(),
        contentColor = discipline.onContainer(),
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** A short discipline label with a shape cue, e.g. "● Today's run". */
@Composable
fun DisciplineLabel(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(8.dp)
                .clip(CircleShape)
                .background(LocalContentColor.current)
                .clearAndSetSemantics {}
        )
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** One figure: a large value with a smaller unit in the same text, and a label beneath. */
class Stat(
    val label: String,
    val value: String,
    val unit: String = "",
    val unitGap: Boolean = true,
)

@Composable
fun StatBlock(stat: Stat, modifier: Modifier = Modifier, large: Boolean = false) {
    Column(
        modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            buildAnnotatedString {
                append(stat.value)
                if (stat.unit.isNotEmpty()) {
                    if (stat.unitGap) append(" ")
                    withStyle(
                        SpanStyle(
                            fontSize = MaterialTheme.typography.titleSmall.fontSize,
                            fontWeight = FontWeight.Medium,
                        )
                    ) {
                        append(stat.unit)
                    }
                }
            },
            style =
                if (large) MaterialTheme.typography.displaySmall
                else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(stat.label, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * Up to three figures side by side, separated by space rather than boxes; stacked at very large
 * text so nothing truncates.
 */
@Composable
fun StatRow(stats: List<Stat>, modifier: Modifier = Modifier) {
    if (stats.isEmpty()) return
    if (LocalDensity.current.fontScale >= 1.5f)
        Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            stats.forEach { StatBlock(it) }
        }
    else
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            stats.forEach { StatBlock(it, Modifier.weight(1f)) }
        }
}

/** Completion state of one session on one day; the week strip draws it as a distinct shape. */
enum class SessionMark {
    DONE,
    PLANNED,
    MISSED,
}

class DaySession(val discipline: Discipline, val mark: SessionMark, val title: String)

class WeekDay(val date: LocalDate, val weekday: String, val sessions: List<DaySession>)

/**
 * Seven days at a glance, today highlighted, one mark per session. Done is a filled dot, planned an
 * outlined ring, missed a muted ring with a slash, so state never relies on colour. Tapping a day
 * opens its sessions below the strip (tap again to close); nothing is open at first, so the strip
 * stays a compact overview and does not repeat today's hero or Coming up. At 150% text and above
 * the days stack as full-width rows so dates and marks are never squeezed.
 */
@Composable
fun WeekStrip(days: List<WeekDay>, today: LocalDate, modifier: Modifier = Modifier) {
    if (days.isEmpty()) return
    var selectedDay by
        rememberSaveable(days.first().date.toString()) { mutableStateOf<String?>(null) }
    val selected = days.firstOrNull { it.date.toString() == selectedDay }
    fun toggle(day: WeekDay) {
        selectedDay = if (selectedDay == day.date.toString()) null else day.date.toString()
    }
    val large = LocalDensity.current.fontScale >= 1.5f
    Surface(
        modifier = modifier.fillMaxWidth().testTag("week-strip"),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Text(
                WeekAtAGlance.range(days),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )
            if (large) {
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    days.forEach { day ->
                        DayRow(day, day.date == today, day == selected) { toggle(day) }
                        if (day == selected) DayDetail(day, day.date == today)
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    days.forEach { day ->
                        DayColumn(day, day.date == today, day == selected, Modifier.weight(1f)) {
                            toggle(day)
                        }
                    }
                }
                selected?.let { DayDetail(it, it.date == today) }
            }
            WeekLegend(days)
        }
    }
}

private fun markText(mark: SessionMark) =
    when (mark) {
        SessionMark.DONE -> "done"
        SessionMark.PLANNED -> "planned"
        SessionMark.MISSED -> "not recorded"
    }

private fun daySemantics(day: WeekDay, isToday: Boolean, isSelected: Boolean, toggle: () -> Unit) =
    Modifier.clearAndSetSemantics {
        contentDescription =
            WeekAtAGlance.spokenDay(day.date) +
                (if (isToday) ", today" else "") +
                ": " +
                if (day.sessions.isEmpty()) "rest day, nothing scheduled"
                else day.sessions.joinToString("; ") { "${it.title} ${markText(it.mark)}" }
        selected = isSelected
        role = Role.Tab
        onClick(label = if (isSelected) "Hide sessions" else "Show sessions") {
            toggle()
            true
        }
    }

@Composable
private fun DayColumn(
    day: WeekDay,
    isToday: Boolean,
    isSelected: Boolean,
    modifier: Modifier,
    toggle: () -> Unit,
) {
    Column(
        modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.secondaryContainer
                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = toggle)
            .heightIn(min = 84.dp)
            .padding(vertical = 8.dp)
            .testTag("week-day-${day.date}")
            .then(daySemantics(day, isToday, isSelected, toggle)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            day.weekday,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        DayNumber(day, isToday)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            day.sessions.take(3).forEach { SessionDot(it) }
        }
    }
}

@Composable
private fun DayNumber(day: WeekDay, isToday: Boolean) {
    Box(
        Modifier.size(32.dp)
            .clip(CircleShape)
            .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${day.date.getDayOfMonth()}",
            style = MaterialTheme.typography.labelLarge,
            color =
                if (isToday) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** Large-text layout: one full-width row per day, at least 48 dp tall. */
@Composable
private fun DayRow(day: WeekDay, isToday: Boolean, isSelected: Boolean, toggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
            )
            .clickable(onClick = toggle)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("week-day-${day.date}")
            .then(daySemantics(day, isToday, isSelected, toggle)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            WeekAtAGlance.dayLabel(day.date) + if (isToday) " · Today" else "",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            day.sessions.take(3).forEach { SessionDot(it) }
        }
    }
}

/** The selected day's sessions; a rest day says so plainly. */
@Composable
private fun DayDetail(day: WeekDay, isToday: Boolean) {
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("week-day-detail")
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            (if (isToday) "Today, " else "") + WeekAtAGlance.dayLabel(day.date),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        if (day.sessions.isEmpty())
            Text(
                "Rest day: nothing scheduled.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        day.sessions.forEach { session ->
            Row(
                Modifier.semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SessionDot(session)
                Text(
                    "${session.title} · ${markText(session.mark).replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/**
 * A visible key, so a first-time user reads the marks without explanation: the three shapes, plus
 * the discipline colours when the week mixes running and strength. Each day already speaks its own
 * state, so the key is hidden from screen readers.
 */
@Composable
private fun WeekLegend(days: List<WeekDay>) {
    val sessions = days.flatMap { it.sessions }
    if (sessions.isEmpty()) return
    val disciplines = sessions.map { it.discipline }.distinct()
    val ink = disciplines.singleOrNull() ?: Discipline.RUNNING
    FlowRow(
        Modifier.fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val marks = sessions.map { it.mark }.toSet()
        listOf(
                SessionMark.DONE to "Done",
                SessionMark.PLANNED to "Planned",
                SessionMark.MISSED to "Missed",
            )
            .filter { it.first in marks }
            .forEach { (mark, label) ->
                LegendItem(label) { SessionDot(DaySession(ink, mark, label)) }
            }
        if (disciplines.size > 1)
            disciplines.forEach { discipline ->
                LegendItem(discipline.label) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(discipline.accent()))
                }
            }
    }
}

@Composable
private fun LegendItem(label: String, mark: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        mark()
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SessionDot(session: DaySession) {
    val accent = session.discipline.accent()
    val base = Modifier.size(9.dp).clip(CircleShape)
    when (session.mark) {
        SessionMark.DONE -> Box(base.background(accent))
        SessionMark.PLANNED -> Box(base.border(2.dp, accent, CircleShape))
        SessionMark.MISSED -> {
            val outline = MaterialTheme.colorScheme.outline
            Box(
                base.border(1.5.dp, outline, CircleShape).drawBehind {
                    drawLine(
                        outline,
                        Offset(0f, size.height),
                        Offset(size.width, 0f),
                        1.5.dp.toPx(),
                    )
                }
            )
        }
    }
}

/** A compact calendar date for list rows: weekday over day number. Decorative to screen readers. */
@Composable
fun DateBadge(weekday: String, day: Int, discipline: Discipline) {
    Column(
        Modifier.size(48.dp)
            .clip(MaterialTheme.shapes.small)
            .background(discipline.container())
            .clearAndSetSemantics {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            weekday.take(3).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = discipline.onContainer(),
        )
        Text(
            "$day",
            style = MaterialTheme.typography.titleMedium,
            color = discipline.onContainer(),
            fontWeight = FontWeight.Bold,
        )
    }
}

/** A list row led by a date badge, for upcoming sessions. At least 64dp tall. */
@Composable
fun ScheduleRow(
    weekday: String,
    day: Int,
    discipline: Discipline,
    title: String,
    supporting: String,
    value: String?,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DateBadge(weekday, day, discipline)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (value != null) Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * A countdown to a dated goal: the number of days is the headline, in a large accent disc beside a
 * flag-marked label, on a borderless tonal panel.
 */
@Composable
fun CountdownCard(
    days: Long,
    label: String,
    title: String,
    supporting: String,
    onClick: (() -> Unit)?,
) {
    val content: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().padding(16.dp).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                Modifier.widthIn(min = 76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "$days",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    if (days == 1L) "day" else "days",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GlyphIcon(Glyph.FLAG, MaterialTheme.colorScheme.primary, 16.dp)
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onClick != null)
                Box(Modifier.clearAndSetSemantics {}) {
                    GlyphIcon(Glyph.CHEVRON, MaterialTheme.colorScheme.onSurfaceVariant, 20.dp)
                }
        }
    }
    val shape = RoundedCornerShape(28.dp)
    val colour = MaterialTheme.colorScheme.surfaceContainerLow
    if (onClick != null)
        Surface(
            onClick = onClick,
            shape = shape,
            color = colour,
            modifier = Modifier.fillMaxWidth().testTag("race-countdown"),
        ) {
            content()
        }
    else
        Surface(
            shape = shape,
            color = colour,
            modifier = Modifier.fillMaxWidth().testTag("race-countdown"),
        ) {
            content()
        }
}

/** Section title with an optional quiet trailing summary, e.g. "This week · 2 of 4 done". */
@Composable
fun SectionTitle(title: String, trailing: String? = null) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (trailing != null)
            Text(
                trailing,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}
