package org.racedaydreams.domain

import java.util.Locale
import java.util.Random
import kotlin.test.*
import org.racedaydreams.domain.presentation.fixedDecimalText
import org.racedaydreams.domain.support.*

class DreamPlatformCompatibilityTest {
    @Test
    fun nativeRandomMatchesTheOriginalJvmSequenceIncludingRejectionBounds() {
        for (seed in
            listOf(
                Long.MIN_VALUE,
                -123456789L,
                -1L,
                0L,
                1L,
                42L,
                123456789L,
                Long.MAX_VALUE,
            )) for (bound in listOf(1, 2, 3, 5, 31, 32, 97, 1073741825, Int.MAX_VALUE)) {
            val expected = Random(seed)
            val actual = PortableDreamRandom(seed)
            repeat(100) {
                assertEquals(expected.nextInt(bound), actual.nextInt(bound), "$seed/$bound")
                assertEquals(
                    expected.nextDouble().toBits(),
                    actual.nextDouble().toBits(),
                    "$seed/$bound",
                )
            }
        }
    }

    @Test
    fun originalRandomSignatureAndImmutableGeometryCollectionsAreRetained() {
        val method =
            DreamRoutes::class.java.getMethod("assign", java.lang.Double.TYPE, Random::class.java)
        assertEquals(DreamRoutes.Assignment::class.java, method.returnType)
        @Suppress("UNCHECKED_CAST")
        val vertices = DreamRoutes.forId("london").vertices() as java.util.List<DreamRoutes.Vertex>
        assertFailsWith<UnsupportedOperationException> { vertices.clear() }
        assertFailsWith<UnsupportedOperationException> {
            vertices.iterator().let {
                it.next()
                it.remove()
            }
        }
    }

    @Test
    fun nativeZeroOneAndTwoDecimalTextMatchesExistingUkFormatters() {
        for (places in 0..2) {
            for (i in -4000..4000) {
                val value = i / 100.0
                assertEquals(
                    String.format(Locale.UK, "%.$places" + "f", value),
                    fixedDecimalText(value, places),
                    "$value/$places",
                )
            }
            for (value in
                listOf(
                    -0.0,
                    Double.NaN,
                    Double.POSITIVE_INFINITY,
                    Double.NEGATIVE_INFINITY,
                    Double.MAX_VALUE,
                    Double.MIN_VALUE,
                    0.005,
                    1.005,
                    42.195,
                    99.999,
                    1e100,
                    1e-100,
                )) assertEquals(
                String.format(Locale.UK, "%.$places" + "f", value),
                fixedDecimalText(value, places),
                "$value/$places",
            )
        }
    }
}
