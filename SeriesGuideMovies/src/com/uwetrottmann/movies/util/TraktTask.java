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
import com.jakewharton.trakt.entities.Response;
import com.jakewharton.trakt.enumerations.Rating;
import com.jakewharton.trakt.services.MovieService.CheckinBuilder;
import com.uwetrottmann.movies.R;
import com.uwetrottmann.movies.entities.TraktStatus;
import com.uwetrottmann.movies.ui.CancelCheckInDialogFragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Toast;

public class TraktTask extends AsyncTask<Void, Void, Response> {

    private static final String TAG = "TraktTask";

    private Bundle mArgs;

    private final Context mContext;

    private final FragmentManager mFm;

    private TraktAction mAction;

    private OnTraktActionCompleteListener mListener;

    public interface OnTraktActionCompleteListener {
        public void onTraktActionComplete(boolean wasSuccessfull);
    }

    public interface ShareItems {

        String SHARESTRING = "sharestring";

        String IMDBID = "imdbid";

        String RATING = "rating";

        String TRAKTACTION = "traktaction";

        String ISSPOILER = "isspoiler";
    }

    public enum TraktAction {
        SEEN_MOVIE(0), RATE_MOVIE(1), CHECKIN_MOVIE(2), SHOUT(3);

        final int index;

        private TraktAction(int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }

    /**
     * Initial constructor. Call <b>one</b> of the setup-methods, like
     * {@code shout(tvdbid, shout, isSpoiler)}, afterwards.
     * 
     * @param context
     * @param fm
     * @param listener
     */
    public TraktTask(Context context, FragmentManager fm, OnTraktActionCompleteListener listener) {
        mContext = context;
        mFm = fm;
        mListener = listener;
        mArgs = new Bundle();
    }

    /**
     * Fast constructor, allows passing of an already pre-built {@code args}
     * {@link Bundle}.
     * 
     * @param context
     * @param manager
     * @param args
     * @param listener
     */
    public TraktTask(Context context, FragmentManager manager, Bundle args,
            OnTraktActionCompleteListener listener) {
        this(context, manager, listener);
        mArgs = args;
    }

    /**
     * Check into a movie. Optionally provide a checkin message.
     * 
     * @param imdbid
     * @param message
     * @return TraktTask
     */
    public TraktTask checkin(String imdbid, String message) {
        mArgs.putInt(ShareItems.TRAKTACTION, TraktAction.CHECKIN_MOVIE.index);
        mArgs.putString(ShareItems.IMDBID, imdbid);
        mArgs.putString(ShareItems.SHARESTRING, message);
        return this;
    }

    /**
     * Rate a movie.
     * 
     * @param imdbid
     * @param rating
     * @return TraktTask
     */
    public TraktTask rateMovie(String imdbid, Rating rating) {
        mArgs.putInt(ShareItems.TRAKTACTION, TraktAction.RATE_MOVIE.index);
        mArgs.putString(ShareItems.IMDBID, imdbid);
        mArgs.putString(ShareItems.RATING, rating.toString());
        return this;
    }

    /**
     * Post a shout about a movie.
     * 
     * @param imdbid
     * @param shout
     * @param isSpoiler
     * @return TraktTask
     */
    public TraktTask shout(String imdbid, String shout, boolean isSpoiler) {
        mArgs.putInt(ShareItems.TRAKTACTION, TraktAction.SHOUT.index);
        mArgs.putString(ShareItems.IMDBID, imdbid);
        mArgs.putString(ShareItems.SHARESTRING, shout);
        mArgs.putBoolean(ShareItems.ISSPOILER, isSpoiler);
        return this;
    }

    @Override
    protected Response doInBackground(Void... params) {
        // we need this value in onPostExecute, so get it already here
        mAction = TraktAction.values()[mArgs.getInt(ShareItems.TRAKTACTION)];

        // check for network connection
        if (!Utils.isNetworkConnected(mContext)) {
            Response r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.offline);
            return r;
        }

        // check for valid credentials
        if (!Utils.isTraktCredentialsValid(mContext)) {
            // return null so a credentials dialog is displayed
            // it will call us again with valid credentials
            return null;
        }

        // get an authenticated trakt-java ServiceManager
        ServiceManager manager;
        try {
            manager = Utils.getServiceManagerWithAuth(mContext, false);
        } catch (Exception e) {
            // password could not be decrypted
            Response r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.trakt_generalerror);
            return r;
        }

        // last chance to abort
        if (isCancelled()) {
            return null;
        }

        // get values used by all actions
        final String imdbid = mArgs.getString(ShareItems.IMDBID);

        try {
            Response r = null;

            switch (mAction) {
                case CHECKIN_MOVIE: {
                    final String message = mArgs.getString(ShareItems.SHARESTRING);

                    final CheckinBuilder checkinBuilder = manager.movieService().checkin(imdbid);
                    if (message != null && message.length() != 0) {
                        checkinBuilder.message(message);
                    }
                    r = checkinBuilder.fire();

                    break;
                }
                case RATE_MOVIE: {
                    final Rating rating = Rating.fromValue(mArgs.getString(ShareItems.RATING));
                    r = manager.rateService().movie(imdbid).rating(rating).fire();
                    break;
                }
                case SHOUT: {
                    final String shout = mArgs.getString(ShareItems.SHARESTRING);
                    final boolean isSpoiler = mArgs.getBoolean(ShareItems.ISSPOILER);
                    r = manager.shoutService().movie(imdbid).shout(shout).spoiler(isSpoiler).fire();
                }
            }

            return r;
        } catch (TraktException te) {
            Log.w(TAG, te);
            Response r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.trakt_generalerror);
            return r;
        } catch (ApiException e) {
            Log.w(TAG, e);
            Response r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.trakt_generalerror);
            return r;
        }
    }

    @Override
    protected void onPostExecute(Response r) {
        // dismiss a potential progress dialog
        if (mAction == TraktAction.CHECKIN_MOVIE) {
            Fragment prev = mFm.findFragmentByTag("progress-dialog");
            if (prev != null) {
                FragmentTransaction ft = mFm.beginTransaction();
                ft.remove(prev);
                ft.commit();
            }
        }

        if (r != null) {
            if (r.status.equalsIgnoreCase(TraktStatus.SUCCESS)) {

                // all good
                Toast.makeText(mContext,
                        mContext.getString(R.string.trakt_success) + ": " + r.message,
                        Toast.LENGTH_SHORT).show();

                if (mListener != null) {
                    mListener.onTraktActionComplete(true);
                }

            } else if (r.status.equalsIgnoreCase(TraktStatus.FAILURE)) {
                if (r.wait != 0) {

                    // looks like a check in is in progress
                    CancelCheckInDialogFragment newFragment = CancelCheckInDialogFragment
                            .newInstance(mArgs, r.wait);
                    FragmentTransaction ft = mFm.beginTransaction();
                    newFragment.show(ft, "cancel-checkin-dialog");

                } else {

                    // well, something went wrong
                    Toast.makeText(mContext, r.error, Toast.LENGTH_LONG).show();

                }

                if (mListener != null) {
                    mListener.onTraktActionComplete(false);
                }
            }
        }
    }
}
