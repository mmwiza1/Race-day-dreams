package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.TrainingPhase
import org.racedaydreams.domain.presentation.CalendarData
import org.racedaydreams.domain.time.LocalDate

/*
 * W2 Plan and Calendar composition: the programme as a journey (phase track, milestone list) and
 * calendar marks that share the Home week strip's language (done filled, planned ring, missed
 * slashed; running and strength by discipline colour). Presentation only: every date, phase and
 * state comes from existing plan and calendar data.
 */

/** A phase's readable name, unchanged from the earlier programme details wording. */
internal fun phaseName(kind: TrainingPhase.Kind): String =
    kind.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

/** One phase as a share of the programme, for the journey track. */
internal class JourneySegment(val start: Float, val end: Float, val current: Boolean)

/** Where today and the phases sit within a programme's own start and end dates. */
internal class PlanJourney(
    val segments: List<JourneySegment>,
    val todayFraction: Float,
    val currentPhase: TrainingPhase?,
    val raceFraction: Float?,
)

internal fun planJourney(
    start: LocalDate,
    end: LocalDate,
    phases: List<TrainingPhase>,
    today: LocalDate,
    raceDate: LocalDate?,
): PlanJourney {
    val first = start.toEpochDay()
    val days = (end.toEpochDay() - first + 1).coerceAtLeast(1)
    fun at(epochDay: Long) = ((epochDay - first).toFloat() / days).coerceIn(0f, 1f)
    val day = today.toEpochDay()
    val current =
        phases.firstOrNull { day in it.startDate().toEpochDay()..it.endDate().toEpochDay() }
            ?: phases.lastOrNull { it.endDate().toEpochDay() < day }
            ?: phases.firstOrNull()
    val segments =
        if (phases.isEmpty()) listOf(JourneySegment(0f, 1f, true))
        else
            phases.map {
                JourneySegment(
                    at(it.startDate().toEpochDay()),
                    at(it.endDate().toEpochDay() + 1),
                    it == current,
                )
            }
    return PlanJourney(
        segments,
        at(day) + if (day in first until first + days) 0.5f / days else 0f,
        current,
        raceDate?.toEpochDay()?.takeIf { it in first until first + days }?.let { at(it + 1) },
    )
}

/**
 * The programme as a track: one segment per phase (completed part solid, the rest soft), today as a
 * ringed dot and a flag glyph at the finish. Decorative; the header states the same in text.
 */
@Composable
internal fun JourneyTrack(journey: PlanJourney, modifier: Modifier = Modifier) {
    val accent = Discipline.RUNNING.accent()
    val ring = Discipline.RUNNING.container()
    Row(
        modifier.fillMaxWidth().testTag("plan-journey-track").clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(Modifier.weight(1f).height(22.dp)) {
            val bar = 8.dp.toPx()
            val gap = 3.dp.toPx()
            val top = (size.height - bar) / 2
            val radius = CornerRadius(bar / 2, bar / 2)
            val today = journey.todayFraction * size.width
            journey.segments.forEach { segment ->
                val left = segment.start * size.width + if (segment.start > 0f) gap / 2 else 0f
                val right = segment.end * size.width - if (segment.end < 1f) gap / 2 else 0f
                if (right <= left) return@forEach
                drawRoundRect(
                    accent.copy(alpha = if (segment.current) 0.38f else 0.2f),
                    Offset(left, top),
                    Size(right - left, bar),
                    radius,
                )
                val done = today.coerceIn(left, right)
                if (done > left)
                    drawRoundRect(accent, Offset(left, top), Size(done - left, bar), radius)
            }
            val dot = 7.dp.toPx()
            val x = today.coerceIn(dot, size.width - dot)
            drawCircle(ring, dot + 2.5.dp.toPx(), Offset(x, size.height / 2))
            drawCircle(accent, dot, Offset(x, size.height / 2))
        }
        GlyphIcon(Glyph.FLAG, accent, 20.dp)
    }
}

/**
 * The Plan header: race-distance eyebrow, the single "Plan" title, the journey position and its
 * phase, then the phase track with the start and race (or finish) dates. Compact by design: the
 * Plan's content is the week below, not an action.
 */
