package org.racedaydreams.domain.activity

import org.racedaydreams.domain.time.*

/** Original retained evidence model; missing values remain unknown and snapshots immutable. */
data class RecordedResult(
    private var id: String?,
    private val distance: PerformanceDistance?,
    private val duration: Duration?,
    private val date: LocalDate?,
    private val eventName: String?,
    private val source: ResultSource?,
    private val activityId: String?,
    private val note: String?,
) {
    init {
        id = ActivityChecks.required(id, "Result ID")
        if (distance == null) throw NullPointerException("Result distance")
        if (duration == null) throw NullPointerException("Result duration")
        require(!duration.isNegative() && !duration.isZero()) { "Result time must be positive." }
        if (date == null) throw NullPointerException("Result date")
        if (source == null) throw NullPointerException("Result source")
        require(
            source != ResultSource.ROLLING ||
                activityId != null && activityId.trim { it <= ' ' }.isNotEmpty()
        ) {
            "Rolling efforts must link an activity."
        }
    }

    fun id(): String = id!!

    fun distance(): PerformanceDistance = distance!!

    fun duration(): Duration = duration!!

    fun date(): LocalDate = date!!

    fun eventName(): String? = eventName

    fun source(): ResultSource = source!!

    fun activityId(): String? = activityId

    fun note(): String? = note

    override fun toString(): String =
        "RecordedResult[id=$id, distance=$distance, duration=$duration, date=$date, eventName=$eventName, source=$source, activityId=$activityId, note=$note]"
}
