package org.racedaydreams.coach.documents

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.racedaydreams.health.HealthAvailability

class AppleDiagnosticReportTest {
    @Test
    fun exportsOnlyExplicitlyAllowedMetadata() {
        val report =
            appleDiagnosticReport("0.12.3", "19", "26.0", HealthAvailability.AVAILABLE, false)
        val keys = Json.parseToJsonElement(report.decodeToString()).jsonObject.keys
        assertEquals(
            setOf(
                "format",
                "version",
                "platform",
                "appVersion",
                "versionCode",
                "schema",
                "backupEnvelope",
                "iosVersion",
                "appleHealthAvailability",
                "locationGranted",
                "preciseLocationGranted",
            ),
            keys,
        )
    }
}
