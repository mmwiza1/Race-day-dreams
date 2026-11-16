package org.racedaydreams.storage

import kotlin.math.floor

/**
 * Legacy Android JSON spelling for the bounded non-negative Double measurements in actual sets.
 * Decimal digits are selected from exact binary-integer residues and rounding intervals. Native
 * Double.toString is deliberately not used: its shortest spelling can differ from legacy Java. This
 * is storage compatibility, never rounding/changing an athlete's retained measurements.
 */
internal object LegacyStrengthNumbers {
    private val powersOfFive = buildList {
        add(Magnitude.of(1))
        repeat(324) { add(last().times(5)) }
    }

    fun text(value: Double): String {
        require(value.isFinite() && value >= 0 && value <= 100000) { "Invalid actual measurement." }
        if (value.toBits() == Long.MIN_VALUE) return "-0"
        val integral = value.toLong()
        if (integral.toDouble() == value) return integral.toString()
        val bits = value.toBits()
        var fraction = bits and 0x000fffffffffffffL
        var exponent = ((bits ushr 52) and 0x7ff).toInt()
        val precision: Int
        if (exponent == 0) {
            val normalization = fraction.countLeadingZeroBits() - 11
            fraction = fraction shl normalization
            exponent = 1 - normalization
            precision = 64 - (bits and 0x000fffffffffffffL).countLeadingZeroBits()
        } else {
            fraction = fraction or (1L shl 52)
            precision = 53
        }
        exponent -= 1023
        val trailing = fraction.countTrailingZeroBits()
        val significant = 53 - trailing
        val tiny = maxOf(0, significant - exponent - 1)
        // Legacy normalization estimate (overestimates by at most one decimal place).
        val normalized = Double.fromBits(0x3ff0000000000000L or (fraction and 0x000fffffffffffffL))
        var decimal =
            floor((normalized - 1.5) * 0.289529654 + 0.176091259 + exponent * 0.301029995663981)
                .toInt()
        val numeratorFives = maxOf(0, -decimal)
        val denominatorFives = maxOf(0, decimal)
        var numeratorTwos = numeratorFives + tiny + exponent - (significant - 1)
        var denominatorTwos = denominatorFives + tiny
        var marginTwos = numeratorFives + tiny + exponent - precision
        val common = minOf(numeratorTwos, denominatorTwos)
        numeratorTwos -= common
        denominatorTwos -= common
        marginTwos -= common
        if (significant == 1) marginTwos--
        if (marginTwos < 0) {
            numeratorTwos -= marginTwos
            denominatorTwos -= marginTwos
            marginTwos = 0
        }
        var remainder =
            Magnitude.of(fraction ushr trailing)
                .multiply(powersOfFive[numeratorFives])
                .shift(numeratorTwos)
        val scale = powersOfFive[denominatorFives].shift(denominatorTwos)
        var margin = powersOfFive[numeratorFives].shift(marginTwos)
        val tenScale = scale.times(10)
        val digits = StringBuilder()
        fun next(): Int {
            var digit = 0
            while (remainder >= scale) {
                remainder = remainder.minus(scale)
                digit++
            }
            check(digit <= 9)
            remainder = remainder.times(10)
            margin = margin.times(10)
            return digit
        }
        var digit = next()
        var low = remainder < margin
        var high = remainder.plus(margin) >= tenScale
        if (digit == 0 && !high) decimal-- else digits.append(('0'.code + digit).toChar())
        if (decimal < -3 || decimal >= 8) {
            low = false
            high = false
        }
        while (!low && !high) {
            digit = next()
            digits.append(('0'.code + digit).toChar())
            low = remainder < margin
            high = remainder.plus(margin) >= tenScale
            check(digits.length <= 20)
        }
        val midpoint = remainder.times(2).compareTo(tenScale)
        if (
            high &&
                (!low || midpoint > 0 || midpoint == 0 && (digits.last().code - '0'.code) % 2 != 0)
        ) {
            var index = digits.lastIndex
            while (index >= 0 && digits[index] == '9') {
                digits[index] = '0'
                index--
            }
            if (index < 0) {
                digits[0] = '1'
                decimal++
            } else digits[index] = digits[index] + 1
        }
        val point = decimal + 1
        return when {
            point > 0 && point < 8 -> {
                if (digits.length < point)
                    digits.toString() + "0".repeat(point - digits.length) + ".0"
                else
                    digits.substring(0, point) +
                        "." +
                        (if (point < digits.length) digits.substring(point) else "0")
            }
            point <= 0 && point > -3 -> "0." + "0".repeat(-point) + digits
            else ->
                digits.substring(0, 1) +
                    "." +
                    (if (digits.length > 1) digits.substring(1) else "0") +
                    "E" +
                    decimal
        }
    }

