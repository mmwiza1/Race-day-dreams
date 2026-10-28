package org.racedaydreams.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

/** Owns the local schema. Future versions must supply explicit, non-destructive migrations. */
final class ProfileDatabase extends SQLiteOpenHelper {
    static final String NAME = "racedaydreams.db";
    static final int VERSION = 13;
    private static ProfileDatabase shared;

    /** A shared helper ensures confirmed reset closes every repository's connection pool. */
    static synchronized ProfileDatabase get(Context context) {
        if (shared == null) shared = new ProfileDatabase(context);
        return shared;
    }

    ProfileDatabase(Context context) {
        super(
                context.getApplicationContext(),
                NAME,
                null,
                VERSION,
                db -> {
                    // Android's default corruption handler deletes database files. Preserve them
                    // until
                    // the runner explicitly chooses the app's confirmed local-data reset instead.
                    throw new SQLiteDatabaseCorruptException(
                            "Local data could not be read. The database files have been retained.");
                });
        setWriteAheadLoggingEnabled(true);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createVersionOne(db);
        migrateOneToTwo(db);
        migrateTwoToThree(db);
        migrateThreeToFour(db);
        migrateFourToFive(db);
        migrateFiveToSix(db);
        migrateSixToSeven(db);
        migrateSevenToEight(db);
        migrateEightToNine(db);
        migrateNineToTen(db);
        migrateTenToEleven(db);
        migrateElevenToTwelve(db);
        migrateTwelveToThirteen(db);
    }

    private static void createVersionOne(SQLiteDatabase db) {
        // V1_SCHEMA_BEGIN - retained unchanged for migration fixtures.
        db.execSQL(
                "CREATE TABLE user_profile (id INTEGER PRIMARY KEY CHECK(id = 1),"
                        + " performance_eligible INTEGER NOT NULL CHECK(performance_eligible IN (0,"
                        + " 1)))");
        db.execSQL(
                "CREATE TABLE race_goal (user_id INTEGER PRIMARY KEY REFERENCES user_profile(id) ON"
                    + " DELETE CASCADE, distance TEXT NOT NULL, race_name TEXT NOT NULL, race_date"
                    + " TEXT, goal_mode TEXT NOT NULL, target_seconds INTEGER CHECK(target_seconds"
                    + " > 0))");
        db.execSQL(
                "CREATE TABLE training_preferences (user_id INTEGER PRIMARY KEY REFERENCES"
                    + " user_profile(id) ON DELETE CASCADE, strategy TEXT NOT NULL, runs_per_week"
                    + " INTEGER NOT NULL CHECK(runs_per_week BETWEEN 1 AND 7), long_run_day INTEGER"
                    + " CHECK(long_run_day BETWEEN 1 AND 7), schedule_type TEXT NOT NULL, units"
                    + " TEXT NOT NULL)");
        db.execSQL(
                "CREATE TABLE availability_day ("
                        + "user_id INTEGER NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE, "
                        + "kind TEXT NOT NULL CHECK(kind IN ('RUN', 'GYM')), "
                        + "day INTEGER NOT NULL CHECK(day BETWEEN 1 AND 7), "
                        + "PRIMARY KEY(user_id, kind, day))");
        db.execSQL(
                "CREATE TABLE onboarding_draft ("
                        + "id INTEGER PRIMARY KEY CHECK(id = 1), payload TEXT NOT NULL)");
        // V1_SCHEMA_END
    }

