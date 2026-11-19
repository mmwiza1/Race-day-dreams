package org.racedaydreams.storage

import kotlin.test.*
import org.racedaydreams.domain.body.*
import org.racedaydreams.domain.body.BodyMeasurementsRepository.Preferences
import org.racedaydreams.domain.time.*

/** One real SQLite body adapter on JDBC and Apple; SDK visibility is never invented. */
abstract class BodyMeasurementsRepositoryContract {
    abstract fun database(): SqlDatabase

    private val at = StoredInstant.parse("2026-11-17T12:00:00.123456789Z")
    private val from = StoredInstant.parse("2026-11-17T00:00:00Z")
    private val until = StoredInstant.parse("2026-11-18T00:00:00Z")

    private fun height(id: String, provider: String = "manual") =
        BodyHeight(
            id,
            1.82,
            at,
            Offsets.ofTotalSeconds(50400),
            provider,
            id,
            null,
            if (provider == "manual") null else at,
        )

    private fun weight(id: String, provider: String = "manual") =
        BodyWeight(id, 87.25, at, Offsets.ofTotalSeconds(50400), provider, id, null)

    private fun repository(db: SqlDatabase) =
        SqliteBodyMeasurements(db, { at }) {
            error("Reading or manually saving supplied evidence must not invent another identity.")
        }

    private fun use(block: (SqlDatabase, SqliteBodyMeasurements) -> Unit) {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "body-repository")
            }
            block(db, repository(db))
        } finally {
            db.close()
        }
    }

    @Test
    fun manualEvidenceAndExplicitProteinBasisShareOriginalTablesAndRollback() = use { db, body ->
        val h = height("manual-height:original")
        val w = weight("manual-weight:original")
        body.saveHeight(h)
        body.saveWeight(w)
        assertEquals(Preferences.defaults(), body.preferences())
        val chosen =
            Preferences(
                true,
                HeightUnit.INCHES,
                ProteinGuidance.Basis(w.id(), w.at(), w.kilograms()),
            )
        body.preferences(chosen)
        assertEquals(listOf(h), SqliteBodyHeights(db).heights(20, 0))
        assertEquals(listOf(w), SqliteCoachingContext(db, { at }, { error("No new ID") }).weights())
        assertFails {
            db.transaction {
                body.saveHeight(height("manual-height:uncommitted"))
                body.saveWeight(weight("manual-weight:uncommitted"))
                body.preferences(Preferences.defaults())
                error("Abort the owning application transaction")
            }
        }
        val reopened = repository(db)
        assertEquals(listOf(h), reopened.heights(20, 0))
        assertEquals(listOf(w), reopened.weights())
        assertEquals(chosen, reopened.preferences())
        assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
    }

    @Test
    fun visibleAppleImportsNeverBecomeAuthoritativeReplacementOrImplicitWeightChoice() =
        use { db, body ->
            val retainedHeight =
                BodyHeight("hk-height:one", 1.82, at, null, "health-kit", "height-one", "scale", at)
            val retainedWeight =
                BodyWeight(
                    "hk-weight:one",
                    87.25,
                    at,
                    Offsets.ofTotalSeconds(0),
                    "health-kit",
                    "weight-one",
                    "scale",
                )
            val imported = SqliteHealthKitBodyImport(db, { at }, { error("No decision identity") })
            imported.merge(listOf(retainedHeight), listOf(retainedWeight))
            assertEquals(listOf(retainedHeight), body.heights(20, 0))
            assertEquals(listOf(retainedWeight), body.weights())
            assertNull(body.preferences().basis())
            assertFalse(body.preferences().proteinEnabled())
            assertFailsWith<IllegalArgumentException> {
                body.replaceImportedWindow("health-kit", from, until, emptyList(), emptyList())
            }
            imported.merge(emptyList(), emptyList())
            assertEquals(listOf(retainedHeight), body.heights(20, 0))
            assertEquals(listOf(retainedWeight), body.weights())
            assertNull(body.preferences().basis())
        }

    @Test
    fun authoritativeWindowValidatesScopeAndRollsBackHeightWhenWeightWriteFails() =
        use { db, body ->
            val manual = height("manual-height:keep")
            val oldHeight = height("hc-height:old", "health-connect")
            val oldWeight = weight("hc-weight:old", "health-connect")
            body.saveHeight(manual)
            body.saveHeight(oldHeight)
            body.saveWeight(oldWeight)
            val originalHeights = body.heights(20, 0)
            val newHeight = height("hc-height:new", "health-connect")
            val newWeight = weight("hc-weight:fail", "health-connect")
            assertFailsWith<IllegalArgumentException> {
                body.replaceImportedWindow(
                    "health-connect",
                    from,
                    until,
                    listOf(newHeight),
                    listOf(newWeight, newWeight),
                )
            }
            assertEquals(originalHeights, body.heights(20, 0))
            db.execute(
                "CREATE TRIGGER body_write_failure BEFORE INSERT ON coach_evidence WHEN NEW.kind='WEIGHT' AND NEW.id='hc-weight:fail' BEGIN SELECT RAISE(ABORT,'injected body failure'); END"
            )
            assertFails {
                body.replaceImportedWindow(
                    "health-connect",
                    from,
                    until,
                    listOf(newHeight),
                    listOf(newWeight),
                )
            }
            assertEquals(originalHeights, body.heights(20, 0))
            assertEquals(listOf(oldWeight), body.weights())
            db.execute("DROP TRIGGER body_write_failure")
            body.replaceImportedWindow("health-connect", from, until, listOf(newHeight), null)
            assertEquals(setOf(manual, newHeight), body.heights(20, 0).toSet())
            assertEquals(listOf(oldWeight), body.weights())
            body.replaceImportedWindow("health-connect", from, until, null, emptyList())
            assertTrue(body.weights().isEmpty())
            assertEquals(setOf(manual, newHeight), body.heights(20, 0).toSet())
        }
}
