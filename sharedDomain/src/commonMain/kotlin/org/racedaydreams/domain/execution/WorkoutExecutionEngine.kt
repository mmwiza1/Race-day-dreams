package org.racedaydreams.domain.execution

import kotlin.jvm.JvmStatic
import kotlin.math.max
import kotlin.math.min
import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.WorkoutSegment
import org.racedaydreams.domain.activity.roundedNanos as roundedBoundaryMillis

/**
 * One projection over accepted recorder metres/active time, never location recording or a new
 * prescription. Distance wins over legacy estimated duration; each observed overshoot is
 * apportioned at the interpolated boundary without counting it again in the next step.
 */
class WorkoutExecutionEngine(steps: List<Step>) {
    data class Step(
        private val label: String?,
        private val kind: WorkoutSegment.Kind?,
        private val metres: Double?,
        private val millis: Long?,
        private val fastPace: Double?,
        private val slowPace: Double?,
        private val rpe: Double?,
    ) {
        init {
            require(label != null && label.length <= 500 && kind != null) { "Invalid workout step" }
            require(
                !(metres != null && (!metres.isFinite() || metres <= 0) ||
                    millis != null && millis <= 0)
            ) {
                "Step targets must be positive"
            }
            require(
                !(fastPace != null && (!fastPace.isFinite() || fastPace <= 0) ||
                    slowPace != null && (!slowPace.isFinite() || slowPace <= 0) ||
                    fastPace != null && slowPace != null && fastPace > slowPace)
            ) {
                "Invalid pace range"
            }
            require(rpe == null || rpe.isFinite() && rpe >= 1 && rpe <= 10) { "Invalid effort" }
        }

        fun label(): String = label!!

        fun kind(): WorkoutSegment.Kind = kind!!

        fun metres(): Double? = metres

        fun millis(): Long? = millis

        fun fastPace(): Double? = fastPace

        fun slowPace(): Double? = slowPace

        fun rpe(): Double? = rpe

        override fun toString(): String =
            "Step[label=$label, kind=$kind, metres=$metres, millis=$millis, fastPace=$fastPace, slowPace=$slowPace, rpe=$rpe]"
    }

    data class Input(
        private val metres: Double,
        private val activeMillis: Long,
        private val elapsedMillis: Long,
        private val smoothedPace: Double?,
        private val reliablePace: Boolean,
        private val paused: Boolean,
    ) {
        init {
            require(
                metres.isFinite() &&
                    metres >= 0 &&
                    activeMillis >= 0 &&
                    elapsedMillis >= activeMillis &&
                    (smoothedPace == null || smoothedPace.isFinite() && smoothedPace > 0)
            ) {
                "Invalid recorder metrics"
            }
        }

        fun metres(): Double = metres

        fun activeMillis(): Long = activeMillis

        fun elapsedMillis(): Long = elapsedMillis

        fun smoothedPace(): Double? = smoothedPace

        fun reliablePace(): Boolean = reliablePace

        fun paused(): Boolean = paused

        override fun toString(): String =
            "Input[metres=$metres, activeMillis=$activeMillis, elapsedMillis=$elapsedMillis, smoothedPace=$smoothedPace, reliablePace=$reliablePace, paused=$paused]"
    }

    data class Event(
        private val type: String?,
        private val step: Int,
        private val metres: Double,
        private val activeMillis: Long,
    ) {
        fun type(): String? = type

        fun step(): Int = step

        fun metres(): Double = metres

        fun activeMillis(): Long = activeMillis

        override fun toString(): String =
            "Event[type=$type, step=$step, metres=$metres, activeMillis=$activeMillis]"
    }

    data class State(
        private val index: Int,
        private val total: Int,
        private val current: Step?,
        private val next: Step?,
        private val metresRemaining: Double?,
        private val millisRemaining: Long?,
        private val complete: Boolean,
        private val early: Boolean,
        private val paused: Boolean,
    ) {
        fun index(): Int = index

        fun total(): Int = total

        fun current(): Step? = current

        fun next(): Step? = next

        fun metresRemaining(): Double? = metresRemaining

        fun millisRemaining(): Long? = millisRemaining

        fun complete(): Boolean = complete

        fun early(): Boolean = early

        fun paused(): Boolean = paused

        override fun toString(): String =
            "State[index=$index, total=$total, current=$current, next=$next, metresRemaining=$metresRemaining, millisRemaining=$millisRemaining, complete=$complete, early=$early, paused=$paused]"
    }

    // Original unmodifiable ArrayList snapshots allowed raw null elements. Do not invent
    // validation.
    private val stepRows: List<Step?>
    private val eventRows = mutableListOf<Event>()
    private var index = 0
    private var startMetres = 0.0
    private var startMillis = 0L
    private var previous: Input? = null
    private var early = false
    private var started = false

    init {
        require(steps.isNotEmpty() && steps.size <= 500) { "Choose 1–500 steps" }
        stepRows = executionSnapshot(steps)
    }

