package org.racedaydreams.data;

import android.database.sqlite.SQLiteDatabase;

/**
 * Removes v11 and subsequent additive tables when constructing an authentic older-schema fixture.
 */
final class GymSchemaFixture {
    private GymSchemaFixture() {}

    static void removeVersionElevenTables(SQLiteDatabase db) {
        removeVersionFourteenTables(db);
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

    static void removeVersionNineteenTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE recording_detail_removals");
        db.execSQL("DROP TABLE detail_retention_preferences");
        db.execSQL("DROP TABLE detail_retention_state");
    }

    static void removeVersionEighteenTables(SQLiteDatabase db) {
        removeVersionNineteenTables(db);
        db.execSQL("DROP TABLE body_heights");
        db.execSQL("DROP TABLE body_guidance_preferences");
    }

    static void removeVersionSeventeenTables(SQLiteDatabase db) {
        removeVersionEighteenTables(db);
        db.execSQL("DROP TABLE training_milestone_sources");
        db.execSQL("DROP TABLE training_milestones");
        db.execSQL("DROP TABLE training_milestone_preferences");
    }

    static void removeVersionSixteenTables(SQLiteDatabase db) {
        removeVersionSeventeenTables(db);
        db.execSQL("DROP TABLE gym_rest_timers");
        db.execSQL("DROP TABLE gym_rest_preferences");
    }

    static void removeVersionFifteenTables(SQLiteDatabase db) {
        removeVersionSixteenTables(db);
        for (String table :
                new String[] {
                    "strength_concrete_draft_workouts",
                    "strength_concrete_draft_blocks",
                    "strength_concrete_drafts"
                }) db.execSQL("DROP TABLE " + table);
    }

    static void removeVersionFourteenTables(SQLiteDatabase db) {
        removeVersionFifteenTables(db);
        for (String table :
                new String[] {
                    "strength_programme_current",
                    "strength_programme_decisions",
                    "strength_programme_sessions",
                    "strength_programme_blocks",
                    "strength_programme_revisions",
                    "strength_programme_draft_blocks",
                    "strength_programme_drafts",
                    "strength_custom_exercises"
                }) db.execSQL("DROP TABLE " + table);
    }
}
