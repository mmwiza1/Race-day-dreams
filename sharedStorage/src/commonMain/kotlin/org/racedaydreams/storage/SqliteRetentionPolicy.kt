package org.racedaydreams.storage

import org.racedaydreams.domain.storage.DetailRetention
import org.racedaydreams.domain.time.Instant

/**
 * Original informed-choice/preview/approval workflow. No cleanup command is exposed here. The host
 * must supply its real replacement-operation state, never assume it is idle. A future deletion
 * kernel still needs operation serialization and fresh checkBatch inside its transaction.
 */
class SqliteRetentionPolicy(
    private val database: SqlDatabase,
    private val freshChoiceId: () -> String,
    private val replacementInProgress: () -> Boolean,
) {
    /** Only call after the athlete has read and accepted the consequences of this policy. */
    fun acknowledge(policy: DetailRetention.Policy, now: Instant): Unit =
        database.transaction {
            val old = SqliteRetentionPreferences(database).load()
            val choice = sqlRequired(freshChoiceId(), "retention choice")
            require(choice != old.choiceId()) {
                "An acknowledged choice requires a fresh identity."
            }
            val next = DetailRetention.Preferences.acknowledged(policy, now, choice)
            database.execute(
                "UPDATE detail_retention_preferences SET policy=?,choice=?,chosen_at=?,choice_id=? WHERE id=1",
                listOf(
                    SqlValue.Text(next.policy().name),
                    SqlValue.Text(next.choice().name),
                    SqlValue.Text(requireNotNull(next.chosenAt()).toString()),
                    SqlValue.Text(requireNotNull(next.choiceId())),
                ),
            )
            require(database.query("SELECT changes()").single().single().sqlLong() == 1L) {
                "Retention preferences missing; existing detail retained."
            }
        }

    /** Read-only consequence preview is available before existing-user consent. */
    fun preview(policy: DetailRetention.Policy, now: Instant): DetailRetention.Preview =
        database.transaction {
            DetailRetention.preview(
                policy,
                SqliteRecordingInventory(database).inspect().recordings,
                now,
                epoch(),
            )
        }

    fun approve(preview: DetailRetention.Preview, now: Instant): DetailRetention.Approval =
        database.transaction {
            DetailRetention.approve(
                preview,
                SqliteRetentionPreferences(database).load(),
                now,
                epoch(),
                database
                    .query(
                        "SELECT 1 FROM recording_sessions WHERE state IN ('ACTIVE','PAUSED','RECOVERABLE') LIMIT 1"
                    )
                    .isNotEmpty(),
                replacementInProgress(),
            )
        }

    private fun epoch() =
        database
            .query("SELECT epoch FROM detail_retention_state WHERE id=1")
            .single()
            .single()
            .sqlText()
}
