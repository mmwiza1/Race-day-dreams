package org.racedaydreams.storage

import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate
import org.racedaydreams.storage.FrozenGymWorkoutRecordJson.Record
import org.racedaydreams.storage.FrozenGymWorkoutRecordJson.RepeatProof
import org.racedaydreams.storage.FrozenGymWorkoutRecordJson.TemplateKind

/**
 * SHA-256 of ORIGINAL Android CoachingJson.strength(actual).toString() UTF-8 bytes, lowercase hex.
 * This is a compatibility boundary, not permission to hash semantically equivalent JSON. A native
 * host must supply a verified compatible implementation before enabling execution commands.
 */
fun interface LegacyStrengthFingerprint {
    fun fingerprint(actual: StrengthSession): String
}

/** All calls belong to the enclosing execution/actual write transaction on this SAME database. */
internal class SqliteGymSources(
    private val database: SqlDatabase,
    private val fingerprints: LegacyStrengthFingerprint,
) {
    private val workouts = SqliteGymWorkouts(database)
    private val executions = SqliteGymExecutionRecords(database)
    private val programmes = SqliteStrengthProgrammingRecords(database)

    fun actual(id: String): StrengthSession? =
        database
            .query("SELECT payload FROM strength_sessions WHERE id=?", texts(id))
            .firstOrNull()
            ?.let { StrengthEvidenceJson.decodeSession(it.single().sqlText()) }

    fun ensure(definition: GymWorkoutDefinition?, source: GymExecution.SourceReference?) {
        if (definition == null) {
            require(source == null) { "A workout source requires its prescription." }
            return
        }
        val retained = workouts.record(definition.id(), definition.revision())
        if (retained != null) {
            require(retained.definition == definition) { "The saved workout changed." }
            if (source?.kind() == GymExecution.SourceKind.SAVED_WORKOUT)
                GymWorkoutSourceValidator.savedWorkout(definition, source, retained.definition)
            else require(retained.source == source) { "The workout source changed." }
            if (retained.source?.kind() == GymExecution.SourceKind.PROGRAMME)
                programmeDate(retained.definition, retained.source)
            if (retained.source?.kind() == GymExecution.SourceKind.ACTUAL_SESSION)
                validateAvailable(
                    retained,
                    actual(retained.source.id()),
                    executions.load(retained.source.id())?.execution(),
                )
            return
        }
        var proof: RepeatProof? = null
        if (source != null)
            when (source.kind()) {
                GymExecution.SourceKind.SAVED_WORKOUT ->
                    errorMissing("The saved workout revision is missing.")
                GymExecution.SourceKind.PROGRAMME -> programmeDate(definition, source)
                GymExecution.SourceKind.ACTUAL_SESSION -> proof = repeatProof(definition, source)
            }
        val record = Record(definition, source, proof)
        database.execute(
            "INSERT INTO strength_workout_versions(workout_id,revision,source_kind,source_id,payload) VALUES(?,?,?,?,?)",
            listOf(
                SqlValue.Text(definition.id()),
                SqlValue.Integer(definition.revision().toLong()),
                sqlText(source?.kind()?.name),
                sqlText(source?.id()),
                SqlValue.Text(bounded(FrozenGymWorkoutRecordJson.encode(record))),
            ),
        )
    }

    fun programmeDate(
        definition: GymWorkoutDefinition,
        source: GymExecution.SourceReference,
    ): LocalDate {
        val authored = programmes.programme(source.id(), source.revision())
        if (authored != null)
            return GymWorkoutSourceValidator.authoredProgramme(definition, source, authored).date()
        val legacy =
            SqliteLegacyGymProgrammes(database, { definition.createdAt() }) {
                    error("Source validation cannot generate programme choices.")
                }
                .version(source.id(), source.revision())
        return GymWorkoutSourceValidator.programme(definition, source, legacy).date()
    }

    private fun repeatProof(
        definition: GymWorkoutDefinition,
        source: GymExecution.SourceReference,
    ): RepeatProof {
        val actual = actual(source.id()) ?: errorMissing("The actual repeat source is missing.")
        val previous = executions.load(source.id())?.execution()
        if (previous?.status() == GymExecution.Status.FINISHED) {
            try {
                GymWorkoutSourceValidator.repeatedExecution(definition, source, previous)
                return proof(TemplateKind.EXECUTION, previous.slots(), actual)
            } catch (_: IllegalArgumentException) {
                // Original behaviour also permits an explicit repeat of the actual exercise list.
            }
        }
        GymWorkoutSourceValidator.repeatedActual(definition, source, actual)
        return proof(TemplateKind.ACTUAL, definition.slots(), actual)
    }

    private fun proof(
        kind: TemplateKind,
        slots: List<GymWorkoutDefinition.Slot>,
        actual: StrengthSession,
    ): RepeatProof {
        val hash = fingerprint(actual)
        return RepeatProof(kind, slots, hash, hash, null, null)
    }

    private fun validateAvailable(row: Record, actual: StrengthSession?, execution: GymExecution?) =
        FrozenGymWorkoutRecordJson.validateAvailable(row, actual, execution, fingerprints)

    fun acknowledgeChange(id: String, changed: StrengthSession?, at: Instant) {
        val rows =
            database
                .query(
                    "SELECT payload FROM strength_workout_versions WHERE source_kind='ACTUAL_SESSION' AND source_id=?",
                    texts(id),
                )
                .map { FrozenGymWorkoutRecordJson.decode(it.single().sqlText()) }
        if (rows.isEmpty()) return
        val before = actual(id)
        val original = executions.load(id)?.execution()
        for (row in rows) {
            validateAvailable(row, before, original)
            val proof = row.sourceSnapshot!!
            if (changed == null && proof.sourceDeletedAt != null) continue
            require(proof.sourceDeletedAt == null) {
                "A deleted repeat source identity needs a validated restore, not a new actual edit."
            }
            val updated =
                if (changed == null)
                    proof.copy(sourceDeletedAt = at, currentSourceFingerprint = null)
                else
                    proof.copy(sourceEditedAt = at, currentSourceFingerprint = fingerprint(changed))
            database.execute(
                "UPDATE strength_workout_versions SET payload=? WHERE workout_id=? AND revision=?",
                listOf(
                    SqlValue.Text(
                        bounded(
                            FrozenGymWorkoutRecordJson.encode(row.copy(sourceSnapshot = updated))
                        )
                    ),
                    SqlValue.Text(row.definition.id()),
                    SqlValue.Integer(row.definition.revision().toLong()),
                ),
            )
            require(database.query("SELECT changes()").single().single().sqlLong() == 1L) {
                "The retained repeat receipt changed. Actual evidence was retained."
            }
        }
    }

    private fun fingerprint(actual: StrengthSession): String =
        FrozenGymWorkoutRecordJson.compatibleFingerprint(actual, fingerprints)

    private fun bounded(payload: String): String =
        payload.also {
            require(it.encodeToByteArray().size <= 262144) {
                "This gym record exceeds the supported 256 KiB row limit. Existing staged sets were retained."
            }
        }

    private fun texts(vararg values: String): List<SqlValue> = values.map { SqlValue.Text(it) }

    private fun errorMissing(message: String): Nothing = throw IllegalArgumentException(message)
}
