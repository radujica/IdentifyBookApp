package com.example.dimebag.identifybookapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Radu Jica
 * @version 1.0
 *
 * Database handler implementing database access and creation and methods
 * that allow querying it.
 */

//TODO: simpleCursorAdapter; loader; transaction when display ready

public class DisplayRecordDBHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "books";

    private static final String TABLE_NAME_DISPLAYS = "display_record";

    static final String COLUMN_NAME_ID = "_id";
    static final String COLUMN_NAME_DISPLAY_NAME = "display_name";
    static final String COLUMN_NAME_BOOK_ISBN = "book_ISBN";
    static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    static final String COLUMN_NAME_ANDROID_ID = "android_id";

    public DisplayRecordDBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_NAME_DISPLAYS + "("
                + COLUMN_NAME_ID + " INTEGER PRIMARY KEY," + COLUMN_NAME_DISPLAY_NAME + " TEXT,"
                + COLUMN_NAME_BOOK_ISBN + " TEXT," + COLUMN_NAME_TIMESTAMP + " TEXT,"
                + COLUMN_NAME_ANDROID_ID + " TEXT" + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_DISPLAYS);
        // Create tables again
        onCreate(db);
    }

    // Adding new book entry
    public void addDisplayRecord(DisplayRecord displayRecord) {
        SQLiteDatabase db = this.getWritableDatabase();

        //add only if the param record is a new record
        if (displayRecord.getId() >= getDisplayRecordCount()) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME_DISPLAY_NAME, displayRecord.getDisplayName());
            values.put(COLUMN_NAME_BOOK_ISBN, displayRecord.getBookISBN());
            values.put(COLUMN_NAME_TIMESTAMP, displayRecord.getTimestamp());
            values.put(COLUMN_NAME_ANDROID_ID, displayRecord.getAndroidId());

            db.insert(TABLE_NAME_DISPLAYS, null, values);
            db.close();
        }
    }

    // Getting a single displayRecord
    public DisplayRecord getDisplayRecord(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        //currently using null instead of all the column names; replace back if it does not work
        Cursor cursor = db.query(TABLE_NAME_DISPLAYS, null, COLUMN_NAME_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);

        DisplayRecord displayRecord;
        if (cursor != null) {
            cursor.moveToFirst();
            displayRecord = new DisplayRecord(Integer.parseInt(cursor.getString(0)),
                    cursor.getString(1), cursor.getString(2),cursor.getString(3),cursor.getString(4));
            cursor.close();
            return displayRecord;
        }
        return null;
    }

    // Getting display records for a specific display name; only book_ISBNs
    public List<String> getBookISBNs(String displayName) {
        SQLiteDatabase db = this.getReadableDatabase();

        List<String> bookISBNs = new ArrayList<>();

        Cursor cursor = db.query(TABLE_NAME_DISPLAYS, new String[] { COLUMN_NAME_BOOK_ISBN },
                COLUMN_NAME_DISPLAY_NAME + "=?", new String[] { displayName }, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                bookISBNs.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return bookISBNs;
    }

    // Getting display records for a specific display name
    public List<String> getDisplayNames() {
        SQLiteDatabase db = this.getReadableDatabase();

        List<String> displayNames = new ArrayList<>();

        Cursor cursor = db.query(true, TABLE_NAME_DISPLAYS, new String[] { COLUMN_NAME_DISPLAY_NAME },
                null, null, COLUMN_NAME_DISPLAY_NAME, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                displayNames.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return displayNames;
    }

    // Getting All Display Records
    public List<DisplayRecord> getAllDisplayRecords() {
        List<DisplayRecord> displayRecordsList = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_NAME_DISPLAYS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                DisplayRecord displayRecord = new DisplayRecord();
                displayRecord.setId(Integer.parseInt(cursor.getString(0)));
                displayRecord.setDisplayName(cursor.getString(1));
                displayRecord.setBookISBN(cursor.getString(2));
                displayRecord.setTimestamp(cursor.getString(3));
                displayRecord.setAndroidId(cursor.getString(4));

                displayRecordsList.add(displayRecord);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return displayRecordsList;
    }

    // Getting Display Record Count
    public int getDisplayRecordCount() {
        String countQuery = "SELECT  * FROM " + TABLE_NAME_DISPLAYS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    // Updating single Display Record
    public int updateDisplayRecord(DisplayRecord displayRecord) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_DISPLAY_NAME, displayRecord.getDisplayName());
        values.put(COLUMN_NAME_BOOK_ISBN, displayRecord.getBookISBN());
        values.put(COLUMN_NAME_TIMESTAMP, displayRecord.getTimestamp());
        values.put(COLUMN_NAME_ANDROID_ID, displayRecord.getAndroidId());

        return db.update(TABLE_NAME_DISPLAYS, values, COLUMN_NAME_ID + " = ?",
                new String[] { String.valueOf(displayRecord.getId()) });
    }

    // Deleting single Display Record
    public void deleteDisplayRecord(DisplayRecord displayRecord) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME_DISPLAYS, COLUMN_NAME_ID + " = ?",
                new String[] { String.valueOf(displayRecord.getId()) });
        db.close();
    }
}
