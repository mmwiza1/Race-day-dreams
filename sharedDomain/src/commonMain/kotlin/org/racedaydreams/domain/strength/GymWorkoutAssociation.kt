package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import kotlin.math.abs
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.*

/** Explicit post-workout choices. Association never claims that every target was performed. */
object GymWorkoutAssociation {
    const val FORMAT_VERSION: Int = 1

    enum class Choice {
        KEEP_EXTRA,
        COUNT_TOWARD_SESSION,
    }

    data class Proposal(
        private val actual: StrengthSession?,
        private val origin: GymExecution.Origin?,
        private val programmeId: String?,
        private val programmeRevision: Int,
        private val prescription: GymProgramme.Session?,
        private val reviewedAt: Instant?,
        private val authoredPrescription: StrengthProgramme.Workout?,
    ) {
        constructor(
            actual: StrengthSession?,
            origin: GymExecution.Origin?,
            programmeId: String?,
            programmeRevision: Int,
            prescription: GymProgramme.Session?,
            reviewedAt: Instant?,
        ) : this(actual, origin, programmeId, programmeRevision, prescription, reviewedAt, null)

        init {
            if (actual == null) throw NullPointerException("reviewed actual")
            if (origin == null) throw NullPointerException("original workout origin")
            require((prescription == null) != (authoredPrescription == null)) {
                "Review exactly one preserved prescription family."
            }
            if (reviewedAt == null) throw NullPointerException("review time")
            GymWorkoutDefinition.requireId(programmeId, "programme")
            require(programmeRevision >= 1 && origin != GymExecution.Origin.PLANNED) {
                "Choose an independent completed workout."
            }
            require(!completion(actual).isAfter(reviewedAt)) {
                "Only completed actual evidence can be associated."
            }
        }

        fun actual(): StrengthSession = actual!!

        fun origin(): GymExecution.Origin = origin!!

        fun programmeId(): String = programmeId!!

        fun programmeRevision(): Int = programmeRevision

        fun prescription(): GymProgramme.Session? = prescription

        fun reviewedAt(): Instant = reviewedAt!!

        fun authoredPrescription(): StrengthProgramme.Workout? = authoredPrescription

        fun authored(): Boolean = authoredPrescription != null

        fun sessionId(): String = authoredPrescription?.id() ?: prescription!!.id()

        fun date(): LocalDate = authoredPrescription?.date() ?: prescription!!.date()

        fun title(): String = authoredPrescription?.definition()?.title() ?: prescription!!.title()

        fun prescribedExerciseCount(): Int =
            authoredPrescription?.definition()?.slots()?.size ?: prescription!!.exercises().size

        fun reviewed(
            latestActual: StrengthSession?,
            retainedOrigin: GymExecution.Origin?,
            at: Instant?,
        ): Proposal =
            Proposal(
                latestActual,
                retainedOrigin,
                programmeId,
                programmeRevision,
                prescription,
                at,
                authoredPrescription,
            )

        override fun toString(): String =
            "Proposal[actual=$actual, origin=$origin, programmeId=$programmeId, programmeRevision=$programmeRevision, prescription=$prescription, reviewedAt=$reviewedAt, authoredPrescription=$authoredPrescription]"

        companion object {
            @JvmStatic
            fun authored(
                actual: StrengthSession?,
                origin: GymExecution.Origin?,
                programmeId: String?,
                revision: Int,
                prescription: StrengthProgramme.Workout?,
                at: Instant?,
            ): Proposal = Proposal(actual, origin, programmeId, revision, null, at, prescription)
        }
    }

    data class Decision(
        private val id: String?,
        private val proposal: Proposal?,
        private val choice: Choice?,
        private val decidedAt: Instant?,
    ) {
        init {
            GymWorkoutDefinition.requireId(id, "association decision")
            if (proposal == null) throw NullPointerException("reviewed proposal")
            if (choice == null) throw NullPointerException("athlete choice")
            if (decidedAt == null) throw NullPointerException("decision time")
            require(!decidedAt.isBefore(proposal.reviewedAt())) {
                "A decision cannot precede its review."
            }
        }

        fun id(): String = id!!

        fun proposal(): Proposal = proposal!!

        fun choice(): Choice = choice!!

        fun decidedAt(): Instant = decidedAt!!

        override fun toString(): String =
            "Decision[id=$id, proposal=$proposal, choice=$choice, decidedAt=$decidedAt]"
    }

    /** Discovery only: within three stored local days with an exact working exercise/task. */
    @JvmStatic
    fun proposals(
        actual: StrengthSession?,
        origin: GymExecution.Origin?,
        programme: GymProgramme?,
        occupiedSessionIds: Set<String?>?,
        now: Instant?,
    ): List<Proposal> {
        if (actual == null) throw NullPointerException()
        if (origin == null) throw NullPointerException()
        if (occupiedSessionIds == null) throw NullPointerException()
        if (now == null) throw NullPointerException()
        if (
            programme == null ||
                origin == GymExecution.Origin.PLANNED ||
                completion(actual).isAfter(now)
        )
            return Immutable.list()
        val working = working(actual)
        val day = CalendarDates.atOffset(actual.startedAt(), actual.offset())
        val out = mutableListOf<Proposal>()
        for (session in programme.sessions()) {
            if (
                session.id() in occupiedSessionIds ||
                    abs(session.date().toEpochDay() - day.toEpochDay()) > 3 ||
                    session.exercises().none {
                        Task(
                            it.exerciseId(),
                            it.minimumReps() > 0,
                            it.seconds() != null,
                            it.metres() != null,
                        ) in working
                    }
            )
                continue
            out.add(Proposal(actual, origin, programme.id(), programme.version(), session, now))
        }
        return Immutable.copyList(out)
    }

