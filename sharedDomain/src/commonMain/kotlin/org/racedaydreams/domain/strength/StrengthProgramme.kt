package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.isJavaBlank
import org.racedaydreams.domain.time.*

/** Authored immutable revision. Concrete dated prescriptions never contain actual sets. */
data class StrengthProgramme(
    private val id: String?,
    private val revision: Int,
    private val parentRevision: Int,
    private val createdAt: Instant?,
    private var name: String?,
    private val origin: Origin?,
    private val templateId: String?,
    private val settings: Settings?,
    private var blocks: List<Block>?,
    private val effectiveFrom: LocalDate?,
    private var reason: String?,
    private var tradeoffs: List<String>?,
) {
    enum class Origin {
        GENERATED,
        CUSTOM,
        TEMPLATE,
    }

    enum class Phase {
        FOUNDATION,
        DEVELOPMENT,
        INTENSIFICATION,
        RECOVERY_ASSESSMENT,
    }

    data class Settings(
        private val goal: GymProgramme.Goal?,
        private val priority: GymProgramme.Priority?,
        private val experience: GymProgramme.Experience?,
        private var days: Set<DayOfWeek>?,
        private val frequency: Int,
        private val sessionMinutes: Int,
        private var equipment: Set<ExerciseMetadata.Equipment>?,
    ) {
        init {
            if (goal == null) throw NullPointerException()
            if (priority == null) throw NullPointerException()
            if (experience == null) throw NullPointerException()
            days = Immutable.copySet(days ?: throw NullPointerException())
            equipment = Immutable.copySet(equipment ?: throw NullPointerException())
            require(
                days!!.isNotEmpty() &&
                    frequency in 1..6 &&
                    frequency <= days!!.size &&
                    sessionMinutes in 20..120
            ) {
                "Choose 1–6 available days and 20–120 minutes."
            }
        }

        fun goal(): GymProgramme.Goal = goal!!

        fun priority(): GymProgramme.Priority = priority!!

        fun experience(): GymProgramme.Experience = experience!!

        fun days(): Set<DayOfWeek> = days!!

        fun frequency(): Int = frequency

        fun sessionMinutes(): Int = sessionMinutes

        fun equipment(): Set<ExerciseMetadata.Equipment> = equipment!!

        override fun toString(): String =
            "Settings[goal=$goal, priority=$priority, experience=$experience, days=$days, frequency=$frequency, sessionMinutes=$sessionMinutes, equipment=$equipment]"
    }

    data class Workout(
        private val id: String?,
        private val date: LocalDate?,
        private val expectedMinutes: Int,
        private val definition: GymWorkoutDefinition?,
        private var notes: String?,
    ) {
        init {
            GymWorkoutDefinition.requireId(id, "programme session")
            if (date == null) throw NullPointerException()
            if (definition == null) throw NullPointerException()
            require(expectedMinutes in 1..180) { "Choose a realistic session duration." }
            notes = text(notes, 2000, true, "workout notes")
            require(definition.slots().size <= 12) {
                "A programme workout supports up to 12 exercises."
            }
            val ids = mutableSetOf<String>()
            for (slot in definition.slots()) require(ids.add(slot.exercise().id())) {
                "Combine repeated exercise sets in one slot."
            }
        }

        fun id(): String = id!!

        fun date(): LocalDate = date!!

        fun expectedMinutes(): Int = expectedMinutes

        fun definition(): GymWorkoutDefinition = definition!!

        fun notes(): String? = notes

        override fun toString(): String =
            "Workout[id=$id, date=$date, expectedMinutes=$expectedMinutes, definition=$definition, notes=$notes]"
    }

    data class Block(
        private val id: String?,
        private val phase: Phase?,
        private var name: String?,
        private val start: LocalDate?,
        private val weeks: Int,
        private var workouts: List<Workout>?,
        private var explanation: String?,
    ) {
        init {
            GymWorkoutDefinition.requireId(id, "block")
            if (phase == null) throw NullPointerException()
            name = text(name, 120, false, "block name")
            if (start == null) throw NullPointerException()
            require(weeks in 1..26) { "A block spans 1–26 programme weeks." }
            workouts = Immutable.copyList(workouts ?: throw NullPointerException())
            explanation = text(explanation, 2000, false, "block explanation")
            require(workouts!!.isNotEmpty() && workouts!!.size <= weeks * 6) {
                "A block needs 1–6 workouts per programme week."
            }
            val dates = mutableSetOf<LocalDate>()
            val ids = mutableSetOf<String>()
            for (workout in workouts!!) {
                val day = workout.date().toEpochDay() - start.toEpochDay()
                require(
                    day >= 0 &&
                        day < weeks * 7L &&
                        dates.add(workout.date()) &&
                        ids.add(workout.id())
                ) {
                    "Workouts need unique dates/IDs within the block."
                }
            }
            // Keep the actual start-day anchor and original checked date arithmetic.
            for (week in 0 until weeks) {
                val first = start.plusWeeks(week.toLong())
                val count =
                    workouts!!.count {
                        it.date().toEpochDay() >= first.toEpochDay() &&
                            it.date().toEpochDay() < first.plusWeeks(1).toEpochDay()
                    }
                require(count in 1..6) { "Every block week needs 1–6 sessions." }
            }
        }

        fun id(): String = id!!

        fun phase(): Phase = phase!!

        fun name(): String = name!!

        fun start(): LocalDate = start!!

        fun weeks(): Int = weeks

        fun workouts(): List<Workout> = workouts!!

        fun explanation(): String = explanation!!

        fun end(): LocalDate = start().plusWeeks(weeks.toLong()).minusDays(1)

        override fun toString(): String =
            "Block[id=$id, phase=$phase, name=$name, start=$start, weeks=$weeks, workouts=$workouts, explanation=$explanation]"
    }

    init {
        GymWorkoutDefinition.requireId(id, "programme")
        require(revision >= 1 && parentRevision >= 0 && parentRevision == revision - 1) {
            "Programme revisions form an explicit lineage."
        }
        if (createdAt == null) throw NullPointerException()
        if (origin == null) throw NullPointerException()
        if (settings == null) throw NullPointerException()
        if (effectiveFrom == null) throw NullPointerException()
        name = text(name, 120, false, "programme name")
        reason = text(reason, 2000, false, "revision reason")
        if (origin == Origin.TEMPLATE) GymWorkoutDefinition.requireId(templateId, "template")
        else require(templateId == null) { "Only templates have template provenance." }
        blocks = Immutable.copyList(blocks ?: throw NullPointerException())
        tradeoffs = Immutable.copyList(tradeoffs ?: throw NullPointerException())
        require(blocks!!.isNotEmpty() && blocks!!.size <= 12 && tradeoffs!!.size <= 100) {
            "A programme needs 1–12 blocks and bounded explanations."
        }
        val ids = mutableSetOf<String>()
        val sessions = mutableSetOf<String>()
        var next = blocks!![0].start()
        var weeks = 0
        for (block in blocks!!) {
            require(ids.add(block.id()) && block.start() == next) {
                "Programme blocks must be contiguous and uniquely identified."
            }
            weeks += block.weeks()
            next = block.end().plusDays(1)
            for (workout in block.workouts()) require(sessions.add(workout.id())) {
                "Duplicate session ID."
            }
        }
        require(
            weeks <= 52 &&
                effectiveFrom.toEpochDay() >= blocks!![0].start().toEpochDay() &&
                effectiveFrom.toEpochDay() <= next.minusDays(1).toEpochDay()
        ) {
            "A programme spans at most 52 weeks with an in-range effective date."
        }
        for (note in tradeoffs!!) text(note, 2000, false, "trade-off")
    }

    fun id(): String = id!!

    fun revision(): Int = revision

    fun parentRevision(): Int = parentRevision

    fun createdAt(): Instant = createdAt!!

    fun name(): String = name!!

    fun origin(): Origin = origin!!

    fun templateId(): String? = templateId

    fun settings(): Settings = settings!!

    fun blocks(): List<Block> = blocks!!

    fun effectiveFrom(): LocalDate = effectiveFrom!!

    fun reason(): String = reason!!

    fun tradeoffs(): List<String> = tradeoffs!!

    fun start(): LocalDate = blocks!![0].start()

    fun end(): LocalDate = blocks!!.last().end()

    fun workouts(): List<Workout> =
        Immutable.copyList(
            blocks!!
                .flatMap { it.workouts() }
                .sortedWith(compareBy<Workout> { it.date().toEpochDay() }.thenBy { it.id() })
        )

    override fun toString(): String =
        "StrengthProgramme[id=$id, revision=$revision, parentRevision=$parentRevision, createdAt=$createdAt, name=$name, origin=$origin, templateId=$templateId, settings=$settings, blocks=$blocks, effectiveFrom=$effectiveFrom, reason=$reason, tradeoffs=$tradeoffs]"

    companion object {
        const val FORMAT_VERSION: Int = 1

        @JvmStatic
        fun text(value: String?, maximum: Int, optional: Boolean, label: String): String? {
            if (value == null && optional) return null
            require(value != null && !isJavaBlank(value) && value.length <= maximum) {
                "Provide $label (up to $maximum characters)."
            }
            return value
        }
    }
}
