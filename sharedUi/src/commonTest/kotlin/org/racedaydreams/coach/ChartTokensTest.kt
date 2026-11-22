package org.racedaydreams.coach

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.racedaydreams.domain.time.LocalDate

class ChartTokensTest {
    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance()
        val lb = b.luminance()
        return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
    }

    // The lightest and darkest surfaces charts sit on (pure white and a raised dark container).
    private val lightSurfaces = listOf(Color.White, Color(0xFFFAF8FF), Color(0xFFF3F3FA))
    private val darkSurfaces = listOf(Color(0xFF121318), Color(0xFF1E1F25), Color(0xFF292A2F))

    @Test
    fun everyChartToneKeepsThreeToOneContrastOnLightAndDarkSurfaces() {
        for (hue in ChartHue.entries) {
            for (surface in lightSurfaces) {
                assertTrue(contrast(hue.strong(false), surface) >= 3f, "$hue strong on $surface")
                assertTrue(contrast(hue.soft(false), surface) >= 3f, "$hue soft on $surface")
            }
            for (surface in darkSurfaces) {
                assertTrue(
                    contrast(hue.strong(true), surface) >= 3f,
                    "$hue strong dark on $surface",
                )
                assertTrue(contrast(hue.soft(true), surface) >= 3f, "$hue soft dark on $surface")
            }
        }
    }

    @Test
    fun selectedToneStandsApartFromTheSoftToneInBothThemes() {
        for (hue in ChartHue.entries) {
            // Light: the strong tone is darker; dark: lighter. Selection also has a marker and
            // outline, so this only checks the tones are clearly different, not colour alone.
            assertTrue(hue.strong(false).luminance() < hue.soft(false).luminance(), "$hue light")
            assertTrue(
                hue.strong(true).luminance() > hue.soft(true).luminance() * 1.8f,
                "$hue dark",
            )
        }
    }

    @Test
    fun eachRunningMeasureHasItsOwnHue() {
        assertEquals(
            RunningTrendMetric.entries.size,
            RunningTrendMetric.entries.map { it.hue }.toSet().size,
        )
    }

    private fun mondays(first: LocalDate, count: Int) =
        (0 until count).map { first.plusDays(7L * it) }

    private fun assertNoOverlap(marks: List<ChartTimeline.Mark>, total: Float, gap: Float) {
        marks.zipWithNext().forEach { (a, b) -> assertTrue(a.x + a.width + gap <= b.x + 0.01f) }
        marks.forEach { assertTrue(it.x >= 0f && it.x + it.width <= total + 0.01f, it.label) }
    }

    @Test
    fun oneYearKeepsTheJanuaryTransitionWithoutOverlapAtNormalAndLargeText() {
        val weeks = mondays(LocalDate.of(2025, 11, 24), 52)
        for (charWidth in listOf(6.5f, 13f)) { // about 100% and 200% text
            val marks = ChartTimeline.monthMarks(weeks, 300f, 6f) { it.length * charWidth }
            val labels = marks.map { it.label }
            assertTrue("Jan '26" in labels, "$charWidth: $labels")
            assertNoOverlap(marks, 300f, 6f)
        }
        // The start of the range is still labelled: with its year when there is room, otherwise
        // as the plain month next to "Jan '26".
        val normal = ChartTimeline.monthMarks(weeks, 300f, 6f) { it.length * 6.5f }
        assertEquals(0, normal.first().index)
        assertTrue(normal.first().label.startsWith("Nov"), normal.first().label)
        val wide = ChartTimeline.monthMarks(weeks, 600f, 6f) { it.length * 6.5f }
        assertEquals("Nov '25", wide.first().label)
    }

    @Test
    fun rangesWithoutJanuaryStillNameTheirYearAtTheStart() {
        val weeks = mondays(LocalDate.of(2026, 5, 25), 26)
        val marks = ChartTimeline.monthMarks(weeks, 300f, 6f) { it.length * 6.5f }
        assertEquals("May '26", marks.first().label)
        assertTrue(marks.none { it.label.startsWith("Jan") })
        assertNoOverlap(marks, 300f, 6f)
    }
}
