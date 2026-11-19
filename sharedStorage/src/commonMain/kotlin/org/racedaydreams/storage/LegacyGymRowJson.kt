package org.racedaydreams.storage

import kotlinx.serialization.json.*

/** Original Android row-byte budget for validated gym DTOs, not arbitrary measurement JSON. */
object LegacyGymRowJson {
    const val MAX_BYTES = 262144

    /** Reuses verified original number, slash/control escaping and Java UTF-8 replacement rules. */
    fun bytes(value: JsonElement): ByteArray =
        PortableLegacyStrengthFingerprint.javaUtf8(render(value))

    internal fun check(value: JsonElement) {
        require(bytes(value).size <= MAX_BYTES) {
            "This gym record exceeds the supported 256 KiB row limit. Existing staged sets were retained."
        }
    }

    private fun render(value: JsonElement): String =
        when (value) {
            JsonNull -> "null"
            is JsonObject ->
                value.entries.joinToString(",", "{", "}") {
                    PortableLegacyStrengthFingerprint.quoted(it.key) + ":" + render(it.value)
                }
            is JsonArray -> value.joinToString(",", "[", "]") { render(it) }
            is JsonPrimitive ->
                when {
                    value.isString -> PortableLegacyStrengthFingerprint.quoted(value.content)
                    value.booleanOrNull != null -> value.content
                    value.content.toLongOrNull() != null -> value.content.toLong().toString()
                    else -> LegacyStrengthNumbers.text(value.double)
                }
        }
}
