package org.racedaydreams.domain.strength

import org.racedaydreams.domain.time.LocalDate

/** Independent historical evidence, never a fabricated completed workout. */
data class HistoricalStrengthRecord(
    private val id: String?,
    private val exercise: StrengthExercise?,
    private val kilograms: Double,
    private val reps: Int,
    private val date: LocalDate?,
    private val precision: DatePrecision?,
    private val notes: String?,
    private val durationSeconds: Long?,
    private val distanceMetres: Double?,
) {
    enum class DatePrecision {
        EXACT,
        APPROXIMATE,
        UNKNOWN,
    }

    constructor(
        id: String?,
        exercise: StrengthExercise?,
        kilograms: Double,
        reps: Int,
        date: LocalDate?,
        precision: DatePrecision?,
        notes: String?,
    ) : this(id, exercise, kilograms, reps, date, precision, notes, null, null)

    init {
        if (exercise == null) throw NullPointerException()
        if (precision == null) throw NullPointerException()
        StrengthSession.ExerciseSets(
            exercise,
            listOf(
                StrengthSession.SetEntry(
                    id,
                    reps,
                    kilograms,
                    null,
                    false,
                    durationSeconds,
                    distanceMetres,
                )
            ),
        )
        require(notes == null || notes.length <= 10000) { "Notes are too long." }
        require(
            id != null &&
                id.trim { it <= ' ' }.isNotEmpty() &&
                kilograms.isFinite() &&
                kilograms >= 0 &&
                kilograms <= 1500 &&
                reps in 0..500 &&
                (precision == DatePrecision.UNKNOWN) == (date == null)
        ) {
            "Check historical record and date precision."
        }
    }

    fun id(): String = id!!

    fun exercise(): StrengthExercise = exercise!!

    fun kilograms(): Double = kilograms

    fun reps(): Int = reps

    fun date(): LocalDate? = date

    fun precision(): DatePrecision = precision!!

    fun notes(): String? = notes

    fun durationSeconds(): Long? = durationSeconds

    fun distanceMetres(): Double? = distanceMetres

    override fun toString(): String =
        "HistoricalStrengthRecord[id=$id, exercise=$exercise, kilograms=$kilograms, reps=$reps, date=$date, precision=$precision, notes=$notes, durationSeconds=$durationSeconds, distanceMetres=$distanceMetres]"
}
