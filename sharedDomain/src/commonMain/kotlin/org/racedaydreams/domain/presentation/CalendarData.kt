package org.racedaydreams.domain.presentation

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.*
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

/** Labelled calendar entries. A blank date is never sufficient evidence of rest. */
object CalendarData {
    enum class Category {
        EASY,
        QUALITY,
        LONG,
        STRENGTH,
        RACE,
        REST,
        RUN,
    }

    enum class State {
        PLANNED,
        COMPLETED,
        NOT_COMPLETED,
        MOVED,
        SKIPPED,
    }

    data class Entry(
        private val category: Category?,
        private val state: State?,
        private val label: String?,
        private val id: String?,
        private val activityId: String?,
    ) {
        fun category(): Category? = category

        fun state(): State? = state

        fun label(): String? = label

        fun id(): String? = id

        fun activityId(): String? = activityId

        fun description(): String = "$label: ${state!!.name.lowercase().replace('_',' ')}"

        override fun toString(): String =
            "Entry[category=$category, state=$state, label=$label, id=$id, activityId=$activityId]"
    }

    @JvmStatic
    fun withGym(
        date: LocalDate,
        today: LocalDate,
        original: List<Entry>,
        gym: GymProgramme?,
        actualByPrescription: Map<String, String?>,
    ): List<Entry> {
        if (gym == null || before(date, gym.start()) || before(gym.end(), date)) return original
        val out = withoutGenericStrength(original)
        for (s in gym.sessions()) if (s.date() == date) {
            out.removeAll { it.category() == Category.REST }
            if (!actualByPrescription.containsKey(s.id()))
                out.add(
                    Entry(
                        Category.STRENGTH,
                        if (before(date, today)) State.NOT_COMPLETED else State.PLANNED,
                        "${s.title()} · programmed strength",
                        s.id(),
                        null,
                    )
                )
        }
        return Immutable.copyList(out)
    }

    @JvmStatic
    fun withAuthoredGym(
        date: LocalDate,
        today: LocalDate,
        original: List<Entry>,
        programme: StrengthProgramme?,
        actualByPrescription: Map<String, String?>,
    ): List<Entry> {
        if (programme == null || before(date, programme.start()) || before(programme.end(), date))
            return original
        val out = withoutGenericStrength(original)
        for (w in programme.workouts()) if (w.date() == date) {
            out.removeAll { it.category() == Category.REST }
            if (!actualByPrescription.containsKey(w.id()))
                out.add(
                    Entry(
                        Category.STRENGTH,
                        if (before(date, today)) State.NOT_COMPLETED else State.PLANNED,
                        "${w.definition().title()} · programmed strength",
                        w.id(),
                        null,
                    )
                )
        }
        return Immutable.copyList(out)
    }

    private fun withoutGenericStrength(original: List<Entry>) =
        original
            .filterNot {
                it.category() == Category.STRENGTH &&
                    it.state() == State.PLANNED &&
                    it.label()!!.equals("Strength routine")
            }
            .toMutableList()

    @JvmStatic
    fun category(c: WorkoutCategory): Category =
        when (c) {
            WorkoutCategory.RECOVERY_RUN,
            WorkoutCategory.EASY_RUN,
            WorkoutCategory.ZONE_2_RUN -> Category.EASY
            WorkoutCategory.LONG_RUN -> Category.LONG
            WorkoutCategory.RACE -> Category.RACE
            WorkoutCategory.CUSTOM_RUN -> Category.RUN
            else -> Category.QUALITY
        }

    @JvmStatic
    fun outsideMonth(date: LocalDate, month: LocalDate): Boolean =
        date.getYear() != month.getYear() || date.getMonthValue() != month.getMonthValue()

