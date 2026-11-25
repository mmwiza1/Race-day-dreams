package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import org.racedaydreams.data.SupportStore
import org.racedaydreams.domain.support.*

/** Android owns the existing completion transaction, coroutine lifetime and licensed assets. */
@Composable fun RoutineLibrary(open: (String) -> Unit) = SharedRoutineLibrary(open)

@Composable
fun RoutineScreen(id: String?, related: String?, done: () -> Unit) {
    val routine = remember(id) { runCatching { RunningRoutines.get(id ?: "") }.getOrNull() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    SharedRoutineContent(
        id,
        routine,
        saving,
        error,
        illustration = { movementId ->
            val art = routineArtwork(movementId)
            if (art != null) IllustratedMovement(art)
            else
                Text(
                    "Visual guide unavailable. Follow the written guidance or choose a familiar movement."
                )
        },
        finish = {
            val selected = requireNotNull(routine)
            saving = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        SupportStore(context)
                            .completeRoutine(requireNotNull(selected.id()), related)
                    }
                    done()
                } catch (_: Exception) {
                    error = "Completion could not be saved. Your training plan is unchanged."
                } finally {
                    saving = false
                }
            }
        },
    )
}

@Composable
fun DreamRouteSection(activity: org.racedaydreams.domain.activity.ActivityRecord) {
    val metres = activity.distanceMetres() ?: return
    if (metres <= 0) return
    val context = LocalContext.current
    val assignment by
        produceState<DreamRoutes.Assignment?>(null, activity.id()) {
            value =
                withContext(Dispatchers.IO) { SupportStore(context).dream(activity.id(), metres) }
        }
    val a = assignment ?: return
    val route = DreamRoutes.forId(a.routeId())
    DreamRouteExperience(route, a)
}

@Composable
fun MobilitySupportCard(open: () -> Unit) {
    GuidanceCard(guidanceMint()) {
        Text("Mobility this week", style = MaterialTheme.typography.titleMedium)
        Text("2–4 short sessions suggested")
        Text("Next: Hips & glutes · 8 min", style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = open) { Text("View routines") }
    }
}

@Composable
fun WarmupSupportLink(id: String, related: String, open: () -> Unit) {
    val context = LocalContext.current
    val completed by
        produceState(false, id, related) {
            value =
                withContext(Dispatchers.IO) {
                    SupportStore(context).routineCount(java.time.Instant.EPOCH, id, related) > 0
                }
        }
    TextButton(onClick = open) {
        Text(if (completed) "✓ Warm-up" else "Warm-up · ${RunningRoutines.get(id).minutes()} min")
    }
}
