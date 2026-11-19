package org.racedaydreams.coach.documents

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.racedaydreams.health.HealthAvailability
import org.racedaydreams.storage.ApplicationSchema
import org.racedaydreams.storage.BackupEnvelopeJson

/** Fixed metadata allow-list. This boundary has no access to athlete records or device IDs. */
internal fun appleDiagnosticReport(
    appVersion: String?,
    buildNumber: String?,
    systemVersion: String,
    healthAvailability: HealthAvailability,
    locationGranted: Boolean,
): ByteArray =
    buildJsonObject {
            put("format", "rdd-diagnostic")
            put("version", 1)
            put("platform", "iOS")
            appVersion?.let { put("appVersion", it) }
            buildNumber?.let { put("versionCode", it) }
            put("schema", ApplicationSchema.VERSION)
            put("backupEnvelope", BackupEnvelopeJson.VERSION)
            put("iosVersion", systemVersion)
            put("appleHealthAvailability", healthAvailability.name)
            put("locationGranted", locationGranted)
        }
        .toString()
        .encodeToByteArray()
