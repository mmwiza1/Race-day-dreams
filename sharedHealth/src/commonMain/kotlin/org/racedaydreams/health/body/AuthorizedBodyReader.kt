package org.racedaydreams.health.body

/** Page boundary for providers which really expose read grants, such as Health Connect. */
interface AuthorizedBodyPageSource {
    suspend fun grantedMetrics(): Set<BodyMetric>

    suspend fun page(window: BodyReadWindow, metric: BodyMetric, token: String?): BodyReadPage
}

data class BodyReadPage(val records: List<BodyObservation>, val nextToken: String?)

/** One staged operation; no persistence occurs here and incomplete reads never return authority. */
class AuthorizedBodyReader(
    private val source: AuthorizedBodyPageSource,
    private val maximumPagesPerMetric: Int = 1000,
) : BodyMeasurementReader {
    init {
        require(maximumPagesPerMetric > 0)
    }

    override suspend fun read(window: BodyReadWindow, metrics: Set<BodyMetric>): BodyReadSnapshot {
        val requested = metrics.toSet()
        check(source.grantedMetrics().containsAll(requested)) {
            "Body permissions changed; retained measurements are unchanged."
        }
        val staged = mutableListOf<BodyObservation>()
        for (metric in requested) {
            var token: String? = null
            var pages = 0
            do {
                check(++pages <= maximumPagesPerMetric) {
                    "Body history exceeds this import limit; retained measurements are unchanged."
                }
                val page = source.page(window, metric, token)
                staged += page.records
                token = page.nextToken
            } while (token != null)
        }
        check(source.grantedMetrics().containsAll(requested)) {
            "Body permissions changed during import; retained measurements are unchanged."
        }
        return BodyReadSnapshot(window, requested, staged, ReadCompleteness.AUTHORIZED_WINDOW)
    }
}
