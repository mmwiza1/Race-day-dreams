package org.racedaydreams.storage

import org.racedaydreams.domain.activity.ActivitySources
import org.racedaydreams.domain.settings.SettingsRepository
import org.racedaydreams.domain.strength.ExerciseExclusionRepository
import org.racedaydreams.domain.strength.ExerciseExclusionRepository.Exclusion
import org.racedaydreams.domain.strength.ExerciseExclusionRepository.Reason

/** Typed repositories share the same connection and caller's transaction, as on Android. */
class SqliteSettings(private val database: SqlDatabase) : SettingsRepository {
    private fun valid(key: String): String {
        require(key.length <= 256 && key.any { it.code > 0x20 }) { "Missing setting key." }
        return key
    }

    override fun getSetting(key: String): String? =
        (database
                .query(
                    "SELECT setting_value FROM settings WHERE setting_key=?",
                    listOf(SqlValue.Text(valid(key))),
                )
                .firstOrNull()
                ?.single() as? SqlValue.Text)
            ?.value

    override fun putSetting(key: String, value: String) {
        database.execute(
            "INSERT OR REPLACE INTO settings(setting_key,setting_value) VALUES(?,?)",
            listOf(
                SqlValue.Text(valid(key)),
                SqlValue.Text(
                    if (key == "provider.preference")
                        ActivitySources.normalizeLegacyPreference(value)
                    else value
                ),
            ),
        )
    }

    override fun removeSetting(key: String) {
        database.execute(
            "DELETE FROM settings WHERE setting_key=?",
            listOf(SqlValue.Text(valid(key))),
        )
    }
}

class SqliteExerciseExclusions(private val database: SqlDatabase) : ExerciseExclusionRepository {
    override fun exclusions(): List<Exclusion> =
        database
            .query(
                "SELECT exercise_id,reason,excluded_at FROM strength_exclusions ORDER BY exercise_id"
            )
            .map { row ->
                Exclusion(
                    (row[0] as SqlValue.Text).value,
                    Reason.valueOf((row[1] as SqlValue.Text).value),
                    StoredInstant.parse((row[2] as SqlValue.Text).value),
                )
            }

    override fun exclude(exclusion: Exclusion) {
        database.execute(
            "INSERT OR REPLACE INTO strength_exclusions(exercise_id,reason,excluded_at) VALUES(?,?,?)",
            listOf(
                SqlValue.Text(exclusion.exerciseId()),
                SqlValue.Text(exclusion.reason().name),
                SqlValue.Text(exclusion.excludedAt().toString()),
            ),
        )
    }

    override fun restoreExercise(exerciseId: String) {
        Exclusion(
            exerciseId,
            Reason.PREFERENCE,
            org.racedaydreams.domain.time.Instants.ofEpochSecond(0),
        )
        database.execute(
            "DELETE FROM strength_exclusions WHERE exercise_id=?",
            listOf(SqlValue.Text(exerciseId)),
        )
    }
}
