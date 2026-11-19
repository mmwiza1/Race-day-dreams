package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.WorkoutSegment
import org.racedaydreams.domain.execution.WorkoutExecutionEngine.Step
import org.racedaydreams.storage.StoredJson.row

/** Existing org.json scalar conversion rules, confined to the legacy support formats. */
internal object SupportPayloadJson {
    fun value(row: JsonObject, key: String): JsonElement =
        row[key] ?: error("Missing support field: $key")

    fun text(row: JsonObject, key: String): String =
        when (val v = value(row, key)) {
            is JsonPrimitive -> v.content
            else -> v.toString()
        }

    fun version(row: JsonObject) {
        val v = value(row, "version")
        require(v is JsonPrimitive && !v.isString && v.content.toIntOrNull() == 1) {
            "Unsupported support record version."
        }
    }

    fun number(row: JsonObject, key: String): Double {
        val v = value(row, key)
        require(v is JsonPrimitive && v != JsonNull) { "Expected support number." }
        // Android first parses unquoted integer literals as Long, including negative zero.
        return (if (!v.isString) v.content.toLongOrNull()?.toDouble() else null)
            ?: v.content.toDoubleOrNull()
            ?: error("Expected support number.")
    }

    fun long(row: JsonObject, key: String): Long {
        val v = value(row, key)
        require(v is JsonPrimitive && v != JsonNull) { "Expected support integer." }
        return (if (!v.isString) v.content.toLongOrNull() else null) ?: number(row, key).toLong()
    }

    fun integer(row: JsonObject, key: String): Int {
        val v = value(row, key)
        require(v is JsonPrimitive && v != JsonNull) { "Expected support integer." }
        return (if (!v.isString) v.content.toLongOrNull()?.toInt() else null)
            ?: number(row, key).toInt()
    }

    fun bool(row: JsonObject, key: String): Boolean {
        val v = value(row, key)
        require(v is JsonPrimitive && v != JsonNull) { "Expected support boolean." }
        return when {
            v.content.equals("true", true) -> true
            v.content.equals("false", true) -> false
            else -> error("Expected support boolean.")
        }
    }

    fun array(row: JsonObject, key: String, maximum: Int): JsonArray {
        val a = value(row, key) as? JsonArray ?: error("Expected support array: $key")
        require(a.size <= maximum) { "Too many support records; nothing was truncated." }
        return a
    }

    fun steps(row: JsonObject): List<Step> {
        val a = array(row, "steps", 500)
        require(a.isNotEmpty()) { "Choose at least one step." }
        return a.map { item ->
            val s = item.row()
            fun optional(key: String) =
                if (s[key] == null || s[key] == JsonNull) null else number(s, key)
            Step(
                text(s, "label"),
                WorkoutSegment.Kind.valueOf(text(s, "kind")),
                optional("metres"),
                if (s["millis"] == null || s["millis"] == JsonNull) null else long(s, "millis"),
                optional("fast"),
                optional("slow"),
                optional("rpe"),
            )
        }
    }
}
