package org.racedaydreams.domain

/**
 * Original JDK String.isBlank boundary for retained authored text. Non-breaking spaces are not
 * blank.
 */
internal fun isJavaBlank(value: String): Boolean =
    value.all { c ->
        c in '\t'..'\r' ||
            c in '\u001c'..' ' ||
            c == '\u1680' ||
            c in '\u2000'..'\u2006' ||
            c in '\u2008'..'\u200a' ||
            c == '\u2028' ||
            c == '\u2029' ||
            c == '\u205f' ||
            c == '\u3000'
    }

// StandardCharsets.UTF_8 replaces isolated UTF-16 surrogates with '?' rather than U+FFFD.
// Persisted source IDs must hash identically on both platforms, even for such old raw input.
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
