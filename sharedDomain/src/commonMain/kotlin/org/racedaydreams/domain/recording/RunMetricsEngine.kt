package org.racedaydreams.domain.recording

import kotlin.jvm.JvmStatic
import kotlin.math.*
import org.racedaydreams.domain.activity.DistanceSample
import org.racedaydreams.domain.time.*

/** Original deterministic trace processor. Never owns sensors or invents missing coordinates. */
class RunMetricsEngine(monotonicMillis: Long, wall: Instant?) {
    data class Fix(
        private val timestamp: Instant?,
        private val monotonicMillis: Long,
        private val latitude: Double,
        private val longitude: Double,
        private val accuracy: Double?,
        private val altitude: Double?,
        private val verticalAccuracy: Double?,
        private val speed: Double?,
    ) {
        init {
            if (timestamp == null) throw NullPointerException()
            require(
                monotonicMillis >= 0 &&
                    latitude.isFinite() &&
                    longitude.isFinite() &&
                    abs(latitude) <= 90 &&
                    abs(longitude) <= 180 &&
                    (accuracy == null || accuracy.isFinite() && accuracy >= 0)
            ) {
                "Invalid location"
            }
            require(
                (altitude == null || altitude.isFinite()) &&
                    (verticalAccuracy == null ||
                        verticalAccuracy.isFinite() && verticalAccuracy >= 0) &&
                    (speed == null || speed.isFinite() && speed >= 0)
            ) {
                "Invalid optional location evidence"
            }
        }

        fun timestamp(): Instant = timestamp!!

        fun monotonicMillis(): Long = monotonicMillis

        fun latitude(): Double = latitude

        fun longitude(): Double = longitude

        fun accuracy(): Double? = accuracy

        fun altitude(): Double? = altitude

        fun verticalAccuracy(): Double? = verticalAccuracy

        fun speed(): Double? = speed

        override fun toString(): String =
            "Fix[timestamp=$timestamp, monotonicMillis=$monotonicMillis, latitude=$latitude, longitude=$longitude, accuracy=$accuracy, altitude=$altitude, verticalAccuracy=$verticalAccuracy, speed=$speed]"
    }

    data class Point(
        private val fix: Fix?,
        private val cumulativeMetres: Double,
        private val activeMillis: Long,
        private val segment: Int,
        private val discontinuity: Boolean,
        private val heartRate: Double?,
        private val cadence: Double?,
    ) {
        init {
            if (fix == null) throw NullPointerException()
            require(
                cumulativeMetres.isFinite() &&
                    cumulativeMetres >= 0 &&
                    activeMillis >= 0 &&
                    segment >= 0 &&
                    (heartRate == null || heartRate.isFinite() && heartRate > 0) &&
                    (cadence == null || cadence.isFinite() && cadence >= 0)
            ) {
                "Invalid recorded point"
            }
        }

        fun fix(): Fix = fix!!

        fun cumulativeMetres(): Double = cumulativeMetres

        fun activeMillis(): Long = activeMillis

        fun segment(): Int = segment

        fun discontinuity(): Boolean = discontinuity

        fun heartRate(): Double? = heartRate

        fun cadence(): Double? = cadence

        override fun toString(): String =
            "Point[fix=$fix, cumulativeMetres=$cumulativeMetres, activeMillis=$activeMillis, segment=$segment, discontinuity=$discontinuity, heartRate=$heartRate, cadence=$cadence]"
    }

    data class Event(
        private val type: String?,
        private val monotonicMillis: Long,
        private val timestamp: Instant?,
        private val value: Double?,
    ) {
        constructor(
            type: String?,
            monotonicMillis: Long,
            timestamp: Instant?,
        ) : this(type, monotonicMillis, timestamp, null)

        fun type(): String? = type

        fun monotonicMillis(): Long = monotonicMillis

        fun timestamp(): Instant? = timestamp

        fun value(): Double? = value

        override fun toString(): String =
            "Event[type=$type, monotonicMillis=$monotonicMillis, timestamp=$timestamp, value=$value]"
    }

    data class Status(
        private val metres: Double,
        private val elapsedMillis: Long,
        private val movingMillis: Long,
        private val secondsPerKm: Double?,
        private val gps: String?,
        private val paused: Boolean,
    ) {
        fun metres(): Double = metres

        fun elapsedMillis(): Long = elapsedMillis

        fun movingMillis(): Long = movingMillis

        fun secondsPerKm(): Double? = secondsPerKm

        fun gps(): String? = gps

        fun paused(): Boolean = paused

        /** A recent pace alone must not trigger cues while the current fix is degraded/rejected. */
        fun hasReliablePace(): Boolean =
            !paused &&
                gps in listOf("GPS available", "GPS ready", "GPS recovered") &&
                secondsPerKm != null &&
                secondsPerKm.isFinite() &&
                secondsPerKm > 0

        override fun toString(): String =
            "Status[metres=$metres, elapsedMillis=$elapsedMillis, movingMillis=$movingMillis, secondsPerKm=$secondsPerKm, gps=$gps, paused=$paused]"
    }

