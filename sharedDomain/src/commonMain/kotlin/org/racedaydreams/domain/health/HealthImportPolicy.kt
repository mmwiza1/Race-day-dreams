package org.racedaydreams.domain.health

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.coaching.coachingReadOnlyList
import org.racedaydreams.domain.time.Instant

/** Deterministic validation and bounded reconciliation; permission facts remain caller-owned. */
object HealthImportPolicy {
    data class Batch(
        private val provider: String?,
        private var upserts: List<ActivityRecord>?,
        private var deletedRecordIds: List<String>?,
        private val nextToken: String?,
        private val windowStart: Instant?,
        private val windowEnd: Instant?,
        private val completeSnapshot: Boolean,
    ) {
        init {
            require(provider != null && provider.trim { it <= ' ' }.isNotEmpty()) {
                "An import provider is required."
            }
            if (windowStart == null || windowEnd == null) throw NullPointerException()
            require(windowEnd.isAfter(windowStart)) {
                "The import window must have positive duration."
            }
            upserts = coachingReadOnlyList(upserts ?: throw NullPointerException())
            deletedRecordIds =
                coachingReadOnlyList(deletedRecordIds ?: throw NullPointerException())
            val ids = mutableSetOf<String>()
            for (activity in upserts!!) {
                require(provider == activity.source().provider()) {
                    "A batch cannot overwrite a different provider."
                }
                require(ids.add(activity.source().recordId())) {
                    "The batch contains a duplicate source record."
                }
            }
        }

        fun provider(): String = provider!!

        fun upserts(): List<ActivityRecord> = upserts!!

        fun deletedRecordIds(): List<String> = deletedRecordIds!!

        fun nextToken(): String? = nextToken

        fun windowStart(): Instant = windowStart!!

        fun windowEnd(): Instant = windowEnd!!

        fun completeSnapshot(): Boolean = completeSnapshot

        override fun toString(): String =
            "Batch[provider=$provider, upserts=$upserts, deletedRecordIds=$deletedRecordIds, nextToken=$nextToken, windowStart=$windowStart, windowEnd=$windowEnd, completeSnapshot=$completeSnapshot]"
    }

    /** Missing implies deletion only inside a completely read, authorized snapshot window. */
    @JvmStatic
    fun deletions(existing: List<ActivityRecord>, batch: Batch): List<String> {
        val seen = batch.upserts().map { it.source().recordId() }.toSet()
        val deleted = LinkedHashSet(batch.deletedRecordIds())
        if (batch.completeSnapshot())
            for (activity in existing) {
                if (batch.provider() != activity.source().provider()) continue
                val inside =
                    !activity.start().isBefore(batch.windowStart()) &&
                        !activity.end().isAfter(batch.windowEnd())
                if (inside && activity.source().recordId() !in seen)
                    deleted.add(activity.source().recordId())
            }
        deleted.removeAll(seen)
        return coachingReadOnlyList(deleted.toList())
    }

    private data class SourceKey(val provider: String, val recordId: String)

    /** Later upsert wins for an exact provider identity; unrelated opaque IDs stay distinct. */
    @JvmStatic
    fun unique(records: List<ActivityRecord>): List<ActivityRecord> {
        val unique = LinkedHashMap<SourceKey, ActivityRecord>()
        for (record in records) unique[
            SourceKey(record.source().provider(), record.source().recordId())] = record
        return coachingReadOnlyList(unique.values.toList())
    }

    @JvmStatic
    fun commit(repository: HealthImportRepository, batch: Batch) {
        val removed = deletions(repository.activities(), batch)
        repository.applyImport(batch.provider(), batch.upserts(), removed, batch.nextToken())
    }
}
