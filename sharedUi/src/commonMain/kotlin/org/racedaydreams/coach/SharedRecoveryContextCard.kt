package org.racedaydreams.coach

import androidx.compose.material3.*
import androidx.compose.runtime.*
import org.racedaydreams.domain.health.RecoveryCoaching

/** Existing preview/keep/accept interaction; never applies a replacement without athlete choice. */
@Composable
fun SharedRecoveryContextCard(r: RecoveryCoaching.Context, chooseEasy: ((String) -> Unit)?) {
    if (r.reviewSuggested())
        SemanticCard(Tile.Insight) {
            Text("Consider an easier option", style = MaterialTheme.typography.titleMedium)
            Text(
                "Several signals support reviewing today's demand. This is training context, not a diagnosis."
            )
            WhyDetails(requireNotNull(r.evidence()).joinToString("\n"), "Why?")
            Text("Keep the plan, run by effort, move or rest. Your goal stays unchanged.")
            if (chooseEasy != null) {
                var confirm by remember { mutableStateOf(false) }
                TextButton(onClick = { confirm = true }) { Text("Preview easy alternative") }
                if (confirm)
                    AlertDialog(
                        onDismissRequest = { confirm = false },
                        title = { Text("Replace only this session with easy running?") },
                        text = {
                            Text(
                                "The same distance/time ceiling is retained at conversational effort, RPE about 3. No extra mileage is added. Other sessions, your goal and recurring days stay unchanged. The original version is retained."
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    confirm = false
                                    chooseEasy(requireNotNull(r.evidence()).joinToString(" "))
                                }
                            ) {
                                Text("Choose easy")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirm = false }) { Text("Keep original") }
                        },
                    )
            }
        }
}
