package org.racedaydreams.domain.activity

import org.racedaydreams.domain.health.HealthImportRepository
import org.racedaydreams.domain.time.*

/** Existing typed persistence boundary; platform owns IO and transactions. */
interface ActivityRepository : HealthImportRepository {
    fun activitySummaries(): List<ActivityRecord>

    fun activityDetail(id: String): ActivityRecord?

    fun coachingActivities(today: LocalDate): List<ActivityRecord>

    fun activitySummaryPage(before: Instant?, afterId: String?, limit: Int): List<ActivityRecord>

    fun upsertManualActivity(activity: ActivityRecord)

    fun deleteActivity(id: String)

    fun saveActivityNotes(id: String, notes: String?, rpe: Double?)

    fun assignShoe(activityId: String, shoeId: String?)
}
