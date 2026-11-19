package org.racedaydreams.domain.activity

/** Frozen original analysis and its qualification; never upgrades uncertain evidence. */
data class SplitAnalysis(
    private var splits: List<RunSplit>?,
    private val quality: SampleQuality?,
    private var reasons: List<String>?,
) {
    init {
        splits = ActivityChecks.copy(splits)
        if (quality == null) throw NullPointerException("Split quality")
        reasons = ActivityChecks.copy(reasons)
    }

    fun splits(): List<RunSplit> = splits!!

    fun quality(): SampleQuality = quality!!

    fun reasons(): List<String> = reasons!!

    override fun toString(): String =
        "SplitAnalysis[splits=$splits, quality=$quality, reasons=$reasons]"
}
