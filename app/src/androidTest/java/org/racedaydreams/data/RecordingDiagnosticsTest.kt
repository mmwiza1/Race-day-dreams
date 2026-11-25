package org.racedaydreams.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.racedaydreams.domain.recording.RunTroubleshooting

class RecordingDiagnosticsTest {
    @Test
    fun persistedReportRetainsCoordinateFreeEvidenceAndDiskAcknowledgements() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val id = UUID.randomUUID().toString()
        val trace = RunTroubleshooting(1_000)
        trace.event("service_started", 1_000)
        trace.callback(16_000, 2, 2_000)
        trace.fix(16_000, 2_000, false, "GPS_FIX_STALE")
        trace.fix(16_001, 16_000, true, null)
        trace.flushFailed(17_000)
        RecordingDiagnostics(context)
            .write(id, Instant.EPOCH, "staging_failed", trace.snapshot(17_000, 1, 0))
        val report =
            RecordingDiagnostics(context).recent().first { it.getString("sessionId") == id }
        assertEquals("rdd-recording-troubleshooting", report.getString("format"))
        assertEquals(1, report.getJSONObject("delivery").getInt("batchedCallbacks"))
        assertEquals(1, report.getJSONObject("routeEvidence").getInt("flushFailures"))
        assertEquals(0, report.getJSONObject("routeEvidence").getInt("acknowledgedOnDisk"))
        assertTrue(report.toString().contains("GPS_FIX_STALE"))
        for (privateField in
            listOf("latitude", "longitude", "heartRate", "notes", "healthPermissions")) assertFalse(
            report.toString().contains(privateField)
        )
    }
}
