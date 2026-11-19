package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.*

/** Optional task-specific progression. No actual, reference or programme is mutated. */
object StrengthTargetProgression {
    data class Advice(
        private val action: StrengthProgressionAdvisor.Action?,
        private val reason: String?,
        private val proposedTarget: GymWorkoutDefinition.Target?,
        private var supportingSessionIds: List<String>?,
    ) {
        init {
            if (action == null) throw NullPointerException()
            if (reason == null) throw NullPointerException()
            supportingSessionIds =
                Immutable.copyList(supportingSessionIds ?: throw NullPointerException())
            require(
                (action == StrengthProgressionAdvisor.Action.CONSIDER_INCREASE) ==
                    (proposedTarget != null)
            ) {
                "A progression target is an optional increase only."
            }
        }

        fun action(): StrengthProgressionAdvisor.Action = action!!

        fun reason(): String = reason!!

        fun proposedTarget(): GymWorkoutDefinition.Target? = proposedTarget

        fun supportingSessionIds(): List<String> = supportingSessionIds!!

        override fun toString(): String =
            "Advice[action=$action, reason=$reason, proposedTarget=$proposedTarget, supportingSessionIds=$supportingSessionIds]"
    }

    private data class Evidence(
        val session: StrengthSession,
        val sets: List<StrengthSession.SetEntry>,
    )

