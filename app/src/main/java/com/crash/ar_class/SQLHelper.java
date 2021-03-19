package com.crash.ar_class;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.IOException;
import java.util.ArrayList;
class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "salaryDatabase3";
    public static final String CONTACTS_TABLE_NAME = "SalaryDetails";
    public DatabaseHelper(Context context) {
        super(context,DATABASE_NAME,null,1);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(
                    "create table "+ CONTACTS_TABLE_NAME + "(id INTEGER PRIMARY KEY, name text,salary text )"
            );
        } catch (SQLiteException e) {
            try {
                throw new IOException(e);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + CONTACTS_TABLE_NAME);
        onCreate(db);
    }

    public ArrayList getSchoolClass(String cabinet) {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<String> array_list = new ArrayList<String>();
        Cursor res = db.rawQuery( "select * from " + CONTACTS_TABLE_NAME, null );
        res.moveToFirst();
        array_list.add(res.getString(res.getColumnIndex("name")));

        return array_list;
    }
}