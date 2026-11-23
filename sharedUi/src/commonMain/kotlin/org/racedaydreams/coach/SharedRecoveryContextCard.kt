package org.racedaydreams.coach

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.health.RecoveryCoaching

/** Existing preview/keep/accept interaction; never applies a replacement without athlete choice. */
@Composable
fun SharedRecoveryContextCard(r: RecoveryCoaching.Context, chooseEasy: ((String) -> Unit)?) {
    if (r.reviewSuggested())
        ExpressiveSection("Consider an easier option", Glyph.BODY, ChartHue.TEAL) {
            Text(
                "Several signals support reviewing today's demand. This is training context, not a diagnosis."
            )
            WhyDetails(requireNotNull(r.evidence()).joinToString("\n"), "Why?")
            Text("Keep the plan, run by effort, move or rest. Your goal stays unchanged.")
            if (chooseEasy != null) {
                var confirm by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { confirm = true },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Preview easy alternative")
                }
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
