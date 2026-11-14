package org.racedaydreams.domain.activity

import org.racedaydreams.domain.time.*

/** Original retained evidence model; missing values remain unknown and snapshots immutable. */
data class ActivityRecord(
    private var id: String?,
    private val source: SourceIdentity?,
    private val start: Instant?,
    private val end: Instant?,
    private val startOffset: ZoneOffset?,
    private val endOffset: ZoneOffset?,
    private val distanceMetres: Double?,
    private val movingTime: Duration?,
    private val averageHeartRate: Double?,
    private val maxHeartRate: Double?,
    private val elevationGainMetres: Double?,
    private val cadence: Double?,
    private val rpe: Double?,
    private val notes: String?,
    private var samples: List<DistanceSample>?,
    private var laps: List<ActivityLap>?,
    private val plannedWorkoutId: String?,
    private val shoeId: String?,
) {
    init {
        id = ActivityChecks.required(id, "Activity ID")
        if (source == null) throw NullPointerException("Activity source")
        if (start == null) throw NullPointerException("Activity start")
        if (end == null) throw NullPointerException("Activity end")
        require(end.isAfter(start)) { "Activity end must follow start." }
        ActivityChecks.nonNegative(distanceMetres, "Activity distance")
        ActivityChecks.duration(movingTime, "Moving duration")
        require(movingTime == null || movingTime <= Durations.between(start, end)) {
            "Moving duration cannot exceed elapsed duration."
        }
        ActivityChecks.positive(averageHeartRate, "Average heart rate")
        ActivityChecks.positive(maxHeartRate, "Maximum heart rate")
        require(
            averageHeartRate == null || maxHeartRate == null || maxHeartRate >= averageHeartRate
        ) {
            "Maximum heart rate cannot be below its average."
        }
        ActivityChecks.nonNegative(elevationGainMetres, "Elevation gain")
        ActivityChecks.nonNegative(cadence, "Cadence")
        require(rpe == null || rpe.isFinite() && rpe >= 0 && rpe <= 10) {
            "RPE must be between 0 and 10."
        }
        samples = ActivityChecks.copy(samples)
        laps = ActivityChecks.copy(laps)
    }

    fun id(): String = id!!

    fun source(): SourceIdentity = source!!

    fun start(): Instant = start!!

    fun end(): Instant = end!!

    fun startOffset(): ZoneOffset? = startOffset

    fun endOffset(): ZoneOffset? = endOffset

    fun distanceMetres(): Double? = distanceMetres

    fun movingTime(): Duration? = movingTime

    fun averageHeartRate(): Double? = averageHeartRate

    fun maxHeartRate(): Double? = maxHeartRate

    fun elevationGainMetres(): Double? = elevationGainMetres

    fun cadence(): Double? = cadence

    fun rpe(): Double? = rpe

    fun notes(): String? = notes

    fun samples(): List<DistanceSample> = samples!!

    fun laps(): List<ActivityLap> = laps!!

    fun plannedWorkoutId(): String? = plannedWorkoutId

    fun shoeId(): String? = shoeId

    fun elapsedTime(): Duration = Durations.between(start(), end())

    override fun toString(): String =
        "ActivityRecord[id=$id, source=$source, start=$start, end=$end, startOffset=$startOffset, endOffset=$endOffset, distanceMetres=$distanceMetres, movingTime=$movingTime, averageHeartRate=$averageHeartRate, maxHeartRate=$maxHeartRate, elevationGainMetres=$elevationGainMetres, cadence=$cadence, rpe=$rpe, notes=$notes, samples=$samples, laps=$laps, plannedWorkoutId=$plannedWorkoutId, shoeId=$shoeId]"
}
