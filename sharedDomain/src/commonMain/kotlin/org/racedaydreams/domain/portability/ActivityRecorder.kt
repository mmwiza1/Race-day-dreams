package org.racedaydreams.domain.portability

import org.racedaydreams.domain.PlannedWorkout

/** Original adapter contract, not another recording engine or a permission request. */
interface ActivityRecorder {
    enum class State {
        IDLE,
        RECORDING,
        PAUSED,
        FINISHED,
    }

    fun state(): State

    fun start(optionalPrescription: PlannedWorkout?)

    fun pause()

    fun resume()

    fun finish(): RecordedRoute
}
