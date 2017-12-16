package com.yakovlaptev.demon.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import static com.yakovlaptev.demon.SplashScreen.dbHelper;

public class DBManager {

    private static final String LOG_TAG = "DBManager";

    public static void dbInsert(String[] where, String[] values) {
        ContentValues cv = new ContentValues();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Log.d(LOG_TAG, "--- Insert in Profile: ---");

        for (int i = 0; i < where.length; i++) {
            cv.put(where[i], values[i]);
        }

        String strFilter = "_id=" + 1;
        db.update("profile", cv, strFilter, null);
        dbHelper.close();

        Log.d(LOG_TAG, "--- " +where[0] + "-" +values[0] + ": ---");
        db.close();
    }

}
