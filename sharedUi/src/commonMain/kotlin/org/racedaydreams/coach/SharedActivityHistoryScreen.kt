@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.ZoneOffset

@Composable
fun SharedActivityHistoryScreen(
    state: CoachUiState,
    open: (String) -> Unit,
    allStrength: () -> Unit,
    loadSummaryPage: suspend (Instant?, String?, Int) -> List<ActivityRecord>,
    activityDate: (ActivityRecord) -> String,
    strengthDate: (Instant, ZoneOffset) -> String,
    distance: (Double) -> String,
    elapsedPace: (ActivityRecord, Boolean) -> String?,
    emptyMessage: String,
) {
    var page by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf<List<ActivityRecord>>(emptyList()) }
    var hasMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var loadedSource by remember { mutableStateOf<List<ActivityRecord>?>(null) }
    LaunchedEffect(state.activities, page) {
        val requestedSource = state.activities
        val append = loadedSource == requestedSource
        if (!append) visible = emptyList()
        loading = true
        error = false
        try {
            val last = if (append) visible.lastOrNull() else null
            // The optional suspend boundary permits deterministic interrupted-query tests;
            // production continues to read the same bounded, summary-only page on IO.
            val found = loadSummaryPage(last?.start(), last?.id(), 31)
            visible = if (append) visible + found.take(30) else found.take(30)
            hasMore = found.size > 30
            loadedSource = requestedSource
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = true
        } finally {
            // A cancelled predecessor can finish after its replacement has started.
            // It must not advertise that replacement's still-pending page as ready.
            if (currentCoroutineContext().isActive) loading = false
        }
    }
    val groups = remember(visible) { visible.groupBy(activityDate) }
    // A parent refresh can replace the source before LaunchedEffect starts its new
    // query. Do not advertise the old page as ready during that transition.
    val historyPending = loading || state.busy || (!error && loadedSource != state.activities)
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
        LazyColumn(
            Modifier.fillMaxHeight().widthIn(max = 840.dp).fillMaxWidth().semantics {
                stateDescription =
                    if (historyPending) "Loading recorded history"
                    else if (error) "History unavailable" else "Recorded history ready"
            },
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CoachHeading("Activity calendar")
                Text(
                    "Recorded runs · list view",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "Your week can change. Recorded activities are retained independently of planned workouts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (state.strengthSessions.isNotEmpty())
                item { SectionLabel("Strength history", Glyph.DUMBBELL, ChartHue.PLUM) }
            items(
                state.strengthSessions.sortedByDescending { it.startedAt() }.take(20),
                key = { "strength:" + it.id() },
            ) { session ->
                RecordedWorkoutSummaryRow(Discipline.STRENGTH, Glyph.DUMBBELL) {
                    Text(
                        strengthDate(session.startedAt(), session.offset()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Strength workout",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val exercises = session.exercises().size
                    Text(
                        "${session.duration().getSeconds() / 60} min · $exercises ${if (exercises == 1) "exercise" else "exercises"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    session.exercises().forEach { RowDetail(it.exercise().name()) }
                    RowDetail("Completed")
                }
            }
            if (state.strengthSessions.size > 20)
                item {
                    TextButton(onClick = allStrength, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("View all strength history")
                    }
                }
            if (error)
                item {
                    Text("History could not be loaded. Your records are retained.")
                    TextButton(onClick = { page++ }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Try again")
                    }
                }
            if (groups.isEmpty() && !historyPending && !error)
                item {
                    RecordedWorkoutSummaryRow(Discipline.RUNNING, Glyph.RUN) {
                        RowDetail(emptyMessage, MaterialTheme.colorScheme.onSurface)
                    }
                }
            if (groups.isNotEmpty())
                item(key = "runs-label") { SectionLabel("Recorded runs", Glyph.RUN, ChartHue.BLUE) }
            groups.forEach { (date, activities) ->
                item(key = "date:$date") { HistoryDateLabel(date) }
                items(activities, key = { it.id() }) { activity ->
                    RecordedWorkoutRow(
                        Discipline.RUNNING,
                        Glyph.RUN,
                        onClick = { open(activity.id()) },
                    ) {
                        Text(
                            if (activity.source().provider() == "assessment") "Assessment run"
                            else "Recorded run",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        val pace =
                            elapsedPace(
                                activity,
                                state.profile?.preferences()?.units() == UnitSystem.IMPERIAL,
                            )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            HistoryFigure(
                                activity.distanceMetres()?.let { distance(it) }
                                    ?: "Distance unavailable",
                                "distance",
                            )
                            HistoryFigure(
                                PaceCalculator.formatTime(activity.elapsedTime()),
                                "elapsed",
                            )
                            pace?.let { HistoryFigure(it, "elapsed average pace") }
                        }
                        RowDetail(
                            "Source: ${org.racedaydreams.domain.activity.ActivitySources.friendly(activity.source())}"
                        )
                        val score =
                            remember(activity, state.runResponses, state.planVersions) {
                                val response =
                                    state.runResponses.firstOrNull {
                                        it.activityId() == activity.id()
                                    }
                                val prescription =
                                    state.planVersions
                                        .firstOrNull { it.id() == response?.planVersionId() }
                                        ?.plan()
                                        ?.workouts()
                                        ?.firstOrNull { it.id() == response?.plannedWorkoutId() }
                                if (prescription == null) null
                                else
                                    org.racedaydreams.domain.coaching.RunScoreEngine.analyse(
                                            prescription,
                                            activity,
                                            response,
                                        )
                                        .score()
                            }
                        score?.let { RowDetail("Run Score $it/10") }
                        RowDetail("Completed · tap for details")
                    }
                }
            }
            if (historyPending) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            else if (hasMore)
                item {
                    TextButton(
                        onClick = { page++ },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("Load more recorded runs")
                    }
                }
        }
    }
}

/** A history figure: the value in bold with its plain-word measure after it, read as one phrase. */
@Composable
private fun HistoryFigure(value: String, measure: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(value) }
            append(" ")
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                )
            ) {
                append(measure)
            }
        },
        style = MaterialTheme.typography.titleMedium,
    )
}