    /** Additive only: runner goals, availability and unfinished onboarding remain untouched. */
    private static void migrateOneToTwo(SQLiteDatabase db) {
        // V2_SCHEMA_BEGIN
        db.execSQL(
                "CREATE TABLE activities (id TEXT PRIMARY KEY NOT NULL, provider TEXT NOT NULL,"
                    + " record_id TEXT NOT NULL, origin_package TEXT, start_millis INTEGER NOT"
                    + " NULL, end_millis INTEGER NOT NULL, payload TEXT NOT NULL, UNIQUE(provider,"
                    + " record_id), CHECK(end_millis >= start_millis))");
        db.execSQL("CREATE INDEX activities_start ON activities(start_millis DESC)");
        db.execSQL(
                "CREATE TABLE shoes (id TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL, "
                        + "is_default INTEGER NOT NULL DEFAULT 0 CHECK(is_default IN (0, 1)))");
        db.execSQL(
                "CREATE UNIQUE INDEX shoes_one_default ON shoes(is_default) WHERE is_default = 1");
        db.execSQL(
                "CREATE TABLE activity_annotations (provider TEXT NOT NULL, record_id TEXT NOT"
                    + " NULL, notes TEXT NOT NULL DEFAULT '', rpe REAL CHECK(rpe BETWEEN 0 AND 10),"
                    + " shoe_id TEXT REFERENCES shoes(id) ON DELETE SET NULL, PRIMARY KEY(provider,"
                    + " record_id))");
        db.execSQL(
                "CREATE TABLE manual_results (id TEXT PRIMARY KEY NOT NULL, activity_id TEXT,"
                        + " distance_code TEXT NOT NULL, distance_metres REAL NOT NULL"
                        + " CHECK(distance_metres > 0), result_date TEXT NOT NULL, payload TEXT NOT"
                        + " NULL)");
        db.execSQL(
                "CREATE TABLE best_efforts (id TEXT PRIMARY KEY NOT NULL, activity_id TEXT NOT NULL"
                    + " REFERENCES activities(id) ON DELETE CASCADE, distance_code TEXT NOT NULL,"
                    + " payload TEXT NOT NULL, UNIQUE(activity_id, distance_code))");
        db.execSQL(
                "CREATE TABLE preserved_best_efforts (id TEXT PRIMARY KEY NOT NULL, activity_id"
                        + " TEXT NOT NULL, distance_code TEXT NOT NULL, provider TEXT NOT NULL,"
                        + " record_id TEXT NOT NULL, origin_package TEXT, payload TEXT NOT NULL,"
                        + " UNIQUE(activity_id, distance_code))");
        db.execSQL(
                "CREATE INDEX preserved_efforts_source ON preserved_best_efforts(provider,"
                        + " record_id)");
        db.execSQL(
                "CREATE TABLE import_checkpoints (provider TEXT PRIMARY KEY NOT NULL, "
                        + "token TEXT, updated_at TEXT NOT NULL)");
        db.execSQL(
                "CREATE TABLE baseline_state (id INTEGER PRIMARY KEY CHECK(id = 1), "
                        + "manual_json TEXT NOT NULL, manual_recorded_at TEXT, "
                        + "reviewed INTEGER NOT NULL DEFAULT 0 CHECK(reviewed IN (0, 1)))");
        db.execSQL(
                "CREATE TABLE settings (setting_key TEXT PRIMARY KEY NOT NULL, setting_value TEXT"
                        + " NOT NULL)");
        db.execSQL(
                "CREATE TABLE assessment_results (id TEXT PRIMARY KEY NOT NULL, activity_id TEXT, "
                        + "payload TEXT NOT NULL)");
        db.execSQL(
                "CREATE TABLE planned_workouts (id TEXT PRIMARY KEY NOT NULL, scheduled_date TEXT"
                    + " NOT NULL, revision INTEGER NOT NULL CHECK(revision > 0), payload TEXT NOT"
                    + " NULL)");
        db.execSQL(
                "CREATE TABLE training_plans (id TEXT PRIMARY KEY NOT NULL, payload TEXT NOT"
                        + " NULL)");
        db.execSQL(
                "CREATE TABLE plan_adjustments (id TEXT PRIMARY KEY NOT NULL, plan_id TEXT, "
                        + "user_decision TEXT, payload TEXT NOT NULL)");
        // V2_SCHEMA_END
    }

