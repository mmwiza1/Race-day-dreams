package org.racedaydreams.storage

/**
 * Internal full JSON replacement kernel. Validate the COMPLETE file before any database mutation.
 * Native UI remains disabled until companion raw merge and platform operation/consent wiring are
 * finished. This kernel never removes recording sessions, raw points/events or data held by
 * external health providers.
 */
internal class SqliteApplicationBackupRestore(
    private val database: SqlDatabase,
    private val freshEpoch: () -> String,
) {
    fun replace(bytes: ByteArray) {
        val parent = UserBackupDataJson.inspect(bytes).snapshot
        replaceValidated(parent)
    }

    /** Only a complete UserBackupDataJson inspection may supply this internal transaction step. */
    internal fun replaceValidated(parent: UserBackupDataJson.Snapshot) {
        database.transaction {
            require(
                database
                    .query(
                        "SELECT 1 FROM recording_sessions WHERE state IN ('ACTIVE','PAUSED','RECOVERABLE') LIMIT 1"
                    )
                    .isEmpty()
            ) {
                "Finish or recover the active recording before replacing app data."
            }
            require(
                database
                    .query(
                        "SELECT 1 FROM strength_executions WHERE state IN ('ACTIVE','PAUSED','RECOVERY_REQUIRED') LIMIT 1"
                    )
                    .isEmpty()
            ) {
                "Finish or discard the unfinished gym workout before replacing app data."
            }
            require(!SqliteClimbing(database).hasDraft()) {
                "Save or discard the unsaved bouldering session before replacing app data."
            }
            SqliteOwnedActivityRestore(database).requirePreserved(parent.core)
            val contextRestore = SqliteRecordedContextRestore(database)
            val contexts = contextRestore.preserve(parent.support)
            // Keep known removed-detail provenance when an older JSON predates its receipt.
            // Existing raw evidence is never erased or hidden by an imported metadata receipt.
            val retained = SqliteRecordingDetailReceipts(database).snapshot().removals
            val removals = parent.retention.removals.associateBy { it.sessionId() }.toMutableMap()
            for (old in retained) {
                val prior = removals[old.sessionId()]
                require(prior == null || prior == old) {
                    "Conflicting removed-detail provenance; existing data retained."
                }
                if (prior == null) removals[old.sessionId()] = old
            }
            val retention =
                DetailRetentionArchiveJson.Snapshot(
                    parent.retention.preferences,
                    removals.values.toList(),
                )
            CoreBackupDataJson.validateRetention(parent.core, retention)
            for (receipt in retention.removals) {
                val available =
                    listOf("recording_points", "recording_events").any { table ->
                        database
                            .query(
                                "SELECT 1 FROM $table WHERE session_id=? LIMIT 1",
                                listOf(SqlValue.Text(receipt.sessionId())),
                            )
                            .isNotEmpty()
                    }
                if (available) SqliteRecordingEvidence(database).requireMatches(receipt)
            }
            for (table in CLEAR) database.execute("DELETE FROM $table")
            // Retain dense legacy payloads in place, avoiding a whole-history memory copy/staging
            // DB.
            database.execute("DELETE FROM activities WHERE provider NOT IN ('rdd','gpx')")
            SqliteCoreBackupRestore(database).populate(parent.core, retainOwnedRecordings = true)
            SqliteArchiveBackupRestore(database).populate(parent)
            contextRestore.populate(contexts)
            SqliteStrengthProgramming(
                    database,
                    { error("Restore cannot create programme timestamps") },
                    { error("Restore cannot re-date programmes") },
                )
                .populateArchive(parent)
            SqliteBodyAwardsBackupRestore(database).populate(parent)
            SqliteClimbing(database).replaceFromBackup(parent.climbing)
            // Invalidate old approvals BEFORE loading a legitimately unfinished imported gym
            // session.
            SqliteRetentionRestore(database, freshEpoch).replace(retention)
            SqliteGymExecutions(
                    database,
                    { error("Restore cannot create actuals") },
                    LegacyStrengthFingerprint {
                        error("Restore cannot regenerate repeat fingerprints")
                    },
                )
                .populateArchive(parent)
            require(database.query("PRAGMA foreign_key_check").isEmpty()) {
                "Restored data has missing references; previous data retained."
            }
        }
    }

    private companion object {
        val CLEAR =
            listOf(
                "gym_rest_timers",
                "gym_rest_preferences",
                "strength_association_targets",
                "strength_association_decisions",
                "strength_execution_events",
                "strength_execution_sets",
                "strength_executions",
                "strength_workout_preferences",
                "strength_workout_versions",
                "strength_concrete_draft_workouts",
                "strength_concrete_draft_blocks",
                "strength_concrete_drafts",
                "strength_programme_current",
                "strength_programme_decisions",
                "strength_programme_sessions",
                "strength_programme_blocks",
                "strength_programme_revisions",
                "strength_programme_draft_blocks",
                "strength_programme_drafts",
                "strength_custom_exercises",
                "gym_current",
                "gym_plan_versions",
                "gym_progression_decisions",
                "strength_user_state",
                "pb_acknowledgements",
                "strength_exclusions",
                "coach_evidence",
                "run_feedback",
                "run_responses",
                "coach_proposals",
                "coach_current_plan",
                "coach_plan_versions",
                "strength_sessions",
                "strength_exercises",
                "best_efforts",
                "preserved_best_efforts",
                "activity_annotations",
                "manual_results",
                "shoes",
                "import_checkpoints",
                "baseline_state",
                "settings",
                "assessment_results",
                "planned_workouts",
                "training_plans",
                "plan_adjustments",
                "onboarding_draft",
                "user_profile",
                "training_milestone_sources",
                "training_milestones",
                "training_milestone_preferences",
                "body_heights",
                "body_guidance_preferences",
                "recording_detail_removals",
                "coach_support",
                "release08",
            )
    }
}
