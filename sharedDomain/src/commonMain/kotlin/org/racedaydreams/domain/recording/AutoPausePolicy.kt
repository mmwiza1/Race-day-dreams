package org.racedaydreams.domain.recording

/**
 * Original opt-in heuristic. Missing/poor fixes never request a transition.
 *
 * Pausing stays strict (≤ 8 m accuracy, 30 s still) because a false pause hides running. Resuming
 * accepts every fix the recorder itself accepts (≤ 35 m, audit A-B2): with an accuracy above 8 m
 * the runner must also have left the pause position by twice the fix uncertainty, so GPS jitter
 * alone cannot resume. [transitionMillis] reports when the change really happened, the start of the
 * still window or the first moving fix (audit A-B3), so the recorder can attribute time and replay
 * the distance covered while the resume was being confirmed.
 */
class AutoPausePolicy {
    enum class Decision {
        NONE,
        PAUSE,
        RESUME,
    }

    private var first: RunMetricsEngine.Fix? = null
    private var pausedAt: RunMetricsEngine.Fix? = null
    private var movingSince = -1L
    private var lastObservation = -1L
    private var paused = false
    private var transition = -1L

    fun reset() {
        first = null
        pausedAt = null
        movingSince = -1
        lastObservation = -1
        paused = false
        transition = -1
    }

    /** Monotonic time the last PAUSE or RESUME decision applies from; -1 before any decision. */
    fun transitionMillis(): Long = transition

    fun observe(f: RunMetricsEngine.Fix): Decision {
        if (
            lastObservation >= 0 &&
                (f.monotonicMillis() <= lastObservation ||
                    f.monotonicMillis() - lastObservation > 5000)
        ) {
            first = null
            movingSince = -1
            if (f.monotonicMillis() <= lastObservation) return Decision.NONE
        }
        lastObservation = f.monotonicMillis()
        return if (paused) observePaused(f) else observeMoving(f)
    }

    private fun observeMoving(f: RunMetricsEngine.Fix): Decision {
        val accuracy = f.accuracy()
        val speed = f.speed()
        if (accuracy == null || accuracy > STILL_ACCURACY || speed == null) {
            first = null
            return Decision.NONE
        }
        if (speed <= .25) {
            val initial = first
            if (initial == null) first = f
            else if (RunMetricsEngine.metres(initial, f) > 4) first = f
            val still = first!!
            if (f.monotonicMillis() - still.monotonicMillis() >= 30000) {
                paused = true
                pausedAt = f
                transition = still.monotonicMillis()
                first = null
                movingSince = -1
                return Decision.PAUSE
            }
        } else first = null
        return Decision.NONE
    }

    private fun observePaused(f: RunMetricsEngine.Fix): Decision {
        val accuracy = f.accuracy()
        // A fix the recorder would not accept is no evidence either way.
        if (accuracy == null || accuracy > RECORDING_ACCURACY) return Decision.NONE
        val speed = f.speed()
        val anchor = pausedAt
        // Twice the radius: reported accuracy is only a 68% bound, so jitter can exceed it.
        val uncertainty = maxOf(STILL_ACCURACY, 2 * maxOf(accuracy, anchor?.accuracy() ?: 0.0))
        val displaced = anchor != null && RunMetricsEngine.metres(anchor, f) > uncertainty
        val moving = if (speed != null) speed >= 1.2 else displaced
        if (!moving) {
            movingSince = -1
            return Decision.NONE
        }
        if (movingSince < 0) movingSince = f.monotonicMillis()
        val confirmed = accuracy <= STILL_ACCURACY || displaced
        if (f.monotonicMillis() - movingSince >= 5000 && confirmed) {
            paused = false
            pausedAt = null
            transition = movingSince
            movingSince = -1
            return Decision.RESUME
        }
        return Decision.NONE
    }

    companion object {
        /** Accuracy needed to declare the runner still. */
        const val STILL_ACCURACY = 8.0
        /** The recorder's own acceptance ceiling (RunMetricsEngine). */
        const val RECORDING_ACCURACY = 35.0
    }
}
