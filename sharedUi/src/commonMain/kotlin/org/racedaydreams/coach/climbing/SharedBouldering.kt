@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach.climbing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.racedaydreams.coach.*
import org.racedaydreams.domain.climbing.*
import org.racedaydreams.domain.presentation.DashboardAnalytics.Period
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate

/** "1 h 25 min" / "45 min". */
fun climbingTime(seconds: Long): String {
    val minutes = (seconds + 30) / 60
    return if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
}

private fun gradeText(grade: ClimbingGrade?, preferred: GradeSystem): String =
    when {
        grade == null -> "Ungraded"
        grade.system == preferred -> grade.label
        else -> "${grade.label} (≈ ${ClimbingGrade.approximately(grade, preferred).label})"
    }

private fun problemSummary(problem: ClimbingProblem) =
    "${problem.outcome.label} · ${plural(problem.attempts, "attempt")}"

@Composable
private fun BackRow(onBack: () -> Unit) {
    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
}

@Composable
private fun Feedback(state: BoulderingState) {
    state.error?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    state.message?.let {
        Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
}

/** Bouldering overview: start or resume a session, period analytics and recent sessions. */
@Composable
fun SharedBoulderingHome(
    model: SharedBoulderingCoordinator,
    today: LocalDate,
    friendlyDate: (LocalDate) -> String,
    onBack: () -> Unit,
    openSession: () -> Unit,
    openDetail: (String) -> Unit,
    openHistory: () -> Unit,
) = CoachPage {
    val state by model.state.collectAsState()
    LaunchedEffect(model) { model.refresh() }
    var periodName by rememberSaveable { mutableStateOf(Period.FOUR_WEEKS.name) }
    val period = Period.valueOf(periodName)
    val summary =
        remember(state.sessions, today, period, state.gradeSystem) {
            ClimbingAnalytics.summary(state.sessions, today, period, state.gradeSystem)
        }
    BackRow(onBack)
    val draft = state.draft
    // Same composition as the Strength tab: one hero carries starting or resuming a session.
    ScreenHero(
        Discipline.STRENGTH,
        Glyph.TREND,
        "Climbing",
        "Bouldering",
        when {
            draft != null ->
                "Unsaved session · ${if (draft.live()) "In progress since" else "Logged for"} ${friendlyDate(draft.startedAt.let { org.racedaydreams.domain.time.CalendarDates.atOffset(it, draft.offset) })} · " +
                    plural(draft.problems.size, "problem")
            // Starting is only offered once an interrupted session on this device would be found.
            state.loading -> "Checking for an unsaved session…"
            else -> "Problems, grades and attempts, kept separate from weightlifting."
        },
        primary =
            when {
                draft != null -> "Resume session" to openSession
                state.loading -> null
                else ->
                    "Start bouldering session" to
                        {
                            model.startLive()
                            openSession()
                        }
            },
        secondary =
            if (draft == null && !state.loading)
                listOf(
                    "Log a past session" to
                        {
                            model.startPast(today)
                            openSession()
                        }
                )
            else emptyList(),
    )
    Feedback(state)

    val highest = summary.highestSend[state.gradeSystem]
    val otherSystem = GradeSystem.values().first { it != state.gradeSystem }
    val otherHighest = summary.highestSend[otherSystem]
    SectionLabel("Your climbing", Glyph.VOLUME, ChartHue.PLUM, period.label)
    PeriodSelector(period) { periodName = it.name }
    Panel {
        KpiGrid(
            listOf(
                Kpi("Sessions", summary.sessions.toString(), period.label),
                Kpi(
                    "Climbing time",
                    if (summary.sessions == 0) "None" else climbingTime(summary.climbingSeconds),
                    "Recorded session time",
                ),
                Kpi(
                    "Problems",
                    summary.problems.toString(),
                    "${plural(summary.attempts, "attempt")} in total",
                ),
                Kpi(
                    "Sends",
                    summary.sends.toString(),
                    "${summary.flashes} ${if (summary.flashes == 1) "flash" else "flashes"} included",
                ),
                Kpi(
                    "Highest send",
                    highest?.label ?: otherHighest?.label ?: "None yet",
                    when {
                        highest != null -> state.gradeSystem.label
                        otherHighest != null -> "${otherSystem.label} (as entered)"
                        else -> "Sent problems with a grade"
                    },
                ),
                Kpi("Flashes", summary.flashes.toString(), "Sent on the first attempt"),
            )
        )
    }

    ExpressiveSection(
        "Grade distribution",
        Glyph.VOLUME,
        ChartHue.PLUM,
        caption = "${state.gradeSystem.label} problems tried · ${period.label}",
    ) {
        if (summary.distribution.isEmpty())
            Text("No ${state.gradeSystem.label} problems in this period.")
        else GradeDistribution(summary.distribution)
        val excluded =
            listOfNotNull(
                summary.otherSystemProblems
                    .takeIf { it > 0 }
                    ?.let { "${plural(it, "problem")} graded in ${otherSystem.label}" },
                summary.ungradedProblems
                    .takeIf { it > 0 }
                    ?.let { "${plural(it, "ungraded problem")}" },
            )
        if (excluded.isNotEmpty())
            Text(
                "Not converted or shown here: ${excluded.joinToString(" and ")}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }

    ExpressiveSection(
        "Hardest send by week",
        Glyph.TREND,
        ChartHue.PLUM,
        caption = "${state.gradeSystem.label} · ${period.label} · this week in progress",
    ) {
        if (summary.progression.none { it.highestSend != null })
            Text("Send a graded problem to see your progression.")
        else Progression(summary.progression, state.gradeSystem, friendlyDate)
    }

    ExpressiveSection(
        "Recent sessions",
        Glyph.LIST,
        ChartHue.PLUM,
        trailing =
            if (state.sessions.isNotEmpty()) {
                { TextButton(onClick = openHistory) { Text("See all") } }
            } else null,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
    ) {
        if (summary.recent.isEmpty())
            Text("No bouldering sessions recorded yet.", Modifier.padding(vertical = 12.dp))
        for (session in summary.recent) SessionRow(session, state.gradeSystem, friendlyDate) {
            openDetail(session.id)
        }
    }

    ExpressiveSection("Grades", Glyph.LIST, ChartHue.PLUM) {
        GradeSystemChooser(state.gradeSystem) { model.setGradeSystem(it) }
        Text(
            "Grades are always kept as you entered them. Conversions between V-scale and Fontainebleau are approximate.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Explainer(
            "How bouldering is counted",
            "Bouldering is recorded separately from weightlifting. Problems are never counted as working sets, used for estimated 1RM or lifting records, or added to muscle-group workload. A send includes flashes; a flash is a send on the first attempt of the session.",
        )
    }
}

@Composable
private fun GradeSystemChooser(selected: GradeSystem, choose: (GradeSystem) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (system in GradeSystem.values()) FilterChip(
            selected = system == selected,
            onClick = { choose(system) },
            label = { Text(system.label) },
            modifier = Modifier.heightIn(min = 48.dp).testTag("grade-system-${system.name}"),
        )
    }
}

@Composable
private fun SessionRow(
    session: ClimbingSession,
    preferred: GradeSystem,
    friendlyDate: (LocalDate) -> String,
    open: () -> Unit,
) {
    val hardest =
        session.problems
            .filter { it.sent() && it.grade != null }
            .mapNotNull { it.grade }
            .groupBy { it.system }
            .let { groups ->
                groups[preferred]?.maxByOrNull { it.rank }
                    ?: groups.values.flatten().maxByOrNull { it.rank }
            }
    DashboardRow(
        title = session.location.ifBlank { "Bouldering" },
        detail = "${friendlyDate(session.date())} · ${climbingTime(session.durationSeconds)}",
        value = "${session.sends()} / ${session.problems.size} sent",
        secondary = hardest?.let { "Hardest ${gradeText(it, preferred)}" },
        onClick = open,
    )
}

@Composable
private fun GradeDistribution(rows: List<ClimbingAnalytics.GradeCount>) {
    val most = rows.maxOf { it.attempted }.coerceAtLeast(1)
    val tones = chartAccent(ChartHue.PLUM)
    val tried = tones.soft.copy(alpha = 0.3f)
    val sent = tones.strong
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in rows) Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                    contentDescription =
                        "${row.grade.label}: ${row.sent} of ${plural(row.attempted, "problem")} sent"
                },
        ) {
            Text(row.grade.label, modifier = Modifier.widthIn(min = 44.dp))
            Canvas(Modifier.weight(1f).height(18.dp)) {
                val full = size.width * row.attempted / most
                drawRoundRect(tried, Offset.Zero, Size(full, size.height), CornerRadius(9f, 9f))
                drawRoundRect(
                    sent,
                    Offset.Zero,
                    Size(size.width * row.sent / most, size.height),
                    CornerRadius(9f, 9f),
                )
            }
            Text(
                "${row.sent}/${row.attempted}",
                modifier = Modifier.padding(start = 8.dp).widthIn(min = 40.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text("Filled: sent · light: tried but not sent", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Progression(
    weeks: List<ClimbingAnalytics.WeekPoint>,
    system: GradeSystem,
    friendlyDate: (LocalDate) -> String,
) {
    val labels = ClimbingGrade.labels(system)
    val ranks = weeks.mapNotNull { it.highestSend?.rank }
    val top = (ranks.maxOrNull() ?: 0) + 1
    val bottom = ((ranks.minOrNull() ?: 0) - 1).coerceAtLeast(0)
    val bar = chartAccent(ChartHue.PLUM).strong
    val description =
        weeks
            .filter { it.highestSend != null }
            .joinToString("; ") {
                "Week of ${friendlyDate(it.weekStart)}: ${it.highestSend!!.label}"
            }
    Text(
        "Hardest: ${labels[(ranks.maxOrNull() ?: 0)]} · scale ${labels[bottom]}–${labels[top.coerceAtMost(labels.lastIndex)]}",
        style = MaterialTheme.typography.bodySmall,
    )
    Canvas(
        Modifier.fillMaxWidth()
            .height(120.dp)
            .testTag("climbing-progression")
            .clearAndSetSemantics { contentDescription = "Hardest send by week. $description" }
    ) {
        val cell = size.width / weeks.size
        val span = (top - bottom).coerceAtLeast(1)
        weeks.forEachIndexed { index, week ->
            val rank = week.highestSend?.rank ?: return@forEachIndexed
            val height = size.height * (rank - bottom + 1) / (span + 1)
            val width = (cell * 0.6f).coerceAtMost(28.dp.toPx())
            drawRoundRect(
                bar,
                Offset(cell * index + (cell - width) / 2, size.height - height),
                Size(width, height),
                CornerRadius(width / 2, width / 2),
            )
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(friendlyDate(weeks.first().weekStart), style = MaterialTheme.typography.bodySmall)
        Text(friendlyDate(weeks.last().weekStart), style = MaterialTheme.typography.bodySmall)
    }
}

/** Logging problems for the unsaved session (live or past) or a saved session being corrected. */
@Composable
fun SharedBoulderingSession(
    model: SharedBoulderingCoordinator,
    now: () -> Instant,
    friendlyDate: (LocalDate) -> String,
    onBack: () -> Unit,
    openReview: () -> Unit,
) = CoachPage {
    val state by model.state.collectAsState()
    val target = state.target()
    BackRow(onBack)
    if (target == null) {
        CoachHeading("Bouldering session")
        Text("There is no session in progress.")
        return@CoachPage
    }
    val editing = state.editing != null
    CoachHeading(if (editing) "Correct session" else "Bouldering session")
    Feedback(state)
    if (target.live()) {
        var elapsed by remember { mutableStateOf(target.elapsedSeconds(now())) }
        // Ticks only while this screen is shown; nothing runs in the background.
        LaunchedEffect(target.id, target.startedAt) {
            while (true) {
                elapsed = target.elapsedSeconds(now())
                delay(1_000)
            }
        }
        HeroSurface(Discipline.STRENGTH, Glyph.TREND, "Live session") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Climbing for", style = MaterialTheme.typography.titleMedium)
                Text(
                    climbingTime(elapsed),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("climbing-elapsed"),
                )
                Text(
                    "Started ${friendlyDate(org.racedaydreams.domain.time.CalendarDates.atOffset(target.startedAt, target.offset))}",
                    color = LocalContentColor.current.copy(alpha = 0.85f),
                )
            }
        }
    } else {
        Text(
            "${friendlyDate(org.racedaydreams.domain.time.CalendarDates.atOffset(target.startedAt, target.offset))} · ${climbingTime(target.durationSeconds ?: 0)}",
            style = MaterialTheme.typography.titleMedium,
        )
    }
    CoachTextInput("Climbing gym or location (optional)", target.location, model::setLocation)

    var editingProblem by remember { mutableStateOf<ClimbingProblem?>(null) }
    var adding by remember { mutableStateOf(false) }
    ExpressiveSection("Problems", Glyph.LIST, ChartHue.PLUM) {
        Text(
            "${plural(target.problems.size, "problem")} · ${target.problems.count { it.sent() }} sent · " +
                "${target.problems.count { it.outcome == ClimbOutcome.FLASH }} flashed",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("climbing-problem-summary"),
        )
        if (target.problems.isEmpty())
            Text("Add each problem you try, with its grade and attempts.")
        for (problem in target.problems) DashboardRow(
            title =
                gradeText(problem.grade, state.gradeSystem) +
                    if (problem.name.isNotBlank()) " · ${problem.name}" else "",
            detail = problemSummary(problem),
            value = "",
            secondary = "Edit",
            onClick = { editingProblem = problem },
        )
        Button(
            onClick = { adding = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("climbing-add-problem"),
        ) {
            Text("Add problem")
        }
    }
    OutlinedTextField(
        target.notes,
        model::setNotes,
        label = { Text("Notes (optional)") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
    )
    Button(
        onClick = {
            model.stopTimer()
            openReview()
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Text(if (editing) "Review corrections" else "Finish and review")
    }
    var confirmDiscard by remember { mutableStateOf(false) }
    if (editing)
        TextButton(
            onClick = {
                model.cancelEdit()
                onBack()
            },
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text("Cancel corrections")
        }
    else
        TextButton(onClick = { confirmDiscard = true }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Discard session")
        }
    if (confirmDiscard)
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard this session?") },
            text = { Text("Its ${plural(target.problems.size, "problem")} will not be saved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDiscard = false
                        model.discardDraft()
                        onBack()
                    }
                ) {
                    Text("Discard")
                }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep") } },
        )
    val shown = editingProblem
    if (adding || shown != null)
        ProblemEditor(
            initial = shown,
            preferred = state.gradeSystem,
            newId = model::newProblemId,
            save = {
                model.saveProblem(it)
                adding = false
                editingProblem = null
            },
            remove =
                shown?.let { p ->
                    {
                        model.removeProblem(p.id)
                        editingProblem = null
                    }
                },
            dismiss = {
                adding = false
                editingProblem = null
            },
        )
}

/** Accessible problem editor: grade (or ungraded), attempts and outcome, with validation. */
@Composable
private fun ProblemEditor(
    initial: ClimbingProblem?,
    preferred: GradeSystem,
    newId: () -> String,
    save: (ClimbingProblem) -> Unit,
    remove: (() -> Unit)?,
    dismiss: () -> Unit,
) {
    var system by remember { mutableStateOf(initial?.grade?.system ?: preferred) }
    var grade by remember { mutableStateOf(initial?.grade?.label) }
    var attempts by remember { mutableStateOf(initial?.attempts ?: 1) }
    var outcome by remember { mutableStateOf(initial?.outcome ?: ClimbOutcome.SEND) }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    if (outcome == ClimbOutcome.FLASH && attempts > 1) outcome = ClimbOutcome.SEND
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(if (initial == null) "Add problem" else "Edit problem") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Grade system", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (s in GradeSystem.values()) FilterChip(
                        selected = s == system,
                        onClick = {
                            if (s != system) {
                                system = s
                                grade = null
                            }
                        },
                        label = { Text(s.label) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
                Text("Grade", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    FilterChip(
                        selected = grade == null,
                        onClick = { grade = null },
                        label = { Text("Ungraded") },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                    for (label in ClimbingGrade.labels(system)) FilterChip(
                        selected = grade == label,
                        onClick = { grade = label },
                        label = { Text(label) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("grade-$label"),
                    )
                }
                Text("Attempts", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { attempts = (attempts - 1).coerceAtLeast(1) },
                        enabled = attempts > 1,
                        modifier =
                            Modifier.heightIn(min = 48.dp).semantics {
                                contentDescription = "Fewer attempts"
                            },
                    ) {
                        Text("−")
                    }
                    Text(
                        attempts.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        modifier =
                            Modifier.padding(horizontal = 16.dp)
                                .testTag("climbing-attempts")
                                .semantics { contentDescription = plural(attempts, "attempt") },
                    )
                    OutlinedButton(
                        onClick = {
                            attempts = (attempts + 1).coerceAtMost(ClimbingProblem.MAX_ATTEMPTS)
                        },
                        modifier =
                            Modifier.heightIn(min = 48.dp).semantics {
                                contentDescription = "More attempts"
                            },
                    ) {
                        Text("+")
                    }
                }
                Text("Outcome", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (o in ClimbOutcome.values()) FilterChip(
                        selected = o == outcome,
                        enabled = o != ClimbOutcome.FLASH || attempts == 1,
                        onClick = { outcome = o },
                        label = { Text(o.label) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("outcome-${o.name}"),
                    )
                }
                if (attempts > 1)
                    Text(
                        "A flash is a send on the first attempt.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                OutlinedTextField(
                    name,
                    { if (it.length <= ClimbingProblem.MAX_NAME) name = it },
                    label = { Text("Name or colour (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    save(
                        ClimbingProblem(
                            initial?.id ?: newId(),
                            grade?.let { ClimbingGrade(system, it) },
                            attempts,
                            outcome,
                            name.trim(),
                        )
                    )
                },
                modifier = Modifier.testTag("climbing-save-problem"),
            ) {
                Text("Save problem")
            }
        },
        dismissButton = {
            Row {
                if (remove != null) TextButton(onClick = remove) { Text("Remove") }
                TextButton(onClick = dismiss) { Text("Cancel") }
            }
        },
    )
}

/** Review and correct date, time, location and notes before saving. */
@Composable
fun SharedBoulderingReview(
    model: SharedBoulderingCoordinator,
    now: () -> Instant,
    friendlyDate: (LocalDate) -> String,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
) = CoachPage {
    val state by model.state.collectAsState()
    val target = state.target()
    BackRow(onBack)
    CoachHeading("Review session")
    if (target == null) {
        Text("There is no session to review.")
        return@CoachPage
    }
    Feedback(state)
    val date = org.racedaydreams.domain.time.CalendarDates.atOffset(target.startedAt, target.offset)
    Panel {
        Text("Date", style = MaterialTheme.typography.labelLarge)
        Text(
            friendlyDate(date),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("climbing-review-date"),
        )
        Row {
            TextButton(
                onClick = { model.shiftDate(-1) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("‹ Earlier day")
            }
            TextButton(
                onClick = { model.shiftDate(1) },
                enabled =
                    date.toEpochDay() <
                        org.racedaydreams.domain.time.CalendarDates.atOffset(now(), target.offset)
                            .toEpochDay(),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Later day ›")
            }
        }
        var minutes by
            remember(target.id) {
                mutableStateOf(
                    ((target.durationSeconds ?: target.elapsedSeconds(now())) / 60).toString()
                )
            }
        OutlinedTextField(
            minutes,
            { value ->
                val digits = value.filter { it.isDigit() }.take(4)
                minutes = digits
                digits.toLongOrNull()?.let { model.setDurationMinutes(it) }
            },
            label = { Text("Climbing time (minutes)") },
            singleLine = true,
            keyboardOptions =
                androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
            modifier = Modifier.fillMaxWidth().testTag("climbing-minutes"),
        )
    }
    CoachTextInput("Climbing gym or location (optional)", target.location, model::setLocation)
    ExpressiveSection("Problems", Glyph.LIST, ChartHue.PLUM) {
        Text(
            "${plural(target.problems.size, "problem")} · ${target.problems.count { it.sent() }} sent · " +
                "${target.problems.count { it.outcome == ClimbOutcome.FLASH }} flashed · " +
                "${plural(target.problems.sumOf { it.attempts }, "attempt")}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        for (problem in target.problems) Text(
            "${gradeText(problem.grade, state.gradeSystem)} — ${problemSummary(problem)}" +
                if (problem.name.isNotBlank()) " · ${problem.name}" else ""
        )
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Change problems")
        }
    }
    OutlinedTextField(
        target.notes,
        model::setNotes,
        label = { Text("Notes (optional)") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
    )
    Button(
        onClick = { model.save(onSaved) },
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("climbing-save-session"),
    ) {
        Text(if (state.editing != null) "Save corrections" else "Save session")
    }
}

/** A saved session with its problems; correct or delete it. */
@Composable
fun SharedBoulderingDetail(
    model: SharedBoulderingCoordinator,
    sessionId: String,
    friendlyDate: (LocalDate) -> String,
    onBack: () -> Unit,
    openCorrection: () -> Unit,
) = CoachPage {
    val state by model.state.collectAsState()
    LaunchedEffect(model) { if (state.loading) model.refresh() }
    BackRow(onBack)
    val session = state.sessions.firstOrNull { it.id == sessionId }
    if (session == null) {
        CoachHeading("Bouldering session")
        Text(if (state.loading) "Loading…" else "This session is no longer saved.")
        return@CoachPage
    }
    CoachHeading(session.location.ifBlank { "Bouldering session" })
    Feedback(state)
    Text("${friendlyDate(session.date())} · ${climbingTime(session.durationSeconds)}")
    Panel {
        KpiGrid(
            listOf(
                Kpi(
                    "Problems",
                    session.problems.size.toString(),
                    plural(session.attempts(), "attempt"),
                ),
                Kpi("Sends", session.sends().toString(), "${session.flashes()} flashed"),
            )
        )
    }
    ExpressiveSection("Problems", Glyph.LIST, ChartHue.PLUM) {
        if (session.problems.isEmpty()) Text("No problems were recorded.")
        for (problem in session.problems) DashboardRow(
            title =
                gradeText(problem.grade, state.gradeSystem) +
                    if (problem.name.isNotBlank()) " · ${problem.name}" else "",
            detail = problemSummary(problem),
            value = "",
            secondary = null,
            onClick = null,
        )
        if (session.problems.any { it.grade != null && it.grade!!.system != state.gradeSystem })
            Text(
                "≈ values are approximate conversions; the original grade is kept.",
                style = MaterialTheme.typography.bodySmall,
            )
    }
    if (session.notes.isNotBlank())
        ExpressiveSection("Notes", Glyph.LIST, ChartHue.PLUM) { Text(session.notes) }
    OutlinedButton(
        onClick = {
            model.edit(session.id)
            openCorrection()
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Text("Correct session")
    }
    var confirm by remember { mutableStateOf(false) }
    TextButton(onClick = { confirm = true }, modifier = Modifier.heightIn(min = 48.dp)) {
        Text("Delete session")
    }
    if (confirm)
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Delete this session?") },
            text = { Text("This removes the session and its problems from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm = false
                        model.delete(session.id)
                        onBack()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep") } },
        )
}

/** Every saved session, newest first. */
@Composable
fun SharedBoulderingHistory(
    model: SharedBoulderingCoordinator,
    friendlyDate: (LocalDate) -> String,
    onBack: () -> Unit,
    openDetail: (String) -> Unit,
) = CoachPage {
    val state by model.state.collectAsState()
    BackRow(onBack)
    CoachHeading("Bouldering history")
    if (state.sessions.isEmpty()) Text("No bouldering sessions recorded yet.")
    else
        Panel(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
            for (session in state.sessions) SessionRow(session, state.gradeSystem, friendlyDate) {
                openDetail(session.id)
            }
        }
}