    @JvmStatic
    fun authoredProposals(
        actual: StrengthSession?,
        origin: GymExecution.Origin?,
        programme: StrengthProgramme?,
        occupiedSessionIds: Set<String?>?,
        now: Instant?,
    ): List<Proposal> {
        if (actual == null) throw NullPointerException()
        if (origin == null) throw NullPointerException()
        if (occupiedSessionIds == null) throw NullPointerException()
        if (now == null) throw NullPointerException()
        if (
            programme == null ||
                origin == GymExecution.Origin.PLANNED ||
                completion(actual).isAfter(now)
        )
            return Immutable.list()
        val working = working(actual)
        val day = CalendarDates.atOffset(actual.startedAt(), actual.offset())
        val out = mutableListOf<Proposal>()
        for (session in programme.workouts()) {
            if (
                session.id() in occupiedSessionIds ||
                    abs(session.date().toEpochDay() - day.toEpochDay()) > 3 ||
                    session.definition().slots().none { slot ->
                        val task = slot.exercise().measurement()
                        slot.target() != null &&
                            Task(
                                slot.exercise().id(),
                                task.usesReps(),
                                task.usesDuration(),
                                task.usesDistance(),
                            ) in working
                    }
            )
                continue
            out.add(
                Proposal.authored(
                    actual,
                    origin,
                    programme.id(),
                    programme.revision(),
                    session,
                    now,
                )
            )
        }
        return Immutable.copyList(out)
    }

    private data class Task(
        val exerciseId: String,
        val reps: Boolean,
        val duration: Boolean,
        val distance: Boolean,
    )

    private fun working(actual: StrengthSession): Set<Task> =
        actual
            .exercises()
            .filter { it.sets().any { set -> !set.warmUp() } }
            .map {
                val measurement = it.exercise().measurement()
                Task(
                    it.exercise().id(),
                    measurement.usesReps(),
                    measurement.usesDuration(),
                    measurement.usesDistance(),
                )
            }
            .toSet()

    private fun completion(actual: StrengthSession): Instant =
        Instants.ofEpochSecond(
            checkedAdd(actual.startedAt().getEpochSecond(), actual.duration().getSeconds()),
            actual.startedAt().getNano().toLong() + actual.duration().getNano(),
        )

    @JvmStatic
    fun validateAuthoredForAcceptance(
        proposal: Proposal?,
        currentActual: StrengthSession?,
        origin: GymExecution.Origin?,
        programme: StrengthProgramme?,
        occupied: Set<String?>?,
        linked: Boolean,
    ) {
        validateAuthoredRetainedEvidence(proposal, currentActual, origin, programme)
        if (occupied == null) throw NullPointerException()
        val p = proposal!!
        require(!linked && p.sessionId() !in occupied) {
            "This workout or target already has an association. Review it again."
        }
        require(
            p in authoredProposals(currentActual, origin, programme, occupied, p.reviewedAt())
        ) {
            "No comparable nearby prescription was reviewed."
        }
    }

    @JvmStatic
    fun validateAuthoredRetainedEvidence(
        proposal: Proposal?,
        currentActual: StrengthSession?,
        origin: GymExecution.Origin?,
        programme: StrengthProgramme?,
    ) {
        if (proposal == null) throw NullPointerException()
        if (origin == null) throw NullPointerException()
        require(
            proposal.authored() &&
                proposal.origin() == origin &&
                proposal.actual() == currentActual &&
                programme != null &&
                proposal.programmeId() == programme.id() &&
                proposal.programmeRevision() == programme.revision() &&
                proposal.authoredPrescription() in programme.workouts()
        ) {
            "The reviewed actual or preserved prescription changed."
        }
    }

    @JvmStatic
    fun validateForAcceptance(
        proposal: Proposal?,
        currentActual: StrengthSession?,
        recordedOrigin: GymExecution.Origin?,
        currentProgramme: GymProgramme?,
        occupiedSessionIds: Set<String?>?,
        actualLinked: Boolean,
    ) {
        validateRetainedEvidence(proposal, currentActual, recordedOrigin, currentProgramme)
        if (occupiedSessionIds == null) throw NullPointerException()
        val p = proposal!!
        require(!actualLinked && p.prescription()!!.id() !in occupiedSessionIds) {
            "This workout or target already has an association. Review it again."
        }
        require(
            p in
                proposals(
                    currentActual,
                    p.origin(),
                    currentProgramme,
                    occupiedSessionIds,
                    p.reviewedAt(),
                )
        ) {
            "No comparable nearby prescription was reviewed."
        }
    }

    @JvmStatic
    fun validateRetainedEvidence(
        proposal: Proposal?,
        currentActual: StrengthSession?,
        recordedOrigin: GymExecution.Origin?,
        retainedProgramme: GymProgramme?,
    ) {
        if (proposal == null) throw NullPointerException()
        if (recordedOrigin == null) throw NullPointerException("retained workout provenance")
        require(
            !proposal.authored() &&
                proposal.origin() == recordedOrigin &&
                proposal.actual() == currentActual &&
                retainedProgramme != null &&
                proposal.programmeId() == retainedProgramme.id() &&
                proposal.programmeRevision() == retainedProgramme.version() &&
                proposal.prescription() in retainedProgramme.sessions()
        ) {
            "The reviewed actual or preserved prescription changed."
        }
    }
}