    /**
     * Public draft builds used both non-null and nullable v2 annotation notes. Copying the stored
     * value unchanged supports both: null retains provider text, while empty text is an explicit
     * clearing. SQLiteOpenHelper runs the entire upgrade in one transaction.
     */
    private static void migrateTwoToThree(SQLiteDatabase db) {
        // V3_SCHEMA_BEGIN
        db.execSQL(
                "CREATE TABLE activity_annotations_v3 (provider TEXT NOT NULL, record_id TEXT NOT"
                        + " NULL, notes TEXT, rpe REAL CHECK(rpe BETWEEN 0 AND 10), shoe_id TEXT"
                        + " REFERENCES shoes(id) ON DELETE SET NULL, PRIMARY KEY(provider,"
                        + " record_id))");
        db.execSQL(
                "INSERT INTO activity_annotations_v3 (provider, record_id, notes, rpe, shoe_id)"
                    + " SELECT provider, record_id, notes, rpe, shoe_id FROM activity_annotations");
        db.execSQL("DROP TABLE activity_annotations");
        db.execSQL("ALTER TABLE activity_annotations_v3 RENAME TO activity_annotations");
        // V3_SCHEMA_END
    }

    private static void migrateThreeToFour(SQLiteDatabase db) {
        // V4_SCHEMA_BEGIN
        db.execSQL(
                "CREATE TABLE coach_plan_versions (id TEXT PRIMARY KEY NOT NULL, plan_id TEXT NOT"
                    + " NULL, version INTEGER NOT NULL CHECK(version > 0), payload TEXT NOT NULL,"
                    + " UNIQUE(plan_id, version))");
        db.execSQL(
                "CREATE TABLE coach_current_plan (id INTEGER PRIMARY KEY CHECK(id = 1), version_id"
                        + " TEXT NOT NULL REFERENCES coach_plan_versions(id))");
        db.execSQL(
                "CREATE TABLE coach_proposals (id TEXT PRIMARY KEY NOT NULL, base_version_id TEXT"
                    + " NOT NULL REFERENCES coach_plan_versions(id), payload TEXT NOT NULL,"
                    + " decision TEXT, result_version_id TEXT REFERENCES coach_plan_versions(id))");
        db.execSQL(
                "CREATE TABLE strength_exercises (id TEXT PRIMARY KEY NOT NULL, payload TEXT NOT"
                        + " NULL)");
        db.execSQL(
                "CREATE TABLE strength_sessions (id TEXT PRIMARY KEY NOT NULL, started_at TEXT NOT"
                        + " NULL, payload TEXT NOT NULL)");
        db.execSQL(
                "CREATE TABLE run_responses (activity_id TEXT PRIMARY KEY NOT NULL,"
                        + " planned_workout_id TEXT, plan_version_id TEXT REFERENCES"
                        + " coach_plan_versions(id), payload TEXT NOT NULL)");
        db.execSQL(
                "CREATE TABLE run_feedback (activity_id TEXT PRIMARY KEY NOT NULL, plan_version_id"
                    + " TEXT NOT NULL REFERENCES coach_plan_versions(id), payload TEXT NOT NULL)");
        // V4_SCHEMA_END
    }

    private static void migrateFourToFive(SQLiteDatabase db) {
        // V5_SCHEMA_BEGIN
        db.execSQL(
                "CREATE TABLE coach_evidence (kind TEXT NOT NULL CHECK(kind IN"
                    + " ('WEIGHT','HISTORICAL_PB','WEEKLY_REVIEW','REVIEW_DECISION','INTERRUPTION','STRENGTH_LINK','SESSION_DECISION')),"
                    + " id TEXT NOT NULL, payload TEXT NOT NULL, PRIMARY KEY(kind,id))");
        // V5_SCHEMA_END
    }

