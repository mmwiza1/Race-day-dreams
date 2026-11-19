package org.racedaydreams.domain.strength

/** Original input rules and duration wording in platform-independent canonical seconds. */
object StrengthMeasurementPolicy {
    fun seconds(input: String): Long {
        val pieces = input.trim { it <= ' ' }.split(':')
        fun invalid(): Nothing = throw IllegalArgumentException("Enter seconds or minutes:seconds.")
        if (pieces.size !in 1..2) invalid()
        val first = pieces[0].toLongOrNull() ?: invalid()
        val value =
            if (pieces.size == 1) first
            else {
                val last = pieces[1].toLongOrNull() ?: invalid()
                if (first < 0 || last < 0 || last >= 60 || first > (Long.MAX_VALUE - last) / 60)
                    invalid()
                first * 60 + last
            }
        require(value > 0 && value <= 43200) { "Duration must be positive and at most 12 hours." }
        return value
    }

    fun duration(seconds: Long): String =
        if (seconds < 60) "$seconds sec"
        else "${seconds / 60} min" + if (seconds % 60 == 0L) "" else " ${seconds % 60} sec"
}
