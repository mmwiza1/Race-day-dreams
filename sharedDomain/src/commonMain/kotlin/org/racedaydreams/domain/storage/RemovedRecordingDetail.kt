package org.racedaydreams.domain.storage

import org.racedaydreams.domain.isJavaBlank
import org.racedaydreams.domain.time.Instant

/** Original exact retained fingerprints for explicit compatible-archive restoration. */
data class RemovedRecordingDetail(
    private val sessionId: String?,
    private val activityId: String?,
    private val provider: String?,
    private val recordId: String?,
    private val endedAt: Instant?,
    private val removedAt: Instant?,
    private val policy: DetailRetention.Policy?,
    private val startedAt: Instant?,
    private val startedMono: Long,
    private val lastMono: Long,
    private val points: Long,
    private val events: Long,
    private val pointsHash: String?,
    private val eventsHash: String?,
) {
    init {
        for (id in listOf(sessionId, activityId, recordId)) require(
            id != null && !isJavaBlank(id) && id.length <= 256
        ) {
            "Invalid removed-detail identity."
        }
        require(sessionId == recordId && (provider == "rdd" || provider == "gpx")) {
            "Only app-owned recording detail can be removed."
        }
        if (endedAt == null || removedAt == null || startedAt == null || policy == null)
            throw NullPointerException()
        require(
            policy != DetailRetention.Policy.KEEP_FOREVER &&
                !endedAt.isAfter(removedAt) &&
                !startedAt.isAfter(endedAt) &&
                startedMono >= 0 &&
                lastMono >= startedMono &&
                points >= 0 &&
                events >= 0 &&
                (points != 0L || events != 0L)
        ) {
            "Invalid removed-detail evidence."
        }
        for (hash in listOf(pointsHash, eventsHash)) require(
            hash != null && hash.matches(Regex("[a-f0-9]{64}"))
        ) {
            "Missing original detail fingerprint."
        }
    }

    fun sessionId(): String = sessionId!!

    fun activityId(): String = activityId!!

    fun provider(): String = provider!!

    fun recordId(): String = recordId!!

    fun endedAt(): Instant = endedAt!!

    fun removedAt(): Instant = removedAt!!

    fun policy(): DetailRetention.Policy = policy!!

    fun startedAt(): Instant = startedAt!!

    fun startedMono(): Long = startedMono

    fun lastMono(): Long = lastMono

    fun points(): Long = points

    fun events(): Long = events

    fun pointsHash(): String = pointsHash!!

    fun eventsHash(): String = eventsHash!!

    override fun toString(): String =
        "RemovedRecordingDetail[sessionId=$sessionId, activityId=$activityId, provider=$provider, recordId=$recordId, endedAt=$endedAt, removedAt=$removedAt, policy=$policy, startedAt=$startedAt, startedMono=$startedMono, lastMono=$lastMono, points=$points, events=$events, pointsHash=$pointsHash, eventsHash=$eventsHash]"
}
