package org.racedaydreams.coach

import kotlin.math.round
import kotlin.test.Test
import kotlin.test.assertEquals

class SetCountTest {
    private val decimal: (Double) -> String = { (round(it * 10) / 10).toString() }

    @Test
    fun wholeCountsHaveNoDecimals() {
        assertEquals("279 sets", setCount(279.0, decimal))
        assertEquals("0 sets", setCount(0.0, decimal))
        assertEquals("1 set", setCount(1.0, decimal))
    }

    @Test
    fun fractionalCountsKeepTheirPrecision() {
        assertEquals("12.5 sets", setCount(12.5, decimal))
        assertEquals("1.5 sets", setCount(1.5, decimal))
    }
}
