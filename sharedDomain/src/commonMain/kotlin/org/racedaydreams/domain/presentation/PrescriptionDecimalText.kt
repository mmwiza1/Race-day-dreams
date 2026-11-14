package org.racedaydreams.domain.presentation

/** Expand the platform's shortest round-trip decimal without rounding or a precision cap. */
internal fun expandPrescriptionDecimal(rawText: String): String {
    val raw = rawText.lowercase()
    val negative = raw.startsWith('-')
    val unsigned = raw.removePrefix("-")
    val mantissa = unsigned.substringBefore('e')
    val exponent = if ('e' in unsigned) unsigned.substringAfter('e').toInt() else 0
    val digits = mantissa.replace(".", "")
    val point = mantissa.substringBefore('.').length + exponent
    val expanded =
        when {
            point <= 0 -> "0." + "0".repeat(-point) + digits
            point >= digits.length -> digits + "0".repeat(point - digits.length)
            else -> digits.substring(0, point) + "." + digits.substring(point)
        }
    val result = normalizeDecimal(expanded)
    return if (negative && result != "0") "-$result" else result
}

/** Exact decimal subtraction of validated non-negative optional load references. */
internal fun differencePrescriptionDecimals(a: String, b: String): String {
    val scale = maxOf(a.substringAfter('.', "").length, b.substringAfter('.', "").length)
    fun scaled(s: String): String =
        (s.substringBefore('.') + s.substringAfter('.', "").padEnd(scale, '0'))
            .trimStart('0')
            .ifEmpty { "0" }
    var left = scaled(a)
    var right = scaled(b)
    if (left.length < right.length || left.length == right.length && left < right) {
        val swap = left
        left = right
        right = swap
    }
    right = right.padStart(left.length, '0')
    val result = CharArray(left.length)
    var borrow = 0
    for (i in left.indices.reversed()) {
        var digit = (left[i] - '0') - (right[i] - '0') - borrow
        borrow = if (digit < 0) 1 else 0
        if (digit < 0) digit += 10
        result[i] = '0' + digit
    }
    val digits = result.concatToString().padStart(scale + 1, '0')
    return normalizeDecimal(
        if (scale == 0) digits else digits.dropLast(scale) + "." + digits.takeLast(scale)
    )
}

private fun normalizeDecimal(value: String): String {
    val whole = value.substringBefore('.').trimStart('0').ifEmpty { "0" }
    val fraction = value.substringAfter('.', "").trimEnd('0')
    return if (fraction.isEmpty()) whole else "$whole.$fraction"
}