    private static void migrateFiveToSix(SQLiteDatabase db) {
        // V6_SCHEMA_BEGIN
        db.execSQL(
                "UPDATE settings SET setting_value='HEALTH_CONNECT' WHERE"
                        + " setting_key='provider.preference'");
        // Local user choices. Legacy activity payloads/identities remain untouched.
        db.execSQL(
                "CREATE TABLE strength_user_state (exercise_id TEXT PRIMARY KEY NOT NULL, favourite"
                    + " INTEGER NOT NULL DEFAULT 0 CHECK(favourite IN (0,1)), archived INTEGER NOT"
                    + " NULL DEFAULT 0 CHECK(archived IN (0,1)))");
        db.execSQL("CREATE TABLE pb_acknowledgements (id TEXT PRIMARY KEY NOT NULL)");
        db.execSQL(
                "CREATE TABLE gym_plan_versions (version_id TEXT PRIMARY KEY NOT NULL, plan_id TEXT"
                        + " NOT NULL, version INTEGER NOT NULL CHECK(version > 0), payload TEXT NOT"
                        + " NULL, UNIQUE(plan_id,version))");
        db.execSQL(
                "CREATE TABLE gym_current (id INTEGER PRIMARY KEY CHECK(id=1), version_id TEXT NOT"
                        + " NULL REFERENCES gym_plan_versions(version_id))");
        db.execSQL(
                "CREATE TABLE gym_progression_decisions (id TEXT PRIMARY KEY NOT NULL, payload TEXT"
                        + " NOT NULL)");
        // V6_SCHEMA_END
    }

    private static void migrateSixToSeven(SQLiteDatabase db) {
        // V7_SCHEMA_BEGIN
        db.execSQL(
                "CREATE TABLE health_observations (id TEXT PRIMARY KEY NOT NULL, metric TEXT NOT"
                    + " NULL, day TEXT NOT NULL, start_time TEXT NOT NULL, end_time TEXT NOT NULL,"
                    + " value REAL NOT NULL CHECK(value>=0), source TEXT NOT NULL, imported_at TEXT"
                    + " NOT NULL)");
        db.execSQL("CREATE INDEX health_metric_day ON health_observations(metric,day)");
        db.execSQL(
                "CREATE TABLE recording_sessions (id TEXT PRIMARY KEY NOT NULL, state TEXT NOT"
                    + " NULL, started_at TEXT NOT NULL, started_mono INTEGER NOT NULL, last_mono"
                    + " INTEGER NOT NULL, activity_id TEXT)");
        db.execSQL(
                "CREATE UNIQUE INDEX one_active_recording ON recording_sessions((1)) WHERE state IN"
                        + " ('ACTIVE','PAUSED','RECOVERABLE')");
        db.execSQL(
                "CREATE TABLE recording_points (session_id TEXT NOT NULL REFERENCES"
                        + " recording_sessions(id) ON DELETE CASCADE, sequence INTEGER NOT NULL,"
                        + " payload TEXT NOT NULL, PRIMARY KEY(session_id,sequence))");
        db.execSQL(
                "CREATE TABLE recording_events (session_id TEXT NOT NULL REFERENCES"
                        + " recording_sessions(id) ON DELETE CASCADE, sequence INTEGER NOT NULL,"
                        + " payload TEXT NOT NULL, PRIMARY KEY(session_id,sequence))");
        // V7_SCHEMA_END
    }

    private static void migrateSevenToEight(SQLiteDatabase db) {
        // V8_SCHEMA_BEGIN
        db.execSQL(
                "CREATE TABLE coach_support (kind TEXT NOT NULL CHECK(kind IN"
                    + " ('execution','dream','routine')), id TEXT NOT NULL, payload TEXT NOT NULL,"
                    + " PRIMARY KEY(kind,id))");
        // V8_SCHEMA_END
    }

    private static void migrateEightToNine(SQLiteDatabase db) {
        // V9_SCHEMA_BEGIN
        db.execSQL(
                "CREATE TABLE release08 (kind TEXT NOT NULL CHECK(kind IN"
                    + " ('custom','config','origin','association','checklist')), id TEXT NOT NULL,"
                    + " payload TEXT NOT NULL, PRIMARY KEY(kind,id))");
        // V9_SCHEMA_END
    }

