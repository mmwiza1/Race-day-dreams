package org.racedaydreams.domain.activity

import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.time.*

/** Original retained evidence model; missing values remain unknown and snapshots immutable. */
data class PersonalBestEntry(
    private val result: RecordedResult?,
    private val previousBest: RecordedResult?,
    private val improvement: Duration?,
) {
    init {
        if (result == null) throw NullPointerException("PB result")
        require((previousBest == null) == (improvement == null)) {
            "First PBs have no invented previous time or improvement."
        }
        require(improvement == null || !improvement.isNegative() && !improvement.isZero()) {
            "A PB improvement must be positive."
        }
    }

    fun result(): RecordedResult = result!!

    fun previousBest(): RecordedResult? = previousBest

    fun improvement(): Duration? = improvement

    fun pace(units: UnitSystem?): String =
        PerformancePace.format(result().duration(), result().distance(), units)

    override fun toString(): String =
        "PersonalBestEntry[result=$result, previousBest=$previousBest, improvement=$improvement]"
}
