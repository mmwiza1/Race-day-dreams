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
                CoachHeading("Workout history")
                Text(
                    "Your recorded training",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WorkoutHistoryQuery.Kind.values().forEach { k ->
                        FilterChip(
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
                Text("${rows.size} workouts loaded", style = MaterialTheme.typography.labelLarge)
            }
            if (rows.isEmpty() && !loading && error == null)
                item {
                    CoachCard { Text("No workouts match. Try a wider range, or record a workout.") }
                }
            items(rows.size, key = { rows[it].kind.name + rows[it].id }) { index ->
                val entry = rows[index]
                Card(
                    onClick = {
                        if (entry.kind == WorkoutHistoryQuery.Kind.RUNNING) openRun(entry.id)
                        else openStrength(entry.id)
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    shape = MaterialTheme.shapes.large,
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(friendlyDate(entry.date), style = MaterialTheme.typography.labelLarge)
                        Text(
                            entry.label.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        entry.distanceMetres?.let {
                            Text(
                                formatDistance(it, state),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                        entry.minutes?.let {
                            Text(
                                "${entry.exerciseCount} exercises · $it min",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        if (
                            entry.kind == WorkoutHistoryQuery.Kind.RUNNING &&
                                entry.distanceMetres == null
                        )
                            Text(
                                "Distance unavailable",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        Text(
                            "View recorded workout",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
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
