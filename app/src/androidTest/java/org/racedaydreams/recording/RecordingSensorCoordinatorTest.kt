package org.racedaydreams.recording

import android.test.AndroidTestCase

/** Source wiring only: these tests need no Bluetooth, motion permission or hardware. */
class RecordingSensorCoordinatorTest : AndroidTestCase() {
    private class Motion : MotionSource {
        var callback: ((Double?, Long) -> Unit)? = null
        var stops = 0
        var failStart = false

        override fun start(onCadence: (Double?, Long) -> Unit) {
            if (failStart) error("Motion unavailable")
            callback = onCadence
        }

        override fun stop() {
            stops++
        }
    }

    private class HeartRate : HeartRateSource {
        var values: ((Double?) -> Unit)? = null
        var connection: ((Boolean) -> Unit)? = null
        var address: String? = null
        var stops = 0
        var failConnect = false

        override fun connect(
            address: String,
            onValue: (Double?) -> Unit,
            onConnection: (Boolean) -> Unit,
        ) {
            this.address = address
            if (failConnect) error("Heart rate unavailable")
            values = onValue
            connection = onConnection
        }

        override fun stop() {
            stops++
        }
    }

    fun testDisabledOptionalSensorsNeverCreateOrStartSources() {
        val sensors =
            RecordingSensorCoordinator(
                { error("Disabled motion source created") },
                { error("Unselected heart-rate source created") },
            )
        sensors.start(
            false,
            null,
            { _, _ -> fail("Invented cadence") },
            { fail("Invented heart rate") },
            { fail("Invented connection") },
            { fail("Unexpected foreground type change") },
            { fail("Disabled sensor should not report failure") },
        )
        sensors.stop()
    }

    fun testValuesNullsAndMonotonicCadenceTimestampPassThroughUnchanged() {
        val motion = Motion()
        val heartRate = HeartRate()
        val calls = mutableListOf<String>()
        val cadence = mutableListOf<Pair<Double?, Long>>()
        val values = mutableListOf<Double?>()
        val connections = mutableListOf<Boolean>()
        val sensors =
            RecordingSensorCoordinator(
                {
                    calls.add("motion")
                    motion
                },
                {
                    calls.add("heart-rate")
                    heartRate
                },
            )
        sensors.start(
            true,
            "selected-device",
            { value, time -> cadence.add(value to time) },
            { values.add(it) },
            { connections.add(it) },
            { calls.add("foreground") },
            { fail(it) },
        )
        assertEquals(listOf("motion", "foreground", "heart-rate"), calls)
        assertEquals("selected-device", heartRate.address)
        motion.callback!!(172.0, 123456L)
        motion.callback!!(null, 123457L)
        heartRate.values!!(146.0)
        heartRate.connection!!(true)
        heartRate.values!!(null)
        heartRate.connection!!(false)
        assertEquals(listOf(172.0 to 123456L, null to 123457L), cadence)
        assertEquals(listOf(146.0, null), values)
        assertEquals(listOf(true, false), connections)
        sensors.stop()
        assertEquals(1, motion.stops)
        assertEquals(1, heartRate.stops)
    }

    fun testMotionFailureDoesNotPreventHeartRateConnection() {
        val motion = Motion().apply { failStart = true }
        val heartRate = HeartRate()
        val failures = mutableListOf<String>()
        val sensors = RecordingSensorCoordinator({ motion }, { heartRate })
        sensors.start(true, "selected-device", { _, _ -> }, {}, {}, {}, { failures.add(it) })
        assertEquals(listOf("Cadence unavailable; GPS recording continues."), failures)
        assertNotNull(heartRate.values)
        sensors.stop()
        assertEquals(1, motion.stops)
        assertEquals(1, heartRate.stops)
    }

    fun testForegroundFailureDoesNotCreateHeartRateSourceOrStopMotion() {
        val motion = Motion()
        val cadence = mutableListOf<Double?>()
        val failures = mutableListOf<String>()
        val sensors = RecordingSensorCoordinator({ motion }, { error("Must not create source") })
        sensors.start(
            true,
            "selected-device",
            { value, _ -> cadence.add(value) },
            {},
            {},
            { error("Foreground permission revoked") },
            { failures.add(it) },
        )
        assertEquals(listOf("Heart-rate monitor unavailable; GPS recording continues."), failures)
        motion.callback!!(171.0, 2000L)
        assertEquals(listOf(171.0), cadence)
        assertEquals(0, motion.stops)
        sensors.stop()
        assertEquals(1, motion.stops)
    }

    fun testConnectionFailureLeavesIndependentCadenceAndCleanupAvailable() {
        val motion = Motion()
        val heartRate = HeartRate().apply { failConnect = true }
        val failures = mutableListOf<String>()
        val cadence = mutableListOf<Double?>()
        val sensors = RecordingSensorCoordinator({ motion }, { heartRate })
        sensors.start(
            true,
            "selected-device",
            { value, _ -> cadence.add(value) },
            {},
            {},
            {},
            { failures.add(it) },
        )
        motion.callback!!(168.0, 3000L)
        assertEquals(listOf(168.0), cadence)
        assertEquals(listOf("Heart-rate monitor unavailable; GPS recording continues."), failures)
        sensors.stop()
        assertEquals(1, motion.stops)
        assertEquals(1, heartRate.stops)
    }
}
