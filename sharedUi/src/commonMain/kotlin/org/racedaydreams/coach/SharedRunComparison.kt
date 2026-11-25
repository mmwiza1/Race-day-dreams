package org.racedaydreams.coach

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.ActivitySources
import org.racedaydreams.domain.activity.DuplicateRunChoice
import org.racedaydreams.domain.activity.PhysicalRunSessions

@Composable
fun SharedRunComparison(
    run: ActivityRecord,
    activities: List<ActivityRecord>,
    imperial: Boolean,
    busy: Boolean,
    date: (ActivityRecord) -> String,
    save: (String, String, DuplicateRunChoice?) -> Unit,
    remove: (String, String) -> Unit,
) {
    val current = activities.firstOrNull { it.id() == run.id() } ?: run
    val peers =
        remember(current, activities) {
            activities
                .filter { other ->
                    PhysicalRunSessions.possibleDuplicate(current, other) ||
                        PhysicalRunSessions.decision(current, other) != null
                }
                .sortedBy { it.start() }
                .take(12)
        }
    if (peers.isEmpty()) return
    var open by rememberSaveable(run.id()) { mutableStateOf(false) }
    var peerId by rememberSaveable(run.id()) { mutableStateOf<String?>(null) }
    var removeId by rememberSaveable(run.id()) { mutableStateOf<String?>(null) }
    CoachCard {
        Text("Possible duplicate runs", style = MaterialTheme.typography.titleMedium)
        Text(
            "Compare overlapping recordings and choose which one counts. Both stay in your history."
        )
        TextButton(onClick = { open = !open }) {
            Text(if (open) "Hide run comparisons" else "Compare recordings")
        }
        if (open) {
            peers.forEach { peer ->
                TextButton(onClick = { peerId = peer.id() }) {
                    Text(
                        "Compare with ${ActivitySources.detailed(peer.source())} · ${compareDistance(peer, imperial)}"
                    )
                }
            }
            peers
                .firstOrNull { it.id() == peerId }
                ?.let { other ->
                    HorizontalDivider()
                    CompareRunRecord("This run", current, imperial, date)
                    CompareRunRecord("Other run", other, imperial, date)
                    val choice = PhysicalRunSessions.decision(current, other)
                    Text(
                        when (choice) {
                            DuplicateRunChoice.COUNT_THIS -> "Your choice: count this run"
                            DuplicateRunChoice.COUNT_OTHER -> "Your choice: count the other run"
                            DuplicateRunChoice.SEPARATE ->
                                "Your choice: count both as separate runs"
                            null -> "Automatic selection"
                        }
                    )
                    val compatibleLinks =
                        current.plannedWorkoutId() == null ||
                            other.plannedWorkoutId() == null ||
                            current.plannedWorkoutId() == other.plannedWorkoutId()
                    val overlap = PhysicalRunSessions.possibleDuplicate(current, other)
                    if (!compatibleLinks || !overlap)
                        Text(
                            "These recordings cannot be combined. Review the scheduled workouts or keep them separate."
                        )
                    TextButton(
                        enabled = !busy && compatibleLinks && overlap,
                        onClick = { save(current.id(), other.id(), DuplicateRunChoice.COUNT_THIS) },
                    ) {
                        Text("Count this run")
                    }
                    TextButton(
                        enabled = !busy && compatibleLinks && overlap,
                        onClick = { save(current.id(), other.id(), DuplicateRunChoice.COUNT_OTHER) },
                    ) {
                        Text("Count the other run")
                    }
                    TextButton(
                        enabled = !busy && overlap,
                        onClick = { save(current.id(), other.id(), DuplicateRunChoice.SEPARATE) },
                    ) {
                        Text("Count both separately")
                    }
                    if (choice != null)
                        TextButton(
                            enabled = !busy,
                            onClick = { save(current.id(), other.id(), null) },
                        ) {
                            Text("Use automatic selection")
                        }
                    val local =
                        when (choice) {
                            DuplicateRunChoice.COUNT_THIS -> other
                            DuplicateRunChoice.COUNT_OTHER -> current
                            else -> null
                        }
                    if (
                        local?.source()?.provider() in
                            listOf("rdd", "gpx", "manual", "assessment") &&
                            local?.plannedWorkoutId() == null
                    )
                        TextButton(enabled = !busy, onClick = { removeId = local?.id() }) {
                            Text("Remove the uncounted local copy")
                        }
                    removeId?.let { discard ->
                        AlertDialog(
                            onDismissRequest = { removeId = null },
                            title = { Text("Remove local recording?") },
                            text = {
                                Text(
                                    "This permanently removes the uncounted local copy and any route it contains. The recording you chose to count stays in your history."
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        removeId = null
                                        remove(
                                            discard,
                                            if (discard == current.id()) other.id()
                                            else current.id(),
                                        )
                                    },
                                    enabled = !busy,
                                ) {
                                    Text("Remove local recording")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { removeId = null }) {
                                    Text("Keep both copies")
                                }
                            },
                        )
                    }
                }
        }
    }
}

@Composable
private fun CompareRunRecord(
    label: String,
    run: ActivityRecord,
    imperial: Boolean,
    date: (ActivityRecord) -> String,
) {
    Text(label, style = MaterialTheme.typography.titleSmall)
    Text(ActivitySources.detailed(run.source()))
    Text(date(run))
    val time = run.elapsedTime().getSeconds()
    val clock =
        if (time >= 3600) "${time / 3600} h ${(time % 3600) / 60} min ${time % 60} s"
        else "${time / 60} min ${time % 60} s"
    Text("${compareDistance(run, imperial)} · $clock elapsed")
    run.averageHeartRate()?.let { Text("Average heart rate ${it.toInt()} bpm") }
}

private fun compareDistance(run: ActivityRecord, imperial: Boolean): String =
    run.distanceMetres()?.let { metres ->
        val units = if (imperial) 1609.344 else 1000.0
        val hundredths = kotlin.math.round(metres / units * 100).toLong()
        "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')} ${if (imperial) "mi" else "km"}"
    } ?: "Distance unavailable"
