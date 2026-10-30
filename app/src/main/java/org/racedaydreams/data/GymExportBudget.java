package org.racedaydreams.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Rejects provably oversized gym exports before any JSON trees or destination bytes exist. */
final class GymExportBudget {
    private GymExportBudget() {}

    /**
     * Counts only compact JSON fragments which the backup necessarily contains. Reconstructed
     * fields, indentation, escaping and the other application sections can make the final export
     * larger; passing this lower-bound check is not a guarantee that it fits or a heap bound.
     *
     * <p>SQLite aggregates UTF-8 byte lengths without returning payloads through CursorWindow.
     * TOTAL avoids integer-SUM overflow. Values at or below the supported integer-sized budget are
     * exact doubles; an aggregate beyond that budget is rejected before conversion or addition.
     * Call inside the same database snapshot transaction as the subsequent export.
     */
    static void requireFits(SQLiteDatabase db, long maximumBytes) {
        if (maximumBytes < 1 || maximumBytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Choose a supported backup byte budget.");
        }
        String query =
                "SELECT TOTAL(length(CAST(payload AS BLOB))) FROM strength_workout_versions"
                        + " UNION ALL SELECT TOTAL(length(CAST(workout_id AS BLOB)))"
                        + " FROM strength_workout_preferences"
                        + " UNION ALL SELECT TOTAL(length(CAST(header AS BLOB))"
                        + " + COALESCE(length(CAST(accepted_actual AS BLOB)),0))"
                        + " FROM strength_executions"
                        + " UNION ALL SELECT TOTAL(length(CAST(payload AS BLOB)))"
                        + " FROM strength_execution_sets"
                        + " UNION ALL SELECT TOTAL(length(CAST(payload AS BLOB)))"
                        + " FROM strength_execution_events";
        query +=
                " UNION ALL SELECT TOTAL(length(CAST(payload AS BLOB))) FROM"
                        + " strength_association_decisions";
        for (String table :
                new String[] {
                    "strength_programme_revisions",
                    "strength_programme_blocks",
                    "strength_programme_sessions",
                    "strength_programme_decisions",
                    "strength_programme_drafts",
                    "strength_programme_draft_blocks",
                    "strength_custom_exercises"
                }) {
            String field =
                    table.equals("strength_programme_revisions")
                                    || table.equals("strength_programme_blocks")
                                    || table.equals("strength_programme_drafts")
                            ? "header"
                            : "payload";
            query += " UNION ALL SELECT TOTAL(length(CAST(" + field + " AS BLOB))) FROM " + table;
        }
        long lowerBound = 0;
        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                double bytes = cursor.getDouble(0);
                if (!Double.isFinite(bytes) || bytes < 0 || bytes > maximumBytes - lowerBound) {
                    throw new IllegalArgumentException(
                            "Your retained gym data exceeds the supported backup size."
                                    + " Existing local data was preserved; no backup was written.");
                }
                lowerBound += (long) bytes;
            }
        }
    }
}
