package org.racedaydreams.domain.time

import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone

internal actual fun systemOffsetAt(instant: Instant): ZoneOffset =
    Offsets.ofTotalSeconds(
        NSTimeZone.localTimeZone
            .secondsFromGMTForDate(
                NSDate.dateWithTimeIntervalSince1970(instant.getEpochSecond().toDouble())
            )
            .toInt()
    )
