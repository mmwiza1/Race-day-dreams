package org.racedaydreams.domain.recording

/** Original opt-in heuristic. Missing/poor fixes never request a transition. */
class AutoPausePolicy {
    enum class Decision {
        NONE,
        PAUSE,
        RESUME,
    }

    private var first: RunMetricsEngine.Fix? = null
    private var movingSince = -1L
    private var lastObservation = -1L
    private var paused = false

    fun reset() {
        first = null
        movingSince = -1
        lastObservation = -1
        paused = false
    }

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
        val accuracy = f.accuracy()
        val speed = f.speed()
        if (accuracy == null || accuracy > 8 || speed == null) {
            first = null
            movingSince = -1
            return Decision.NONE
        }
        if (!paused) {
            if (speed <= .25) {
                val initial = first
                if (initial == null) first = f
                else if (RunMetricsEngine.metres(initial, f) > 4) first = f
                if (f.monotonicMillis() - first!!.monotonicMillis() >= 30000) {
                    paused = true
                    first = null
                    return Decision.PAUSE
                }
            } else first = null
        } else if (speed >= 1.2) {
            if (movingSince < 0) movingSince = f.monotonicMillis()
            if (f.monotonicMillis() - movingSince >= 5000) {
                paused = false
                movingSince = -1
                return Decision.RESUME
            }
        } else movingSince = -1
        return Decision.NONE
    }
}
