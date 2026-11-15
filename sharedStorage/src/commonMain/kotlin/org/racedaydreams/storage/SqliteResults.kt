package org.racedaydreams.storage

import kotlin.math.abs
import org.racedaydreams.domain.activity.*

/** Same three retained-result tables, one transaction boundary, no duplicate PB engine. */
class SqliteResults(private val database: SqlDatabase) : ResultRepository {
    override fun results(): List<RecordedResult> =
        database.transaction {
            val seen = mutableSetOf<String>()
            buildList {
                for (table in TABLES) for (row in
                    database.query("SELECT id,payload FROM $table ORDER BY id")) {
                    if (seen.add(row[0].sqlText())) add(ResultJson.decode(row[1].sqlText()))
                }
            }
        }

    override fun saveResult(result: RecordedResult) {
        require(result.source() != ResultSource.ROLLING) {
            "Rolling results must be calculated from retained activity samples."
        }
        database.transaction {
            val catalog = mutableMapOf<String, Double>()
            for (distance in PerformanceDistance.CORE) register(catalog, distance)
            for (table in TABLES) for (row in database.query("SELECT id,payload FROM $table")) {
                if (row[0].sqlText() == result.id())
                    require(table == "manual_results") {
                        "A manual result cannot replace a derived effort identity."
                    }
                else register(catalog, ResultJson.decode(row[1].sqlText()).distance())
            }
            register(catalog, result.distance())
            putResult(result, false)
        }
    }

    internal fun putResult(result: RecordedResult, rolling: Boolean) {
        sqlRequired(result.id(), "result ID")
        sqlRequired(result.distance().label(), "distance label")
        val values =
            listOf(
                SqlValue.Text(result.id()),
                sqlText(result.activityId()),
                SqlValue.Text(result.distance().code()),
                SqlValue.Text(ResultJson.encode(result)),
            )
        if (rolling)
            database.execute(
                "INSERT OR REPLACE INTO best_efforts(id,activity_id,distance_code,payload) VALUES(?,?,?,?)",
                values,
            )
        else
            database.execute(
                "INSERT OR REPLACE INTO manual_results(id,activity_id,distance_code,payload,distance_metres,result_date) VALUES(?,?,?,?,?,?)",
                values +
                    listOf(
                        SqlValue.Real(result.distance().metres()),
                        SqlValue.Text(result.date().toString()),
                    ),
            )
    }

    override fun deleteResult(id: String) {
        database.execute(
            "DELETE FROM manual_results WHERE id=?",
            listOf(SqlValue.Text(sqlRequired(id, "result ID"))),
        )
    }

    private fun register(catalog: MutableMap<String, Double>, distance: PerformanceDistance) {
        val previous = catalog.put(distance.code(), distance.metres())
        require(previous == null || abs(previous - distance.metres()) <= 0.000001) {
            "One distance code cannot refer to different distances."
        }
    }

    private companion object {
        val TABLES = listOf("manual_results", "best_efforts", "preserved_best_efforts")
    }
}
