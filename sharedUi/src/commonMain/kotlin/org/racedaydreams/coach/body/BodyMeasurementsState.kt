package org.racedaydreams.coach.body

import org.racedaydreams.domain.body.*

/** Existing screen state; permission identifiers and read visibility stay platform-owned. */
data class BodyMeasurementsState(
    val busy: Boolean = true,
    val heights: List<BodyHeight> = emptyList(),
    val moreHeights: Boolean = false,
    val weights: List<BodyWeight> = emptyList(),
    val preferences: BodyMeasurementsRepository.Preferences =
        BodyMeasurementsRepository.Preferences.defaults(),
    val advice: ProteinGuidance.Advice? = null,
    val available: Boolean = false,
    val granted: Set<String> = emptySet(),
    val error: String? = null,
    val notice: String? = null,
)
