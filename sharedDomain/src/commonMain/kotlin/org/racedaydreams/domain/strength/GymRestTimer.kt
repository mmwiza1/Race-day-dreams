package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import kotlin.math.max
import kotlin.math.min

/** Existing advisory monotonic countdown; never changes workout actuals or prescriptions. */
data class GymRestTimer(
    private val executionId: String,
    private val triggerId: String,
    private val phase: Phase,
    private val epoch: String,
    private val observedMillis: Long,
    private val remainingMillis: Long,
    private val durationMillis: Long,
) {
    enum class Phase {
        RUNNING,
        PAUSED,
        COMPLETE,
        SKIPPED,
        INTERRUPTED,
    }

    data class Clock(private val epoch: String, private val millis: Long) {
        init {
            requireRestId(epoch, "rest clock epoch")
            require(millis >= 0) { "Invalid monotonic clock." }
        }

        fun epoch(): String = epoch

        fun millis(): Long = millis

        override fun toString(): String = "Clock[epoch=$epoch, millis=$millis]"
    }

    data class Preferences(
        private val defaultSeconds: Int,
        private val suggestAfterSet: Boolean,
        private val notifications: Boolean,
        private val vibration: Boolean,
        private val audio: Boolean,
    ) {
        init {
            require(defaultSeconds in 0..1800) { "Choose 0–30 minutes of optional rest." }
        }

        fun defaultSeconds(): Int = defaultSeconds

        fun suggestAfterSet(): Boolean = suggestAfterSet

        fun notifications(): Boolean = notifications

        fun vibration(): Boolean = vibration

        fun audio(): Boolean = audio

        override fun toString(): String =
            "Preferences[defaultSeconds=$defaultSeconds, suggestAfterSet=$suggestAfterSet, notifications=$notifications, vibration=$vibration, audio=$audio]"

        companion object {
            @JvmStatic fun defaults(): Preferences = Preferences(90, true, false, false, false)
        }
    }

    init {
        requireRestId(executionId, "rest workout")
        requireRestId(triggerId, "rest start")
        Clock(epoch, observedMillis)
        require(
            !(observedMillis > Long.MAX_VALUE - durationMillis ||
                durationMillis < 0 ||
                durationMillis > 3_600_000 ||
                remainingMillis < 0 ||
                remainingMillis > durationMillis ||
                (phase == Phase.COMPLETE || phase == Phase.SKIPPED) && remainingMillis != 0L)
        ) {
            "Invalid rest duration/state."
        }
    }

    fun executionId(): String = executionId

    fun triggerId(): String = triggerId

    fun phase(): Phase = phase

    fun epoch(): String = epoch

    fun observedMillis(): Long = observedMillis

    fun remainingMillis(): Long = remainingMillis

    fun durationMillis(): Long = durationMillis

    fun secondsRemaining(): Long = (remainingMillis + 999) / 1000

    /** Read-only observation; wall-clock changes never change remaining rest. */
    fun observe(now: Clock): GymRestTimer {
        if (phase != Phase.RUNNING) return this
        if (epoch != now.epoch() || now.millis() < observedMillis)
            return transition(Phase.INTERRUPTED, now, remainingMillis, durationMillis)
        val remaining = max(0, remainingMillis - (now.millis() - observedMillis))
        return transition(
            if (remaining == 0L) Phase.COMPLETE else Phase.RUNNING,
            now,
            remaining,
            durationMillis,
        )
    }

    fun pause(now: Clock): GymRestTimer {
        val current = observe(now)
        return if (current.phase == Phase.RUNNING)
            current.transition(Phase.PAUSED, now, current.remainingMillis, durationMillis)
        else current
    }

    /** Explicitly resumes interruptions without inventing an unknown elapsed interval. */
    fun resume(now: Clock): GymRestTimer {
        if (phase != Phase.PAUSED && phase != Phase.INTERRUPTED) return observe(now)
        return transition(
            if (remainingMillis == 0L) Phase.COMPLETE else Phase.RUNNING,
            now,
            remainingMillis,
            durationMillis,
        )
    }

    fun reset(now: Clock): GymRestTimer =
        transition(
            if (durationMillis == 0L) Phase.COMPLETE else Phase.RUNNING,
            now,
            durationMillis,
            durationMillis,
        )

    fun skip(now: Clock): GymRestTimer = transition(Phase.SKIPPED, now, 0, durationMillis)

    fun adjust(seconds: Int, now: Clock): GymRestTimer {
        require(seconds in -1800..1800) { "Adjust up to30minutes." }
        val current = observe(now)
        val remaining = max(0, min(3_600_000, current.remainingMillis + seconds * 1000L))
        val next =
            when {
                remaining == 0L -> Phase.COMPLETE
                current.phase == Phase.PAUSED || current.phase == Phase.INTERRUPTED -> current.phase
                else -> Phase.RUNNING
            }
        val duration = max(0, min(3_600_000, current.durationMillis + seconds * 1000L))
        return transition(next, now, remaining, duration)
    }

    private fun transition(next: Phase, now: Clock, remaining: Long, duration: Long): GymRestTimer =
        GymRestTimer(executionId, triggerId, next, now.epoch(), now.millis(), remaining, duration)

    override fun toString(): String =
        "GymRestTimer[executionId=$executionId, triggerId=$triggerId, phase=$phase, epoch=$epoch, observedMillis=$observedMillis, remainingMillis=$remainingMillis, durationMillis=$durationMillis]"

    companion object {
        /**
         * The platform caller projects the optional prescription; no workout model is duplicated.
         */
        @JvmStatic
        fun suggestedSeconds(prescribedSeconds: Int?, preferences: Preferences): Int =
            prescribedSeconds ?: preferences.defaultSeconds()

        @JvmStatic
        fun start(execution: String, trigger: String, seconds: Int, now: Clock): GymRestTimer {
            require(seconds in 0..1800) { "Choose 0–30 minutes." }
            return GymRestTimer(
                execution,
                trigger,
                if (seconds == 0) Phase.COMPLETE else Phase.RUNNING,
                now.epoch(),
                now.millis(),
                seconds * 1000L,
                seconds * 1000L,
            )
        }
    }
}

private fun requireRestId(value: String?, label: String) {
    require(value != null && value.trim { it <= ' ' }.isNotEmpty() && value.length <= 256) {
        "A stable $label ID is required."
    }
}
