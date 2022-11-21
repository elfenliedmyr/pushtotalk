package com.auxilium.auxiliummobilesolutions.dbhelpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class DBContactsHelper extends SQLiteOpenHelper {

    private static final String TAG = "DBContactsHelper";
    private static final String TABLE_NAME = "contacts_table";
    private static final String COL1 = "ID";
    private static final String COL2 = "NAME";
    private static final String COL3 = "PHONE";
    private static final String COL4 = "EMAIL";

    public DBContactsHelper(@Nullable Context context) {
        super(context, TABLE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" + COL1 + " INTEGER PRIMARY KEY, " +
                COL2 + " TEXT, " + COL3 + " TEXT, " + COL4 + " TEXT )";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean addData(Integer id, String name, String phone, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL1, id);
        cv.put(COL2, name);
        cv.put(COL3, phone);
        cv.put(COL4, email);

        long result = db.insert(TABLE_NAME, null, cv);
        return (result >= 0);
    }

    public Cursor getData() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME;
        return db.rawQuery(query, null);
    }

    public Cursor getItem(Integer id) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME +
                " WHERE " + COL1 + " = " + id;
        return db.rawQuery(query, null);
    }

    public Cursor getItemsIdWithPhone(String name, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + COL1 + " FROM " + TABLE_NAME +
                " WHERE " + COL2 + " = '" + name +
                "' AND (" + COL3 + " LIKE '%" + phone.replaceAll("[^0-9,\\s+]", "") +
                "%' OR " + COL3 + " LIKE '%" + phone.replaceAll("[^0-9,\\s+]", "").substring(1) + "%')";
        return db.rawQuery(query, null);
    }

    public Cursor getItemsIdWithEmail(String name, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + COL1 + " FROM " + TABLE_NAME +
                " WHERE " + COL2 + " = '" + name + "' AND " + COL4 + " = '" + email + "'";
        return db.rawQuery(query, null);
    }

    public void deleteAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME;
        db.execSQL(query);
    }
}
