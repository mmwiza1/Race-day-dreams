package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.strength.GymExecution.Event
import org.racedaydreams.domain.strength.GymExecution.EventType
import org.racedaydreams.domain.strength.GymExecution.Status
import org.racedaydreams.domain.strength.GymExecution.Tick
import org.racedaydreams.domain.time.*

/** One deterministic execution path. Callers supply clocks and persist snapshots; no IO here. */
class GymExecutionEngine private constructor(private var state: GymExecution) {
    companion object {
        @JvmStatic
        fun start(
            id: String?,
            origin: GymExecution.Origin?,
            prescription: GymWorkoutDefinition?,
            source: GymExecution.SourceReference?,
            offset: ZoneOffset?,
            at: Tick?,
        ): GymExecutionEngine {
            if (at == null) throw NullPointerException("start tick")
            val slots = prescription?.slots() ?: Immutable.list()
            val first = slots.firstOrNull()?.id()
            val event = Event(0, EventType.WORKOUT_STARTED, first, null, 0, 0, at.wallTime())
            return GymExecutionEngine(
                GymExecution(
                    id,
                    origin,
                    prescription,
                    source,
                    at.wallTime(),
                    offset,
                    Status.ACTIVE,
                    at,
                    0,
                    0,
                    first,
                    slots,
                    Immutable.list(),
                    Immutable.list(event),
                    null,
                    false,
                )
            )
        }

        /** Process interruption is an explicit gap. Never count clock downtime as exercise. */
        @JvmStatic
        fun recover(saved: GymExecution?, at: Tick?): GymExecutionEngine {
            if (saved == null) throw NullPointerException("saved execution")
            if (at == null) throw NullPointerException("recovery tick")
            require(saved.status() != Status.FINISHED && saved.status() != Status.DISCARDED) {
                "Only unfinished gym execution can be recovered."
            }
            val engine = GymExecutionEngine(saved)
            val recovered =
                if (saved.status() == Status.PAUSED) Status.PAUSED else Status.RECOVERY_REQUIRED
            engine.state =
                engine.withEvent(
                    recovered,
                    Timing(at, saved.recordedElapsedMillis(), saved.activeMillis()),
                    saved.selectedSlotId(),
                    saved.slots(),
                    saved.actualSets(),
                    EventType.RECOVERED_WITH_GAP,
                    saved.selectedSlotId(),
                    null,
                    null,
                    false,
                )
            return engine
        }
    }

    fun snapshot(): GymExecution = state

    fun observe(at: Tick?): GymExecution {
        requireUnfinished()
        val timing = timing(at)
        state =
            snapshot(
                state.status(),
                timing,
                state.selectedSlotId(),
                state.slots(),
                state.actualSets(),
                state.events(),
                null,
                false,
            )
        return state
    }

    fun pause(at: Tick?): GymExecution {
        requireActive()
        state =
            withEvent(
                Status.PAUSED,
                timing(at),
                state.selectedSlotId(),
                state.slots(),
                state.actualSets(),
                EventType.PAUSED,
                state.selectedSlotId(),
                null,
                null,
                false,
            )
        return state
    }

    fun resume(at: Tick?): GymExecution {
        require(state.status() == Status.PAUSED || state.status() == Status.RECOVERY_REQUIRED) {
            "Only a paused or explicitly recovered workout can resume."
        }
        state =
            withEvent(
                Status.ACTIVE,
                timing(at),
                state.selectedSlotId(),
                state.slots(),
                state.actualSets(),
                EventType.RESUMED,
                state.selectedSlotId(),
                null,
                null,
                false,
            )
        return state
    }

    fun selectExercise(slotId: String?, at: Tick?): GymExecution {
        requireActive()
        slot(slotId)
        if (slotId == state.selectedSlotId()) return state
        state =
            withEvent(
                state.status(),
                timing(at),
                slotId,
                state.slots(),
                state.actualSets(),
                EventType.EXERCISE_SELECTED,
                slotId,
                null,
                null,
                false,
            )
        return state
    }

