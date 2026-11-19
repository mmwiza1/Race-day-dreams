package org.racedaydreams.domain.execution

import kotlin.math.abs
import org.racedaydreams.domain.recording.RunMetricsEngine

/**
 * Original session cue arbitration, distinct from platform audio and its state-announcement clock.
 */
class RecordingCueCoordinator {
    data class RaceTarget(private val metres: Double, private val targetSeconds: Long?) {
        fun metres(): Double = metres

        fun targetSeconds(): Long? = targetSeconds

        override fun toString(): String = "RaceTarget[metres=$metres, targetSeconds=$targetSeconds]"
    }

    data class Update(
        private val selected: CoachCues.Cue?,
        private val paceEvent: String?,
        private val raceContext: String?,
    ) {
        fun selected(): CoachCues.Cue? = selected

        fun paceEvent(): String? = paceEvent

        fun raceContext(): String? = raceContext

        override fun toString(): String =
            "Update[selected=$selected, paceEvent=$paceEvent, raceContext=$raceContext]"
    }

    private val paceCues = CoachCues()
    private var lastSplit = 0
    private var gpsWasLost = false
    private var lastGpsAnnouncement = -60000L
    private var hadHeartRate = false
    private var raceHalf = false
    private var raceFive = false

    /** Initial connection failure is not loss of a connected sensor. */
    fun heartRateConnection(connected: Boolean): Boolean {
        if (connected) hadHeartRate = true
        else if (hadHeartRate) {
            hadHeartRate = false
            return true
        }
        return false
    }

    fun update(
        metrics: RunMetricsEngine.Status,
        execution: WorkoutExecutionCoordinator.Update?,
        steps: List<WorkoutExecutionEngine.Step>,
        race: RaceTarget?,
        hilly: Boolean,
        sustainedMillis: Long,
        miles: Boolean,
        announceSplits: Boolean,
    ): Update {
        val candidates = mutableListOf<CoachCues.Cue>()
        val active = metrics.movingMillis()
        val lost =
            !metrics.paused() &&
                (metrics.gps()!!.contains("interrupted") ||
                    metrics.gps()!!.contains("temporarily lost"))
        if (lost && !gpsWasLost && metrics.elapsedMillis() - lastGpsAnnouncement >= 60000) {
            lastGpsAnnouncement = metrics.elapsedMillis()
            candidates.add(
                CoachCues.Cue(
                    "gps-lost",
                    "GPS interrupted. Distance is incomplete; continue by effort.",
                    0,
                    active,
                    false,
                )
            )
        }
        gpsWasLost = lost
        var raceContext: String? = null
        if (race != null) {
            val target = race.targetSeconds()
            if (target == null) raceContext = "Race elapsed time includes pauses."
            else {
                val margin =
                    target * metrics.metres() / race.metres() - metrics.elapsedMillis() / 1000.0
                raceContext =
                    "${abs(margin).toInt()} sec ${if (margin >= 0) "ahead of" else "behind"} chosen average, relative to recorded GPS distance. Elapsed timing includes pauses."
            }
            if (!raceHalf && metrics.metres() >= race.metres() / 2) {
                raceHalf = true
                candidates.add(
                    CoachCues.Cue(
                        "race-half",
                        "Halfway by recorded distance. Keep your effort controlled.",
                        2,
                        active,
                        false,
                    )
                )
            }
            if (!raceFive && race.metres() > 10000 && metrics.metres() >= race.metres() - 5000) {
                raceFive = true
                candidates.add(
                    CoachCues.Cue(
                        "race-five",
                        "Five kilometres remaining by recorded distance.",
                        2,
                        active,
                        false,
                    )
                )
            }
        }
        var paceEvent: String? = null
        if (execution != null) {
            for (event in execution.newEvents()) {
                val type = event.type() ?: throw NullPointerException()
                if (type == "STEP_STARTED" && event.step() < steps.size) {
                    val step = steps[event.step()]
                    candidates.add(
                        CoachCues.Cue(
                            "step-${event.step()}",
                            "Step ${event.step() + 1} of ${steps.size}. ${step.label()}",
                            1,
                            active,
                            true,
                        )
                    )
                } else if (type == "WORKOUT_COMPLETED")
                    candidates.add(
                        CoachCues.Cue(
                            "complete",
                            "Workout complete. Recording continues until you finish and save.",
                            1,
                            active,
                            true,
                        )
                    )
            }
            if (!hilly) {
                val input =
                    WorkoutExecutionEngine.Input(
                        metrics.metres(),
                        active,
                        metrics.elapsedMillis(),
                        metrics.secondsPerKm(),
                        metrics.hasReliablePace(),
                        metrics.paused(),
                    )
                val pace =
                    paceCues.pace(
                        execution.state() ?: throw NullPointerException(),
                        input,
                        sustainedMillis,
                    )
                if (pace != null) {
                    candidates.add(pace)
                    paceEvent =
                        if (pace.id().startsWith("pace-in")) "TARGET_RANGE_REENTERED"
                        else "TARGET_RANGE_EXITED"
                }
            }
        }
        val unit = if (miles) 1609.344 else 1000.0
        val split = (metrics.metres() / unit).toInt()
        if (split > lastSplit) {
            lastSplit = split
            if (announceSplits)
                candidates.add(
                    CoachCues.Cue(
                        "split-$split",
                        "$split${if (miles) " miles" else " kilometres"} recorded.",
                        5,
                        active,
                        false,
                    )
                )
        }
        return Update(CoachCues.select(candidates, active), paceEvent, raceContext)
    }
}
