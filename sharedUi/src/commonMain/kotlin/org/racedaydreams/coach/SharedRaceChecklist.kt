package org.racedaydreams.coach

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    ExpressiveSection(
        "Race morning checklist",
        Glyph.LIST,
        ChartHue.BLUE,
        caption = "Use the items relevant to your race. No extra training is required.",
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        val saved = values
        if (saved == null && error == null) QuietNote("Loading checklist…")
        if (saved == null && error != null) {
            TextButton(onClick = { attempt++ }) { Text("Retry checklist") }
        }
        if (saved != null)
            Column {
                raceChecklistLabels.forEach { label ->
                    val checked = saved[label] == true
                    CheckRow(label, checked, enabled = !busy) { selected ->
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
                    }
                }
            }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
