package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.datetime.Instant as HealthInstant
import org.racedaydreams.domain.body.*
import org.racedaydreams.domain.health.HealthImportPolicy
import org.racedaydreams.domain.time.*
import org.racedaydreams.health.body.BodyReadWindow
import org.racedaydreams.health.workouts.HealthKitRun
import org.racedaydreams.health.workouts.HealthKitRunningImport

/** Same real JDBC/Apple SQLite import and rollback contracts, not fabricated SDK permission. */
abstract class HealthKitBodyRepositoryContract {
    abstract fun database(): SqlDatabase

    private val observed = StoredInstant.parse("2026-11-16T08:30:01.123456789Z")
    private val imported = StoredInstant.parse("2026-11-17T09:00:00.123456789Z")

    private fun height(id: String = "hk-height:sample", source: String = "height-source") =
        BodyHeight(id, 1.83, observed, null, "health-kit", source, "watch.source", imported)

    private fun weight(id: String = "hk-weight:sample", source: String = "weight-source") =
        BodyWeight(
            id,
            87.25,
            observed,
            Offsets.ofTotalSeconds(0),
            "health-kit",
            source,
            "scale.source",
        )

    private fun storage(
        block:
            (
                SqlDatabase, SqliteHealthKitBodyImport, SqliteCoachingContext, SqliteBodyHeights,
            ) -> Unit
    ) {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(imported.toString(), "body-import")
            }
            val ids = {
                error("Body reads must not create athlete decisions or random identities.")
            }
            val context = SqliteCoachingContext(db, { imported }, ids)
            block(
                db,
                SqliteHealthKitBodyImport(db, { imported }, ids),
                context,
                SqliteBodyHeights(db),
            )
        } finally {
            db.close()
        }
    }

    private fun rows(db: SqlDatabase) =
        listOf(
            db.query("SELECT * FROM body_heights ORDER BY id"),
            db.query("SELECT * FROM coach_evidence ORDER BY kind,id"),
            db.query("SELECT * FROM body_guidance_preferences ORDER BY id"),
            db.query("SELECT * FROM activities ORDER BY id"),
            db.query("SELECT * FROM import_checkpoints ORDER BY provider"),
        )

    @Test
    fun visibleMeasurementsRetainSourceNanosAndNeverChooseAProteinBasisOrCreateWorkouts() =
        storage { db, merge, context, heights ->
            val manual =
                BodyWeight(
                    "manual",
                    70.0,
                    observed,
                    Offsets.ofTotalSeconds(3600),
                    "manual",
                    "manual",
                    null,
                )
            context.saveWeight(manual)
            assertEquals(
                SqliteHealthKitBodyImport.Result(1, 1),
                merge.merge(listOf(height()), listOf(weight())),
            )
            assertEquals(listOf(height()), heights.heights(500, 0))
            assertEquals(setOf(manual, weight()), context.weights().toSet())
            assertNull(heights.preferences().basis())
            assertFalse(heights.preferences().proteinEnabled())
            assertEquals(
                123456789,
                context.weights().single { it.provider() == "health-kit" }.at().getNano(),
            )
            assertTrue(db.query("SELECT id FROM activities").isEmpty())
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
            assertTrue(db.query("PRAGMA foreign_key_check").isEmpty())
        }

    @Test
    fun repeatedOrEmptyReadsPreserveHistoricalIdentityMetadataAndExactStoredBytes() =
        storage { db, merge, context, heights ->
            val oldHeight =
                height("old-backup-height")
                    .copy(offset = Offsets.ofTotalSeconds(19815), importedAt = null)
            val oldWeight = weight("old-backup-weight").copy(offset = Offsets.ofTotalSeconds(19815))
            heights.saveHeight(oldHeight)
            context.saveWeight(oldWeight)
            val basis = ProteinGuidance.Basis(oldWeight.id(), oldWeight.at(), oldWeight.kilograms())
            heights.preferences(
                BodyMeasurementsRepository.Preferences(true, HeightUnit.CENTIMETRES, basis)
            )
            val before = rows(db)
            val changes = db.query("SELECT total_changes()")
            assertEquals(
                SqliteHealthKitBodyImport.Result(0, 0),
                merge.merge(listOf(height()), listOf(weight())),
            )
            assertEquals(
                SqliteHealthKitBodyImport.Result(0, 0),
                merge.merge(emptyList(), emptyList()),
            )
            assertEquals(before, rows(db))
            assertEquals(changes, db.query("SELECT total_changes()"))
            assertEquals(oldHeight, heights.heights(500, 0).single())
            assertEquals(oldWeight, context.weights().single())
            assertEquals(basis, heights.preferences().basis())
        }

    @Test
    fun changedImmutableSamplesOrManualIdentityCollisionsRejectBeforeAnyWrite() =
        storage { db, merge, context, _ ->
            merge.merge(listOf(height()), listOf(weight()))
            context.saveWeight(weight("manual-id", "manual-source").copy(provider = "manual"))
            val before = rows(db)
            val changes = db.query("SELECT total_changes()")
            for (changed in
                listOf(
                    weight().copy(kilograms = 88.0),
                    weight().copy(at = imported),
                    weight().copy(origin = "different.source"),
                )) {
                assertFails {
                    merge.merge(listOf(height("new-height", "new-height-source")), listOf(changed))
                }
            }
            assertFails { merge.merge(listOf(height().copy(metres = 1.84)), emptyList()) }
            assertFails { merge.merge(emptyList(), listOf(weight("manual-id", "new-source"))) }
            assertEquals(before, rows(db))
            assertEquals(changes, db.query("SELECT total_changes()"))
        }

    @Test
    fun duplicateVisibleObservationsAreIdempotentAndConflictingOrUnboundedReadsAreRejected() =
        storage { db, merge, _, _ ->
            assertEquals(
                SqliteHealthKitBodyImport.Result(1, 1),
                merge.merge(listOf(height(), height()), listOf(weight(), weight())),
            )
            val before = rows(db)
            assertFails { merge.merge(listOf(height(), height().copy(metres = 1.84)), emptyList()) }
            assertFails {
                merge.merge(emptyList(), listOf(weight(), weight().copy(kilograms = 88.0)))
            }
            assertFails {
                merge.merge(emptyList(), listOf(weight("same", "a"), weight("same", "b")))
            }
            assertFails { merge.merge(List(10001) { height() }, emptyList()) }
            assertFails {
                merge.merge(emptyList(), listOf(weight().copy(provider = "health-connect")))
            }
            assertEquals(before, rows(db))
        }

    @Test
    fun bodyFailureRollsBackWorkoutsAndCheckpointInTheSameOuterApplicationTransaction() =
        storage { db, merge, _, _ ->
            val activities = SqliteActivities(db) { imported }
            activities.applyImport("health-kit", emptyList(), emptyList(), "retained-anchor")
            val before = rows(db)
            db.execute(
                "CREATE TRIGGER reject_body_weight BEFORE INSERT ON coach_evidence WHEN NEW.kind='WEIGHT' BEGIN SELECT RAISE(ABORT,'Reject staged weight'); END"
            )
            assertFails {
                db.transaction {
                    val window =
                        BodyReadWindow(
                            HealthInstant.parse("2026-11-01T00:00:00Z"),
                            HealthInstant.parse(imported.toString()),
                        )
                    val run =
                        HealthKitRun(
                            "workout",
                            "watch.source",
                            HealthInstant.parse("2026-11-16T09:00:00Z"),
                            HealthInstant.parse("2026-11-16T09:30:00Z"),
                            5000.0,
                        )
                    val batch =
                        HealthKitRunningImport.batch(
                            window,
                            "own.app",
                            listOf(run),
                            activities.activities(),
                            activities.importToken("health-kit"),
                        )
                    HealthImportPolicy.commit(activities, batch)
                    merge.merge(listOf(height()), listOf(weight()))
                }
            }
            assertEquals(before, rows(db))
            assertTrue(activities.activities().isEmpty())
            assertEquals("retained-anchor", activities.importToken("health-kit"))
        }

    @Test
    fun bodyMergeJoinsCallerRollbackAndUnselectedWeightHistoryIsNotHydrated() =
        storage { db, merge, _, _ ->
            val before = rows(db)
            assertFails {
                db.transaction {
                    merge.merge(listOf(height()), listOf(weight()))
                    error("Caller rejects selection")
                }
            }
            assertEquals(before, rows(db))
            db.execute(
                "INSERT INTO coach_evidence(kind,id,payload) VALUES('WEIGHT','unrelated-corrupt','not-json')"
            )
            assertEquals(
                SqliteHealthKitBodyImport.Result(1, 0),
                merge.merge(listOf(height()), emptyList()),
            )
            assertEquals(
                SqliteHealthKitBodyImport.Result(0, 0),
                merge.merge(emptyList(), emptyList()),
            )
            assertEquals(
                "not-json",
                db.query("SELECT payload FROM coach_evidence WHERE id='unrelated-corrupt'")
                    .single()
                    .single()
                    .sqlText(),
            )
        }

    @Test
    fun missingOlderSourceMetadataRemainsUnknownRatherThanBlockingOrRewritingCompatibleEvidence() =
        storage { db, merge, context, heights ->
            val oldHeight = height("old-height").copy(origin = null, importedAt = null)
            val oldWeight = weight("old-weight").copy(origin = null)
            heights.saveHeight(oldHeight)
            context.saveWeight(oldWeight)
            val before = rows(db)
            assertEquals(
                SqliteHealthKitBodyImport.Result(0, 0),
                merge.merge(listOf(height()), listOf(weight())),
            )
            assertEquals(before, rows(db))
            assertNull(heights.heights(500, 0).single().origin())
            assertNull(context.weights().single().origin())
            merge.merge(
                listOf(height("known-height", "known-height-source")),
                listOf(weight("known-weight", "known-weight-source")),
            )
            val known = rows(db)
            assertEquals(
                SqliteHealthKitBodyImport.Result(0, 0),
                merge.merge(
                    listOf(
                        height("other-generated-height", "known-height-source").copy(origin = null)
                    ),
                    listOf(
                        weight("other-generated-weight", "known-weight-source").copy(origin = null)
                    ),
                ),
            )
            assertEquals(known, rows(db))
            assertEquals(
                "watch.source",
                heights.heights(500, 0).single { it.id() == "known-height" }.origin(),
            )
        }
}
