package org.racedaydreams.domain.portability;

import org.racedaydreams.domain.PlannedWorkout;

/** Future GPS/foreground-service adapter; not an implemented recorder or permission request. */
public interface ActivityRecorder {
    enum State { IDLE, RECORDING, PAUSED, FINISHED }
    State state();
    void start(PlannedWorkout optionalPrescription);
    void pause();
    void resume();
    RecordedRoute finish();
}
