package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.*
import org.racedaydreams.storage.GymStoredJson.child
import org.racedaydreams.storage.GymStoredJson.read
import org.racedaydreams.storage.GymStoredJson.text

/**
 * Normalised, read-only programming records. Future commands must recheck evidence transactionally.
 */
class SqliteStrengthProgrammingRecords(private val database: SqlDatabase) {
    fun currentReference(): Reference? =
        database.transaction {
            val modern =
                database
                    .query(
                        "SELECT programme_id,revision FROM strength_programme_current WHERE id=1"
                    )
                    .firstOrNull()
            if (modern != null) {
                require(database.query("SELECT 1 FROM gym_current WHERE id=1").isEmpty()) {
                    "Two current strength programmes."
                }
                Reference(Owner.AUTHORED, modern[0].sqlText(), modern[1].sqlLong().toInt())
            } else
                database
                    .query(
                        "SELECT v.plan_id,v.version FROM gym_current c JOIN gym_plan_versions v ON c.version_id=v.version_id WHERE c.id=1"
                    )
                    .firstOrNull()
                    ?.let { Reference(Owner.LEGACY, it[0].sqlText(), it[1].sqlLong().toInt()) }
        }

    fun programme(id: String, revision: Int): StrengthProgramme? =
        database.transaction {
            Reference(Owner.AUTHORED, id, revision)
            val values = listOf(SqlValue.Text(id), SqlValue.Integer(revision.toLong()))
            val row =
                database
                    .query(
                        "SELECT header,name,start_date,end_date,parent_revision FROM strength_programme_revisions WHERE programme_id=? AND revision=?",
                        values,
                    )
                    .firstOrNull() ?: return@transaction null
            val root = read(row[0].sqlText())
            val blocks =
                database
                    .query(
                        "SELECT position,block_id,header FROM strength_programme_blocks WHERE programme_id=? AND revision=? ORDER BY position",
                        values,
                    )
                    .mapIndexed { i, b ->
                        require(b[0].sqlLong() == i.toLong()) { "Programme block order." }
                        val block = read(b[2].sqlText())
                        require(text(block, "id", 256) == b[1].sqlText()) {
                            "Programme block identity."
                        }
                        val workouts =
                            database
                                .query(
                                    "SELECT position,session_id,session_date,payload FROM strength_programme_sessions WHERE programme_id=? AND revision=? AND block_id=? ORDER BY position",
                                    values + b[1],
                                )
                                .mapIndexed { n, w -> indexedWorkout(w, n) }
                        JsonObject(block + ("workouts" to JsonArray(workouts)))
                    }
            val p =
                StrengthProgrammingPayloadJson.decode(
                    JsonObject(root + ("blocks" to JsonArray(blocks)))
                )
            require(
                p.id() == id &&
                    p.revision() == revision &&
                    p.name() == row[1].sqlText() &&
                    p.start().toString() == row[2].sqlText() &&
                    p.end().toString() == row[3].sqlText() &&
                    p.parentRevision().toLong() == row[4].sqlLong()
            ) {
                "Programme summary index."
            }
            p
        }

    fun versions(limit: Int, offset: Int): List<Summary> {
        page(limit, offset)
        return database
            .query(
                "SELECT programme_id,revision,name,start_date,end_date FROM strength_programme_revisions ORDER BY start_date DESC,programme_id,revision DESC LIMIT ? OFFSET ?",
                pageValues(limit, offset),
            )
            .map {
                Summary(
                    it[0].sqlText(),
                    it[1].sqlLong().toInt(),
                    it[2].sqlText(),
                    StoredDate.parse(it[3].sqlText()),
                    StoredDate.parse(it[4].sqlText()),
                )
            }
    }

    fun draft(id: String): StrengthProgrammeDraft? =
        database.transaction {
            val args = listOf(SqlValue.Text(sqlRequired(id, "draft")))
            val row =
                database
                    .query(
                        "SELECT header,revision,programme_id,base_revision,name,start_date FROM strength_programme_drafts WHERE id=?",
                        args,
                    )
                    .firstOrNull() ?: return@transaction null
            val root = read(row[0].sqlText())
            val blocks =
                database
                    .query(
                        "SELECT position,payload FROM strength_programme_draft_blocks WHERE draft_id=? ORDER BY position",
                        args,
                    )
                    .mapIndexed { i, b ->
                        require(b[0].sqlLong() == i.toLong()) { "Draft block order." }
                        read(b[1].sqlText())
                    }
            val d =
                StrengthProgrammingPayloadJson.decodeDraft(
                    JsonObject(root + ("blocks" to JsonArray(blocks))).toString()
                )
            require(
                d.id() == id &&
                    d.revision().toLong() == row[1].sqlLong() &&
                    d.programmeId() == row[2].sqlText() &&
                    d.baseRevision().toLong() == row[3].sqlLong() &&
                    d.name() == row[4].sqlText() &&
                    d.start().toString() == row[5].sqlText()
            ) {
                "Draft summary index."
            }
            d
        }

