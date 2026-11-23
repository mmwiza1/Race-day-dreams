package org.racedaydreams.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.presentation.CalendarData
import org.racedaydreams.domain.time.*

fun sharedCalendarEntries(
    s: CoachUiState,
    d: LocalDate,
    today: LocalDate,
): List<CalendarData.Entry> {
    val base =
        CalendarData.day(
            d,
            today,
            s.currentPlan,
            s.currentPlan?.plan()?.workouts() ?: s.plannedWorkouts,
            s.activities,
            s.strengthSessions,
            s.strengthPlannedDates,
            s.workoutDecisions,
        )
    val authored = s.authoredGymProgramme
    val entries =
        if (authored == null)
            CalendarData.withGym(
                d,
                today,
                base,
                s.gymProgramme,
                s.gymLinks.values.associate { it.prescribedSessionId() to it.actualSessionId() },
            )
        else {
            val actualIds = s.strengthSessions.map { it.id() }.toSet()
            val prefix = authored.id() + ":v"
            val links =
                s.gymLinks.values
                    .filter { link ->
                        val version =
                            link
                                .versionKey()
                                .takeIf { it.startsWith(prefix) }
                                ?.removePrefix(prefix)
                                ?.toIntOrNull()
                        link.actualSessionId() in actualIds &&
                            version != null &&
                            version in 1..authored.revision()
                    }
                    .associate { it.prescribedSessionId() to it.actualSessionId() }
            CalendarData.withAuthoredGym(d, today, base, authored, links)
        }
    return if (
        s.gymProgramme == null &&
            s.authoredGymProgramme == null &&
            s.currentPlan == null &&
            s.profile?.preferences()?.gymDays()?.contains(d.getDayOfWeek()) == true &&
            entries.none { it.category() == CalendarData.Category.STRENGTH } &&
            s.strengthPlannedDates.values.none { it == d }
    )
        entries +
            CalendarData.Entry(
                CalendarData.Category.STRENGTH,
                CalendarData.State.PLANNED,
                "Strength routine",
                null,
                null,
            )
    else entries
}

private fun markerLetter(c: CalendarData.Category) =
    when (c) {
        CalendarData.Category.REST -> "–"
        CalendarData.Category.RUN -> "U"
        else -> c.name.take(1)
    }

private fun markerName(c: CalendarData.Category) =
    when (c) {
        CalendarData.Category.RUN -> "Unclassified run"
        else -> c.name.lowercase().replaceFirstChar { it.uppercase() }
    }

/**
 * One session's mark: the Home week strip's shapes (completed filled, planned a ring, moved a
 * dashed ring, missed or skipped slashed), the discipline's colour (running or strength, rest
 * neutral) and the category letter. Screen readers hear the session and its state.
 */
