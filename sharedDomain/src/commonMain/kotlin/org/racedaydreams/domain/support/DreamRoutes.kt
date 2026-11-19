package org.racedaydreams.domain.support

import kotlin.jvm.JvmStatic
import kotlin.math.*
import org.racedaydreams.domain.coaching.*

/** Versioned offline marathon-equivalent drawing geometry, not GPS evidence or navigation. */
object DreamRoutes {
    data class Vertex(private val x: Double, private val y: Double) {
        fun x(): Double = x

        fun y(): Double = y

        override fun toString(): String = "Vertex[x=$x, y=$y]"
    }

    data class Route(
        private val id: String?,
        private val name: String?,
        private var vertices: List<Vertex>?,
    ) {
        init {
            require(
                id != null &&
                    id.trim { it <= ' ' }.isNotEmpty() &&
                    name != null &&
                    name.trim { it <= ' ' }.isNotEmpty() &&
                    vertices != null &&
                    vertices!!.size in 2..10000
            ) {
                "Invalid Dream geometry"
            }
            vertices = coachingReadOnlyList(vertices!!)
            var length = false
            for (i in vertices!!.indices) {
                val v = vertices!![i]
                require(
                    v.x().isFinite() &&
                        v.y().isFinite() &&
                        v.x() >= 0 &&
                        v.x() <= 10 &&
                        v.y() >= 0 &&
                        v.y() <= 10
                ) {
                    "Invalid drawing bounds"
                }
                if (i > 0 && dreamDistance(vertices!![i - 1], v) > 0) length = true
            }
            require(length) { "Empty route geometry" }
        }

        fun id(): String = id!!

        fun name(): String = name!!

        fun vertices(): List<Vertex> = vertices!!

        override fun toString(): String = "Route[id=$id, name=$name, vertices=$vertices]"
    }

    data class Assignment(
        private val routeId: String?,
        private val startMetres: Double,
        private val coveredMetres: Double,
    ) {
        init {
            require(
                routeId != null &&
                    DreamGeometryRegistry.index.containsKey(routeId) &&
                    startMetres.isFinite() &&
                    coveredMetres.isFinite() &&
                    coveredMetres > 0 &&
                    startMetres >= 0 &&
                    startMetres + min(42195.0, coveredMetres) <= 42195.001
            ) {
                "Invalid Dream Route segment"
            }
        }

        fun routeId(): String = routeId!!

        fun startMetres(): Double = startMetres

        fun coveredMetres(): Double = coveredMetres

        override fun toString(): String =
            "Assignment[routeId=$routeId, startMetres=$startMetres, coveredMetres=$coveredMetres]"
    }

    data class Landmark(
        private val label: String?,
        private val x: Double,
        private val y: Double,
        private val shape: String?,
    ) {
        fun label(): String? = label

        fun x(): Double = x

        fun y(): Double = y

        fun shape(): String? = shape

        override fun toString(): String = "Landmark[label=$label, x=$x, y=$y, shape=$shape]"
    }

    @JvmStatic fun routes(): List<Route> = DreamGeometryRegistry.cities

    @JvmStatic
    fun forId(id: String?): Route =
        if (id == null) throw IllegalArgumentException("Unknown route geometry")
        else
            DreamGeometryRegistry.index[id]
                ?: throw IllegalArgumentException("Unknown route geometry")

    @JvmStatic
    fun currentGeometry(city: Route): Route =
        when (city.id()) {
            "paris-v2" -> forId("paris-2026-geo-v1")
            "new-york-v2" -> forId("new-york-2026-geo-v1")
            "london-v2" -> forId("london-historical-geo-v1")
            "berlin-v2" -> forId("berlin-2014-geo-v1")
            "tokyo-v2" -> forId("tokyo-2026-geo-v1")
            else -> city
        }

    @JvmStatic fun provenance(route: Route): String = dreamProvenance(route.id())

    @JvmStatic
    fun attribution(route: Route): String =
        when (route.id()) {
            "paris-2026-geo-v1",
            "new-york-2026-geo-v1",
            "london-historical-geo-v1",
            "berlin-2014-geo-v1",
            "tokyo-2026-geo-v1" ->
                "OpenFreeMap © OpenMapTiles · © OpenStreetMap contributors · ODbL"
            "paris-2024-olympic-geo-v1" ->
                "OpenFreeMap © OpenMapTiles · © OpenStreetMap contributors · ODbL. Course: Thomas Krakow · ODbL"
            else -> ""
        }

    @JvmStatic
    fun landmarks(route: Route): List<Landmark> {
        val geographic = DreamCartography.landmarks(route.id())
        return if (geographic.isNotEmpty()) geographic
        else schematicLandmarks(route.id().replace("-v2", ""))
    }

