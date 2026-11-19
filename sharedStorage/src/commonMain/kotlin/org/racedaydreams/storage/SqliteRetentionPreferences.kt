package org.racedaydreams.storage

import org.racedaydreams.domain.storage.DetailRetention

/** Reads existing consent faithfully. This adapter deliberately does not expose cleanup. */
class SqliteRetentionPreferences(private val database: SqlDatabase) {
    fun load(): DetailRetention.Preferences {
        val row =
            database
                .query(
                    "SELECT policy,choice,chosen_at,choice_id FROM detail_retention_preferences WHERE id=1"
                )
                .single()
        return DetailRetention.Preferences(
            DetailRetention.Policy.valueOf((row[0] as SqlValue.Text).value),
            DetailRetention.Choice.valueOf((row[1] as SqlValue.Text).value),
            (row[2] as? SqlValue.Text)?.value?.let { StoredInstant.parse(it) },
            (row[3] as? SqlValue.Text)?.value,
        )
    }
}
