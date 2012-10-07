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
import android.util.Log;

import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.TraktException;
import com.uwetrottmann.movies.entities.MovieDetails;
import com.uwetrottmann.movies.util.Utils;
import com.uwetrottmann.tmdb.ServiceManager;

/**
 * Loads details for a movie from TMDb.
 */
public class TmdbMovieLoader extends GenericAsyncTaskLoader<MovieDetails> {

    private static final String TAG = "TmdbMoviesLoader";
    private int mTmdbId;

    public TmdbMovieLoader(Context context, int tmdbId) {
        super(context);
        mTmdbId = tmdbId;
    }

    @Override
    public MovieDetails loadInBackground() {
        ServiceManager manager = Utils.getTmdbServiceManager(getContext());
        MovieDetails details = new MovieDetails();
        try {
            details.movie = manager.moviesService().summary(mTmdbId).fire();
            details.trailers = manager.moviesService().trailers(mTmdbId).fire();
            return details;
        } catch (TraktException e) {
            Log.w(TAG, e);
        } catch (ApiException e) {
            Log.w(TAG, e);
        }

        return null;
    }
}
