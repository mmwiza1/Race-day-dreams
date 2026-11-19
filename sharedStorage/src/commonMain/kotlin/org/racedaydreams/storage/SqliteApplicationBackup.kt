package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.achievements.TrainingMilestones
import org.racedaydreams.domain.body.BodyHeight
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.Owner
import org.racedaydreams.domain.time.Instant

/**
 * Complete original app-data snapshot, never replacement or cleanup permission. Every feature is
 * captured explicitly on the same SQLite connection/transaction; old-file empty defaults are NOT
 * used to manufacture missing current-database sections. Raw recordings remain a separate archive.
 */
class SqliteApplicationBackup(private val database: SqlDatabase) {
    fun snapshot(): UserBackupDataJson.Snapshot =
        database.transaction {
            val core = SqliteCoreBackup(database).snapshot()
            val values = core.data.toMutableMap()
            var bytes = core.data.toString().encodeToByteArray().size.toLong()
            fun section(name: String, value: JsonObject) {
                val next = value.toString().encodeToByteArray().size.toLong()
                require(next <= BackupEnvelopeJson.MAX_BYTES - bytes) {
                    "Complete app data exceeds the supported backup size; nothing was truncated."
                }
                bytes += next
                values[name] = value
            }
            val noClock: () -> Instant = {
                error("Backup capture cannot create training timestamps.")
            }
            val legacy =
                SqliteLegacyGymProgrammes(database, noClock) {
                        error("Backup capture cannot create programme identities.")
                    }
                    .archiveSnapshot()
            section(
                "coaching",
                CoachingArchiveJson.element(SqliteCoachingArchive(database).snapshot()),
            )
            section(
                "development",
                DevelopmentArchiveJson.userOwnedElement(
                    SqliteDevelopmentArchive(database).snapshot()
                ),
            )
            section(
                "strengthState",
                StrengthChoicesArchiveJson.element(
                    SqliteStrengthChoices(database).archiveSnapshot()
                ),
            )
            section("gymProgramming", LegacyGymProgrammeArchiveJson.element(legacy))
            val support = SqliteRunningSupportArchives(database).snapshot()
            section("support", SupportArchiveJson.element(support.support))
            section("release08", Release08ArchiveJson.element(support.release08))
            programmingBudget()
            section("strengthProgramming", programming())
            section("strengthEngine", executions(noClock))
            section("gymTools", GymToolsPayloadJson.element(SqliteGymTools(database).snapshot()))
            section("achievements", achievements())
            section("bodyMeasurements", body())
            section(
                "detailRetention",
                DetailRetentionArchiveJson.element(
                    SqliteRecordingDetailReceipts(database).snapshot()
                ),
            )
            // Saved sessions and grading preference; an unsaved draft stays on this device only.
            section("climbing", ClimbingArchiveJson.element(SqliteClimbing(database).snapshot()))
            val root = JsonObject(values)
            val text = root.toString()
            require(text.encodeToByteArray().size <= BackupEnvelopeJson.MAX_BYTES) {
                "Complete app backup exceeds 32 MiB."
            }
            StoredJsonSyntax(text, 32, 2000000, 1024 * 1024) {
                    throw IllegalArgumentException("Invalid complete app backup: $it")
                }
                .validate()
            UserBackupDataJson.decode(root)
        }

    fun encode(createdAt: Instant): ByteArray =
        UserBackupDataJson.encodeCurrent(snapshot(), createdAt)

    /** Same original unavoidable JSON fragments; count/lower-bound checks precede hydration. */
    private fun programmingBudget() {
        var total = 0L
        for ((table, field) in
            listOf(
                "strength_workout_versions" to "payload",
                "strength_workout_preferences" to "workout_id",
                "strength_executions" to "header",
                "strength_executions" to "accepted_actual",
                "strength_execution_sets" to "payload",
                "strength_execution_events" to "payload",
                "strength_association_decisions" to "payload",
                "strength_programme_revisions" to "header",
                "strength_programme_blocks" to "header",
                "strength_programme_sessions" to "payload",
                "strength_programme_decisions" to "payload",
                "strength_programme_drafts" to "header",
                "strength_programme_draft_blocks" to "payload",
                "strength_custom_exercises" to "payload",
                "strength_concrete_drafts" to "header",
                "strength_concrete_draft_blocks" to "header",
                "strength_concrete_draft_workouts" to "payload",
            )) {
            val n =
                database
                    .query("SELECT TOTAL(LENGTH(CAST($field AS BLOB))) FROM $table")
                    .single()
                    .single()
                    .nullableSqlNumber()!!
            require(n.isFinite() && n >= 0 && n <= BackupEnvelopeJson.MAX_BYTES - total) {
                "Retained strength data exceeds the backup size; existing data was retained."
            }
            total += n.toLong()
        }
    }

    private fun keys(sql: String, maximum: Int): List<List<SqlValue>> {
        val count = database.query("SELECT COUNT(*) FROM ($sql)").single().single().sqlLong()
        require(count <= maximum) { "Too many retained backup records; nothing was truncated." }
        return database.query("$sql LIMIT ${maximum + 1}").also { require(it.size <= maximum) }
    }

