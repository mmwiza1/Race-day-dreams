package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.Owner
import org.racedaydreams.storage.GymStoredJson.array
import org.racedaydreams.storage.GymStoredJson.child
import org.racedaydreams.storage.GymStoredJson.read
import org.racedaydreams.storage.GymStoredJson.row
import org.racedaydreams.storage.GymStoredJson.text
import org.racedaydreams.storage.GymStoredJson.write

/** Explicit draft/custom commands on the original pool. Saving is never programme activation. */
class SqliteStrengthDrafts(private val database: SqlDatabase) {
    private val records = SqliteStrengthProgrammingRecords(database)

    fun saveDraft(value: StrengthProgrammeDraft, expectedRevision: Int) =
        database.transaction {
            val previous = records.draft(value.id())
            if (value == previous) return@transaction
            require(
                expectedRevision >= 0 &&
                    expectedRevision == (previous?.revision() ?: 0) &&
                    value.revision() > expectedRevision
            ) {
                "The draft changed. Reload and review before saving."
            }
            require(
                !legacyId(value.programmeId()) &&
                    (value.baseRevision() == 0 ||
                        records.programme(value.programmeId(), value.baseRevision()) != null)
            ) {
                "Draft base programme is missing or ambiguous."
            }
            for (b in value.blocks()) for (d in b.days()) for (slot in d.slots()) checkExercise(
                slot.exercise()
            )
            StrengthProgrammingArchiveJson.validateRows(value)
            writeDraft(value, previous != null)
        }

    /**
     * As before, deleting an already absent weekly draft is harmless; stale edits cannot delete.
     */
    fun deleteDraft(id: String, expectedRevision: Int) =
        database.transaction {
            val old = records.draft(id)
            if (old != null) {
                require(old.revision() == expectedRevision) {
                    "The draft changed. Reload before deleting."
                }
                database.execute("DELETE FROM strength_programme_drafts WHERE id=?", texts(id))
            }
        }

    fun saveCustom(value: CustomStrengthExercise, expected: CustomStrengthExercise?) =
        database.transaction {
            val previous = records.customExercise(value.exercise().id())
            if (value == previous) return@transaction
            require(previous == expected) { "The custom exercise changed. Reload before saving." }
            if (previous != null) sameMeasurement(previous.exercise(), value.exercise())
            database
                .query(
                    "SELECT payload FROM strength_exercises WHERE id=?",
                    texts(value.exercise().id()),
                )
                .firstOrNull()
                ?.let {
                    require(
                        StrengthEvidenceJson.decodeExercise(it[0].sqlText()) ==
                            (previous?.exercise() ?: value.exercise())
                    ) {
                        "Custom metadata and logging definition changed."
                    }
                }
            writeCustom(value)
        }

    /**
     * Existing logging editors keep the typed metadata and logging definition in one transaction.
     */
    fun updateLoggingDefinition(exercise: StrengthExercise): Boolean =
        database.transaction {
            val previous = records.customExercise(exercise.id()) ?: return@transaction false
            sameMeasurement(previous.exercise(), exercise)
            writeCustom(
                CustomStrengthExercise(
                    exercise,
                    previous.bodyArea(),
                    previous.role(),
                    previous.equipment(),
                    previous.notes(),
                )
            )
            true
        }

    fun saveConcrete(value: ConcreteStrengthProgrammeDraft, expectedRevision: Int) =
        database.transaction {
            StrengthProgrammingArchiveJson.validateRows(value)
            val previous = records.concreteDraft(value.id())
            if (value == previous) return@transaction
            require(expectedRevision >= 0 && expectedRevision == (previous?.revision() ?: 0)) {
                "The concrete draft changed. Reload before saving."
            }
            if (expectedRevision == Int.MAX_VALUE)
                throw ArithmeticException("Draft revision overflow.")
            require(value.revision() == expectedRevision + 1) {
                "Review the next concrete draft revision."
            }
            require(
                previous == null ||
                    previous.programme().id() == value.programme().id() &&
                        previous.programme().revision() == value.programme().revision() &&
                        previous.before() == value.before()
            ) {
                "A saved preview must preserve programme identity and base."
            }
            require(
                !legacyId(value.programme().id()) &&
                    records.programme(value.programme().id(), value.programme().revision()) == null
            ) {
                "Preserved programmes cannot be edited as unaccepted drafts."
            }
            checkReferences(value)
            if (previous == null)
                require(
                    database
                        .query("SELECT COUNT(*) FROM strength_concrete_drafts")
                        .single()
                        .single()
                        .sqlLong() < 1000
                ) {
                    "Manage saved drafts before adding another."
                }
            writeConcrete(value, previous != null)
        }

