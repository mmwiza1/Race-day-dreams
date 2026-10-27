package org.racedaydreams.recording

/**
 * Owns optional sensor adapters for one service lifetime. Android permissions and
 * foreground-service types stay at the service boundary. Factories make this wiring testable
 * without Bluetooth or motion hardware.
 *
 * The service starts this on Main; the existing adapters deliver on Main too. Sensor failures never
 * change recording state or invent observations.
 */
internal class RecordingSensorCoordinator(
    private val motionFactory: () -> MotionSource,
    private val heartRateFactory: () -> HeartRateSource,
) {
    private var motion: MotionSource? = null
    private var heartRate: HeartRateSource? = null

    fun start(
        motionEnabled: Boolean,
        heartRateAddress: String?,
        onCadence: (Double?, Long) -> Unit,
        onHeartRate: (Double?) -> Unit,
        onConnection: (Boolean) -> Unit,
        beforeHeartRate: () -> Unit,
        onUnavailable: (String) -> Unit,
    ) {
        if (motionEnabled) {
            try {
                motion = motionFactory()
                motion?.start(onCadence)
            } catch (_: Exception) {
                onUnavailable("Cadence unavailable; GPS recording continues.")
            }
        }
        if (heartRateAddress != null) {
            try {
                beforeHeartRate()
                heartRate = heartRateFactory()
                heartRate?.connect(heartRateAddress, onHeartRate, onConnection)
            } catch (_: Exception) {
                onUnavailable("Heart-rate monitor unavailable; GPS recording continues.")
            }
        }
    }

    fun stop() {
        heartRate?.stop()
        motion?.stop()
    }
}
