package org.racedaydreams.coach.application

import org.racedaydreams.domain.body.BodyWeight
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate
import org.racedaydreams.domain.time.ZoneOffset
import org.racedaydreams.storage.SqliteCoachingContext

/** Optional body-weight history; missing or future evidence is never fabricated. */
class ProgressCoordinator(
    private val operations: CoachOperationRunner,
    private val development: SqliteCoachingContext,
    private val today: () -> LocalDate,
    private val localNoon: (LocalDate) -> Pair<Instant, ZoneOffset>,
    private val newId: () -> String,
) {
    fun saveWeight(value: Double, date: LocalDate, id: String? = null) =
        operations.submit {
            require(date.toEpochDay() <= today().toEpochDay()) {
                "A measurement cannot be in the future."
            }
            val at = localNoon(date)
            val identity = id ?: newId()
            development.saveWeight(
                BodyWeight(
                    identity,
                    operations.current.massUnit.toKilograms(value),
                    at.first,
                    at.second,
                    "manual",
                    identity,
                    null,
                )
            )
        }

    fun deleteWeight(id: String) = operations.submit { development.deleteWeight(id) }
}
