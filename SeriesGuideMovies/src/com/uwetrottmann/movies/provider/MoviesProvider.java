/*
 * Copyright 2012 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.uwetrottmann.movies.provider;

import com.uwetrottmann.movies.provider.MoviesContract.Movies;
import com.uwetrottmann.movies.util.SelectionBuilder;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

public class MoviesProvider extends ContentProvider {

    private static final String TAG = "MoviesProvider";

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int MOVIES = 100;

    private static final int MOVIE_ID = 101;

    private static final boolean LOGV = false;

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri}
     * variations supported by this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = MoviesContract.CONTENT_AUTHORITY;

        // Movies
        matcher.addURI(authority, MoviesContract.PATH_MOVIES, MOVIES);
        matcher.addURI(authority, MoviesContract.PATH_MOVIES + "/*", MOVIE_ID);

        return matcher;
    }

    private DatabaseHelper mOpenHelper;

    public interface Tables {
        String MOVIES = "movies";
    }

    public boolean onCreate() {
        /*
         * Creates a new helper object. This method always returns quickly.
         * Notice that the database itself isn't created or opened until
         * SQLiteOpenHelper.getWritableDatabase is called
         */
        mOpenHelper = new DatabaseHelper(getContext());

        return true;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case MOVIES:
                return Movies.CONTENT_TYPE;
            case MOVIE_ID:
                return Movies.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (LOGV) {
            Log.v(TAG, "insert(uri=" + uri + ", values=" + values.toString() + ")");
        }
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case MOVIES: {
                db.insertOrThrow(Tables.MOVIES, null, values);
                getContext().getContentResolver().notifyChange(uri, null);
                return Movies.buildMovieUri(values.getAsInteger(Movies._ID));
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (LOGV) {
            Log.v(TAG, "query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");
        }
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case MOVIES:
            case MOVIE_ID: {
                // Most cases are handled with simple SelectionBuilder
                final SelectionBuilder builder = buildExpandedSelection(uri, match);
                Cursor query = builder.where(selection, selectionArgs).query(db, projection,
                        sortOrder);
                query.setNotificationUri(getContext().getContentResolver(), uri);
                return query;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (LOGV) {
            Log.v(TAG, "update(uri=" + uri + ", values=" + values.toString() + ")");
        }
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).update(db, values);
        getContext().getContentResolver().notifyChange(uri, null);
        return retVal;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (LOGV) {
            Log.v(TAG, "delete(uri=" + uri + ")");
        }
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs).delete(db);
        getContext().getContentResolver().notifyChange(uri, null);
        return retVal;
    }

    /**
     * Apply the given set of {@link ContentProviderOperation}, executing inside
     * a {@link SQLiteDatabase} transaction. All changes will be rolled back if
     * any single one fails.
     */
    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                results[i] = operations.get(i).apply(this, results, i);
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Build a simple {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually enough to support {@link #insert},
     * {@link #update}, and {@link #delete} operations.
     */
    private SelectionBuilder buildSimpleSelection(Uri uri) {
        final SelectionBuilder builder = new SelectionBuilder();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case MOVIES: {
                return builder.table(Tables.MOVIES);
            }
            case MOVIE_ID: {
                final String id = Movies.getId(uri);
                return builder.table(Tables.MOVIES).where(Movies._ID + "=?", id);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    /**
     * Build an advanced {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually only used by {@link #query}, since it
     * performs table joins useful for {@link Cursor} data.
     */
    private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case MOVIES: {
                return builder.table(Tables.MOVIES);
            }
            case MOVIE_ID: {
                final String id = Movies.getId(uri);
                return builder.table(Tables.MOVIES).where(Movies._ID + "=?", id);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    /**
     * This class helps open, create, and upgrade the database file. Set to
     * package visibility for testing purposes.
     */
    static class DatabaseHelper extends SQLiteOpenHelper {

        private static final int DBVER_ORIGINAL = 1;

        private static final String DATABASE_NAME = "movies.db";

        private static final String TAG = "DatabaseHelper";

        public static final int DATABASE_VERSION = DBVER_ORIGINAL;

        private static final String CREATE_MOVIES_TABLE = "CREATE TABLE " + Tables.MOVIES + " ("
                + Movies._ID + " INTEGER PRIMARY KEY,"

                + Movies.TITLE + " TEXT NOT NULL,"

                + Movies.YEAR + " TEXT,"

                + Movies.RELEASED + " INTEGER,"

                + Movies.URL + " TEXT,"

                + Movies.TRAILER + " TEXT,"

                + Movies.RUNTIME + " INTEGER,"

                + Movies.TAGLINE + " TEXT,"

                + Movies.OVERVIEW + " TEXT,"

                + Movies.CERTIFICATION + " TEXT,"

                + Movies.IMDBID + " TEXT,"

                + Movies.LASTUPDATED + " INTEGER,"

                + Movies.POSTER + " TEXT,"

                + Movies.FANART + " TEXT,"

                + Movies.GENRES + " BLOB,"

                + Movies.RATINGS_PERCENTAGE + " INTEGER,"

                + Movies.RATINGS_VOTES + " INTEGER,"

                + Movies.WATCHED + " INTEGER,"

                + Movies.INWATCHLIST + " INTEGER,"

                + Movies.INCOLLECTION + " INTEGER"

                + ");";

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_MOVIES_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

            // run necessary upgrades
            int version = oldVersion;
            switch (version) {
            // case 1:
            // upgradeToTwo(db);
            // version = 2;
            }

            // drop all tables if version is not right
            Log.d(TAG, "after upgrade logic, at version " + version);
            if (version != DATABASE_VERSION) {
                Log.w(TAG, "Database has incompatible version, starting from scratch");
                db.execSQL("DROP TABLE IF EXISTS " + Tables.MOVIES);

                onCreate(db);
            }
        }
    }

}
