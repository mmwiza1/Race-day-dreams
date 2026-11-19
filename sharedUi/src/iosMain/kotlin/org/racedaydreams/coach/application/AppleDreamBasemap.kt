@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.coach.application

import androidx.compose.ui.graphics.Path
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.racedaydreams.domain.support.BasemapInput
import org.racedaydreams.domain.support.DreamBasemap
import org.racedaydreams.domain.support.DreamRoutes
import platform.Foundation.NSBundle
import platform.posix.*

internal fun loadAppleDreamBasemap(routeId: String): Map<DreamBasemap.Kind, Path>? {
    if (!routeId.matches(Regex("[a-z0-9-]+-geo-v1"))) return null
    val resource =
        NSBundle.mainBundle.pathForResource(
            routeId,
            ofType = "rddmap",
            inDirectory = "dream-basemaps",
        ) ?: return null
    val file = fopen(resource, "rb") ?: return null
    val data =
        try {
            DreamBasemap.read(
                object : BasemapInput() {
                    override fun readChunk(buffer: ByteArray): Int {
                        val count =
                            buffer
                                .usePinned {
                                    fread(it.addressOf(0), 1u, buffer.size.toULong(), file)
                                }
                                .toInt()
                        check(ferror(file) == 0) { "Could not read bundled Dream Route geography." }
                        return if (count == 0) -1 else count
                    }
                },
                routeId,
            )
        } finally {
            fclose(file)
        }
    val layers = DreamBasemap.Kind.entries.associateWith { Path() }
    for (feature in data.features()) {
        val kind = requireNotNull(feature.kind())
        for (vertices in feature.paths()) {
            layers.getValue(kind).appendDreamVertices(vertices)
            if (kind != DreamBasemap.Kind.ROAD) layers.getValue(kind).close()
        }
    }
    return layers
}

internal fun Path.appendDreamVertices(vertices: List<DreamRoutes.Vertex>) {
    vertices.forEachIndexed { index, vertex ->
        val x = vertex.x().toFloat()
        val y = vertex.y().toFloat()
        if (index == 0) moveTo(x, y) else lineTo(x, y)
    }
}
