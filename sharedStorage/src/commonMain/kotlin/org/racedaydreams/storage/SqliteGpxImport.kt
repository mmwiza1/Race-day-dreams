package org.racedaydreams.storage

import org.racedaydreams.domain.portability.TrackPoint
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.domain.time.Durations
import org.racedaydreams.domain.time.Instant

/** Imports validated track points through the existing recording evidence and finalisation path. */
class SqliteGpxImport(private val database: SqlDatabase, now: () -> Instant, newId: () -> String) {
    private val activities = SqliteActivities(database, now)
    private val staging = SqliteRecordingStaging(database)
    private val finalisation = SqliteRecordingFinalisation(database, now, newId)

    fun importTrack(segments: List<List<TrackPoint>>): String {
        val count = segments.sumOf { it.size.toLong() }
        require(count in 2..100000 && segments.size <= 100000) {
            "Choose a GPX track with 2 to 100,000 timestamped points."
        }
        val start = segments.first { it.isNotEmpty() }.first().time()
        val end = segments.last { it.isNotEmpty() }.last().time()
        val elapsed = Durations.between(start, end).toMillis()
        require(elapsed in 1..86_400_000) { "GPX tracks must be no longer than 24 hours." }
        val engine = RunMetricsEngine(0, start)
        val hash = archiveSha256()
        var previous: Instant? = null
        val id =
            try {
                segments.forEachIndexed { index, segment ->
                    segment.forEachIndexed { pointIndex, point ->
                        val time = point.time()
                        require(previous?.let(time::isAfter) != false) {
                            "GPX times must increase."
                        }
                        val millis = Durations.between(start, time).toMillis()
                        if (pointIndex == 0 && previous != null) engine.breakSegment(millis, time)
                        engine.accept(
                            RunMetricsEngine.Fix(
                                time,
                                millis,
                                point.latitude(),
                                point.longitude(),
                                null,
                                point.altitudeMetres(),
                                null,
                                null,
                            )
                        )
                        previous = time
                        val key =
                            "$time|${point.latitude()}|${point.longitude()}|${index + 1}\n"
                                .encodeToByteArray()
                        hash.update(key, 0, key.size)
                    }
                }
                "gpx-" +
                    hash.finish().joinToString("") {
                        (it.toInt() and 255).toString(16).padStart(2, '0')
                    }
            } finally {
                hash.close()
            }
        return database.transaction {
            val existing =
                database
                    .query(
                        "SELECT id FROM activities WHERE provider='gpx' AND record_id=?",
                        listOf(SqlValue.Text(id)),
                    )
                    .singleOrNull()
                    ?.single()
                    ?.sqlText()
            if (existing != null) return@transaction existing

            val matching = matchingRecordedActivity(segments.flatten(), start)
            if (matching != null) return@transaction matching

            require(engine.points().size >= 2 && engine.status(elapsed).metres() > 0) {
                "Insufficient reliable continuous GPX evidence."
            }
            require(staging.unfinished() == null) {
                "Finish or recover the open run before importing GPX."
            }
            staging.start(id, start, 0, null, null)
            staging.append(id, engine.points(), engine.events(), 0, 0, elapsed, "ACTIVE", null)
            finalisation.finish(
                requireNotNull(staging.unfinished()),
                movingMillis = null,
                recovered = false,
            )
        }
    }

    private fun matchingRecordedActivity(points: List<TrackPoint>, start: Instant): String? {
        val millis = storedEpochMillis(start)
        val identities =
            database.query(
                "SELECT a.id FROM activities a JOIN recording_sessions s ON s.activity_id=a.id WHERE a.provider IN ('rdd','gpx') AND s.state='FINISHED' AND a.start_millis BETWEEN ? AND ? AND (SELECT COUNT(*) FROM recording_points p WHERE p.session_id=s.id)=? ORDER BY a.start_millis DESC,a.id",
                listOf(
                    SqlValue.Integer(millis.coerceAtLeast(Long.MIN_VALUE + 600_000) - 600_000),
                    SqlValue.Integer(millis.coerceAtMost(Long.MAX_VALUE - 600_000) + 600_000),
                    SqlValue.Integer(points.size.toLong()),
                ),
            )
        return identities
            .asSequence()
            .map { it.single().sqlText() }
            .firstOrNull { candidate ->
                val retained = activities.recordedRoute(candidate).points
                retained.size == points.size &&
                    points.indices.all { index ->
                        val source = points[index]
                        val saved = retained[index].fix()
                        source.time() == saved.timestamp() &&
                            source.latitude() == saved.latitude() &&
                            source.longitude() == saved.longitude()
                    }
            }
    }
}
