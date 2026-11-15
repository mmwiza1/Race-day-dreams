package org.racedaydreams.storage

import kotlin.test.*
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID

class AppleStorageTest : StorageContract() {
    override fun database(): SqlDatabase = AppleSqliteDatabase(":memory:")

    @Test
    fun actualNativeFileSurvivesCloseAndReopen() {
        val path = NSTemporaryDirectory() + NSUUID().UUIDString + ".db"
        try {
            val first = AppleSqliteDatabase(path)
            ApplicationSchema.open(first) { choice() }
            SqliteSettings(first).putSetting("persisted", "actual native evidence")
            first.close()
            val reopened = AppleSqliteDatabase(path)
            try {
                ApplicationSchema.open(reopened) { error("Existing database") }
                assertEquals(
                    "actual native evidence",
                    SqliteSettings(reopened).getSetting("persisted"),
                )
            } finally {
                reopened.close()
            }
        } finally {
            NSFileManager.defaultManager.removeItemAtPath(path, null)
        }
    }

    @Test
    fun closedConnectionRejectsReadsAndCloseIsIdempotent() {
        val db = database()
        db.close()
        db.close()
        assertFailsWith<IllegalStateException> { db.query("SELECT 1") }
    }

    @Test
    fun productionFactoryUsesActualIdentityAndProtectsExistingChoice() {
        val path = NSTemporaryDirectory() + NSUUID().UUIDString + ".db"
        try {
            val first = AppleApplicationDatabase.open(path)
            val original = SqliteRetentionPreferences(first).load()
            assertNotNull(original.chosenAt())
            assertFalse(original.choiceId().isNullOrBlank())
            first.close()
            val second = AppleApplicationDatabase.open(path)
            try {
                assertEquals(original, SqliteRetentionPreferences(second).load())
            } finally {
                second.close()
            }
        } finally {
            for (suffix in listOf("", "-wal", "-shm")) NSFileManager.defaultManager
                .removeItemAtPath(path + suffix, null)
        }
    }
}