    private fun programming(): JsonObject {
        val repository = SqliteStrengthProgrammingRecords(database)
        val programmes = linkedMapOf<String, StrengthProgramme>()
        for (key in
            keys(
                "SELECT programme_id,revision FROM strength_programme_revisions ORDER BY programme_id,revision",
                1000,
            )) {
            val p = requireNotNull(repository.programme(key[0].sqlText(), key[1].sqlLong().toInt()))
            programmes[
                StrengthProgrammingRepository.Reference(Owner.AUTHORED, p.id(), p.revision())
                    .key()] = p
        }
        val drafts =
            keys("SELECT id FROM strength_programme_drafts ORDER BY id", 1000).map {
                requireNotNull(repository.draft(it.single().sqlText()))
            }
        val concrete =
            keys("SELECT id FROM strength_concrete_drafts ORDER BY id", 1000).map {
                requireNotNull(repository.concreteDraft(it.single().sqlText()))
            }
        keys("SELECT id FROM strength_custom_exercises", 10000)
        keys("SELECT id FROM strength_programme_decisions", 100000)
        val current = repository.currentReference()?.takeIf { it.owner() == Owner.AUTHORED }
        return StrengthProgrammingArchiveJson.element(
            StrengthProgrammingArchiveJson.Snapshot(
                programmes,
                drafts,
                concrete,
                repository.customExercises(),
                repository.decisions(),
                current,
            )
        )
    }

    private fun executions(noClock: () -> Instant): JsonObject {
        val workouts =
            keys(
                    "SELECT payload FROM strength_workout_versions ORDER BY workout_id,revision",
                    10000,
                )
                .map { StoredJson.objectValue(it.single().sqlText()) }
        keys("SELECT workout_id FROM strength_workout_preferences", 10000)
        val preferences =
            SqliteGymWorkouts(database).preferences().map { p ->
                buildJsonObject {
                    put("id", p.workoutId())
                    put("currentRevision", p.currentRevision())
                    put("favourite", p.favourite())
                }
            }
        val repository = SqliteGymExecutionRecords(database)
        val executions =
            keys(
                    "SELECT id,accepted_actual,actual_edited_at FROM strength_executions ORDER BY started_at,id",
                    100000,
                )
                .map { row ->
                    val stored = requireNotNull(repository.load(row[0].sqlText()))
                    buildJsonObject {
                        put("revision", stored.revision())
                        put("snapshot", GymExecutionSnapshotJson.element(stored.execution()))
                        put(
                            "currentActual",
                            row[1].nullableSqlText()?.let { StoredJson.objectValue(it) } ?: JsonNull,
                        )
                        put("actualEditedAt", StoredJson.nullable(row[2].nullableSqlText()))
                    }
                }
        keys("SELECT id FROM strength_association_decisions", 100000)
        keys("SELECT actual_id FROM strength_association_targets", 100000)
        return buildJsonObject {
            put("version", 2)
            put("workouts", JsonArray(workouts))
            put("preferences", JsonArray(preferences))
            put("executions", JsonArray(executions))
            put(
                "associations",
                GymAssociationArchiveJson.element(
                    SqliteGymAssociations(database, noClock).archive()
                ),
            )
        }
    }

    private fun achievements(): JsonObject {
        keys("SELECT kind FROM training_milestones", TrainingMilestones.Kind.entries.size)
        keys(
            "SELECT award_kind,position FROM training_milestone_sources",
            TrainingMilestones.Kind.entries.size * 50,
        )
        val repository = SqliteTrainingMilestones(database)
        return AchievementsArchiveJson.element(
            AchievementsArchiveJson.Snapshot(repository.celebrations(), repository.awards())
        )
    }

    private fun body(): JsonObject {
        val total =
            database
                .query(
                    "SELECT COUNT(*),TOTAL(LENGTH(CAST(id AS BLOB))+LENGTH(CAST(observed_at AS BLOB))+LENGTH(CAST(provider AS BLOB))+LENGTH(CAST(source_id AS BLOB))+COALESCE(LENGTH(CAST(origin AS BLOB)),0)+COALESCE(LENGTH(CAST(imported_at AS BLOB)),0)) FROM body_heights"
                )
                .single()
        require(
            total[0].sqlLong() <= 100000 &&
                total[1].nullableSqlNumber()!! <= BackupEnvelopeJson.MAX_BYTES
        ) {
            "Height history exceeds the backup size."
        }
        val repository = SqliteBodyHeights(database)
        val heights = mutableListOf<BodyHeight>()
        var offset = 0
        while (true) {
            val page = repository.heights(500, offset)
            if (page.isEmpty()) break
            heights.addAll(page)
            offset += page.size
        }
        return BodyMeasurementsArchiveJson.element(
            BodyMeasurementsArchiveJson.Snapshot(heights, repository.preferences())
        )
    }
}
