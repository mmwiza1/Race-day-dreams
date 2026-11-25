package org.racedaydreams.coach

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.racedaydreams.data.DiagnosticReport
import org.racedaydreams.data.RecordingDiagnostics

@Composable
fun DiagnosticExport(state: CoachUiState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var preview by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var sessions by remember { mutableStateOf<List<JSONObject>?>(null) }
    val export =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            val text = pendingExport
            pendingExport = null
            if (uri != null && text != null)
                scope.launch {
                    message =
                        try {
                            withContext(Dispatchers.IO) {
                                requireNotNull(context.contentResolver.openOutputStream(uri, "wt"))
                                    .use { it.write(text.toByteArray(Charsets.UTF_8)) }
                            }
                            "Diagnostic report exported."
                        } catch (_: Exception) {
                            "Diagnostic export could not complete."
                        }
                }
        }
    fun basic() =
        DiagnosticReport.build(context, state.healthAvailability.name, state.healthPermissions)

    SemanticCard(Tile.Neutral) {
        Text("Diagnostics", style = MaterialTheme.typography.titleLarge)
        Text("Basic diagnostics show device, app and permission information.")
        CoachButton("Review basic diagnostics") { preview = basic().toString(2) }
        CoachButton(
            if (sessions == null) "Find recording troubleshooting reports"
            else "Hide recording reports"
        ) {
            if (sessions != null) sessions = null
            else
                scope.launch {
                    sessions =
                        withContext(Dispatchers.IO) { RecordingDiagnostics(context).recent() }
                }
        }
        sessions?.let { runs ->
            Text(
                "Reports cover up to 12 recent recording attempts on this device. They include the session time, GPS delivery and saving evidence. Coordinates, routes and health measurements are excluded."
            )
            if (runs.isEmpty()) Text("No troubleshooting reports are available yet.")
            runs.forEach { run ->
                val whenRecorded =
                    Instant.parse(run.getString("startedAt"))
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm", Locale.UK))
                TextButton(onClick = { preview = basic().put("recording", run).toString(2) }) {
                    Text("Review recording from $whenRecorded")
                }
            }
        }
        message?.let { Text(it) }
    }
    preview?.let { json ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text("Review diagnostic report") },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text(json, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingExport = json
                        preview = null
                        export.launch("race-day-dreams-diagnostic.json")
                    }
                ) {
                    Text("Export diagnostic report")
                }
            },
            dismissButton = { TextButton(onClick = { preview = null }) { Text("Close") } },
        )
    }
}
