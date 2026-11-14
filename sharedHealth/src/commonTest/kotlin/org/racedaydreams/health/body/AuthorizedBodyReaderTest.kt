package org.racedaydreams.health.body

import kotlin.test.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant

class AuthorizedBodyReaderTest {
    private val from = Instant.parse("2026-01-01T00:00:00Z")
    private val window = BodyReadWindow(from, Instant.parse("2026-02-01T00:00:00Z"))
    private val metric = BodyMetric.WEIGHT_KILOGRAMS

    private fun record(id: String) = BodyObservation(metric, 87.0, from, null, "provider", id, null)

    private class Source(var grants: Set<BodyMetric>, val read: suspend (String?) -> BodyReadPage) :
        AuthorizedBodyPageSource {
        var calls = 0
        var grantReads = 0

        override suspend fun grantedMetrics(): Set<BodyMetric> {
            grantReads++
            return grants
        }

        override suspend fun page(
            window: BodyReadWindow,
            metric: BodyMetric,
            token: String?,
        ): BodyReadPage {
            calls++
            return read(token)
        }
    }

    @Test
    fun deniedMetricNeverReadsAnyProviderPage() = runTest {
        val source = Source(emptySet()) { error("Unauthorized query") }
        assertFailsWith<IllegalStateException> {
            AuthorizedBodyReader(source).read(window, setOf(metric))
        }
        assertEquals(0, source.calls)
    }

    @Test
    fun completePagesAreStagedAndPermissionsAreCheckedAgain() = runTest {
        val source =
            Source(setOf(metric)) { token ->
                if (token == null) BodyReadPage(listOf(record("a")), "next")
                else BodyReadPage(listOf(record("b")), null)
            }
        val snapshot = AuthorizedBodyReader(source).read(window, setOf(metric))
        assertEquals(listOf("a", "b"), snapshot.recordsForReplacement(metric).map { it.sourceId })
        assertEquals(2, source.grantReads)
    }

    @Test
    fun revokedReadNeverReturnsAnAuthoritativeReplacement() = runTest {
        lateinit var source: Source
        source =
            Source(setOf(metric)) {
                source.grants = emptySet()
                BodyReadPage(listOf(record("a")), null)
            }
        assertFailsWith<IllegalStateException> {
            AuthorizedBodyReader(source).read(window, setOf(metric))
        }
    }

    @Test
    fun failingLaterPageNeverReturnsAnEarlierPartialWindow() = runTest {
        val source =
            Source(setOf(metric)) { token ->
                if (token == null) BodyReadPage(listOf(record("a")), "next")
                else throw IllegalStateException("Provider failure")
            }
        assertFailsWith<IllegalStateException> {
            AuthorizedBodyReader(source).read(window, setOf(metric))
        }
        assertEquals(2, source.calls)
    }

    @Test
    fun repeatingPageTokenIsBoundedRatherThanSilentlyTruncated() = runTest {
        val source = Source(setOf(metric)) { BodyReadPage(listOf(record("a")), "repeated") }
        assertFailsWith<IllegalStateException> {
            AuthorizedBodyReader(source, maximumPagesPerMetric = 2).read(window, setOf(metric))
        }
        assertEquals(2, source.calls)
    }
}
