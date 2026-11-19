package org.racedaydreams.domain.presentation

/** Canonical decimal half-up for existing UK labels; never rounds retained measurements. */
internal fun fixedDecimalText(value: Double, places: Int): String {
    require(places in 0..2)
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value < 0) "-Infinity" else "Infinity"
    val negative = value.toBits() < 0
    val text = kotlin.math.abs(value).toString().lowercase()
    val mantissa = text.substringBefore('e')
    val exponent = text.substringAfter('e', "0").toInt()
    val point = mantissa.indexOf('.').let { if (it < 0) mantissa.length else it } + exponent
    val digits = mantissa.replace(".", "")
    val cut = point + places
    var rounded =
        when {
            cut < 0 -> "0"
            cut == 0 -> if (digits.first() >= '5') "1" else "0"
            cut >= digits.length -> digits.padEnd(cut, '0')
            else -> {
                val prefix = digits.take(cut).toCharArray()
                if (digits[cut] >= '5') {
                    var i = prefix.lastIndex
                    while (i >= 0 && prefix[i] == '9') {
                        prefix[i] = '0'
                        i--
                    }
                    if (i < 0) "1" + prefix.concatToString()
                    else {
                        prefix[i]++
                        prefix.concatToString()
                    }
                } else prefix.concatToString()
            }
        }
    rounded = rounded.trimStart('0').ifEmpty { "0" }.padStart(places + 1, '0')
    return (if (negative) "-" else "") +
        if (places == 0) rounded else rounded.dropLast(places) + "." + rounded.takeLast(places)
}
