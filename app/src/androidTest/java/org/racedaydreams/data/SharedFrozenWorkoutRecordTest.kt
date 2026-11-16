package org.racedaydreams.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.*

@RunWith(AndroidJUnit4::class)
class SharedFrozenWorkoutRecordTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val at = Instant.parse("2026-11-15T09:00:00.123456789Z")
    private lateinit var db: android.database.sqlite.SQLiteDatabase
    private lateinit var coaching: CoachingStore
    private lateinit var execution: SqliteGymExecutionRepository

    @Before
    fun prepare() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
        db = ProfileDatabase.get(context).writableDatabase
        coaching = CoachingStore(context)
        execution = SqliteGymExecutionRepository(context)
    }

    @After
    fun cleanup() {
        ProfileDatabase.get(context).close()
        context.deleteDatabase(ProfileDatabase.NAME)
    }

    private fun actual() =
        StrengthSession(
            "actual",
            at,
            ZoneOffset.UTC,
            Duration.ofMinutes(30),
            null,
            "Retained actual note",
            listOf(
                StrengthSession.ExerciseSets(
                    ExerciseCatalog.definition("bench-press")!!.exercise(),
                    listOf(
                        StrengthSession.SetEntry("working", 6, 82.0, 7.0, false, null, null),
                        StrengthSession.SetEntry("warmup", 8, 40.0, 5.0, true, null, null),
                    ),
                )
            ),
        )

    private fun row(): FrozenGymWorkoutRecordJson.Record =
        db.rawQuery(
                "SELECT payload FROM strength_workout_versions WHERE workout_id='repeat' AND revision=1",
                null,
            )
            .use {
                assertTrue(it.moveToFirst())
                FrozenGymWorkoutRecordJson.decode(it.getString(0))
            }

    private fun rewriteAndValidate(record: FrozenGymWorkoutRecordJson.Record) {
        val v = android.content.ContentValues()
        v.put("payload", FrozenGymWorkoutRecordJson.encode(record))
        db.update("strength_workout_versions", v, "workout_id=? AND revision=1", arrayOf("repeat"))
        execution.saveWorkout(record.definition, record.source)
        assertEquals(record, row())
    }

    private fun legacyFingerprint(actual: StrengthSession): String =
        MessageDigest.getInstance("SHA-256")
            .digest(CoachingJson.strength(actual).toString().toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }

    @Test
    fun originalActualRepeatReceiptSurvivesSharedEncodingAndOriginalSourceValidation() {
        val actual = actual()
        coaching.saveStrength(actual)
        val repeat = GymWorkoutAdapter.repeatActual("repeat", 1, at, "Repeat my workout", actual)
        execution.saveWorkout(repeat.definition(), repeat.source())
        val record = row()
        assertEquals(repeat.definition(), record.definition)
        assertEquals(repeat.source(), record.source)
        assertEquals(legacyFingerprint(actual), record.sourceSnapshot!!.originalSourceFingerprint)
        assertEquals(legacyFingerprint(actual), record.sourceSnapshot!!.currentSourceFingerprint)
        assertEquals(1, record.definition.slots().size)
        assertNull(record.definition.slots().single().target())
        rewriteAndValidate(record)
        assertEquals(actual, coaching.strength().single())
        // Preserve actual measurements independently of their retained byte-level fingerprint.
        assertEquals(
            actual,
            StrengthEvidenceJson.decodeSession(StrengthEvidenceJson.encodeSession(actual)),
        )
    }

    @Test
    fun explicitActualEditsAndDeletionRetainTheirOriginalReceiptThroughSharedEncoding() {
        val actual = actual()
        coaching.saveStrength(actual)
        val repeat = GymWorkoutAdapter.repeatActual("repeat", 1, at, "Repeat", actual)
        execution.saveWorkout(repeat.definition(), repeat.source())
        val original = row()
        val changed = actual.copy(notes = "Athlete corrected the historical note")
        coaching.saveStrength(changed)
        val edited = row()
        assertEquals(original.definition, edited.definition)
        assertEquals(
            original.sourceSnapshot!!.originalSourceFingerprint,
            edited.sourceSnapshot!!.originalSourceFingerprint,
        )
        assertEquals(legacyFingerprint(changed), edited.sourceSnapshot!!.currentSourceFingerprint)
        assertNotNull(edited.sourceSnapshot!!.sourceEditedAt)
        assertNull(edited.sourceSnapshot!!.sourceDeletedAt)
        rewriteAndValidate(edited)
        coaching.deleteStrength(actual.id())
        val deleted = row()
        assertEquals(original.definition, deleted.definition)
        assertEquals(
            original.sourceSnapshot!!.originalSourceFingerprint,
            deleted.sourceSnapshot!!.originalSourceFingerprint,
        )
        assertNull(deleted.sourceSnapshot!!.currentSourceFingerprint)
        assertNotNull(deleted.sourceSnapshot!!.sourceDeletedAt)
        rewriteAndValidate(deleted)
        assertTrue(coaching.strength().isEmpty())
    }

    @Test
    fun savedLibraryAndFavouritePreferencesShareTheOriginalRowsAndRevisions() {
        val bridge = AndroidSqlTestConnection(db)
        val library = SqliteGymWorkouts(bridge)
        val first =
            GymWorkoutDefinition(
                "saved",
                1,
                at,
                "My workout",
                listOf(
                    GymWorkoutDefinition.Slot(
                        "slot",
                        ExerciseCatalog.definition("bench-press")!!.exercise(),
                        null,
                        null,
                    )
                ),
            )
        val second = first.copy(revision = 2, title = "Athlete revision")
        execution.saveWorkout(first, null)
        execution.saveWorkout(second, null)
        execution.savePreference(GymExecutionRepository.Preference("saved", 1, true))
        assertEquals(execution.workout("saved", 1), library.workout("saved", 1))
        assertEquals(execution.favouriteWorkouts(10), library.favourites(10))
        library.savePreference(GymExecutionRepository.Preference("saved", 2, true))
        assertEquals(execution.preferences(), library.preferences())
        assertEquals(listOf(second), execution.favouriteWorkouts(10))
        library.savePreference(GymExecutionRepository.Preference("saved", 2, false))
        assertTrue(execution.favouriteWorkouts(10).isEmpty())
        assertEquals(first, execution.workout("saved", 1))
        assertThrows(IllegalArgumentException::class.java) {
            library.savePreference(GymExecutionRepository.Preference("saved", 3, true))
        }
        assertTrue(coaching.strength().isEmpty())
    }

    @Test
    fun libraryFavouriteChangesParticipateInTheExistingOuterTransaction() {
        val library = SqliteGymWorkouts(AndroidSqlTestConnection(db))
        val definition =
            GymWorkoutDefinition(
                "saved",
                1,
                at,
                "My workout",
                listOf(
                    GymWorkoutDefinition.Slot(
                        "slot",
                        ExerciseCatalog.definition("bench-press")!!.exercise(),
                        null,
                        null,
                    )
                ),
            )
        execution.saveWorkout(definition, null)
        execution.savePreference(GymExecutionRepository.Preference("saved", 1, true))
        db.beginTransaction()
        try {
            library.savePreference(GymExecutionRepository.Preference("saved", 1, false))
        } finally {
            db.endTransaction()
        }
        assertEquals(listOf(definition), execution.favouriteWorkouts(10))
        assertEquals(execution.preferences(), library.preferences())
    }
}
