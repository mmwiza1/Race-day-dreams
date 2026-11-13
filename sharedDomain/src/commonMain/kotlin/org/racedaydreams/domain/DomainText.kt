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