    @JvmStatic
    fun day(
        date: LocalDate,
        today: LocalDate,
        plan: PlanVersion?,
        workouts: List<PlannedWorkout>,
        activities: List<ActivityRecord>,
        strength: List<StrengthSession>,
        strengthLinks: Map<String, LocalDate>,
        decisions: List<WorkoutDecision>,
    ): List<Entry> {
        val out = mutableListOf<Entry>()
        val represented = mutableSetOf<String>()
        val latest = HashMap<String, WorkoutDecision>()
        for (d in decisions) {
            val old = latest[d.workoutId()]
            if (old == null || old.at().isBefore(d.at())) latest[d.workoutId()] = d
        }
        for (w in workouts) if (w.date() == date) {
            val a = activities.firstOrNull { w.id() == it.plannedWorkoutId() }
            val decision = latest[w.id()]
            if (a != null) {
                if (runDate(a) == date) {
                    out.add(
                        Entry(
                            category(w.category()),
                            State.COMPLETED,
                            label(w.category()),
                            w.id(),
                            a.id(),
                        )
                    )
                    represented.add(a.id())
                } else
                    out.add(
                        Entry(
                            category(w.category()),
                            State.MOVED,
                            "${label(w.category())} · completed on another day",
                            w.id(),
                            a.id(),
                        )
                    )
            } else if (decision == null || decision.kind() != WorkoutDecision.Kind.SKIPPED)
                out.add(
                    Entry(
                        category(w.category()),
                        if (before(date, today)) State.NOT_COMPLETED else State.PLANNED,
                        label(w.category()),
                        w.id(),
                        null,
                    )
                )
        }
        for (a in activities) if (runDate(a) == date && represented.add(a.id())) {
            val w = workouts.firstOrNull { it.id() == a.plannedWorkoutId() }
            out.add(
                Entry(
                    if (w == null) Category.RUN else category(w.category()),
                    State.COMPLETED,
                    if (w == null) "Recorded run" else label(w.category()),
                    a.plannedWorkoutId(),
                    a.id(),
                )
            )
        }
        for (s in strength) if (strengthDate(s) == date)
            out.add(
                Entry(
                    Category.STRENGTH,
                    State.COMPLETED,
                    "Strength · ${s.duration().getSeconds() / 60} min",
                    s.id(),
                    null,
                )
            )
        if (
            plan != null &&
                !before(date, plan.plan().startDate()) &&
                !before(plan.plan().endDate(), date) &&
                date.getDayOfWeek() in plan.goalSnapshot().preferences().gymDays() &&
                strength.none { strengthDate(it) == date } &&
                strengthLinks.values.none { date == it }
        )
            out.add(Entry(Category.STRENGTH, State.PLANNED, "Strength routine", null, null))
        for ((id, linked) in strengthLinks) if (linked == date) {
            val s = strength.firstOrNull { it.id() == id && strengthDate(it) != date }
            if (s != null)
                out.add(
                    Entry(
                        Category.STRENGTH,
                        State.MOVED,
                        "Strength routine · recorded on ${formatGymProgrammeDate(strengthDate(s))}",
                        s.id(),
                        null,
                    )
                )
        }
        for (d in latest.values) if (d.originalDate() == date) {
            val w = workouts.firstOrNull { it.id() == d.workoutId() }
            if (d.kind() == WorkoutDecision.Kind.SKIPPED || date != d.movedTo())
                out.add(
                    Entry(
                        if (w == null) Category.RUN else category(w.category()),
                        if (d.kind() == WorkoutDecision.Kind.SKIPPED) State.SKIPPED
                        else State.MOVED,
                        if (d.kind() == WorkoutDecision.Kind.SKIPPED) "Skipped by choice"
                        else "Moved to ${formatGymProgrammeDate(d.movedTo()!!)}",
                        d.workoutId(),
                        null,
                    )
                )
        }
        if (
            out.isEmpty() &&
                plan != null &&
                !before(date, plan.plan().startDate()) &&
                !before(plan.plan().endDate(), date) &&
                date.getDayOfWeek() in plan.goalSnapshot().preferences().runningDays()
        )
            out.add(Entry(Category.REST, State.PLANNED, "Rest day", null, null))
        return coachingReadOnlyList(out)
    }

    private fun runDate(a: ActivityRecord): LocalDate =
        CalendarDates.atOffset(a.start(), a.startOffset() ?: Offsets.ofTotalSeconds(0))

    private fun strengthDate(s: StrengthSession): LocalDate =
        CalendarDates.atOffset(s.startedAt(), s.offset())

    private fun before(a: LocalDate, b: LocalDate): Boolean = a.toEpochDay() < b.toEpochDay()

    @JvmStatic
    fun label(c: WorkoutCategory): String =
        when (c) {
            WorkoutCategory.TEMPO_THRESHOLD_RUN -> "Tempo run"
            WorkoutCategory.ZONE_2_RUN -> "Zone 2 run"
            else -> c.name.lowercase().replace('_', ' ').replaceFirstChar { c.name.first() }
        }
}