    /** Small unsigned binary magnitude; all operations return independent immutable values. */
    private class Magnitude private constructor(private val limbs: IntArray) :
        Comparable<Magnitude> {
        override fun compareTo(other: Magnitude): Int {
            if (limbs.size != other.limbs.size) return limbs.size.compareTo(other.limbs.size)
            for (i in limbs.lastIndex downTo 0) if (limbs[i] != other.limbs[i])
                return limbs[i].compareTo(other.limbs[i])
            return 0
        }

        fun times(factor: Int): Magnitude {
            val result = IntArray(limbs.size + 1)
            var carry = 0L
            for (i in limbs.indices) {
                val value = limbs[i].toLong() * factor + carry
                result[i] = (value and MASK).toInt()
                carry = value ushr 30
            }
            result[limbs.size] = carry.toInt()
            return make(result)
        }

        fun multiply(other: Magnitude): Magnitude {
            val result = IntArray(limbs.size + other.limbs.size)
            for (i in limbs.indices) {
                var carry = 0L
                for (j in other.limbs.indices) {
                    val index = i + j
                    val value = limbs[i].toLong() * other.limbs[j] + result[index] + carry
                    result[index] = (value and MASK).toInt()
                    carry = value ushr 30
                }
                result[i + other.limbs.size] = carry.toInt()
            }
            return make(result)
        }

        fun shift(bits: Int): Magnitude {
            require(bits >= 0)
            val whole = bits / 30
            val partial = bits % 30
            val result = IntArray(limbs.size + whole + 1)
            var carry = 0L
            for (i in limbs.indices) {
                val value = (limbs[i].toLong() shl partial) or carry
                result[i + whole] = (value and MASK).toInt()
                carry = value ushr 30
            }
            result[limbs.size + whole] = carry.toInt()
            return make(result)
        }

        fun plus(other: Magnitude): Magnitude {
            val result = IntArray(maxOf(limbs.size, other.limbs.size) + 1)
            var carry = 0L
            for (i in 0 until result.lastIndex) {
                val value =
                    (limbs.getOrNull(i) ?: 0).toLong() + (other.limbs.getOrNull(i) ?: 0) + carry
                result[i] = (value and MASK).toInt()
                carry = value ushr 30
            }
            result[result.lastIndex] = carry.toInt()
            return make(result)
        }

        fun minus(other: Magnitude): Magnitude {
            check(this >= other)
            val result = IntArray(limbs.size)
            var borrow = 0L
            for (i in limbs.indices) {
                val value = limbs[i].toLong() - (other.limbs.getOrNull(i) ?: 0) - borrow
                result[i] = (value and MASK).toInt()
                borrow = if (value < 0) 1 else 0
            }
            return make(result)
        }

        companion object {
            private const val MASK = (1L shl 30) - 1

            fun of(value: Long): Magnitude {
                require(value >= 0)
                return make(intArrayOf((value and MASK).toInt(), (value ushr 30).toInt()))
            }

            private fun make(values: IntArray): Magnitude {
                var size = values.size
                while (size > 1 && values[size - 1] == 0) size--
                return Magnitude(values.copyOf(size))
            }
        }
    }
}
