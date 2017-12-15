package com.yakovlaptev.demon;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class SplashScreen extends Activity {

    private static String LOG_TAG = "Logs";
    public static DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DBHelper(this);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent;
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                Cursor cursor = db.query("profile", null, null, null, null, null, null);
                if (!cursor.moveToFirst()) {
                    Log.d(LOG_TAG, "--- not exist ---");
                    intent = new Intent(SplashScreen.this, Registration.class);
                } else {
                    Log.d(LOG_TAG, "---  exist ---");
                    intent = new Intent(SplashScreen.this, MainActivity.class);
                }
                cursor.close();
                startActivity(intent);
                finish();
            }
        }, 1800);
    }

    public static class DBHelper extends SQLiteOpenHelper {

        DBHelper(Context context) {
            super(context, "p2pChat", null, 1);
            Log.d(LOG_TAG, "---  onCreate ---");
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("create table if not exists profile (_id integer primary key autoincrement,name text,email text);");
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(LOG_TAG, "--- OnCreate database ---");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(LOG_TAG, "---  onUpgrade ---");
        }
    }

}