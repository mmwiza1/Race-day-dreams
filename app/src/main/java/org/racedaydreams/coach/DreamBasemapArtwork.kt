package org.racedaydreams.coach

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.racedaydreams.domain.support.DreamBasemap
import org.racedaydreams.domain.support.DreamCartography
import org.racedaydreams.domain.support.DreamRoutes
import java.io.IOException

/** A small on-demand cache: opening one city never prepares the other cities. */
private class DreamPreparedCache<T>(private val capacity: Int = 3) {
    private val entries = java.util.LinkedHashMap<String, T>(capacity, .75f, true)
    fun get(id: String): T? = synchronized(entries) { entries[id] }
    fun retain(id: String, prepared: T): T = synchronized(entries) {
        entries[id] ?: prepared.also {
            entries[id] = it
            if (entries.size > capacity) entries.remove(entries.keys.first())
        }
    }
}

internal data class DreamMapPaths(val routeId: String, val roads: Path, val water: Path, val green: Path)
internal data class DreamMapBounds(val minX: Double, val maxX: Double, val minY: Double, val maxY: Double)
internal data class DreamKilometreMark(val kilometre: Int, val position: DreamRoutes.Vertex)
internal data class DreamCourseBase(
    val routeId: String,
    val bounds: DreamMapBounds,
    val course: Path,
    val rivers: List<Path>,
    val kilometres: List<DreamKilometreMark>,
    val landmarks: List<DreamRoutes.Landmark>,
    val start: DreamRoutes.Vertex,
    val finish: DreamRoutes.Vertex
)
internal data class DreamCoursePaths(
    val base: DreamCourseBase,
    val startMetres: Double,
    val finishMetres: Double,
    val highlighted: Path,
    val sectionVertices: List<DreamRoutes.Vertex>,
    val sectionStart: DreamRoutes.Vertex,
    val sectionFinish: DreamRoutes.Vertex
)

private fun unitPath(points: List<DreamRoutes.Vertex>) = Path().apply {
    points.forEachIndexed { index, point ->
        if (index == 0) moveTo(point.x().toFloat(), point.y().toFloat())
        else lineTo(point.x().toFloat(), point.y().toFloat())
    }
}

private object DreamBasemapCache {
    private val maps = DreamPreparedCache<DreamMapPaths>()

    suspend fun get(context: Context, id: String): DreamMapPaths? {
        if (!id.matches(Regex("[a-z0-9-]+-geo-v1"))) return null
        maps.get(id)?.let { return it }
        return try {
            val data = withContext(Dispatchers.IO) {
                context.assets.open("dream-basemaps/$id.rddmap").use { DreamBasemap.read(it, id) }
            }
            val prepared = withContext(Dispatchers.Default) {
                val paths = DreamBasemap.Kind.values().associateWith { Path() }
                for (feature in data.features()) for (points in feature.paths()) {
                    // Context-only visual detail filtering. Source data and the complete
                    // course remain untouched; tiny street fragments add map noise.
                    if (feature.kind() == DreamBasemap.Kind.ROAD) {
                        var length = 0.0
                        for (i in 1 until points.size) length += kotlin.math.hypot(
                            points[i].x() - points[i - 1].x(), points[i].y() - points[i - 1].y()
                        )
                        if (length < .10) continue
                    }
                    val path = paths.getValue(feature.kind())
                    points.forEachIndexed { index, point ->
                        if (index == 0) path.moveTo(point.x().toFloat(), point.y().toFloat())
                        else path.lineTo(point.x().toFloat(), point.y().toFloat())
                    }
                    if (feature.kind() != DreamBasemap.Kind.ROAD) path.close()
                }
                DreamMapPaths(id, paths.getValue(DreamBasemap.Kind.ROAD),
                    paths.getValue(DreamBasemap.Kind.WATER), paths.getValue(DreamBasemap.Kind.GREEN))
            }
            // Only prepared paths are retained. Decoded coordinate objects are released.
            maps.retain(id, prepared)
        } catch (error: IOException) {
            android.util.Log.w("DreamBasemap", "Offline city context unavailable for $id", error)
            null // The true course/progress still work; never invent replacement geography.
        }
    }
}

private object DreamCourseCache {
    private val courses = DreamPreparedCache<DreamCourseBase>()
    fun get(route: DreamRoutes.Route): DreamCourseBase = courses.get(route.id()) ?: run {
        // Preserve the frozen 0.8.1 course-plus-landmark frame and .75-unit margin.
        val landmarks = DreamRoutes.landmarks(route)
        val vertices = route.vertices() + landmarks.map { DreamRoutes.Vertex(it.x(), it.y()) }
        val bounds = DreamMapBounds(vertices.minOf { it.x() } - .75, vertices.maxOf { it.x() } + .75,
            vertices.minOf { it.y() } - .75, vertices.maxOf { it.y() } + .75)
        courses.retain(route.id(), DreamCourseBase(route.id(), bounds, unitPath(route.vertices()),
            DreamCartography.rivers(route.id()).map { unitPath(it.vertices()) },
            (5..40 step 5).map { DreamKilometreMark(it, DreamRoutes.at(route, it * 1000.0)) }, landmarks,
            DreamRoutes.at(route, 0.0), DreamRoutes.at(route, 42195.0)))
    }
}

@Composable internal fun rememberDreamBasemap(id: String): DreamMapPaths? {
    val context = LocalContext.current.applicationContext
    val paths by produceState<DreamMapPaths?>(null, id) { value = DreamBasemapCache.get(context, id) }
    // A changed activity may reuse this composition: never show another course frame
    // while its own paths are being prepared.
    return paths?.takeIf { it.routeId == id }
}

@Composable internal fun rememberDreamCoursePaths(
    route: DreamRoutes.Route, start: Double, finish: Double
): DreamCoursePaths? {
    val paths by produceState<DreamCoursePaths?>(null, route.id(), start, finish) {
        value = withContext(Dispatchers.Default) {
            val section = DreamRoutes.section(route, start, finish)
            DreamCoursePaths(DreamCourseCache.get(route), start, finish, unitPath(section), section,
                DreamRoutes.at(route, start), DreamRoutes.at(route, finish))
        }
    }
    // The optional existing live panel advances its finish every accepted sample.
    // Keep its already prepared (shorter) section visible while the next path is
    // built. Different routes/offsets and decreasing ranges never reuse that path.
    return paths?.takeIf {
        it.base.routeId == route.id() && it.startMetres == start && it.finishMetres <= finish
    }
}
