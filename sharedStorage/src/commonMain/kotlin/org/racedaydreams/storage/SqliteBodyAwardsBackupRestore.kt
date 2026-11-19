package org.racedaydreams.storage

/**
 * Internal population of retained body observations and milestone proof. Not an independent restore
 * API: the validated whole-parent coordinator owns replacement. No health import, measurement
 * selection, award candidate calculation or celebration is performed here.
 */
internal class SqliteBodyAwardsBackupRestore(private val database: SqlDatabase) {
    fun populate(parent: UserBackupDataJson.Snapshot) =
        database.transaction {
            for (table in
                listOf(
                    "body_heights",
                    "body_guidance_preferences",
                    "training_milestones",
                    "training_milestone_sources",
                    "training_milestone_preferences",
                )) require(database.query("SELECT 1 FROM $table LIMIT 1").isEmpty()) {
                "Body and milestone restore requires empty prepared tables; existing data was retained."
            }
            val body = SqliteBodyHeights(database)
            parent.body.heights.forEach { body.saveHeight(it) }
            body.preferences(parent.body.preferences)
            val milestones = SqliteTrainingMilestones(database)
            // These are already validated retained awards, NOT newly calculated candidates. Each
            // original recognition time is supplied explicitly; the repository has no alert
            // callback.
            parent.achievements.awards.forEach { stored ->
                milestones.recognize(listOf(stored.award), stored.recognizedAt)
            }
            milestones.celebrations(parent.achievements.celebrations)
        }
}
