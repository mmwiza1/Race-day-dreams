package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.*

/** A persistable snapshot. Unfinished actuals are not completed StrengthSession evidence. */
data class GymExecution(
    private val id: String?,
    private val origin: Origin?,
    private val prescription: GymWorkoutDefinition?,
    private val source: SourceReference?,
    private val startedAt: Instant?,
    private val offset: ZoneOffset?,
    private val status: Status?,
    private val lastTick: Tick?,
    private val recordedElapsedMillis: Long,
    private val activeMillis: Long,
    private val selectedSlotId: String?,
    private var slots: List<GymWorkoutDefinition.Slot>?,
    private var actualSets: List<GymExecutionSet>?,
    private var events: List<Event>?,
    private val completedSession: StrengthSession?,
    private val partial: Boolean,
) {
    enum class Origin {
        PLANNED,
        QUICK,
        CUSTOM,
        REPEATED,
        FAVOURITE,
        GENERATED,
        FREE,
    }

    enum class Status {
        ACTIVE,
        PAUSED,
        RECOVERY_REQUIRED,
        FINISHED,
        DISCARDED,
    }

    enum class SourceKind {
        PROGRAMME,
        SAVED_WORKOUT,
        ACTUAL_SESSION,
    }

    enum class EventType {
        WORKOUT_STARTED,
        EXERCISE_SELECTED,
        EXERCISES_ADDED,
        EXERCISE_REPLACED,
        EXERCISE_SKIPPED,
        SUPERSET_UNGROUPED,
        SET_SAVED,
        PAUSED,
        RESUMED,
        RECOVERED_WITH_GAP,
        WORKOUT_COMPLETED,
        WORKOUT_FINISHED_EARLY,
        WORKOUT_DISCARDED,
    }

    /** Preserved provenance, never an instruction to auto-link a spontaneous workout. */
    data class SourceReference(
        private val kind: SourceKind?,
        private val id: String?,
        private val revision: Int,
        private val sessionId: String?,
    ) {
        init {
            if (kind == null) throw NullPointerException("source kind")
            GymWorkoutDefinition.requireId(id, "source")
            if (kind == SourceKind.PROGRAMME) {
                require(revision >= 1) { "Preserve the programme revision." }
                GymWorkoutDefinition.requireId(sessionId, "prescribed session")
            } else
                require(
                    sessionId == null &&
                        !(kind == SourceKind.SAVED_WORKOUT && revision < 1) &&
                        !(kind == SourceKind.ACTUAL_SESSION && revision != 0)
                ) {
                    "Invalid frozen workout source."
                }
        }

        fun kind(): SourceKind = kind!!

        fun id(): String = id!!

        fun revision(): Int = revision

        fun sessionId(): String? = sessionId

        override fun toString(): String =
            "SourceReference[kind=$kind, id=$id, revision=$revision, sessionId=$sessionId]"
    }

    /** Identifies the monotonic clock epoch, never a user/account ID. */
    data class Tick(
        private val continuityId: String?,
        private val monotonicMillis: Long,
        private val wallTime: Instant?,
    ) {
        init {
            GymWorkoutDefinition.requireId(continuityId, "clock continuity")
            require(monotonicMillis >= 0) { "Monotonic time must be non-negative." }
            if (wallTime == null) throw NullPointerException("wall time")
        }

        fun continuityId(): String = continuityId!!

        fun monotonicMillis(): Long = monotonicMillis

        fun wallTime(): Instant = wallTime!!

        override fun toString(): String =
            "Tick[continuityId=$continuityId, monotonicMillis=$monotonicMillis, wallTime=$wallTime]"
    }

    data class Event(
        private val sequence: Int,
        private val type: EventType?,
        private val slotId: String?,
        private val actualSetId: String?,
        private val recordedElapsedMillis: Long,
        private val activeMillis: Long,
        private val observedAt: Instant?,
    ) {
        init {
            require(
                sequence >= 0 &&
                    recordedElapsedMillis >= 0 &&
                    activeMillis >= 0 &&
                    activeMillis <= recordedElapsedMillis
            ) {
                "Invalid gym event timing."
            }
            if (type == null) throw NullPointerException("event type")
            if (observedAt == null) throw NullPointerException("event observation time")
            if (slotId != null) GymWorkoutDefinition.requireId(slotId, "exercise slot")
            if (actualSetId != null) GymWorkoutDefinition.requireId(actualSetId, "actual set")
            require(
                (type == EventType.SET_SAVED) == (actualSetId != null) &&
                    !(type == EventType.SET_SAVED && slotId == null)
            ) {
                "Set events require exact actual/slot identities."
            }
            require(
                type !in
                    listOf(
                        EventType.EXERCISE_SKIPPED,
                        EventType.SUPERSET_UNGROUPED,
                        EventType.EXERCISE_SELECTED,
                        EventType.EXERCISE_REPLACED,
                        EventType.EXERCISES_ADDED,
                    ) || slotId != null
            ) {
                "Exercise events require the affected slot identity."
            }
        }

        fun sequence(): Int = sequence

        fun type(): EventType = type!!

        fun slotId(): String? = slotId

        fun actualSetId(): String? = actualSetId

        fun recordedElapsedMillis(): Long = recordedElapsedMillis

        fun activeMillis(): Long = activeMillis

        fun observedAt(): Instant = observedAt!!

        override fun toString(): String =
            "Event[sequence=$sequence, type=$type, slotId=$slotId, actualSetId=$actualSetId, recordedElapsedMillis=$recordedElapsedMillis, activeMillis=$activeMillis, observedAt=$observedAt]"
    }

    init {
        GymWorkoutDefinition.requireId(id, "execution")
        if (origin == null) throw NullPointerException("origin")
        if (startedAt == null) throw NullPointerException("started at")
        if (offset == null) throw NullPointerException("offset")
        if (status == null) throw NullPointerException("status")
        if (lastTick == null) throw NullPointerException("last tick")
        require(prescription != null || origin == Origin.FREE) {
            "Preserve the workout prescription."
        }
        require(origin != Origin.PLANNED || source?.kind() == SourceKind.PROGRAMME) {
            "Planned execution requires the exact preserved programme session."
        }
        require(
            !(origin == Origin.FAVOURITE && source?.kind() != SourceKind.SAVED_WORKOUT) &&
                !(origin == Origin.REPEATED && source == null)
        ) {
            "Preserve the source of a favourite or repeated workout."
        }
        require(
            source?.kind() != SourceKind.SAVED_WORKOUT ||
                prescription != null &&
                    prescription.id() == source.id() &&
                    prescription.revision() == source.revision()
        ) {
            "Saved workout source must match the preserved revision."
        }
        require(
            source?.kind() != SourceKind.PROGRAMME ||
                prescription != null &&
                    prescription.revision() == source.revision() &&
                    prescription.id() == GymWorkoutAdapter.programmeWorkoutId(source)
        ) {
            "Programme source must match the preserved workout identity and revision."
        }
        require(
            recordedElapsedMillis in 0..7L * 24 * 60 * 60 * 1000 &&
                activeMillis in 0..recordedElapsedMillis &&
                activeMillis <= 12L * 60 * 60 * 1000
        ) {
            "Invalid recorded gym timing."
        }
        slots = Immutable.copyList(slots ?: throw NullPointerException())
        GymWorkoutDefinition.validateSlots(slots!!, origin == Origin.FREE)
        val ids = slots!!.map { it.id() }.toSet()
        require(
            !(selectedSlotId != null && selectedSlotId !in ids) &&
                !(selectedSlotId == null && slots!!.isNotEmpty())
        ) {
            "Invalid selected exercise slot."
        }
        actualSets = Immutable.copyList(actualSets ?: throw NullPointerException())
        require(actualSets!!.size <= 10000) { "Too many actual gym sets." }
        val setIds = mutableSetOf<String>()
        val actualById = linkedMapOf<String, GymExecutionSet>()
        for (set in actualSets!!) {
            require(set.slotId() in ids && setIds.add(set.actual().id())) {
                "Invalid or duplicate actual set identity."
            }
            actualById[set.actual().id()] = set
        }
        actualBlocks(actualSets!!)
        events = Immutable.copyList(events ?: throw NullPointerException())
        require(events!!.isNotEmpty() && events!!.size <= 50000) { "Invalid gym event history." }
        var elapsed = 0L
        var active = 0L
        val savedEvents = mutableSetOf<String?>()
        var replayed = Status.ACTIVE
        var actualIndex = 0
        for ((i, event) in events!!.withIndex()) {
            require(
                event.sequence() == i &&
                    event.recordedElapsedMillis() >= elapsed &&
                    event.activeMillis() >= active &&
                    event.recordedElapsedMillis() <= recordedElapsedMillis &&
                    event.activeMillis() <= activeMillis &&
                    (event.slotId() == null || event.slotId() in ids) &&
                    (event.actualSetId() == null || event.actualSetId() in setIds)
            ) {
                "Corrupt gym execution events."
            }
            elapsed = event.recordedElapsedMillis()
            active = event.activeMillis()
            if (i == 0)
                require(
                    event.type() == EventType.WORKOUT_STARTED && elapsed == 0L && active == 0L
                ) {
                    "Workout must start once at zero recorded duration."
                }
            else replayed = afterEvent(replayed, event.type())
            if (event.type() == EventType.SET_SAVED) {
                require(savedEvents.add(event.actualSetId())) {
                    "An actual set must have one save event."
                }
                val recorded = actualById[event.actualSetId()]
                require(
                    recorded != null &&
                        recorded.slotId() == event.slotId() &&
                        actualSets!![actualIndex++].actual().id() == event.actualSetId()
                ) {
                    "Actual set event and recorded exercise slot disagree."
                }
            }
        }
        require(savedEvents == setIds) { "Actual set events are incomplete." }
        require(events!![0].type() == EventType.WORKOUT_STARTED) { "Missing workout start event." }
        require(status == replayed) { "Workout status disagrees with event transitions." }
        require((status == Status.FINISHED) == (completedSession != null)) {
            "Completed gym evidence is missing or premature."
        }
        require(
            completedSession == null ||
                completedSession.id() == id &&
                    completedSession.startedAt() == startedAt &&
                    completedSession.offset() == offset &&
                    completedSession.duration() == Durations.ofMillis(activeMillis)
        ) {
            "Completed evidence does not match this execution."
        }
        val lastType = events!!.last().type()
        require(
            !(status == Status.FINISHED &&
                lastType !=
                    (if (partial) EventType.WORKOUT_FINISHED_EARLY
                    else EventType.WORKOUT_COMPLETED)) &&
                !(status == Status.DISCARDED && lastType != EventType.WORKOUT_DISCARDED) &&
                !(status != Status.FINISHED && partial)
        ) {
            "Completion state and events disagree."
        }
        require(
            !(status == Status.PAUSED &&
                lastType != EventType.PAUSED &&
                lastType != EventType.RECOVERED_WITH_GAP) &&
                !(status == Status.RECOVERY_REQUIRED && lastType != EventType.RECOVERED_WITH_GAP) &&
                !(status == Status.ACTIVE &&
                    lastType in
                        listOf(
                            EventType.PAUSED,
                            EventType.RECOVERED_WITH_GAP,
                            EventType.WORKOUT_COMPLETED,
                            EventType.WORKOUT_FINISHED_EARLY,
                            EventType.WORKOUT_DISCARDED,
                        ))
        ) {
            "Workout status disagrees with its event history."
        }
        if (completedSession != null) {
            var count = 0
            for (block in completedSession.exercises()) for (set in block.sets()) {
                val recorded = actualById[set.id()]
                require(
                    recorded != null &&
                        recorded.actual() == set &&
                        recorded.exercise() == block.exercise()
                ) {
                    "Completed sets differ from staged actual evidence."
                }
                count++
            }
            require(count == actualSets!!.size) { "Completed evidence omitted actual sets." }
            require(partial || !hasUnfinishedTargets(prescription, slots!!, actualSets!!)) {
                "Unfinished prescribed work cannot be labelled fully complete."
            }
        }
    }

    fun id(): String = id!!

    fun origin(): Origin = origin!!

    fun prescription(): GymWorkoutDefinition? = prescription

    fun source(): SourceReference? = source

    fun startedAt(): Instant = startedAt!!

    fun offset(): ZoneOffset = offset!!

    fun status(): Status = status!!

    fun lastTick(): Tick = lastTick!!

    fun recordedElapsedMillis(): Long = recordedElapsedMillis

    fun activeMillis(): Long = activeMillis

    fun selectedSlotId(): String? = selectedSlotId

    fun slots(): List<GymWorkoutDefinition.Slot> = slots!!

    fun actualSets(): List<GymExecutionSet> = actualSets!!

    fun events(): List<Event> = events!!

    fun completedSession(): StrengthSession? = completedSession

    fun partial(): Boolean = partial

    fun plannedAssociation(): SourceReference? =
        if (origin == Origin.PLANNED && status == Status.FINISHED) source else null

    fun hasUnfinishedTargets(): Boolean = hasUnfinishedTargets(prescription, slots!!, actualSets!!)

    override fun toString(): String =
        "GymExecution[id=$id, origin=$origin, prescription=$prescription, source=$source, startedAt=$startedAt, offset=$offset, status=$status, lastTick=$lastTick, recordedElapsedMillis=$recordedElapsedMillis, activeMillis=$activeMillis, selectedSlotId=$selectedSlotId, slots=$slots, actualSets=$actualSets, events=$events, completedSession=$completedSession, partial=$partial]"

    private data class BlockKey(val slotId: String, val exercise: StrengthExercise)

    companion object {
        const val FORMAT_VERSION: Int = 1

        @JvmStatic
        fun hasUnfinishedTargets(
            prescription: GymWorkoutDefinition?,
            slots: List<GymWorkoutDefinition.Slot>,
            sets: List<GymExecutionSet>,
        ): Boolean {
            for (slot in slots) {
                val expected = slot.target()?.sets() ?: if (prescription == null) 0 else 1
                val performed =
                    sets.count {
                        it.slotId() == slot.id() &&
                            it.exercise() == slot.exercise() &&
                            !it.actual().warmUp()
                    }
                if (performed < expected) return true
            }
            return false
        }

        private fun afterEvent(before: Status, type: EventType): Status {
            require(
                before != Status.FINISHED &&
                    before != Status.DISCARDED &&
                    type != EventType.WORKOUT_STARTED
            ) {
                "Invalid event after a workout start or terminal event."
            }
            return when (type) {
                EventType.PAUSED -> {
                    require(before == Status.ACTIVE) { "Pause requires an active workout." }
                    Status.PAUSED
                }
                EventType.RESUMED -> {
                    require(before == Status.PAUSED || before == Status.RECOVERY_REQUIRED) {
                        "Resume requires a paused or recovered workout."
                    }
                    Status.ACTIVE
                }
                EventType.RECOVERED_WITH_GAP ->
                    if (before == Status.PAUSED) Status.PAUSED else Status.RECOVERY_REQUIRED
                EventType.WORKOUT_COMPLETED,
                EventType.WORKOUT_FINISHED_EARLY -> Status.FINISHED
                EventType.WORKOUT_DISCARDED -> Status.DISCARDED
                else -> {
                    require(before == Status.ACTIVE) {
                        "Exercise/set events require an active workout."
                    }
                    before
                }
            }
        }

        @JvmStatic
        fun actualBlocks(sets: List<GymExecutionSet>): List<StrengthSession.ExerciseSets> {
            val groups = linkedMapOf<BlockKey, MutableList<StrengthSession.SetEntry>>()
            for (row in sets) groups
                .getOrPut(BlockKey(row.slotId(), row.exercise())) { mutableListOf() }
                .add(row.actual())
            require(groups.size <= 100) { "Too many recorded exercise blocks." }
            return Immutable.copyList(
                groups.map { (key, value) -> StrengthSession.ExerciseSets(key.exercise, value) }
            )
        }
    }
}
