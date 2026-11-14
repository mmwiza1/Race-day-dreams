package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable

/** Frozen groups in the existing execution engine. No copying of actuals between exercises. */
object GymSupersets {
    data class Next(
        private val slotId: String?,
        private val round: Int,
        private val restBeforeNext: Boolean,
        private val instruction: String?,
    ) {
        fun slotId(): String? = slotId

        fun round(): Int = round

        fun restBeforeNext(): Boolean = restBeforeNext

        fun instruction(): String? = instruction

        override fun toString(): String =
            "Next[slotId=$slotId, round=$round, restBeforeNext=$restBeforeNext, instruction=$instruction]"
    }

    @JvmStatic
    fun group(
        slots: List<GymWorkoutDefinition.Slot>,
        from: Int,
        count: Int,
        groupId: String?,
    ): List<GymWorkoutDefinition.Slot> {
        require(count >= 2 && from >= 0 && from + count <= slots.size) {
            "Choose at least two adjacent exercises."
        }
        GymWorkoutDefinition.requireId(groupId, "superset")
        val result = slots.toMutableList()
        for (i in from until from + count) {
            val slot = slots[i]
            require(slot.supersetGroup() == null) { "Break the existing group first." }
            result[i] =
                GymWorkoutDefinition.Slot(slot.id(), slot.exercise(), slot.target(), groupId)
        }
        GymWorkoutDefinition.validateSlots(result, false)
        return Immutable.copyList(result)
    }

    @JvmStatic
    fun ungroup(
        slots: List<GymWorkoutDefinition.Slot>,
        group: String?,
    ): List<GymWorkoutDefinition.Slot> {
        val result =
            slots.map {
                if (it.supersetGroup() == group)
                    GymWorkoutDefinition.Slot(it.id(), it.exercise(), it.target(), null)
                else it
            }
        GymWorkoutDefinition.validateSlots(result, false)
        return Immutable.copyList(result)
    }

    @JvmStatic
    fun next(execution: GymExecution): Next? {
        val current =
            execution.slots().firstOrNull { it.id() == execution.selectedSlotId() } ?: return null
        if (current.supersetGroup() == null) return null
        val skipped =
            execution
                .events()
                .filter { it.type() == GymExecution.EventType.EXERCISE_SKIPPED }
                .map { it.slotId() }
                .toSet()
        val group =
            execution.slots().filter {
                it.supersetGroup() == current.supersetGroup() && it.id() !in skipped
            }
        var chosen: GymWorkoutDefinition.Slot? = null
        var round = Int.MAX_VALUE
        for (slot in group) {
            val saved =
                execution.actualSets().count { it.slotId() == slot.id() && !it.actual().warmUp() }
            val target = slot.target()?.sets() ?: 1
            if (saved < target && saved < round) {
                chosen = slot
                round = saved
            }
        }
        if (chosen == null) return null
        val rest =
            round > 0 &&
                (group.indexOf(current) < 0 || group.indexOf(chosen) <= group.indexOf(current))
        return Next(
            chosen.id(),
            round + 1,
            rest,
            (if (rest) "Rest before the next round, then complete " else "Next, complete ") +
                chosen.exercise().name() +
                ". Record only what you actually perform.",
        )
    }
}