    private val started = monotonicMillis
    private var minimumFixTime = 0L
    private var lastTime = monotonicMillis
    private var pausedSince = -1L
    private var lastResume = -1L
    private var pausedTotal = 0L
    private var segment = 0
    private var lastDelivery = -1L
    private var recoveredUntil = -1L
    private var lastUsableFix = -1L
    private var lastRejection: String? = null
    private var distance = 0.0
    private var anchor: Fix? = null
    private var distanceAnchor: Fix? = null
    private var gap = true
    private var gps = "Waiting for GPS"
    private var hr: Double? = null
    private var cadence: Double? = null
    private var hrTime = -1L
    private var cadenceTime = -1L
    private val pointRows = mutableListOf<Point>()
    private val eventRows = mutableListOf<Event>()
    /** Usable fixes observed while paused; an automatic resume replays those after it began. */
    private val pausedFixes = ArrayDeque<Fix>()
    /** Still time before automatic pauses, counted as moving live but not when saved (A-B3). */
    private var stillCredit = 0L

    init {
        eventRows.add(Event("START", monotonicMillis, wall))
    }

    fun heartRate(value: Double?, time: Long) {
        require(value == null || value.isFinite() && value >= 25 && value <= 250) { "Invalid HR" }
        hr = value
        hrTime = time
    }

    fun cadence(value: Double?, time: Long) {
        require(value == null || value.isFinite() && value >= 0 && value <= 300) {
            "Invalid cadence"
        }
        cadence = value
        cadenceTime = time
    }

    fun pause(time: Long, wall: Instant?) = pause(time, wall, false)

    fun pause(time: Long, wall: Instant?, auto: Boolean) = pause(time, wall, auto, time)

    /**
     * [effectiveFrom] is when the runner actually stopped (the start of an automatic pause's still
     * window). Live moving time must never move backwards (structured workouts require it), so the
     * live clock pauses at [time]; the event is dated [effectiveFrom] and the difference is removed
     * from the saved moving time, exactly as recovery computes it from the events (audit A-B3).
     */
    fun pause(time: Long, wall: Instant?, auto: Boolean, effectiveFrom: Long) {
        tick(time)
        if (pausedSince < 0) {
            val from = minOf(time, maxOf(effectiveFrom, started, lastResume))
            minimumFixTime = time
            pausedSince = time
            stillCredit += time - from
            anchor = null
            distanceAnchor = null
            gap = true
            pausedFixes.clear()
            eventRows.add(Event(if (auto) "AUTO_PAUSE" else "PAUSE", from, wall))
        }
    }

    fun resume(time: Long, wall: Instant?) = resume(time, wall, false)

    fun resume(time: Long, wall: Instant?, auto: Boolean) = resume(time, wall, auto, time)

    /**
     * [effectiveFrom] is when the runner actually started moving again (the first moving fix of an
     * automatic resume). Time from then counts as moving and the usable fixes received since then
     * are replayed, so the distance covered while the resume was being confirmed, including its
     * first fix, is kept (audit A-B2, A-B3). A manual resume passes [time] and replays nothing.
     */
    fun resume(time: Long, wall: Instant?, auto: Boolean, effectiveFrom: Long) {
        tick(time)
        if (pausedSince >= 0) {
            val from = minOf(time, maxOf(effectiveFrom, pausedSince))
            minimumFixTime = from
            pausedTotal += from - pausedSince
            pausedSince = -1
            lastResume = from
            segment++
            anchor = null
            distanceAnchor = null
            gap = true
            eventRows.add(Event(if (auto) "AUTO_RESUME" else "RESUME", from, wall))
            val replay = pausedFixes.filter { it.monotonicMillis() >= from }
            pausedFixes.clear()
            for (f in replay) accept(f)
        }
    }

    /** Moving time to save: live moving time less the still time before automatic pauses. */
    fun recordedMovingMillis(time: Long): Long = max(0L, status(time).movingMillis() - stillCredit)

    fun sensorEvent(type: String?, value: Double, time: Long, wall: Instant?) {
        tick(time)
        eventRows.add(Event(type, time, wall, value))
    }

    fun event(type: String?, time: Long, wall: Instant?) {
        tick(time)
        eventRows.add(Event(type, time, wall))
    }

    private fun tick(time: Long) {
        require(time >= lastTime) { "Monotonic time moved backwards" }
        lastTime = time
    }

