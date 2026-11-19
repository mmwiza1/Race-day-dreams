package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.TrainingPlan
import org.racedaydreams.domain.coaching.WeeklyCoaching
import org.racedaydreams.domain.presentation.WeeklyReviewData
import org.racedaydreams.domain.time.LocalDate

/** Actual weekly review and explicit preview/accept flow; platform owns storage and formatting. */
@Composable
fun SharedWeeklyReviewContent(
    review: WeeklyCoaching.Review,
    currentPlan: TrainingPlan?,
    decision: String?,
    busy: Boolean,
    editRoutine: () -> Unit,
    onDecision: (String) -> Unit,
    formatDate: (LocalDate) -> String,
    formatShortDate: (LocalDate) -> String,
    formatNumber: (Double) -> String,
    workoutSummary: (PlannedWorkout) -> String,
) {
    Text("Week of ${formatDate(requireNotNull(review.week()))}")
    val presentation = WeeklyReviewData.of(review)
    val attention = requireNotNull(presentation.attention())
    val positives = requireNotNull(presentation.positives())
    val recommendation = requireNotNull(review.recommendation())
    GuidanceCard(if (presentation.noChange()) guidanceMint() else guidanceBlue()) {
        Text(
            if (!presentation.noChange() || attention.isNotEmpty()) "Something may need adjusting"
            else if (review.completed() < 2) "Still learning your training"
            else "Training is on track",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text("${review.completed()} of ${review.planned()} planned runs completed")
        Text(
            "${formatNumber(review.kilometres())} km" +
                (review.longestKm()?.let { " · longest ${formatNumber(it)} km" } ?: "")
        )
        Text(
            if (attention.isNotEmpty()) attention.first()
            else if (review.completed() < 2)
                "More recorded training will help us understand your response."
            else "No meaningful warning signals in this week's recorded evidence."
        )
        WhyDetails(
            (positives.map { "✓ $it" } + attention.map { "△ $it" }).joinToString("\n\n").ifBlank {
                "More recorded training will help us see your response."
            },
            "Why?",
        )
    }
    WhyDetails(review.observations().joinToString("\n\n"), "Week observations")
    if (review.observations().any { it.contains("missed or moved") })
        TextButton(onClick = editRoutine) { Text("Review training days") }
    WhyDetails(
        review.channels().joinToString("\n\n") {
            "${it.name()}: ${it.state()?.name?.lowercase()?.replace('_', ' ')}\n${it.reason()}"
        },
        "Evidence channels",
    )
    CoachHeading("Recommendation")
    CoachCard {
        Text(
            if (presentation.noChange()) "No adjustment required" else "Review the next step",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            if (presentation.noChange()) {
                if (review.completed() >= 2)
                    "Your recent training supports continuing the current plan."
                else "Continue the current plan while more training evidence becomes available."
            } else recommendation
        )
    }
    var previewId by rememberSaveable(review.id()) { mutableStateOf<String?>(null) }
    val preview = review.choices().firstOrNull { it.id() == previewId }
    if (decision != null) Text("Your decision: $decision. Your choice is retained.")
    else
        review.choices().forEach { choice ->
            OutlinedButton(
                onClick = {
                    if (choice.replacements().isEmpty()) onDecision(requireNotNull(choice.id()))
                    else previewId = choice.id()
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                choice.label()?.let { Text(it) }
            }
        }
    if (preview != null)
        AlertDialog(
            onDismissRequest = { previewId = null },
            title = { Text("Preview plan change") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        if (preview.id() == "paces") "Optional quality-pace reassessment"
                        else recommendation
                    )
                    review
                        .channels()
                        .firstOrNull { it.name() == "Performance trend" }
                        ?.takeIf { preview.id() == "paces" }
                        ?.reason()
                        ?.let {
                            Text(it.substringBefore("."))
                            WhyDetails(it, "Why these pace ranges?")
                        }
                    if (preview.id() != "paces") {
                        review
                            .observations()
                            .firstOrNull {
                                it.contains("effort increased", true) ||
                                    it.contains("below 80%") ||
                                    it.contains("slowed")
                            }
                            ?.let { Text(it) }
                        WhyDetails(review.observations().joinToString("\n\n"), "Why this change?")
                    }
                    preview.replacements().forEach { next ->
                        val current = currentPlan?.workouts()?.firstOrNull { it.id() == next.id() }
                        Text(
                            formatShortDate(next.date()),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            "Current: ${current?.let(workoutSummary) ?: "Preserved workout unavailable"}"
                        )
                        Text("Proposed: ${workoutSummary(next)}")
                    }
                    Text("Other sessions stay unchanged. Your race goal stays unchanged.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDecision(requireNotNull(preview.id()))
                        previewId = null
                    },
                    enabled = !busy,
                ) {
                    Text("Accept change")
                }
            },
            dismissButton = {
                TextButton(onClick = { previewId = null }) { Text("Keep original plan") }
            },
        )
}
