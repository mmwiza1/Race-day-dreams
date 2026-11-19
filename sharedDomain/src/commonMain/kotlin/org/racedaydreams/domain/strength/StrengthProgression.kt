package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.CalendarDates
import org.racedaydreams.domain.time.LocalDate

/** Original presentation projection. Unknown historical dates never gain invented chronology. */
object StrengthProgression {
    data class Point(
        private val id: String?,
        private val exerciseId: String?,
        private val category: String?,
        private val value: Double,
        private val kilograms: Double,
        private val reps: Int,
        private val date: LocalDate?,
        private val precision: HistoricalStrengthRecord.DatePrecision?,
        private val provenance: String?,
        private val sessionId: String?,
        private val notes: String?,
    ) {
        fun id(): String? = id

        fun exerciseId(): String? = exerciseId

        fun category(): String? = category

        fun value(): Double = value

        fun kilograms(): Double = kilograms

        fun reps(): Int = reps

        fun date(): LocalDate? = date

        fun precision(): HistoricalStrengthRecord.DatePrecision? = precision

        fun provenance(): String? = provenance

        fun sessionId(): String? = sessionId

        fun notes(): String? = notes

        override fun toString(): String =
            "Point[id=$id, exerciseId=$exerciseId, category=$category, value=$value, kilograms=$kilograms, reps=$reps, date=$date, precision=$precision, provenance=$provenance, sessionId=$sessionId, notes=$notes]"
    }

    @JvmStatic
    fun timeline(
        sessions: List<StrengthSession>,
        manual: List<HistoricalStrengthRecord>,
    ): List<Point> {
        val out = mutableListOf<Point>()
        val byId = mutableMapOf<String, StrengthSession>()
        for (session in sessions) byId[session.id()] = session
        for (record in StrengthAnalytics.progression(sessions)) {
            val session =
                byId[record.sessionId()] ?: throw NullPointerException("PB source session")
            out.add(
                Point(
                    record.setId() + record.category(),
                    record.exerciseId(),
                    record.category(),
                    record.value(),
                    record.kilograms(),
                    record.reps(),
                    CalendarDates.atOffset(record.at()!!, session.offset()),
                    HistoricalStrengthRecord.DatePrecision.EXACT,
                    "Recorded workout",
                    record.sessionId(),
                    record.notes(),
                )
            )
        }
        for (record in manual) {
            val mode = record.exercise().mode()
            val prefix =
                if (mode == StrengthExercise.Mode.WEIGHTED_BODYWEIGHT) "Added load" else "Load"
            if (!record.exercise().measurement().usesReps()) {
                var category =
                    if (record.durationSeconds() != null) "Duration seconds" else "Distance metres"
                if (record.exercise().measurement().usesLoad())
                    category += " at ${record.kilograms()} kg"
                add(
                    out,
                    record,
                    category,
                    record.durationSeconds()?.toDouble() ?: record.distanceMetres()!!,
                )
            } else if (mode == StrengthExercise.Mode.BODYWEIGHT)
                add(out, record, "Bodyweight reps", record.reps().toDouble())
            else {
                add(out, record, prefix, record.kilograms())
                add(out, record, "$prefix × ${record.reps()}", record.kilograms())
                val estimate =
                    StrengthAnalytics.estimatedOneRepMax(
                        record.exercise(),
                        StrengthSession.SetEntry(
                            record.id(),
                            record.reps(),
                            record.kilograms(),
                            null,
                            false,
                        ),
                    )
                if (estimate != null) add(out, record, "Estimated 1RM", estimate)
            }
        }
        val chronological =
            Comparator<Point> { a, b ->
                    val first = a.date()
                    val second = b.date()
                    when {
                        first == null -> if (second == null) 0 else 1
                        second == null -> -1
                        else -> first.toEpochDay().compareTo(second.toEpochDay())
                    }
                }
                .thenComparator { a, b -> a.id()!!.compareTo(b.id()!!) }
        return Immutable.copyList(out.sortedWith(chronological))
    }

    private fun add(
        out: MutableList<Point>,
        record: HistoricalStrengthRecord,
        category: String,
        value: Double,
    ) {
        out.add(
            Point(
                record.id(),
                record.exercise().id(),
                category,
                value,
                record.kilograms(),
                record.reps(),
                record.date(),
                record.precision(),
                "Historical/manual PB",
                null,
                record.notes(),
            )
        )
    }

    @JvmStatic
    fun best(timeline: List<Point>, exerciseId: String?, category: String?): Point? =
        timeline
            .filter { it.exerciseId()!! == exerciseId && it.category()!! == category }
            .maxByOrNull { it.value() }
}