    /**
     * Concrete deletion requires the exact retained preview, including its independent revision.
     */
    fun deleteConcrete(id: String, expectedRevision: Int) =
        database.transaction {
            val old = records.concreteDraft(id)
            require(old != null && old.revision() == expectedRevision) {
                "The concrete draft changed. Reload before deleting."
            }
            database.execute("DELETE FROM strength_concrete_drafts WHERE id=?", texts(id))
        }

    private fun checkReferences(draft: ConcreteStrengthProgrammeDraft) {
        val ref = draft.before()
        if (ref != null) {
            val found =
                if (ref.owner() == Owner.AUTHORED)
                    records.programme(ref.id(), ref.revision()) != null
                else
                    database
                        .query(
                            "SELECT 1 FROM gym_plan_versions WHERE plan_id=? AND version=?",
                            listOf(
                                SqlValue.Text(ref.id()),
                                SqlValue.Integer(ref.revision().toLong()),
                            ),
                        )
                        .isNotEmpty()
            require(found) { "Concrete draft base reference is missing." }
        }
        if (draft.programme().parentRevision() > 0)
            draft.validateAgainst(records.programme(ref!!.id(), ref.revision())!!)
        for (w in draft.programme().workouts()) for (slot in w.definition().slots()) checkExercise(
            slot.exercise()
        )
    }

    private fun checkExercise(exercise: StrengthExercise) {
        require(
            ExerciseCatalog.definition(exercise.id()) != null ||
                records.customExercise(exercise.id()) != null
        ) {
            "Draft custom exercise metadata is missing."
        }
    }

    /** Only the enclosing validated archive transaction may populate retained draft revisions. */
    internal fun populateArchive(snapshot: StrengthProgrammingArchiveJson.Snapshot) =
        database.transaction {
            for (table in
                listOf(
                    "strength_custom_exercises",
                    "strength_programme_drafts",
                    "strength_programme_draft_blocks",
                    "strength_concrete_drafts",
                    "strength_concrete_draft_blocks",
                    "strength_concrete_draft_workouts",
                )) require(database.query("SELECT 1 FROM $table LIMIT 1").isEmpty()) {
                "Draft restore requires empty prepared tables; existing data was retained."
            }
            for (custom in snapshot.custom) writeCustom(custom)
            for (draft in snapshot.drafts) {
                StrengthProgrammingArchiveJson.validateRows(draft)
                writeDraft(draft, false)
            }
            for (draft in snapshot.concreteDrafts) {
                StrengthProgrammingArchiveJson.validateRows(draft)
                writeConcrete(draft, false)
            }
        }

    private fun writeDraft(value: StrengthProgrammeDraft, exists: Boolean) {
        val root = read(StrengthProgrammingPayloadJson.encodeDraft(value))
        val payload = bounded(JsonObject(root - "blocks"), 262144)
        val values =
            listOf(
                SqlValue.Integer(value.revision().toLong()),
                SqlValue.Text(value.programmeId()),
                SqlValue.Integer(value.baseRevision().toLong()),
            ) + texts(value.name(), value.start().toString(), payload, value.id())
        if (!exists)
            database.execute(
                "INSERT INTO strength_programme_drafts(revision,programme_id,base_revision,name,start_date,header,id) VALUES(?,?,?,?,?,?,?)",
                values,
            )
        else
            database.execute(
                "UPDATE strength_programme_drafts SET revision=?,programme_id=?,base_revision=?,name=?,start_date=?,header=? WHERE id=?",
                values,
            )
        database.execute(
            "DELETE FROM strength_programme_draft_blocks WHERE draft_id=?",
            texts(value.id()),
        )
        for ((i, b) in array(root, "blocks", 12).withIndex()) database.execute(
            "INSERT INTO strength_programme_draft_blocks(draft_id,position,payload) VALUES(?,?,?)",
            listOf(
                SqlValue.Text(value.id()),
                SqlValue.Integer(i.toLong()),
                SqlValue.Text(bounded(row(b), 262144)),
            ),
        )
    }

