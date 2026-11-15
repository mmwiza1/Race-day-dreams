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
        Syntax(input).validate()
        return Json.parseToJsonElement(input) as? JsonObject ?: invalid("root object")
    }

    private fun checkSize(input: String) {
        if (input.length > MAX_BYTES || input.encodeToByteArray().size > MAX_BYTES)
            invalid("payload size")
    }

    private class Syntax(private val input: String) {
        private var at = 0
        private var tokens = 0

        fun validate() {
            whitespace()
            if (at >= input.length || input[at] != '{') invalid("root object")
            item(0)
            whitespace()
            if (at != input.length) invalid("trailing data")
        }

        private fun item(depth: Int) {
            if (depth > MAX_DEPTH || ++tokens > MAX_TOKENS) invalid("structure limit")
            whitespace()
            if (at == input.length) invalid("unexpected end")
            when (input[at]) {
                '{' -> {
                    at++
                    val keys = mutableSetOf<String>()
                    whitespace()
                    if (take('}')) return
                    do {
                        whitespace()
                        val start = at
                        string()
                        val key =
                            (Json.parseToJsonElement(input.substring(start, at)) as JsonPrimitive)
                                .content
                        if (!keys.add(key)) invalid("duplicate field")
                        whitespace()
                        if (!take(':')) invalid("object separator")
                        item(depth + 1)
                        whitespace()
                        if (take('}')) return
                    } while (take(','))
                    invalid("object ending")
                }
                '[' -> {
                    at++
                    whitespace()
                    if (take(']')) return
                    do {
                        item(depth + 1)
                        whitespace()
                        if (take(']')) return
                    } while (take(','))
                    invalid("array ending")
                }
                '"' -> string()
                't' -> literal("true")
                'f' -> literal("false")
                'n' -> literal("null")
                else -> number()
            }
        }

        private fun string() {
            if (!take('"')) invalid("quoted string")
            val start = at
            while (at < input.length) {
                val c = input[at++]
                if (at - start > 100000) invalid("string limit")
                if (c == '"') return
                if (c < ' ') invalid("string control character")
                if (c != '\\') continue
                if (at == input.length) invalid("string escape")
                val escaped = input[at++]
                if (escaped == 'u') {
                    repeat(4) {
                        if (at == input.length || input[at++].digitToIntOrNull(16) == null)
                            invalid("unicode escape")
                    }
                } else if (escaped !in "\"\\/bfnrt") invalid("string escape")
            }
            invalid("unfinished string")
        }

        private fun number() {
            take('-')
            if (!take('0')) digits()
            if (take('.')) digits()
            if (take('e') || take('E')) {
                if (!take('+')) take('-')
                digits()
            }
        }

        private fun digits() {
            val start = at
            while (at < input.length && input[at] in '0'..'9') at++
            if (start == at) invalid("number")
        }

        private fun literal(value: String) {
            if (!input.startsWith(value, at)) invalid("literal")
            at += value.length
        }

        private fun whitespace() {
            while (at < input.length && input[at] in " \t\r\n") at++
        }

        private fun take(value: Char): Boolean {
            if (at < input.length && input[at] == value) {
                at++
                return true
            }
            return false
        }
    }
}
