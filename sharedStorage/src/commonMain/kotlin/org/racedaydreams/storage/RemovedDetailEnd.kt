package org.racedaydreams.storage

import org.racedaydreams.domain.time.Instant

/**
 * Original Android inventory stores floor-to-milliseconds +1ms, not the actual end timestamp.
 * Exact-end receipts also remain supported. Accept ONLY these two representations, never a
 * tolerance, and do not change either the original receipt or measured activity.
 */
internal fun removedDetailMatchesActivityEnd(receipt: Instant, actual: Instant): Boolean {
    if (receipt == actual) return true
    val nextNanos = (actual.getNano() / 1000000 + 1) * 1000000
    return if (nextNanos == 1000000000)
        receipt.getEpochSecond() == actual.getEpochSecond() + 1 && receipt.getNano() == 0
    else receipt.getEpochSecond() == actual.getEpochSecond() && receipt.getNano() == nextNanos
}
