package org.racedaydreams.domain.activity

import kotlin.jvm.JvmField

/** Existing extensible distance value and fixed core ordering; no training result is inferred. */
class PerformanceDistance(code: String?, private val distance: Double, label: String?) {
    private val identifier = requiredEvidenceText(code, "Distance code")
    private val displayLabel: String

    init {
        require(Regex("[a-z0-9_-]{1,32}").matches(identifier)) {
            "Distance code must contain lowercase letters, numbers, underscores or hyphens."
        }
        displayLabel = requiredEvidenceText(label, "Distance label")
        require(distance.isFinite() && distance > 0) { "Performance distance must be positive." }
    }

    fun code(): String = identifier

    fun metres(): Double = distance

    fun label(): String = displayLabel

    override fun equals(other: Any?): Boolean =
        other is PerformanceDistance &&
            identifier == other.identifier &&
            distance == other.distance &&
            displayLabel == other.displayLabel

    override fun hashCode(): Int {
        val bits = distance.toBits()
        val numberHash = (bits xor (bits ushr 32)).toInt()
        return (stableTextHash(identifier) * 31 + numberHash) * 31 + stableTextHash(displayLabel)
    }

    override fun toString(): String =
        "PerformanceDistance[code=$identifier, metres=$distance, label=$displayLabel]"

    companion object {
        @JvmField val ONE_K = PerformanceDistance("1k", 1000.0, "1K")
        @JvmField val ONE_MILE = PerformanceDistance("mile", 1609.344, "1 mile")
        @JvmField val TWO_K = PerformanceDistance("2k", 2000.0, "2K")
        @JvmField val THREE_K = PerformanceDistance("3k", 3000.0, "3K")
        @JvmField val FIVE_K = PerformanceDistance("5k", 5000.0, "5K")
        @JvmField val TEN_K = PerformanceDistance("10k", 10000.0, "10K")
        @JvmField val HALF_MARATHON = PerformanceDistance("half_marathon", 21097.5, "Half Marathon")
        @JvmField val MARATHON = PerformanceDistance("marathon", 42195.0, "Marathon")
        @JvmField
        val CORE: List<PerformanceDistance> =
            object : AbstractList<PerformanceDistance>() {
                private val rows =
                    listOf(ONE_K, ONE_MILE, TWO_K, THREE_K, FIVE_K, TEN_K, HALF_MARATHON, MARATHON)
                override val size: Int
                    get() = rows.size

                override fun get(index: Int): PerformanceDistance = rows[index]
            }
    }
}
