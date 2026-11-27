package org.racedaydreams.domain.recording

/** Coordinate-free evidence, owned by the recording thread. All times are relative to run start. */
class RunTroubleshooting(private val startMillis: Long) {
    data class Event(
        val elapsedMillis: Long,
        val kind: String,
        val observationMillis: Long?,
        val count: Int?,
    )

    data class Snapshot(
        val checkpoints: List<Event>,
        val omittedEvents: Int,
        val callbacks: Int,
        val batchedCallbacks: Int,
        val deliveredFixes: Int,
        val callbackObservations: Int,
        val lateFixes: Int,
        val acceptedFixes: Int,
        val rejected: Map<String, Int>,
        val checkpointElapsedMillis: Long,
        val pointCount: Int,
        val acknowledgedPointCount: Int,
        val longestCallbackGapMillis: Long,
        val longestAcceptedGapMillis: Long,
        val maximumReceiptDelayMillis: Long,
        val flushFailures: Int,
    )

    private val first = mutableListOf<Event>()
    private val recent = ArrayDeque<Event>()
    private var omitted = 0
    private var callbacks = 0
    private var batches = 0
    private var delivered = 0
    private var callbackObservations = 0
    private var late = 0
    private var accepted = 0
    private var lastCallback = startMillis
    private var lastAccepted = startMillis
    private var callbackGap = 0L
    private var acceptedGap = 0L
    private var maxAge = 0L
    private var failures = 0
    private var gps: String? = null
    private val rejected = mutableMapOf<String, Int>()

    fun event(kind: String, received: Long, observation: Long? = null, count: Int? = null) {
        val row = Event(received - startMillis, kind, observation?.minus(startMillis), count)
        if (first.size < 32) first.add(row)
        else {
            if (recent.size == 224) {
                recent.removeFirst()
                omitted++
            }
            recent.addLast(row)
        }
    }

    fun callback(received: Long, count: Int, oldest: Long?) {
        callbacks++
        callbackObservations += count
        if (count > 1) batches++
        val interval = received - lastCallback
        callbackGap = maxOf(callbackGap, interval)
        lastCallback = received
        if (callbacks == 1 || count > 1 || interval >= 15_000 || callbacks % 30 == 0)
            event("location_callback", received, oldest, count)
    }

    fun fix(received: Long, observation: Long, acceptedForRoute: Boolean, reason: String?) {
        delivered++
        val age = received - observation
        maxAge = maxOf(maxAge, age)
        if (age > 10_000) late++
        if (acceptedForRoute) {
            accepted++
            val interval = observation - lastAccepted
            acceptedGap = maxOf(acceptedGap, interval)
            if (accepted == 1 || interval >= 15_000 || accepted % 30 == 0)
                event("accepted_fix", received, observation)
            lastAccepted = observation
        } else {
            val key = reason ?: "not_staged"
            val count = (rejected[key] ?: 0) + 1
            rejected[key] = count
            if (count == 1 || count % 30 == 0) event(key, received, observation, count)
        }
    }

    fun status(value: String?, received: Long) {
        val kind =
            when {
                value?.contains("lost", ignoreCase = true) == true -> "gps_lost"
                value?.contains("weak", ignoreCase = true) == true -> "gps_weak"
                value?.contains("recovered", ignoreCase = true) == true -> "gps_returned"
                value?.contains("ready", ignoreCase = true) == true -> "gps_ready"
                else -> "gps_waiting"
            }
        if (kind != gps) {
            gps = kind
            event(kind, received)
        }
    }

    fun flushFailed(received: Long) {
        failures++
        event("recording_flush_failed", received, count = failures)
    }

    fun snapshot(now: Long, points: Int, persisted: Int) =
        Snapshot(
            first + recent,
            omitted,
            callbacks,
            batches,
            delivered,
            callbackObservations,
            late,
            accepted,
            rejected.toMap(),
            now - startMillis,
            points,
            persisted,
            maxOf(callbackGap, now - lastCallback),
            maxOf(acceptedGap, now - lastAccepted),
            maxAge,
            failures,
        )
}
