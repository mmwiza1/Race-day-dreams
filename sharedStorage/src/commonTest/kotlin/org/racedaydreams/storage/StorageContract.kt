package org.racedaydreams.storage

import kotlin.test.*
import org.racedaydreams.domain.strength.ExerciseExclusionRepository.*

/** Executed against real SQLite on both JVM and Apple, never an in-memory repository substitute. */
abstract class StorageContract {
    protected abstract fun database(): SqlDatabase

    protected fun opened(): SqlDatabase =
        database().also { ApplicationSchema.open(it) { choice() } }

    protected fun choice() =
        ApplicationSchema.NewInstallation("2026-11-15T10:00:00Z", "installation-test")

    @Test
    fun freshInstallationHasExactSchemaAndExplicitDefault() {
        val db = opened()
        try {
            assertEquals(listOf(SqlValue.Integer(19)), db.query("PRAGMA user_version").single())
            assertEquals(
                listOf(
                    SqlValue.Text("LAST_24_MONTHS"),
                    SqlValue.Text("NEW_INSTALL_DEFAULT"),
                    SqlValue.Text("installation-test"),
                ),
                db.query("SELECT policy,choice,choice_id FROM detail_retention_preferences")
                    .single(),
            )
            assertTrue(db.query("PRAGMA foreign_key_check").isEmpty())
            assertEquals(listOf(SqlValue.Text("ok")), db.query("PRAGMA integrity_check").single())
        } finally {
            db.close()
        }
    }

    @Test
    fun allHistoricalUpgradesKeepExistingSettingsAndProtectedRetention() {
        for (version in 1 until 19) {
            val db = database()
            try {
                db.execute("PRAGMA foreign_keys=ON")
                db.transaction {
                    for (v in 1..version) for (sql in SchemaStatements.versions[v - 1]) db.execute(
                        sql
                    )
                    db.execute("PRAGMA user_version=$version")
                    db.execute("INSERT INTO user_profile(id,performance_eligible) VALUES(1,0)")
                    if (version >= 2) SqliteSettings(db).putSetting("history", "keep-$version")
                }
                ApplicationSchema.open(db) {
                    error("Existing users must not receive new-install consent")
                }
                assertEquals(
                    SqlValue.Integer(0),
                    db.query("SELECT performance_eligible FROM user_profile").single().single(),
                )
                if (version >= 2)
                    assertEquals("keep-$version", SqliteSettings(db).getSetting("history"))
                assertEquals(
                    SqlValue.Text("EXISTING_UNREVIEWED"),
                    db.query("SELECT choice FROM detail_retention_preferences").single().single(),
                )
                assertTrue(db.query("PRAGMA foreign_key_check").isEmpty())
            } finally {
                db.close()
            }
        }
    }

    @Test
    fun futureAndUnversionedExistingDatabasesRemainUnchanged() {
        for (future in listOf(false, true)) {
            val db = database()
            try {
                db.execute("CREATE TABLE retained(value TEXT)")
                db.execute("INSERT INTO retained VALUES('original')")
                if (future) db.execute("PRAGMA user_version=20")
                assertFailsWith<IllegalArgumentException> {
                    ApplicationSchema.open(db) { choice() }
                }
                assertEquals(
                    SqlValue.Text("original"),
                    db.query("SELECT value FROM retained").single().single(),
                )
                assertEquals(
                    SqlValue.Integer(if (future) 20 else 0),
                    db.query("PRAGMA user_version").single().single(),
                )
            } finally {
                db.close()
            }
        }
    }

