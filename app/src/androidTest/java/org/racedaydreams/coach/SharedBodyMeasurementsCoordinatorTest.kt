package org.racedaydreams.coach

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.body.*
import org.racedaydreams.data.*
import org.racedaydreams.domain.body.*

/** Real existing Android body SQL; only the optional SDK source is a declared fixture. */
@RunWith(AndroidJUnit4::class)
class SharedBodyMeasurementsCoordinatorTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val now = Instant.parse("2026-11-17T18:00:00.123456789Z")
    private val offset = ZoneOffset.ofHours(14)

    private class Fixture(
        val model: SharedBodyMeasurementsCoordinator,
        val repository: SqliteBodyMeasurementsRepository,
        val clock: AtomicReference<Instant>,
        val providerCalls: AtomicInteger,
    )

    private fun fixture(available: Boolean = false, block: (Fixture) -> Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        runBlocking(Dispatchers.IO) { SqliteUserPreferencesRepository(app).clear() }
        try {
            val repository = SqliteBodyMeasurementsRepository(app)
            val focus = SqlitePrimaryFocusRepository(app)
            val clock = AtomicReference(now)
            val ids = AtomicInteger()
            val calls = AtomicInteger()
            val model = AtomicReference<SharedBodyMeasurementsCoordinator>()
            instrumentation.runOnMainSync {
                model.set(
                    SharedBodyMeasurementsCoordinator(
                        repository,
                        { focus.load().orElse(null) },
                        {
                            calls.incrementAndGet()
                            available
                        },
                        { emptySet() },
                        { same ->
                            assertTrue(same === repository)
                            calls.incrementAndGet()
                            throw IllegalStateException("Read access revoked")
                        },
                        scope,
                        clock::get,
                        { offset },
                        { "body-event-${ids.incrementAndGet()}" },
                    )
                )
            }
            assertEquals("No provider IO during construction", 0, calls.get())
            block(Fixture(model.get(), repository, clock, calls))
        } finally {
            instrumentation.runOnMainSync { scope.cancel() }
            runBlocking(Dispatchers.IO) { SqliteUserPreferencesRepository(app).clear() }
        }
    }

    private fun command(f: Fixture, action: () -> Unit): BodyMeasurementsState {
        instrumentation.runOnMainSync { action() }
        runBlocking { withTimeout(20000) { f.model.state.first { !it.busy } } }
        instrumentation.waitForIdleSync()
        return f.model.state.value
    }

    private fun <T> io(block: () -> T): T = runBlocking(Dispatchers.IO) { block() }

    @Test
    fun manualMeasurementsRetainExactSourceAndProteinNeedsExplicitFreshWeightSelection() =
        fixture { f ->
            val empty = command(f) { f.model.refresh() }
            assertNull(empty.error)
            assertFalse(empty.available)
            assertTrue(empty.granted.isEmpty())
            assertFalse(empty.advice!!.available())
            command(f) { f.model.protein(true) }
            val height =
                command(f) { f.model.saveHeight("180", HeightUnit.CENTIMETRES) }.heights.single()
            assertEquals(1.8, height.metres(), 0.0000001)
            assertEquals(now, height.at())
            assertEquals(offset, height.offset())
            assertEquals("manual", height.provider())
            assertEquals(height.id(), height.sourceId())
            assertNull(height.importedAt())
            val saved = command(f) { f.model.saveWeight("87", MassUnit.KG) }
            assertNull(saved.error)
            val weight = saved.weights.single()
            assertEquals(now, weight.at())
            assertEquals(offset, weight.offset())
            assertEquals("manual", weight.provider())
            assertEquals(weight.id(), weight.sourceId())
            assertEquals(87.0, weight.kilograms(), 0.0)
            assertNull(saved.preferences.basis())
            assertFalse(saved.advice!!.available())
            val chosen = command(f) { f.model.chooseWeight(weight.id()) }
            assertNull(chosen.error)
            assertEquals(weight.id(), chosen.preferences.basis()!!.id())
            assertTrue(chosen.advice!!.available())
            assertEquals(weight, chosen.advice!!.basis())
            f.clock.set(now.plus(Duration.ofDays(91)))
            val stale = command(f) { f.model.refresh() }
            assertNull(stale.error)
            assertEquals(chosen.preferences, stale.preferences)
            assertEquals(weight, stale.weights.single())
            assertEquals(height, stale.heights.single())
            assertFalse(stale.advice!!.available())
            assertNull(stale.advice!!.minimumGrams())
            assertNull(stale.advice!!.maximumGrams())
            assertTrue(io { CoachingStore(app).strength().isEmpty() })
        }

    @Test
    fun heightPagingAndRevokedProviderReadPreserveManualHistoryAndDoNotInferGrants() =
        fixture(available = true) { f ->
            io {
                repeat(45) { index ->
                    f.repository.saveHeight(
                        BodyHeight(
                            "history-$index",
                            1.75,
                            now.minusSeconds(index.toLong()),
                            offset,
                            "manual",
                            "history-$index",
                            null,
                            null,
                        )
                    )
                }
            }
            val first = command(f) { f.model.refresh() }
            assertNull(first.error)
            assertTrue(first.available)
            assertTrue(first.granted.isEmpty())
            assertEquals(20, first.heights.size)
            assertTrue(first.moreHeights)
            val second = command(f) { f.model.moreHeights() }
            assertEquals(40, second.heights.size)
            assertTrue(second.moreHeights)
            val third = command(f) { f.model.moreHeights() }
            assertEquals(45, third.heights.size)
            assertEquals(45, third.heights.map { it.id() }.toSet().size)
            assertFalse(third.moreHeights)
            val failed = command(f) { f.model.sync() }
            assertEquals("Read access revoked", failed.error)
            assertEquals(third.heights, failed.heights)
            assertEquals(third.preferences, failed.preferences)
            assertTrue(failed.granted.isEmpty())
            assertEquals(third.heights, io { f.repository.heights(100, 0) })
            val retry = command(f) { f.model.refresh() }
            assertNull(retry.error)
            assertEquals(third.heights, retry.heights)
        }
}
