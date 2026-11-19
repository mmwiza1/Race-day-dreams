package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.storage.StoredJson.exactLong
import org.racedaydreams.storage.StoredJson.string

/**
 * Supported root1–13 file envelope. A COMPLETE snapshot validator is mandatory: this boundary does
 * not implement replacement, raw archive restoration, consent or section-level graph validation.
 * Platform file adapters must bound their reads before constructing this byte array.
 */
object BackupEnvelopeJson {
    const val FORMAT = "race-day-dreams-backup"
    const val VERSION = 13
    const val MAX_BYTES = 32 * 1024 * 1024
    private const val MAX_STRING_CHARACTERS = 1024 * 1024

    data class Checked<T>(val version: Int, val createdAt: Instant, val snapshot: T)

    fun <T> inspect(bytes: ByteArray, validateSnapshot: (JsonObject) -> T): Checked<T> {
        require(bytes.size <= MAX_BYTES) { "The backup exceeds the supported 32 MiB limit." }
        // Android JSONTokener accepts one UTF-8 BOM at the beginning of an imported document.
        val text = bytes.decodeToString(throwOnInvalidSequence = true).removePrefix("\uFEFF")
        complexity(text)
        StoredJsonSyntax(text, 32, Int.MAX_VALUE, MAX_STRING_CHARACTERS, ::invalid).validate()
        val envelope = Json.parseToJsonElement(text) as? JsonObject ?: invalid("root object")
        require(envelope.keys == setOf("format", "version", "createdAt", "data")) {
            "This backup contains missing or unsupported envelope fields."
        }
        require(envelope.string("format") == FORMAT) { "This is not a Race Day Dreams backup." }
        val version = envelope.exactLong("version")
        require(version in 1..VERSION.toLong()) {
            "This backup version is not supported by this app."
        }
        val data = envelope["data"] as? JsonObject ?: invalid("data object")
        for ((minimum, sections) in requiredSections) {
            if (version >= minimum)
                for (section in sections) require(section in data) {
                    "This backup is missing its $section section."
                }
        }
        val createdAt = StoredInstant.parse(envelope.string("createdAt"))
        return Checked(version.toInt(), createdAt, validateSnapshot(data))
    }

    /**
     * Capture a consistent bounded snapshot first; bytes are returned only after full validation.
     */
    fun encode(
        data: JsonObject,
        createdAt: Instant,
        validateSnapshot: (JsonObject) -> Unit,
    ): ByteArray {
        val bytes =
            buildJsonObject {
                    put("format", FORMAT)
                    put("version", VERSION)
                    put("createdAt", createdAt.toString())
                    put("data", data)
                }
                .toString()
                .encodeToByteArray(throwOnInvalidSequence = true)
        inspect(bytes, validateSnapshot)
        return bytes
    }

    private val requiredSections =
        listOf(
            2 to listOf("coaching"),
            3 to listOf("development"),
            4 to listOf("strengthState", "gymProgramming"),
            6 to listOf("support"),
            7 to listOf("release08"),
            8 to listOf("strengthEngine"),
            9 to listOf("strengthProgramming"),
            10 to listOf("gymTools"),
            11 to listOf("achievements"),
            12 to listOf("bodyMeasurements"),
            13 to listOf("detailRetention"),
        )

    /** Same original root budgets; standalone gym limits must not narrow whole-backup support. */
    private fun complexity(text: String) {
        var quoted = false
        var escaped = false
        var depth = 0
        var structuralTokens = 0
        var stringLength = 0
        for (value in text) {
            if (quoted) {
                require(++stringLength <= MAX_STRING_CHARACTERS) {
                    "A backup text field is too large."
                }
                if (escaped) escaped = false
                else if (value == '\\') escaped = true else if (value == '"') quoted = false
            } else
                when (value) {
                    '"' -> {
                        quoted = true
                        stringLength = 0
                    }
                    '{',
                    '[' -> {
                        require(++depth <= 32) { "The backup nesting is too deep." }
                        structuralTokens++
                    }
                    '}',
                    ']' -> {
                        require(--depth >= 0) { "The backup has unbalanced JSON structure." }
                        structuralTokens++
                    }
                    ',',
                    ':' -> structuralTokens++
                }
            require(structuralTokens <= 2000000) { "The backup contains too many records." }
        }
        require(!quoted && depth == 0) { "The backup is truncated." }
    }

    private fun invalid(detail: String): Nothing =
        throw IllegalArgumentException("Invalid backup JSON: $detail.")
}
