package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import kotlinx.serialization.json.*

@Composable
fun DiagnosticReview(report: String, dismiss: () -> Unit, export: () -> Unit) {
    val json =
        remember(report) {
            runCatching { Json.parseToJsonElement(report) as? JsonObject }.getOrNull()
        }
    var raw by rememberSaveable(report) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Review diagnostic report") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CoachSpace.Related),
            ) {
                if (json != null) {
                    OutlinedButton(
                        onClick = { raw = !raw },
                        modifier = Modifier.heightIn(min = CoachSpace.Touch),
                    ) {
                        Text(if (raw) "Show summary" else "View full JSON")
                    }
                }
                if (raw || json == null) {
                    Text(
                        report,
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                    )
                } else {
                    val recording = json.objectField("recording")
                    if (recording == null) {
                        DiagnosticMetric("App version", json.stringField("appVersion"))
                        DiagnosticMetric("Device", json.stringField("deviceModel"))
                        DiagnosticMetric(
                            "System version",
                            json.stringField("androidVersion") ?: json.stringField("iosVersion"),
                        )
                        Text("The full report includes permissions and other device settings.")
                    } else {
                        DiagnosticMetric(
                            "Last recording state",
                            recording.stringField("lastCheckpointState")?.let {
                                when (it) {
                                    "saved" -> "Saved"
                                    "recording" -> "Recording at last update"
                                    "interrupted" -> "Interrupted"
                                    "discarded" -> "Discarded"
                                    "staging_failed" -> "Saving points failed"
                                    else ->
                                        it.replace('_', ' ').replaceFirstChar { char ->
                                            char.uppercaseChar()
                                        }
                                }
                            },
                        )
                        val delivery = recording.objectField("delivery")
                        val evidence = recording.objectField("routeEvidence")
                        DiagnosticMetric("GPS callbacks", delivery?.stringField("callbacks"))
                        DiagnosticMetric(
                            "Longest callback gap",
                            delivery?.stringField("longestCallbackGapMillis")?.let { "$it ms" },
                        )
                        DiagnosticMetric(
                            "GPS fixes received over 10 seconds late",
                            delivery?.stringField("fixesOverTenSecondsLate"),
                        )
                        DiagnosticMetric(
                            "Accepted fixes",
                            evidence?.stringField("acceptedOnDelivery"),
                        )
                        val rejected = evidence?.objectField("rejectedOrDeferred")
                        DiagnosticMetric(
                            "Rejected or deferred fixes",
                            rejected?.let { reasons ->
                                if (reasons.isEmpty()) "0"
                                else
                                    reasons.entries.joinToString(" · ") { (reason, count) ->
                                        "${reason.replace('_', ' ')}: ${(count as? JsonPrimitive)?.contentOrNull ?: "unknown"}"
                                    }
                            },
                        )
                        DiagnosticMetric(
                            "Point save failures",
                            evidence?.stringField("flushFailures"),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = export, modifier = Modifier.heightIn(min = CoachSpace.Touch)) {
                Text("Export JSON")
            }
        },
        dismissButton = {
            TextButton(onClick = dismiss, modifier = Modifier.heightIn(min = CoachSpace.Touch)) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun DiagnosticMetric(label: String, value: String?) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value ?: "Unknown", style = MaterialTheme.typography.bodyMedium)
    }
}

private fun JsonObject.objectField(key: String): JsonObject? = get(key) as? JsonObject

private fun JsonObject.stringField(key: String): String? =
    (get(key) as? JsonPrimitive)?.contentOrNull
