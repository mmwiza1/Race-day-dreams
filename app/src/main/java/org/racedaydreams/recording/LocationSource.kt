package org.racedaydreams.recording

import android.annotation.SuppressLint
import android.content.Context
import android.location.*
import android.os.*
import java.time.Instant
import org.racedaydreams.domain.recording.RunMetricsEngine

interface LocationSource {
    fun start(onBatch: (Int, Long?) -> Unit, onFix: (RunMetricsEngine.Fix) -> Unit)

    fun stop()
}

/** Android GNSS only: no Play Services, network provider or background location permission. */
class AndroidLocationSource(context: Context) : LocationSource, LocationListener {
    private val manager = context.getSystemService(LocationManager::class.java)
    private var batch: ((Int, Long?) -> Unit)? = null
    private var callback: ((RunMetricsEngine.Fix) -> Unit)? = null

    @SuppressLint("MissingPermission")
    override fun start(onBatch: (Int, Long?) -> Unit, onFix: (RunMetricsEngine.Fix) -> Unit) {
        batch = onBatch
        callback = onFix
        manager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L,
            0f,
            this,
            Looper.getMainLooper(),
        )
    }

    override fun stop() {
        try {
            manager.removeUpdates(this)
        } finally {
            batch = null
            callback = null
        }
    }

    override fun onLocationChanged(location: Location) {
        batch?.invoke(1, location.elapsedRealtimeNanos / 1_000_000)
        deliver(location)
    }

    private fun deliver(l: Location) {
        val fix =
            runCatching {
                    RunMetricsEngine.Fix(
                        Instant.ofEpochMilli(l.time),
                        l.elapsedRealtimeNanos / 1000000,
                        l.latitude,
                        l.longitude,
                        if (l.hasAccuracy()) l.accuracy.toDouble() else null,
                        if (l.hasAltitude()) l.altitude else null,
                        if (l.hasVerticalAccuracy()) l.verticalAccuracyMeters.toDouble() else null,
                        if (l.hasSpeed()) l.speed.toDouble() else null,
                    )
                }
                .getOrNull() ?: return
        callback?.invoke(fix)
    }

    // Android may deliver batches. Qualify each original observation in chronological order;
    // the service independently checks receipt age before accepting it or observing auto-pause.
    override fun onLocationChanged(locations: MutableList<Location>) {
        if (locations.isEmpty()) return
        batch?.invoke(locations.size, locations.minOf { it.elapsedRealtimeNanos } / 1_000_000)
        locations.sortedBy { it.elapsedRealtimeNanos }.forEach(::deliver)
    }

    @Deprecated("Platform callback")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}
}
