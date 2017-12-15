package com.yakovlaptev.demon;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.yakovlaptev.R;

import static com.yakovlaptev.demon.SplashScreen.dbHelper;


public class Registration extends Activity {

    final String LOG_TAG = "Logs";
    EditText etName, etEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration);
        etName = (EditText) findViewById(R.id.userNameEditText);
        etEmail = (EditText) findViewById(R.id.userEmailEditText);
    }

    public void singUp(View view) {
        ContentValues cv = new ContentValues();
        String name = etName.getText().toString();
        String email = etEmail.getText().toString();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Log.d(LOG_TAG, "--- Insert in Profile: ---");

        cv.put("name", name);
        cv.put("email", email);

        long rowID = db.insert("profile", null, cv);
        Log.d(LOG_TAG, "row inserted, ID = " + rowID);
        Toast.makeText(getApplicationContext(), "Row inserted", Toast.LENGTH_SHORT).show();
        dbHelper.close();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

}
