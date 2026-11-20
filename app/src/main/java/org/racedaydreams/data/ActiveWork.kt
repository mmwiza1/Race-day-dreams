package org.racedaydreams.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase

/**
 * Work that an app update must never interrupt: the same in-progress states that already block a
 * backup restore. Installing replaces the app process, so it waits until these are finished or
 * saved.
 */
object ActiveWork {
    /** A plain explanation of what is in progress, or null when an update may proceed. */
    @JvmStatic
    fun describe(context: Context): String? = describe(ProfileDatabase.get(context).readableDatabase)

    @JvmStatic
    fun describe(db: SQLiteDatabase): String? {
        db.rawQuery(
                "SELECT 1 FROM recording_sessions WHERE state IN ('ACTIVE','PAUSED','RECOVERABLE') LIMIT 1",
                null,
            )
            .use { if (it.moveToFirst()) return "A run is being recorded or needs recovery." }
        if (SqliteGymExecutionRepository.hasUnfinished(db)) return "A gym workout is in progress."
        if (AndroidClimbingBackup.hasUnsavedSession(db))
            return "A bouldering session hasn't been saved yet."
        return null
    }
}
