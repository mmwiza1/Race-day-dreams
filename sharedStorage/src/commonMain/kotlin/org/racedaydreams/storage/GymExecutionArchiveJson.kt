package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.GymExecutionRepository.Preference
import org.racedaydreams.domain.strength.GymExecutionRepository.Stored
import org.racedaydreams.domain.time.LocalDate
import org.racedaydreams.storage.GymStoredJson.array
import org.racedaydreams.storage.GymStoredJson.bool
import org.racedaydreams.storage.GymStoredJson.child
import org.racedaydreams.storage.GymStoredJson.fields
import org.racedaydreams.storage.GymStoredJson.integer
import org.racedaydreams.storage.GymStoredJson.isNull
import org.racedaydreams.storage.GymStoredJson.row
import org.racedaydreams.storage.GymStoredJson.text
import org.racedaydreams.storage.GymStoredJson.whole

/** Original strength-engine v1/v2 graph. No actual replacement, finalisation or source mutation. */
object GymExecutionArchiveJson {
    data class WorkoutKey(val id: String, val revision: Int)

    class Snapshot
    internal constructor(
        workouts: Map<WorkoutKey, FrozenGymWorkoutRecordJson.Record>,
        preferences: List<Preference>,
        executions: Map<String, SqliteGymExecutionRecords.Evidence>,
        associations: GymAssociationArchiveJson.Archive?,
        internal val payload: JsonObject,
    ) {
        val associations =
            associations?.let {
                GymAssociationArchiveJson.Archive(
                    Immutable.copyList(it.decisions),
                    Immutable.copyMap(it.current),
                )
            }
        val workouts = Immutable.copyMap(workouts)
        val preferences = Immutable.copyList(preferences)
        val executions = Immutable.copyMap(executions)
    }

    /** All source sections and parent syntax/32MiB budget must be validated before this call. */
    fun decode(
        root: JsonObject,
        actuals: Map<String, StrengthSession>,
        programmes: Map<String, GymProgramme>,
        links: Map<String, GymAssociationArchiveJson.Link>,
        authored: Map<String, StrengthProgramme>,
        fingerprints: LegacyStrengthFingerprint,
    ): Snapshot {
        val version = integer(root, "version")
        when (version) {
            1 -> fields(root, "version", "workouts", "preferences", "executions")
            2 -> fields(root, "version", "workouts", "preferences", "executions", "associations")
            else -> error("Unsupported strength engine backup version.")
        }
        val workouts = linkedMapOf<WorkoutKey, FrozenGymWorkoutRecordJson.Record>()
        for (item in array(root, "workouts", 10000)) {
            val parsed = FrozenGymWorkoutRecordJson.decode(row(item))
            val w =
                parsed.copy(
                    sourceSnapshot =
                        parsed.sourceSnapshot?.let {
                            it.copy(
                                originalSourceSlots = Immutable.copyList(it.originalSourceSlots)
                            )
                        }
                )
            LegacyGymRowJson.check(FrozenGymWorkoutRecordJson.element(w))
            require(
                workouts.put(WorkoutKey(w.definition.id(), w.definition.revision()), w) == null
            ) {
                "Duplicate frozen workout revision."
            }
            if (w.source?.kind() == GymExecution.SourceKind.PROGRAMME)
                programmeDate(w.definition, w.source, programmes, authored)
        }
        val ids = mutableSetOf<String>()
        val preferences =
            array(root, "preferences", 10000).map { item ->
                val p = row(item)
                fields(p, "id", "currentRevision", "favourite")
                val v =
                    Preference(
                        text(p, "id", 256),
                        integer(p, "currentRevision"),
                        bool(p, "favourite"),
                    )
                require(
                    ids.add(v.workoutId()) &&
                        WorkoutKey(v.workoutId(), v.currentRevision()) in workouts
                ) {
                    "Saved workout preference is duplicated or missing its revision."
                }
                v
            }
        val executions = linkedMapOf<String, SqliteGymExecutionRecords.Evidence>()
        var unfinished = 0
        for (item in array(root, "executions", 100000)) {
            val r = row(item)
            fields(r, "revision", "snapshot", "currentActual", "actualEditedAt")
            val revision = whole(r, "revision")
            require(revision >= 1) { "A stored execution needs a positive revision." }
            val e = GymExecutionSnapshotJson.decode(child(r, "snapshot"))
            checkParts(e)
            require(e.id() !in executions) { "Duplicate gym execution." }
            val source = e.source()
            require(
                source?.kind() != GymExecution.SourceKind.ACTUAL_SESSION || source.id() != e.id()
            ) {
                "A gym execution cannot repeat itself."
            }
            val prescribed = e.prescription()
            if (prescribed != null) {
                val w = workouts[WorkoutKey(prescribed.id(), prescribed.revision())]
                require(w != null && w.definition == prescribed) {
                    "The execution's exact workout snapshot is missing."
                }
                if (source?.kind() == GymExecution.SourceKind.SAVED_WORKOUT)
                    GymWorkoutSourceValidator.savedWorkout(prescribed, source, w.definition)
                else require(source == w.source) { "Execution and workout source disagree." }
            } else require(source == null) { "A source requires its workout." }
            val current: StrengthSession?
            val edited: org.racedaydreams.domain.time.Instant?
            if (e.status() == GymExecution.Status.FINISHED) {
                LegacyGymRowJson.check(StrengthEvidenceJson.session(e.completedSession()!!))
                current = StrengthEvidenceJson.readAcceptedSession(child(r, "currentActual"))
                LegacyGymRowJson.check(StrengthEvidenceJson.session(current))
                require(current.id() == e.id() && current == actuals[e.id()]) {
                    "Accepted gym evidence is missing or differs from its actual workout."
                }
                edited =
                    if (isNull(r, "actualEditedAt")) null
                    else StoredInstant.parse(text(r, "actualEditedAt", 64))
                require(current == e.completedSession() || edited != null) {
                    "A changed actual workout needs its explicit edit observation."
                }
                if (e.plannedAssociation() != null) {
                    val date =
                        programmeDate(
                            requireNotNull(prescribed),
                            requireNotNull(source),
                            programmes,
                            authored,
                        )
                    require(
                        links[e.id()] ==
                            GymAssociationArchiveJson.Link(
                                e.id(),
                                "${source.id()}:v${source.revision()}",
                                source.sessionId(),
                                date.toString(),
                            )
                    ) {
                        "The gym execution association differs from its original prescription."
                    }
                }
            } else {
                require(
                    isNull(r, "currentActual") && isNull(r, "actualEditedAt") && e.id() !in actuals
                ) {
                    "Unfinished or discarded execution cannot claim completed actual evidence."
                }
                current = null
                edited = null
                if (e.status() != GymExecution.Status.DISCARDED) {
                    unfinished++
                    require(unfinished <= 1) {
                        "A backup contains several unfinished gym workouts."
                    }
                }
            }
            executions[e.id()] =
                SqliteGymExecutionRecords.Evidence(Stored(e, revision), current, edited)
        }
        val retained = executions.mapValues { it.value.original.execution() }
        for (w in workouts.values) if (w.source?.kind() == GymExecution.SourceKind.ACTUAL_SESSION)
            FrozenGymWorkoutRecordJson.validateAvailable(
                w,
                actuals[w.source.id()],
                retained[w.source.id()],
                fingerprints,
            )
        val associations =
            if (version == 1) null
            else
                GymAssociationArchiveJson.decode(child(root, "associations")).also {
                    GymAssociationArchiveJson.validate(
                        it,
                        retained,
                        actuals,
                        programmes,
                        links,
                        authored,
                    )
                }
        return Snapshot(
            workouts,
            preferences,
            executions,
            associations,
            Json.parseToJsonElement(root.toString()).jsonObject,
        )
    }

