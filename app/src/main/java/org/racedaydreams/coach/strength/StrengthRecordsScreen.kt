package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.racedaydreams.coach.*
import org.racedaydreams.domain.body.MassUnit
import org.racedaydreams.domain.strength.*

@Composable
fun StrengthRecordsScreen(
    sessions: List<StrengthSession>,
    unit: MassUnit,
    historicalCount: Int,
    onHistory: () -> Unit,
    onWorkout: (String) -> Unit,
    onBack: () -> Unit,
) {
    val cutoff = remember(sessions) { Instant.now() }
    val history by
        produceState<List<StrengthRecords.Point>?>(null, sessions, cutoff) {
            value =
                withContext(Dispatchers.Default) { StrengthRecords.progression(sessions, cutoff) }
        }
    CoachPage {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Back to Strength")
        }
        CoachHeading("Strength records")
        Text("What you recorded and what we estimate are kept separate. No maximal test is needed.")
        OutlinedButton(
            onClick = onHistory,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Historical entries & full PB history")
        }
        if (historicalCount > 0)
            Text(
                "$historicalCount self-reported historical entries are available in full PB history. They are separate from recorded workout evidence."
            )
        val points = history
        if (points == null) CircularProgressIndicator()
        else if (points.isEmpty())
            SemanticCard(Tile.Neutral) {
                Text("No recorded working-set records yet.")
                Text(
                    "Record your actual performance in Gym Now or log a workout. Warm-ups do not award these records."
                )
            }
        else {
            val best = remember(points) { StrengthRecords.best(points) }
            val groups =
                remember(best) {
                    best
                        .groupBy {
                            Triple(it.key().exerciseId(), it.key().mode(), it.key().measurement())
                        }
                        .entries
                        .sortedByDescending { entry -> entry.value.maxOf { it.at() } }
                }
            val historyByKey = remember(points) { points.groupBy { it.key() } }
            var exerciseLimit by rememberSaveable { mutableIntStateOf(12) }
            groups.take(exerciseLimit).forEach { (identity, records) ->
                val primary =
                    records.filter {
                        !it.estimated() &&
                            it.key().kind() != StrengthRecords.Kind.EXACT_REPETITIONS_LOAD &&
                            it.key().kind() != StrengthRecords.Kind.REPETITIONS_AT_LOAD
                    }
                var details by rememberSaveable(identity.toString()) { mutableStateOf(false) }
                SemanticCard(Tile.Strength) {
                    Text(
                        records.first().exercise().name(),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text("Recorded performance", style = MaterialTheme.typography.titleMedium)
                    primary.take(if (details) Int.MAX_VALUE else 3).forEach { record ->
                        RecordResult(record, unit, onWorkout)
                    }
                    TextButton(
                        onClick = { details = !details },
                        modifier =
                            Modifier.heightIn(min = 48.dp).semantics {
                                stateDescription = if (details) "Expanded" else "Collapsed"
                            },
                    ) {
                        Text(if (details) "Hide measured history" else "View measured history")
                    }
                    if (details) {
                        Text(
                            "Repetition records and progression",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        var shown by rememberSaveable(identity.toString()) { mutableIntStateOf(12) }
                        val measured = records.filter { !it.estimated() }
                        measured.take(shown).forEach { record ->
                            HorizontalDivider()
                            Text(
                                recordLabel(record, unit),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            val progression = historyByKey[record.key()].orEmpty()
                            var historyCount by
                                rememberSaveable(record.key().toString()) { mutableIntStateOf(8) }
                            progression.takeLast(historyCount).forEach { previous ->
                                Text(
                                    "${friendlyDate(previous.date())} · ${recordValue(previous,unit)}"
                                )
                                LinearProgressIndicator(
                                    progress = {
                                        (previous.value() / record.value())
                                            .toFloat()
                                            .coerceIn(0f, 1f)
                                    },
                                    modifier =
                                        Modifier.fillMaxWidth().height(8.dp).clearAndSetSemantics {},
                                )
                            }
                            if (progression.size > historyCount)
                                TextButton(onClick = { historyCount += 20 }) {
                                    Text("Show earlier improvements")
                                }
                            TextButton(onClick = { onWorkout(record.workoutId()) }) {
                                Text("View supporting workout")
                            }
                        }
                        if (measured.size > shown)
                            TextButton(onClick = { shown += 12 }) {
                                Text("Show more measured tasks")
                            }
                        Text(
                            "Each line compares the same exercise, measurement and task. Different carry loads and repetition counts are not merged.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                records
                    .firstOrNull { it.estimated() }
                    ?.let { estimate ->
                        SemanticCard(Tile.Insight) {
                            Text(
                                "${estimate.exercise().name()} · estimated single-repetition maximum",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "About ${recordValue(estimate,unit)}",
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text(
                                "A calculation of a possible single-repetition maximum, not a recorded lift or a training-weight suggestion."
                            )
                            Text("Source: recorded ${friendlyDate(estimate.date())}")
                            Text(
                                "Based on ${estimate.actual().reps()} recorded repetitions at ${recordMass(estimate.actual().kilograms(),unit)}."
                            )
                            WhyDetails(
                                StrengthRecords.estimateExplanation(),
                                "How is this estimated?",
                            )
                            TextButton(onClick = { onWorkout(estimate.workoutId()) }) {
                                Text("View estimate's source workout")
                            }
                        }
                    }
            }
            if (groups.size > exerciseLimit)
                TextButton(
                    onClick = { exerciseLimit += 12 },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Show more exercises")
                }
        }
    }
}

@Composable
private fun RecordResult(
    record: StrengthRecords.Point,
    unit: MassUnit,
    onWorkout: (String) -> Unit,
) {
    Text(recordLabel(record, unit), style = MaterialTheme.typography.titleSmall)
    Text(recordValue(record, unit), style = MaterialTheme.typography.titleLarge)
    Text("Recorded ${friendlyDate(record.date())}")
    TextButton(
        onClick = { onWorkout(record.workoutId()) },
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Text("View supporting workout")
    }
}

private fun recordMass(kg: Double, unit: MassUnit) =
    "${decimal(unit.fromKilograms(kg))} ${unit.name.lowercase()}"

private fun recordLabel(record: StrengthRecords.Point, unit: MassUnit): String =
    when (record.key().kind()) {
        StrengthRecords.Kind.HEAVIEST_LOAD ->
            if (record.exercise().mode() == StrengthExercise.Mode.WEIGHTED_BODYWEIGHT)
                "Heaviest recorded added load"
            else "Heaviest recorded working set"
        StrengthRecords.Kind.EXACT_REPETITIONS_LOAD ->
            "Best recorded load for ${record.key().repetitions()} repetitions"
        StrengthRecords.Kind.REPETITIONS_AT_LOAD ->
            "Most recorded repetitions at ${recordMass(record.key().load()!!,unit)}"
        StrengthRecords.Kind.BODYWEIGHT_REPETITIONS -> "Most recorded bodyweight repetitions"
        StrengthRecords.Kind.DURATION ->
            "Longest recorded hold" +
                (record.key().load()?.let { " at ${recordMass(it,unit)}" } ?: "")
        StrengthRecords.Kind.DISTANCE ->
            "Furthest recorded distance" +
                (record.key().load()?.let { " at ${recordMass(it,unit)}" } ?: "")
        StrengthRecords.Kind.ESTIMATED_ONE_REP_MAX -> "Estimated one-repetition maximum"
    }

private fun recordValue(record: StrengthRecords.Point, unit: MassUnit): String =
    when (record.key().kind()) {
        StrengthRecords.Kind.HEAVIEST_LOAD,
        StrengthRecords.Kind.EXACT_REPETITIONS_LOAD ->
            "${recordMass(record.actual().kilograms(),unit)} for ${record.actual().reps()} repetitions"
        StrengthRecords.Kind.REPETITIONS_AT_LOAD,
        StrengthRecords.Kind.BODYWEIGHT_REPETITIONS -> "${record.actual().reps()} repetitions"
        StrengthRecords.Kind.DURATION -> StrengthMeasurements.duration(record.value().toLong())
        StrengthRecords.Kind.DISTANCE -> "${decimal(record.value())} m"
        StrengthRecords.Kind.ESTIMATED_ONE_REP_MAX ->
            "${kotlin.math.round(unit.fromKilograms(record.value())).toLong()} ${unit.name.lowercase()}"
    }
