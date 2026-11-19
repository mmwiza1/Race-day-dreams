@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.coach.documents

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.racedaydreams.domain.portability.TrackPoint
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.Instants
import platform.Foundation.*
import platform.darwin.NSObject

internal object AppleGpxDecoder {
    const val MAX_BYTES = 24 * 1024 * 1024

    private val rootPath = listOf("gpx")
    private val trackPath = rootPath + "trk"
    private val segmentPath = trackPath + "trkseg"
    private val pointPath = segmentPath + "trkpt"
    private val timePath = pointPath + "time"
    private val heightPath = pointPath + "ele"
    private val structuralPaths =
        setOf(rootPath, trackPath, segmentPath, pointPath, timePath, heightPath)
    private val gpxNamespaces =
        setOf("http://www.topografix.com/GPX/1/0", "http://www.topografix.com/GPX/1/1")

    fun decode(bytes: ByteArray): List<List<TrackPoint>> {
        require(bytes.size in 1..MAX_BYTES) { "Choose a GPX file no larger than 24 MiB." }
        val text = bytes.decodeToString(throwOnInvalidSequence = true)
        require('\u0000' !in text && "<!DOCTYPE" !in text && "<!ENTITY" !in text) {
            "GPX document types and entities are not supported."
        }
        val data =
            bytes.usePinned {
                NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
            }
        val reader = TrackReader()
        val parser = NSXMLParser(data)
        parser.shouldProcessNamespaces = true
        parser.shouldResolveExternalEntities = false
        parser.delegate = reader
        val success = parser.parse()
        require(success && !reader.rejected && reader.count >= 2) {
            "Choose a valid GPX file with one timestamped track."
        }
        return reader.segments
    }

    private class TrackReader : NSObject(), NSXMLParserDelegateProtocol {
        val segments = mutableListOf<MutableList<TrackPoint>>()
        var count = 0
            private set

        var rejected = false
            private set

        private val path = mutableListOf<String>()
        private var tracks = 0
        private var latitude: Double? = null
        private var longitude: Double? = null
        private var height: Double? = null
        private var time: Instant? = null
        private val value = StringBuilder()

        override fun parser(
            parser: NSXMLParser,
            didStartElement: String,
            namespaceURI: String?,
            qualifiedName: String?,
            attributes: Map<Any?, *>,
        ) =
            validate(parser) {
                require(path != timePath && path != heightPath)
                path.add(didStartElement)
                require(path.size <= 16 && path.first() == "gpx")
                if (path in structuralPaths) {
                    require(namespaceURI.isNullOrEmpty() || namespaceURI in gpxNamespaces)
                }
                when (path) {
                    trackPath -> require(++tracks == 1)
                    segmentPath -> {
                        require(segments.size < 100000)
                        segments.add(mutableListOf())
                    }
                    pointPath -> {
                        latitude = (attributes["lat"] as? String)?.toDouble()
                        longitude = (attributes["lon"] as? String)?.toDouble()
                        height = null
                        time = null
                    }
                    timePath,
                    heightPath -> value.clear()
                }
            }

        override fun parser(parser: NSXMLParser, foundCharacters: String) =
            validate(parser) {
                if (path == timePath || path == heightPath) {
                    require(foundCharacters.length <= 128 - value.length)
                    value.append(foundCharacters)
                }
            }

        override fun parser(
            parser: NSXMLParser,
            didEndElement: String,
            namespaceURI: String?,
            qualifiedName: String?,
        ) =
            validate(parser) {
                when (path) {
                    timePath -> {
                        require(time == null)
                        val parsed = kotlinx.datetime.Instant.parse(value.toString().trim())
                        time =
                            Instants.ofEpochSecond(
                                parsed.epochSeconds,
                                parsed.nanosecondsOfSecond.toLong(),
                            )
                    }
                    heightPath -> {
                        require(height == null)
                        height = value.toString().trim().toDouble()
                    }
                    pointPath -> {
                        require(++count <= 100000)
                        segments
                            .last()
                            .add(
                                TrackPoint(
                                    requireNotNull(time),
                                    requireNotNull(latitude),
                                    requireNotNull(longitude),
                                    height,
                                    null,
                                    null,
                                    null,
                                )
                            )
                    }
                }
                path.removeAt(path.lastIndex)
            }

        private inline fun validate(parser: NSXMLParser, read: () -> Unit) {
            if (rejected) return
            try {
                read()
            } catch (_: IllegalArgumentException) {
                rejected = true
                parser.abortParsing()
            }
        }
    }
}
