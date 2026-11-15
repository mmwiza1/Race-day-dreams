@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.cinterop.*
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.posix.*

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

    @Test
    fun corruptDatabaseIsRejectedWithoutReplacingOriginalBytes() {
        val path = NSTemporaryDirectory() + NSUUID().UUIDString + ".db"
        val original = "Existing unreadable data must survive".encodeToByteArray()
        try {
            val output = checkNotNull(fopen(path, "wb"))
            try {
                original.usePinned {
                    assertEquals(
                        original.size.toULong(),
                        fwrite(it.addressOf(0), 1u, original.size.toULong(), output),
                    )
                }
            } finally {
                fclose(output)
            }
            assertFails {
                val db = AppleSqliteDatabase(path)
                try {
                    ApplicationSchema.open(db) { choice() }
                } finally {
                    db.close()
                }
            }
            val input = checkNotNull(fopen(path, "rb"))
            val actual = ByteArray(original.size + 1)
            try {
                actual.usePinned {
                    assertEquals(
                        original.size.toULong(),
                        fread(it.addressOf(0), 1u, actual.size.toULong(), input),
                    )
                }
            } finally {
                fclose(input)
            }
            assertContentEquals(original, actual.copyOf(original.size))
        } finally {
            NSFileManager.defaultManager.removeItemAtPath(path, null)
        }
    }

    @Test
    fun statementTailAndBindingErrorsCannotRunAdditionalSql() {
        val db = AppleApplicationDatabase.open(":memory:")
        try {
            assertFails {
                db.execute("INSERT INTO settings VALUES('first','bad'); DELETE FROM settings")
            }
            assertNull(SqliteSettings(db).getSetting("first"))
            assertFails { db.execute("SELECT 1\u0000; DELETE FROM settings") }
            assertFails {
                db.execute("INSERT INTO settings VALUES(?,?)", listOf(SqlValue.Text("missing")))
            }
            assertFails { db.query("SELECT ?", listOf(SqlValue.Real(Double.NaN))) }
            db.execute("INSERT INTO settings VALUES('ok','retained'); \n\t")
            assertEquals("retained", SqliteSettings(db).getSetting("ok"))
        } finally {
            db.close()
        }
    }
}
