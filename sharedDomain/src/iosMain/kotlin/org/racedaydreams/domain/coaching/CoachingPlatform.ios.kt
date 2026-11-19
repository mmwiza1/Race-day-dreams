package org.racedaydreams.domain.coaching

actual interface CoachingTransaction {
    actual fun run()
}

internal actual fun performanceDecimal(value: Double): String = coachingDecimalOne(value)
