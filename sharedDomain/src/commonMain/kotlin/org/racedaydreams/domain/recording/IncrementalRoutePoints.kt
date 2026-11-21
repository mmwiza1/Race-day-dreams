package org.racedaydreams.domain.recording

/**
 * Keeps the decoded saved route of the recording on screen and reads only the points appended since
 * the last refresh, instead of re-reading and decoding the whole run every time.
 *
 * Saved recording points are append-only with gap-free sequences, so the points already held stay
 * valid until the session changes. [read] returns the points stored at `fromSequence` and later, in
 * order; it must throw if the stored sequences are not contiguous from `fromSequence`. Any failure
 * forgets the held points, so the next refresh starts again from a full read. Read-only: it never
 * writes or changes recording data.
 */
class IncrementalRoutePoints(
    private val read: (sessionId: String, fromSequence: Int) -> List<RunMetricsEngine.Point>
) {
    private var session: String? = null
    private var held: List<RunMetricsEngine.Point> = emptyList()
    private var answered = false

    /** Number of points decoded by [read] so far; for measurements and tests. */
    var decoded = 0L
        private set

    /**
     * The whole ordered route for [sessionId] when it differs from the previous result, or null
     * when nothing new was saved (the previous result is still complete). A null [sessionId] (no
     * active recording) clears the held route and returns an empty list once.
     */
    fun refresh(sessionId: String?): List<RunMetricsEngine.Point>? {
        if (sessionId == null) {
            val changed = !answered || session != null
            reset()
            answered = true
            return if (changed) emptyList() else null
        }
        val first = session != sessionId
        if (first) {
            reset()
            session = sessionId
        }
        val added =
            try {
                read(sessionId, held.size)
            } catch (failure: Exception) {
                // Never keep a possibly partial copy; the caller reports the read problem.
                reset()
                throw failure
            }
        decoded += added.size
        answered = true
        if (added.isEmpty() && !first) return null
        held = held + added
        return held
    }

    private fun reset() {
        session = null
        held = emptyList()
        answered = false
    }
}
