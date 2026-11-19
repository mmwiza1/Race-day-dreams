package org.racedaydreams.storage

import org.racedaydreams.domain.climbing.ClimbingDraft
import org.racedaydreams.domain.climbing.ClimbingSession
import org.racedaydreams.domain.climbing.GradeSystem

/**
 * Local bouldering storage (schema 20). Sessions are independent of gym programmes and never touch
 * strength tables. The single unsaved draft survives app termination so an interrupted session can
 * be resumed, reviewed or discarded explicitly.
 */
class SqliteClimbing(private val database: SqlDatabase) {
    /** All saved sessions, newest first. */
    fun sessions(): List<ClimbingSession> =
        database
            .query("SELECT payload FROM climbing_sessions ORDER BY started_seconds DESC,id")
            .map { ClimbingArchiveJson.session(GymStoredJson.read(it[0].sqlText())) }

    fun session(id: String): ClimbingSession? =
        database
            .query("SELECT payload FROM climbing_sessions WHERE id=?", listOf(SqlValue.Text(id)))
            .singleOrNull()
            ?.let { ClimbingArchiveJson.session(GymStoredJson.read(it[0].sqlText())) }

    /**
     * Saves a new session. Saving the same session again (for example after a repeated tap) is a
     * no-op; a different session with an existing identity is rejected rather than overwritten.
     */
    fun saveNew(session: ClimbingSession) =
        database.transaction {
            val existing = session(session.id)
            if (existing != null) {
                require(existing == session) {
                    "A different session is already saved with this identity; nothing was changed."
                }
                return@transaction
            }
            database.execute(
                "INSERT INTO climbing_sessions(id,started_seconds,payload) VALUES(?,?,?)",
                row(session),
            )
        }

    /** Replaces the corrected session; the session must already exist. */
    fun update(session: ClimbingSession) =
        database.transaction {
            require(session(session.id) != null) { "This session is no longer saved." }
            database.execute(
                "UPDATE climbing_sessions SET started_seconds=?,payload=? WHERE id=?",
                listOf(
                    SqlValue.Integer(session.startedAt.getEpochSecond()),
                    SqlValue.Text(GymStoredJson.write(ClimbingArchiveJson.session(session))),
                    SqlValue.Text(session.id),
                ),
            )
        }

    fun delete(id: String) =
        database.execute("DELETE FROM climbing_sessions WHERE id=?", listOf(SqlValue.Text(id)))

    fun draft(): ClimbingDraft? =
        database
            .query("SELECT payload FROM climbing_session_draft WHERE id=1")
            .singleOrNull()
            ?.let { ClimbingArchiveJson.draft(GymStoredJson.read(it[0].sqlText())) }

    fun saveDraft(draft: ClimbingDraft) =
        database.execute(
            // INSERT OR REPLACE: Android API 28 ships SQLite 3.22, before UPSERT support.
            "INSERT OR REPLACE INTO climbing_session_draft(id,payload) VALUES(1,?)",
            listOf(SqlValue.Text(GymStoredJson.write(ClimbingArchiveJson.draft(draft)))),
        )

    fun clearDraft() = database.execute("DELETE FROM climbing_session_draft")

    /**
     * Saves the reviewed session and removes its draft in one transaction, so a crash can never
     * leave both or neither.
     */
    fun finish(session: ClimbingSession) =
        database.transaction {
            saveNew(session)
            if (draft()?.id == session.id) clearDraft()
        }

    fun gradeSystem(): GradeSystem =
        database
            .query("SELECT grade_system FROM climbing_preferences WHERE id=1")
            .singleOrNull()
            ?.let { GradeSystem.valueOf(it[0].sqlText()) } ?: GradeSystem.V_SCALE

    fun gradeSystem(system: GradeSystem) =
        database.execute(
            "INSERT OR REPLACE INTO climbing_preferences(id,grade_system) VALUES(1,?)",
            listOf(SqlValue.Text(system.name)),
        )

    fun hasDraft(): Boolean =
        database.query("SELECT 1 FROM climbing_session_draft LIMIT 1").isNotEmpty()

    fun snapshot(): ClimbingArchiveJson.Snapshot =
        ClimbingArchiveJson.Snapshot(sessions(), gradeSystem())

    /**
     * Whole-backup restore step only: replaces saved sessions and the preference with an already
     * validated snapshot inside the caller's transaction. The coordinator must first refuse to
     * restore while an unsaved draft exists; the draft table is never touched here.
     */
    fun replaceFromBackup(snapshot: ClimbingArchiveJson.Snapshot) =
        database.transaction {
            for (table in RESTORED_TABLES) database.execute("DELETE FROM $table")
            snapshot.sessions.forEach {
                database.execute(
                    "INSERT INTO climbing_sessions(id,started_seconds,payload) VALUES(?,?,?)",
                    row(it),
                )
            }
            gradeSystem(snapshot.gradeSystem)
        }

    private fun row(session: ClimbingSession) =
        listOf(
            SqlValue.Text(session.id),
            SqlValue.Integer(session.startedAt.getEpochSecond()),
            SqlValue.Text(GymStoredJson.write(ClimbingArchiveJson.session(session))),
        )

    companion object {
        /** Tables the whole-backup restore clears before populating the climbing section. */
        val RESTORED_TABLES = listOf("climbing_sessions", "climbing_preferences")
    }
}
