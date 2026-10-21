package org.racedaydreams.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

/** Owns the local schema. Future versions must supply explicit, non-destructive migrations. */
final class ProfileDatabase extends SQLiteOpenHelper {
    static final String NAME = "racedaydreams.db";
    static final int VERSION = 1;

    ProfileDatabase(Context context) {
        super(context.getApplicationContext(), NAME, null, VERSION);
        setWriteAheadLoggingEnabled(true);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE user_profile ("
                + "id INTEGER PRIMARY KEY CHECK(id = 1), "
                + "performance_eligible INTEGER NOT NULL CHECK(performance_eligible IN (0, 1)))");
        db.execSQL("CREATE TABLE race_goal ("
                + "user_id INTEGER PRIMARY KEY REFERENCES user_profile(id) ON DELETE CASCADE, "
                + "distance TEXT NOT NULL, race_name TEXT NOT NULL, race_date TEXT, "
                + "goal_mode TEXT NOT NULL, target_seconds INTEGER CHECK(target_seconds > 0))");
        db.execSQL("CREATE TABLE training_preferences ("
                + "user_id INTEGER PRIMARY KEY REFERENCES user_profile(id) ON DELETE CASCADE, "
                + "strategy TEXT NOT NULL, runs_per_week INTEGER NOT NULL "
                + "CHECK(runs_per_week BETWEEN 1 AND 7), "
                + "long_run_day INTEGER CHECK(long_run_day BETWEEN 1 AND 7), "
                + "schedule_type TEXT NOT NULL, units TEXT NOT NULL)");
        db.execSQL("CREATE TABLE availability_day ("
                + "user_id INTEGER NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE, "
                + "kind TEXT NOT NULL CHECK(kind IN ('RUN', 'GYM')), "
                + "day INTEGER NOT NULL CHECK(day BETWEEN 1 AND 7), "
                + "PRIMARY KEY(user_id, kind, day))");
        db.execSQL("CREATE TABLE onboarding_draft ("
                + "id INTEGER PRIMARY KEY CHECK(id = 1), payload TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new SQLiteException("An explicit migration is required from schema "
                + oldVersion + " to " + newVersion + ". Existing data was retained.");
    }
}
