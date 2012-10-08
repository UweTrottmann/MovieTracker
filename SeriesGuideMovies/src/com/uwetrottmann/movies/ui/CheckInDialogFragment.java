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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.movies.R;
import com.uwetrottmann.movies.getglueapi.GetGlue;
import com.uwetrottmann.movies.getglueapi.GetGlue.CheckInTask;
import com.uwetrottmann.movies.getglueapi.GetGlueAuthActivity;
import com.uwetrottmann.movies.util.TraktCredentialsDialogFragment;
import com.uwetrottmann.movies.util.TraktTask;
import com.uwetrottmann.movies.util.Utils;

public class CheckInDialogFragment extends SherlockDialogFragment {

    interface InitBundle {
        String IMDBID = "imdbid";

        String TITLE = "title";
    }

    public static CheckInDialogFragment newInstance(String imdbId, String title) {
        CheckInDialogFragment f = new CheckInDialogFragment();

        Bundle args = new Bundle();
        args.putString(InitBundle.IMDBID, imdbId);
        args.putString(InitBundle.TITLE, title);
        f.setArguments(args);

        return f;
    }

    protected boolean mGetGlueChecked;

    protected boolean mTraktChecked;

    private CompoundButton mToggleTraktButton;

    private CompoundButton mToggleGetGlueButton;

    private EditText mMessageBox;

    private View mCheckinButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // hide title
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.checkin_dialog, null);
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getSherlockActivity());

        // some required values
        final String imdbId = getArguments().getString(InitBundle.IMDBID);
        final String title = getArguments().getString(InitBundle.TITLE);

        // get share service enabled settings
        mGetGlueChecked = prefs.getBoolean(AppPreferences.KEY_SHAREWITHGETGLUE, false);
        mTraktChecked = prefs.getBoolean(AppPreferences.KEY_SHAREWITHTRAKT, false);

        // Message box
        mMessageBox = (EditText) layout.findViewById(R.id.message);

        // Paste title button
        layout.findViewById(R.id.paste).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int start = mMessageBox.getSelectionStart();
                int end = mMessageBox.getSelectionEnd();
                mMessageBox.getText().replace(Math.min(start, end), Math.max(start, end), title, 0,
                        title.length());
            }
        });

        // Clear button
        layout.findViewById(R.id.textViewClear).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mMessageBox.setText(null);
            }
        });

        // GetGlue toggle
        mToggleGetGlueButton = (CompoundButton) layout.findViewById(R.id.toggleGetGlue);
        mToggleGetGlueButton.setChecked(mGetGlueChecked);
        mToggleGetGlueButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!GetGlue.isAuthenticated(prefs)) {
                        if (!AndroidUtils.isNetworkConnected(getActivity())) {
                            Toast.makeText(getActivity(), R.string.offline, Toast.LENGTH_LONG)
                                    .show();
                            buttonView.setChecked(false);
                            return;
                        } else {
                            // authenticate already here
                            Intent i = new Intent(getSherlockActivity(),
                                    GetGlueAuthActivity.class);
                            startActivity(i);
                        }
                    }
                }

                mGetGlueChecked = isChecked;
                prefs.edit().putBoolean(AppPreferences.KEY_SHAREWITHGETGLUE, isChecked)
                        .commit();
                updateCheckInButtonState();
            }
        });

        // trakt toggle
        mToggleTraktButton = (CompoundButton) layout.findViewById(R.id.toggleTrakt);
        mToggleTraktButton.setChecked(mTraktChecked);
        mToggleTraktButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!Utils.isTraktCredentialsValid(getActivity())) {
                        // authenticate already here
                        TraktCredentialsDialogFragment newFragment = TraktCredentialsDialogFragment
                                .newInstance();
                        newFragment.show(getFragmentManager(), "traktdialog");
                    }
                }

                mTraktChecked = isChecked;
                prefs.edit().putBoolean(AppPreferences.KEY_SHAREWITHTRAKT, isChecked).commit();
                updateCheckInButtonState();
            }
        });

        // Check in button
        mCheckinButton = layout.findViewById(R.id.checkinButton);
        updateCheckInButtonState();
        mCheckinButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(imdbId)) {
                    return;
                }

                if (!AndroidUtils.isNetworkConnected(getActivity())) {
                    Toast.makeText(getActivity(), R.string.offline, Toast.LENGTH_LONG).show();
                    return;
                }

                final String message = mMessageBox.getText().toString();

                if (mGetGlueChecked) {
                    if (!GetGlue.isAuthenticated(prefs)) {
                        // cancel if required auth data is missing
                        mToggleGetGlueButton.setChecked(false);
                        mGetGlueChecked = false;
                        updateCheckInButtonState();
                        return;
                    } else {
                        // check in, use task on thread pool
                        AndroidUtils.executeAsyncTask(new CheckInTask(imdbId, message,
                                getActivity()), new Void[] {});
                    }
                }

                if (mTraktChecked) {
                    if (!Utils.isTraktCredentialsValid(getActivity())) {
                        // cancel if required auth data is missing
                        mToggleTraktButton.setChecked(false);
                        mTraktChecked = false;
                        updateCheckInButtonState();
                        return;
                    } else {
                        // check in

                        // We want to remove any currently showing
                        // dialog, so make our own transaction and
                        // take care of that here.
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        Fragment prev = getFragmentManager().findFragmentByTag("progress-dialog");
                        if (prev != null) {
                            ft.remove(prev);
                        }
                        ProgressDialog newFragment = ProgressDialog.newInstance();
                        newFragment.show(ft, "progress-dialog");

                        // start the trakt check in task
                        AndroidUtils.executeAsyncTask(new TraktTask(getActivity(),
                                getFragmentManager(), null).checkin(imdbId,
                                message), new Void[] {
                                null
                        });
                    }
                }

                dismiss();
            }
        });

        return layout;
    }

    private void updateCheckInButtonState() {
        if (mGetGlueChecked || mTraktChecked) {
            mCheckinButton.setEnabled(true);
        } else {
            mCheckinButton.setEnabled(false);
        }
    }
}
