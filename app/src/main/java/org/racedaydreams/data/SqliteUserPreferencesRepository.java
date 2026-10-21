package org.racedaydreams.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;
import org.racedaydreams.domain.GoalMode;
import org.racedaydreams.domain.RaceDistance;
import org.racedaydreams.domain.RaceGoal;
import org.racedaydreams.domain.ScheduleType;
import org.racedaydreams.domain.TrainingPreferences;
import org.racedaydreams.domain.TrainingStrategy;
import org.racedaydreams.domain.UnitSystem;
import org.racedaydreams.domain.UserPreferencesRepository;
import org.racedaydreams.domain.UserProfile;

import java.io.File;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * On-device, account-free profile storage. All methods except construction must run on an IO
 * executor. A save replaces the complete profile in one transaction, preserving the previous
 * profile if any insert fails. Missing or invalid related rows are errors, never new-user state.
 */
public final class SqliteUserPreferencesRepository
        implements UserPreferencesRepository, AutoCloseable {
    private final Context context;
    private final ProfileDatabase helper;

    public SqliteUserPreferencesRepository(Context context) {
        this.context = Objects.requireNonNull(context).getApplicationContext();
        helper = new ProfileDatabase(this.context);
    }

    @Override
    public Optional<UserProfile> load() {
        requireIoThread();
        SQLiteDatabase db = helper.getReadableDatabase();
        // A read transaction ensures that the profile and all child rows form one snapshot.
        db.beginTransactionNonExclusive();
        try {
            Optional<UserProfile> result = readProfile(db);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    private Optional<UserProfile> readProfile(SQLiteDatabase db) {
        try (Cursor profile = db.rawQuery(
                "SELECT performance_eligible FROM user_profile WHERE id = 1", null)) {
            if (!profile.moveToFirst()) {
                requireNoOrphanedRows(db);
                return Optional.empty();
            }
            int eligibility = profile.getInt(0);
            if (eligibility != 0 && eligibility != 1) {
                throw new IllegalStateException("Saved performance eligibility is invalid.");
            }
            RaceGoal goal = readGoal(db);
            TrainingPreferences preferences = readPreferences(db);
            return Optional.of(new UserProfile(goal, preferences, eligibility == 1));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalStateException(
                    "Saved preferences could not be read. Your data has not been changed.", ex);
        }
    }

    private static void requireNoOrphanedRows(SQLiteDatabase db) {
        for (String table : new String[]{"race_goal", "training_preferences", "availability_day"}) {
            try (Cursor rows = db.rawQuery("SELECT COUNT(*) FROM " + table, null)) {
                rows.moveToFirst();
                if (rows.getInt(0) != 0) {
                    throw new IllegalStateException("Saved profile is incomplete; data was retained.");
                }
            }
        }
    }

    private static RaceGoal readGoal(SQLiteDatabase db) {
        try (Cursor goal = db.rawQuery(
                "SELECT distance, race_name, race_date, goal_mode, target_seconds "
                        + "FROM race_goal WHERE user_id = 1", null)) {
            requireRow(goal, "race goal");
            return new RaceGoal(
                    RaceDistance.valueOf(goal.getString(0)),
                    goal.getString(1),
                    goal.isNull(2) ? null : LocalDate.parse(goal.getString(2)),
                    GoalMode.valueOf(goal.getString(3)),
                    goal.isNull(4) ? null : Duration.ofSeconds(goal.getLong(4)));
        }
    }

    private static TrainingPreferences readPreferences(SQLiteDatabase db) {
        try (Cursor prefs = db.rawQuery(
                "SELECT strategy, runs_per_week, long_run_day, schedule_type, units "
                        + "FROM training_preferences WHERE user_id = 1", null)) {
            requireRow(prefs, "training preferences");
            return new TrainingPreferences(
                    TrainingStrategy.valueOf(prefs.getString(0)),
                    readDays(db, "RUN"), prefs.getInt(1),
                    prefs.isNull(2) ? null : DayOfWeek.of(prefs.getInt(2)),
                    readDays(db, "GYM"),
                    ScheduleType.valueOf(prefs.getString(3)),
                    UnitSystem.valueOf(prefs.getString(4)));
        }
    }

    private static Set<DayOfWeek> readDays(SQLiteDatabase db, String kind) {
        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        try (Cursor cursor = db.rawQuery(
                "SELECT day FROM availability_day WHERE user_id = 1 AND kind = ? ORDER BY day",
                new String[]{kind})) {
            while (cursor.moveToNext()) {
                days.add(DayOfWeek.of(cursor.getInt(0)));
            }
        }
        return days;
    }

    private static void requireRow(Cursor cursor, String name) {
        if (!cursor.moveToFirst()) {
            throw new IllegalStateException("Saved " + name + " is missing; data was retained.");
        }
    }

    @Override
    public void save(UserProfile profile) {
        requireIoThread();
        Objects.requireNonNull(profile, "profile");
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("user_profile", "id = 1", null);
            ContentValues user = new ContentValues();
            user.put("id", 1);
            user.put("performance_eligible", profile.performanceEligible() ? 1 : 0);
            db.insertOrThrow("user_profile", null, user);
            saveGoal(db, profile.raceGoal());
            savePreferences(db, profile.preferences());
            db.delete("onboarding_draft", null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static void saveGoal(SQLiteDatabase db, RaceGoal goal) {
        ContentValues values = new ContentValues();
        values.put("user_id", 1);
        values.put("distance", goal.distance().name());
        values.put("race_name", goal.raceName());
        if (goal.raceDate() == null) values.putNull("race_date");
        else values.put("race_date", goal.raceDate().toString());
        values.put("goal_mode", goal.mode().name());
        if (goal.targetTime() == null) values.putNull("target_seconds");
        else values.put("target_seconds", goal.targetTime().getSeconds());
        db.insertOrThrow("race_goal", null, values);
    }

    private static void savePreferences(SQLiteDatabase db, TrainingPreferences preferences) {
        ContentValues values = new ContentValues();
        values.put("user_id", 1);
        values.put("strategy", preferences.strategy().name());
        values.put("runs_per_week", preferences.runsPerWeek());
        if (preferences.longRunDay() == null) values.putNull("long_run_day");
        else values.put("long_run_day", preferences.longRunDay().getValue());
        values.put("schedule_type", preferences.scheduleType().name());
        values.put("units", preferences.units().name());
        db.insertOrThrow("training_preferences", null, values);
        saveDays(db, "RUN", preferences.runningDays());
        saveDays(db, "GYM", preferences.gymDays());
    }

    private static void saveDays(SQLiteDatabase db, String kind, Set<DayOfWeek> days) {
        for (DayOfWeek day : days) {
            ContentValues values = new ContentValues();
            values.put("user_id", 1);
            values.put("kind", kind);
            values.put("day", day.getValue());
            db.insertOrThrow("availability_day", null, values);
        }
    }

    public Optional<String> loadDraft() {
        requireIoThread();
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                "SELECT payload FROM onboarding_draft WHERE id = 1", null)) {
            return cursor.moveToFirst() ? Optional.of(cursor.getString(0)) : Optional.empty();
        }
    }

    /** Stores only an app-owned JSON object; no health-provider or network data is involved. */
    public void saveDraft(String json) {
        requireIoThread();
        Objects.requireNonNull(json, "json");
        try {
            new JSONObject(json);
        } catch (JSONException ex) {
            throw new IllegalArgumentException("An onboarding draft must be a JSON object.", ex);
        }
        ContentValues values = new ContentValues();
        values.put("id", 1);
        values.put("payload", json);
        long rowId = helper.getWritableDatabase().insertWithOnConflict(
                "onboarding_draft", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (rowId == -1) {
            throw new SQLiteException("Your onboarding draft could not be saved.");
        }
    }

    public void clearDraft() {
        requireIoThread();
        helper.getWritableDatabase().delete("onboarding_draft", null, null);
    }

    @Override
    public void clear() {
        requireIoThread();
        // This is the explicitly confirmed reset, not a read-error recovery path. Deleting the
        // closed database also works when corruption prevents SQLite from opening it at all.
        helper.close();
        File database = context.getDatabasePath(ProfileDatabase.NAME);
        boolean databaseExisted = database.exists();
        boolean deleted = context.deleteDatabase(ProfileDatabase.NAME);
        if (!deleted && databaseExisted) {
            throw new SQLiteException("Your local data could not be deleted.");
        }
        for (String suffix : new String[]{"", "-journal", "-wal", "-shm"}) {
            if (new File(database.getPath() + suffix).exists()) {
                throw new SQLiteException("Your local data could not be completely deleted.");
            }
        }
    }

    @Override
    public void close() {
        requireIoThread();
        helper.close();
    }

    private static void requireIoThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Local storage must be accessed on an IO executor.");
        }
    }
}
