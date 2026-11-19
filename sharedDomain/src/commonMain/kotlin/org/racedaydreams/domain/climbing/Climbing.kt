package org.racedaydreams.domain.climbing

import org.racedaydreams.domain.time.CalendarDates
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate
import org.racedaydreams.domain.time.ZoneOffset

/** Bouldering grading systems. A problem always keeps the system its grade was entered in. */
enum class GradeSystem(val label: String) {
    V_SCALE("V-scale"),
    FONTAINEBLEAU("Fontainebleau"),
}

/**
 * A boulder grade exactly as entered. Grades are compared only within their own system; any
 * cross-system value comes from [approximately] and must be presented as approximate.
 */
data class ClimbingGrade(val system: GradeSystem, val label: String) {
    init {
        require(label in labels(system)) { "Unknown ${system.label} grade: $label" }
    }

    /** Position within this grade's own system (higher is harder). */
    val rank: Int
        get() = labels(system).indexOf(label)

    override fun toString(): String = label

    companion object {
        val V_SCALE: List<String> = listOf("VB") + (0..17).map { "V$it" }
        val FONTAINEBLEAU: List<String> =
            listOf(
                "3",
                "4",
                "4+",
                "5",
                "5+",
                "6A",
                "6A+",
                "6B",
                "6B+",
                "6C",
                "6C+",
                "7A",
                "7A+",
                "7B",
                "7B+",
                "7C",
                "7C+",
                "8A",
                "8A+",
                "8B",
                "8B+",
                "8C",
                "8C+",
                "9A",
            )

        fun labels(system: GradeSystem): List<String> =
            when (system) {
                GradeSystem.V_SCALE -> V_SCALE
                GradeSystem.FONTAINEBLEAU -> FONTAINEBLEAU
            }

        fun of(system: GradeSystem, label: String) = ClimbingGrade(system, label)

        // Commonly published boulder-grade correspondence. Published charts disagree by about one
        // grade, so every converted value is approximate and never replaces the original grade.
        private val FONT_TO_V =
            mapOf(
                "3" to "VB",
                "4" to "V0",
                "4+" to "V0",
                "5" to "V1",
                "5+" to "V2",
                "6A" to "V3",
                "6A+" to "V3",
                "6B" to "V4",
                "6B+" to "V4",
                "6C" to "V5",
                "6C+" to "V5",
                "7A" to "V6",
                "7A+" to "V7",
                "7B" to "V8",
                "7B+" to "V8",
                "7C" to "V9",
                "7C+" to "V10",
                "8A" to "V11",
                "8A+" to "V12",
                "8B" to "V13",
                "8B+" to "V14",
                "8C" to "V15",
                "8C+" to "V16",
                "9A" to "V17",
            )
        private val V_TO_FONT =
            mapOf(
                "VB" to "3",
                "V0" to "4",
                "V1" to "5",
                "V2" to "5+",
                "V3" to "6A",
                "V4" to "6B",
                "V5" to "6C",
                "V6" to "7A",
                "V7" to "7A+",
                "V8" to "7B",
                "V9" to "7C",
                "V10" to "7C+",
                "V11" to "8A",
                "V12" to "8A+",
                "V13" to "8B",
                "V14" to "8B+",
                "V15" to "8C",
                "V16" to "8C+",
                "V17" to "9A",
            )

        /**
         * The approximately equivalent grade in [target], or the same grade when already in it.
         * Callers must label a converted result as approximate (for example "≈ V5").
         */
        fun approximately(grade: ClimbingGrade, target: GradeSystem): ClimbingGrade =
            if (grade.system == target) grade
            else
                when (target) {
                    GradeSystem.V_SCALE -> ClimbingGrade(target, FONT_TO_V.getValue(grade.label))
                    GradeSystem.FONTAINEBLEAU ->
                        ClimbingGrade(target, V_TO_FONT.getValue(grade.label))
                }
    }
}

/** What happened on a problem during this session. */
enum class ClimbOutcome(val label: String) {
    /** Sent on the first attempt of this session. */
    FLASH("Flash"),
    /** Sent after one or more attempts. */
    SEND("Sent"),
    /** Tried but not completed. */
    NOT_SENT("Not sent"),
}

