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
import com.uwetrottmann.movies.ui.MovieDetailsFragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraktMoviesLoader extends AsyncTaskLoader<List<Movie>> {

    public interface InitBundle {
        String CATEGORY = "category";
    }

    private static final String TAG = "TraktMoviesLoader";

    private List<Movie> mData;

    private Bundle mArgs;

    public TraktMoviesLoader(Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Override
    public List<Movie> loadInBackground() {
        TraktCategory category = TraktCategory.fromValue(mArgs.getInt(InitBundle.CATEGORY));
        try {
            // if possible get an auth data so we can return additional
            // information (seen, checkins, etc.)
            ServiceManager serviceManager;
            if (Utils.isTraktCredentialsValid(getContext())) {
                serviceManager = Utils.getServiceManagerWithAuth(getContext(), false);
            } else {
                serviceManager = Utils.getServiceManager(getContext());
            }

            switch (category) {
                case TRENDING: {
                    return serviceManager.movieService().trending().fire();
                }
                case WATCHLIST: {
                    // there will always be a username as the fragment using
                    // this only loads with valid credentials
                    return serviceManager.userService()
                            .watchlistMovies(Utils.getTraktUsername(getContext())).fire();
                }
                case SUMMARY: {
                    // will just return a single item
                    Movie movie = serviceManager.movieService()
                            .summary(mArgs.getString(MovieDetailsFragment.InitBundle.IMDBID))
                            .fire();
                    ArrayList<Movie> list = new ArrayList<Movie>();
                    list.add(movie);
                    return list;
                }
            }
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

    public enum TraktCategory {
        TRENDING(0), SUMMARY(1), WATCHLIST(2);

        private final int index;

        private TraktCategory(int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }

        private static final Map<Integer, TraktCategory> INT_MAPPING = new HashMap<Integer, TraktCategory>();

        static {
            for (TraktCategory via : TraktCategory.values()) {
                INT_MAPPING.put(via.index(), via);
            }
        }

        public static TraktCategory fromValue(int value) {
            return INT_MAPPING.get(value);
        }
    }
}
