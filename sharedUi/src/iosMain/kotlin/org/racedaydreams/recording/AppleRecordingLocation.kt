@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.recording

import kotlin.math.floor
import kotlin.math.roundToLong
import kotlinx.cinterop.convert
import kotlinx.cinterop.useContents
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.Instants
import platform.CoreLocation.*
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSThread
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject
import platform.posix.CLOCK_MONOTONIC_RAW
import platform.posix.clock_gettime_nsec_np

/** Device clocks for live recording. Recovery uses the stored times from the former process. */
object AppleRecordingClock {
    /**
     * Continuous monotonic time that keeps advancing while the device sleeps (Darwin's
     * CLOCK_MONOTONIC_RAW, as mach_continuous_time), like Android's elapsedRealtime. The former
     * systemUptime base stops during sleep, which could under-report a locked-screen run (audit
     * A-R1). Values are only compared within one process; recovery uses stored times.
     */
    fun monotonicMillis(): Long =
        (clock_gettime_nsec_np(CLOCK_MONOTONIC_RAW.convert()) / 1_000_000u).toLong()

    fun now(): Instant = instant(NSDate().timeIntervalSince1970)

    internal fun instant(seconds: Double): Instant {
        val whole = floor(seconds)
        return Instants.ofEpochSecond(
            whole.toLong(),
            ((seconds - whole) * 1_000_000_000).roundToLong(),
        )
    }
}

/** Core Location source confined to the same main thread as the recording engine. */
class AppleRecordingLocation(
    private val onFix: (RunMetricsEngine.Fix, Long, Instant) -> Unit,
    private val onUnavailable: (Long, Instant) -> Unit,
    private val onAuthorizationChanged: (Boolean) -> Unit,
) {
    init {
        check(NSThread.isMainThread)
    }

    private var listening = false
    private val delegate =
        object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                if (!authorized(manager)) stop()
                onAuthorizationChanged(authorized(manager))
            }

            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                if (!listening) return
                val receivedMillis = AppleRecordingClock.monotonicMillis()
                val receivedSeconds = NSDate().timeIntervalSince1970
                val receivedWall = AppleRecordingClock.instant(receivedSeconds)
                for (location in didUpdateLocations.filterIsInstance<CLLocation>()) {
                    if (!listening) break
                    val timestamp = location.timestamp.timeIntervalSince1970
                    if (!timestamp.isFinite()) continue
                    val ageMillis = ((receivedSeconds - timestamp) * 1_000).roundToLong()
                    val latitude = location.coordinate.useContents { latitude }
                    val longitude = location.coordinate.useContents { longitude }
                    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) continue
                    val vertical = location.verticalAccuracy.nonnegative()
                    val fix =
                        RunMetricsEngine.Fix(
                            AppleRecordingClock.instant(timestamp),
                            (receivedMillis - ageMillis).coerceAtLeast(0),
                            latitude,
                            longitude,
                            location.horizontalAccuracy.nonnegative(),
                            location.altitude.takeIf { vertical != null && it.isFinite() },
                            vertical,
                            location.speed.nonnegative(),
                        )
                    onFix(fix, receivedMillis, receivedWall)
                }
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                if (listening)
                    onUnavailable(AppleRecordingClock.monotonicMillis(), AppleRecordingClock.now())
            }
        }

    private val manager =
        CLLocationManager().apply {
            desiredAccuracy = kCLLocationAccuracyBest
            activityType = CLActivityTypeFitness
            pausesLocationUpdatesAutomatically = false
            delegate = this@AppleRecordingLocation.delegate
        }

    fun requestPermission() {
        check(NSThread.isMainThread)
        manager.requestWhenInUseAuthorization()
    }

    fun canStart(): Boolean {
        check(NSThread.isMainThread)
        return authorized(manager)
    }

    fun start() {
        check(canStart()) { "Location permission is required to record a run." }
        if (listening) return
        listening = true
        manager.allowsBackgroundLocationUpdates = true
        manager.showsBackgroundLocationIndicator = true
        manager.startUpdatingLocation()
    }

    fun stop() {
        check(NSThread.isMainThread)
        if (!listening) return
        listening = false
        manager.stopUpdatingLocation()
        manager.allowsBackgroundLocationUpdates = false
    }

    private fun authorized(manager: CLLocationManager): Boolean =
        manager.authorizationStatus == kCLAuthorizationStatusAuthorizedWhenInUse ||
            manager.authorizationStatus == kCLAuthorizationStatusAuthorizedAlways

    private fun Double.nonnegative(): Double? = takeIf { it.isFinite() && it >= 0 }
}
