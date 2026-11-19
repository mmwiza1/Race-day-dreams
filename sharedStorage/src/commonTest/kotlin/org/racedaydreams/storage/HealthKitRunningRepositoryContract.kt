package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.datetime.Instant
import org.racedaydreams.domain.health.HealthImportPolicy
import org.racedaydreams.health.body.BodyReadWindow
import org.racedaydreams.health.workouts.HealthKitRun
import org.racedaydreams.health.workouts.HealthKitRunningImport

/** Real imported-source persistence, using the same SQLite/transaction model on JVM and Apple. */
abstract class HealthKitRunningRepositoryContract {
    abstract fun database(): SqlDatabase

    private val now = StoredInstant.parse("2026-11-17T00:00:00Z")
    private val window =
        BodyReadWindow(Instant.parse("2026-11-01T00:00:00Z"), Instant.parse(now.toString()))
    private val observed =
        HealthKitRun(
            "native-workout",
            "org.source.watch",
            Instant.parse("2026-11-16T09:00:00Z"),
            Instant.parse("2026-11-16T09:30:00Z"),
            5000.0,
        )

    private fun storage(block: (SqlDatabase, SqliteActivities) -> Unit) {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(now.toString(), "healthkit-test")
            }
            block(db, SqliteActivities(db) { now })
        } finally {
            db.close()
        }
    }

    private fun import(
        db: SqlDatabase,
        repository: SqliteActivities,
        observations: List<HealthKitRun>,
    ) =
        db.transaction {
            val batch =
                HealthKitRunningImport.batch(
                    window,
                    "org.racedaydreams",
                    observations,
                    repository.activities(),
                    repository.importToken("health-kit"),
                )
            HealthImportPolicy.commit(repository, batch)
        }

    @Test
    fun actualProviderIdentityAndUnknownMeasurementsSurviveSqlAndRepeatedImports() =
        storage { db, repository ->
            import(db, repository, listOf(observed))
            val original = repository.activities().single()
            assertEquals("health-kit", original.source().provider())
            assertEquals("native-workout", original.source().recordId())
            assertEquals("org.source.watch", original.source().originPackage())
            assertNull(original.movingTime())
            assertNull(original.averageHeartRate())
            assertTrue(original.samples().isEmpty())
            import(db, repository, listOf(observed, observed))
            assertEquals(listOf(original), repository.activities())
            assertTrue(db.query("PRAGMA foreign_key_check").isEmpty())
        }

    @Test
    fun emptyVisibleReadAndWithheldDistanceKeepActualHistoryNotesAndRetainedCheckpoint() =
        storage { db, repository ->
            import(db, repository, listOf(observed))
            val id = repository.activities().single().id()
            repository.saveActivityNotes(id, "Athlete-owned notes", 5.0)
            val before = repository.activities()
            repository.applyImport("health-kit", emptyList(), emptyList(), "old-anchor")
            import(db, repository, emptyList())
            assertEquals(before, repository.activities())
            assertEquals("old-anchor", repository.importToken("health-kit"))
            import(db, repository, listOf(observed.copy(distanceMetres = null)))
            assertEquals(before, repository.activities())
            assertEquals("old-anchor", repository.importToken("health-kit"))
        }

    @Test
    fun callerRollbackRetainsHistoryAndCheckpointAfterAStagedHealthImport() =
        storage { db, repository ->
            import(db, repository, listOf(observed))
            val before = repository.activities()
            assertFailsWith<IllegalStateException> {
                db.transaction {
                    import(db, repository, listOf(observed.copy(recordId = "new-workout")))
                    error("Caller rejects import")
                }
            }
            assertEquals(before, repository.activities())
            assertNull(repository.importToken("health-kit"))
        }

    @Test
    fun conflictingSourceIdentityRejectsBeforeAnySqlMutation() = storage { db, repository ->
        import(db, repository, listOf(observed))
        val before = repository.activities()
        val changes = db.query("SELECT total_changes()")
        assertFailsWith<IllegalArgumentException> {
            import(db, repository, listOf(observed.copy(origin = "different.source")))
        }
        assertEquals(before, repository.activities())
        assertEquals(changes, db.query("SELECT total_changes()"))
    }
}
