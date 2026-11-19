package org.racedaydreams.storage

import org.racedaydreams.domain.strength.*

/**
 * Original CoachingJson field order, Android JSON number/escape spelling and Java UTF-8 bytes.
 * Existing receipt strings are never normalized or regenerated merely by reading them.
 */
object PortableLegacyStrengthFingerprint : LegacyStrengthFingerprint {
    override fun fingerprint(actual: StrengthSession): String =
        legacyEvidenceSha256(inputBytes(actual)).joinToString("") {
            (it.toInt() and 255).toString(16).padStart(2, '0')
        }

    fun input(actual: StrengthSession): String =
        obj(
            "id" to quoted(actual.id()),
            "startedAt" to quoted(actual.startedAt().toString()),
            "offset" to quoted(actual.offset().toString()),
            "duration" to quoted(actual.duration().toString()),
            "rpe" to number(actual.rpe()),
            "notes" to quoted(actual.notes()),
            "exercises" to
                actual.exercises().joinToString(",", "[", "]") { e ->
                    obj(
                        "exercise" to exercise(e.exercise()),
                        "sets" to
                            e.sets().joinToString(",", "[", "]") { s ->
                                obj(
                                    "id" to quoted(s.id()),
                                    "reps" to s.reps().toString(),
                                    "kg" to number(s.kilograms()),
                                    "rpe" to number(s.rpe()),
                                    "warmUp" to s.warmUp().toString(),
                                    "seconds" to (s.durationSeconds()?.toString() ?: "null"),
                                    "metres" to number(s.distanceMetres()),
                                    "rir" to number(s.repetitionsInReserve()),
                                )
                            },
                    )
                },
        )

    fun inputBytes(actual: StrengthSession): ByteArray = javaUtf8(input(actual))

    private fun exercise(e: StrengthExercise): String =
        obj(
            "id" to quoted(e.id()),
            "name" to quoted(e.name()),
            "region" to quoted(e.region().name),
            "mode" to quoted(e.mode().name),
            "estimate" to e.estimateOneRepMax().toString(),
            "measurement" to quoted(e.measurement().name),
        )

    private fun obj(vararg fields: Pair<String, String>): String =
        fields.joinToString(",", "{", "}") { quoted(it.first) + ":" + it.second }

    private fun number(value: Double?): String =
        value?.let { LegacyStrengthNumbers.text(it) } ?: "null"

    internal fun quoted(value: String): String = buildString {
        append('"')
        for (c in value) when (c) {
            '"',
            '\\',
            '/' -> {
                append('\\')
                append(c)
            }
            '\t' -> append("\\t")
            '\b' -> append("\\b")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\u000c' -> append("\\f")
            else ->
                if (c.code <= 0x1f) append("\\u" + c.code.toString(16).padStart(4, '0'))
                else append(c)
        }
        append('"')
    }

    internal fun javaUtf8(value: String): ByteArray {
        val normalized = buildString {
            var i = 0
            while (i < value.length) {
                val c = value[i++]
                if (c in '\uD800'..'\uDBFF') {
                    if (i < value.length && value[i] in '\uDC00'..'\uDFFF') {
                        append(c)
                        append(value[i++])
                    } else append('?')
                } else append(if (c in '\uDC00'..'\uDFFF') '?' else c)
            }
        }
        return normalized.encodeToByteArray()
    }
}

internal expect fun legacyEvidenceSha256(bytes: ByteArray): ByteArray
