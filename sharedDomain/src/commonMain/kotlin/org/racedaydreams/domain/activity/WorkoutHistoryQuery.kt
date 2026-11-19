package org.racedaydreams.domain.activity

import kotlin.jvm.JvmStatic
import kotlin.math.min
import org.racedaydreams.domain.*
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

/** Original bounded history projection. Prescriptions never create imaginary completions. */
object WorkoutHistoryQuery {
    enum class Kind {
        ALL,
        RUNNING,
        STRENGTH,
    }

    data class Entry(
        private val id: String?,
        private val kind: Kind?,
        private val at: Instant?,
        private val date: LocalDate?,
        private val label: String?,
        private val run: ActivityRecord?,
        private val strength: StrengthSession?,
    ) {
        fun id(): String? = id

        fun kind(): Kind? = kind

        fun at(): Instant? = at

        fun date(): LocalDate? = date

        fun label(): String? = label

        fun run(): ActivityRecord? = run

        fun strength(): StrengthSession? = strength

        override fun toString(): String =
            "Entry[id=$id, kind=$kind, at=$at, date=$date, label=$label, run=$run, strength=$strength]"
    }

    data class Page(
        private val entries: List<Entry>?,
        private val total: Int,
        private val hasMore: Boolean,
    ) {
        fun entries(): List<Entry>? = entries

        fun total(): Int = total

        fun hasMore(): Boolean = hasMore

        override fun toString(): String = "Page[entries=$entries, total=$total, hasMore=$hasMore]"
    }

    @JvmStatic
    fun page(
        runs: List<ActivityRecord>,
        strength: List<StrengthSession>,
        prescriptions: List<PlannedWorkout>,
        kind: Kind?,
        type: WorkoutCategory?,
        exerciseQuery: String?,
        from: LocalDate?,
        through: LocalDate?,
        offset: Int,
        limit: Int,
    ): Page {
        require(
            offset >= 0 &&
                limit in 1..100 &&
                !(from != null && through != null && through.toEpochDay() < from.toEpochDay())
        ) {
            "Check history range and page size."
        }
        val types = mutableMapOf<String, WorkoutCategory>()
        for (p in prescriptions) types[p.id()] = p.category()
        val query = exerciseQuery?.trim { it <= ' ' }?.lowercase() ?: ""
        val entries = mutableListOf<Entry>()
        if (kind != Kind.STRENGTH && query.isEmpty())
            for (run in runs) {
                val date =
                    CalendarDates.atOffset(
                        run.start(),
                        run.startOffset() ?: Offsets.ofTotalSeconds(0),
                    )
                val category = types[run.plannedWorkoutId()]
                if (inRange(date, from, through) && (type == null || category == type))
                    entries.add(
                        Entry(
                            run.id(),
                            Kind.RUNNING,
                            run.start(),
                            date,
                            category?.name?.lowercase()?.replace('_', ' ') ?: "Run",
                            run,
                            null,
                        )
                    )
            }
        if (kind != Kind.RUNNING && type == null)
            for (session in strength) {
                val date = CalendarDates.atOffset(session.startedAt(), session.offset())
                var matches = query.isEmpty()
                if (!matches)
                    for (block in session.exercises()) if (
                        ExerciseSearch.search(
                                Immutable.list(block.exercise()),
                                query,
                                Immutable.set(),
                                Immutable.list(),
                                Immutable.set(),
                            )
                            .isNotEmpty()
                    ) {
                        matches = true
                        break
                    }
                if (matches && inRange(date, from, through))
                    entries.add(
                        Entry(
                            session.id(),
                            Kind.STRENGTH,
                            session.startedAt(),
                            date,
                            "Strength",
                            null,
                            session,
                        )
                    )
            }
        entries.sortWith(compareByDescending<Entry> { it.at()!! }.thenBy { it.id()!! })
        val start = min(offset, entries.size)
        val end = min(start + limit, entries.size)
        return Page(
            Immutable.copyList(entries.subList(start, end)),
            entries.size,
            end < entries.size,
        )
    }

    private fun inRange(date: LocalDate, from: LocalDate?, through: LocalDate?): Boolean =
        (from == null || date.toEpochDay() >= from.toEpochDay()) &&
            (through == null || date.toEpochDay() <= through.toEpochDay())
}