@Composable
fun CalendarMarker(entry: CalendarData.Entry) {
    Box(
        Modifier.size(20.dp).semantics { contentDescription = entry.description() },
        contentAlignment = Alignment.Center,
    ) {
        SessionMarkShape(entry, 20.dp, markerLetter(requireNotNull(entry.category())))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarLegend() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CalendarMarkKey()
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 0.dp),
        ) {
            Text(if (expanded) "Hide calendar key" else "Calendar key ⓘ")
        }
        if (expanded) {
            FlowRow(
                Modifier.semantics { contentDescription = "Workout category legend" },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CalendarData.Category.entries.forEach { c ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CalendarMarker(
                            CalendarData.Entry(
                                c,
                                CalendarData.State.PLANNED,
                                markerName(c),
                                null,
                                null,
                            )
                        )
                        Text(markerName(c), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Text(
                "Legend: outline planned · filled completed · dashed moved · slashed missed or skipped",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                "Running sessions use the running colour and strength sessions the strength colour.",
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                "Rest means an available day left free within your programme. Dates outside it and unavailable dates are not rest. U is an unclassified run; no intensity is inferred.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SharedActivityCalendarScreen(
    state: CoachUiState,
    onViewChange: (String) -> Unit,
    workout: (String) -> Unit,
    strength: (String) -> Unit,
    activity: (String) -> Unit,
    dates: CoachCalendarDates,
    distanceLabel: (Double) -> String,
    strengthAction: String = "Log workout",
    history: @Composable () -> Unit,
) {
    var focus by rememberSaveable { mutableStateOf(dates.today().toString()) }
    var showDay by rememberSaveable { mutableStateOf(false) }
    val day = dates.parse(focus)
    val month = state.calendarView == "Month"
    Column(Modifier.fillMaxSize()) {
        ViewSelector(
            listOf("List" to Glyph.LIST, "Week" to Glyph.VOLUME, "Month" to Glyph.CALENDAR),
            state.calendarView,
            onViewChange,
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        if (state.calendarView == "List") {
            history()
            return@Column
        }
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenHeader(
                Discipline.RUNNING,
                Glyph.CALENDAR,
                if (month) "Month at a glance" else "Week at a glance",
                "Training calendar",
            )
            PeriodNavigator(
                label =
                    if (month) dates.monthLabel(day)
                    else "Week of ${dates.label(dates.weekStart(day))}",
                unit = if (month) "month" else "week",
                previous = {
                    focus = (if (month) dates.shiftMonth(day, -1) else day.minusDays(7)).toString()
                },
                today = { focus = dates.today().toString() },
                next = {
                    focus = (if (month) dates.shiftMonth(day, 1) else day.plusWeeks(1)).toString()
                },
            )
            CalendarLegend()
            val start = if (month) dates.weekStart(dates.monthStart(day)) else dates.weekStart(day)
            val entries =
                remember(state, start, state.calendarView) {
                    (0 until if (month) 42 else 7).associate {
                        val d = start.plusDays(it.toLong())
                        d to sharedCalendarEntries(state, d, dates.today())
                    }
                }
            if (month) {
                Panel(contentPadding = PaddingValues(horizontal = 6.dp, vertical = 12.dp)) {
                    MonthGrid(start, day, dates, entries) { date ->
                        focus = date.toString()
                        showDay = true
                    }
                }
                if (showDay)
                    ModalBottomSheet(onDismissRequest = { showDay = false }) {
                        Column(
                            Modifier.fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            CalendarDay(
                                state,
                                day,
                                entries[day].orEmpty(),
                                workout,
                                strength,
                                activity,
                                dates,
                                distanceLabel,
                                strengthAction,
                            )
                        }
                    }
            } else
                repeat(7) {
                    val d = start.plusDays(it.toLong())
                    CalendarDay(
                        state,
                        d,
                        entries[d].orEmpty(),
                        workout,
                        strength,
                        activity,
                        dates,
                        distanceLabel,
                        strengthAction,
                    )
                }
        }
    }
}

/** The shown week or month between previous and next arrows, with a way back to today. */
@Composable
private fun PeriodNavigator(
    label: String,
    unit: String,
    previous: () -> Unit,
    today: () -> Unit,
    next: () -> Unit,
) {
    val large = LocalDensity.current.fontScale >= 1.5f
    val back: @Composable () -> Unit = {
        FilledTonalButton(
            onClick = today,
            modifier = Modifier.heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
        ) {
            Text("Today")
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PeriodArrow("Previous $unit", flip = true, previous)
            Text(
                label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            PeriodArrow("Next $unit", flip = false, next)
            if (!large) back()
        }
        if (large) back()
    }
}

@Composable
private fun PeriodArrow(description: String, flip: Boolean, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp).semantics { contentDescription = description },
    ) {
        Box(Modifier.graphicsLayer { if (flip) rotationZ = 180f }) {
            GlyphIcon(Glyph.CHEVRON, LocalContentColor.current, 22.dp)
        }
    }
}

/**
 * Six weeks of the month. Today's number sits in a filled disc; the selected date is a tonal tile
 * with an outline. Each day shows up to three marks (then "+n") and speaks every session.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthGrid(
    start: LocalDate,
    focus: LocalDate,
    dates: CoachCalendarDates,
    entries: Map<LocalDate, List<CalendarData.Entry>>,
    choose: (LocalDate) -> Unit,
) {
    val today = dates.today()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(
                    it,
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        repeat(6) { week ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { i ->
                    val date = start.plusDays((week * 7 + i).toLong())
                    val sessions = entries[date].orEmpty()
                    val outside = CalendarData.outsideMonth(date, focus)
                    val selected = date == focus
                    val isToday = date == today
                    val description =
                        "${dates.label(date)}${if (isToday) ", today" else ""}${if (selected) ", selected" else ""}. " +
                            if (sessions.isEmpty()) "No session planned"
                            else sessions.joinToString("; ") { it.description() }
                    Column(
                        Modifier.weight(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.secondaryContainer
                                else Color.Transparent
                            )
                            .then(
                                if (selected)
                                    Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(14.dp),
                                    )
                                else Modifier
                            )
                            .clickable { choose(date) }
                            .heightIn(min = 56.dp)
                            .padding(vertical = 4.dp)
                            .testTag("calendar-day-$date")
                            .clearAndSetSemantics {
                                contentDescription = description
                                this.selected = selected
                                role = Role.Button
                                onClick {
                                    choose(date)
                                    true
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Box(
                            Modifier.size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isToday) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${date.getDayOfMonth()}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight =
                                    if (isToday || selected) FontWeight.Bold else FontWeight.Normal,
                                color =
                                    when {
                                        isToday -> MaterialTheme.colorScheme.onPrimary
                                        outside ->
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = .6f
                                            )
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                maxLines = 1,
                            )
                        }
                        FlowRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            sessions.take(3).forEach { SessionMarkShape(it, 14.dp, null) }
                            if (sessions.size > 3)
                                Text(
                                    "+${sessions.size - 3}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    s: CoachUiState,
    d: LocalDate,
    entries: List<CalendarData.Entry>,
    workout: (String) -> Unit,
    strength: (String) -> Unit,
    activity: (String) -> Unit,
    dates: CoachCalendarDates,
    distanceLabel: (Double) -> String,
    strengthAction: String,
) {
    val isToday = d == dates.today()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "${d.getDayOfWeek().name.lowercase().replaceFirstChar{it.uppercase()}} · ${dates.label(d)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color =
                    if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            if (isToday)
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clearAndSetSemantics { contentDescription = "Today" },
                ) {
                    Text(
                        "NOW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
        }
        Panel(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
            if (entries.isEmpty())
                Text(
                    "No session planned",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            entries.forEach { entry ->
                CalendarEntryRow(
                    s,
                    d,
                    entry,
                    workout,
                    strength,
                    activity,
                    distanceLabel,
                    strengthAction,
                )
            }
        }
    }
}

@Composable
private fun CalendarEntryRow(
    s: CoachUiState,
    d: LocalDate,
    entry: CalendarData.Entry,
    workout: (String) -> Unit,
    strength: (String) -> Unit,
    activity: (String) -> Unit,
    distanceLabel: (Double) -> String,
    strengthAction: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SessionDisc(entry.glyph(), entry.discipline() ?: Discipline.RECOVERY, entry.tone(), 36.dp)
        val status =
            when (entry.state()) {
                CalendarData.State.COMPLETED -> "Completed"
                CalendarData.State.NOT_COMPLETED -> "Not completed"
                CalendarData.State.SKIPPED -> "Skipped by choice"
                CalendarData.State.MOVED -> "Moved"
                else -> "Planned"
            }
        val volume =
            if (
                entry.category() == CalendarData.Category.STRENGTH ||
                    entry.category() == CalendarData.Category.REST
            )
                null
            else {
                val recorded = s.activities.firstOrNull { it.id() == entry.activityId() }
                val planned =
                    (s.currentPlan?.plan()?.workouts() ?: s.plannedWorkouts).firstOrNull {
                        it.id() == entry.id()
                    }
                recorded?.let {
                    it.distanceMetres()?.let { metres -> distanceLabel(metres) }
                        ?: "${it.elapsedTime().getSeconds() / 60} min"
                }
                    ?: planned?.let {
                        it.distanceKm()?.let { km -> distanceLabel(km * 1000) }
                            ?: it.duration()?.let { time -> "${time.getSeconds() / 60} min" }
                    }
            }
        val text = "${entry.label()}${volume?.let{" · $it"}?:""} · $status"
        val muted =
            entry.state() == CalendarData.State.NOT_COMPLETED ||
                entry.state() == CalendarData.State.SKIPPED ||
                entry.category() == CalendarData.Category.REST
        val plain: @Composable () -> Unit = {
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (muted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            )
        }
        val action: (@Composable (() -> Unit, String) -> Unit) = { onClick, label ->
            TextButton(
                onClick = onClick,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    color =
                        if (muted) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                GlyphIcon(Glyph.CHEVRON, MaterialTheme.colorScheme.onSurfaceVariant, 18.dp)
            }
        }
        when {
            entry.activityId() != null ->
                action({ activity(requireNotNull(entry.activityId())) }, text)
            entry.category() == CalendarData.Category.STRENGTH &&
                entry.state() == CalendarData.State.PLANNED ->
                action({ strength(d.toString()) }, "$text · $strengthAction")
            entry.category() == CalendarData.Category.STRENGTH -> plain()
            entry.id() != null && entry.state() != CalendarData.State.SKIPPED ->
                action({ workout(requireNotNull(entry.id())) }, text)
            else -> plain()
        }
    }
}

/** Required real platform calendar/UK labels. No cached zone or invented date defaults. */
interface CoachCalendarDates {
    fun today(): LocalDate

    fun parse(value: String): LocalDate

    fun shiftMonth(date: LocalDate, months: Long): LocalDate

    fun monthStart(date: LocalDate): LocalDate

    fun weekStart(date: LocalDate): LocalDate

    fun label(date: LocalDate): String

    fun monthLabel(date: LocalDate): String
}
