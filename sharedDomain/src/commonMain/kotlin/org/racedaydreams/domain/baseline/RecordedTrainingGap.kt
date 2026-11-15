package org.racedaydreams.domain.baseline

import org.racedaydreams.domain.time.*

/** Original retained evidence snapshot. Missing values remain unknown. */
data class RecordedTrainingGap(
    private val after: Instant?,
    private val before: Instant?,
    private val daysWithoutRecordedRun: Long,
) {
    init {
        if (after == null) throw NullPointerException("after")
        if (before == null) throw NullPointerException("before")
        require(before.isAfter(after) && daysWithoutRecordedRun >= 1) { "Invalid recorded gap." }
    }

    fun after(): Instant = after!!

    fun before(): Instant = before!!

    fun daysWithoutRecordedRun(): Long = daysWithoutRecordedRun

    override fun toString(): String =
        "RecordedTrainingGap[after=$after, before=$before, daysWithoutRecordedRun=$daysWithoutRecordedRun]"
}
