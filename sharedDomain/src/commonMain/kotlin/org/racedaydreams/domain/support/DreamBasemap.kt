package org.racedaydreams.domain.support

import kotlin.jvm.JvmStatic
import kotlin.math.abs
import org.racedaydreams.domain.coaching.Immutable

/** Same bounded offline ODbL geographic data parser. Original renderer/code remains MIT. */
object DreamBasemap {
    enum class Kind {
        ROAD,
        WATER,
        GREEN,
    }

    data class Feature(private val kind: Kind?, private var paths: List<List<DreamRoutes.Vertex>>) {
        init {
            paths = Immutable.copyList(paths.map { Immutable.copyList(it) })
        }

        fun kind(): Kind? = kind

        fun paths(): List<List<DreamRoutes.Vertex>> = paths

        override fun toString(): String = "Feature[kind=$kind, paths=$paths]"
    }

    data class MapData(private val routeId: String?, private var features: List<Feature>) {
        init {
            features = Immutable.copyList(features)
        }

        fun routeId(): String? = routeId

        fun features(): List<Feature> = features

        override fun toString(): String = "MapData[routeId=$routeId, features=$features]"
    }

    /** No downsampling, projection or mutation. Caller owns the input stream. */
    @JvmStatic
    @Throws(BasemapReadException::class)
    fun read(input: BasemapInput, expectedRouteId: String?): MapData {
        val chunks = mutableListOf<ByteArray>()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            if (total + count > 3000000) throw BasemapReadException("Basemap exceeds size limit")
            if (count > 0) {
                chunks.add(buffer.copyOfRange(0, count))
                total += count
            }
        }
        val bytes = ByteArray(total)
        var index = 0
        for (chunk in chunks) {
            chunk.copyInto(bytes, index)
            index += chunk.size
        }
        val reader = Lines(bytes.decodeToString())
        if (reader.next() != "RDD-MAP 1") throw BasemapReadException("Unsupported basemap format")
        val id = reader.next()
        if (id != expectedRouteId || id == null || id.length > 80)
            throw BasemapReadException("Basemap/course frame mismatch")
        val features = mutableListOf<Feature>()
        var points = 0
        while (true) {
            val line = reader.next() ?: break
            if (line.isEmpty()) throw BasemapReadException("Empty basemap feature")
            val space = line.indexOf(' ')
            if (space < 1 || features.size >= 20000)
                throw BasemapReadException("Invalid basemap feature")
            val kind =
                try {
                    Kind.valueOf(line.substring(0, space).uppercase())
                } catch (e: IllegalArgumentException) {
                    throw BasemapReadException("Unknown basemap feature", e)
                }
            val paths = mutableListOf<List<DreamRoutes.Vertex>>()
            for (encoded in line.substring(space + 1).split('|')) {
                val path = mutableListOf<DreamRoutes.Vertex>()
                for (pair in encoded.split(' ')) {
                    if (++points > 150000) throw BasemapReadException("Too many basemap points")
                    val xy = pair.split(',')
                    try {
                        if (xy.size != 2) throw NumberFormatException()
                        val x = xy[0].toDouble()
                        val y = xy[1].toDouble()
                        if (!(x + y).isFinite() || abs(x) >= 100 || abs(y) >= 100)
                            throw NumberFormatException()
                        path.add(DreamRoutes.Vertex(x, y))
                    } catch (e: NumberFormatException) {
                        throw BasemapReadException("Invalid basemap coordinate", e)
                    }
                }
                if (path.size < (if (kind == Kind.ROAD) 2 else 4))
                    throw BasemapReadException("Incomplete basemap path")
                if (kind != Kind.ROAD && path.first() != path.last())
                    throw BasemapReadException("Unclosed basemap polygon")
                paths.add(path)
            }
            features.add(Feature(kind, paths))
        }
        if (features.isEmpty()) throw BasemapReadException("Empty basemap")
        return MapData(id, features)
    }

    private class Lines(private val text: String) {
        private var position = 0

        fun next(): String? {
            if (position >= text.length) return null
            val start = position
            while (
                position < text.length && text[position] != '\r' && text[position] != '\n'
            ) position++
            val end = position
            if (position < text.length) {
                val terminator = text[position++]
                if (terminator == '\r' && position < text.length && text[position] == '\n')
                    position++
            }
            return text.substring(start, end)
        }
    }
}
