package org.racedaydreams.storage

import kotlinx.serialization.json.*

/** Shared strict scanner; callers retain their original independent allocation budgets. */
internal class StoredJsonSyntax(
    private val input: String,
    private val maxDepth: Int,
    private val maxValues: Int,
    private val maxStringCharacters: Int,
    private val invalid: (String) -> Nothing,
) {
    private var at = 0
    private var tokens = 0
    private var captureField: String? = null
    private var capturedValue: String? = null

    /** Retain the exact value spelling after full strict validation, never JSON reserialization. */
    fun validatedRootField(name: String): String? {
        captureField = name
        validate()
        return capturedValue
    }

    fun validate() {
        whitespace()
        if (at >= input.length || input[at] != '{') invalid("root object")
        item(0)
        whitespace()
        if (at != input.length) invalid("trailing data")
    }

    private fun item(depth: Int) {
        if (depth > maxDepth || ++tokens > maxValues) invalid("structure limit")
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
                    whitespace()
                    val valueStart = at
                    item(depth + 1)
                    if (depth == 0 && key == captureField)
                        capturedValue = input.substring(valueStart, at)
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
            if (at - start > maxStringCharacters) invalid("string limit")
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
