package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.PersonalBestCalculator
import org.racedaydreams.domain.activity.RecordedResult
import org.racedaydreams.domain.activity.ResultSource
import org.racedaydreams.domain.time.LocalDate

@Composable
fun SharedRankedRunningResult(
    result: RecordedResult,
    rank: Int,
    units: UnitSystem,
    dateLabel: (LocalDate) -> String,
    linkedActivity: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().testTag("running-record-${result.distance().code()}-$rank"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color =
                if (rank == 1) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.large,
        ) {
            Text(
                "$rank",
                modifier =
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp).semantics {
                        contentDescription = "Rank $rank"
                    },
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                PaceCalculator.formatTime(result.duration()),
                style =
                    if (rank == 1) MaterialTheme.typography.headlineLarge
                    else MaterialTheme.typography.titleLarge,
            )
            Text(
                "${org.racedaydreams.domain.activity.PerformancePace.format(result.duration(), result.distance(), units)} average pace",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(dateLabel(result.date()), style = MaterialTheme.typography.bodyMedium)
            Text(sharedResultSource(result), style = MaterialTheme.typography.labelMedium)
            result.eventName()?.takeIf { it.isNotBlank() }?.let { Text(it) }
            linkedActivity()
        }
    }
}

fun sharedResultSource(result: RecordedResult): String =
    when (result.source()) {
        ResultSource.OFFICIAL_CHIP -> "Official chip time (entered)"
        ResultSource.MANUAL -> "Manually entered result"
        ResultSource.ROLLING -> "Calculated rolling effort"
    }

/** Reuse the existing source-precedence calculator before ranking display rows. */
fun sharedRankedRunningResults(results: List<RecordedResult>, code: String): List<RecordedResult> {
    val atDistance = results.filter { it.distance().code() == code }
    val best =
        PersonalBestCalculator.current(atDistance).firstOrNull()?.result() ?: return emptyList()
    val remaining =
        atDistance
            .groupBy { result ->
                result.activityId()?.let { "activity:$it" } ?: "entry:${result.id()}"
            }
            .values
            .flatMap { PersonalBestCalculator.current(it).map { entry -> entry.result() } }
            .filter { it.id() != best.id() }
            .sortedWith(
                compareBy<RecordedResult> { it.duration() }
                    .thenBy { it.date().toEpochDay() }
                    .thenBy { it.id() }
            )
            .take(2)
    return listOf(best) + remaining
}
