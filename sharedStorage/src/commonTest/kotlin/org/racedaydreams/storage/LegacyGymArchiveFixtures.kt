package org.racedaydreams.storage

import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.time.*

internal object LegacyGymArchiveFixtures {
    val at = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")
    val start = CalendarDates.of(2026, 11, 17)

    fun programme(version: Int = 1, id: String = "programme"): GymProgramme =
        GymProgramme(
            id,
            version,
            at,
            "Athlete accepted",
            GymProgramme.Settings(
                GymProgramme.Goal.STRENGTH,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(DayOfWeek.TUESDAY),
                1,
                setOf("full gym"),
                45,
                GymProgramme.Effort.RIR,
            ),
            start,
            start.plusDays(41),
            listOf(
                GymProgramme.Session(
                    "session",
                    start,
                    "Upper",
                    listOf(
                        GymProgramme.Prescription(
                            "bench-press",
                            2,
                            4,
                            6,
                            null,
                            null,
                            6,
                            8,
                            180,
                            "Retained intention",
                            82.25,
                        )
                    ),
                    "Original explanation",
                )
            ),
            listOf("Retained trade-off"),
        )
}
