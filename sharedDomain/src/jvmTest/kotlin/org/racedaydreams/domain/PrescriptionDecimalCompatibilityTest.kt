package org.racedaydreams.domain

import java.math.BigDecimal
import kotlin.random.Random
import kotlin.test.*
import org.racedaydreams.domain.presentation.differencePrescriptionDecimals
import org.racedaydreams.domain.presentation.expandPrescriptionDecimal

class PrescriptionDecimalCompatibilityTest {
    @Test
    fun nativeDecimalExpansionAndDifferenceMatchOriginalDecimalArithmetic() {
        val random = Random(240123)
        for (value in
            listOf(
                Double.MIN_VALUE,
                Double.MAX_VALUE,
                -Double.MIN_VALUE,
                -Double.MAX_VALUE,
                1e23,
                -0.0,
            ) + List(1000) { random.nextDouble(-1500.0, 1500.0) }) {
            assertEquals(
                BigDecimal.valueOf(value).stripTrailingZeros().toPlainString(),
                expandPrescriptionDecimal(value.toString()),
            )
        }
        repeat(1000) {
            val a = random.nextDouble(0.0, 1500.0)
            val b = random.nextDouble(0.0, 1500.0)
            assertEquals(
                BigDecimal.valueOf(a)
                    .subtract(BigDecimal.valueOf(b))
                    .abs()
                    .stripTrailingZeros()
                    .toPlainString(),
                differencePrescriptionDecimals(
                    expandPrescriptionDecimal(a.toString()),
                    expandPrescriptionDecimal(b.toString()),
                ),
            )
        }
    }
}