    /**
     * Live receipt qualification; historical GPX imports still deliberately call accept directly.
     */
    fun qualifyDelivery(f: Fix, receivedMillis: Long, receivedWall: Instant?): Boolean {
        require(receivedMillis >= lastTime) { "Receipt clock moved backwards" }
        lastTime = receivedMillis
        if (f.monotonicMillis() > receivedMillis)
            return reject("GPS_FIX_FUTURE", receivedMillis, receivedWall, true)
        if (
            f.monotonicMillis() < max(started, minimumFixTime) ||
                receivedMillis - f.monotonicMillis() > 10000
        )
            return reject("GPS_FIX_STALE", receivedMillis, receivedWall, true)
        if (f.monotonicMillis() <= lastDelivery)
            return reject("GPS_FIX_ORDER", receivedMillis, receivedWall, false)
        lastDelivery = f.monotonicMillis()
        if (f.accuracy() == null || f.accuracy()!! > 35) {
            lost(
                Fix(
                    receivedWall,
                    receivedMillis,
                    f.latitude(),
                    f.longitude(),
                    f.accuracy(),
                    null,
                    null,
                    null,
                )
            )
            gps = "GPS signal weak"
            return reject("GPS_FIX_ACCURACY", receivedMillis, receivedWall, false)
        }
        if (implausible(f)) return reject("GPS_FIX_JUMP", receivedMillis, receivedWall, true)
        lastUsableFix = f.monotonicMillis()
        return true
    }

    /** Read immediately after a failed delivery qualification. */
    fun deliveryRejection(): String? = lastRejection

    private fun reject(reason: String, time: Long, wall: Instant?, weak: Boolean): Boolean {
        if (reason != lastRejection) eventRows.add(Event(reason, time, wall))
        lastRejection = reason
        if (weak && !gap) gps = "GPS signal weak"
        return false
    }

    private fun implausible(f: Fix): Boolean {
        if (f.speed() != null && f.speed()!! > 12) return true
        val previous = anchor ?: return false
        val dt = f.monotonicMillis() - previous.monotonicMillis()
        return dt > 0 && dt <= 15000 && metres(previous, f) / (dt / 1000.0) > 12
    }

    fun accept(f: Fix): Boolean {
        if (
            f.monotonicMillis() < max(started, minimumFixTime) ||
                pointRows.isNotEmpty() &&
                    f.monotonicMillis() <= pointRows.last().fix().monotonicMillis()
        )
            return false
        lastTime = max(lastTime, f.monotonicMillis())
        if (f.accuracy() != null && f.accuracy()!! > 35) {
            lost(f)
            gps = "GPS signal weak"
            return false
        }
        if (implausible(f)) {
            gps = "GPS signal weak"
            return false
        }
        lastUsableFix = max(lastUsableFix, f.monotonicMillis())
        if (pausedSince >= 0) {
            gps = "GPS ready · paused"
            pausedFixes.addLast(f)
            if (pausedFixes.size > PAUSED_FIX_LIMIT) pausedFixes.removeFirst()
            return false
        }
        val previous = anchor
        if (previous != null) {
            val dt = f.monotonicMillis() - previous.monotonicMillis()
            if (dt > 15000) lost(f)
            else if (!stationaryCluster(f)) {
                distance += metres(distanceAnchor!!, f)
                distanceAnchor = f
            }
        }
        if (gap) {
            if (pointRows.isNotEmpty()) recoveredUntil = f.monotonicMillis() + 5000
            segment++
            eventRows.add(Event("GPS_RETURNED", f.monotonicMillis(), f.timestamp()))
            distanceAnchor = f
        }
        gps = "GPS ready"
        lastRejection = null
        pointRows.add(
            Point(
                f,
                distance,
                active(f.monotonicMillis()),
                segment,
                gap,
                if (
                    hr != null &&
                        f.monotonicMillis() >= hrTime &&
                        f.monotonicMillis() - hrTime <= 10000
                )
                    hr
                else null,
                if (
                    cadence != null &&
                        f.monotonicMillis() >= cadenceTime &&
                        f.monotonicMillis() - cadenceTime <= 10000
                )
                    cadence
                else null,
            )
        )
        gap = false
        anchor = f
        return true
    }

    /** Retain original points; unknown speed/accuracy never supplies stationary evidence. */
    private fun stationaryCluster(f: Fix): Boolean {
        val distanceStart = distanceAnchor ?: return false
        val previous = anchor ?: return false
        val speed = f.speed() ?: return false
        val previousSpeed = previous.speed() ?: return false
        val accuracy = f.accuracy() ?: return false
        val previousAccuracy = previous.accuracy() ?: return false
        if (speed > .25 || previousSpeed > .25 || accuracy > 8 || previousAccuracy > 8) return false
        val radius = max(4.0, min(8.0, max(accuracy, previousAccuracy)))
        return metres(distanceStart, f) <= radius + .000001
    }

