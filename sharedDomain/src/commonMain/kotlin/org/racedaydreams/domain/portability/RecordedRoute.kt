package org.racedaydreams.domain.portability

import org.racedaydreams.domain.activity.SourceIdentity
import org.racedaydreams.domain.coaching.coachingReadOnlyList

/** Original route segments keep pause/discontinuity boundaries; no connecting distance invented. */
data class RecordedRoute(
    private val id: String?,
    private val activityId: String?,
    private val source: SourceIdentity?,
    private var segments: List<List<TrackPoint>>?,
    private var name: String?,
) {
    init {
        if (id == null) throw NullPointerException("route id")
        if (source == null) throw NullPointerException("route source")
        if (segments == null) throw NullPointerException("route segments")
        require(id.trim { it <= ' ' }.isNotEmpty()) { "A route id is required." }
        val copy = mutableListOf<List<TrackPoint>>()
        for (segment in segments!!) {
            val points = coachingReadOnlyList(segment)
            var previous: TrackPoint? = null
            for (point in points) {
                if (point == null) throw NullPointerException("route point")
                if (previous != null && !point.time().isAfter(previous.time()))
                    throw IllegalArgumentException("Route points must be ordered within a segment.")
                previous = point
            }
            copy.add(points)
        }
        segments = coachingReadOnlyList(copy)
        name = name ?: ""
    }

    fun id(): String = id!!

    fun activityId(): String? = activityId

    fun source(): SourceIdentity = source!!

    fun segments(): List<List<TrackPoint>> = segments!!

    fun name(): String = name!!

    override fun toString(): String =
        "RecordedRoute[id=$id, activityId=$activityId, source=$source, segments=$segments, name=$name]"
}
