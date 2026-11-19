package org.racedaydreams.storage

import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.time.Instant

/** Same original baseline/assessment rows and clock semantics; no answers become recorded runs. */
class SqliteBaselines(private val database: SqlDatabase, private val now: () -> Instant) :
    BaselineRepository {
    override fun savedManualBaseline(): ManualBaselineInput =
        database
            .query("SELECT manual_json FROM baseline_state WHERE id=1")
            .firstOrNull()
            ?.single()
            ?.sqlText()
            ?.let { BaselineJson.decode(it) } ?: ManualBaselineInput.empty()

    override fun manualBaselineRecordedAt(): Instant? =
        database
            .query("SELECT manual_recorded_at FROM baseline_state WHERE id=1")
            .firstOrNull()
            ?.single()
            ?.nullableSqlText()
            ?.let { StoredInstant.parse(it) }

    override fun saveManualBaseline(input: ManualBaselineInput) {
        putBaseline(input, now())
    }

    internal fun putBaseline(input: ManualBaselineInput, recordedAt: Instant?) {
        database.execute(
            "INSERT OR REPLACE INTO baseline_state(id,manual_json,manual_recorded_at,reviewed) VALUES(1,?,?,1)",
            listOf(SqlValue.Text(BaselineJson.encode(input)), sqlText(recordedAt?.toString())),
        )
    }

    override fun assessments(): List<AssessmentResult> =
        database.query("SELECT payload FROM assessment_results ORDER BY id").map {
            BaselineJson.decodeAssessment(it.single().sqlText())
        }

    override fun saveAssessment(assessment: AssessmentResult) {
        database.execute(
            "INSERT OR REPLACE INTO assessment_results(id,activity_id,payload) VALUES(?,?,?)",
            listOf(
                SqlValue.Text(sqlRequired(assessment.id(), "id")),
                sqlText(assessment.activityId()),
                SqlValue.Text(BaselineJson.encodeAssessment(assessment)),
            ),
        )
    }

    override fun saveAssessment(activity: ActivityRecord, assessment: AssessmentResult) {
        require(isUserActivity(activity) && activity.id() == assessment.activityId()) {
            "Assessment must link its recorded manual activity."
        }
        database.transaction {
            SqliteActivities(database, now).putActivity(activity, true)
            saveAssessment(assessment)
        }
    }
}
