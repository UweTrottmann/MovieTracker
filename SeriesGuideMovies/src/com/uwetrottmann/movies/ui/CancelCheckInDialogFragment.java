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

package com.uwetrottmann.movies.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Response;
import com.uwetrottmann.movies.R;
import com.uwetrottmann.movies.entities.TraktStatus;
import com.uwetrottmann.movies.util.TraktTask;
import com.uwetrottmann.movies.util.Utils;

public class CancelCheckInDialogFragment extends DialogFragment {

    private static final String TAG = "CancelCheckInDialogFragment";

    private int mWait;

    public static CancelCheckInDialogFragment newInstance(Bundle traktData, int wait) {
        CancelCheckInDialogFragment f = new CancelCheckInDialogFragment();
        f.setArguments(traktData);
        f.mWait = wait;
        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity().getApplicationContext();
        final FragmentManager fm = getFragmentManager();
        final Bundle args = getArguments();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage(context.getString(R.string.traktcheckin_inprogress,
                DateUtils.formatElapsedTime(mWait)));

        builder.setPositiveButton(R.string.traktcheckin_cancel, new OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                FragmentTransaction ft = fm.beginTransaction();
                Fragment prev = fm.findFragmentByTag("progress-dialog");
                if (prev != null) {
                    ft.remove(prev);
                }
                ProgressDialog newFragment = ProgressDialog.newInstance();
                newFragment.show(ft, "progress-dialog");

                AsyncTask<String, Void, Response> cancelCheckinTask = new AsyncTask<String, Void, Response>() {

                    @Override
                    protected Response doInBackground(String... params) {

                        ServiceManager manager;
                        try {
                            manager = Utils.getServiceManagerWithAuth(context, false);
                        } catch (Exception e) {
                            // password could not be decrypted
                            Response r = new Response();
                            r.status = TraktStatus.FAILURE;
                            r.error = context.getString(R.string.trakt_generalerror);
                            return r;
                        }

                        Response response;
                        try {
                            response = manager.movieService().cancelCheckin().fire();
                        } catch (TraktException e) {
                            Log.w(TAG, e);
                            Response r = new Response();
                            r.status = TraktStatus.FAILURE;
                            r.error = e.getMessage();
                            return r;
                        } catch (ApiException e) {
                            Log.w(TAG, e);
                            Response r = new Response();
                            r.status = TraktStatus.FAILURE;
                            r.error = e.getMessage();
                            return r;
                        }
                        return response;
                    }

                    @Override
                    protected void onPostExecute(Response r) {
                        if (r.status.equalsIgnoreCase(TraktStatus.SUCCESS)) {
                            // all good
                            Toast.makeText(context,
                                    r.message + " " + context.getString(R.string.ontrakt),
                                    Toast.LENGTH_SHORT).show();

                            // relaunch the trakt task which called us to
                            // try the check in again
                            new TraktTask(context, fm, args, null).execute();
                        } else if (r.status.equalsIgnoreCase(TraktStatus.FAILURE)) {
                            // well, something went wrong
                            Toast.makeText(
                                    context,
                                    context.getString(R.string.trakt_generalerror) + ": " + r.error,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                };

                cancelCheckinTask.execute();
            }
        });
        builder.setNegativeButton(R.string.traktcheckin_wait, null);

        return builder.create();
    }
}
