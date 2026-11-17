package org.racedaydreams.storage

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.recording.RunMetricsEngine.*
import org.racedaydreams.domain.storage.DetailRetention
import org.racedaydreams.domain.time.*

/** Exercises the actual application owner and platform SQLite/operation lock together. */
abstract class DataLifecycleRepositoryContract {
    abstract fun database(): SqlDatabase

    abstract fun directory(): String

    private val now = StoredInstant.parse("2026-11-17T09:00:00Z")
    private var identities = 0

    private fun owner(db: SqlDatabase) =
        SqliteDataLifecycle(
            db,
            directory(),
            { "lifecycle-epoch-${++identities}" },
            { "lifecycle-choice-${++identities}" },
            { 1234L },
        )

    private fun storage(block: (SqlDatabase, SqliteDataLifecycle) -> Unit) {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(now.toString(), "lifecycle-initial")
            }
            SqliteSettings(db).putSetting("retained", "original")
            val start = StoredInstant.parse("2020-01-01T09:00:00Z")
            val end = StoredInstant.parse("2020-01-01T09:01:00Z")
            SqliteActivities(db) { now }
                .upsertManualActivity(
                    ActivityRecord(
                        "activity",
                        SourceIdentity("rdd", "run", null),
                        start,
                        end,
                        null,
                        null,
                        100.0,
                        Durations.ofSeconds(60),
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Original retained summary",
                        emptyList(),
                        emptyList(),
                        null,
                        null,
                    )
                )
            db.execute(
                "INSERT INTO recording_sessions VALUES('run','FINISHED',?,0,60000,'activity')",
                listOf(SqlValue.Text(start.toString())),
            )
            val point =
                Point(
                    Fix(start, 0, 51.5, -.12, 3.0, null, null, null),
                    0.0,
                    0,
                    0,
                    false,
                    null,
                    null,
                )
            db.execute(
                "INSERT INTO recording_points VALUES('run',0,?)",
                listOf(SqlValue.Text(RecordingPointJson.encode(point))),
            )
            block(db, owner(db))
        } finally {
            db.close()
        }
    }

    private fun copied(source: BackupArchiveSource): BackupStagingFile {
        val file = BackupStagingFile.create(directory(), source.size)
        try {
            var at = 0L
            while (at < source.size) {
                val bytes = source.read(at, minOf(32768L, source.size - at).toInt())
                require(bytes.isNotEmpty())
                file.append(bytes)
                at += bytes.size
            }
            file.seal()
            return file
        } catch (error: Throwable) {
            file.close()
            throw error
        }
    }

    @Test
    fun informedReviewUsesItsOwnExclusiveScopeWithoutDeletingEvidence() = storage { db, lifecycle ->
        lifecycle.acknowledgeRetention(DetailRetention.Policy.LAST_24_MONTHS, now)
        val preview = lifecycle.previewCleanup(DetailRetention.Policy.LAST_24_MONTHS, now)
        lifecycle.approveCleanup(preview, now)
        assertEquals(
            DetailRetention.Policy.LAST_24_MONTHS,
            lifecycle.retentionPreferences().policy(),
        )
        assertEquals(1234L, lifecycle.inventory().usage.databaseFileBytes)
        assertEquals("original", SqliteSettings(db).getSetting("retained"))
        assertTrue(db.query("SELECT * FROM recording_detail_removals").isEmpty())
        assertEquals(
            1L,
            db.query("SELECT COUNT(*) FROM recording_points").single().single().sqlLong(),
        )
    }

    @Test
    fun applicationBackupScopeRejectsNestedMutationsAndRestorationReleasesTheGuard() =
        storage { db, lifecycle ->
            var saved: ByteArray? = null
            lifecycle.withApplicationBackup(now) { bytes ->
                saved = bytes
                assertFails { lifecycle.restoreApplication(bytes) }
                assertFails { lifecycle.inventory() }
                assertFails { lifecycle.retentionPreferences() }
                assertFails {
                    lifecycle.acknowledgeRetention(DetailRetention.Policy.KEEP_FOREVER, now)
                }
                assertFails { lifecycle.previewCleanup(DetailRetention.Policy.LAST_24_MONTHS, now) }
                assertFails { lifecycle.withCompleteBackup(now) { error("Must not export") } }
                assertEquals("original", SqliteSettings(db).getSetting("retained"))
            }
            SqliteSettings(db).putSetting("retained", "changed")
            lifecycle.restoreApplication(requireNotNull(saved))
            assertEquals("original", SqliteSettings(db).getSetting("retained"))
            lifecycle.withApplicationBackup(now) { assertTrue(it.isNotEmpty()) }
        }

    @Test
    fun completeBackupCopiesUnderGuardAndClosesItsPrivateSourceBeforeReturn() =
        storage { db, lifecycle ->
            var escaped: BackupArchiveSource? = null
            var saved: BackupStagingFile? = null
            lifecycle.withCompleteBackup(now) { source ->
                escaped = source
                saved = copied(source)
                assertFails { lifecycle.restoreCompleteBackup(source) }
                assertFails { lifecycle.withRecordingArchive { error("Must not export") } }
            }
            assertFails { requireNotNull(escaped).read(0, 1) }
            try {
                storage { target, targetOwner ->
                    SqliteSettings(target).putSetting("retained", "different")
                    targetOwner.restoreCompleteBackup(requireNotNull(saved))
                    assertEquals("original", SqliteSettings(target).getSetting("retained"))
                    assertNotEquals(
                        db.query("SELECT epoch FROM detail_retention_state"),
                        target.query("SELECT epoch FROM detail_retention_state"),
                    )
                }
            } finally {
                saved?.close()
            }
        }

    @Test
    fun failedCompleteBackupConsumerClosesSourceAndDoesNotLeaveOwnerBusy() =
        storage { _, lifecycle ->
            var escaped: BackupArchiveSource? = null
            assertFailsWith<IllegalStateException> {
                lifecycle.withCompleteBackup(now) { source ->
                    escaped = source
                    error("Copy failed")
                }
            }
            assertFails { requireNotNull(escaped).read(0, 1) }
            lifecycle.withCompleteBackup(now) { assertTrue(it.size > 0) }
        }

    @Test
    fun recordingArchiveCallbackClosesSourceAndMergeUsesTheSameOwner() = storage { _, lifecycle ->
        var escaped: BackupArchiveSource? = null
        var saved: BackupStagingFile? = null
        lifecycle.withRecordingArchive { source ->
            escaped = source
            saved = copied(source)
            assertFails { lifecycle.restoreRecordingArchive(source) }
        }
        assertFails { requireNotNull(escaped).read(0, 1) }
        try {
            lifecycle.restoreRecordingArchive(requireNotNull(saved))
        } finally {
            saved?.close()
        }
        lifecycle.withRecordingArchive { assertTrue(it.size > 0) }
    }

    @Test
    fun invalidRestoreLeavesApplicationUntouchedAndOwnerCanBeUsedAgain() =
        storage { db, lifecycle ->
            val before = db.query("SELECT * FROM detail_retention_state")
            assertFails { lifecycle.restoreApplication("not a backup".encodeToByteArray()) }
            val invalid =
                object : BackupArchiveSource {
                    override val size = 0L

                    override fun read(offset: Long, maximum: Int) = ByteArray(0)
                }
            assertFails { lifecycle.restoreCompleteBackup(invalid) }
            assertFails { lifecycle.restoreRecordingArchive(invalid) }
            assertEquals(before, db.query("SELECT * FROM detail_retention_state"))
            assertEquals("original", SqliteSettings(db).getSetting("retained"))
            lifecycle.withApplicationBackup(now) { assertTrue(it.isNotEmpty()) }
        }

    @Test
    fun approvedCleanupCannotOverlapExportAndRestoredIdentityInvalidatesApproval() =
        storage { db, lifecycle ->
            lifecycle.acknowledgeRetention(DetailRetention.Policy.LAST_24_MONTHS, now)
            val approval =
                lifecycle.approveCleanup(
                    lifecycle.previewCleanup(DetailRetention.Policy.LAST_24_MONTHS, now),
                    now,
                )
            var parent: ByteArray? = null
            lifecycle.withApplicationBackup(now) { bytes ->
                parent = bytes
                assertFails { lifecycle.cleanOne(approval, "run", now) }
            }
            assertEquals(
                1L,
                db.query("SELECT COUNT(*) FROM recording_points").single().single().sqlLong(),
            )
            lifecycle.restoreApplication(requireNotNull(parent))
            assertFails { lifecycle.cleanOne(approval, "run", now) }
            assertEquals(
                1L,
                db.query("SELECT COUNT(*) FROM recording_points").single().single().sqlLong(),
            )
            assertTrue(db.query("SELECT * FROM recording_detail_removals").isEmpty())
        }

    @Test
    fun constructionAndInvalidImportValidationNeverAccessTheDatabase() {
        val untouched =
            object : SqlDatabase {
                override fun execute(sql: String, arguments: List<SqlValue>) {
                    error("Must validate before execute")
                }

                override fun query(sql: String, arguments: List<SqlValue>): List<List<SqlValue>> =
                    error("Must validate before query")

                override fun <T> transaction(block: () -> T): T =
                    error("Must validate before transaction")

                override fun close() {
                    error("Owner does not own database lifetime")
                }
            }
        val lifecycle = owner(untouched)
        assertFailsWith<IllegalArgumentException> {
            lifecycle.restoreApplication("bad".encodeToByteArray())
        }
        val invalid =
            object : BackupArchiveSource {
                override val size = 0L

                override fun read(offset: Long, maximum: Int) = ByteArray(0)
            }
        assertFailsWith<IllegalArgumentException> { lifecycle.restoreRecordingArchive(invalid) }
        assertFailsWith<IllegalArgumentException> { lifecycle.restoreCompleteBackup(invalid) }
    }
}
