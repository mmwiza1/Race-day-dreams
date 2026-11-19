package org.racedaydreams.storage

import org.racedaydreams.domain.time.Instants
import platform.Foundation.NSDate
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970

/** Opens the real app schema with a real new-install identity, closing on failed migration. */
object AppleApplicationDatabase {
    fun open(path: String): AppleSqliteDatabase {
        val database = AppleSqliteDatabase(path)
        try {
            ApplicationSchema.open(database) {
                ApplicationSchema.NewInstallation(
                    Instants.ofEpochSecond(NSDate().timeIntervalSince1970.toLong()).toString(),
                    NSUUID().UUIDString,
                )
            }
            // Match Android's existing WAL policy. In-memory test databases return "memory".
            database.query("PRAGMA journal_mode=WAL")
            return database
        } catch (error: Throwable) {
            try {
                database.close()
            } catch (close: Throwable) {
                error.addSuppressed(close)
            }
            throw error
        }
    }
}