    fun breakSegment(time: Long, wall: Instant?) {
        tick(time)
        minimumFixTime = time
        anchor = null
        distanceAnchor = null
        gap = true
        segment++
        gps = "GPS temporarily lost · distance incomplete"
        eventRows.add(Event("GPS_LOST", time, wall))
    }

    private fun lost(f: Fix) {
        if (!gap) {
            eventRows.add(Event("GPS_LOST", f.monotonicMillis(), f.timestamp()))
            segment++
        }
        minimumFixTime = max(minimumFixTime, f.monotonicMillis())
        gap = true
        anchor = null
        distanceAnchor = null
        gps = "GPS temporarily lost · distance incomplete"
    }

    fun missingFix(time: Long, wall: Instant?) {
        tick(time)
        val previous = anchor
        if (previous != null && time - previous.monotonicMillis() > 15000) {
            lost(Fix(wall, time, previous.latitude(), previous.longitude(), 99.0, null, null, null))
            anchor = null
        } else if (previous != null && time - previous.monotonicMillis() > 5000)
            gps = "GPS signal weak"
    }

    private fun active(time: Long): Long =
        max(0L, time - started - pausedTotal - if (pausedSince >= 0) time - pausedSince else 0)

    fun status(time: Long): Status {
        val now = max(time, lastTime)
        var pace: Double? = null
        if (
            pointRows.isNotEmpty() &&
                pausedSince < 0 &&
                now - pointRows.last().fix().monotonicMillis() <= 5000
        ) {
            val end = pointRows.last()
            var begin = end
            for (i in pointRows.size - 2 downTo 0) {
                val p = pointRows[i]
                if (p.segment() != end.segment() || end.activeMillis() - p.activeMillis() > 30000)
                    break
                begin = p
            }
            val d = end.cumulativeMetres() - begin.cumulativeMetres()
            val dt = end.activeMillis() - begin.activeMillis()
            if (d >= 10 && dt >= 5000) pace = dt / d
        }
        var description = gps
        if (lastUsableFix >= 0 && now - lastUsableFix > 15000)
            description =
                "GPS temporarily lost" +
                    if (pausedSince >= 0) " · paused" else " · distance incomplete"
        else if (lastUsableFix >= 0 && now - lastUsableFix > 5000 && gps.startsWith("GPS ready"))
            description = "GPS signal weak" + if (pausedSince >= 0) " · paused" else ""
        if (description == "GPS ready" && now <= recoveredUntil) description = "GPS recovered"
        if (description == "GPS signal weak" || description.contains("lost")) pace = null
        return Status(distance, now - started, active(now), pace, description, pausedSince >= 0)
    }

    fun pointsFrom(index: Int): List<Point> =
        recordingSnapshot(pointRows.subList(index, pointRows.size))

    fun eventsFrom(index: Int): List<Event> =
        recordingSnapshot(eventRows.subList(index, eventRows.size))

    fun pointCount(): Int = pointRows.size

    fun eventCount(): Int = eventRows.size

    fun points(): List<Point> = recordingSnapshot(pointRows)

    fun events(): List<Event> = recordingSnapshot(eventRows)

    /** Mutable original output; elapsed samples preserve every pause/gap discontinuity. */
    fun samples(): List<DistanceSample> =
        pointRows.mapTo(mutableListOf()) { p ->
            DistanceSample(
                Durations.ofMillis(p.fix().monotonicMillis() - started),
                p.cumulativeMetres(),
                p.heartRate(),
                p.cadence(),
                p.fix().altitude(),
                p.discontinuity(),
            )
        }

    companion object {
        /** About two minutes of 1 Hz fixes: covers any resume confirmation window. */
        private const val PAUSED_FIX_LIMIT = 120

        @JvmStatic
        fun metres(a: Fix, b: Fix): Double {
            val lat = (b.latitude() - a.latitude()) * .017453292519943295
            val lon = (b.longitude() - a.longitude()) * .017453292519943295
            val h =
                sin(lat / 2) * sin(lat / 2) +
                    cos(a.latitude() * .017453292519943295) *
                        cos(b.latitude() * .017453292519943295) *
                        sin(lon / 2) *
                        sin(lon / 2)
            return 6371000 * 2.0 * atan2(sqrt(h), sqrt(max(0.0, 1 - h)))
        }
    }
}

internal fun <T> recordingSnapshot(values: Collection<T>): List<T> =
    object : AbstractList<T>() {
        private val rows = ArrayList(values)
        override val size: Int
            get() = rows.size

        override fun get(index: Int): T = rows[index]
    }
