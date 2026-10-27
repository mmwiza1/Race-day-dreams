package org.racedaydreams.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Looper;
import java.util.Objects;
import org.json.JSONException;
import org.json.JSONObject;

/** Shared connection/SQL primitives only; repositories own their feature rules and queries. */
abstract class SqliteStoreAccess {
    protected final ProfileDatabase helper;

    protected SqliteStoreAccess(Context context) {
        helper = ProfileDatabase.get(Objects.requireNonNull(context));
    }

    protected final SQLiteDatabase read() {
        requireIo();
        return helper.getReadableDatabase();
    }

    protected final SQLiteDatabase write() {
        requireIo();
        return helper.getWritableDatabase();
    }

    protected static void requireIo() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Local storage requires an IO thread.");
        }
    }

    protected static String required(String value, String field) {
        if (value == null || value.trim().isEmpty() || value.length() > 256) {
            throw new IllegalArgumentException("Invalid " + field);
        }
        return value;
    }

    protected static ContentValues values(String key, String value) {
        ContentValues values = new ContentValues();
        values.put(key, value);
        return values;
    }

    protected static void replace(SQLiteDatabase db, String table, ContentValues values) {
        if (db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE) == -1) {
            throw new SQLiteException("Local data could not be saved.");
        }
    }
    protected static void putOpaque(SQLiteDatabase db,String table,JSONObject object){try{ContentValues v=values("id",FeatureJson.required(object,"id"));v.put("payload",object.toString());if(table.equals("assessment_results"))v.put("activity_id",FeatureJson.text(object,"activityId"));if(table.equals("plan_adjustments")){v.put("plan_id",FeatureJson.text(object,"planId"));v.put("user_decision",FeatureJson.text(object,"userDecision"));}replace(db,table,v);}catch(JSONException e){throw FeatureJson.invalid(e);}}
}
