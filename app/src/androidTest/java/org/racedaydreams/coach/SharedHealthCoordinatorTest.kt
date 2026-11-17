package org.racedaydreams.coach

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.application.*
import org.racedaydreams.data.AndroidSqlConnection
import org.racedaydreams.data.createAndroidSchemaFixture
import org.racedaydreams.domain.health.HealthImportRepository
import org.racedaydreams.health.*
import org.racedaydreams.storage.*

/**
 * Platform SDK results are supplied at the boundary; original common permission/review rules run.
 */
@RunWith(AndroidJUnit4::class)
class SharedHealthCoordinatorTest {
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private class Gateway : HealthGateway {
        var available = HealthAvailability.AVAILABLE
        var granted = emptySet<String>()
        var calls = 0
        var reads = 0

        override fun availability() = available

        override suspend fun grantedPermissions(): Set<String> {
            reads++
            return granted
        }

        override suspend fun sync(store: HealthImportRepository): HealthSyncResult {
            calls++
            return HealthSyncResult(
                HealthSyncStatus.PARTIAL,
                message = "Only available run evidence imported.",
            )
        }
    }

    private class Fixture(
        val gateway: Gateway,
        val service: HealthCoordinator,
        val runner: CoachOperationRunner,
        val settings: SqliteSettings,
        val db: SqlDatabase,
        val bodyCalls: () -> Int,
    )

    private fun fixture(block: (Fixture) -> Unit) {
        val raw = SQLiteDatabase.create(null)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            createAndroidSchemaFixture(raw)
            val db = AndroidSqlConnection { raw }
            val settings = SqliteSettings(db)
            settings.putSetting("retained.setting", "User choice")
            val gateway = Gateway()
            var bodyCalls = 0
            val runner = CoachOperationRunner(scope, { CoachUiState(loading = false) })
            val service =
                HealthCoordinator(
                    runner,
                    gateway,
                    SqliteActivities(db) { Instant.parse("2026-11-17T09:00:00Z") },
                    settings,
                    setOf("weight", "height"),
                    {
                        bodyCalls++
                        "Only available body measurements imported."
                    },
                )
            block(Fixture(gateway, service, runner, settings, db, { bodyCalls }))
        } finally {
            instrumentation.runOnMainSync { scope.cancel() }
            raw.close()
        }
    }

    private fun execute(f: Fixture, action: () -> Unit): CoachUiState {
        instrumentation.runOnMainSync { action() }
        return runBlocking { withTimeout(20_000) { f.runner.state.first { !it.busy } } }
    }

    @Test
    fun declinedAccessRefreshesWithoutImportOrDiscardingManualPreferences() = fixture { f ->
        val state = execute(f) { f.service.permissionResult(emptySet()) }
        assertNull(state.error)
        assertTrue(state.message!!.contains("continue with optional manual details"))
        assertEquals(0, f.gateway.calls)
        assertEquals(0, f.bodyCalls())
        assertEquals("User choice", f.settings.getSetting("retained.setting"))
        assertTrue(f.db.query("SELECT id FROM activities").isEmpty())
    }

    @Test
    fun explicitSyncReadsBodyOnlyForActualGrantedBodyPermissions() = fixture { f ->
        f.gateway.granted = setOf("exercise")
        val runs = execute(f) { f.service.syncHealth() }
        assertNull(runs.error)
        assertEquals("Only available run evidence imported.", runs.message)
        assertEquals(1, f.gateway.calls)
        assertEquals(0, f.bodyCalls())
        f.gateway.granted = setOf("height")
        val partial = execute(f) { f.service.permissionResult(setOf("height")) }
        assertNull(partial.error)
        assertEquals(
            "Only available run evidence imported.\nOnly available body measurements imported.",
            partial.message,
        )
        assertEquals(2, f.gateway.calls)
        assertEquals(1, f.bodyCalls())
        // A later revocation must not reuse the preceding permission set.
        f.gateway.granted = emptySet()
        assertNull(execute(f) { f.service.syncHealth() }.error)
        assertEquals(3, f.gateway.calls)
        assertEquals(1, f.bodyCalls())
    }

    @Test
    fun automaticImportRespectsReviewAvailabilityAndMissingPermissionsWithoutClaimingCoverage() =
        fixture { f ->
            runBlocking {
                f.service.beforeRefresh(false)
                f.service.beforeRefresh(true)
            }
            assertEquals(0, f.gateway.calls)
            f.gateway.granted = setOf("exercise")
            runBlocking { f.service.beforeRefresh(true) }
            assertEquals(1, f.gateway.calls)
            assertEquals("Only available run evidence imported.", f.runner.current.message)
            assertEquals(0, f.bodyCalls())
            f.settings.putSetting("baseline.reviewed", "true")
            runBlocking { f.service.beforeRefresh(true) }
            assertEquals(1, f.gateway.calls)
            f.settings.putSetting("baseline.reviewed", "false")
            f.gateway.available = HealthAvailability.UNAVAILABLE
            val reads = f.gateway.reads
            runBlocking { f.service.beforeRefresh(true) }
            assertEquals(reads, f.gateway.reads)
            assertEquals(1, f.gateway.calls)
            assertTrue(f.db.query("SELECT id FROM activities").isEmpty())
        }
}