    @JvmStatic
    fun assign(metres: Double, random: DreamRandom): Assignment =
        assign(
            metres,
            random,
            setOf("london-v2", "berlin-v2", "paris-v2", "tokyo-v2", "new-york-v2"),
        )

    @JvmStatic
    fun assign(metres: Double, random: DreamRandom, eligible: Set<String>): Assignment {
        require(metres.isFinite() && metres > 0) { "No recorded distance" }
        val routes = routes().filter { it.id() in eligible }
        require(routes.isNotEmpty()) { "No eligible city" }
        val route = currentGeometry(routes[random.nextInt(routes.size)])
        return Assignment(route.id(), random.nextDouble() * max(0.0, 42195 - metres), metres)
    }

    @JvmStatic
    fun at(route: Route, metres: Double): Vertex {
        require(metres.isFinite()) { "Invalid route progress" }
        val vertices = route.vertices()
        if (metres <= 0) return vertices.first()
        if (metres >= 42195) return vertices.last()
        var length = 0.0
        for (i in 1 until vertices.size) length += dreamDistance(vertices[i - 1], vertices[i])
        var left = max(0.0, min(1.0, metres / 42195)) * length
        for (i in 1 until vertices.size) {
            val a = vertices[i - 1]
            val b = vertices[i]
            val distance = dreamDistance(a, b)
            if (distance == 0.0) continue
            if (left <= distance)
                return Vertex(
                    a.x() + (b.x() - a.x()) * left / distance,
                    a.y() + (b.y() - a.y()) * left / distance,
                )
            left -= distance
        }
        return vertices.last()
    }

    /**
     * Exact clipped polyline; retains every intervening original vertex rather than sampled chords.
     */
    @JvmStatic
    fun section(route: Route, from: Double, to: Double): List<Vertex> {
        require(from.isFinite() && to.isFinite() && to >= from) { "Invalid route section" }
        val low = max(0.0, min(42195.0, from))
        val high = max(0.0, min(42195.0, to))
        val points = mutableListOf(at(route, low))
        if (high == low) return coachingReadOnlyList(points)
        val vertices = route.vertices()
        var total = 0.0
        for (i in 1 until vertices.size) total += dreamDistance(vertices[i - 1], vertices[i])
        var cumulative = 0.0
        for (i in 1 until vertices.size) {
            cumulative += dreamDistance(vertices[i - 1], vertices[i])
            val equivalent = cumulative / total * 42195
            if (equivalent > low && equivalent < high && vertices[i] != points.last())
                points.add(vertices[i])
        }
        val end = at(route, high)
        if (end != points.last()) points.add(end)
        return coachingReadOnlyList(points)
    }
}

private fun dreamDistance(a: DreamRoutes.Vertex, b: DreamRoutes.Vertex): Double =
    hypot(a.x() - b.x(), a.y() - b.y())

/** Original immutable registry; built once and never rehydrated per card/recomposition. */
private object DreamGeometryRegistry {
    val cities = Immutable.copyList(cityDreamRoutes())
    val index: Map<String, DreamRoutes.Route> = build()

    private fun build(): Map<String, DreamRoutes.Route> {
        val routes = legacyDreamRoutes().toMutableList()
        routes.addAll(cities)
        routes.add(Berlin2014Geometry.route())
        routes.add(Tokyo2026Geometry.route())
        routes.add(LondonHistoricalGeometry.route())
        routes.add(NewYork2026Geometry.route())
        routes.add(Paris2024Geometry.route())
        routes.add(Paris2026Geometry.route())
        val index = LinkedHashMap<String, DreamRoutes.Route>()
        for (route in routes) check(index.put(route.id(), route) == null) {
            "Duplicate geometry revision"
        }
        return Immutable.copyMap(index)
    }
}

/** JVM remains exactly java.util.Random at the original source/binary boundary. */
expect open class DreamRandom {
    open fun nextInt(bound: Int): Int

    open fun nextDouble(): Double
}

expect object DreamRandoms {
    fun seeded(seed: Long): DreamRandom
}

/**
 * Original implementation of the documented 48-bit LCG used by java.util.Random, for native parity.
 */
internal class PortableDreamRandom(seed: Long) {
    private var state = (seed xor 0x5DEECE66DL) and ((1L shl 48) - 1)

    private fun next(bits: Int): Int {
        state = (state * 0x5DEECE66DL + 0xBL) and ((1L shl 48) - 1)
        return (state ushr (48 - bits)).toInt()
    }

    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive" }
        if (bound and (bound - 1) == 0) return ((bound.toLong() * next(31)) shr 31).toInt()
        var bits: Int
        var value: Int
        do {
            bits = next(31)
            value = bits % bound
        } while (bits - value + (bound - 1) < 0)
        return value
    }

    fun nextDouble(): Double = ((next(26).toLong() shl 27) + next(27)) / 9007199254740992.0
}
