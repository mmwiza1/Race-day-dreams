package org.racedaydreams.health

/** Provider availability only; AVAILABLE does not assert permission or complete read coverage. */
enum class HealthAvailability {
    AVAILABLE,
    UPDATE_REQUIRED,
    UNAVAILABLE,
}
