package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

internal object GymEngineArchiveFixtures {
    val at = LegacyGymArchiveFixtures.at
    val bench = ExerciseCatalog.definition("bench-press")!!.exercise()

    fun tick(ms: Long) =
        GymExecution.Tick(
            "boot",
            1000000 + ms,
            Instants.ofEpochSecond(at.getEpochSecond() + ms / 1000, at.getNano().toLong()),
        )

    fun engine(
        id: String = "actual",
        p: GymWorkoutAdapter.PreparedWorkout? = null,
    ): GymExecutionEngine {
        val e =
            GymExecutionEngine.start(
                id,
                if (p == null) GymExecution.Origin.FREE
                else if (p.source().kind() == GymExecution.SourceKind.PROGRAMME)
                    GymExecution.Origin.PLANNED
                else GymExecution.Origin.REPEATED,
                p?.definition(),
                p?.source(),
                Offsets.ofTotalSeconds(0),
                tick(0),
            )
        if (p == null)
            e.addExercises(listOf(GymWorkoutDefinition.Slot("bench", bench, null, null)), tick(10))
        return e
    }

    fun finish(id: String = "actual", p: GymWorkoutAdapter.PreparedWorkout? = null): GymExecution {
        val e = engine(id, p)
        val slot = e.snapshot().slots().first().id()
        e.saveSet(slot, StrengthSession.SetEntry("one", 6, 50.0, 7.0, false), tick(1000))
        e.saveSet(slot, StrengthSession.SetEntry("two", 6, 50.0, 7.0, false), tick(2000))
        return e.finish(tick(3000), null, "Recorded / 🏋️", true)
    }

    fun entry(
        e: GymExecution,
        current: StrengthSession? = e.completedSession(),
        edit: String? = null,
    ) = buildJsonObject {
        put("revision", 9007199254740993L)
        put("snapshot", GymExecutionSnapshotJson.element(e))
        put("currentActual", current?.let { StrengthEvidenceJson.session(it) } ?: JsonNull)
        put("actualEditedAt", edit?.let { JsonPrimitive(it) } ?: JsonNull)
    }

    fun root(
        executions: List<JsonObject> = emptyList(),
        workouts: List<FrozenGymWorkoutRecordJson.Record> = emptyList(),
        v: Int = 2,
        associations: GymAssociationArchiveJson.Archive =
            GymAssociationArchiveJson.Archive(emptyList(), emptyMap()),
    ) = buildJsonObject {
        put("version", v)
        put("workouts", JsonArray(workouts.map { FrozenGymWorkoutRecordJson.element(it) }))
        put("preferences", JsonArray(emptyList()))
        put("executions", JsonArray(executions))
        if (v == 2) put("associations", GymAssociationArchiveJson.element(associations))
    }
}
