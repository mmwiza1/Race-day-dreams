package org.racedaydreams.storage

import org.racedaydreams.domain.strength.GymRestTimer

/** The existing advisory timer on the application database; never writes workout evidence. */
class SqliteGymTools(private val database: SqlDatabase) {
    fun preferences(): GymRestTimer.Preferences =
        database
            .query(
                "SELECT default_seconds,suggest_after_set,notifications,vibration,audio FROM gym_rest_preferences WHERE id=1"
            )
            .firstOrNull()
            ?.let {
                GymRestTimer.Preferences(
                    it[0].sqlLong().toInt(),
                    it[1].sqlLong() == 1L,
                    it[2].sqlLong() == 1L,
                    it[3].sqlLong() == 1L,
                    it[4].sqlLong() == 1L,
                )
            } ?: GymRestTimer.Preferences.defaults()

    fun preferences(value: GymRestTimer.Preferences) {
        database.execute(
            "INSERT OR REPLACE INTO gym_rest_preferences(id,default_seconds,suggest_after_set,notifications,vibration,audio) VALUES(1,?,?,?,?,?)",
            listOf(
                SqlValue.Integer(value.defaultSeconds().toLong()),
                flag(value.suggestAfterSet()),
                flag(value.notifications()),
                flag(value.vibration()),
                flag(value.audio()),
            ),
        )
    }

    fun timer(execution: String): GymRestTimer? =
        database
            .query(
                "SELECT trigger_id,phase,epoch,observed_millis,remaining_millis,duration_millis FROM gym_rest_timers WHERE execution_id=?",
                listOf(SqlValue.Text(execution)),
            )
            .firstOrNull()
            ?.let { readTimer(execution, it) }

    private fun readTimer(execution: String, row: List<SqlValue>): GymRestTimer =
        GymRestTimer(
            execution,
            row[0].sqlText(),
            GymRestTimer.Phase.valueOf(row[1].sqlText()),
            row[2].sqlText(),
            row[3].sqlLong(),
            row[4].sqlLong(),
            row[5].sqlLong(),
        )

    fun change(execution: String, command: (GymRestTimer?) -> GymRestTimer): GymRestTimer =
        database.transaction {
            require(unfinished(execution)) { "Rest belongs to an unfinished workout." }
            val value = command(timer(execution))
            require(value.executionId() == execution) { "Rest workout identity changed." }
            putTimer(value)
            value
        }

    /** Stable trigger acknowledgement is atomic and cannot complete a newer or finished workout. */
    fun completeIfDue(execution: String, trigger: String, now: GymRestTimer.Clock): Boolean =
        database.transaction {
            val saved = timer(execution)
            if (
                saved == null ||
                    saved.triggerId() != trigger ||
                    saved.phase() != GymRestTimer.Phase.RUNNING ||
                    !unfinished(execution)
            )
                return@transaction false
            val observed = saved.observe(now)
            if (observed.phase() != GymRestTimer.Phase.COMPLETE) return@transaction false
            putTimer(observed)
            true
        }

    fun snapshot(): GymToolsPayloadJson.Snapshot =
        database.transaction {
            GymToolsPayloadJson.Snapshot(
                preferences(),
                database
                    .query(
                        "SELECT execution_id,trigger_id,phase,epoch,observed_millis,remaining_millis,duration_millis FROM gym_rest_timers ORDER BY execution_id"
                    )
                    .map { readTimer(it[0].sqlText(), it.drop(1)) },
            )
        }

    private fun unfinished(execution: String): Boolean =
        database
            .query(
                "SELECT state FROM strength_executions WHERE id=?",
                listOf(SqlValue.Text(execution)),
            )
            .firstOrNull()
            ?.single()
            ?.sqlText() in setOf("ACTIVE", "PAUSED", "RECOVERY_REQUIRED")

    private fun putTimer(t: GymRestTimer) {
        database.execute(
            "INSERT OR REPLACE INTO gym_rest_timers(execution_id,trigger_id,phase,epoch,observed_millis,remaining_millis,duration_millis) VALUES(?,?,?,?,?,?,?)",
            listOf(
                SqlValue.Text(t.executionId()),
                SqlValue.Text(t.triggerId()),
                SqlValue.Text(t.phase().name),
                SqlValue.Text(t.epoch()),
                SqlValue.Integer(t.observedMillis()),
                SqlValue.Integer(t.remainingMillis()),
                SqlValue.Integer(t.durationMillis()),
            ),
        )
    }

    private fun flag(value: Boolean): SqlValue = SqlValue.Integer(if (value) 1L else 0L)
}
