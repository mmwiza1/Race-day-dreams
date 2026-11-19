package org.racedaydreams.domain.polish

import kotlin.jvm.JvmStatic
import kotlin.math.*
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.domain.recording.recordingSnapshot

/**
 * Original rendering-only simplification. Every reduction spans at most 64 edges, retaining segment
 * ends and kilometre transitions. Never changes stored route or cumulative evidence.
 */
object RouteRenderGeometry {
    data class Edge(private val from: Int, private val to: Int) {
        fun from(): Int = from

        fun to(): Int = to

        override fun toString(): String = "Edge[from=$from, to=$to]"
    }

    @JvmStatic
    fun prepare(
        points: List<RunMetricsEngine.Point>,
        projected: List<RoutePresentation.XY>,
        tolerance: Double,
    ): List<Edge> {
        require(points.size == projected.size && tolerance.isFinite() && tolerance >= 0) {
            "Invalid render geometry"
        }
        val output = mutableListOf<Edge>()
        var start = 0
        for (i in 1 until points.size) {
            val point = points[i]
            if (point.discontinuity() || point.segment() != points[i - 1].segment()) {
                append(output, projected, start, i - 1, tolerance)
                start = i
            } else if (
                (point.cumulativeMetres() / 1000).toInt() !=
                    (points[i - 1].cumulativeMetres() / 1000).toInt()
            ) {
                append(output, projected, start, i - 1, tolerance)
                output.add(Edge(i - 1, i))
                start = i
            } else if (i - start == 64) {
                append(output, projected, start, i, tolerance)
                start = i
            }
        }
        append(output, projected, start, points.size - 1, tolerance)
        return recordingSnapshot(output)
    }

    private fun append(
        output: MutableList<Edge>,
        xy: List<RoutePresentation.XY>,
        start: Int,
        end: Int,
        tolerance: Double,
    ) {
        if (end <= start) return
        var maximum = tolerance * tolerance
        var split = -1
        for (i in start + 1 until end) {
            val error = errorSquared(xy[i], xy[start], xy[end])
            if (error > maximum) {
                maximum = error
                split = i
            }
        }
        if (split < 0) output.add(Edge(start, end))
        else {
            append(output, xy, start, split, tolerance)
            append(output, xy, split, end, tolerance)
        }
    }

    private fun errorSquared(
        p: RoutePresentation.XY,
        a: RoutePresentation.XY,
        b: RoutePresentation.XY,
    ): Double {
        val dx = b.x() - a.x()
        val dy = b.y() - a.y()
        val length = dx * dx + dy * dy
        val t =
            if (length == 0.0) 0.0
            else max(0.0, min(1.0, ((p.x() - a.x()) * dx + (p.y() - a.y()) * dy) / length))
        val x = p.x() - a.x() - t * dx
        val y = p.y() - a.y() - t * dy
        return x * x + y * y
    }
}
