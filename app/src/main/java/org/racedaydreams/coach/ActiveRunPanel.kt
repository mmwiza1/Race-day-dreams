package org.racedaydreams.coach

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import java.time.Duration
import java.util.Locale
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.execution.WorkoutExecutionEngine
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.domain.support.DreamRoutes
import org.racedaydreams.recording.RunRecordingService

/** Platform-owned recording, route persistence and Dream context; the actual panel is shared. */
@Composable
fun ActiveRunPanel(
    s: RunMetricsEngine.Status,
    w: WorkoutExecutionEngine.State?,
    hr: Double?,
    cue: String?,
    race: String?,
    notice: String?,
    imperial: Boolean,
    send: (String) -> Unit,
) {
    SharedActiveRunPanel(
        s,
        w,
        hr,
        cue,
        race,
        notice,
        imperial,
        send,
        formatTime = { PaceCalculator.formatTime(Duration.ofMillis(it)) },
        formatPace = ::formatPaceSeconds,
        formatDecimal = ::decimal,
        formatDistance = {
            String.format(Locale.ROOT, "%.2f", it / if (imperial) 1609.344 else 1000.0) +
                if (imperial) " mi" else " km"
        },
        routeContent = { LiveRecordedRoute() },
        dreamContent = {
            val dream by RunRecordingService.dreamRoute.collectAsState()
            dream?.let { route ->
                var show by remember { mutableStateOf(false) }
                TextButton(onClick = { show = !show }) {
                    Text(if (show) "Hide Dream progress" else "Dream Route progress")
                }
                if (show) {
                    Text("Equivalent distance only · not your physical location")
                    DreamDrawing(
                        DreamRoutes.forId(route),
                        0.0,
                        kotlin.math.min(42195.0, s.metres()),
                    )
                }
            }
        },
    )
}
