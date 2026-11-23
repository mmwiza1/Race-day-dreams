@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.SplitAnalysis
import org.racedaydreams.domain.activity.SplitCalculator
import org.racedaydreams.domain.baseline.AssessmentAnalyzer

@Composable
fun ActivityHistoryScreen(
    state: CoachUiState,
    open: (String) -> Unit,
    allStrength: () -> Unit = {},
    loadSummaryPage: (suspend (java.time.Instant?, String?, Int) -> List<ActivityRecord>)? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    SharedActivityHistoryScreen(
        state,
        open,
        allStrength,
        loadSummaryPage
            ?: { instant, id, limit ->
                withContext(Dispatchers.IO) {
                    org.racedaydreams.data
                        .FeatureStore(context)
                        .activitySummaryPage(instant, id, limit)
                }
            },
        ::displayActivityDate,
        { instant, offset -> friendlyDate(instant.atOffset(offset).toLocalDate()) },
        { formatDistance(it, state) },
        ::recordedElapsedPace,
        "No completed runs have been added. Connect Health Connect or enter an optional completed assessment to begin your history.",
    )
}

@Composable
fun ActivityDetailScreen(state: CoachUiState, model: CoachViewModel, id: String?) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val retained by
        produceState<ActivityRecord?>(null, id, state.activities) {
            value =
                withContext(Dispatchers.IO) {
                    id?.let { org.racedaydreams.data.FeatureStore(context).activityDetail(it) }
                }
        }
    val activity = retained ?: state.activities.firstOrNull { it.id() == id }
    if (activity == null) {
        CoachPage {
            CoachHeading("Activity unavailable")
            Text(
                "This activity is no longer in local history. A source may have updated or removed it."
            )
        }
        return
    }
    var imperial by
        rememberSaveable(activity.id()) {
            mutableStateOf(state.profile?.preferences()?.units() == UnitSystem.IMPERIAL)
        }
    val splitUnits = if (imperial) UnitSystem.IMPERIAL else UnitSystem.METRIC
    val analysis by
        produceState<SplitAnalysis?>(null, activity, splitUnits) {
            value =
                withContext(Dispatchers.Default) { SplitCalculator.calculate(activity, splitUnits) }
        }
    var chooseShoe by rememberSaveable { mutableStateOf(false) }
    val assessment = state.assessments.firstOrNull { it.activityId() == activity.id() }
    val assessmentSummary =
        remember(activity, assessment, state.baseline) {
            assessment?.let { AssessmentAnalyzer.analyze(it, activity, state.baseline) }
        }
    val units = if (imperial) "mi" else "km"
    CoachPage {
        // A compact running header: the start date and time as the eyebrow, the title as heading.
        ScreenHeader(
            Discipline.RUNNING,
            Glyph.RUN,
            activity
                .start()
                .atOffset(activity.startOffset() ?: ZoneOffset.UTC)
                .format(
                    java.time.format.DateTimeFormatter.ofPattern(
                        "EEEE, d MMMM uuuu · HH:mm XXX",
                        java.util.Locale.UK,
                    )
                ),
            if (assessment != null) "Assessment run" else "Run detail",
        )
        if (activity.startOffset() == null)
            Text(
                "The source did not provide a time-zone offset; this time is shown explicitly in UTC.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        RecordedRunSummary(activity, state)
        RecordedHeartRateSummary(activity)
        RecordedRouteCard(activity)
        SpontaneousAssociation(state, model, activity)
        if (activity.shoeId() != null)
            CheckRow(
                "I noted discomfort while wearing this shoe (no cause inferred)",
                activity.id() in state.shoeDiscomfort,
            ) {
                model.runningEvidence.shoeDiscomfort(activity.id(), it)
            }
        ExpressiveSection("Source and measurements", Glyph.LIST, ChartHue.TEAL) {
            activity.elevationGainMetres()?.let {
                Text("Recorded elevation gain: ${decimal(it)} m")
            }
            activity.cadence()?.let { Text("Recorded cadence: ${decimal(it)} steps/min") }
            activity.rpe()?.let { Text("Manually entered effort: ${decimal(it)}/10") }
            Text(
                "Source: ${org.racedaydreams.domain.activity.ActivitySources.friendly(activity.source())}",
                style = MaterialTheme.typography.titleSmall,
            )
            WhyDetails(
                "Provider: ${activity.source().provider()}\nOrigin: ${activity.source().originPackage() ?: "Unavailable"}\nRecord: ${activity.source().recordId()}",
                "Technical data source",
            )

            Text(
                "Values absent from the source remain unavailable. Elapsed time is not presented as moving time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SharedRunDurabilityCard(
            activity,
            state.profile?.preferences()?.units() == org.racedaydreams.domain.UnitSystem.IMPERIAL,
            ::formatPaceSeconds,
            ::decimal,
        )
        StructuredRunSection(state, model, activity)
        DreamRouteSection(activity)
        RunComparisonSection(state, activity)
        RunFeedbackSection(state, model, activity)
        assessmentSummary?.let { summary ->
            ExpressiveSection("What this assessment tells us", Glyph.SPARK, ChartHue.BLUE) {
                Text("Mode: ${assessment!!.assessment().mode().name.lowercase().replace('_', ' ')}")
                summary.confidence()?.let { Text("Baseline confidence: ${it.name.lowercase()}") }
                summary.findings().forEach { Text(it) }
            }
        }
        SectionLabel("Distance splits", Glyph.VOLUME, ChartHue.VIOLET)
        Text(
            "See the time and pace for each completed kilometre or mile. A final shorter section is shown separately when recorded."
        )
        CheckRow("Show mile splits instead of kilometre splits", imperial) { imperial = it }
        val splitAnalysis = analysis
        if (splitAnalysis == null) Text("Analysing retained samples…")
        else {
            if (splitAnalysis.splits().isEmpty())
                Text(
                    "Reliable distance/time samples are unavailable. Splits have not been invented from average pace."
                )
            splitAnalysis.reasons().forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            var selectedSplit by
                rememberSaveable(activity.id(), splitUnits) { mutableIntStateOf(1) }
            val fastestPace = splitAnalysis.splits().minOfOrNull { it.secondsPerKilometre() }
            // Fastest and slowest are named among complete splits only (as the route card does),
            // and only when there are at least two to compare. Values are the recorded ones.
            val complete = splitAnalysis.splits().filterNot { it.partial() }
            val fastestNumber =
                complete.takeIf { it.size > 1 }?.minByOrNull { it.secondsPerKilometre() }?.number()
            val slowestNumber =
                complete
                    .takeIf { it.size > 1 }
                    ?.maxByOrNull { it.secondsPerKilometre() }
                    ?.number()
                    ?.takeIf { it != fastestNumber }
            if (splitAnalysis.splits().isNotEmpty()) {
                Text(
                    "Tap a split to highlight it · a longer bar means a faster recorded pace",
                    style = MaterialTheme.typography.bodySmall,
                )
                Panel(contentPadding = PaddingValues(8.dp)) {
                    splitAnalysis.splits().forEach { split ->
                        SplitRow(
                            split,
                            selected = selectedSplit == split.number(),
                            fastest = split.number() == fastestNumber,
                            slowest = split.number() == slowestNumber,
                            fastestPace = fastestPace,
                            imperial = imperial,
                            units = units,
                        ) {
                            selectedSplit = split.number()
                        }
                    }
                }
            }
        }
        SectionLabel("Recorded laps / intervals", Glyph.LIST, ChartHue.TEAL)
        if (activity.laps().isEmpty())
            Text(
                "No actual laps were supplied. Planned repetitions are never substituted for completed laps."
            )
        else
            Panel {
                activity.laps().forEachIndexed { index, lap ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surface)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            lap.label()?.takeIf { it.isNotBlank() } ?: "Lap ${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Elapsed: ${PaceCalculator.formatTime(Duration.between(lap.start(), lap.end()))}"
                        )
                        lap.distanceMetres()?.let { Text(formatDistance(it, state)) }
                        lap.plannedSegmentId()?.let { Text("Linked planned segment: $it") }
                    }
                }
            }
        val response = state.runResponses.firstOrNull { it.activityId() == activity.id() }
        val planned =
            state.planVersions
                .firstOrNull { it.id() == response?.planVersionId() }
                ?.plan()
                ?.workouts()
                ?.firstOrNull { it.id() == response?.plannedWorkoutId() }
                ?: state.plannedWorkouts.firstOrNull { it.id() == activity.plannedWorkoutId() }
        SectionLabel("Planned and actual", Glyph.FLAG, ChartHue.BLUE)
        if (planned == null)
            Text(
                if (activity.plannedWorkoutId() == null)
                    "No planned workout is linked to this activity."
                else
                    "The linked planned workout is not currently available. The actual result remains retained."
            )
        else
            Panel {
                Text(
                    "Planned: ${workoutName(planned.category())}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                planned.distanceKm()?.let {
                    Text("Planned distance: ${formatDistance(it * 1000, state)}")
                }
                planned.duration()?.let {
                    Text("Planned duration: ${PaceCalculator.formatTime(it)}")
                }
                activity.distanceMetres()?.let { actual ->
                    Text("Actual distance: ${formatDistance(actual, state)}")
                    planned.distanceKm()?.let { target ->
                        Text("Distance difference: ${decimal(actual / 1000 - target)} km")
                    }
                }
                Text(
                    "The planned prescription is retained separately from this completion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        val results = state.results.filter { it.activityId() == activity.id() }
        if (results.isNotEmpty())
            Panel {
                Text(
                    "Linked performance results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                results.forEach {
                    Text(
                        "${it.distance().label()}: ${PaceCalculator.formatTime(it.duration())} · ${it.source().name.lowercase().replace('_', ' ')}"
                    )
                }
                Text(
                    "Personal-best progression is compared with your other retained results in Progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        val shoe = state.shoes.firstOrNull { it.id() == activity.shoeId() }
        ExpressiveSection("Shoe and your notes", Glyph.LIST, ChartHue.BLUE) {
            Text(
                shoe?.let {
                    "Shoe: ${it.brand()} ${it.model()}${if (it.retired()) " · retired" else ""}"
                } ?: "No shoe assigned",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedButton(
                onClick = { chooseShoe = true },
                enabled = !state.busy,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Change shoe assignment")
            }
            org.racedaydreams.coach.activity.SharedRunNotes(activity, state.busy) { notes, effort ->
                model.runningEvidence.saveNotes(activity.id(), notes, effort)
            }
        }
    }
    if (chooseShoe)
        AlertDialog(
            onDismissRequest = { chooseShoe = false },
            title = { Text("Assign a shoe") },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    item {
                        TextButton(
                            onClick = {
                                chooseShoe = false
                                model.runningEvidence.assignShoe(activity.id(), null)
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text("No shoe")
                        }
                    }
                    items(state.shoes, key = { it.id() }) { shoe ->
                        TextButton(
                            onClick = {
                                chooseShoe = false
                                model.runningEvidence.assignShoe(activity.id(), shoe.id())
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text(
                                "${shoe.brand()} ${shoe.model()}${if (shoe.retired()) " · retired" else ""}"
                            )
                        }
                    }
                    if (state.shoes.isEmpty())
                        item { Text("Add shoes from Progress or Settings first.") }
                }
            },
            confirmButton = { TextButton(onClick = { chooseShoe = false }) { Text("Close") } },
        )
}

/**
 * One distance split as a table row: number, recorded time and pace, a pace bar (longer means
 * faster) and sample-based extras. Fastest and slowest are named in words; the selected row is
 * tinted and labelled. Values are the existing split analysis, unchanged.
 */
@Composable
private fun SplitRow(
    split: org.racedaydreams.domain.activity.RunSplit,
    selected: Boolean,
    fastest: Boolean,
    slowest: Boolean,
    fastestPace: Double?,
    imperial: Boolean,
    units: String,
    select: () -> Unit,
) {
    val tones = chartAccent(ChartHue.VIOLET)
    Surface(
        onClick = select,
        color = if (selected) tones.soft.copy(alpha = 0.18f) else Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("run-split-${split.number()}")
                .semantics {
                    stateDescription = if (selected) "Selected split" else "Recorded split"
                },
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Split ${split.number()}${if (split.partial()) " · partial" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (fastest) FigureTag("Fastest", ChartHue.VIOLET)
                if (slowest) FigureTag("Slowest", ChartHue.VIOLET)
                if (selected)
                    Text(
                        "Selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
            }
            val length =
                if (imperial) split.distanceMetres() / 1609.344 else split.distanceMetres() / 1000.0
            val pace = split.secondsPerKilometre() * if (imperial) 1.609344 else 1.0
            Text(
                "${decimal(length)} $units · ${PaceCalculator.formatTime(split.duration())} · ${formatPaceSeconds(pace)}/$units",
                style = MaterialTheme.typography.titleMedium,
            )
            LinearProgressIndicator(
                progress = {
                    ((fastestPace ?: split.secondsPerKilometre()) / split.secondsPerKilometre())
                        .toFloat()
                        .coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth().height(10.dp).clearAndSetSemantics {},
                color = if (selected || fastest) tones.strong else tones.soft,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            listOfNotNull(
                    split.averageHeartRate()?.let { "Available-sample HR: ${decimal(it)} bpm" },
                    split.elevationChangeMetres()?.let { "Elevation change: ${decimal(it)} m" },
                    split.cadence()?.let { "Cadence: ${decimal(it)} steps/min" },
                )
                .forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
        }
    }
}
