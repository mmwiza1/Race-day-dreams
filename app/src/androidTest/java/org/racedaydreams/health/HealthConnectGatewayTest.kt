package org.racedaydreams.health

import android.test.AndroidTestCase
import kotlinx.coroutines.runBlocking
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.health.HealthImportRepository
import java.time.Instant

/** Actual SDK availability/denied-access checks; no provider writes or fabricated records. */
@Suppress("DEPRECATION")
class HealthConnectGatewayTest : AndroidTestCase() {
    fun testAvailabilityDoesNotTouchTheApplicationStore() {
        val gateway = HealthConnectGateway(context)
        assertNotNull(gateway.availability())
        if (gateway.availability() != HealthAvailability.AVAILABLE) {
            assertFalse(gateway.historyPermissionAvailable())
            assertTrue(runBlocking { gateway.grantedPermissions() }.isEmpty())
        }
    }

    fun testUnavailableOrDeniedAccessCannotCommitAnImport() = runBlocking {
        val gateway = HealthConnectGateway(context)
        val grants = gateway.grantedPermissions()
        // This condition holds on fresh CI installs. Never revoke a developer's existing access.
        if (HealthConnectGateway.EXERCISE_PERMISSION !in grants) {
            val store = RejectingStore()
            val result = gateway.sync(store)
            assertTrue(result.status in setOf(HealthSyncStatus.UNAVAILABLE,
                HealthSyncStatus.UPDATE_REQUIRED, HealthSyncStatus.PERMISSION_REQUIRED))
            assertEquals(0, result.importedCount)
            assertNull(result.windowStart)
            assertNull(result.windowEnd)
            assertEquals(0, store.calls)
        }
    }

    fun testRequestedRecordsMatchImplementedReadOnlyFeatures() {
        assertEquals(setOf("android.permission.health.READ_EXERCISE",
            "android.permission.health.READ_DISTANCE"), HealthConnectGateway.BASELINE_PERMISSIONS)
        assertEquals(setOf("android.permission.health.READ_HEART_RATE"), HealthConnectGateway.OPTIONAL_PERMISSIONS)
        assertEquals("android.permission.health.READ_HEALTH_DATA_HISTORY", HealthConnectGateway.HISTORY_PERMISSION)
        assertTrue(HealthConnectGateway.ALL_PERMISSIONS.none { it.contains("WRITE_") || it.contains("BACKGROUND") })
    }

    fun testCoverageCanBeRestoredWithoutReadingHealthRecords() {
        val checkpoint = """{"version":1,"token":"opaque-test-token","permissions":[],"windowStart":"2030-01-01T00:00:00Z","windowEnd":"2030-01-30T00:00:00Z"}"""
        val window = HealthConnectGateway.importWindow(checkpoint)
        assertEquals(Instant.parse("2030-01-01T00:00:00Z"), window?.first)
        assertEquals(Instant.parse("2030-01-30T00:00:00Z"), window?.second)
        assertNull(HealthConnectGateway.importWindow(null))
        assertNull(HealthConnectGateway.importWindow("not JSON"))
        assertNull(HealthConnectGateway.importWindow(checkpoint.replace("\"version\":1", "\"version\":99")))
    }

    fun testMalformedCheckpointCannotAdvertiseFalseCoverage() {
        val checkpoint = """{"version":1,"token":"opaque-test-token","permissions":[],"windowStart":"2030-01-01T00:00:00Z","windowEnd":"2030-01-30T00:00:00Z"}"""
        assertNull(HealthConnectGateway.importWindow(checkpoint.replace("opaque-test-token", "")))
        assertNull(HealthConnectGateway.importWindow(checkpoint.replace("2030-01-30T00:00:00Z", "2029-01-01T00:00:00Z")))
        assertNull(HealthConnectGateway.importWindow(checkpoint.replace("\"version\":1", "\"version\":1.5")))
        assertNull(HealthConnectGateway.importWindow("""{"version":1,"token":"opaque-test-token","permissions":[]}"""))
    }

    fun testWeightPermissionIsSeparateAndSamsungIsUnavailableWithoutSdk() {
        assertEquals("android.permission.health.READ_WEIGHT",HealthConnectGateway.WEIGHT_PERMISSION)
        assertFalse(HealthConnectGateway.BASELINE_PERMISSIONS.contains(HealthConnectGateway.WEIGHT_PERMISSION))
        assertEquals(org.racedaydreams.domain.activity.EnhancedHealthProvider.Availability.SDK_NOT_INCLUDED,SamsungHealthProvider().availability())
        assertTrue(SamsungHealthProvider().readRuns(Instant.now().minusSeconds(3600),Instant.now()).isEmpty())
    }

    private class RejectingStore : HealthImportRepository {
        var calls = 0
        override fun activities(): List<ActivityRecord> { calls++; error("An unauthorized import touched storage") }
        override fun importToken(provider: String): String? { calls++; error("An unauthorized import touched storage") }
        override fun applyImport(provider: String, upserts: List<ActivityRecord>, deletedRecordIds: List<String>, nextToken: String?) {
            calls++; error("An unauthorized import committed data")
        }
    }
}
