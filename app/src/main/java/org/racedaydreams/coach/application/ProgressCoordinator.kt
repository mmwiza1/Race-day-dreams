package org.racedaydreams.coach.application

import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import org.racedaydreams.data.DevelopmentStore
import org.racedaydreams.domain.body.BodyWeight

/** Optional body-weight history; missing or future evidence is never fabricated. */
class ProgressCoordinator(
    private val operations: CoachOperationRunner,
    private val development: DevelopmentStore,
) {
    fun saveWeight(value: Double,date: LocalDate,id: String? = null) = operations.submit {
        require(date <= LocalDate.now()) { "A measurement cannot be in the future." }
        val at = date.atTime(12,0).atZone(ZoneId.systemDefault())
        val identity=id ?: UUID.randomUUID().toString()
        development.saveWeight(BodyWeight(identity,operations.current.massUnit.toKilograms(value),at.toInstant(),at.offset,"manual",identity,null))
    }

    fun deleteWeight(id:String) = operations.submit { development.deleteWeight(id) }

}