    private static void migrateNineToTen(SQLiteDatabase db) {
        // V10_SCHEMA_BEGIN
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS activities_history_order ON activities(start_millis"
                        + " DESC,id)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS strength_history_order ON"
                    + " strength_sessions(CAST(ROUND((julianday(started_at)-2440587.5)*86400000) AS"
                    + " INTEGER) DESC,id)");
        // V10_SCHEMA_END
    }

    /** Typed gym durability; later Strength features must use explicit subsequent migrations. */
    private static void migrateTenToEleven(SQLiteDatabase db) {
        // V11_SCHEMA_BEGIN
        db.execSQL(
                "CREATE TABLE strength_workout_versions (workout_id TEXT NOT NULL, revision INTEGER"
                    + " NOT NULL CHECK(revision > 0), source_kind TEXT CHECK(source_kind IN"
                    + " ('PROGRAMME','ACTUAL_SESSION')), source_id TEXT, payload TEXT NOT NULL"
                    + " CHECK(length(CAST(payload AS BLOB)) <= 262144), CHECK((source_kind IS NULL)"
                    + " = (source_id IS NULL)), PRIMARY KEY(workout_id,revision))");
        db.execSQL(
                "CREATE INDEX strength_workout_sources ON"
                        + " strength_workout_versions(source_kind,source_id)");
        db.execSQL(
                "CREATE TABLE strength_workout_preferences (workout_id TEXT PRIMARY KEY NOT NULL,"
                    + " current_revision INTEGER NOT NULL CHECK(current_revision > 0), favourite"
                    + " INTEGER NOT NULL CHECK(favourite IN (0,1)), FOREIGN"
                    + " KEY(workout_id,current_revision) REFERENCES"
                    + " strength_workout_versions(workout_id,revision))");
        db.execSQL(
                "CREATE TABLE strength_executions (id TEXT PRIMARY KEY NOT NULL, revision INTEGER"
                    + " NOT NULL CHECK(revision > 0), origin TEXT NOT NULL CHECK(origin IN"
                    + " ('PLANNED','QUICK','CUSTOM','REPEATED','FAVOURITE','GENERATED','FREE')),"
                    + " state TEXT NOT NULL CHECK(state IN"
                    + " ('ACTIVE','PAUSED','RECOVERY_REQUIRED','FINISHED','DISCARDED')), started_at"
                    + " TEXT NOT NULL, title TEXT NOT NULL, recorded_elapsed INTEGER NOT NULL"
                    + " CHECK(recorded_elapsed BETWEEN 0 AND 604800000), active_millis INTEGER NOT"
                    + " NULL CHECK(active_millis BETWEEN 0 AND 43200000 AND active_millis <="
                    + " recorded_elapsed), set_count INTEGER NOT NULL CHECK(set_count BETWEEN 0 AND"
                    + " 10000), event_count INTEGER NOT NULL CHECK(event_count BETWEEN 1 AND"
                    + " 50000), workout_id TEXT, workout_revision INTEGER, header TEXT NOT NULL"
                    + " CHECK(length(CAST(header AS BLOB)) <= 262144), accepted_actual TEXT"
                    + " CHECK(accepted_actual IS NULL OR length(CAST(accepted_actual AS BLOB)) <="
                    + " 262144), actual_edited_at TEXT, CHECK((state = 'FINISHED') ="
                    + " (accepted_actual IS NOT NULL)), CHECK(actual_edited_at IS NULL OR state ="
                    + " 'FINISHED'), CHECK((workout_id IS NULL) = (workout_revision IS NULL)),"
                    + " FOREIGN KEY(workout_id,workout_revision) REFERENCES"
                    + " strength_workout_versions(workout_id,revision))");
        db.execSQL(
                "CREATE UNIQUE INDEX one_unfinished_gym_execution ON strength_executions((1)) WHERE"
                        + " state IN ('ACTIVE','PAUSED','RECOVERY_REQUIRED')");
        db.execSQL(
                "CREATE INDEX strength_executions_history ON strength_executions(started_at"
                        + " DESC,id)");
        db.execSQL(
                "CREATE TABLE strength_execution_sets (execution_id TEXT NOT NULL REFERENCES"
                    + " strength_executions(id) ON DELETE CASCADE, sequence INTEGER NOT NULL"
                    + " CHECK(sequence BETWEEN 0 AND 9999), actual_set_id TEXT NOT NULL, payload"
                    + " TEXT NOT NULL CHECK(length(CAST(payload AS BLOB)) <= 262144), PRIMARY"
                    + " KEY(execution_id,sequence), UNIQUE(execution_id,actual_set_id))");
        db.execSQL(
                "CREATE TABLE strength_execution_events (execution_id TEXT NOT NULL REFERENCES"
                        + " strength_executions(id) ON DELETE CASCADE, sequence INTEGER NOT NULL"
                        + " CHECK(sequence BETWEEN 0 AND 49999), payload TEXT NOT NULL"
                        + " CHECK(length(CAST(payload AS BLOB)) <= 262144), PRIMARY"
                        + " KEY(execution_id,sequence))");
        // V11_SCHEMA_END
    }

