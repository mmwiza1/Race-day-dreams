package org.racedaydreams.coach

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.racedaydreams.domain.RaceGoal

private val raceChecklistLabels =
    listOf(
        "Shoes & socks",
        "Race number / timing chip",
        "Phone charged",
        "Familiar fuel / hydration plan",
        "Kit & travel arrangements",
    )

@Composable
fun SharedRaceChecklist(
    goal: RaceGoal,
    load: suspend (String) -> Boolean,
    save: suspend (String, String, Boolean) -> Unit,
) {
    val key = "${goal.raceDate()}:${goal.raceName()}"
    val scope = rememberCoroutineScope()
    var values by remember(key) { mutableStateOf<Map<String, Boolean>?>(null) }
    var error by remember(key) { mutableStateOf<String?>(null) }
    var busy by remember(key) { mutableStateOf(false) }
    var attempt by remember(key) { mutableIntStateOf(0) }
    LaunchedEffect(key, attempt) {
        error = null
        try {
            values = raceChecklistLabels.associateWith { load("$key:$it") }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = "Could not load the race checklist."
        }
    }
    SemanticCard(Tile.Race) {
        Text("Race morning checklist", style = MaterialTheme.typography.titleLarge)
        val saved = values
        if (saved == null && error == null) Text("Loading checklist…")
        if (saved == null && error != null) {
            TextButton(onClick = { attempt++ }) { Text("Retry checklist") }
        }
        if (saved != null)
            raceChecklistLabels.forEach { label ->
                val checked = saved[label] == true
                Row(
                    Modifier.heightIn(min = 48.dp)
                        .toggleable(
                            value = checked,
                            enabled = !busy,
                            role = Role.Checkbox,
                            onValueChange = { selected ->
                                if (!busy) {
                                    busy = true
                                    error = null
                                    scope.launch {
                                        try {
                                            save("$key:$label", label, selected)
                                            values = saved + (label to selected)
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (_: Exception) {
                                            error = "Could not save the checklist item. Try again."
                                        } finally {
                                            busy = false
                                        }
                                    }
                                }
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked, onCheckedChange = null, enabled = !busy)
                    Text(label)
                }
            }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Use the items relevant to your race. No extra training is required.")
    }
}
