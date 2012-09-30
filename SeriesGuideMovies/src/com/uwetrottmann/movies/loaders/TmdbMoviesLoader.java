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

package com.uwetrottmann.movies.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.TraktException;
import com.uwetrottmann.movies.util.Utils;
import com.uwetrottmann.tmdb.ServiceManager;
import com.uwetrottmann.tmdb.entities.Movie;
import com.uwetrottmann.tmdb.entities.ResultsPage;

import java.util.List;

/**
 * Loads a list of movies from TMDb.
 */
public class TmdbMoviesLoader extends AsyncTaskLoader<List<Movie>> {

    private static final String TAG = "TmdbMoviesLoader";

    private List<Movie> mData;

    public TmdbMoviesLoader(Context context) {
        super(context);
    }

    @Override
    public List<Movie> loadInBackground() {
        ServiceManager manager = Utils.getTmdbServiceManager(getContext());

        try {
            ResultsPage page = manager.moviesService().nowPlaying().fire();
            if (page != null && page.results != null) {
                return page.results;
            }
        } catch (TraktException e) {
            Log.w(TAG, e);
        } catch (ApiException e) {
            Log.w(TAG, e);
        }

        return null;
    }

    /**
     * Called when there is new data to deliver to the client. The super class
     * will take care of delivering it; the implementation here just adds a
     * little more logic.
     */
    @Override
    public void deliverResult(List<Movie> data) {
        if (isReset()) {
            // An async query came in while the loader is stopped. We
            // don't need the result.
            if (data != null) {
                onReleaseResources(data);
            }
        }
        List<Movie> oldData = data;
        mData = data;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(data);
        }

        if (oldData != null) {
            onReleaseResources(oldData);
        }
    }

    @Override
    protected void onStartLoading() {
        if (mData != null) {
            deliverResult(mData);
        } else {
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(List<Movie> data) {
        super.onCanceled(data);

        onReleaseResources(data);
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release resources
        if (mData != null) {
            onReleaseResources(mData);
            mData = null;
        }
    }

    /**
     * Helper function to take care of releasing resources associated with an
     * actively loaded data set.
     */
    protected void onReleaseResources(List<Movie> data) {
        // For a simple List<> there is nothing to do. For something
        // like a Cursor, we would close it here.
    }
}
