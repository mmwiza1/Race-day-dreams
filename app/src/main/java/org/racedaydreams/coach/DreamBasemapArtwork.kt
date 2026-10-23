package org.racedaydreams.coach

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.racedaydreams.domain.support.DreamBasemap
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** Five immutable geographic extracts; parsed once off-main, never a runtime map client. */
private object DreamBasemapCache {
    private val maps = ConcurrentHashMap<String, DreamBasemap.MapData>()
    fun get(context: Context, id: String): DreamBasemap.MapData? {
        if (!id.matches(Regex("[a-z0-9-]+-geo-v1"))) return null // legacy assignments keep their original frame
        maps[id]?.let { return it }
        return try {
            context.assets.open("dream-basemaps/$id.rddmap").use { DreamBasemap.read(it, id) }
                .also { maps.putIfAbsent(id, it) }
        } catch (error: IOException) {
            android.util.Log.w("DreamBasemap", "Offline city context unavailable for $id", error)
            null // course and progress still work; never invent replacement geography
        }
    }
}

internal data class DreamMapPaths(val roads: Path, val water: Path, val green: Path)

@Composable internal fun rememberDreamBasemap(id: String): DreamMapPaths? {
    val context = LocalContext.current.applicationContext
    val paths by produceState<DreamMapPaths?>(null, id) {
        value = withContext(Dispatchers.Default) {
            val data = withContext(Dispatchers.IO) { DreamBasemapCache.get(context, id) }
            data?.let {
                val paths = DreamBasemap.Kind.values().associateWith { Path() }
                for (feature in it.features()) for (points in feature.paths()) {
                    val path = paths.getValue(feature.kind())
                    points.forEachIndexed { index, p ->
                        if (index == 0) path.moveTo(p.x().toFloat(), p.y().toFloat())
                        else path.lineTo(p.x().toFloat(), p.y().toFloat())
                    }
                    if (feature.kind() != DreamBasemap.Kind.ROAD) path.close()
                }
                DreamMapPaths(paths.getValue(DreamBasemap.Kind.ROAD), paths.getValue(DreamBasemap.Kind.WATER), paths.getValue(DreamBasemap.Kind.GREEN))
            }
        }
    }
    return paths
}
