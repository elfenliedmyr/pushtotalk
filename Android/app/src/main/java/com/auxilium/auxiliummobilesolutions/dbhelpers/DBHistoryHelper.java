package com.auxilium.auxiliummobilesolutions.dbhelpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.auxilium.auxiliummobilesolutions.models.CallHistory;

public class DBHistoryHelper extends SQLiteOpenHelper {

    private static final String TAG = "DBHistoryHelper";
    private static final String TABLE_NAME = "call_history_table";
    private static final String COL1 = "ID";
    private static final String COL2 = "NAME";
    private static final String COL3 = "NUMBER";
    private static final String COL4 = "TIME";
    private static final String COL5 = "DURATION";
    private static final String COL6 = "TYPE";
    private static final String COL7 = "STATUS";

    public DBHistoryHelper(@Nullable Context context) {
        super(context, TABLE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" + COL1 + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL2 + " TEXT, " + COL3 + " TEXT, " + COL4 + " TEXT, " + COL5 + " TEXT, " + COL6 + " INTEGER, " +
                COL7 + " TEXT )";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean addData(String name, String number, String time, String duration, Integer type, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL2, name);
        cv.put(COL3, number);
        cv.put(COL4, time);
        cv.put(COL5, duration);
        cv.put(COL6, type);
        cv.put(COL7, status);

        long result = db.insert(TABLE_NAME, null, cv);
        return (result >= 0);
    }

    public void setStatus(CallHistory item, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + TABLE_NAME + " SET " + COL7 + " = '" + status + "' WHERE " +
                COL2 + " = '" + item.getContact_name() + "' AND " +
                COL3 + " = '" + item.getContact_number() + "' AND " +
                COL4 + " = '" + item.getCall_time() + "' AND " +
                COL5 + " = '" + item.getCall_duration() + "' AND " +
                COL6 + " = " + item.getCall_type();
        db.execSQL(query);
    }

    public Cursor getData() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COL4 + " DESC";
        return db.rawQuery(query, null);
    }

    public Cursor getItemsID(String name, String number, String time, Integer type) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + COL1 + " FROM " + TABLE_NAME +
            " WHERE " + COL2 + " = '" + name + "' AND " + COL3 + " = '" + number + "' AND " +
                COL4 + " = '" + time + "' AND " + COL6 + " = " + type;
        return db.rawQuery(query, null);
    }

    public void deleteAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME;
        db.execSQL(query);
    }
}