@Composable
internal fun JourneyHeader(
    eyebrow: String,
    position: String,
    journey: PlanJourney,
    startLabel: String,
    finishLabel: String,
) {
    val onContainer = Discipline.RUNNING.onContainer()
    Surface(
        color = Discipline.RUNNING.container(),
        contentColor = onContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth().testTag("plan-journey"),
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column {
                Eyebrow(
                    eyebrow,
                    MaterialTheme.typography.labelLarge,
                    onContainer.copy(alpha = .85f),
                )
                Text(
                    "Plan",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    position,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            JourneyTrack(journey)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    startLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = onContainer.copy(alpha = .8f),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    finishLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** How strongly a session's glyph disc is drawn: done, planned, or muted (rest, missed). */
internal enum class DiscTone {
    FILLED,
    TONAL,
    MUTED,
}

/** A decorative discipline glyph disc for session rows; hidden from screen readers. */
@Composable
internal fun SessionDisc(glyph: Glyph, discipline: Discipline, tone: DiscTone, size: Dp = 40.dp) {
    val (background, tint) =
        when (tone) {
            DiscTone.FILLED -> discipline.accent() to discipline.onAccent()
            DiscTone.TONAL -> discipline.container() to discipline.accent()
            DiscTone.MUTED ->
                MaterialTheme.colorScheme.surfaceContainerHighest to
                    MaterialTheme.colorScheme.onSurfaceVariant
        }
    Box(
        Modifier.size(size).clip(CircleShape).background(background).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, tint, size * 0.55f)
    }
}

/** The discipline a calendar entry belongs to; rest days have none. */
internal fun CalendarData.Entry.discipline(): Discipline? =
    when (category()) {
        CalendarData.Category.STRENGTH -> Discipline.STRENGTH
        CalendarData.Category.REST -> null
        else -> Discipline.RUNNING
    }

internal fun CalendarData.Entry.glyph(): Glyph =
    when (category()) {
        CalendarData.Category.STRENGTH -> Glyph.DUMBBELL
        CalendarData.Category.RACE -> Glyph.FLAG
        CalendarData.Category.REST -> Glyph.SPARK
        else -> Glyph.RUN
    }

internal fun CalendarData.Entry.tone(): DiscTone =
    when {
        category() == CalendarData.Category.REST -> DiscTone.MUTED
        state() == CalendarData.State.COMPLETED -> DiscTone.FILLED
        state() == CalendarData.State.PLANNED || state() == CalendarData.State.MOVED ->
            DiscTone.TONAL
        else -> DiscTone.MUTED
    }

/** Long runs, quality sessions and races: the sessions a week is built around. */
internal fun CalendarData.Entry.isKey(): Boolean =
    category() == CalendarData.Category.LONG ||
        category() == CalendarData.Category.QUALITY ||
        category() == CalendarData.Category.RACE

/**
 * A calendar mark in the week strip's language: completed filled, planned a ring, moved a dashed
 * ring, missed or skipped a muted ring with a slash. Running and strength take their discipline
 * colours; rest is neutral. The category letter is drawn inside for the month grid.
 */
@Composable
internal fun SessionMarkShape(entry: CalendarData.Entry, size: Dp, letter: String?) {
    val discipline = entry.discipline()
    val muted = MaterialTheme.colorScheme.outline
    val accent = discipline?.accent() ?: muted
    val state = entry.state()
    val missed = state == CalendarData.State.NOT_COMPLETED || state == CalendarData.State.SKIPPED
    val filled = state == CalendarData.State.COMPLETED && discipline != null
    // The letter is sized from the mark, not the text scale, so it always fits inside it; the
    // day's spoken description carries the same meaning at any text size.
    val letterSize = with(LocalDensity.current) { (size * 0.6f).toSp() }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 1.75.dp.toPx()
            val r = (this.size.minDimension - stroke) / 2
            when {
                filled -> drawCircle(accent)
                missed -> drawCircle(muted, r, style = Stroke(stroke))
                state == CalendarData.State.MOVED ->
                    drawCircle(
                        accent,
                        r,
                        style =
                            Stroke(
                                stroke,
                                pathEffect =
                                    PathEffect.dashPathEffect(
                                        floatArrayOf(3.dp.toPx(), 2.dp.toPx())
                                    ),
                            ),
                    )
                else -> drawCircle(accent, r, style = Stroke(stroke * 1.15f))
            }
        }
        if (letter != null)
            Text(
                letter,
                fontSize = letterSize,
                lineHeight = letterSize,
                fontWeight = FontWeight.ExtraBold,
                color =
                    when {
                        filled -> discipline!!.onAccent()
                        missed || discipline == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> accent
                    },
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.clearAndSetSemantics {},
            )
        if (missed)
            Canvas(Modifier.fillMaxSize()) {
                drawLine(
                    muted,
                    Offset(this.size.width * 0.18f, this.size.height * 0.82f),
                    Offset(this.size.width * 0.82f, this.size.height * 0.18f),
                    1.75.dp.toPx(),
                )
            }
    }
}

/**
 * The calendar's always-visible key, on one compact line where it fits: the three state shapes
 * (done filled, planned ring, missed slashed), then the two discipline colours as small squares so
 * colour and state are never confused. The letter glossary lives in the expandable "Calendar key".
 * Each day already speaks its own sessions, so this key is hidden from screen readers.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CalendarMarkKey() {
    FlowRow(
        Modifier.fillMaxWidth().testTag("calendar-mark-key").clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
                CalendarData.State.COMPLETED to "Done",
                CalendarData.State.PLANNED to "Planned",
                CalendarData.State.NOT_COMPLETED to "Missed",
            )
            .forEach { (state, label) ->
                KeyItem(label) {
                    SessionMarkShape(
                        CalendarData.Entry(CalendarData.Category.EASY, state, label, null, null),
                        12.dp,
                        null,
                    )
                }
            }
        Box(
            Modifier.size(width = 1.dp, height = 12.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        listOf(Discipline.RUNNING to "Run", Discipline.STRENGTH to "Strength").forEach {
            (discipline, label) ->
            KeyItem(label) {
                Box(
                    Modifier.size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(discipline.accent())
                )
            }
        }
    }
}

@Composable
private fun KeyItem(label: String, mark: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        mark()
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A tonal pill of equal segments (List, Week, Month): the selected one is filled, carries a check
 * and is exposed as a selected tab, so the choice never relies on colour alone.
 */
@Composable
internal fun ViewSelector(
    options: List<Pair<String, Glyph>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(50),
        modifier = modifier.fillMaxWidth().testTag("calendar-view-selector"),
    ) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { (label, glyph) ->
                val isSelected = label == selected
                val ink =
                    if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                Row(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                            else Color.Transparent
                        )
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onSelect(label) },
                        )
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.clearAndSetSemantics {}) {
                        if (isSelected) Text("✓", color = ink) else GlyphIcon(glyph, ink, 16.dp)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = ink,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * One phase as a milestone: a marker on a connecting line (past filled, current ringed in the
 * running colour, future outlined), its name and dates, then the phase's own details.
 */
@Composable
internal fun PhaseMilestone(
    name: String,
    dates: String,
    past: Boolean,
    current: Boolean,
    last: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val accent = Discipline.RUNNING.accent()
    val line = MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier.fillMaxWidth().drawBehind {
            if (!last)
                drawLine(
                    line,
                    Offset(12.dp.toPx(), 26.dp.toPx()),
                    Offset(12.dp.toPx(), size.height),
                    2.dp.toPx(),
                )
        },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.padding(top = 4.dp).size(24.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
                when {
                    current -> {
                        drawCircle(accent.copy(alpha = .25f))
                        drawCircle(accent, this.size.minDimension / 4)
                    }
                    past -> drawCircle(accent, this.size.minDimension / 3)
                    else ->
                        drawCircle(line, this.size.minDimension / 3, style = Stroke(2.dp.toPx()))
                }
            }
        }
        Column(
            Modifier.weight(1f).padding(bottom = if (last) 0.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (current) FontWeight.Bold else FontWeight.SemiBold,
                color = if (current) accent else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                if (current) "Now · $dates" else dates,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}
