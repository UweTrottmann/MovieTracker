
package com.uwetrottmann.movies.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class MoviesProvider extends ContentProvider {
    private DatabaseHelper mOpenHelper;

    private static final String DATABASE_NAME = "movies.db";

    public static final int DATABASE_VERSION = 1;

    // Holds the database object
    private SQLiteDatabase db;

    public boolean onCreate() {

        /*
         * Creates a new helper object. This method always returns quickly.
         * Notice that the database itself isn't created or opened until
         * SQLiteOpenHelper.getWritableDatabase is called
         */
        mOpenHelper = new DatabaseHelper(getContext());

        return true;
    }

    /**
     * This class helps open, create, and upgrade the database file. Set to
     * package visibility for testing purposes.
     */
    static class DatabaseHelper extends SQLiteOpenHelper {

        private static final String TAG = "DatabaseHelper";

        DatabaseHelper(Context context) {
            // calls the super constructor, requesting the default cursor
            // factory.
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // TODO
        }

        /**
         * Demonstrates that the provider must consider what happens when the
         * underlying datastore is changed. In this sample, the database is
         * upgraded the database by destroying the existing data. A real
         * application should upgrade the database in place.
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            // Logs that the database is being upgraded
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                    + ", which will destroy all old data");

            // Kills the table and existing data
            // TODO
            db.execSQL("DROP TABLE IF EXISTS notes");

            // Recreates the database with a new version
            onCreate(db);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }
}
