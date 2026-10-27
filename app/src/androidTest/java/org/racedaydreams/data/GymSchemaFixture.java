package org.racedaydreams.data;

import android.database.sqlite.SQLiteDatabase;

/** Removes only the v11 additions when constructing an authentic older-schema fixture. */
final class GymSchemaFixture {
    private GymSchemaFixture() {}

    static void removeVersionElevenTables(SQLiteDatabase db) {
        // Children must disappear before parents when the fixture connection enforces FKs.
        for (String table :
                new String[] {
                    "strength_execution_events",
                    "strength_execution_sets",
                    "strength_executions",
                    "strength_workout_preferences",
                    "strength_workout_versions"
                }) {
            db.execSQL("DROP TABLE " + table);
        }
    }
}
