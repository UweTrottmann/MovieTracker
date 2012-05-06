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

package com.uwetrottmann.movies.util;

import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Movie;
import com.uwetrottmann.movies.provider.MoviesContract;
import com.uwetrottmann.movies.provider.MoviesContract.Movies;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class MoviesUpdateTask extends AsyncTask<Void, Void, Integer> {

    private static final String TAG = "MoviesUpdateTask";

    private static final int SUCCESS = 1;

    private static final Integer INVALID_CREDENTIALS = -1;

    private Context mContext;

    public MoviesUpdateTask(Context context) {
        mContext = context;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        List<Movie> newWatchlist;
        try {

            // if possible get auth data so we can return additional
            // information (seen, checkins, etc.)
            ServiceManager serviceManager;
            if (Utils.isTraktCredentialsValid(getContext())) {
                serviceManager = Utils.getServiceManagerWithAuth(getContext(), false);
            } else {
                return INVALID_CREDENTIALS;
            }

            // there will always be a username as the fragment using
            // this only loads with valid credentials
            newWatchlist = serviceManager.userService()
                    .watchlistMovies(Utils.getTraktUsername(getContext())).fire();

        } catch (TraktException e) {
            Log.w(TAG, e);
            return null;
        } catch (ApiException e) {
            Log.w(TAG, e);
            return null;
        } catch (Exception e) {
            Log.w(TAG, e);
            return null;
        }

        // build a list of movies already in the database
        final Cursor oldWatchlist = getContext().getContentResolver().query(Movies.CONTENT_URI,
                new String[] {
                        Movies.IMDBID, Movies.LASTUPDATED
                }, null, null, null);
        HashMap<String, Long> oldWatchlistIds = Maps.newHashMap();
        while (oldWatchlist.moveToNext()) {
            oldWatchlistIds.put(oldWatchlist.getString(0), oldWatchlist.getLong(1));
        }
        oldWatchlist.close();

        // build db ops to update or add movies
        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        for (Movie movie : newWatchlist) {
            ContentValues values = new ContentValues();
            onBuildMovieValues(movie, values);

            ContentProviderOperation op = null;
            Long movieLastUpdated = oldWatchlistIds.remove(movie.imdbId);
            if (movieLastUpdated != null) {
                // update (only if there are changes
                if (movieLastUpdated < movie.lastUpdated.getTime()) {
                    op = ContentProviderOperation.newUpdate(Movies.CONTENT_URI)
                            .withSelection(Movies.IMDBID + "=?", new String[] {
                                movie.imdbId
                            }).withValues(values).build();
                }
            } else {
                // insert
                op = ContentProviderOperation.newInsert(Movies.CONTENT_URI).withValues(values)
                        .build();
            }

            if (op != null) {
                batch.add(op);
            }
        }

        // build db ops to remove movies that got deleted from the watchlist
        for (Entry<String, Long> movie : oldWatchlistIds.entrySet()) {
            ContentProviderOperation op = ContentProviderOperation.newDelete(Movies.CONTENT_URI)
                    .withSelection(Movies.IMDBID + "=?", new String[] {
                        movie.getKey()
                    }).build();
            batch.add(op);
        }

        // apply dp ops
        try {
            getContext().getContentResolver().applyBatch(MoviesContract.CONTENT_AUTHORITY, batch);
        } catch (RemoteException e) {
            // Failed binder transactions aren't recoverable
            Log.e(TAG, e.getMessage());
            throw new RuntimeException("Problem applying batch operation", e);
        } catch (OperationApplicationException e) {
            // Failures like constraint violation aren't recoverable
            Log.e(TAG, e.getMessage());
            throw new RuntimeException("Problem applying batch operation", e);
        }

        return SUCCESS;
    }

    @Override
    protected void onCancelled() {
        onFinishUp();
    }

    @Override
    protected void onPostExecute(Integer result) {
        onFinishUp();
    }

    private void onBuildMovieValues(Movie movie, ContentValues values) {
        // TODO support missing values
        values.put(Movies.TITLE, movie.title);
        values.put(Movies.YEAR, movie.year);
        values.put(Movies.RELEASED, movie.released.getTime());
        values.put(Movies.URL, movie.url);
        values.put(Movies.TRAILER, movie.trailer);
        values.put(Movies.RUNTIME, movie.runtime);
        values.put(Movies.TAGLINE, movie.tagline);
        values.put(Movies.OVERVIEW, movie.overview);
        values.put(Movies.CERTIFICATION, movie.certification);
        values.put(Movies.IMDBID, movie.imdbId);
        values.put(Movies.LASTUPDATED, movie.lastUpdated.getTime());
        values.put(Movies.POSTER, movie.images.poster);
        values.put(Movies.FANART, movie.images.fanart);
        // values.put(Movies.GENRES, movie.);
        values.put(Movies.RATINGS_PERCENTAGE, movie.ratings.percentage);
        values.put(Movies.RATINGS_VOTES, movie.ratings.votes);
        values.put(Movies.WATCHED, movie.watched);
        values.put(Movies.INWATCHLIST, movie.inWatchlist);
        values.put(Movies.INCOLLECTION, movie.inCollection);
    }

    private Context getContext() {
        return mContext;
    }

    private void onFinishUp() {
        mContext = null;
        TaskManager.getInstance(getContext()).onTaskCompleted();
    }

}