/** One boulder problem tried in a session. An ungraded problem keeps a null grade. */
data class ClimbingProblem(
    val id: String,
    val grade: ClimbingGrade?,
    val attempts: Int,
    val outcome: ClimbOutcome,
    val name: String = "",
) {
    init {
        require(id.isNotBlank()) { "A problem needs an identity." }
        require(attempts in 1..MAX_ATTEMPTS) { "Attempts must be between 1 and $MAX_ATTEMPTS." }
        require(outcome != ClimbOutcome.FLASH || attempts == 1) {
            "A flash is a send on the first attempt."
        }
        require(name.length <= MAX_NAME) { "Keep the problem name to $MAX_NAME characters." }
    }

    fun sent(): Boolean = outcome != ClimbOutcome.NOT_SENT

    companion object {
        const val MAX_ATTEMPTS = 999
        const val MAX_NAME = 80
    }
}

/** A saved bouldering session: actual climbing, never weightlifting sets. */
data class ClimbingSession(
    val id: String,
    val startedAt: Instant,
    val offset: ZoneOffset,
    val durationSeconds: Long,
    val location: String,
    val notes: String,
    val problems: List<ClimbingProblem>,
) {
    init {
        require(id.isNotBlank()) { "A session needs an identity." }
        require(durationSeconds in MIN_DURATION..MAX_DURATION) {
            "Climbing time must be between 1 minute and 24 hours."
        }
        require(location.length <= MAX_LOCATION) {
            "Keep the location to $MAX_LOCATION characters."
        }
        require(notes.length <= MAX_NOTES) { "Keep notes to $MAX_NOTES characters." }
        require(problems.size <= MAX_PROBLEMS) {
            "A session can hold up to $MAX_PROBLEMS problems."
        }
        require(problems.map { it.id }.toSet().size == problems.size) {
            "Each problem must be recorded once."
        }
    }

    fun date(): LocalDate = CalendarDates.atOffset(startedAt, offset)

    fun attempts(): Int = problems.sumOf { it.attempts }

    fun sends(): Int = problems.count { it.sent() }

    fun flashes(): Int = problems.count { it.outcome == ClimbOutcome.FLASH }

    companion object {
        const val MIN_DURATION = 60L
        const val MAX_DURATION = 24 * 3600L
        const val MAX_LOCATION = 80
        const val MAX_NOTES = 2000
        const val MAX_PROBLEMS = 300
    }
}

/**
 * An unsaved session, kept on the device so an interrupted session can be resumed. A null
 * [durationSeconds] means the session is live and its time is measured from [startedAt]; a manual
 * log of a past session stores its entered duration.
 */
data class ClimbingDraft(
    val id: String,
    val startedAt: Instant,
    val offset: ZoneOffset,
    val durationSeconds: Long?,
    val location: String,
    val notes: String,
    val problems: List<ClimbingProblem>,
) {
    init {
        require(id.isNotBlank()) { "A session needs an identity." }
        require(durationSeconds == null || durationSeconds in 0..ClimbingSession.MAX_DURATION) {
            "Climbing time must be at most 24 hours."
        }
        require(location.length <= ClimbingSession.MAX_LOCATION) {
            "Keep the location to ${ClimbingSession.MAX_LOCATION} characters."
        }
        require(notes.length <= ClimbingSession.MAX_NOTES) {
            "Keep notes to ${ClimbingSession.MAX_NOTES} characters."
        }
        require(problems.size <= ClimbingSession.MAX_PROBLEMS) {
            "A session can hold up to ${ClimbingSession.MAX_PROBLEMS} problems."
        }
        require(problems.map { it.id }.toSet().size == problems.size) {
            "Each problem must be recorded once."
        }
    }

    fun live(): Boolean = durationSeconds == null

    /** Elapsed live time, never negative. */
    fun elapsedSeconds(now: Instant): Long =
        durationSeconds ?: (now.getEpochSecond() - startedAt.getEpochSecond()).coerceAtLeast(0)

    /** The session to save; validation explains anything that still needs correcting. */
    fun toSession(durationSeconds: Long): ClimbingSession =
        ClimbingSession(
            id,
            startedAt,
            offset,
            durationSeconds,
            location.trim(),
            notes.trim(),
            problems,
        )

    companion object {
        fun from(session: ClimbingSession) =
            ClimbingDraft(
                session.id,
                session.startedAt,
                session.offset,
                session.durationSeconds,
                session.location,
                session.notes,
                session.problems,
            )
    }
}
