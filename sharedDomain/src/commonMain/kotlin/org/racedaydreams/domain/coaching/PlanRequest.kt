package org.racedaydreams.domain.coaching

import kotlin.jvm.JvmOverloads
import org.racedaydreams.domain.*
import org.racedaydreams.domain.baseline.RunnerBaseline
import org.racedaydreams.domain.strength.StrengthSession
import org.racedaydreams.domain.time.*

/**
 * Retained prescription/response snapshot; actual activity and explicit choices remain separate.
 */
data class PlanRequest
@JvmOverloads
constructor(
    private val profile: UserProfile?,
    private val baseline: RunnerBaseline?,
    private val startDate: LocalDate?,
    private var unavailableDates: Set<LocalDate>?,
    private var strengthSessions: List<StrengthSession>?,
    private val maximumMinutes: Int? = null,
    private val distribution: String? = "BALANCED",
) {

    init {
        require(
            (maximumMinutes == null || maximumMinutes in 15..360) &&
                distribution in listOf("FEWER_LONGER", "BALANCED", "MORE_SHORTER")
        ) {
            "Check session time and distribution preference"
        }
        if (profile == null || baseline == null || startDate == null) throw NullPointerException()
        unavailableDates = Immutable.copySet(unavailableDates ?: throw NullPointerException())
        strengthSessions = Immutable.copyList(strengthSessions ?: throw NullPointerException())
    }

    fun profile(): UserProfile = profile!!

    fun baseline(): RunnerBaseline = baseline!!

    fun startDate(): LocalDate = startDate!!

    fun unavailableDates(): Set<LocalDate> = unavailableDates!!

    fun strengthSessions(): List<StrengthSession> = strengthSessions!!

    fun maximumMinutes(): Int? = maximumMinutes

    fun distribution(): String = distribution!!

    override fun toString(): String =
        "PlanRequest[profile=$profile, baseline=$baseline, startDate=$startDate, unavailableDates=$unavailableDates, strengthSessions=$strengthSessions, maximumMinutes=$maximumMinutes, distribution=$distribution]"
}
