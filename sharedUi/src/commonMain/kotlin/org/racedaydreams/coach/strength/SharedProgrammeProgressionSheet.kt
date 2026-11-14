@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach.strength

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.*
import org.racedaydreams.domain.presentation.StrengthPrescriptionText
import org.racedaydreams.domain.strength.GymWorkoutDefinition
import org.racedaydreams.domain.time.CalendarDates
import org.racedaydreams.domain.time.LocalDate

@Composable
fun SharedProgrammeProgressionContent(
    review: ProgrammeProgressionReview,
    busy: Boolean,
    error: String?,
    keepCurrent: () -> Unit,
    saveSuggestion: () -> Unit,
    formatDate: (LocalDate) -> String,
) {
    var advanced by
        rememberSaveable(review.draftId, review.draftRevision, review.workoutId, review.slot.id()) {
            mutableStateOf(false)
        }
    var supporting by
        rememberSaveable(review.draftId, review.draftRevision, review.workoutId, review.slot.id()) {
            mutableStateOf(false)
        }
    val target = review.advice.proposedTarget()
    val comparisons =
        remember(review.slot, target) { StrengthPrescriptionText.comparisons(review.slot, target) }
    val changed = comparisons.filter { it.changed() }
    val unchanged = comparisons.filterNot { it.changed() }
    val why =
        if (target != null) {
            val count = review.advice.supportingSessionIds().size
            "Based on $count comparable recorded ${if (count == 1) "workout" else "workouts"} and their recorded effort."
        } else review.advice.reason()
    val suggestedSlot =
        if (target == null) review.slot
        else
            GymWorkoutDefinition.Slot(
                review.slot.id(),
                review.slot.exercise(),
                target,
                review.slot.supersetGroup(),
            )
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 20.dp).testTag("progression-review"),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TextButton(
                    onClick = keepCurrent,
                    enabled = !busy,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Keep my current target")
                }
                Text(
                    "Your workout suggestion",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    review.slot.exercise().name(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "Here's how we'd suggest approaching your next workout.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item {
                SemanticCard(Tile.Planned) {
                    ProgressionHeading("Here's what to do", ProgressionSymbol.TASK)
                    if (
                        review.advice.action() ==
                            org.racedaydreams.domain.strength.StrengthProgressionAdvisor.Action
                                .REDUCE_OR_SIMPLIFY
                    ) {
                        Text(review.advice.reason())
                        Text(
                            "Review the current target in Advanced details before choosing your next task."
                        )
                    } else {
                        StrengthPrescriptionText.instructions(suggestedSlot).forEach { Text(it) }
                        if (target == null)
                            Text("Your current target is shown; no increase is suggested.")
                    }
                }
            }
            item {
                SemanticCard(Tile.Progress) {
                    ProgressionHeading("About the weight", ProgressionSymbol.WEIGHT)
                    StrengthPrescriptionText.weight(review.slot, target).forEach { Text(it) }
                }
            }
            if (!advanced) item(key = "summary-reason") { ProgressionReason(why) }
            item(key = "advanced-control") {
                ProgressionDisclosure(
                    "Advanced details",
                    "Training targets and supporting evidence",
                    advanced,
                    ProgressionSymbol.DETAILS,
                ) {
                    advanced = !advanced
                    if (!advanced) supporting = false
                }
            }
            if (advanced) {
                item(key = "target-changes") {
                    ProgressionPanel {
                        ProgressionHeading("What's changing?", ProgressionSymbol.CHANGE)
                        if (changed.isEmpty())
                            Text(
                                "No target changes to review.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        changed.forEach { change -> ProgressionChange(change) }
                    }
                }
                item(key = "unchanged-targets") {
                    ProgressionPanel {
                        ProgressionHeading("What's staying the same?", ProgressionSymbol.KEEP)
                        if (unchanged.isEmpty()) Text("No unchanged targets are specified.")
                        val large = LocalDensity.current.fontScale >= 1.5f
                        if (large)
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                unchanged.forEach {
                                    ProgressionUnchanged(it, Modifier.fillMaxWidth())
                                }
                            }
                        else
                            FlowRow(
                                maxItemsInEachRow = 2,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                unchanged.forEach { ProgressionUnchanged(it, Modifier.weight(1f)) }
                            }
                        val scales =
                            listOfNotNull(
                                    review.slot.target()?.effort()?.scale(),
                                    target?.effort()?.scale(),
                                )
                                .distinct()
                        if (scales.isEmpty())
                            Text(
                                "No effort target is specified.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        scales.forEach { scale ->
                            Text(
                                if (scale == GymWorkoutDefinition.EffortScale.RPE)
                                    "RPE = perceived effort (0–10)."
                                else "RIR = repetitions left before failure.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item(key = "advanced-reason") { ProgressionReason(why) }
                item(key = "supporting-control") {
                    val count = review.advice.supportingSessionIds().size
                    ProgressionDisclosure(
                        "View supporting workouts",
                        "$count comparable recorded ${if (count == 1) "workout" else "workouts"}",
                        supporting,
                        ProgressionSymbol.EVIDENCE,
                    ) {
                        supporting = !supporting
                    }
                }
                if (supporting) {
                    item(key = "supporting-records") {
                        ProgressionPanel {
                            ProgressionHeading("Supporting workouts", ProgressionSymbol.EVIDENCE)
                            Text(
                                "Actual logged working sets",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            if (review.supportingWorkouts.isEmpty())
                                Text("No comparable workout records are available in this review.")
                            review.supportingWorkouts.forEach { workout ->
                                Text(
                                    formatDate(
                                        CalendarDates.atOffset(
                                            workout.startedAt(),
                                            workout.offset(),
                                        )
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                workout
                                    .exercises()
                                    .filter { block ->
                                        block.exercise().id() == review.slot.exercise().id() &&
                                            block.exercise().mode() ==
                                                review.slot.exercise().mode() &&
                                            block.exercise().measurement() ==
                                                review.slot.exercise().measurement()
                                    }
                                    .forEach { block ->
                                        block
                                            .sets()
                                            .filterNot { it.warmUp() }
                                            .forEach { set ->
                                                val durationSeconds = set.durationSeconds()
                                                val distanceMetres = set.distanceMetres()
                                                val rpe = set.rpe()
                                                val repetitionsInReserve =
                                                    set.repetitionsInReserve()
                                                val task =
                                                    when {
                                                        durationSeconds != null ->
                                                            StrengthPrescriptionText.duration(
                                                                durationSeconds
                                                            )
                                                        distanceMetres != null ->
                                                            "${StrengthPrescriptionText.number(distanceMetres)} metres"
                                                        else -> "${set.reps()} repetitions"
                                                    }
                                                val load =
                                                    if (block.exercise().measurement().usesLoad())
                                                        "${StrengthPrescriptionText.number(set.kilograms())} kg recorded · "
                                                    else ""
                                                Text(load + task)
                                                Text(
                                                    when {
                                                        rpe != null ->
                                                            "Recorded perceived effort (RPE): ${StrengthPrescriptionText.number(rpe)}/10"
                                                        repetitionsInReserve != null ->
                                                            "Recorded repetitions left before failure (RIR): ${StrengthPrescriptionText.number(repetitionsInReserve)}"
                                                        else -> "Effort was not recorded."
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                    }
                            }
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Text(
                                "Calculation & safeguards",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(review.advice.reason())
                            Text(
                                "Only recent exact-exercise working sets support an increase. Warm-ups do not count; missing effort stays unknown. Saving rechecks the evidence.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "Saving this suggestion won't change your active programme. You can review and activate it separately."
                )
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (target != null)
                item {
                    Button(
                        onClick = saveSuggestion,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Text("Save this suggestion for review")
                    }
                }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

private enum class ProgressionSymbol {
    TASK,
    WEIGHT,
    DETAILS,
    CHANGE,
    KEEP,
    EVIDENCE,
    INFO,
    DOWN,
    UP,
}

/** Original decorative vector symbols; labels and values carry the accessible information. */
@Composable
private fun ProgressionGlyph(symbol: ProgressionSymbol, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(28.dp).clearAndSetSemantics {}) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        fun line(x: Float, y: Float, xx: Float, yy: Float) =
            drawLine(ink, Offset(w * x, h * y), Offset(w * xx, h * yy), stroke, StrokeCap.Round)
        when (symbol) {
            ProgressionSymbol.TASK,
            ProgressionSymbol.KEEP -> {
                drawCircle(ink, w * .4f, style = Stroke(stroke))
                line(.28f, .5f, .43f, .65f)
                line(.43f, .65f, .73f, .34f)
            }
            ProgressionSymbol.WEIGHT -> {
                line(.25f, .5f, .75f, .5f)
                line(.2f, .32f, .2f, .68f)
                line(.33f, .2f, .33f, .8f)
                line(.67f, .2f, .67f, .8f)
                line(.8f, .32f, .8f, .68f)
            }
            ProgressionSymbol.CHANGE -> {
                line(.18f, .68f, .82f, .32f)
                line(.59f, .28f, .82f, .32f)
                line(.82f, .32f, .78f, .56f)
            }
            ProgressionSymbol.DETAILS,
            ProgressionSymbol.EVIDENCE -> {
                for (y in listOf(.28f, .5f, .72f)) {
                    drawCircle(ink, stroke * .6f, Offset(w * .2f, h * y))
                    line(.37f, y, .82f, y)
                }
            }
            ProgressionSymbol.INFO -> {
                drawCircle(ink, w * .4f, style = Stroke(stroke))
                drawCircle(ink, stroke * .6f, Offset(w * .5f, h * .3f))
                line(.5f, .46f, .5f, .72f)
            }
            ProgressionSymbol.DOWN -> {
                line(.25f, .4f, .5f, .65f)
                line(.5f, .65f, .75f, .4f)
            }
            ProgressionSymbol.UP -> {
                line(.25f, .6f, .5f, .35f)
                line(.5f, .35f, .75f, .6f)
            }
        }
    }
}

@Composable
private fun ProgressionHeading(title: String, symbol: ProgressionSymbol) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressionGlyph(symbol)
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
    }
}

@Composable
private fun ProgressionPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun ProgressionReason(why: String) {
    ProgressionPanel {
        ProgressionHeading("Why this suggestion?", ProgressionSymbol.INFO)
        Text(why, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ProgressionDisclosure(
    title: String,
    subtitle: String,
    expanded: Boolean,
    symbol: ProgressionSymbol,
    toggle: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = 72.dp)
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (expanded) "Collapse $title" else "Expand $title",
                    onClick = toggle,
                )
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressionGlyph(symbol)
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ProgressionGlyph(
                if (expanded) ProgressionSymbol.UP else ProgressionSymbol.DOWN,
                Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ProgressionChange(change: StrengthPrescriptionText.TargetComparison) {
    Column(
        Modifier.fillMaxWidth().testTag("change-${change.key()}"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(requireNotNull(change.label()), style = MaterialTheme.typography.titleSmall)
        BoxWithConstraints {
            val stack = LocalDensity.current.fontScale >= 1.5f || maxWidth < 280.dp
            if (stack)
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProgressionValue(change, suggested = false, Modifier.fillMaxWidth())
                    ProgressionValue(change, suggested = true, Modifier.fillMaxWidth())
                }
            else
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProgressionValue(change, suggested = false, Modifier.weight(1f))
                    Text(
                        "→",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                    ProgressionValue(change, suggested = true, Modifier.weight(1f))
                }
        }
        if (change.key() == "weight-reference")
            Text(
                "Optional planning reference · not a logged weight",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

@Composable
private fun ProgressionValue(
    change: StrengthPrescriptionText.TargetComparison,
    suggested: Boolean,
    modifier: Modifier,
) {
    Column(
        modifier
            .background(
                if (suggested) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(14.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val reference = change.key() == "weight-reference"
        Text(
            if (suggested) (if (reference) "Suggested reference" else "Suggested")
            else (if (reference) "Current reference" else "Current"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (suggested) requireNotNull(change.proposedValue())
            else requireNotNull(change.currentValue()),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color =
                if (suggested) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier.testTag(
                    "change-${change.key()}-${if (suggested) "suggested" else "current"}"
                ),
        )
    }
}

@Composable
private fun ProgressionUnchanged(
    detail: StrengthPrescriptionText.TargetComparison,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("unchanged-${detail.key()}"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            requireNotNull(detail.label()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(requireNotNull(detail.currentValue()), style = MaterialTheme.typography.titleMedium)
    }
}