    fun addExercises(added: List<GymWorkoutDefinition.Slot>?, at: Tick?): GymExecution {
        requireActive()
        require(added != null && added.isNotEmpty()) { "Choose an exercise to add." }
        val slots = state.slots() + added
        GymWorkoutDefinition.validateSlots(slots, false)
        val selected = state.selectedSlotId() ?: slots[0].id()
        state =
            withEvent(
                state.status(),
                timing(at),
                selected,
                slots,
                state.actualSets(),
                EventType.EXERCISES_ADDED,
                added[0].id(),
                null,
                null,
                false,
            )
        return state
    }

    /** Today-only replacement preserves original prescriptions and all already entered actuals. */
    fun replaceExercise(slotId: String?, replacement: StrengthExercise?, at: Tick?): GymExecution {
        requireActive()
        val original = slot(slotId)
        if (replacement == null) throw NullPointerException("replacement exercise")
        var target = original.target()
        if (target != null) {
            target =
                if (original.exercise().measurement() == replacement.measurement())
                    target.withoutLoadReference()
                else null
            if (target != null && replacement.mode() == StrengthExercise.Mode.BODYWEIGHT)
                target = target.withoutLoadReference()
        }
        val changed =
            GymWorkoutDefinition.Slot(slotId, replacement, target, original.supersetGroup())
        val slots = state.slots().map { if (it.id() == slotId) changed else it }
        state =
            withEvent(
                state.status(),
                timing(at),
                state.selectedSlotId(),
                slots,
                state.actualSets(),
                EventType.EXERCISE_REPLACED,
                slotId,
                null,
                null,
                false,
            )
        return state
    }

    fun saveSet(slotId: String?, actual: StrengthSession.SetEntry?, at: Tick?): GymExecution =
        saveSet(GymExecutionSet(slotId, slot(slotId).exercise(), actual), at)

    /** Identical stable-ID retries do not append events, including after completion. */
    fun saveSet(row: GymExecutionSet?, at: Tick?): GymExecution {
        if (row == null) throw NullPointerException("actual set")
        for (saved in state.actualSets()) if (saved.actual().id() == row.actual().id()) {
            if (saved == row) return state
            throw IllegalArgumentException(
                "An actual set ID cannot be reused with different evidence."
            )
        }
        requireActive()
        require(slot(row.slotId()).exercise() == row.exercise()) {
            "Actual set must retain the exercise currently being performed."
        }
        val sets = state.actualSets() + row
        GymExecution.actualBlocks(sets)
        state =
            withEvent(
                state.status(),
                timing(at),
                state.selectedSlotId(),
                state.slots(),
                sets,
                EventType.SET_SAVED,
                row.slotId(),
                row.actual().id(),
                null,
                false,
            )
        return state
    }

    /** Skipping preserves the target. Completion may honestly be partial. */
    fun skipExercise(slotId: String?, at: Tick?): GymExecution {
        requireActive()
        slot(slotId)
        state =
            withEvent(
                state.status(),
                timing(at),
                state.selectedSlotId(),
                state.slots(),
                state.actualSets(),
                EventType.EXERCISE_SKIPPED,
                slotId,
                null,
                null,
                false,
            )
        return state
    }

    fun breakSuperset(slotId: String?, at: Tick?): GymExecution {
        requireActive()
        val selected = slot(slotId)
        if (selected.supersetGroup() == null) return state
        val changed = GymSupersets.ungroup(state.slots(), selected.supersetGroup())
        state =
            withEvent(
                state.status(),
                timing(at),
                state.selectedSlotId(),
                changed,
                state.actualSets(),
                EventType.SUPERSET_UNGROUPED,
                slotId,
                null,
                null,
                false,
            )
        return state
    }

    fun workingSets(slotId: String?): Int {
        val selected = slot(slotId)
        return state.actualSets().count {
            it.slotId() == slotId && it.exercise() == selected.exercise() && !it.actual().warmUp()
        }
    }