    private fun writeConcrete(draft: ConcreteStrengthProgrammeDraft, exists: Boolean) {
        val root = read(ConcreteStrengthDraftPayloadJson.encode(draft))
        val p = child(root, "programme")
        val programme = draft.programme()
        val header = bounded(JsonObject(root + ("programme" to JsonObject(p - "blocks"))), 262144)
        val values =
            listOf(
                SqlValue.Integer(draft.revision().toLong()),
                SqlValue.Text(programme.id()),
                SqlValue.Integer(programme.revision().toLong()),
            ) +
                texts(
                    programme.name(),
                    programme.start().toString(),
                    programme.end().toString(),
                    header,
                    draft.id(),
                )
        if (!exists)
            database.execute(
                "INSERT INTO strength_concrete_drafts(revision,programme_id,programme_revision,name,start_date,end_date,header,id) VALUES(?,?,?,?,?,?,?,?)",
                values,
            )
        else
            database.execute(
                "UPDATE strength_concrete_drafts SET revision=?,programme_id=?,programme_revision=?,name=?,start_date=?,end_date=?,header=? WHERE id=?",
                values,
            )
        database.execute(
            "DELETE FROM strength_concrete_draft_blocks WHERE draft_id=?",
            texts(draft.id()),
        )
        for ((i, value) in array(p, "blocks", 12).withIndex()) {
            val block = row(value)
            val id = text(block, "id", 256)
            database.execute(
                "INSERT INTO strength_concrete_draft_blocks(draft_id,block_id,position,header) VALUES(?,?,?,?)",
                listOf(
                    SqlValue.Text(draft.id()),
                    SqlValue.Text(id),
                    SqlValue.Integer(i.toLong()),
                    SqlValue.Text(bounded(JsonObject(block - "workouts"), 262144)),
                ),
            )
            for ((n, v) in array(block, "workouts", 156).withIndex()) {
                val workout = row(v)
                database.execute(
                    "INSERT INTO strength_concrete_draft_workouts(draft_id,block_id,position,workout_id,workout_date,payload) VALUES(?,?,?,?,?,?)",
                    listOf(
                        SqlValue.Text(draft.id()),
                        SqlValue.Text(id),
                        SqlValue.Integer(n.toLong()),
                    ) +
                        texts(
                            text(workout, "id", 256),
                            text(workout, "date", 64),
                            bounded(workout, 262144),
                        ),
                )
            }
        }
    }

    private fun writeCustom(value: CustomStrengthExercise) {
        val payload = bounded(read(StrengthProgrammingPayloadJson.encodeCustom(value)), 65536)
        database.execute(
            "INSERT OR REPLACE INTO strength_custom_exercises(id,payload) VALUES(?,?)",
            texts(value.exercise().id(), payload),
        )
        database.execute(
            "INSERT OR REPLACE INTO strength_exercises(id,payload) VALUES(?,?)",
            texts(value.exercise().id(), StrengthEvidenceJson.encodeExercise(value.exercise())),
        )
    }

    private fun sameMeasurement(before: StrengthExercise, after: StrengthExercise) {
        require(before.measurement() == after.measurement() && before.mode() == after.mode()) {
            "Create a new exercise for a different measurement task."
        }
    }

    private fun legacyId(id: String): Boolean =
        database
            .query("SELECT 1 FROM gym_plan_versions WHERE plan_id=? LIMIT 1", texts(id))
            .isNotEmpty()

    private fun bounded(value: JsonObject, maximum: Int): String =
        write(value).also {
            require(it.encodeToByteArray().size <= maximum) { "Authoring row is too large." }
        }

    private fun texts(vararg values: String): List<SqlValue> = values.map { SqlValue.Text(it) }
}
