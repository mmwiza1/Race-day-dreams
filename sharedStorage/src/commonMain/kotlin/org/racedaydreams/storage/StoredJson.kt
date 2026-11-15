package org.racedaydreams.storage

import kotlinx.serialization.json.*

/** Explicit compatible value types: absent/null is unknown, never a coerced zero or boolean. */
internal object StoredJson {
    fun objectValue(text: String): JsonObject =
        Json.parseToJsonElement(text) as? JsonObject
            ?: throw IllegalArgumentException("Expected one complete JSON object.")

    fun JsonObject.text(key: String): String? {
        val value = this[key]
        if (value == null || value == JsonNull) return null
        require(value is JsonPrimitive && value.isString) { "Expected text for $key." }
        return value.content
    }

    fun JsonObject.required(key: String): String {
        val value = text(key)
        require(value != null && value.length <= 256 && value.any { it.code > 0x20 }) {
            "Invalid $key."
        }
        return value
    }

    fun JsonObject.number(key: String): Double? {
        val value = this[key]
        if (value == null || value == JsonNull) return null
        require(value is JsonPrimitive && !value.isString) { "Expected a number for $key." }
        val number = value.doubleOrNull
        require(number != null && number.isFinite()) { "Expected a finite number for $key." }
        return number
    }

    fun JsonObject.requiredNumber(key: String): Double =
        number(key) ?: throw IllegalArgumentException("Missing number for $key.")

    fun JsonObject.bool(key: String): Boolean {
        val value = this[key]
        require(value is JsonPrimitive && !value.isString) { "Expected true or false for $key." }
        return value.booleanOrNull
            ?: throw IllegalArgumentException("Expected true or false for $key.")
    }

    fun JsonObject.array(key: String, maximum: Int): JsonArray {
        val rows =
            this[key] as? JsonArray ?: throw IllegalArgumentException("Expected array for $key.")
        require(rows.size <= maximum) { "Too many $key; no records were truncated." }
        return rows
    }

    fun JsonElement.row(): JsonObject =
        this as? JsonObject ?: throw IllegalArgumentException("Expected object row.")

    fun JsonObject.string(key: String): String =
        text(key) ?: throw IllegalArgumentException("Missing text for $key.")

    fun nullable(value: String?): JsonElement = value?.let { JsonPrimitive(it) } ?: JsonNull

    fun nullable(value: Double?): JsonElement = value?.let { JsonPrimitive(it) } ?: JsonNull
}
