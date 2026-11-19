package org.racedaydreams.domain.coaching

actual typealias CoachingTransaction = java.lang.Runnable

internal actual fun performanceDecimal(value: Double): String =
    String.format(java.util.Locale.UK, "%.1f", value)
