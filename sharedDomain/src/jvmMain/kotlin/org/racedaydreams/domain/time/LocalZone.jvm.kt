package org.racedaydreams.domain.time

internal actual fun systemOffsetAt(instant: Instant): ZoneOffset =
    java.time.ZoneId.systemDefault().rules.getOffset(instant)
