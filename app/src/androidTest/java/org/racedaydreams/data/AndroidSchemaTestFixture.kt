package org.racedaydreams.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider

/**
 * Initialise an isolated in-memory Android fixture through the actual Android schema owner.
 * SQLiteDatabase.create() adds android_metadata: it is not an empty portable SQLite connection.
 * Never relax ApplicationSchema's rejection of an unversioned existing database to hide that.
 */
internal fun createAndroidSchemaFixture(database: SQLiteDatabase) {
    require(database.path == ":memory:" && database.version == 0) {
        "Only a new isolated in-memory Android test database may be initialised."
    }
    val helper = ProfileDatabase(ApplicationProvider.getApplicationContext())
    try {
        helper.onConfigure(database)
        database.beginTransaction()
        try {
            helper.onCreate(database)
            database.version = ProfileDatabase.VERSION
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    } finally {
        // This helper never opens its own named database or replaces the shared application pool.
        helper.close()
    }
}