    /** Only entered evidence becomes a completed session; never pre-fill actuals from targets. */
    fun finish(at: Tick?, sessionRpe: Double?, notes: String?, finishEarly: Boolean): GymExecution {
        if (state.status() == Status.FINISHED) {
            if (
                state.completedSession()!!.rpe()?.toBits() == sessionRpe?.toBits() &&
                    state.completedSession()!!.notes() == (notes ?: "")
            )
                return state
            throw IllegalArgumentException(
                "Finished workout evidence cannot be overwritten by a retry."
            )
        }
        requireUnfinished()
        require(state.actualSets().isNotEmpty()) {
            "Record an actual set or discard this unfinished workout."
        }
        val timing = timing(at)
        val partial =
            finishEarly ||
                GymExecution.hasUnfinishedTargets(
                    state.prescription(),
                    state.slots(),
                    state.actualSets(),
                )
        val session =
            StrengthSession(
                state.id(),
                state.startedAt(),
                state.offset(),
                Durations.ofMillis(timing.activeMillis),
                sessionRpe,
                notes,
                GymExecution.actualBlocks(state.actualSets()),
            )
        state =
            withEvent(
                Status.FINISHED,
                timing,
                state.selectedSlotId(),
                state.slots(),
                state.actualSets(),
                if (partial) EventType.WORKOUT_FINISHED_EARLY else EventType.WORKOUT_COMPLETED,
                state.selectedSlotId(),
                null,
                session,
                partial,
            )
        return state
    }

    fun discard(at: Tick?): GymExecution {
        requireUnfinished()
        state =
            withEvent(
                Status.DISCARDED,
                timing(at),
                state.selectedSlotId(),
                state.slots(),
                state.actualSets(),
                EventType.WORKOUT_DISCARDED,
                state.selectedSlotId(),
                null,
                null,
                false,
            )
        return state
    }

    private fun slot(slotId: String?): GymWorkoutDefinition.Slot =
        state.slots().firstOrNull { it.id() == slotId }
            ?: throw IllegalArgumentException("Unknown exercise slot.")

    private fun requireActive() {
        require(state.status() == Status.ACTIVE) {
            "Resume the workout before entering or changing exercise evidence."
        }
    }

    private fun requireUnfinished() {
        require(state.status() != Status.FINISHED && state.status() != Status.DISCARDED) {
            "This workout has already ended."
        }
    }

    private data class Timing(val at: Tick, val elapsedMillis: Long, val activeMillis: Long)

    private fun timing(at: Tick?): Timing {
        if (at == null) throw NullPointerException("clock observation")
        require(
            at.continuityId() == state.lastTick().continuityId() &&
                at.monotonicMillis() >= state.lastTick().monotonicMillis()
        ) {
            "Clock continuity changed. Recover the unfinished workout explicitly."
        }
        val delta =
            if (state.status() == Status.RECOVERY_REQUIRED) 0
            else at.monotonicMillis() - state.lastTick().monotonicMillis()
        return Timing(
            at,
            checkedAdd(state.recordedElapsedMillis(), delta),
            checkedAdd(state.activeMillis(), if (state.status() == Status.ACTIVE) delta else 0),
        )
    }

    private fun withEvent(
        status: Status,
        timing: Timing,
        selected: String?,
        slots: List<GymWorkoutDefinition.Slot>,
        sets: List<GymExecutionSet>,
        type: EventType,
        eventSlot: String?,
        actualId: String?,
        completed: StrengthSession?,
        partial: Boolean,
    ): GymExecution {
        val events =
            state.events() +
                Event(
                    state.events().size,
                    type,
                    eventSlot,
                    actualId,
                    timing.elapsedMillis,
                    timing.activeMillis,
                    timing.at.wallTime(),
                )
        return snapshot(status, timing, selected, slots, sets, events, completed, partial)
    }

    private fun snapshot(
        status: Status,
        timing: Timing,
        selected: String?,
        slots: List<GymWorkoutDefinition.Slot>,
        sets: List<GymExecutionSet>,
        events: List<Event>,
        completed: StrengthSession?,
        partial: Boolean,
    ): GymExecution =
        GymExecution(
            state.id(),
            state.origin(),
            state.prescription(),
            state.source(),
            state.startedAt(),
            state.offset(),
            status,
            timing.at,
            timing.elapsedMillis,
            timing.activeMillis,
            selected,
            slots,
            sets,
            events,
            completed,
            partial,
        )
}
