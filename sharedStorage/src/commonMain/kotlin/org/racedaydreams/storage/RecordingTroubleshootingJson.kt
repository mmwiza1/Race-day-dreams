package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.recording.RunTroubleshooting

/** The JSON schema has no access to route points, sample values or workout notes. */
fun recordingTroubleshootingJson(
    id: String,
    startedAt: String,
    state: String,
    trace: RunTroubleshooting.Snapshot,
): JsonObject = buildJsonObject {
    put("format", "rdd-recording-troubleshooting")
    put("version", 1)
    put("sessionId", id)
    put("startedAt", startedAt)
    put("lastCheckpointState", state)
    put("lastCheckpointElapsedMillis", trace.checkpointElapsedMillis)
    putJsonObject("delivery") {
        put("callbacks", trace.callbacks)
        put("batchedCallbacks", trace.batchedCallbacks)
        put("callbackObservations", trace.callbackObservations)
        put("processedFixes", trace.deliveredFixes)
        put("fixesOverTenSecondsLate", trace.lateFixes)
        put("longestCallbackGapMillis", trace.longestCallbackGapMillis)
        put("longestAcceptedGapMillis", trace.longestAcceptedGapMillis)
        put("maximumReceiptDelayMillis", trace.maximumReceiptDelayMillis)
    }
    putJsonObject("routeEvidence") {
        put("acceptedOnDelivery", trace.acceptedFixes)
        putJsonObject("rejectedOrDeferred") {
            trace.rejected.forEach { (reason, count) -> put(reason, count) }
        }
        put("stagedInMemory", trace.pointCount)
        put("acknowledgedOnDisk", trace.acknowledgedPointCount)
        put("flushFailures", trace.flushFailures)
    }
    put("omittedTimelineEvents", trace.omittedEvents)
    putJsonArray("timeline") {
        trace.checkpoints.forEach { row ->
            addJsonObject {
                put("elapsedMillis", row.elapsedMillis)
                put("kind", row.kind)
                put("observationElapsedMillis", row.observationMillis)
                put("count", row.count)
            }
        }
    }
}
