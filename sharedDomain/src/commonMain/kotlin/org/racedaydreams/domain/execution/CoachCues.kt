package org.racedaydreams.domain.execution

import kotlin.jvm.JvmStatic
import kotlin.math.max

/** Sustained smoothed evidence, transition grace, hysteresis and bounded cue arbitration. */
class CoachCues {
    fun interface Sink {
        fun deliver(cue: Cue)
    }

    data class Cue(
        private val id: String?,
        private val text: String?,
        private val priority: Int,
        private val atMillis: Long,
        private val haptic: Boolean,
    ) {
        init {
            require(id != null && text != null && priority >= 0 && atMillis >= 0) { "Invalid cue" }
        }

        fun id(): String = id!!

        fun text(): String = text!!

        fun priority(): Int = priority

        fun atMillis(): Long = atMillis

        fun haptic(): Boolean = haptic

        override fun toString(): String =
            "Cue[id=$id, text=$text, priority=$priority, atMillis=$atMillis, haptic=$haptic]"
    }

    private var step = -1
    private var direction = 0
    private var since = -1L
    private var lastCue = -120000L
    private var graceUntil = 0L
    private var alerted = false

    fun pace(
        state: WorkoutExecutionEngine.State,
        input: WorkoutExecutionEngine.Input,
        sustainedMillis: Long,
    ): Cue? {
        require(sustainedMillis in 10000..120000) { "Invalid sensitivity" }
        if (step != state.index()) {
            step = state.index()
            graceUntil = input.activeMillis() + 15000
            reset()
        }
        val current = state.current()
        val pace = input.smoothedPace()
        val fast = current?.fastPace()
        val slow = current?.slowPace()
        if (
            current == null ||
                state.early() ||
                input.paused() ||
                !input.reliablePace() ||
                pace == null ||
                fast == null ||
                slow == null ||
                input.activeMillis() < graceUntil
        ) {
            reset()
            return null
        }
        val margin = max(8.0, fast * .03)
        val d = if (pace < fast - margin) -1 else if (pace > slow + margin) 1 else 0
        if (d == 0) {
            if (alerted && pace >= fast && pace <= slow) {
                reset()
                return Cue(
                    "pace-in-${input.activeMillis()}",
                    "Back in target range.",
                    4,
                    input.activeMillis(),
                    false,
                )
            }
            if (!alerted) reset()
            return null
        }
        if (d != direction) {
            direction = d
            since = input.activeMillis()
            alerted = false
        }
        if (
            !alerted &&
                input.activeMillis() - since >= sustainedMillis &&
                input.activeMillis() - lastCue >= 90000
        ) {
            alerted = true
            lastCue = input.activeMillis()
            return Cue(
                "pace-$lastCue",
                if (d < 0) "Slightly fast. Ease off."
                else "A little below target pace. Run by effort if conditions require.",
                4,
                lastCue,
                false,
            )
        }
        return null
    }

    private fun reset() {
        direction = 0
        since = -1
        alerted = false
    }

    companion object {
        /** Only the most important fresh cue survives; never accumulates a stale speech backlog. */
        @JvmStatic
        fun select(cues: List<Cue>, now: Long): Cue? =
            cues
                .filter { now >= it.atMillis() && now - it.atMillis() <= 10000 }
                .minWithOrNull(
                    compareBy<Cue> { it.priority() }
                        .thenByDescending { it.atMillis() }
                        .thenBy { it.id() }
                )
    }
}
