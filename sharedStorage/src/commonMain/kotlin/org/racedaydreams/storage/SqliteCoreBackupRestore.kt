package org.racedaydreams.storage

import kotlinx.serialization.json.JsonObject
import org.racedaydreams.storage.StoredJson.required
import org.racedaydreams.storage.StoredJson.text

/**
 * Internal population step for a fully validated replacement transaction. Never clears tables,
 * activates training, imports health caches or recalculates recorded performances. NOT a public
 * restore entry point: the coordinator still owns all feature sections and raw reconciliation.
 */
internal class SqliteCoreBackupRestore(private val database: SqlDatabase) {
    fun populate(core: CoreBackupDataJson.Snapshot, retainOwnedRecordings: Boolean = false) =
        database.transaction {
            // Fail before the first write if the enclosing replacement has not prepared ALL tables.
            for (table in
                listOf(
                    "user_profile",
                    "race_goal",
                    "training_preferences",
                    "availability_day",
                    "onboarding_draft",
                    "activities",
                    "shoes",
                    "manual_results",
                    "best_efforts",
                    "preserved_best_efforts",
                    "activity_annotations",
                    "baseline_state",
                    "settings",
                    "planned_workouts",
                    "assessment_results",
                    "training_plans",
                    "plan_adjustments",
                    "import_checkpoints",
                )) require(
                (table == "activities" && retainOwnedRecordings) ||
                    database.query("SELECT 1 FROM $table LIMIT 1").isEmpty()
            ) {
                "Core restore requires empty prepared tables; existing data was retained."
            }
            if (retainOwnedRecordings) {
                require(
                    database
                        .query(
                            "SELECT 1 FROM activities WHERE provider NOT IN ('rdd','gpx') LIMIT 1"
                        )
                        .isEmpty()
                ) {
                    "Only protected owned recordings may remain during replacement."
                }
                SqliteOwnedActivityRestore(database).requirePreserved(core)
            }
            core.profile?.let { SqliteAthleteProfile(database).save(it) }
            core.draft?.let {
                database.execute(
                    "INSERT INTO onboarding_draft VALUES(1,?)",
                    listOf(SqlValue.Text(it)),
                )
            }
            core.shoes.forEach { SqliteShoes(database).saveShoe(it) }
            val activities =
                SqliteActivities(database) {
                    error("Restoring evidence must not invent an observation time")
                }
            for (a in core.activities) {
                if (retainOwnedRecordings && a.source().provider() in listOf("rdd", "gpx"))
                    SqliteOwnedActivityRestore(database).put(a)
                else
                    database.execute(
                        "INSERT INTO activities(id,provider,record_id,origin_package,start_millis,end_millis,payload) VALUES(?,?,?,?,?,?,?)",
                        listOf(
                            SqlValue.Text(a.id()),
                            SqlValue.Text(a.source().provider()),
                            SqlValue.Text(a.source().recordId()),
                            sqlText(a.source().originPackage()),
                            SqlValue.Integer(storedEpochMillis(a.start())),
                            SqlValue.Integer(storedEpochMillis(a.end())),
                            SqlValue.Text(
                                ActivityJson.encode(
                                    if (a.source().provider() in listOf("rdd", "gpx"))
                                        a.copy(samples = emptyList())
                                    else a
                                )
                            ),
                        ),
                    )
                // Original restore creates owned annotations without assigning today's default
                // shoe.
                activities.putAnnotation(
                    a.source().provider(),
                    a.source().recordId(),
                    a.notes(),
                    a.rpe(),
                    a.shoeId(),
                )
            }
            core.results.forEach { SqliteResults(database).putResult(it, false) }
            for (e in core.bestEfforts) database.execute(
                "INSERT INTO preserved_best_efforts(id,activity_id,distance_code,provider,record_id,origin_package,payload) VALUES(?,?,?,?,?,?,?)",
                listOf(
                    SqlValue.Text(e.result.id()),
                    sqlText(e.result.activityId()),
                    SqlValue.Text(e.result.distance().code()),
                    SqlValue.Text(e.source.provider()),
                    SqlValue.Text(e.source.recordId()),
                    sqlText(e.source.originPackage()),
                    SqlValue.Text(ResultJson.encode(e.result)),
                ),
            )
            core.annotations.forEach {
                activities.putAnnotation(it.provider, it.recordId, it.notes, it.rpe, it.shoeId)
            }
            core.baseline?.let {
                SqliteBaselines(database) { error("Keep the actual original baseline observation") }
                    .putBaseline(it, core.baselineAt)
            }
            core.settings.forEach { (key, value) ->
                SqliteSettings(database).putSetting(key, value)
            }
            core.planned.forEach {
                SqlitePlannedWorkouts(database).put(it.workout, it.revision, false)
            }
            core.assessments.forEach {
                SqliteBaselines(database) { error("Keep the actual assessment time") }
                    .saveAssessment(it)
            }
            core.trainingPlans.forEach { opaque("training_plans", it) }
            core.adjustments.forEach { opaque("plan_adjustments", it) }
        }

    private fun opaque(table: String, row: JsonObject) {
        val columns = if (table == "plan_adjustments") ",plan_id,user_decision" else ""
        database.execute(
            "INSERT INTO $table(id,payload$columns) VALUES(?,?${if (columns.isEmpty()) "" else ",?,?"})",
            listOf(SqlValue.Text(row.required("id")), SqlValue.Text(row.toString())) +
                if (columns.isEmpty()) emptyList()
                else listOf(sqlText(row.text("planId")), sqlText(row.text("userDecision"))),
        )
    }
}
