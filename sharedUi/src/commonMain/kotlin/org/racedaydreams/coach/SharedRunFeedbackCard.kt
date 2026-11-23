package org.racedaydreams.coach

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.round
import org.racedaydreams.domain.coaching.RunFeedback
import org.racedaydreams.domain.coaching.RunScoreEngine

/**
 * Actual recorded-run feedback on a borderless panel (its section label is supplied by the host),
 * without platform IO, activation or new calculations.
 */
@Composable
fun SharedRunFeedbackCard(result: RunFeedback) {
    Panel {
        if (result.score() != null || result.label() != "Run Analysis") {
            val heading = result.score()?.let { "$it/10 — ${result.label()}" } ?: result.label()
            heading?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        WhyDetails(
            "RPE describes how hard this felt. Run Score describes execution of the intended purpose; neither is a health or fitness-improvement score.",
            "What does this score mean?",
        )
        WhyDetails(
            result.components().joinToString("\n\n") { c ->
                "${c.name()}: ${c.fraction()?.let { "${round(it * 100).toInt()}%" } ?: "Unavailable"}\n${c.explanation()}"
            },
            "Score components",
        )
        if (result.wentWell().isNotEmpty()) {
            FeedbackHeading("What went well")
            result.wentWell().forEach { Text(it) }
        }
        if (result.differed().isNotEmpty()) {
            FeedbackHeading("What differed from the plan")
            result.differed().forEach { Text(it) }
        }
        FeedbackHeading("What we learned")
        result.learned()?.let { Text(it) }
        FeedbackHeading("Next step")
        result.nextStep()?.let { Text(it) }
        WhyDetails(
            "Deterministic product heuristics, rule version ${RunScoreEngine.RULE_VERSION}; not scientifically validated weights. Missing evidence is excluded.",
            "How was this calculated?",
        )
    }
}

@Composable
private fun FeedbackHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp),
    )
}
