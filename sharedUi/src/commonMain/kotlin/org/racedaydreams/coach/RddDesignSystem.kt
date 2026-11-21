@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * The week at a glance: seven days, today highlighted, one mark per session. Done is a filled dot,
 * planned an outlined ring, missed a muted ring with a slash, so state never relies on colour.
 */
@Composable
fun WeekStrip(days: List<WeekDay>, today: LocalDate, modifier: Modifier = Modifier) {
    if (days.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth().testTag("week-strip"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            days.forEach { day ->
                val isToday = day.date == today
                val description =
                    "${day.weekday} ${day.date.getDayOfMonth()}" +
                        (if (isToday) ", today" else "") +
                        ": " +
                        if (day.sessions.isEmpty()) "nothing planned"
                        else
                            day.sessions.joinToString("; ") {
                                "${it.title} " +
                                    when (it.mark) {
                                        SessionMark.DONE -> "done"
                                        SessionMark.PLANNED -> "planned"
                                        SessionMark.MISSED -> "not recorded"
                                    }
                            }
                Column(
                    Modifier.weight(1f).heightIn(min = 72.dp).clearAndSetSemantics {
                        contentDescription = description
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        day.weekday.take(1),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(
                        Modifier.size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isToday) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${day.date.getDayOfMonth()}",
                            style = MaterialTheme.typography.labelLarge,
                            color =
                                if (isToday) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        day.sessions.take(3).forEach { SessionDot(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionDot(session: DaySession) {
    val accent = session.discipline.accent()
    val base = Modifier.size(9.dp).clip(CircleShape)
    when (session.mark) {
        SessionMark.DONE -> Box(base.background(accent))
        SessionMark.PLANNED -> Box(base.border(2.dp, accent, CircleShape))
        SessionMark.MISSED ->
            Box(base.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape))
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

/** A countdown to a dated goal: the number of days is the headline. */
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
                Modifier.widthIn(min = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "$days",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    if (days == 1L) "day" else "days",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onClick != null)
                Text(
                    "›",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clearAndSetSemantics {},
                )
        }
    }
    if (onClick != null)
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
        ) {
            content()
        }
    else
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
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
