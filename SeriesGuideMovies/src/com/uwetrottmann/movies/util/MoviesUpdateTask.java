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

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

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
        try {
            // if possible get an auth data so we can return additional
            // information (seen, checkins, etc.)
            ServiceManager serviceManager;
            if (Utils.isTraktCredentialsValid(getContext())) {
                serviceManager = Utils.getServiceManagerWithAuth(getContext(), false);
            } else {
                return INVALID_CREDENTIALS;
            }

            // there will always be a username as the fragment using
            // this only loads with valid credentials
            List<Movie> movies = serviceManager.userService()
                    .watchlistMovies(Utils.getTraktUsername(getContext())).fire();

            for (Movie movie : movies) {
                // TODO
            }

            return SUCCESS;
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
    }

    private Context getContext() {
        return mContext;
    }

}
