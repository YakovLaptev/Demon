package com.yakovlaptev.demon.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import static com.yakovlaptev.demon.SplashScreen.dbHelper;

public class DBManager {

    private static final String LOG_TAG = "DBManager";

    public static void dbInsertAvatar(byte[] value) {
        ContentValues cv = new ContentValues();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Log.d(LOG_TAG, "--- Insert in Profile: ---");

        cv.put("avatar", value);

        String strFilter = "_id=" + 1;
        db.update("profile", cv, strFilter, null);
        dbHelper.close();

        db.close();
    }

}
