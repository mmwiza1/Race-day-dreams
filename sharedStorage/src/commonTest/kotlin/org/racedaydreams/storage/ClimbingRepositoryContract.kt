package org.racedaydreams.storage

import kotlin.test.*
import org.racedaydreams.domain.climbing.*
import org.racedaydreams.domain.time.*

/** Bouldering storage on each platform's real SQLite. */
abstract class ClimbingRepositoryContract {
    abstract fun database(): SqlDatabase

    private val at = Instants.ofEpochSecond(1_795_000_000)
    private val utc = Offsets.ofTotalSeconds(0)

    private fun storage(block: (SqliteClimbing, SqlDatabase) -> Unit) {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "climbing-test")
            }
            block(SqliteClimbing(db), db)
        } finally {
            db.close()
        }
    }

    private fun session(id: String, seconds: Long = 0, vararg problems: ClimbingProblem) =
        ClimbingSession(
            id,
            Instants.ofEpochSecond(at.getEpochSecond() + seconds),
            utc,
            3600,
            "",
            "",
            problems.toList(),
        )

    private val flash =
        ClimbingProblem("p", ClimbingGrade(GradeSystem.V_SCALE, "V4"), 1, ClimbOutcome.FLASH)

    @Test
    fun savingTheSameSessionTwiceIsHarmlessButAConflictingIdentityIsRejected() =
        storage { climbs, db ->
            val s = session("s", 0, flash)
            climbs.saveNew(s)
            climbs.saveNew(s)
            assertEquals(listOf(s), climbs.sessions())
            val before = db.query("SELECT payload FROM climbing_sessions")
            assertFailsWith<IllegalArgumentException> {
                climbs.saveNew(s.copy(notes = "different"))
            }
            assertEquals(before, db.query("SELECT payload FROM climbing_sessions"))
        }

    @Test
    fun sessionsAreNewestFirstAndCanBeCorrectedAndDeleted() = storage { climbs, _ ->
        climbs.saveNew(session("older", 0))
        climbs.saveNew(session("newer", 7200, flash))
        assertEquals(listOf("newer", "older"), climbs.sessions().map { it.id })
        val corrected =
            climbs.session("older")!!.copy(location = "Corrected gym", durationSeconds = 4500)
        climbs.update(corrected)
        assertEquals(corrected, climbs.session("older"))
        climbs.delete("newer")
        assertNull(climbs.session("newer"))
        assertFailsWith<IllegalArgumentException> { climbs.update(session("missing")) }
    }

    @Test
    fun anInterruptedDraftSurvivesAndFinishingSavesAndClearsItTogether() = storage { climbs, db ->
        assertNull(climbs.draft())
        val draft = ClimbingDraft("live", at, utc, null, "Wall", "", listOf(flash))
        climbs.saveDraft(draft)
        // A new connection (for example after the app was closed) sees the same unsaved draft.
        assertEquals(draft, SqliteClimbing(db).draft())
        climbs.saveDraft(draft.copy(notes = "updated"))
        assertEquals("updated", climbs.draft()!!.notes)
        climbs.finish(draft.toSession(3000))
        assertNull(climbs.draft())
        assertEquals(3000, climbs.session("live")!!.durationSeconds)
        // Repeating finish after a crash between taps cannot duplicate the session.
        climbs.finish(draft.toSession(3000))
        assertEquals(1, climbs.sessions().size)
    }

    @Test
    fun finishingADifferentSessionKeepsTheCurrentDraft() = storage { climbs, _ ->
        val draft = ClimbingDraft("current", at, utc, null, "", "", emptyList())
        climbs.saveDraft(draft)
        climbs.finish(session("manual-log"))
        assertEquals(draft, climbs.draft())
    }

    @Test
    fun gradePreferenceDefaultsToVScaleAndPersists() = storage { climbs, _ ->
        assertEquals(GradeSystem.V_SCALE, climbs.gradeSystem())
        climbs.gradeSystem(GradeSystem.FONTAINEBLEAU)
        assertEquals(GradeSystem.FONTAINEBLEAU, climbs.gradeSystem())
        climbs.gradeSystem(GradeSystem.V_SCALE)
        assertEquals(GradeSystem.V_SCALE, climbs.gradeSystem())
    }

    @Test
    fun climbingNeverWritesStrengthEvidence() = storage { climbs, db ->
        climbs.saveNew(session("s", 0, flash))
        for (table in
            listOf(
                "strength_sessions",
                "strength_executions",
                "strength_execution_sets",
            )) assertTrue(db.query("SELECT 1 FROM $table LIMIT 1").isEmpty(), table)
    }
}
