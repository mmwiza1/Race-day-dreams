package org.racedaydreams.storage

import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Keeps the original race checklist rows readable by Android and the existing backup format. */
class SqliteRaceChecklist(private val database: SqlDatabase) {
    fun checked(id: String): Boolean =
        database
            .query(
                "SELECT payload FROM release08 WHERE kind='checklist' AND id=?",
                listOf(SqlValue.Text(sqlRequired(id, "checklist item"))),
            )
            .singleOrNull()
            ?.single()
            ?.sqlText()
            ?.let { payload ->
                requireNotNull(StoredJson.objectValue(payload)["checked"]).jsonPrimitive.boolean
            } ?: false

    fun save(id: String, label: String, checked: Boolean) {
        require(id.length in 1..256 && label.length in 1..120)
        val payload = buildJsonObject {
            put("version", 1)
            put("label", label)
            put("checked", checked)
        }
        database.execute(
            "INSERT OR REPLACE INTO release08(kind,id,payload) VALUES('checklist',?,?)",
            listOf(SqlValue.Text(id), SqlValue.Text(payload.toString())),
        )
    }
}
