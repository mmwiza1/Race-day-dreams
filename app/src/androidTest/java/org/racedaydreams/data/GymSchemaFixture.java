package org.racedaydreams.data;

import android.database.sqlite.SQLiteDatabase;

/**
 * Removes v11 and subsequent additive tables when constructing an authentic older-schema fixture.
 */
final class GymSchemaFixture {
    private GymSchemaFixture() {}

    static void removeVersionElevenTables(SQLiteDatabase db) {
        // Children must disappear before parents when the fixture connection enforces FKs.
        for (String table :
                new String[] {
                    "strength_exclusions",
                    "strength_association_targets",
                    "strength_association_decisions",
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
