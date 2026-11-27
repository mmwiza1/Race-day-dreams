package org.racedaydreams.health.workouts

import kotlinx.datetime.Instant
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.RunEnvironment
import org.racedaydreams.domain.activity.SourceIdentity
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.health.HealthImportPolicy
import org.racedaydreams.domain.time.Instants
import org.racedaydreams.health.body.BodyReadWindow
import org.racedaydreams.health.body.ReadCompleteness

/** Actual HealthKit workout summary; no route, heart-rate stream or moving time is inferred. */
data class HealthKitRun(
    val recordId: String,
    val origin: String,
    val start: Instant,
    val end: Instant,
    val distanceMetres: Double?,
    val indoor: Boolean? = null,
) {
    init {
        require(recordId.isNotBlank() && origin.isNotBlank()) {
            "Workout source identity is required."
        }
        require(end > start) { "Workout end must follow its start." }
        require(distanceMetres == null || distanceMetres.isFinite() && distanceMetres >= 0) {
            "Recorded workout distance must be finite and non-negative."
        }
    }
}

/**
 * Stage SDK results first; prepare a batch from CURRENT local data inside the host import scope.
 */
class HealthKitRunningRead
internal constructor(
    val window: BodyReadWindow,
    private val ownBundleIdentifier: String,
    records: Collection<HealthKitRun>,
) {
    val completeness = ReadCompleteness.VISIBLE_RECORDS_ONLY
    val records = Immutable.copyList(records)

    /**
     * Host reads previous/checkpoint and commits this batch under its one DB/operation boundary.
     */
    fun batch(
        previous: List<ActivityRecord>,
        retainedCheckpoint: String?,
    ): HealthImportPolicy.Batch =
        HealthKitRunningImport.batch(
            window,
            ownBundleIdentifier,
            records,
            previous,
            retainedCheckpoint,
        )
}

/** Uses the established provider import policy. Apple visible results NEVER establish deletions. */
object HealthKitRunningImport {
    const val PROVIDER = "health-kit"
    const val MAX_RECORDS = 10000

    fun batch(
        window: BodyReadWindow,
        ownBundleIdentifier: String,
        records: Collection<HealthKitRun>,
        previous: List<ActivityRecord>,
        retainedCheckpoint: String?,
    ): HealthImportPolicy.Batch {
        require(ownBundleIdentifier.isNotBlank()) { "The application bundle identity is required." }
        require(records.size <= MAX_RECORDS) {
            "Choose a shorter workout read window; nothing was truncated."
        }
        val prior =
            previous
                .filter { it.source().provider() == PROVIDER }
                .associateBy { it.source().recordId() }
        require(prior.size == previous.count { it.source().provider() == PROVIDER }) {
            "Conflicting retained workout identities; existing history is unchanged."
        }
        val visible = linkedMapOf<String, HealthKitRun>()
        for (record in records) {
            if (
                record.origin == ownBundleIdentifier ||
                    !window.contains(record.start) ||
                    record.end > window.until
            )
                continue
            val old = visible[record.recordId]
            require(old == null || old == record) {
                "A workout changed during this read; retained history is unchanged."
            }
            visible[record.recordId] = record
        }
        val activities =
            visible.values.map { record ->
                val start = time(record.start)
                val end = time(record.end)
                val old = prior[record.recordId]
                if (old != null) {
                    // HealthKit samples are immutable identities. Do not move a retained workout or
                    // transfer its optional measurements to an incompatible source observation.
                    require(
                        old.start() == start &&
                            old.end() == end &&
                            old.source().originPackage() == record.origin
                    ) {
                        "A retained workout conflicts with its source identity; history is unchanged."
                    }
                    old.copy(
                        distanceMetres = record.distanceMetres ?: old.distanceMetres(),
                        environment =
                            if (record.indoor == true) RunEnvironment.INDOOR else old.environment(),
                    )
                } else
                    ActivityRecord(
                        "$PROVIDER:${record.recordId}",
                        SourceIdentity(PROVIDER, record.recordId, record.origin),
                        start,
                        end,
                        null,
                        null,
                        record.distanceMetres,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        emptyList(),
                        emptyList(),
                        null,
                        null,
                        environment = RunEnvironment.INDOOR.takeIf { record.indoor == true },
                    )
            }
        return HealthImportPolicy.Batch(
            PROVIDER,
            activities,
            emptyList(),
            retainedCheckpoint,
            time(window.from),
            time(window.until),
            false,
        )
    }

    private fun time(value: Instant) =
        Instants.ofEpochSecond(value.epochSeconds, value.nanosecondsOfSecond.toLong())
}
