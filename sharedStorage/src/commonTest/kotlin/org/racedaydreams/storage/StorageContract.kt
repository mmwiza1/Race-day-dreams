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
    fun strengthChoiceArchiveCapturesReversiblePreferencesWithoutRewritingActualsOrAcknowledgements() {
        val db = opened()
        try {
            val choices = SqliteStrengthChoices(db)
            choices.save(SqliteStrengthChoices.Choice("retained", true, false))
            choices.acknowledge("historic-record")
            val exclusion =
                Exclusion(
                    "retained",
                    Reason.UNCOMFORTABLE,
                    StoredInstant.parse("2026-11-16T10:00:00Z"),
                )
            SqliteExerciseExclusions(db).exclude(exclusion)
            val changes = db.query("SELECT total_changes()")
            val snapshot = choices.archiveSnapshot()
            assertEquals(choices.choices(), snapshot.choices)
            assertEquals(choices.acknowledged(), snapshot.acknowledged.toSet())
            assertEquals(listOf(exclusion), snapshot.exclusions)
            assertEquals(changes, db.query("SELECT total_changes()"))
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun strengthChoiceArchiveRejectsOversizedIdentityBeforeLoadingRows() {
        val db = opened()
        try {
            db.execute(
                "INSERT INTO pb_acknowledgements(id) VALUES(CAST(zeroblob(33554433) AS TEXT))"
            )
            val changes = db.query("SELECT total_changes()")
            val guarded =
                object : SqlDatabase by db {
                    override fun query(
                        sql: String,
                        arguments: List<SqlValue>,
                    ): List<List<SqlValue>> {
                        check(!sql.contains("ORDER BY")) {
                            "Preflight must run before loading choices."
                        }
                        return db.query(sql, arguments)
                    }
                }
            val error =
                assertFailsWith<IllegalArgumentException> {
                    SqliteStrengthChoices(guarded).archiveSnapshot()
                }
            assertTrue(error.message!!.contains("whole-backup budget"))
            assertEquals(changes, db.query("SELECT total_changes()"))
            assertEquals(
                1L,
                db.query("SELECT COUNT(*) FROM pb_acknowledgements").single().single().sqlLong(),
            )
        } finally {
            db.close()
        }
    }

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

    private fun athlete(race: Boolean = false): org.racedaydreams.domain.UserProfile {
        return org.racedaydreams.domain.UserProfile(
            org.racedaydreams.domain.RaceGoal(
                if (race) org.racedaydreams.domain.RaceDistance.HALF_MARATHON
                else org.racedaydreams.domain.RaceDistance.GENERAL_FITNESS,
                if (race) "Local half" else "",
                if (race) org.racedaydreams.domain.time.CalendarDates.of(2027, 4, 25) else null,
                if (race) org.racedaydreams.domain.GoalMode.TARGET_TIME
                else org.racedaydreams.domain.GoalMode.FINISH,
                if (race) org.racedaydreams.domain.time.Durations.ofSeconds(7200) else null,
            ),
            org.racedaydreams.domain.TrainingPreferences(
                org.racedaydreams.domain.TrainingStrategy.BALANCED,
                setOf(
                    org.racedaydreams.domain.time.DayOfWeek.MONDAY,
                    org.racedaydreams.domain.time.DayOfWeek.FRIDAY,
                ),
                2,
                org.racedaydreams.domain.time.DayOfWeek.FRIDAY,
                setOf(org.racedaydreams.domain.time.DayOfWeek.TUESDAY),
                org.racedaydreams.domain.ScheduleType.FLEXIBLE,
                org.racedaydreams.domain.UnitSystem.METRIC,
            ),
            false,
        )
    }

    @Test
    fun profileRoundTripPreservesGoalOptionalValuesDaysAndHistory() {
        val db = opened()
        try {
            val profiles = SqliteAthleteProfile(db)
            assertNull(profiles.load())
            db.execute(
                "INSERT INTO activities VALUES('retained','rdd','source',NULL,10,20,'original history')"
            )
            db.execute("INSERT INTO onboarding_draft VALUES(1,'draft')")
            for (race in listOf(false, true, false)) {
                val expected = athlete(race)
                profiles.save(expected)
                assertEquals(expected, profiles.load())
                assertEquals(
                    SqlValue.Text("original history"),
                    db.query("SELECT payload FROM activities WHERE id='retained'").single().single(),
                )
                assertTrue(db.query("SELECT * FROM onboarding_draft").isEmpty())
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun profileReplacementFailureRollsBackDeletedProfileAndDraft() {
        val db = opened()
        try {
            val profiles = SqliteAthleteProfile(db)
            val original = athlete(false)
            profiles.save(original)
            db.execute("INSERT INTO onboarding_draft VALUES(1,'retain draft')")
            db.execute(
                "CREATE TRIGGER fail_goal BEFORE INSERT ON race_goal BEGIN SELECT RAISE(ABORT,'test failure'); END"
            )
            assertFails { profiles.save(athlete(true)) }
            assertEquals(original, profiles.load())
            assertEquals(
                SqlValue.Text("retain draft"),
                db.query("SELECT payload FROM onboarding_draft").single().single(),
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun incompleteProfileIsAnErrorRatherThanNewUserState() {
        val db = opened()
        try {
            db.execute("INSERT INTO user_profile VALUES(1,0)")
            assertFails { SqliteAthleteProfile(db).load() }
            assertEquals(
                SqlValue.Integer(1),
                db.query("SELECT COUNT(*) FROM user_profile").single().single(),
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun primaryFocusUsesLegacyFallbackOnlyWhenModernChoiceAbsent() {
        val db = opened()
        try {
            val settings = SqliteSettings(db)
            val focus = SqlitePrimaryFocus(settings)
            assertNull(focus.load())
            settings.putSetting("starting.path", "hybrid")
            assertEquals(org.racedaydreams.domain.settings.PrimaryFocus.HYBRID, focus.load())
            focus.save(org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH)
            assertEquals(org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH, focus.load())
            assertNull(SqliteAthleteProfile(db).load())
            settings.putSetting("primary.focus", "future-choice")
            assertNull(focus.load())
        } finally {
            db.close()
        }
    }

    @Test
    fun heightEvidenceKeepsNanosecondOrderingSourceAndUnknownFields() {
        val db = opened()
        try {
            val repository = SqliteBodyHeights(db)
            val earlier =
                org.racedaydreams.domain.body.BodyHeight(
                    "manual",
                    1.78,
                    StoredInstant.parse("2026-11-15T09:00:00Z"),
                    null,
                    "manual",
                    "manual",
                    null,
                    null,
                )
            val later =
                org.racedaydreams.domain.body.BodyHeight(
                    "imported",
                    1.79,
                    StoredInstant.parse("2026-11-15T09:00:00.000000001Z"),
                    org.racedaydreams.domain.time.Offsets.ofTotalSeconds(3600),
                    "healthkit",
                    "source",
                    "watch",
                    StoredInstant.parse("2026-11-15T10:00:00Z"),
                )
            repository.saveHeight(earlier)
            repository.saveHeight(later)
            assertEquals(listOf(later, earlier), repository.heights(500, 0))
            assertEquals(listOf(earlier), repository.heights(1, 1))
            assertFailsWith<IllegalArgumentException> { repository.heights(501, 0) }
            assertFailsWith<IllegalArgumentException> { repository.heights(1, -1) }
            assertFailsWith<IllegalArgumentException> {
                repository.saveHeight(later.copy(id = "conflicting"))
            }
            assertEquals(listOf(later, earlier), repository.heights(500, 0))
            val corrected = earlier.copy(metres = 1.80)
            repository.saveHeight(corrected)
            assertEquals(listOf(later, corrected), repository.heights(500, 0))
            repository.deleteHeight("manual")
            assertEquals(listOf(later), repository.heights(500, 0))
        } finally {
            db.close()
        }
    }

    @Test
    fun optionalBodyGuidancePreferencesKeepSelectedBasisAndCanClearIt() {
        val db = opened()
        try {
            val repository = SqliteBodyHeights(db)
            val defaults =
                org.racedaydreams.domain.body.BodyMeasurementsRepository.Preferences.defaults()
            assertEquals(defaults, repository.preferences())
            val selected =
                org.racedaydreams.domain.body.BodyMeasurementsRepository.Preferences(
                    true,
                    org.racedaydreams.domain.body.HeightUnit.INCHES,
                    org.racedaydreams.domain.body.ProteinGuidance.Basis(
                        "recorded-weight",
                        StoredInstant.parse("2026-11-15T09:00:00.123456789Z"),
                        87.0,
                    ),
                )
            repository.preferences(selected)
            assertEquals(selected, repository.preferences())
            repository.preferences(defaults)
            assertEquals(defaults, repository.preferences())
        } finally {
            db.close()
        }
    }

    @Test
    fun milestoneRecognitionIsIdempotentAndRetainsOriginalProof() {
        val db = opened()
        try {
            val repository = SqliteTrainingMilestones(db)
            val at = StoredInstant.parse("2026-11-15T09:00:00.123456789Z")
            val proof =
                org.racedaydreams.domain.achievements.TrainingMilestones.Proof(
                    org.racedaydreams.domain.achievements.TrainingMilestones.Source.RUN,
                    "retained-run",
                    at,
                    null,
                    5000.0,
                )
            val award =
                org.racedaydreams.domain.achievements.TrainingMilestones.Award(
                    org.racedaydreams.domain.achievements.TrainingMilestones.Kind.FIRST_RUN,
                    at,
                    listOf(proof),
                )
            val now = StoredInstant.parse("2026-11-15T10:00:00Z")
            assertFalse(repository.celebrations())
            repository.celebrations(true)
            assertTrue(repository.celebrations())
            assertEquals(1, repository.recognize(listOf(award, award), now))
            assertEquals(
                0,
                repository.recognize(listOf(award), StoredInstant.parse("2026-11-15T11:00:00Z")),
            )
            assertEquals(listOf(SqliteTrainingMilestones.Stored(award, now)), repository.awards())
            repository.celebrations(false)
            assertFalse(repository.celebrations())
            assertEquals(1, repository.awards().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun failedMilestoneBatchCannotLeavePartialRecognitionOrProof() {
        val db = opened()
        try {
            val repository = SqliteTrainingMilestones(db)
            val at = StoredInstant.parse("2026-11-15T09:00:00Z")
            val future = StoredInstant.parse("2026-11-15T12:00:00Z")
            val run =
                org.racedaydreams.domain.achievements.TrainingMilestones.Award(
                    org.racedaydreams.domain.achievements.TrainingMilestones.Kind.FIRST_RUN,
                    at,
                    listOf(
                        org.racedaydreams.domain.achievements.TrainingMilestones.Proof(
                            org.racedaydreams.domain.achievements.TrainingMilestones.Source.RUN,
                            "run",
                            at,
                            null,
                            1000.0,
                        )
                    ),
                )
            val gym =
                org.racedaydreams.domain.achievements.TrainingMilestones.Award(
                    org.racedaydreams.domain.achievements.TrainingMilestones.Kind.FIRST_GYM,
                    future,
                    listOf(
                        org.racedaydreams.domain.achievements.TrainingMilestones.Proof(
                            org.racedaydreams.domain.achievements.TrainingMilestones.Source.GYM,
                            "gym",
                            future,
                            null,
                            null,
                        )
                    ),
                )
            assertFailsWith<IllegalArgumentException> { repository.recognize(listOf(run, gym), at) }
            assertTrue(repository.awards().isEmpty())
            assertTrue(db.query("SELECT * FROM training_milestone_sources").isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun importedHeightAndPreferenceChangesShareOuterRollback() {
        val db = opened()
        try {
            val repository = SqliteBodyHeights(db)
            val original =
                org.racedaydreams.domain.body.BodyHeight(
                    "original",
                    1.78,
                    StoredInstant.parse("2026-11-15T09:00:00Z"),
                    null,
                    "healthkit",
                    "source",
                    null,
                    null,
                )
            repository.saveHeight(original)
            val defaults = repository.preferences()
            assertFailsWith<IllegalArgumentException> {
                db.transaction {
                    repository.saveHeight(original.copy(id = "new", sourceId = "new-source"))
                    repository.preferences(
                        org.racedaydreams.domain.body.BodyMeasurementsRepository.Preferences(
                            true,
                            org.racedaydreams.domain.body.HeightUnit.INCHES,
                            null,
                        )
                    )
                    repository.saveHeight(original.copy(id = "conflicting"))
                }
            }
            assertEquals(listOf(original), repository.heights(500, 0))
            assertEquals(defaults, repository.preferences())
        } finally {
            db.close()
        }
    }

    @Test
    fun shoeEditsAndDefaultChangesPreserveForeignKeyAssignments() {
        val db = opened()
        try {
            val repository = SqliteShoes(db)
            val first =
                org.racedaydreams.domain.activity.Shoe(
                    "first",
                    "Brand",
                    "Daily",
                    null,
                    null,
                    false,
                    null,
                    true,
                )
            repository.saveShoe(first)
            db.execute(
                "INSERT INTO activity_annotations(provider,record_id,shoe_id) VALUES('rdd','retained-run','first')"
            )
            repository.saveShoe(first.copy(model = "Updated", notes = "retained note"))
            assertEquals(
                SqlValue.Text("first"),
                db.query("SELECT shoe_id FROM activity_annotations").single().single(),
            )
            val second = first.copy(id = "second", model = "Race")
            repository.saveShoe(second)
            assertEquals(listOf("second", "first"), repository.shoes().map { it.id() })
            assertFalse(repository.shoes().last().defaultShoe())
            assertEquals(
                SqlValue.Text("first"),
                db.query("SELECT shoe_id FROM activity_annotations").single().single(),
            )
            assertEquals("retained note", repository.shoes().last().notes())
        } finally {
            db.close()
        }
    }

    @Test
    fun failedNewDefaultShoeRestoresPreviousDefault() {
        val db = opened()
        try {
            val repository = SqliteShoes(db)
            val original =
                org.racedaydreams.domain.activity.Shoe(
                    "retained",
                    "Brand",
                    "Daily",
                    null,
                    0.0,
                    false,
                    null,
                    true,
                )
            repository.saveShoe(original)
            db.execute(
                "CREATE TRIGGER reject_new_shoe BEFORE INSERT ON shoes BEGIN SELECT RAISE(ABORT,'test failure'); END"
            )
            assertFails { repository.saveShoe(original.copy(id = "blocked")) }
            assertEquals(listOf(original), repository.shoes())
        } finally {
            db.close()
        }
    }

    @Test
    fun strengthChoicesRemainReversibleWithoutCreatingOrDeletingEvidence() {
        val db = opened()
        try {
            val choices = SqliteStrengthChoices(db)
            assertTrue(choices.choices().isEmpty())
            choices.save(SqliteStrengthChoices.Choice("bench", true, false))
            choices.save(SqliteStrengthChoices.Choice("pull-up", false, true))
            choices.save(SqliteStrengthChoices.Choice("bench", false, true))
            assertEquals(
                listOf(
                    SqliteStrengthChoices.Choice("bench", false, true),
                    SqliteStrengthChoices.Choice("pull-up", false, true),
                ),
                choices.choices(),
            )
            choices.save(SqliteStrengthChoices.Choice("bench", true, false))
            assertEquals(
                SqliteStrengthChoices.Choice("bench", true, false),
                choices.choices().first(),
            )
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
            assertTrue(db.query("SELECT exercise_id FROM strength_exclusions").isEmpty())
            assertTrue(db.query("SELECT kind FROM training_milestones").isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun strengthAcknowledgementsAreIdempotentAndNeverCreatePersonalBestEvidence() {
        val db = opened()
        try {
            val choices = SqliteStrengthChoices(db)
            assertTrue(choices.acknowledged().isEmpty())
            choices.acknowledge("recorded-bench-5")
            choices.acknowledge("recorded-bench-5")
            choices.acknowledge("recorded-pull-up")
            assertEquals(setOf("recorded-bench-5", "recorded-pull-up"), choices.acknowledged())
            assertTrue(db.query("SELECT id FROM manual_results").isEmpty())
            assertTrue(db.query("SELECT id FROM strength_sessions").isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun strengthChoiceAndAcknowledgementWritesJoinAnOuterTransaction() {
        val db = opened()
        try {
            val choices = SqliteStrengthChoices(db)
            choices.save(SqliteStrengthChoices.Choice("existing", true, false))
            choices.acknowledge("old-record")
            assertFails {
                db.transaction {
                    choices.save(SqliteStrengthChoices.Choice("existing", false, true))
                    choices.save(SqliteStrengthChoices.Choice("new", true, false))
                    choices.acknowledge("new-record")
                    error("Aborted operation")
                }
            }
            assertEquals(
                listOf(SqliteStrengthChoices.Choice("existing", true, false)),
                choices.choices(),
            )
            assertEquals(setOf("old-record"), choices.acknowledged())
        } finally {
            db.close()
        }
    }

    @Test
    fun strengthChoiceValidationPreservesOriginalIdentityWithoutTrimmingOrCoercion() {
        val db = opened()
        try {
            val choices = SqliteStrengthChoices(db)
            for (id in listOf("", "x".repeat(201))) assertFailsWith<IllegalArgumentException> {
                choices.save(SqliteStrengthChoices.Choice(id, true, false))
            }
            for (id in listOf("", "x".repeat(501))) assertFailsWith<IllegalArgumentException> {
                choices.acknowledge(id)
            }
            val id = " old-press\u0000🏋️ "
            choices.save(SqliteStrengthChoices.Choice(id, true, false))
            choices.acknowledge(id)
            assertEquals(id, choices.choices().single().exerciseId)
            assertEquals(setOf(id), choices.acknowledged())
        } finally {
            db.close()
        }
    }

    @Test
    fun typedOnboardingDraftKeepsExactInputAndClearingDoesNotDeleteProfileOrHistory() {
        val db = opened()
        try {
            val profiles = SqliteAthleteProfile(db)
            val original = athlete()
            profiles.save(original)
            db.execute(
                "INSERT INTO activities VALUES('retained','rdd','source',NULL,10,20,'original history')"
            )
            assertNull(profiles.loadDraft())
            val draft = """ { "step":2, "unknown":"café 🏃", "target":null } """
            profiles.saveDraft(draft)
            assertEquals(draft, SqliteAthleteProfile(db).loadDraft())
            assertEquals(original, profiles.load())
            profiles.clearDraft()
            assertNull(profiles.loadDraft())
            assertEquals(original, profiles.load())
            assertEquals(
                SqlValue.Text("original history"),
                db.query("SELECT payload FROM activities WHERE id='retained'").single().single(),
            )
            assertTrue(db.query("PRAGMA foreign_key_check").isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun invalidDraftAndRejectedReplacementRetainOriginalFormInput() {
        val db = opened()
        try {
            val profiles = SqliteAthleteProfile(db)
            val original = """ {"step":2,"retainedUnknown":"Original input"} """
            profiles.saveDraft(original)
            for (invalid in listOf("not JSON", "[]", "null", "42")) {
                assertFails { profiles.saveDraft(invalid) }
                assertEquals(original, profiles.loadDraft())
            }
            assertFailsWith<IllegalStateException> {
                db.transaction {
                    profiles.saveDraft("""{"step":3}""")
                    assertEquals("""{"step":3}""", profiles.loadDraft())
                    error("Reject the enclosing edit")
                }
            }
            assertEquals(original, profiles.loadDraft())
            assertNull(profiles.load())
        } finally {
            db.close()
        }
    }

    @Test
    fun shoeFeedbackProjectionReadsOnlyThatPreferenceAndNeverLoadsOrRewritesUnrelatedData() {
        val db = opened()
        try {
            val reader = SqliteRunningSupportArchives(db)
            assertTrue(reader.shoeDiscomfort().isEmpty())
            db.execute(
                "INSERT INTO release08(kind,id,payload) VALUES('custom','unrelated','not a loaded route/workout')"
            )
            db.execute(
                "INSERT INTO release08(kind,id,payload) VALUES('config','shoe-feedback',?)",
                listOf(
                    SqlValue.Text(
                        """{"version":1,"type":"shoe_feedback","activities":["retained-run","older-run"]}"""
                    )
                ),
            )
            val before = db.query("SELECT kind,id,payload FROM release08 ORDER BY kind,id")
            assertEquals(setOf("retained-run", "older-run"), reader.shoeDiscomfort())
            assertEquals(before, db.query("SELECT kind,id,payload FROM release08 ORDER BY kind,id"))
            db.execute(
                "UPDATE release08 SET payload=? WHERE kind='config' AND id='shoe-feedback'",
                listOf(
                    SqlValue.Text(
                        """{"version":2,"type":"shoe_feedback","activities":["retained-run"]}"""
                    )
                ),
            )
            val rejected = db.query("SELECT kind,id,payload FROM release08 ORDER BY kind,id")
            assertFails { reader.shoeDiscomfort() }
            assertEquals(
                rejected,
                db.query("SELECT kind,id,payload FROM release08 ORDER BY kind,id"),
            )
        } finally {
            db.close()
        }
    }
}