    fun drafts(limit: Int, offset: Int): List<DraftSummary> {
        page(limit, offset)
        return database
            .query(
                "SELECT id,revision,name,start_date FROM strength_programme_drafts ORDER BY id LIMIT ? OFFSET ?",
                pageValues(limit, offset),
            )
            .map {
                DraftSummary(
                    it[0].sqlText(),
                    it[1].sqlLong().toInt(),
                    it[2].sqlText(),
                    StoredDate.parse(it[3].sqlText()),
                )
            }
    }

    fun concreteDraft(id: String): ConcreteStrengthProgrammeDraft? =
        database.transaction {
            val args = listOf(SqlValue.Text(sqlRequired(id, "concrete draft")))
            val row =
                database
                    .query(
                        "SELECT header,revision,programme_id,programme_revision,name,start_date,end_date FROM strength_concrete_drafts WHERE id=?",
                        args,
                    )
                    .firstOrNull() ?: return@transaction null
            val root = read(row[0].sqlText())
            val p = child(root, "programme")
            val blocks =
                database
                    .query(
                        "SELECT position,block_id,header FROM strength_concrete_draft_blocks WHERE draft_id=? ORDER BY position",
                        args,
                    )
                    .mapIndexed { i, b ->
                        require(b[0].sqlLong() == i.toLong()) { "Concrete block order." }
                        val block = read(b[2].sqlText())
                        require(text(block, "id", 256) == b[1].sqlText()) {
                            "Concrete block identity."
                        }
                        val workouts =
                            database
                                .query(
                                    "SELECT position,workout_id,workout_date,payload FROM strength_concrete_draft_workouts WHERE draft_id=? AND block_id=? ORDER BY position",
                                    args + b[1],
                                )
                                .mapIndexed { n, w -> indexedWorkout(w, n) }
                        JsonObject(block + ("workouts" to JsonArray(workouts)))
                    }
            val d =
                ConcreteStrengthDraftPayloadJson.decode(
                    JsonObject(
                            root + ("programme" to JsonObject(p + ("blocks" to JsonArray(blocks))))
                        )
                        .toString()
                )
            val programme = d.programme()
            require(
                d.id() == id &&
                    d.revision().toLong() == row[1].sqlLong() &&
                    programme.id() == row[2].sqlText() &&
                    programme.revision().toLong() == row[3].sqlLong() &&
                    programme.name() == row[4].sqlText() &&
                    programme.start().toString() == row[5].sqlText() &&
                    programme.end().toString() == row[6].sqlText()
            ) {
                "Concrete draft index."
            }
            d
        }

    fun concreteDrafts(
        limit: Int,
        offset: Int,
    ): List<ConcreteStrengthProgrammeDraftRepository.Summary> {
        page(limit, offset)
        require(offset <= 1000000) { "Invalid draft page." }
        return database
            .query(
                "SELECT d.id,d.revision,d.programme_id,d.programme_revision,d.name,d.start_date,d.end_date,EXISTS(SELECT 1 FROM strength_programme_revisions p WHERE p.programme_id=d.programme_id AND p.revision=d.programme_revision) FROM strength_concrete_drafts d ORDER BY d.start_date DESC,d.id LIMIT ? OFFSET ?",
                pageValues(limit, offset),
            )
            .map {
                ConcreteStrengthProgrammeDraftRepository.Summary(
                    it[0].sqlText(),
                    it[1].sqlLong().toInt(),
                    it[2].sqlText(),
                    it[3].sqlLong().toInt(),
                    it[4].sqlText(),
                    StoredDate.parse(it[5].sqlText()),
                    StoredDate.parse(it[6].sqlText()),
                    it[7].sqlLong() == 1L,
                )
            }
    }

    fun customExercise(id: String): CustomStrengthExercise? =
        database
            .query(
                "SELECT payload FROM strength_custom_exercises WHERE id=?",
                listOf(SqlValue.Text(sqlRequired(id, "custom exercise"))),
            )
            .firstOrNull()
            ?.let { custom(id, it[0]) }

    fun customExercises(): List<CustomStrengthExercise> =
        database.query("SELECT id,payload FROM strength_custom_exercises ORDER BY id").map {
            custom(it[0].sqlText(), it[1])
        }

    fun decisions(): List<Decision> =
        database.query("SELECT payload FROM strength_programme_decisions ORDER BY position").map {
            StrengthProgrammingDecisionJson.decode(it[0].sqlText())
        }

    private fun custom(id: String, payload: SqlValue): CustomStrengthExercise =
        StrengthProgrammingPayloadJson.decodeCustom(payload.sqlText()).also {
            require(it.exercise().id() == id) { "Custom exercise identity." }
        }

    private fun indexedWorkout(row: List<SqlValue>, position: Int): JsonObject =
        read(row[3].sqlText()).also {
            require(
                row[0].sqlLong() == position.toLong() &&
                    text(it, "id", 256) == row[1].sqlText() &&
                    text(it, "date", 64) == row[2].sqlText()
            ) {
                "Workout index."
            }
        }

    private fun page(limit: Int, offset: Int) {
        require(limit in 1..100 && offset >= 0) { "Choose a bounded programme page." }
    }

    private fun pageValues(limit: Int, offset: Int): List<SqlValue> =
        listOf(SqlValue.Integer(limit.toLong()), SqlValue.Integer(offset.toLong()))
}
