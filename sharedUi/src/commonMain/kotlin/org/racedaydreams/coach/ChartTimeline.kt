package org.racedaydreams.coach

import org.racedaydreams.domain.time.LocalDate

/**
 * Month labels under a long weekly chart. Year transitions come first: every January is labelled
 * with its year ("Jan '26"), then the first label (with its year), then other months in order. A
 * label is kept only if it fits without overlapping one already placed, so the less important
 * months give way at narrow widths or large text. Pure layout: no dates or values change.
 */
object ChartTimeline {
    class Mark(val index: Int, val label: String, val x: Float, val width: Float)

    fun monthMarks(
        starts: List<LocalDate>,
        totalWidth: Float,
        gap: Float,
        measure: (String) -> Float,
    ): List<Mark> {
        if (starts.isEmpty()) return emptyList()
        val slot = totalWidth / starts.size
        val candidates =
            starts.indices.filter { i ->
                i == 0 || starts[i].getMonthValue() != starts[i - 1].getMonthValue()
            }
        val priority =
            candidates.filter { starts[it].getMonthValue() == 1 } +
                candidates.filter { it == 0 && starts[it].getMonthValue() != 1 } +
                candidates.filter { it != 0 && starts[it].getMonthValue() != 1 }
        val placed = mutableListOf<Mark>()
        for (i in priority) {
            val date = starts[i]
            val month = WeekAtAGlance.monthShort(date)
            val year = " '" + (date.getYear() % 100).toString().padStart(2, '0')
            // January always tries its year. The first label tries its year, then falls back to
            // the month alone if a January label already takes the space (the range line above
            // the chart names the exact start date).
            val options =
                when {
                    date.getMonthValue() == 1 -> listOf(month + year)
                    i == 0 -> listOf(month + year, month)
                    else -> listOf(month)
                }
            for (label in options) {
                val width = measure(label)
                if (width > totalWidth) continue
                val x = (slot * i).coerceIn(0f, totalWidth - width)
                val collides = placed.any { x < it.x + it.width + gap && it.x < x + width + gap }
                if (!collides) {
                    placed += Mark(i, label, x, width)
                    break
                }
            }
        }
        return placed.sortedBy { it.x }
    }
}
