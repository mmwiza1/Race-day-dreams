package org.racedaydreams.coach.application

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.racedaydreams.domain.support.DreamRoutes
import org.racedaydreams.storage.SqliteRunningSupportArchives

@Composable
internal fun AppleDreamSettings(support: SqliteRunningSupportArchives) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }) {
        Text(if (expanded) "Hide Dream Route settings" else "Dream Route settings")
    }
    if (expanded) AppleDreamPreferences(support)
}

@Composable
private fun AppleDreamPreferences(support: SqliteRunningSupportArchives) {
    val scope = rememberCoroutineScope()
    var current by
        remember(support) { mutableStateOf<SqliteRunningSupportArchives.DreamPreferences?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(support, attempt) {
        error = null
        try {
            current = withContext(applicationIoDispatcher()) { support.dreamPreferences() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = "Could not load Dream Route settings."
        }
    }

    fun save(next: SqliteRunningSupportArchives.DreamPreferences) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                withContext(applicationIoDispatcher()) { support.saveDreamPreferences(next) }
                current = next
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = "Could not save Dream Route settings. Try again."
            } finally {
                busy = false
            }
        }
    }

    Text(
        "Choose whether new runs show an equivalent distance on an illustrated marathon course. This is separate from your GPS route."
    )
    val saved = current
    if (saved != null) {
        DreamPreferenceRow("Assign Dream Routes to new runs", saved.enabled, !busy) {
            save(saved.copy(enabled = it))
        }
        if (saved.enabled) {
            DreamRoutes.routes().forEach { city ->
                val id = city.id()
                DreamPreferenceRow(city.name(), id in saved.cities, !busy) {
                    val cities = if (it) saved.cities + id else saved.cities - id
                    save(saved.copy(cities = cities))
                }
            }
            if (saved.cities.isEmpty())
                Text("Select a course to receive an assignment on a new run.")
        }
    } else if (error != null) {
        TextButton(onClick = { attempt++ }) { Text("Retry Dream Route settings") }
    } else Text("Loading Dream Route settings…")
    error?.let { Text(it) }
}

@Composable
private fun DreamPreferenceRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    change: (Boolean) -> Unit,
) {
    Row(
        Modifier.heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = change,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked, onCheckedChange = null, enabled = enabled)
        Text(label)
    }
}