    @JvmStatic
    fun review(
        slot: GymWorkoutDefinition.Slot,
        history: List<StrengthSession>,
        asOf: Instant,
    ): Advice {
        val target = slot.target()
        val exercise = slot.exercise()
        val canonical = ExerciseCatalog.definition(exercise.id())
        if (
            target == null ||
                canonical == null ||
                canonical.exercise().measurement() != exercise.measurement() ||
                canonical.exercise().mode() != exercise.mode() ||
                canonical.exercise().region() != exercise.region()
        )
            return Advice(
                StrengthProgressionAdvisor.Action.GATHER_EVIDENCE,
                "An explicit supported task and comparable working evidence are needed. No custom-exercise progression is inferred.",
                null,
                emptyList(),
            )
        val recent =
            history.sortedWith(
                compareByDescending<StrengthSession> { it.startedAt() }.thenBy { it.id() }
            )
        val evidence = mutableListOf<Evidence>()
        val seen = mutableSetOf<String>()
        for (session in recent) {
            if (
                session
                    .startedAt()
                    .isBefore(
                        Instants.ofEpochSecond(
                            asOf.getEpochSecond() - 28L * 24 * 60 * 60,
                            asOf.getNano().toLong(),
                        )
                    ) || session.startedAt().isAfter(asOf) || !seen.add(session.id())
            )
                continue
            val sets = mutableListOf<StrengthSession.SetEntry>()
            for (block in session.exercises()) if (
                block.exercise().id() == exercise.id() &&
                    block.exercise().measurement() == exercise.measurement() &&
                    block.exercise().mode() == exercise.mode()
            )
                for (set in block.sets()) if (!set.warmUp()) sets.add(set)
            if (sets.isNotEmpty()) evidence.add(Evidence(session, sets))
            if (evidence.size == 2) break
        }
        val ids = evidence.map { it.session.id() }
        if (evidence.isEmpty())
            return Advice(
                StrengthProgressionAdvisor.Action.GATHER_EVIDENCE,
                "No recent exact-exercise working sets support a change. Older, future or warm-up evidence does not count.",
                null,
                ids,
            )
        val latest = evidence[0].sets
        val effort =
            target.effort()
                ?: return Advice(
                    StrengthProgressionAdvisor.Action.HOLD,
                    "No intended effort was specified. Review effort before considering progression.",
                    null,
                    ids,
                )
        val maximumRpe =
            if (effort.scale() == GymWorkoutDefinition.EffortScale.RIR) 10 - effort.minimum()
            else effort.maximum()
        if (latest.size < target.sets())
            return Advice(
                StrengthProgressionAdvisor.Action.HOLD,
                "The latest session has fewer working sets than prescribed; repeat and review.",
                null,
                ids,
            )
        if (
            latest.any { s ->
                s.coachingRpe()?.let { it > maximumRpe + .5 } == true || !reachedMinimum(s, target)
            }
        )
            return Advice(
                StrengthProgressionAdvisor.Action.REDUCE_OR_SIMPLIFY,
                "Recent sets were harder or shorter than intended. Consider less load or a simpler task; nothing changes automatically.",
                null,
                ids,
            )
        if (evidence.size < 2)
            return Advice(
                StrengthProgressionAdvisor.Action.HOLD,
                "One recent session is useful evidence. Repeat before considering progression.",
                null,
                ids,
            )
        val load = latest[0].kilograms()
        for (e in evidence) if (
            e.sets.size < target.sets() ||
                e.sets.any { s ->
                    s.coachingRpe() == null ||
                        s.coachingRpe()!! > maximumRpe ||
                        !reachedTop(s, target) ||
                        s.kilograms() != load
                }
        )
            return Advice(
                StrengthProgressionAdvisor.Action.HOLD,
                "Two recent comparable sessions at the same loading and intended recorded effort are required. Missing effort is unknown.",
                null,
                ids,
            )
        if (exercise.measurement() == StrengthExercise.MeasurementProfile.LOAD_REPS && load > 0) {
            if (target.sets() > 6 || target.maximumReps()!! > 100)
                return Advice(
                    StrengthProgressionAdvisor.Action.HOLD,
                    "This task is outside the established load-advice bounds; keep your chosen load.",
                    null,
                    ids,
                )
            val lowRpe =
                max(
                        1.0,
                        ceil(
                            if (effort.scale() == GymWorkoutDefinition.EffortScale.RIR)
                                10 - effort.maximum()
                            else effort.minimum()
                        ),
                    )
                    .toInt()
            val highRpe = floor(maximumRpe).toInt()
            if (highRpe < lowRpe)
                return Advice(
                    StrengthProgressionAdvisor.Action.HOLD,
                    "Use a comparable effort range before seeking a numeric load reference.",
                    null,
                    ids,
                )
            val prescription =
                GymProgramme.Prescription(
                    exercise.id(),
                    target.sets(),
                    target.minimumReps()!!,
                    target.maximumReps()!!,
                    null,
                    null,
                    lowRpe,
                    highRpe,
                    90,
                    "Exact preserved exercise target",
                )
            val observations =
                evidence.map { e ->
                    StrengthSession(
                        e.session.id(),
                        e.session.startedAt(),
                        e.session.offset(),
                        e.session.duration(),
                        e.session.rpe(),
                        e.session.notes(),
                        Immutable.list(StrengthSession.ExerciseSets(exercise, e.sets)),
                    )
                }
            val established = StrengthProgressionAdvisor.review(prescription, observations, asOf)
            if (established.action() != StrengthProgressionAdvisor.Action.CONSIDER_INCREASE)
                return Advice(established.action(), established.reason(), null, ids)
            if (
                target.referenceLoadKg() != null &&
                    established.suggestedKilograms()!! <= target.referenceLoadKg()!!
            )
                return Advice(
                    StrengthProgressionAdvisor.Action.HOLD,
                    "A newer optional load reference already exists. Review actual loading before replacing it.",
                    null,
                    ids,
                )
            return Advice(
                established.action(),
                established.reason(),
                GymWorkoutDefinition.Target(
                    target.sets(),
                    target.minimumReps(),
                    target.maximumReps(),
                    null,
                    null,
                    effort,
                    target.restSeconds(),
                    established.suggestedKilograms(),
                ),
                ids,
            )
        }
        var low = target.minimumReps()
        var high = target.maximumReps()
        var seconds = target.seconds()
        var metres = target.metres()
        if (high != null && high < 500) {
            low = low!! + 1
            high++
        } else if (seconds != null && seconds <= 43195) seconds += if (seconds >= 30) 5 else 1
        else if (metres != null && metres <= 99995) metres += if (metres >= 20) 5 else 1
        else
            return Advice(
                StrengthProgressionAdvisor.Action.HOLD,
                "Keep this task within the supported measurement bounds.",
                null,
                ids,
            )
        val reference =
            if (
                exercise.measurement().usesLoad() &&
                    exercise.mode() != StrengthExercise.Mode.BODYWEIGHT
            )
                load
            else null
        return Advice(
            StrengthProgressionAdvisor.Action.CONSIDER_INCREASE,
            "Two recent exact-exercise sessions reached the intended task at recorded effort. A small rep/time/distance increase at the same proven load is optional; do not assume added load is equivalent.",
            GymWorkoutDefinition.Target(
                target.sets(),
                low,
                high,
                seconds,
                metres,
                effort,
                target.restSeconds(),
                reference,
            ),
            ids,
        )
    }

    private fun reachedMinimum(
        s: StrengthSession.SetEntry,
        t: GymWorkoutDefinition.Target,
    ): Boolean =
        if (t.minimumReps() != null) s.reps() >= t.minimumReps()!!
        else if (t.seconds() != null)
            s.durationSeconds() != null && s.durationSeconds()!! >= t.seconds()!!
        else s.distanceMetres() != null && s.distanceMetres()!! >= t.metres()!!

    private fun reachedTop(s: StrengthSession.SetEntry, t: GymWorkoutDefinition.Target): Boolean =
        if (t.maximumReps() != null) s.reps() >= t.maximumReps()!! else reachedMinimum(s, t)
}
