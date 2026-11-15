package org.racedaydreams.storage

import org.racedaydreams.domain.body.BodyHeight
import org.racedaydreams.domain.body.BodyMeasurementsRepository.Preferences
import org.racedaydreams.domain.body.HeightUnit
import org.racedaydreams.domain.body.ProteinGuidance
import org.racedaydreams.domain.time.Offsets

/** Existing typed height tables. Weight history retains its original durable owner/format. */
class SqliteBodyHeights(private val database: SqlDatabase) {
    fun heights(limit: Int, offset: Int): List<BodyHeight> {
        require(limit in 1..500 && offset >= 0) { "Invalid height page." }
        return database
            .query(
                "SELECT id,metres,observed_at,offset_seconds,provider,source_id,origin,imported_at FROM body_heights ORDER BY observed_seconds DESC,observed_nanos DESC,id LIMIT ? OFFSET ?",
                listOf(SqlValue.Integer(limit.toLong()), SqlValue.Integer(offset.toLong())),
            )
            .map { row ->
                BodyHeight(
                    row.text(0),
                    (row[1] as SqlValue.Real).value,
                    StoredInstant.parse(row.text(2)),
                    if (row[3] == SqlValue.Null) null
                    else Offsets.ofTotalSeconds((row[3] as SqlValue.Integer).value.toInt()),
                    row.text(4),
                    row.text(5),
                    row.nullableText(6),
                    row.nullableText(7)?.let { StoredInstant.parse(it) },
                )
            }
    }

    fun saveHeight(value: BodyHeight) =
        database.transaction {
            val source =
                database
                    .query(
                        "SELECT id FROM body_heights WHERE provider=? AND source_id=?",
                        listOf(SqlValue.Text(value.provider()), SqlValue.Text(value.sourceId())),
                    )
                    .singleOrNull()
            require(source == null || source.text(0) == value.id()) {
                "Height source already has a different identity."
            }
            val values =
                listOf(
                    SqlValue.Real(value.metres()),
                    SqlValue.Text(value.at().toString()),
                    SqlValue.Integer(value.at().getEpochSecond()),
                    SqlValue.Integer(value.at().getNano().toLong()),
                    value.offset()?.let { SqlValue.Integer(it.getTotalSeconds().toLong()) }
                        ?: SqlValue.Null,
                    SqlValue.Text(value.provider()),
                    SqlValue.Text(value.sourceId()),
                    text(value.origin()),
                    text(value.importedAt()?.toString()),
                    SqlValue.Text(value.id()),
                )
            val existing =
                database
                    .query(
                        "SELECT id FROM body_heights WHERE id=?",
                        listOf(SqlValue.Text(value.id())),
                    )
                    .isNotEmpty()
            if (existing)
                database.execute(
                    "UPDATE body_heights SET metres=?,observed_at=?,observed_seconds=?,observed_nanos=?,offset_seconds=?,provider=?,source_id=?,origin=?,imported_at=? WHERE id=?",
                    values,
                )
            else
                database.execute(
                    "INSERT INTO body_heights(metres,observed_at,observed_seconds,observed_nanos,offset_seconds,provider,source_id,origin,imported_at,id) VALUES(?,?,?,?,?,?,?,?,?,?)",
                    values,
                )
        }

    fun deleteHeight(id: String) {
        require(id.length <= 256 && id.any { it.code > 0x20 }) { "Invalid height." }
        database.execute("DELETE FROM body_heights WHERE id=?", listOf(SqlValue.Text(id)))
    }

    fun preferences(): Preferences {
        val row =
            database
                .query(
                    "SELECT protein_enabled,height_unit,basis_id,basis_at,basis_kg FROM body_guidance_preferences WHERE id=1"
                )
                .singleOrNull() ?: return Preferences.defaults()
        return Preferences(
            (row[0] as SqlValue.Integer).value == 1L,
            HeightUnit.valueOf(row.text(1)),
            if (row[2] == SqlValue.Null) null
            else
                ProteinGuidance.Basis(
                    row.text(2),
                    StoredInstant.parse(row.text(3)),
                    (row[4] as SqlValue.Real).value,
                ),
        )
    }

    fun preferences(value: Preferences) {
        val basis = value.basis()
        database.execute(
            "INSERT OR REPLACE INTO body_guidance_preferences(id,protein_enabled,height_unit,basis_id,basis_at,basis_kg) VALUES(1,?,?,?,?,?)",
            listOf(
                SqlValue.Integer(if (value.proteinEnabled()) 1 else 0),
                SqlValue.Text(value.heightUnit().name),
                text(basis?.id()),
                text(basis?.at()?.toString()),
                basis?.let { SqlValue.Real(it.kilograms()) } ?: SqlValue.Null,
            ),
        )
    }

    private fun text(value: String?): SqlValue = value?.let { SqlValue.Text(it) } ?: SqlValue.Null

    private fun List<SqlValue>.text(index: Int): String = (this[index] as SqlValue.Text).value

    private fun List<SqlValue>.nullableText(index: Int): String? =
        if (this[index] == SqlValue.Null) null else text(index)
}
