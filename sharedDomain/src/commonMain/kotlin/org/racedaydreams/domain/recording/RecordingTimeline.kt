package org.racedaydreams.domain.recording

import kotlin.jvm.JvmStatic
import kotlin.math.max

/** Active recorded time includes GPS outages, excluding explicit manual/automatic pauses. */
object RecordingTimeline {
    @JvmStatic
    fun pausedAt(start: Long, end: Long, input: List<RunMetricsEngine.Event>): Boolean {
        require(start >= 0 && end >= start) { "Invalid recording bounds" }
        var paused = false
        for (e in input.sortedBy { it.monotonicMillis() }) {
            if (e.monotonicMillis() < start || e.monotonicMillis() > end) continue
            val type = e.type() ?: throw NullPointerException()
            if (type == "PAUSE" || type == "AUTO_PAUSE") paused = true
            else if (type == "RESUME" || type == "AUTO_RESUME") paused = false
        }
        return paused
    }

    @JvmStatic
    fun activeMillis(start: Long, end: Long, input: List<RunMetricsEngine.Event>): Long {
        require(start >= 0 && end >= start) { "Invalid recording bounds" }
        var paused = -1L
        var total = 0L
        for (e in input.sortedBy { it.monotonicMillis() }) {
            val time = e.monotonicMillis()
            if (time < start || time > end) continue
            val type = e.type() ?: throw NullPointerException()
            if ((type == "PAUSE" || type == "AUTO_PAUSE") && paused < 0) paused = time
            else if ((type == "RESUME" || type == "AUTO_RESUME") && paused >= 0) {
                total += time - paused
                paused = -1
            }
        }
        if (paused >= 0) total += end - paused
        return max(0L, end - start - total)
    }
}
