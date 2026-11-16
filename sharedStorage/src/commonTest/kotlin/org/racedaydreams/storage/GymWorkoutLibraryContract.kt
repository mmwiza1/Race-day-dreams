package org.racedaydreams.storage

import kotlin.test.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.GymExecutionRepository.Preference

abstract class GymWorkoutLibraryContract {
    protected abstract fun database(): SqlDatabase

    private val at = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")

    private fun definition(id: String = "workout", revision: Int = 1): GymWorkoutDefinition =
        GymWorkoutDefinition(
            id,
            revision,
            at,
            "Retained workout",
            listOf(
                GymWorkoutDefinition.Slot(
                    "slot",
                    ExerciseCatalog.definition("bench-press")!!.exercise()
                        .copy(name = "Historical retained name"),
                    null,
                    null,
                )
            ),
        )

    private fun seed(
        db: SqlDatabase,
        d: GymWorkoutDefinition = definition(),
        source: GymExecution.SourceReference? = null,
    ): FrozenGymWorkoutRecordJson.Record {
        val record = FrozenGymWorkoutRecordJson.Record(d, source, null)
        db.execute(
            "INSERT INTO strength_workout_versions(workout_id,revision,source_kind,source_id,payload) VALUES(?,?,?,?,?)",
            listOf(
                SqlValue.Text(d.id()),
                SqlValue.Integer(d.revision().toLong()),
                sqlText(source?.kind()?.name),
                sqlText(source?.id()),
                SqlValue.Text(FrozenGymWorkoutRecordJson.encode(record)),
            ),
        )
        return record
    }

    private fun <T> storage(block: (SqlDatabase, SqliteGymWorkouts) -> T): T {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "test-install")
            }
            return block(db, SqliteGymWorkouts(db))
        } finally {
            db.close()
        }
    }

    @Test
    fun exactRevisionsKeepFrozenNamesWithoutLoadingOrCreatingActuals(): Unit =
        storage { db, library ->
            assertNull(library.workout("workout", 1))
            val first = seed(db)
            val second = seed(db, definition(revision = 2).copy(title = "Athlete revision"))
            assertEquals(first, library.record("workout", 1))
            assertEquals(second.definition, library.workout("workout", 2))
            assertEquals(
                "Historical retained name",
                library.workout("workout", 1)!!.slots().single().exercise().name(),
            )
            assertTrue(db.query("SELECT id FROM strength_executions").isEmpty())
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
        }

    @Test
    fun explicitFavouritesChooseOneRevisionAndCanBeReversed(): Unit = storage { db, library ->
        val first = seed(db)
        val second = seed(db, definition(revision = 2))
        library.savePreference(Preference("workout", 1, true))
        assertEquals(listOf(first.definition), library.favourites(10))
        library.savePreference(Preference("workout", 2, true))
        assertEquals(listOf(second.definition), library.favourites(10))
        library.savePreference(Preference("workout", 2, false))
        assertTrue(library.favourites(10).isEmpty())
        assertEquals(listOf(Preference("workout", 2, false)), library.preferences())
        assertEquals(first.definition, library.workout("workout", 1))
    }

    @Test
    fun favouritePagesUseStableIdentityOrderingAndValidBounds(): Unit = storage { db, library ->
        for (id in listOf("c", "a", "b")) {
            seed(db, definition(id))
            library.savePreference(Preference(id, 1, true))
        }
        assertEquals(listOf("a", "b"), library.favourites(2).map { it.id() })
        assertEquals(listOf("c"), library.favourites(2, 2).map { it.id() })
        assertTrue(library.favourites(1, 10000).isEmpty())
        for ((limit, offset) in listOf(0 to 0, 51 to 0, 1 to -1, 1 to 10001)) assertFailsWith<
            IllegalArgumentException
        > {
            library.favourites(limit, offset)
        }
    }

    @Test
    fun preferencesCannotPointToMissingRevisionsAndShareOuterRollback(): Unit =
        storage { db, library ->
            seed(db)
            library.savePreference(Preference("workout", 1, true))
            assertFailsWith<IllegalArgumentException> {
                library.savePreference(Preference("workout", 2, true))
            }
            assertFails {
                db.transaction {
                    library.savePreference(Preference("workout", 1, false))
                    error("Outer failure")
                }
            }
            assertEquals(listOf(Preference("workout", 1, true)), library.preferences())
            db.execute(
                "CREATE TRIGGER fail_preference BEFORE UPDATE ON strength_workout_preferences BEGIN SELECT RAISE(ABORT,'test failure'); END"
            )
            assertFails { library.savePreference(Preference("workout", 1, false)) }
            assertEquals(listOf(Preference("workout", 1, true)), library.preferences())
        }

    @Test
    fun indexedWorkoutIdentityAndSourceColumnsFailClosedWithoutRepair(): Unit =
        storage { db, library ->
            seed(db)
            db.execute(
                "UPDATE strength_workout_versions SET payload=?",
                listOf(
                    SqlValue.Text(
                        FrozenGymWorkoutRecordJson.encode(
                            FrozenGymWorkoutRecordJson.Record(
                                definition(id = "different"),
                                null,
                                null,
                            )
                        )
                    )
                ),
            )
            assertFailsWith<IllegalArgumentException> { library.record("workout", 1) }
            val source =
                GymExecution.SourceReference(
                    GymExecution.SourceKind.PROGRAMME,
                    "plan",
                    1,
                    "session",
                )
            db.execute(
                "UPDATE strength_workout_versions SET payload=?",
                listOf(
                    SqlValue.Text(
                        FrozenGymWorkoutRecordJson.encode(
                            FrozenGymWorkoutRecordJson.Record(definition(), source, null)
                        )
                    )
                ),
            )
            assertFailsWith<IllegalArgumentException> { library.record("workout", 1) }
            assertFailsWith<IllegalArgumentException> {
                library.savePreference(Preference("workout", 1, true))
            }
            assertTrue(library.preferences().isEmpty())
            assertEquals(
                1L,
                db.query("SELECT COUNT(*) FROM strength_workout_versions")
                    .single()
                    .single()
                    .sqlLong(),
            )
        }

    @Test
    fun programmeSourcesAndUnusualIdentitiesRemainExactWithoutInventedValidation(): Unit =
        storage { db, library ->
            val id = "kept\u0000🏋️"
            val source =
                GymExecution.SourceReference(
                    GymExecution.SourceKind.PROGRAMME,
                    "retained-plan",
                    1,
                    "session",
                )
            val record = seed(db, definition(id), source)
            assertEquals(record, library.record(id, 1))
            library.savePreference(Preference(id, 1, true))
            assertEquals(listOf(record.definition), library.favourites(1))
            assertNull(library.workout(id, 0))
            for (invalid in listOf("", " ", "x".repeat(257))) assertFailsWith<
                IllegalArgumentException
            > {
                library.record(invalid, 1)
            }
            // This library returns provenance; only the execution repository can approve its
            // source.
            assertTrue(db.query("SELECT version_id FROM gym_plan_versions").isEmpty())
        }
}