    private static void migrateElevenToTwelve(SQLiteDatabase db) {
        // V12_SCHEMA_BEGIN
        db.execSQL(
                "CREATE TABLE strength_association_decisions (id TEXT PRIMARY KEY NOT NULL,"
                    + " actual_id TEXT NOT NULL, decided_at TEXT NOT NULL, payload TEXT NOT NULL"
                    + " CHECK(length(CAST(payload AS BLOB)) <= 524288))");
        db.execSQL(
                "CREATE INDEX strength_association_history ON"
                        + " strength_association_decisions(actual_id,id)");
        db.execSQL(
                "CREATE TABLE strength_association_targets (actual_id TEXT PRIMARY KEY NOT NULL,"
                    + " programme_key TEXT, session_id TEXT, planned_date TEXT, decision_id TEXT"
                    + " REFERENCES strength_association_decisions(id), CHECK((programme_key IS"
                    + " NULL) = (session_id IS NULL)), CHECK(programme_key IS NULL OR planned_date"
                    + " IS NOT NULL), CHECK(planned_date IS NOT NULL OR decision_id IS NOT NULL))");
        db.execSQL(
                "CREATE INDEX strength_association_target ON"
                        + " strength_association_targets(programme_key,session_id)");
        // V12_SCHEMA_END
        SqliteGymAssociationRepository.rebuildProjection(db);
    }

    private static void migrateTwelveToThirteen(SQLiteDatabase db) {
        // V13_SCHEMA_BEGIN
        db.execSQL(
                "CREATE TABLE strength_exclusions (exercise_id TEXT PRIMARY KEY NOT NULL"
                        + " CHECK(length(exercise_id) BETWEEN 1 AND 200), reason TEXT NOT NULL"
                        + " CHECK(reason IN"
                        + " ('PREFERENCE','EQUIPMENT_UNAVAILABLE','UNCOMFORTABLE','OTHER')),"
                        + " excluded_at TEXT NOT NULL)");
        // V13_SCHEMA_END
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Reject unsupported paths before executing any DDL or touching existing rows.
        if (newVersion != VERSION || (oldVersion < 1 || oldVersion > 12)) {
            throw new SQLiteException(
                    "An explicit migration is required from schema "
                            + oldVersion
                            + " to "
                            + newVersion
                            + ". Existing data was retained.");
        }
        if (oldVersion == 1) migrateOneToTwo(db);
        if (oldVersion <= 2) migrateTwoToThree(db);
        if (oldVersion <= 3) migrateThreeToFour(db);
        if (oldVersion <= 4) migrateFourToFive(db);
        if (oldVersion <= 5) migrateFiveToSix(db);
        if (oldVersion <= 6) migrateSixToSeven(db);
        if (oldVersion <= 7) migrateSevenToEight(db);
        if (oldVersion <= 8) migrateEightToNine(db);
        if (oldVersion <= 9) migrateNineToTen(db);
        if (oldVersion <= 10) migrateTenToEleven(db);
        if (oldVersion <= 11) migrateElevenToTwelve(db);
        migrateTwelveToThirteen(db);
    }
}
