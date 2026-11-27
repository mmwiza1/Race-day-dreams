package org.racedaydreams.coach

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
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
    val other = peers.firstOrNull { it.id() == peerId } ?: peers.first()

    Column(verticalArrangement = Arrangement.spacedBy(CoachSpace.Related)) {
        Text(
            "Possible duplicate runs",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        OutlinedButton(
            onClick = { open = !open },
            modifier =
                Modifier.semantics { stateDescription = if (open) "Expanded" else "Collapsed" },
        ) {
            Text(if (open) "Hide comparison" else "Compare recordings")
        }
        if (!open) return@Column

        if (peers.size > 1) {
            var choosingPeer by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { choosingPeer = true }) {
                    Text(
                        "Compare with ${ActivitySources.detailed(other.source())} · ${compareDistance(other, imperial)}"
                    )
                }
                DropdownMenu(expanded = choosingPeer, onDismissRequest = { choosingPeer = false }) {
                    peers.forEach { peer ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${ActivitySources.detailed(peer.source())} · ${compareDistance(peer, imperial)} · ${date(peer)}"
                                )
                            },
                            onClick = {
                                peerId = peer.id()
                                choosingPeer = false
                            },
                        )
                    }
                }
            }
        }

        val choice = PhysicalRunSessions.decision(current, other)
        val compatibleLinks =
            current.plannedWorkoutId() == null ||
                other.plannedWorkoutId() == null ||
                current.plannedWorkoutId() == other.plannedWorkoutId()
        val overlap = PhysicalRunSessions.possibleDuplicate(current, other)
        val counted =
            remember(current, activities) {
                PhysicalRunSessions.counted(activities + current).map { it.id() }.toSet()
            }
        Text(
            when (choice) {
                DuplicateRunChoice.COUNT_THIS,
                DuplicateRunChoice.COUNT_OTHER -> "Saved · One run counts"
                DuplicateRunChoice.SEPARATE -> "Saved · Both runs count separately"
                null -> "Automatic selection"
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (!compatibleLinks || !overlap) {
            Text(
                "These recordings cannot be combined. Review the scheduled workouts or keep them separate.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Column(
            Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(CoachSpace.Related),
        ) {
            CompareRunRecord(
                current,
                imperial,
                date,
                current.id() in counted,
                choice == DuplicateRunChoice.COUNT_THIS,
                !busy && compatibleLinks && overlap,
            ) {
                save(current.id(), other.id(), DuplicateRunChoice.COUNT_THIS)
            }
            CompareRunRecord(
                other,
                imperial,
                date,
                other.id() in counted,
                choice == DuplicateRunChoice.COUNT_OTHER,
                !busy && compatibleLinks && overlap,
            ) {
                save(current.id(), other.id(), DuplicateRunChoice.COUNT_OTHER)
            }
            CompareRunOption(
                "Count both separately",
                choice == DuplicateRunChoice.SEPARATE,
                !busy && overlap,
            ) {
                save(current.id(), other.id(), DuplicateRunChoice.SEPARATE)
            }
            CompareRunOption("Use automatic selection", choice == null, !busy && choice != null) {
                save(current.id(), other.id(), null)
            }
        }

        val local =
            when (choice) {
                DuplicateRunChoice.COUNT_THIS -> other
                DuplicateRunChoice.COUNT_OTHER -> current
                else -> null
            }
        if (
            local != null &&
                local.source().provider() in listOf("rdd", "gpx", "manual", "assessment") &&
                local.plannedWorkoutId() == null
        ) {
            HorizontalDivider()
            TextButton(enabled = !busy, onClick = { removeId = local.id() }) {
                Text(
                    "Remove uncounted local copy",
                    color = if (busy) LocalContentColor.current else MaterialTheme.colorScheme.error,
                )
            }
        }
        removeId?.let { discard ->
            AlertDialog(
                onDismissRequest = { removeId = null },
                title = { Text("Remove local recording?") },
                text = {
                    Text(
                        "Permanently removes this local copy and any route. Your counted run stays.",
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            removeId = null
                            remove(
                                discard,
                                if (discard == current.id()) other.id() else current.id(),
                            )
                        },
                        enabled = !busy,
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                    ) {
                        Text("Remove copy")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { removeId = null }) { Text("Keep both copies") }
                },
            )
        }
    }
}

@Composable
private fun CompareRunOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    choose: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = CoachSpace.Touch)
            .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = choose)
            .padding(horizontal = CoachSpace.Card),
        horizontalArrangement = Arrangement.spacedBy(CoachSpace.Related),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled || selected)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CompareRunRecord(
    run: ActivityRecord,
    imperial: Boolean,
    date: (ActivityRecord) -> String,
    counted: Boolean,
    selected: Boolean,
    enabled: Boolean,
    choose: () -> Unit,
) {
    val colours = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colours.secondaryContainer else colours.surfaceContainerLow,
        contentColor = if (selected) colours.onSecondaryContainer else colours.onSurface,
        border = BorderStroke(1.dp, if (selected) colours.secondary else colours.outlineVariant),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = choose)
                .padding(CoachSpace.Card),
            horizontalArrangement = Arrangement.spacedBy(CoachSpace.Card),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null, enabled = enabled || selected)
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(CoachSpace.Tiny),
            ) {
                Text(
                    ActivitySources.detailed(run.source()),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(compareDistance(run, imperial), style = MaterialTheme.typography.headlineSmall)
                val seconds = run.elapsedTime().getSeconds()
                val minutes = (seconds / 60 % 60).toString().padStart(2, '0')
                val remainder = (seconds % 60).toString().padStart(2, '0')
                val duration =
                    if (seconds >= 3600) "${seconds / 3600}:$minutes:$remainder"
                    else "${seconds / 60}:$remainder"
                Text(
                    "$duration elapsed · ${date(run)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                run.averageHeartRate()?.let {
                    Text(
                        "Average heart rate ${it.toInt()} bpm",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    if (counted) "Counted" else "Kept in history",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private fun compareDistance(run: ActivityRecord, imperial: Boolean): String =
    run.distanceMetres()?.let { metres ->
        val units = if (imperial) 1609.344 else 1000.0
        val hundredths = kotlin.math.round(metres / units * 100).toLong()
        "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')} ${if (imperial) "mi" else "km"}"
    } ?: "Distance unavailable"
