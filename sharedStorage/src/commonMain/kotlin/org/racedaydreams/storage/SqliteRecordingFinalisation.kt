package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.WorkoutSegment
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.coaching.RunExecution
import org.racedaydreams.domain.recording.RecordingTimeline
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.domain.time.*
import org.racedaydreams.storage.SqlValue.Text
import org.racedaydreams.storage.StoredJson.row
import org.racedaydreams.storage.StoredJson.text
import org.racedaydreams.storage.SupportPayloadJson.array
import org.racedaydreams.storage.SupportPayloadJson.integer
import org.racedaydreams.storage.SupportPayloadJson.long
import org.racedaydreams.storage.SupportPayloadJson.number

/** Finishes retained evidence and its canonical activity as one SQLite operation. */
class SqliteRecordingFinalisation(
    private val database: SqlDatabase,
    private val now: () -> Instant,
    nextId: () -> String,
) {
    private val activities = SqliteActivities(database, now)
    private val coaching = SqliteRunningCoaching(database, now, nextId)

    fun finish(
        session: SqliteRecordingStaging.Session,
        movingMillis: Long?,
        recovered: Boolean,
    ): String =
        database.transaction {
            val key = listOf(Text(session.id))
            val header =
                database
                    .query(
                        "SELECT state,activity_id,started_at,started_mono,last_mono FROM recording_sessions WHERE id=?",
                        key,
                    )
                    .singleOrNull() ?: error("Staging not found; existing data retained")
            require(
                StoredInstant.parse(header[2].sqlText()) == session.start &&
                    header[3].sqlLong() == session.startedMono &&
                    header[4].sqlLong() == session.lastMono
            ) {
                "Staging changed; reload recovery before finishing"
            }
            if (header[0].sqlText() == "FINISHED") return@transaction header[1].sqlText()

            val points = payloads("recording_points", session.id).map(RecordingPointJson::decode)
            require(points.size >= 2) {
                "At least two usable GPS points are needed. Staged evidence is retained."
            }
            val events =
                payloads("recording_events", session.id).map { payload ->
                    val row = StoredJson.objectValue(payload)
                    RunMetricsEngine.Event(
                        requireNotNull(row.text("type")),
                        long(row, "mono"),
                        StoredInstant.parse(requireNotNull(row.text("wall"))),
                    )
                }
            val elapsed = (session.lastMono - session.startedMono).coerceAtLeast(1)
            val active =
                movingMillis
                    ?: RecordingTimeline.activeMillis(session.startedMono, session.lastMono, events)
            val samples =
                points.map {
                    DistanceSample(
                        Durations.ofMillis(it.fix().monotonicMillis() - session.startedMono),
                        it.cumulativeMetres(),
                        it.heartRate(),
                        it.cadence(),
                        it.fix().altitude(),
                        it.discontinuity(),
                    )
                }
            val provider = if (session.id.startsWith("gpx-")) "gpx" else "rdd"
            val id = "$provider:${session.id}"
            val distance = points.last().cumulativeMetres()
            val offset = Offsets.ofTotalSeconds(0)
            val start = session.start
            val end =
                Instants.ofEpochSecond(
                    start.getEpochSecond() + elapsed / 1000,
                    start.getNano().toLong() + (elapsed % 1000) * 1_000_000,
                )
            val heartRates = points.mapNotNull { it.heartRate() }
            val cadences = points.mapNotNull { it.cadence() }
            val sourceNote =
                if (provider == "rdd") "Recorded on this phone"
                else "Imported GPX (accuracy not supplied by file)"
            val activity =
                ActivityRecord(
                    id,
                    SourceIdentity(provider, session.id, "org.racedaydreams"),
                    start,
                    end,
                    offset,
                    offset,
                    distance,
                    if (provider == "gpx") null
                    else Durations.ofMillis(active.coerceIn(0, elapsed)),
                    heartRates.takeIf { it.isNotEmpty() }?.average(),
                    heartRates.maxOrNull(),
                    null,
                    cadences.takeIf { it.isNotEmpty() }?.average(),
                    null,
                    "$sourceNote. Distance excludes gaps; raw timestamps and events retained.",
                    samples,
                    emptyList(),
                    null,
                    null,
                )
            activities.putActivity(activity, userOwned = true)
            database.execute(
                "UPDATE recording_sessions SET state='FINISHED',activity_id=? WHERE id=?",
                listOf(Text(id), Text(session.id)),
            )
            linkDream(session.id, id, distance)
            linkExecution(session.id, id, recovered, samples)
            id
        }

    private fun payloads(table: String, id: String): List<String> =
        database
            .query(
                "SELECT payload FROM $table WHERE session_id=? ORDER BY sequence",
                listOf(Text(id)),
            )
            .map { it.single().sqlText() }

    private fun linkDream(sessionId: String, activityId: String, distance: Double) {
        if (distance <= 0) return
        val origin =
            database
                .query(
                    "SELECT payload FROM release08 WHERE kind='origin' AND id=?",
                    listOf(Text(sessionId)),
                )
                .singleOrNull()
                ?.single()
                ?.sqlText() ?: return
        val route =
            StoredJson.objectValue(origin).text("dreamRoute")?.takeIf {
                it.isNotBlank() && it != "null"
            } ?: return
        val payload = buildJsonObject {
            put("version", 1)
            put("route", route)
            put("start", 0)
            put("distance", distance)
            put("assignedAt", now().toString())
        }
        writeSupport("dream", activityId, payload)
    }

    private fun linkExecution(
        sessionId: String,
        activityId: String,
        recovered: Boolean,
        samples: List<DistanceSample>,
    ) {
        val original =
            database
                .query(
                    "SELECT payload FROM coach_support WHERE kind='execution' AND id=?",
                    listOf(Text(sessionId)),
                )
                .singleOrNull()
                ?.single()
                ?.sqlText() ?: return
        val payload = StoredJson.objectValue(original)
        if (payload.text("mode") in listOf("workout", "race")) {
            val steps = SupportPayloadJson.steps(payload)
            val events =
                if ("events" in payload) array(payload, "events", 2000).map { it.row() }
                else emptyList()
            val byStep = events.groupBy { integer(it, "step") }
            val completed =
                events
                    .filter { it.text("type") == "STEP_COMPLETED" }
                    .map { integer(it, "step") }
                    .toSet()
            val skipped =
                events
                    .filter { it.text("type") == "STEP_SKIPPED" }
                    .map { integer(it, "step") }
                    .toSet()
            val reliable = samples.drop(1).none { it.discontinuity() }
            val evidence =
                steps.mapIndexedNotNull { index, step ->
                    val stepEvents = byStep[index].orEmpty()
                    val start =
                        stepEvents.lastOrNull { it.text("type") == "STEP_STARTED" }
                            ?: return@mapIndexedNotNull null
                    val end =
                        stepEvents.lastOrNull {
                            it.text("type") in
                                listOf("STEP_COMPLETED", "STEP_SKIPPED", "WORKOUT_FINISHED_EARLY")
                        } ?: return@mapIndexedNotNull null
                    RunExecution.StepEvidence(
                        index,
                        step.kind(),
                        number(end, "metres") - number(start, "metres"),
                        long(end, "active") - long(start, "active"),
                        step.fastPace(),
                        step.slowPace(),
                        index in completed,
                        reliable,
                    )
                }
            val intervals =
                PlannedWorkoutJson.read(payload.getValue("workout").row()).category() ==
                    WorkoutCategory.INTERVALS
            val repetitions =
                steps.indices.count {
                    steps[it].kind() == WorkoutSegment.Kind.RUN && it in completed
                }
            val recoveries =
                steps.indices
                    .filter { steps[it].kind() == WorkoutSegment.Kind.RECOVERY }
                    .all { it in completed }
            val interruption =
                if (recovered) " Interrupted run: retained portion only; no completion is assumed."
                else ""
            coaching.saveResponse(
                RunExecution(
                    activityId,
                    payload.text("workoutId"),
                    payload.text("planVersion"),
                    repetitions.takeIf { intervals },
                    recoveries,
                    "Native structured execution: ${completed.size}/${steps.size} steps completed, ${skipped.size} skipped.$interruption",
                    evidence,
                )
            )
        }
        writeSupport(
            "execution",
            sessionId,
            buildJsonObject {
                payload.forEach { (name, value) -> put(name, value) }
                put("activityId", activityId)
                put("interrupted", recovered)
            },
        )
    }

    private fun writeSupport(kind: String, id: String, payload: JsonObject) {
        database.execute(
            "INSERT OR REPLACE INTO coach_support(kind,id,payload) VALUES(?,?,?)",
            listOf(Text(kind), Text(id), Text(payload.toString())),
        )
    }
}
