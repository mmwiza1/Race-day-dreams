package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.achievements.TrainingMilestones.Award
import org.racedaydreams.domain.achievements.TrainingMilestones.Kind
import org.racedaydreams.domain.achievements.TrainingMilestones.Proof
import org.racedaydreams.domain.achievements.TrainingMilestones.Source
import org.racedaydreams.domain.body.*
import org.racedaydreams.domain.body.BodyMeasurementsRepository.Preferences
import org.racedaydreams.domain.time.*

abstract class BodyAwardsBackupRestoreRepositoryContract {
    abstract fun database(): SqlDatabase

    private val at = GymEngineArchiveFixtures.at

    private fun storage(block: (SqlDatabase, SqlDatabase) -> Unit) {
        val source = database()
        val target = database()
        try {
            for (db in listOf(source, target)) ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "body-awards-restore-test")
            }
            block(source, target)
        } finally {
            source.close()
            target.close()
        }
    }

    private fun seed(db: SqlDatabase): UserBackupDataJson.Snapshot {
        val heights = SqliteBodyHeights(db)
        heights.saveHeight(
            BodyHeight("manual-height", 1.83, at, null, "manual", "manual-entry", null, null)
        )
        heights.saveHeight(
            BodyHeight(
                "provider-height",
                1.825,
                at,
                Offsets.ofTotalSeconds(19815),
                "health-connect",
                "provider-entry",
                "source.app",
                Instants.ofEpochSecond(at.getEpochSecond() + 60),
            )
        )
        heights.preferences(
            Preferences(
                true,
                HeightUnit.INCHES,
                ProteinGuidance.Basis("retained-weight", at, 87.25),
            )
        )
        val milestones = SqliteTrainingMilestones(db)
        milestones.recognize(
            listOf(
                Award(
                    Kind.FIRST_RUN,
                    at,
                    listOf(Proof(Source.RUN, "historic-run", at, null, 5000.25)),
                )
            ),
            Instants.ofEpochSecond(at.getEpochSecond() + 120),
        )
        milestones.recognize(
            listOf(
                Award(
                    Kind.FIRST_GYM,
                    at,
                    listOf(
                        Proof(Source.GYM, "historic-gym", at, Offsets.ofTotalSeconds(19815), null)
                    ),
                )
            ),
            Instants.ofEpochSecond(at.getEpochSecond() + 3600),
        )
        milestones.celebrations(false)
        return SqliteApplicationBackup(db).snapshot()
    }

    private fun check(snapshot: UserBackupDataJson.Snapshot, target: SqlDatabase) {
        val restored = SqliteApplicationBackup(target).snapshot()
        assertEquals(
            BodyMeasurementsArchiveJson.element(snapshot.body),
            BodyMeasurementsArchiveJson.element(restored.body),
        )
        assertEquals(
            AchievementsArchiveJson.element(snapshot.achievements),
            AchievementsArchiveJson.element(restored.achievements),
        )
        assertNull(restored.core.profile)
        assertTrue(restored.core.activities.isEmpty())
        assertTrue(restored.coaching!!.sessions.isEmpty())
    }

    @Test
    fun originalMeasurementsSourcesReferencesAndPerAwardRecognitionDatesAreRetainedWithoutNewActuals() =
        storage { source, target ->
            val snapshot = seed(source)
            val retention = SqliteRecordingDetailReceipts(target).snapshot()
            SqliteBodyAwardsBackupRestore(target).populate(snapshot)
            check(snapshot, target)
            val heights = SqliteBodyHeights(target).heights(500, 0)
            assertNull(heights.first { it.id() == "manual-height" }.offset())
            assertEquals(
                19815,
                heights.first { it.id() == "provider-height" }.offset()!!.getTotalSeconds(),
            )
            assertEquals(87.25, SqliteBodyHeights(target).preferences().basis()!!.kilograms())
            assertEquals(
                snapshot.achievements.awards.map { it.recognizedAt },
                SqliteTrainingMilestones(target).awards().map { it.recognizedAt },
            )
            assertFalse(SqliteTrainingMilestones(target).celebrations())
            assertEquals(retention.preferences, SqliteRetentionPreferences(target).load())
            assertTrue(target.query("SELECT * FROM coach_evidence").isEmpty())
            assertTrue(target.query("PRAGMA foreign_key_check").isEmpty())
        }

    @Test
    fun missingOldSectionsDoNotEnableGuidanceOrCreateMeasurementsOrMilestones() =
        storage { source, target ->
            val current = SqliteApplicationBackup(source).snapshot()
            val old =
                UserBackupDataJson.decode(
                    JsonObject(
                        UserBackupDataJson.element(current) - "bodyMeasurements" - "achievements"
                    )
                )
            SqliteBodyAwardsBackupRestore(target).populate(old)
            check(current, target)
            assertFalse(SqliteBodyHeights(target).preferences().proteinEnabled())
            assertNull(SqliteBodyHeights(target).preferences().basis())
            assertTrue(SqliteTrainingMilestones(target).awards().isEmpty())
        }

    @Test
    fun occupiedLatePreferenceTablePreventsAllEarlierBodyAndAwardWrites() =
        storage { source, target ->
            val snapshot = seed(source)
            target.execute("INSERT INTO training_milestone_preferences VALUES(1,1)")
            val before = target.query("SELECT total_changes()")
            assertFails { SqliteBodyAwardsBackupRestore(target).populate(snapshot) }
            assertEquals(before, target.query("SELECT total_changes()"))
            assertTrue(SqliteBodyHeights(target).heights(500, 0).isEmpty())
            assertTrue(SqliteTrainingMilestones(target).awards().isEmpty())
            assertTrue(SqliteTrainingMilestones(target).celebrations())
        }

    @Test
    fun lateFailureAndCallerRollbackCannotRecreateAwardsOrLeavePartialBodyHistory() =
        storage { source, target ->
            val snapshot = seed(source)
            val failed =
                object : SqlDatabase by target {
                    override fun execute(sql: String, arguments: List<SqlValue>) {
                        if (sql.startsWith("INSERT OR REPLACE INTO training_milestone_preferences"))
                            error("Simulated last preference failure")
                        target.execute(sql, arguments)
                    }
                }
            assertFails { SqliteBodyAwardsBackupRestore(failed).populate(snapshot) }
            assertTrue(SqliteBodyHeights(target).heights(500, 0).isEmpty())
            assertTrue(SqliteTrainingMilestones(target).awards().isEmpty())
            assertFalse(SqliteBodyHeights(target).preferences().proteinEnabled())
            assertFails {
                target.transaction {
                    SqliteBodyAwardsBackupRestore(target).populate(snapshot)
                    check(snapshot, target)
                    error("Reject enclosing application replacement")
                }
            }
            assertTrue(SqliteBodyHeights(target).heights(500, 0).isEmpty())
            assertTrue(SqliteTrainingMilestones(target).awards().isEmpty())
            SqliteBodyAwardsBackupRestore(target).populate(snapshot)
            check(snapshot, target)
            val before = target.query("SELECT total_changes()")
            assertFails { SqliteBodyAwardsBackupRestore(target).populate(snapshot) }
            assertEquals(before, target.query("SELECT total_changes()"))
        }
}