    /**
     * Retained original archive, not permission to restore it without the complete parent checks.
     */
    fun element(snapshot: Snapshot): JsonObject = snapshot.payload

    fun links(
        development: DevelopmentArchiveJson.Snapshot?
    ): Map<String, GymAssociationArchiveJson.Link> =
        Immutable.copyMap(
            development
                ?.records
                .orEmpty()
                .mapNotNull { r ->
                    val link =
                        r.evidence as? DevelopmentArchiveJson.Evidence.StrengthLink
                            ?: return@mapNotNull null
                    r.id to
                        GymAssociationArchiveJson.Link(
                            r.id,
                            link.gymVersion,
                            link.gymSession,
                            link.plannedDate.toString(),
                        )
                }
                .toMap()
        )

    private fun programmeDate(
        definition: GymWorkoutDefinition,
        source: GymExecution.SourceReference,
        legacy: Map<String, GymProgramme>,
        authored: Map<String, StrengthProgramme>,
    ): LocalDate {
        val key = "${source.id()}:v${source.revision()}"
        val modern = authored[key]
        return if (modern != null)
            GymWorkoutSourceValidator.authoredProgramme(definition, source, modern).date()
        else GymWorkoutSourceValidator.programme(definition, source, legacy[key]).date()
    }

    private fun checkParts(e: GymExecution) {
        val p = GymExecutionSnapshotJson.element(e)
        for (k in listOf("actualSets", "events")) for (r in
            p.getValue(k).jsonArray) LegacyGymRowJson.check(r)
        val header = JsonObject(p - setOf("actualSets", "events"))
        val reduced =
            if (e.completedSession() == null) header
            else
                JsonObject(
                    header +
                        ("completedSession" to
                            JsonObject(
                                header.getValue("completedSession").jsonObject - "exercises"
                            ))
                )
        LegacyGymRowJson.check(reduced)
    }
}
