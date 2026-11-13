package org.racedaydreams.domain

import org.racedaydreams.domain.time.DayOfWeek

/** Existing independent running/gym availability and validation, without a platform calendar. */
class TrainingPreferences(
    strategy: TrainingStrategy?,
    runningDays: Set<DayOfWeek?>?,
    private val runsPerWeek: Int,
    private val longRunDay: DayOfWeek?,
    gymDays: Set<DayOfWeek?>?,
    scheduleType: ScheduleType?,
    units: UnitSystem?,
) {
    private val strategy = strategy ?: throw NullPointerException("Choose a training strategy.")
    private val scheduleType =
        scheduleType ?: throw NullPointerException("Choose a schedule preference.")
    private val units = units ?: throw NullPointerException("Choose display units.")
    private val runningDays = immutableDays(runningDays, "Running days are required.")
    private val gymDays =
        immutableDays(gymDays, "Gym days are required (an empty set means no gym days).")

    init {
        require(runsPerWeek in 1..7) { "Choose between 1 and 7 runs per week." }
        require(runsPerWeek <= this.runningDays.size) {
            "Choose at least as many preferred running days as runs per week."
        }
        require(longRunDay == null || longRunDay in this.runningDays) {
            "Your preferred long-run day must be a preferred running day."
        }
    }

    fun strategy(): TrainingStrategy = strategy

    fun runningDays(): Set<DayOfWeek> = runningDays

    fun runsPerWeek(): Int = runsPerWeek

    fun longRunDay(): DayOfWeek? = longRunDay

    fun gymDays(): Set<DayOfWeek> = gymDays

    fun scheduleType(): ScheduleType = scheduleType

    fun units(): UnitSystem = units

    override fun equals(other: Any?): Boolean =
        other is TrainingPreferences &&
            strategy == other.strategy &&
            runningDays == other.runningDays &&
            runsPerWeek == other.runsPerWeek &&
            longRunDay == other.longRunDay &&
            gymDays == other.gymDays &&
            scheduleType == other.scheduleType &&
            units == other.units

    override fun hashCode(): Int {
        var result = strategy.hashCode()
        result = result * 31 + runningDays.hashCode()
        result = result * 31 + runsPerWeek
        result = result * 31 + (longRunDay?.hashCode() ?: 0)
        result = result * 31 + gymDays.hashCode()
        result = result * 31 + scheduleType.hashCode()
        return result * 31 + units.hashCode()
    }

    override fun toString(): String =
        "TrainingPreferences[strategy=$strategy, runningDays=$runningDays, runsPerWeek=$runsPerWeek, longRunDay=$longRunDay, gymDays=$gymDays, scheduleType=$scheduleType, units=$units]"
}

private fun immutableDays(days: Set<DayOfWeek?>?, message: String): Set<DayOfWeek> {
    if (days == null) throw NullPointerException(message)
    val copy =
        days
            .map { it ?: throw NullPointerException("Days cannot contain null.") }
            .distinct()
            .sortedBy { it.ordinal }
    // EnumSet iteration is ISO order on the original JVM API, irrespective of input order.
    return object : AbstractSet<DayOfWeek>() {
        override val size: Int
            get() = copy.size

        override fun iterator(): Iterator<DayOfWeek> =
            object : Iterator<DayOfWeek> {
                private var index = 0

                override fun hasNext(): Boolean = index < copy.size

                override fun next(): DayOfWeek {
                    if (!hasNext()) throw NoSuchElementException()
                    return copy[index++]
                }
            }
    }
}
