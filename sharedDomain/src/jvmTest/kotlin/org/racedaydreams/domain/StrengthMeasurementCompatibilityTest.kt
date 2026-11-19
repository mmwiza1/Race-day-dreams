package org.racedaydreams.domain

import java.util.regex.Pattern
import kotlin.test.Test
import kotlin.test.assertEquals
import org.racedaydreams.domain.strength.StrengthMeasurementPolicy

class StrengthMeasurementCompatibilityTest {
    private fun original(input: String): Long {
        val pieces = Pattern.compile(":").split(input.trim { it <= ' ' }, -1)
        require(pieces.size in 1..2) { "Enter seconds or minutes:seconds." }
        val value =
            try {
                if (pieces.size == 1) java.lang.Long.parseLong(pieces[0])
                else {
                    val m = java.lang.Long.parseLong(pieces[0])
                    val s = java.lang.Long.parseLong(pieces[1])
                    if (m < 0 || s < 0 || s >= 60) throw NumberFormatException()
                    Math.addExact(Math.multiplyExact(m, 60), s)
                }
            } catch (_: ArithmeticException) {
                throw IllegalArgumentException("Enter seconds or minutes:seconds.")
            } catch (_: NumberFormatException) {
                throw IllegalArgumentException("Enter seconds or minutes:seconds.")
            }
        require(value > 0 && value <= 43200) { "Duration must be positive and at most 12 hours." }
        return value
    }

    @Test
    fun originalParsingRetainsSignsUnicodeDigitsOverflowAndErrorMessages() {
        val texts =
            listOf(
                "",
                " 90 ",
                "\u00a090",
                "+90",
                "-0",
                "-1",
                "-0:+01",
                "٠١:٣٠",
                "９０",
                "90.0",
                "1:60",
                "1:-1",
                "1:",
                ":01",
                "1:2:3",
                "9223372036854775807",
                "9223372036854775808",
                "153722867280912930:08",
                "153722867280912930:07",
            ) + (-10..1000).flatMap { listOf(it.toString(), "$it:59", "$it:00", "$it:60") }
        fun outcome(block: () -> Long): String =
            try {
                "value:${block()}"
            } catch (e: IllegalArgumentException) {
                "error:${e.message}"
            }
        for (text in texts) assertEquals(
            outcome { original(text) },
            outcome { StrengthMeasurementPolicy.seconds(text) },
            text,
        )
    }
}
