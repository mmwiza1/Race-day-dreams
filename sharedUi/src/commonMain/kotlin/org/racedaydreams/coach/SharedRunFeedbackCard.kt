package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlin.math.round
import org.racedaydreams.domain.coaching.RunFeedback
import org.racedaydreams.domain.coaching.RunScoreEngine

/** Actual recorded-run feedback card, without platform IO, activation or new calculations. */
@Composable
fun SharedRunFeedbackCard(result: RunFeedback) {
    CoachCard {
        if (result.score() != null || result.label() != "Run Analysis") {
            val heading = result.score()?.let { "$it/10 — ${result.label()}" } ?: result.label()
            heading?.let { Text(it, style = MaterialTheme.typography.headlineSmall) }
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
            Text("What went well", style = MaterialTheme.typography.titleMedium)
            result.wentWell().forEach { Text(it) }
        }
        if (result.differed().isNotEmpty()) {
            Text("What differed from the plan", style = MaterialTheme.typography.titleMedium)
            result.differed().forEach { Text(it) }
        }
        Text("What we learned", style = MaterialTheme.typography.titleMedium)
        result.learned()?.let { Text(it) }
        Text("Next step", style = MaterialTheme.typography.titleMedium)
        result.nextStep()?.let { Text(it) }
        WhyDetails(
            "Deterministic product heuristics, rule version ${RunScoreEngine.RULE_VERSION}; not scientifically validated weights. Missing evidence is excluded.",
            "How was this calculated?",
        )
    }
}
