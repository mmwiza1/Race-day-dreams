package org.racedaydreams.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.racedaydreams.domain.PlannedWorkout;
import org.racedaydreams.domain.coaching.PlannedWorkoutRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite adapter for revisioned planned-workout prescriptions. Shares the application database and
 * its transaction boundary.
 */
public final class SqlitePlannedWorkoutRepository extends SqliteStoreAccess
        implements PlannedWorkoutRepository {
    public SqlitePlannedWorkoutRepository(Context context) {
        super(context);
    }

    public List<PlannedWorkout> plannedWorkouts() {
        List<PlannedWorkout> out = new ArrayList<>();
        try (Cursor c =
                read().rawQuery(
                                "SELECT payload FROM planned_workouts ORDER BY scheduled_date,id",
                                null)) {
            while (c.moveToNext()) out.add(FeatureJson.planned(FeatureJson.object(c.getString(0))));
        }
        return out;
    }

    public long plannedWorkoutRevision(String id) {
        try (Cursor c =
                read().rawQuery(
                                "SELECT revision FROM planned_workouts WHERE id=?",
                                new String[] {id})) {
            return c.moveToFirst() ? c.getLong(0) : 0;
        }
    }

    public void savePlannedWorkout(PlannedWorkout workout) {
        SQLiteDatabase db = write();
        db.beginTransaction();
        try {
            long revision = plannedWorkoutRevision(workout.id()) + 1;
            putPlanned(db, workout, revision);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    static void putPlanned(SQLiteDatabase db, PlannedWorkout workout, long revision) {
        ContentValues v = values("id", required(workout.id(), "workout ID"));
        v.put("scheduled_date", workout.date().toString());
        v.put("revision", revision);
        v.put("payload", FeatureJson.planned(workout).toString());
        replace(db, "planned_workouts", v);
    }

    public void deletePlannedWorkout(String id) {
        write().delete("planned_workouts", "id=?", new String[] {required(id, "workout ID")});
    }
}
