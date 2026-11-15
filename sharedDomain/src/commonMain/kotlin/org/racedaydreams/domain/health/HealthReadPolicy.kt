package org.racedaydreams.domain.health

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.time.*

/** Original permission/coverage policy; no platform permission-name assumptions. */
object HealthReadPolicy {
    enum class Access {
        UNAVAILABLE,
        DENIED,
        PARTIAL,
        READY,
    }

    @JvmStatic
    fun access(available: Boolean, exerciseAllowed: Boolean, distanceAllowed: Boolean): Access =
        if (!available) Access.UNAVAILABLE
        else if (!exerciseAllowed) Access.DENIED
        else if (distanceAllowed) Access.READY else Access.PARTIAL

    @JvmStatic
    fun windowStart(now: Instant, extendedHistoryGranted: Boolean): Instant =
        Instants.ofEpochSecond(
            now.getEpochSecond() - (if (extendedHistoryGranted) 84 else 29) * 86400L,
            now.getNano().toLong(),
        )
}