    @Test
    fun migrationFailureRollsBackEveryStepAndVersion() {
        val db = database()
        try {
            for (sql in SchemaStatements.versions[0]) db.execute(sql)
            db.execute("PRAGMA user_version=1")
            db.execute("CREATE TABLE strength_exclusions(retain TEXT)")
            assertFails { ApplicationSchema.open(db) { choice() } }
            assertEquals(SqlValue.Integer(1), db.query("PRAGMA user_version").single().single())
            assertTrue(db.query("SELECT name FROM sqlite_master WHERE name='activities'").isEmpty())
            assertEquals(
                SqlValue.Text("retain"),
                db.query("PRAGMA table_info(strength_exclusions)").single()[1],
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun failedFreshConsentLeavesNoPartialDatabase() {
        val db = database()
        try {
            assertFails { ApplicationSchema.open(db) { error("Clock or identity unavailable") } }
            assertEquals(SqlValue.Integer(0), db.query("PRAGMA user_version").single().single())
            assertTrue(
                db.query("SELECT name FROM sqlite_master WHERE name NOT LIKE 'sqlite_%'").isEmpty()
            )
            ApplicationSchema.open(db) { choice() }
        } finally {
            db.close()
        }
    }

    @Test
    fun nullTextLongRealAndBinaryRemainDistinct() {
        val db = opened()
        try {
            db.execute("CREATE TABLE values_test(n,t,i,r,b,e)")
            val values =
                listOf(
                    SqlValue.Null,
                    SqlValue.Text("café 🏃\u0000after ' quote"),
                    SqlValue.Integer(Long.MAX_VALUE),
                    SqlValue.Real(0.125),
                    SqlValue.Blob(listOf(0, -1, 127)),
                    SqlValue.Blob(emptyList()),
                )
            db.execute("INSERT INTO values_test VALUES(?,?,?,?,?,?)", values)
            assertEquals(values, db.query("SELECT * FROM values_test").single())
            db.execute("DELETE FROM values_test")
            val empty =
                listOf(
                    SqlValue.Text(""),
                    SqlValue.Null,
                    SqlValue.Integer(Long.MIN_VALUE),
                    SqlValue.Real(0.5),
                    SqlValue.Blob(emptyList()),
                    SqlValue.Null,
                )
            db.execute("INSERT INTO values_test VALUES(?,?,?,?,?,?)", empty)
            assertEquals(empty, db.query("SELECT * FROM values_test").single())
        } finally {
            db.close()
        }
    }

    @Test
    fun repositoriesParticipateInSameNestedRollbackAndKeepHistory() {
        val db = opened()
        try {
            val settings = SqliteSettings(db)
            val exclusions = SqliteExerciseExclusions(db)
            val excluded =
                Exclusion("bench", Reason.PREFERENCE, StoredInstant.parse("2026-11-15T09:00:00Z"))
            db.transaction {
                settings.putSetting("outer", "retained")
                assertFails {
                    db.transaction {
                        exclusions.exclude(excluded)
                        settings.putSetting("inner", "rollback")
                        error("stop")
                    }
                }
            }
            assertEquals("retained", settings.getSetting("outer"))
            assertNull(settings.getSetting("inner"))
            assertTrue(exclusions.exclusions().isEmpty())
            exclusions.exclude(excluded)
            assertEquals(listOf(excluded), exclusions.exclusions())
            db.execute("INSERT INTO settings VALUES('history','actual set evidence')")
            exclusions.restoreExercise("bench")
            exclusions.restoreExercise("bench")
            assertTrue(exclusions.exclusions().isEmpty())
            assertEquals("actual set evidence", settings.getSetting("history"))
            assertFails {
                db.transaction {
                    settings.putSetting("outer", "changed")
                    error("rollback")
                }
            }
            assertEquals("retained", settings.getSetting("outer"))
        } finally {
            db.close()
        }
    }

    @Test
    fun foreignKeysAndConstraintsRemainEnabled() {
        val db = opened()
        try {
            assertFails { db.execute("INSERT INTO availability_day VALUES(99,'RUN',1)") }
            assertFails { db.execute("INSERT INTO user_profile VALUES(1,3)") }
            assertFails {
                db.execute("INSERT INTO settings VALUES(?,?)", listOf(SqlValue.Text("missing")))
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun reopenDoesNotRequestOrRewriteRetentionChoice() {
        val db = opened()
        try {
            db.execute(
                "UPDATE detail_retention_preferences SET policy='KEEP_FOREVER',choice='EXISTING_UNREVIEWED',chosen_at=NULL,choice_id=NULL"
            )
            ApplicationSchema.open(db) { error("Must not ask for a fresh install identity") }
            assertEquals(
                SqlValue.Text("EXISTING_UNREVIEWED"),
                db.query("SELECT choice FROM detail_retention_preferences").single().single(),
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun retentionReaderPreservesFreshAndExistingChoice() {
        val db = opened()
        try {
            val repository = SqliteRetentionPreferences(db)
            assertEquals(
                org.racedaydreams.domain.storage.DetailRetention.Choice.NEW_INSTALL_DEFAULT,
                repository.load().choice(),
            )
            assertEquals(choice().chosenAt, repository.load().chosenAt().toString())
            db.execute(
                "UPDATE detail_retention_preferences SET policy='KEEP_FOREVER',choice='EXISTING_UNREVIEWED',chosen_at=NULL,choice_id=NULL"
            )
            val protected = repository.load()
            assertEquals(
                org.racedaydreams.domain.storage.DetailRetention.Preferences.existing(),
                protected,
            )
            assertNull(protected.chosenAt())
            assertNull(protected.choiceId())
        } finally {
            db.close()
        }
    }

    @Test
    fun settingKeysAndNormalisedProviderMatchExistingAndroidContract() {
        val db = opened()
        try {
            val settings = SqliteSettings(db)
            for (key in listOf("", " \t\n", "x".repeat(257))) assertFailsWith<
                IllegalArgumentException
            > {
                settings.putSetting(key, "invalid")
            }
            settings.putSetting("provider.preference", "SAMSUNG_HEALTH")
            assertEquals(
                org.racedaydreams.domain.activity.ActivitySources.normalizeLegacyPreference(
                    "SAMSUNG_HEALTH"
                ),
                settings.getSetting("provider.preference"),
            )
            settings.putSetting("exact", "")
            assertEquals("", settings.getSetting("exact"))
            assertNull(settings.getSetting("unknown"))
        } finally {
            db.close()
        }
    }
}
