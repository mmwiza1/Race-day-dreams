package org.racedaydreams.domain.polish

import kotlin.jvm.JvmStatic
import kotlin.math.*
import org.racedaydreams.domain.activity.roundedNanos as roundedBoundary
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.domain.recording.recordingSnapshot

/** Original pure drawing projection; never mutates observations or joins discontinuities. */
object RoutePresentation {
    data class XY(private val x: Double, private val y: Double) {
        fun x(): Double = x

        fun y(): Double = y

        override fun toString(): String = "XY[x=$x, y=$y]"
    }

    data class Section(private val kilometre: Int, private val secondsPerKm: Double) {
        fun kilometre(): Int = kilometre

        fun secondsPerKm(): Double = secondsPerKm

        override fun toString(): String =
            "Section[kilometre=$kilometre, secondsPerKm=$secondsPerKm]"
    }

    @JvmStatic
    fun fit(points: List<RunMetricsEngine.Point>): List<XY> {
        if (points.isEmpty()) return emptyList()
        val latitude = points.first().fix().latitude()
        val cosine = max(.01, cos(latitude * .017453292519943295))
        var minX = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        for (point in points) {
            val x = point.fix().longitude() * cosine
            val y = -point.fix().latitude()
            minX = min(minX, x)
            maxX = max(maxX, x)
            minY = min(minY, y)
            maxY = max(maxY, y)
        }
        val span = max(1e-8, max(maxX - minX, maxY - minY))
        return recordingSnapshot(
            points.map { point ->
                XY(
                    .5 + (point.fix().longitude() * cosine - (minX + maxX) / 2) / span * .84,
                    .5 + (-point.fix().latitude() - (minY + maxY) / 2) / span * .84,
                )
            }
        )
    }

    @JvmStatic
    fun kilometres(points: List<RunMetricsEngine.Point>): List<Section> {
        val out = mutableListOf<Section>()
        var boundary = 1000.0
        if (points.isEmpty()) return out
        var last = points.first().activeMillis()
        var valid = true
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            if (b.discontinuity() || b.segment() != a.segment()) valid = false
            while (b.cumulativeMetres() >= boundary) {
                val delta = b.cumulativeMetres() - a.cumulativeMetres()
                if (delta <= 0) break
                val time =
                    a.activeMillis() +
                        roundedBoundary(
                            (b.activeMillis() - a.activeMillis()) *
                                (boundary - a.cumulativeMetres()) / delta
                        )
                if (valid && time > last)
                    out.add(Section((boundary / 1000).toInt(), (time - last) / 1000.0))
                last = time
                valid = true
                boundary += 1000
            }
        }
        return recordingSnapshot(out)
    }

    @JvmStatic
    fun story(points: List<RunMetricsEngine.Point>): List<String> {
        val out = mutableListOf<String>()
        val km = kilometres(points)
        if (km.isNotEmpty()) {
            val fast = km.minBy { it.secondsPerKm() }
            out.add(
                "Fastest complete kilometre: km ${fast.kilometre()} at ${pace(fast.secondsPerKm())}/km."
            )
        }
        if (km.size >= 4) {
            var first = 0.0
            var last = 0.0
            val count = km.size / 2
            for (i in 0 until count) {
                first += km[i].secondsPerKm()
                last += km[km.size - count + i].secondsPerKm()
            }
            val diff = roundedBoundary(last - first)
            out.add(
                "The final $count complete kilometres took ${abs(diff)} sec ${if (diff <= 0) "less" else "more"} than the opening $count. This describes pacing, not fitness."
            )
        }
        return out
    }

    private fun pace(value: Double): String {
        val seconds = roundedBoundary(value)
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }

    @JvmStatic
    fun reliableAltitude(points: List<RunMetricsEngine.Point>): Boolean =
        points.size >= 5 &&
            points.count {
                it.fix().altitude() != null &&
                    it.fix().verticalAccuracy()?.let { accuracy -> accuracy <= 10 } == true
            } >= points.size * .8
}
