package org.racedaydreams.domain.activity

/** Frozen original analysis and its qualification; never upgrades uncertain evidence. */
data class BestEffortAnalysis(
    private var efforts: List<BestEffort>?,
    private val quality: SampleQuality?,
    private var reasons: List<String>?,
) {
    init {
        efforts = ActivityChecks.copy(efforts)
        if (quality == null) throw NullPointerException("Sample quality")
        reasons = ActivityChecks.copy(reasons)
    }

    fun efforts(): List<BestEffort> = efforts!!

    fun quality(): SampleQuality = quality!!

    fun reasons(): List<String> = reasons!!

    override fun toString(): String =
        "BestEffortAnalysis[efforts=$efforts, quality=$quality, reasons=$reasons]"
}
