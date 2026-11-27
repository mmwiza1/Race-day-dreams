package org.racedaydreams.recording

import kotlinx.serialization.json.*
import org.racedaydreams.domain.recording.RunTroubleshooting
import org.racedaydreams.storage.recordingTroubleshootingJson
import platform.Foundation.NSUserDefaults

/** Twelve small, device-local reports. App workout backups do not read these preferences. */
internal object AppleRecordingDiagnostics {
    private const val KEY = "recording.troubleshooting.v1"

    fun recent(): List<JsonObject> =
        NSUserDefaults.standardUserDefaults
            .stringForKey(KEY)
            ?.let { encoded ->
                runCatching {
                        Json.parseToJsonElement(encoded).jsonArray.take(12).map { it.jsonObject }
                    }
                    .getOrDefault(emptyList())
            }
            .orEmpty()

    fun write(session: RecordingStageSession, state: String, trace: RunTroubleshooting.Snapshot) {
        val report =
            recordingTroubleshootingJson(session.id, session.start.toString(), state, trace)
        val rows = recent().filter { it["sessionId"]?.jsonPrimitive?.content != session.id }
        val retained = JsonArray(listOf(report) + rows.take(11))
        NSUserDefaults.standardUserDefaults.setObject(retained.toString(), forKey = KEY)
    }
}
