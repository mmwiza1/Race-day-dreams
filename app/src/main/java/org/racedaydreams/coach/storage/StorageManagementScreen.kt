package org.racedaydreams.coach.storage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import org.racedaydreams.coach.*
import org.racedaydreams.domain.storage.DetailRetention

fun retentionPolicyLabel(policy: DetailRetention.Policy): String =
    when (policy) {
        DetailRetention.Policy.LAST_24_MONTHS -> "24 months"
        DetailRetention.Policy.LAST_5_YEARS -> "5 years"
        DetailRetention.Policy.KEEP_FOREVER -> "Keep forever"
    }

@Composable
fun StorageManagementScreen(model: StorageManagementViewModel, onBack: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    StorageManagementContent(
        state,
        onBack,
        model::refresh,
        model::choose,
        model::preview,
        model::cleanup,
    )
}

@Composable
fun StorageManagementContent(
    state: StorageManagementState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onChoice: (DetailRetention.Policy) -> Unit,
    onPreview: () -> Unit,
    onCleanup: () -> Unit,
) {
    var selected by
        rememberSaveable(state.preferences) { mutableStateOf(state.preferences.policy()) }
    var confirming by rememberSaveable { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    CoachPage {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
        CoachHeading("Storage & recording detail")
        Text("Keep your training history. Choose how much detailed recording evidence to retain.")
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        state.notice?.let {
            Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        SemanticCard(Tile.Progress) {
            Text("Your history stays", style = MaterialTheme.typography.titleLarge)
            Text(
                "Workout summaries, actual gym sets, personal bests, race results, programme versions, milestones and coaching decisions are retained unless you explicitly delete them."
            )
            Text(
                "Cleanup affects only eligible finished app-owned GPS, high-frequency sensor and recording event detail. Health Connect providers and body measurements are not deleted."
            )
        }
        SemanticCard(Tile.Planned) {
            Text("Detailed recording retention", style = MaterialTheme.typography.titleLarge)
            Text("Saved choice: ${retentionPolicyLabel(state.preferences.policy())}")
            if (state.preferences.choice() == DetailRetention.Choice.EXISTING_UNREVIEWED)
                Text(
                    "Your existing raw detail is protected. Nothing expires until you make an informed choice and separately confirm a cleanup preview."
                )
            else
                Text(
                    "This choice does not start automatic deletion. You review and confirm each cleanup."
                )
            DetailRetention.Policy.values().forEach { policy ->
                OutlinedButton(
                    onClick = { selected = policy },
                    enabled = !state.busy,
                    modifier =
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                            this.selected = selected == policy
                        },
                ) {
                    Text((if (selected == policy) "✓ " else "") + retentionPolicyLabel(policy))
                }
            }
            Text(
                "Shorter retention can permanently remove old map routes, detailed heart-rate/cadence observations and precise sample-based splits. Removed detail can only be restored from a compatible backup that actually contains it."
            )
            Button(
                onClick = { onChoice(selected) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text("I understand — save retention choice")
            }
        }
        state.inventory?.let { inventory ->
            SemanticCard(Tile.Strength) {
                Text("Local storage", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Database and working files: about ${storageBytes(inventory.usage().databaseFileBytes())}"
                )
                Text(
                    "Raw recording payload: about ${storageBytes(inventory.usage().detailPayloadBytes())}"
                )
                Text("${inventory.usage().activityRows()} activity summaries retained")
                Text(
                    "Payload bytes exclude indexes and database overhead. Deleting rows does not necessarily shrink SQLite files or reclaim device storage immediately."
                )
                TextButton(
                    onClick = { details = !details },
                    modifier =
                        Modifier.heightIn(min = 48.dp).semantics {
                            stateDescription = if (details) "Expanded" else "Collapsed"
                        },
                ) {
                    Text(if (details) "Storage details ▴" else "Storage details ▾")
                }
                if (details) {
                    Text(
                        "${inventory.usage().pointRows()} GPS/sensor points · ${inventory.usage().eventRows()} recording events"
                    )
                    Text(
                        "Activity-summary payload: about ${storageBytes(inventory.usage().activityPayloadBytes())}. Other programme, gym and health records are additional retained data; this is not a whole-device storage scan."
                    )
                    Text(
                        "Database file estimate includes the database, write-ahead log and shared-memory files. Cache, bundled artwork and exported files held by other apps are not included."
                    )
                }
            }
            if (inventory.unfinishedRecording())
                Text(
                    "Finish, recover or explicitly discard the unfinished run before cleanup. Active, paused and recoverable recordings are protected."
                )
            OutlinedButton(
                onClick = onPreview,
                enabled =
                    !state.busy &&
                        !inventory.unfinishedRecording() &&
                        state.preferences.policy() != DetailRetention.Policy.KEEP_FOREVER &&
                        state.preferences.choice() != DetailRetention.Choice.EXISTING_UNREVIEWED,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text("Review eligible detail")
            }
        }
        state.preview?.let { preview ->
            SemanticCard(Tile.Progress) {
                Text("Cleanup preview", style = MaterialTheme.typography.titleLarge)
                preview.policy().cutoff(preview.inspectedAt())?.let { cutoff ->
                    Text(
                        "Eligibility cutoff: " +
                            java.time.format.DateTimeFormatter.ofPattern(
                                    "d MMMM uuuu 'at' HH:mm 'UTC'",
                                    Locale.UK,
                                )
                                .withZone(java.time.ZoneOffset.UTC)
                                .format(cutoff)
                    )
                }
                Text(
                    "${preview.recordings().size} finished ${if (preview.recordings().size == 1) "recording" else "recordings"} · ${preview.points()} GPS/sensor points · ${preview.events()} recording events"
                )
                Text(
                    "Only detail older than your saved retention window is selected. Workout summaries, PBs and programmes stay."
                )
                Text(
                    "This preview expires after 24 hours. Changes to the data, restore or retention choice require a fresh review."
                )
                if (preview.recordings().isEmpty()) Text("No eligible detail to remove.")
                else
                    OutlinedButton(
                        onClick = { confirming = true },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("Review cleanup confirmation")
                    }
            }
        }
        SemanticCard(Tile.Planned) {
            Text("Back up before cleanup", style = MaterialTheme.typography.titleLarge)
            Text(
                "Use Complete backup in Record a run to preserve app history and available raw recordings together. The ordinary JSON backup preserves summaries and removal provenance, but does not contain raw GPS detail. Keep the file somewhere you trust."
            )
        }
        TextButton(
            onClick = onRefresh,
            enabled = !state.busy,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text("Refresh storage")
        }
    }
    if (confirming && state.preview != null && !state.busy) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Remove this raw detail?") },
            text = {
                androidx.compose.foundation.rememberScrollState().let { scroll ->
                    androidx.compose.foundation.layout.Column(
                        Modifier.heightIn(max = 360.dp).verticalScroll(scroll)
                    ) {
                        Text(
                            "Remove detailed evidence from ${state.preview.recordings().size} finished ${if (state.preview.recordings().size == 1) "recording" else "recordings"}? Your recorded summaries, PBs and training history stay. Maps and precise sample-based detail will no longer be available without a suitable earlier raw backup. This cannot be undone by the app alone. Cleanup may continue while the app is running. If interrupted, refresh to review remaining detail."
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        onCleanup()
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Remove previewed raw detail")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirming = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Keep my recording detail")
                }
            },
        )
    }
}

private fun storageBytes(bytes: Long): String =
    if (bytes < 1024 * 1024) String.format(Locale.UK, "%.1f KiB", bytes / 1024.0)
    else String.format(Locale.UK, "%.1f MiB", bytes / (1024.0 * 1024.0))