    fun update(input: Input): State {
        val before = previous
        require(
            before == null ||
                input.metres() >= before.metres() &&
                    input.activeMillis() >= before.activeMillis() &&
                    input.elapsedMillis() >= before.elapsedMillis()
        ) {
            "Recorder metrics moved backwards"
        }
        if (!started) {
            started = true
            eventRows.add(Event("WORKOUT_STARTED", 0, 0.0, 0))
            eventRows.add(Event("STEP_STARTED", 0, 0.0, 0))
        }
        if (!input.paused() && !early) {
            while (index < stepRows.size) {
                val step = stepRows[index] ?: throw NullPointerException()
                val distance = step.metres()
                val duration = step.millis()
                val dm = input.metres() - startMetres
                val dt = input.activeMillis() - startMillis
                val done =
                    if (distance != null) dm >= distance else duration != null && dt >= duration
                if (!done) break
                val beforeM = if (before == null) startMetres else max(startMetres, before.metres())
                val beforeT =
                    if (before == null) startMillis else max(startMillis, before.activeMillis())
                var fraction =
                    if (distance != null)
                        (startMetres + distance - beforeM) / max(.000001, input.metres() - beforeM)
                    else
                        (startMillis + duration!! - beforeT).toDouble() /
                            max(1L, input.activeMillis() - beforeT)
                fraction = max(0.0, min(1.0, fraction))
                val boundaryM = beforeM + (input.metres() - beforeM) * fraction
                // Reuse the existing unitless Math.round-compatible primitive, including
                // saturation.
                val boundaryT =
                    beforeT + roundedBoundaryMillis((input.activeMillis() - beforeT) * fraction)
                advance("STEP_COMPLETED", boundaryM, boundaryT)
            }
        }
        previous = input
        return state()
    }

    private fun advance(type: String, metres: Double, active: Long) {
        eventRows.add(Event(type, index, metres, active))
        index++
        startMetres = metres
        startMillis = active
        eventRows.add(
            Event(
                if (index == stepRows.size) "WORKOUT_COMPLETED" else "STEP_STARTED",
                index,
                metres,
                active,
            )
        )
    }

    fun completeOpen() {
        val before = previous
        if (before != null && index < stepRows.size && !early) {
            val current = stepRows[index] ?: throw NullPointerException()
            if (current.metres() == null && current.millis() == null)
                advance("STEP_COMPLETED", before.metres(), before.activeMillis())
        }
    }

    fun skip() {
        val before = previous
        if (before != null && index < stepRows.size && !early)
            advance("STEP_SKIPPED", before.metres(), before.activeMillis())
    }

    fun finishEarly() {
        if (index < stepRows.size && !early) {
            early = true
            eventRows.add(
                Event(
                    "WORKOUT_FINISHED_EARLY",
                    index,
                    previous?.metres() ?: 0.0,
                    previous?.activeMillis() ?: 0,
                )
            )
        }
    }

    fun state(): State {
        val complete = index == stepRows.size
        val current = if (complete) null else stepRows[index]
        val metres = previous?.metres() ?: 0.0
        val active = previous?.activeMillis() ?: 0
        val distance = current?.metres()
        val duration = current?.millis()
        return State(
            index,
            stepRows.size,
            current,
            if (index + 1 < stepRows.size) stepRows[index + 1] else null,
            if (distance == null) null else max(0.0, distance - (metres - startMetres)),
            if (current == null || distance != null || duration == null) null
            else max(0L, duration - (active - startMillis)),
            complete,
            early,
            previous?.paused() == true,
        )
    }

    fun events(): List<Event> = executionSnapshot(eventRows)

    @Suppress("UNCHECKED_CAST") fun steps(): List<Step> = stepRows as List<Step>

    companion object {
        @JvmStatic
        fun from(workout: PlannedWorkout): List<Step> {
            val out = mutableListOf<Step>()
            for (segment in workout.segments()) out.add(
                Step(
                    segment.guidance() ?: segment.kind().name.replace('_', ' '),
                    segment.kind(),
                    segment.distanceKm()?.times(1000),
                    segment.duration()?.toMillis(),
                    segment.minimumSecondsPerKm(),
                    segment.maximumSecondsPerKm(),
                    segment.rpe(),
                )
            )
            if (out.isEmpty())
                out.add(
                    Step(
                        workout.category().name.replace('_', ' '),
                        WorkoutSegment.Kind.RUN,
                        workout.distanceKm()?.times(1000),
                        workout.duration()?.toMillis(),
                        null,
                        null,
                        null,
                    )
                )
            return out
        }

        /**
         * Flatten repetitions without a recovery after the last rep; retain original mutable
         * output.
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun repetitions(count: Int, work: Step?, recovery: Step?): List<Step> {
            require(count in 1..100) { "Invalid repetition count" }
            val out = mutableListOf<Step?>()
            for (i in 0 until count) {
                out.add(work)
                if (i + 1 < count && recovery != null) out.add(recovery)
            }
            return out as List<Step>
        }
    }
}

private fun <T> executionSnapshot(values: Collection<T>): List<T> =
    object : AbstractList<T>() {
        private val rows = ArrayList(values)
        override val size: Int
            get() = rows.size

        override fun get(index: Int): T = rows[index]
    }
