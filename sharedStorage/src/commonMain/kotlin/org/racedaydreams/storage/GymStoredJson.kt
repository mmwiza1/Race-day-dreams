package org.racedaydreams.storage

import kotlinx.serialization.json.*

/** Existing strict gym envelope guards. Validate bounded syntax before allocating a DOM. */
internal object GymStoredJson {
    const val MAX_BYTES = 16 * 1024 * 1024
    private const val MAX_DEPTH = 20
    private const val MAX_TOKENS = 1000000

    fun invalid(detail: String): Nothing =
        throw IllegalArgumentException("Invalid gym JSON: $detail.")

    fun fields(row: JsonObject, vararg expected: String) {
        if (row.keys != expected.toSet()) invalid("required/unknown fields")
    }

    fun value(row: JsonObject, key: String): JsonElement = row[key] ?: invalid("missing $key")

    fun isNull(row: JsonObject, key: String): Boolean = value(row, key) == JsonNull

    fun child(row: JsonObject, key: String): JsonObject =
        value(row, key) as? JsonObject ?: invalid("$key object")

    fun row(value: JsonElement): JsonObject = value as? JsonObject ?: invalid("array object")

    fun array(row: JsonObject, key: String, maximum: Int): JsonArray {
        val result = value(row, key) as? JsonArray ?: invalid("$key array")
        if (result.size > maximum) invalid("$key limit")
        return result
    }

    fun text(row: JsonObject, key: String, maximum: Int): String {
        val value = value(row, key)
        if (value !is JsonPrimitive || !value.isString || value.content.length > maximum)
            invalid("$key text")
        return value.content
    }

    fun nullableText(row: JsonObject, key: String, maximum: Int): String? =
        if (isNull(row, key)) null else text(row, key, maximum)

    fun bool(row: JsonObject, key: String): Boolean =
        with(StoredJson) {
            value(row, key)
            row.bool(key)
        }

    fun whole(row: JsonObject, key: String): Long =
        with(StoredJson) {
            value(row, key)
            row.exactLong(key)
        }

    fun integer(row: JsonObject, key: String): Int {
        val value = whole(row, key)
        if (value < Int.MIN_VALUE || value > Int.MAX_VALUE) invalid("$key integer range")
        return value.toInt()
    }

    fun nullableInteger(row: JsonObject, key: String): Int? =
        if (isNull(row, key)) null else integer(row, key)

    fun nullableLong(row: JsonObject, key: String): Long? =
        if (isNull(row, key)) null else whole(row, key)

    fun number(row: JsonObject, key: String): Double =
        with(StoredJson) {
            value(row, key)
            row.requiredNumber(key)
        }

    fun nullableNumber(row: JsonObject, key: String): Double? =
        if (isNull(row, key)) null else number(row, key)

    inline fun <reified T : Enum<T>> choice(row: JsonObject, key: String): T =
        enumValueOf(text(row, key, 64))

    fun version(row: JsonObject, expected: Int) {
        require(integer(row, "version") == expected) { "Unsupported gym payload version." }
    }

    fun write(row: JsonObject): String = row.toString().also { checkSize(it) }

    fun read(input: String): JsonObject {
        checkSize(input)
        StoredJsonSyntax(input, MAX_DEPTH, MAX_TOKENS, 100000, ::invalid).validate()
        return Json.parseToJsonElement(input) as? JsonObject ?: invalid("root object")
    }

    private fun checkSize(input: String) {
        if (input.length > MAX_BYTES || input.encodeToByteArray().size > MAX_BYTES)
            invalid("payload size")
    }
}
