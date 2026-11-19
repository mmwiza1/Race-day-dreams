package org.racedaydreams.domain.presentation

import java.math.BigDecimal

internal actual fun prescriptionNumber(value: Double): String =
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

internal actual fun prescriptionDifference(next: Double, current: Double): String =
    BigDecimal.valueOf(next)
        .subtract(BigDecimal.valueOf(current))
        .abs()
        .stripTrailingZeros()
        .toPlainString()
