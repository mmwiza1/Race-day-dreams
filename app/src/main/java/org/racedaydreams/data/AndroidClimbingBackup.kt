package org.racedaydreams.data

import android.database.sqlite.SQLiteDatabase
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject
import org.racedaydreams.storage.ClimbingArchiveJson
import org.racedaydreams.storage.SqliteClimbing

/**
 * Lets the Android backup repository use the SAME shared bouldering codec and storage as iOS, so
 * the "climbing" section (root 14+) is identical on both platforms. Runs inside the caller's open
 * transaction on the same database.
 */
object AndroidClimbingBackup {
    private fun climbing(db: SQLiteDatabase) = SqliteClimbing(AndroidSqlConnection { db })

    @JvmStatic
    fun exportData(db: SQLiteDatabase): JSONObject =
        JSONObject(ClimbingArchiveJson.element(climbing(db).snapshot()).toString())

    /** Full validation before any database change; a missing (older) section restores empty. */
    @JvmStatic
    fun validate(section: JSONObject?): ClimbingArchiveJson.Snapshot =
        ClimbingArchiveJson.decode(section?.let { Json.parseToJsonElement(it.toString()).jsonObject })

    @JvmStatic fun hasUnsavedSession(db: SQLiteDatabase): Boolean = climbing(db).hasDraft()

    @JvmStatic
    fun restore(db: SQLiteDatabase, snapshot: ClimbingArchiveJson.Snapshot) =
        climbing(db).replaceFromBackup(snapshot)
}
