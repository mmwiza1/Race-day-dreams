package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.*

/** Recorded evidence only. Primary set shares are taxonomy, not measured biological stimulus. */
object StrengthVolume {
    data class ExerciseWeek(
        private val exercise: StrengthExercise?,
        private val workouts: Int,
        private val workingSets: Int,
        private val repetitions: Long,
        private val seconds: Long,
        private val metres: Double,
        private val loadRepetitions: Double?,
    ) {
        fun exercise(): StrengthExercise? = exercise

        fun workouts(): Int = workouts

        fun workingSets(): Int = workingSets

        fun repetitions(): Long = repetitions

        fun seconds(): Long = seconds

        fun metres(): Double = metres

        fun loadRepetitions(): Double? = loadRepetitions

        override fun toString(): String =
            "ExerciseWeek[exercise=$exercise, workouts=$workouts, workingSets=$workingSets, repetitions=$repetitions, seconds=$seconds, metres=$metres, loadRepetitions=$loadRepetitions]"
    }

    data class Week(
        private val monday: LocalDate?,
        private val workouts: Int,
        private val workingSets: Int,
        private val unclassifiedSets: Int,
        private var primarySetShares: Map<ExerciseMetadata.BodyArea, Double?>?,
        private var secondaryExposures: Map<ExerciseMetadata.BodyArea, Int?>?,
        private var exercises: List<ExerciseWeek>?,
    ) {
        init {
            primarySetShares = enumMapSnapshot(primarySetShares ?: throw NullPointerException())
            secondaryExposures = enumMapSnapshot(secondaryExposures ?: throw NullPointerException())
            exercises = Immutable.copyList(exercises ?: throw NullPointerException())
        }

        fun monday(): LocalDate? = monday

        fun workouts(): Int = workouts

        fun workingSets(): Int = workingSets

        fun unclassifiedSets(): Int = unclassifiedSets

        fun primarySetShares(): Map<ExerciseMetadata.BodyArea, Double?> = primarySetShares!!

        fun secondaryExposures(): Map<ExerciseMetadata.BodyArea, Int?> = secondaryExposures!!

        fun exercises(): List<ExerciseWeek> = exercises!!

        override fun toString(): String =
            "Week[monday=$monday, workouts=$workouts, workingSets=$workingSets, unclassifiedSets=$unclassifiedSets, primarySetShares=$primarySetShares, secondaryExposures=$secondaryExposures, exercises=$exercises]"
    }

    @JvmStatic
    fun attribution(): String =
        "One recorded working set is shared equally between its documented primary areas. Secondary involvement is shown separately as exposure, not added full sets. These are broad exercise classifications, not exact muscle stimulus or training stress. Warm-ups are excluded. Unknown or changed exercise metadata remains unclassified."

    @JvmStatic
    fun weekly(
        sessions: List<StrengthSession>,
        firstWeek: LocalDate?,
        weeks: Int,
        asOf: Instant?,
    ): List<Week> {
        if (firstWeek == null) throw NullPointerException()
        if (asOf == null) throw NullPointerException()
        require(weeks in 1..2600) { "Choose 1–2600 weeks." }
        val first = firstWeek.minusDays((firstWeek.getDayOfWeek().getValue() - 1).toLong())
        val buckets = List(weeks) { mutableListOf<StrengthSession>() }
        val seen = mutableMapOf<String, StrengthSession>()
        for (session in sessions) {
            val previous = seen[session.id()]
            if (previous != null) {
                require(previous == session) { "Conflicting actual workout evidence." }
                continue
            }
            seen[session.id()] = session
            if (session.startedAt().isAfter(asOf)) continue
            val date = CalendarDates.atOffset(session.startedAt(), session.offset())
            val days = date.toEpochDay() - first.toEpochDay()
            if (days >= 0 && days < weeks * 7L) buckets[(days / 7).toInt()].add(session)
        }
        return Immutable.copyList(
            (0 until weeks).map { summarise(first.plusWeeks(it.toLong()), buckets[it]) }
        )
    }

    private fun summarise(monday: LocalDate, sessions: List<StrengthSession>): Week {
        val primary = mutableMapOf<ExerciseMetadata.BodyArea, Double>()
        val secondary = mutableMapOf<ExerciseMetadata.BodyArea, Int>()
        val exercises = linkedMapOf<StrengthExercise, Totals>()
        var working = 0
        var unknown = 0
        for (session in sessions) for (block in session.exercises()) {
            val exercise = block.exercise()
            val totals = exercises.getOrPut(exercise) { Totals() }
            val canonical = ExerciseCatalog.definition(exercise.id())
            val metadata =
                if (canonical != null && canonical.exercise() == exercise)
                    ExerciseMetadataCatalog.find(exercise.id())
                else null
            for (set in block.sets()) {
                if (set.warmUp()) continue
                working++
                totals.sets++
                totals.sessions.add(session.id())
                totals.reps += set.reps()
                set.durationSeconds()?.let { totals.seconds += it }
                set.distanceMetres()?.let { totals.metres += it }
                if (
                    exercise.mode() == StrengthExercise.Mode.WEIGHTED &&
                        exercise.measurement() == StrengthExercise.MeasurementProfile.LOAD_REPS
                ) {
                    totals.hasLoadReps = true
                    totals.loadReps += set.kilograms() * set.reps()
                }
                if (metadata == null || metadata.primaryAreas().isEmpty()) {
                    unknown++
                    continue
                }
                val share = 1.0 / metadata.primaryAreas().size
                for (area in metadata.primaryAreas()) primary[area] = (primary[area] ?: 0.0) + share
                for (area in metadata.secondaryAreas()) secondary[area] = (secondary[area] ?: 0) + 1
            }
        }
        val rows =
            exercises
                .filter { it.value.sets > 0 }
                .map { (exercise, totals) ->
                    ExerciseWeek(
                        exercise,
                        totals.sessions.size,
                        totals.sets,
                        totals.reps,
                        totals.seconds,
                        totals.metres,
                        if (totals.hasLoadReps) totals.loadReps else null,
                    )
                }
                .sortedWith(
                    compareBy<ExerciseWeek> { it.exercise()!!.name() }
                        .thenBy { it.exercise()!!.id() }
                )
        return Week(monday, sessions.size, working, unknown, primary, secondary, rows)
    }

    private class Totals {
        var sets = 0
        var reps = 0L
        var seconds = 0L
        var metres = 0.0
        var loadReps = 0.0
        var hasLoadReps = false
        val sessions = mutableSetOf<String>()
    }

    private fun <E : Enum<E>, V> enumMapSnapshot(values: Map<E, V>): Map<E, V> {
        for (key in values.keys) if (key == null) throw NullPointerException()
        return Immutable.copyMapRetainingNullValues(
            values.entries.sortedBy { it.key.ordinal }.associate { it.key to it.value }
        )
    }
}
