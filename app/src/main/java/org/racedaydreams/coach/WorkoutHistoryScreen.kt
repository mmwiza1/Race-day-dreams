@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.racedaydreams.data.WorkoutHistoryStore
import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.activity.WorkoutHistoryQuery

@Composable
fun WorkoutHistoryScreen(
    state: CoachUiState,
    openRun: (String) -> Unit,
    openStrength: (String) -> Unit,
    initialKind: String = "ALL",
) {
    var kind by rememberSaveable { mutableStateOf(initialKind) }
    var query by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("") }
    var from by rememberSaveable { mutableStateOf("") }
    var through by rememberSaveable { mutableStateOf("") }
    var more by rememberSaveable { mutableIntStateOf(20) }
    val prescriptions =
        remember(state.planVersions) {
            state.planVersions.flatMap { it.plan().workouts() }.distinctBy { it.id() }
        }
    val context = LocalContext.current
    var rows by remember { mutableStateOf<List<WorkoutHistoryStore.Row>>(emptyList()) }
    var next by remember { mutableStateOf<WorkoutHistoryStore.Cursor?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadedKey by remember { mutableStateOf<List<Any?>?>(null) }
    val queryKey =
        listOf(
            kind,
            query,
            type,
            from,
            through,
            state.activities,
            state.strengthSessions,
            state.planVersions,
        )
    LaunchedEffect(queryKey, more) {
        val append = loadedKey == queryKey
        if (!append) {
            rows = emptyList()
            next = null
            loadedKey = queryKey
        }
        loading = true
        error = null
        try {
            val result =
                withContext(Dispatchers.IO) {
                    WorkoutHistoryStore(context)
                        .page(
                            WorkoutHistoryQuery.Kind.valueOf(kind),
                            type.takeIf { it.isNotBlank() }?.let(WorkoutCategory::valueOf),
                            query,
                            from.takeIf { it.isNotBlank() }?.let(LocalDate::parse),
                            through.takeIf { it.isNotBlank() }?.let(LocalDate::parse),
                            prescriptions,
                            if (append) next else null,
                            versions = state.planVersions,
                        )
                }
            rows = if (append) rows + result.rows else result.rows
            next = result.next
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error =
                "History could not be loaded. Check the date range and try again; your records are retained."
        } finally {
            loading = false
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // Filters share the list's scroll viewport, so expanded controls do not hide history at
        // 200%.
        LazyColumn(
            Modifier.fillMaxHeight().widthIn(max = 840.dp).fillMaxWidth().imePadding(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                // The header takes the colour of the selected kind, as Progress does for its tab.
                val strengthOnly = kind == WorkoutHistoryQuery.Kind.STRENGTH.name
                ScreenHeader(
                    if (strengthOnly) Discipline.STRENGTH else Discipline.RUNNING,
                    if (strengthOnly) Glyph.DUMBBELL else Glyph.LIST,
                    "Your recorded training",
                    "Workout history",
                )
            }
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                    verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                ) {
                    WorkoutHistoryQuery.Kind.values().forEach { k ->
                        CoachFilterChip(
                            selected = kind == k.name,
                            onClick = {
                                kind = k.name
                                more = 20
                            },
                            label = { Text(k.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
            }
            item {
                var filters by rememberSaveable { mutableStateOf(false) }
                TextButton(
                    onClick = { filters = !filters },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Search and date filters")
                }
                if (filters) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CoachInput(
                            "Strength exercise or alias",
                            query,
                            {
                                query = it
                                more = 20
                            },
                        )
                        DateInput(
                            "From (optional)",
                            from,
                            {
                                from = it
                                more = 20
                            },
                        )
                        DateInput(
                            "Through (optional)",
                            through,
                            {
                                through = it
                                more = 20
                            },
                        )
                        var types by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { types = true }) {
                                Text(
                                    type
                                        .takeIf { it.isNotBlank() }
                                        ?.let { workoutName(WorkoutCategory.valueOf(it)) }
                                        ?: "All run types"
                                )
                            }
                            DropdownMenu(expanded = types, onDismissRequest = { types = false }) {
                                DropdownMenuItem(
                                    text = { Text("All run types") },
                                    onClick = {
                                        type = ""
                                        types = false
                                    },
                                )
                                WorkoutCategory.values().forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(workoutName(t)) },
                                        onClick = {
                                            type = t.name
                                            types = false
                                            more = 20
                                        },
                                    )
                                }
                            }
                        }
                        TextButton(
                            onClick = {
                                query = ""
                                type = ""
                                from = ""
                                through = ""
                            }
                        ) {
                            Text("Clear filters")
                        }
                    }
                }
            }
            error?.let { message ->
                item {
                    Text(message)
                    TextButton(onClick = { more++ }) { Text("Try again") }
                }
            }
            item {
                Text(
                    "${rows.size} workouts loaded",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (rows.isEmpty() && !loading && error == null)
                item {
                    RecordedWorkoutSummaryRow(Discipline.RUNNING, Glyph.LIST) {
                        RowDetail(
                            "No workouts match. Try a wider range, or record a workout.",
                            MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            // Rows arrive newest first; group them under month labels. Each row keeps its own date,
            // so a row is still found and announced by its day.
            rows
                .groupBy { java.time.YearMonth.from(it.date) }
                .forEach { (month, entries) ->
                    item(key = "month:$month") {
                        HistoryDateLabel(
                            month.format(
                                java.time.format.DateTimeFormatter.ofPattern(
                                    "MMMM uuuu",
                                    java.util.Locale.UK,
                                )
                            )
                        )
                    }
                    items(entries.size, key = { entries[it].kind.name + entries[it].id }) { index ->
                        WorkoutHistoryRow(entries[index], state, openRun, openStrength)
                    }
                }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            else if (next != null)
                item {
                    TextButton(
                        onClick = { more++ },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("Load more")
                    }
                }
        }
    }
}

@Composable
private fun WorkoutHistoryRow(
    entry: WorkoutHistoryStore.Row,
    state: CoachUiState,
    openRun: (String) -> Unit,
    openStrength: (String) -> Unit,
) {
    val running = entry.kind == WorkoutHistoryQuery.Kind.RUNNING
    RecordedWorkoutRow(
        if (running) Discipline.RUNNING else Discipline.STRENGTH,
        if (running) Glyph.RUN else Glyph.DUMBBELL,
        onClick = { if (running) openRun(entry.id) else openStrength(entry.id) },
    ) {
        Text(
            entry.label.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            friendlyDate(entry.date),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        entry.distanceMetres?.let {
            Text(
                formatDistance(it, state),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        entry.minutes?.let {
            Text(
                "${entry.exerciseCount} exercises · $it min",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        if (running && entry.distanceMetres == null) RowDetail("Distance unavailable")
        Text(
            "View recorded workout",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
