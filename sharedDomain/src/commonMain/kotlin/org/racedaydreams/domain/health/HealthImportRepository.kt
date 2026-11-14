package org.racedaydreams.domain.health

import org.racedaydreams.domain.activity.ActivityRecord

/** Provider-neutral synchronous import boundary. Platform calls from its IO dispatcher. */
interface HealthImportRepository {
    fun activities(): List<ActivityRecord>

    fun importToken(provider: String): String?

    /** Upserts, source-record deletions and nullable checkpoint must commit atomically. */
    fun applyImport(
        provider: String,
        upserts: List<ActivityRecord>,
        deletedRecordIds: List<String>,
        nextToken: String?,
    )
}
