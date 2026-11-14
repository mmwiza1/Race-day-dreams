package org.racedaydreams.domain.strength

import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.*

/** Immutable original prescriptions. Actual sets remain exclusively in StrengthSession. */
data class GymProgramme(
    private val id: String?,
    private val version: Int,
    private val createdAt: Instant?,
    private val reason: String?,
    private val settings: Settings?,
    private val start: LocalDate?,
    private val end: LocalDate?,
    private var sessions: List<Session>?,
    private var tradeoffs: List<String>?,
) {
    enum class Goal {
        SUPPORT_RUNNING,
        STRENGTH,
        MUSCLE,
        GENERAL,
        POWER,
    }

    enum class Priority {
        RUNNING_FIRST,
        BALANCED,
        STRENGTH_FIRST,
    }

    enum class Experience {
        BEGINNER,
        INTERMEDIATE,
        EXPERIENCED,
    }

    enum class Effort {
        RPE,
        RIR,
    }

    data class Settings(
        private val goal: Goal?,
        private val priority: Priority?,
        private val experience: Experience?,
        private var days: Set<DayOfWeek>?,
        private val frequency: Int,
        private var equipment: Set<String>?,
        private val minutes: Int,
        private val effort: Effort?,
    ) {
        init {
            if (goal == null) throw NullPointerException()
            if (priority == null) throw NullPointerException()
            if (experience == null) throw NullPointerException()
            if (effort == null) throw NullPointerException()
            days = Immutable.copySet(days ?: throw NullPointerException())
            equipment = Immutable.copySet(equipment ?: throw NullPointerException())
            require(
                frequency in 1..6 && frequency <= days!!.size && minutes in listOf(30, 45, 60, 75)
            ) {
                "Choose 1–6 available days and a supported session length."
            }
            val supported =
                listOf(
                    "bodyweight",
                    "dumbbells",
                    "barbell",
                    "squat rack",
                    "bench",
                    "machines",
                    "cables",
                    "kettlebells",
                    "pull-up bar",
                    "trap bar",
                    "sled",
                    "full gym",
                )
            require(equipment!!.all { it in supported }) { "Unsupported equipment selection." }
        }

        fun goal(): Goal = goal!!

        fun priority(): Priority = priority!!

        fun experience(): Experience = experience!!

        fun days(): Set<DayOfWeek> = days!!

        fun frequency(): Int = frequency

        fun equipment(): Set<String> = equipment!!

        fun minutes(): Int = minutes

        fun effort(): Effort = effort!!

        override fun toString(): String =
            "Settings[goal=$goal, priority=$priority, experience=$experience, days=$days, frequency=$frequency, equipment=$equipment, minutes=$minutes, effort=$effort]"
    }

    data class Prescription(
        private val exerciseId: String?,
        private val sets: Int,
        private val minimumReps: Int,
        private val maximumReps: Int,
        private val seconds: Int?,
        private val metres: Double?,
        private val minimumRpe: Int,
        private val maximumRpe: Int,
        private val restSeconds: Int,
        private val intent: String?,
        private val referenceLoadKg: Double?,
    ) {
        constructor(
            exerciseId: String?,
            sets: Int,
            minimumReps: Int,
            maximumReps: Int,
            seconds: Int?,
            metres: Double?,
            minimumRpe: Int,
            maximumRpe: Int,
            restSeconds: Int,
            intent: String?,
        ) : this(
            exerciseId,
            sets,
            minimumReps,
            maximumReps,
            seconds,
            metres,
            minimumRpe,
            maximumRpe,
            restSeconds,
            intent,
            null,
        )

        init {
            require(
                referenceLoadKg == null ||
                    referenceLoadKg.isFinite() &&
                        referenceLoadKg >= 0 &&
                        referenceLoadKg <= 1500 &&
                        minimumReps != 0
            ) {
                "Load reference requires a valid repetition prescription."
            }
            require(
                exerciseId != null &&
                    exerciseId.isNotEmpty() &&
                    sets in 1..6 &&
                    minimumReps >= 0 &&
                    maximumReps >= minimumReps &&
                    maximumReps <= 100 &&
                    minimumRpe >= 1 &&
                    maximumRpe <= 10 &&
                    minimumRpe <= maximumRpe &&
                    restSeconds in 30..300 &&
                    intent != null
            ) {
                "Invalid strength prescription."
            }
            require(
                (if (minimumReps > 0) 1 else 0) +
                    (if (seconds != null) 1 else 0) +
                    (if (metres != null) 1 else 0) == 1 &&
                    (seconds == null || seconds in 5..600) &&
                    (metres == null || metres.isFinite() && metres > 0 && metres <= 1000)
            ) {
                "Choose one valid measurement for this prescription."
            }
        }

        fun exerciseId(): String = exerciseId!!

        fun sets(): Int = sets

        fun minimumReps(): Int = minimumReps

        fun maximumReps(): Int = maximumReps

        fun seconds(): Int? = seconds

        fun metres(): Double? = metres

        fun minimumRpe(): Int = minimumRpe

        fun maximumRpe(): Int = maximumRpe

        fun restSeconds(): Int = restSeconds

        fun intent(): String = intent!!

        fun referenceLoadKg(): Double? = referenceLoadKg

        override fun toString(): String =
            "Prescription[exerciseId=$exerciseId, sets=$sets, minimumReps=$minimumReps, maximumReps=$maximumReps, seconds=$seconds, metres=$metres, minimumRpe=$minimumRpe, maximumRpe=$maximumRpe, restSeconds=$restSeconds, intent=$intent, referenceLoadKg=$referenceLoadKg]"
    }

    data class Session(
        private val id: String?,
        private val date: LocalDate?,
        private val title: String?,
        private var exercises: List<Prescription>?,
        private val explanation: String?,
    ) {
        init {
            require(
                id != null &&
                    id.isNotEmpty() &&
                    date != null &&
                    title != null &&
                    title.isNotEmpty() &&
                    explanation != null
            ) {
                "Invalid strength session."
            }
            exercises = Immutable.copyList(exercises ?: throw NullPointerException())
            require(exercises!!.size in 1..10) { "A session requires 1–10 exercises." }
        }

        fun id(): String = id!!

        fun date(): LocalDate = date!!

        fun title(): String = title!!

        fun exercises(): List<Prescription> = exercises!!

        fun explanation(): String = explanation!!

        override fun toString(): String =
            "Session[id=$id, date=$date, title=$title, exercises=$exercises, explanation=$explanation]"
    }

    init {
        require(
            id != null &&
                id.isNotEmpty() &&
                version >= 1 &&
                createdAt != null &&
                reason != null &&
                reason.isNotEmpty() &&
                start != null &&
                end != null &&
                end.toEpochDay() - start.toEpochDay() in 0..56
        ) {
            "Invalid programme version or block."
        }
        if (settings == null) throw NullPointerException()
        sessions = Immutable.copyList(sessions ?: throw NullPointerException())
        tradeoffs = Immutable.copyList(tradeoffs ?: throw NullPointerException())
        val ids = mutableSetOf<String>()
        val dates = mutableSetOf<LocalDate>()
        for (session in sessions!!) {
            require(
                ids.add(session.id()) &&
                    dates.add(session.date()) &&
                    session.date().toEpochDay() >= start.toEpochDay() &&
                    session.date().toEpochDay() <= end.toEpochDay()
            ) {
                "Invalid or duplicated session placement."
            }
        }
    }

    fun id(): String = id!!

    fun version(): Int = version

    fun createdAt(): Instant = createdAt!!

    fun reason(): String = reason!!

    fun settings(): Settings = settings!!

    fun start(): LocalDate = start!!

    fun end(): LocalDate = end!!

    fun sessions(): List<Session> = sessions!!

    fun tradeoffs(): List<String> = tradeoffs!!

    override fun toString(): String =
        "GymProgramme[id=$id, version=$version, createdAt=$createdAt, reason=$reason, settings=$settings, start=$start, end=$end, sessions=$sessions, tradeoffs=$tradeoffs]"
}
