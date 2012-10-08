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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Response;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.movies.R;
import com.uwetrottmann.movies.entities.TraktStatus;
import com.uwetrottmann.movies.ui.AppPreferences;

/**
 * Dialog to gather and verify as well as clear trakt.tv credentials.
 * 
 * @author Uwe Trottmann
 */
public class TraktCredentialsDialogFragment extends SherlockDialogFragment {

    public static TraktCredentialsDialogFragment newInstance() {
        TraktCredentialsDialogFragment f = new TraktCredentialsDialogFragment();
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.pref_traktcredentials);
        final Context context = getActivity().getApplicationContext();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final View layout = inflater.inflate(R.layout.trakt_credentials_dialog, null);

        // restore the username from settings
        final String username = Utils.getTraktUsername(context);
        ((EditText) layout.findViewById(R.id.username)).setText(username);

        // new account toggle
        final View mailviews = layout.findViewById(R.id.mailviews);
        mailviews.setVisibility(View.GONE);

        ((CheckBox) layout.findViewById(R.id.checkNewAccount))
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mailviews.setVisibility(View.VISIBLE);
                        } else {
                            mailviews.setVisibility(View.GONE);
                        }
                    }
                });

        // status strip
        final TextView status = (TextView) layout.findViewById(R.id.status);
        final View progressbar = layout.findViewById(R.id.progressbar);
        final View progress = layout.findViewById(R.id.progress);
        progress.setVisibility(View.GONE);

        final Button connectbtn = (Button) layout.findViewById(R.id.connectbutton);
        final Button disconnectbtn = (Button) layout.findViewById(R.id.disconnectbutton);

        // disable disconnect button if there are no saved credentials
        if (username.length() == 0) {
            disconnectbtn.setEnabled(false);
        }

        connectbtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // prevent multiple instances
                connectbtn.setEnabled(false);
                disconnectbtn.setEnabled(false);

                final String username = ((EditText) layout.findViewById(R.id.username)).getText()
                        .toString();
                final String passwordHash = Utils.toSHA1(((EditText) layout
                        .findViewById(R.id.password)).getText().toString().getBytes());
                final String email = ((EditText) layout.findViewById(R.id.email)).getText()
                        .toString();
                final boolean isNewAccount = ((CheckBox) layout.findViewById(R.id.checkNewAccount))
                        .isChecked();
                final String traktApiKey = getResources().getString(R.string.trakt_apikey);

                AsyncTask<String, Void, Response> accountValidatorTask = new AsyncTask<String, Void, Response>() {

                    @Override
                    protected void onPreExecute() {
                        progress.setVisibility(View.VISIBLE);
                        progressbar.setVisibility(View.VISIBLE);
                        status.setText(R.string.waitplease);
                    }

                    @Override
                    protected Response doInBackground(String... params) {
                        // check if we have any usable data
                        if (username.length() == 0 || passwordHash == null) {
                            return null;
                        }

                        // check for connectivity
                        if (!AndroidUtils.isNetworkConnected(context)) {
                            Response r = new Response();
                            r.status = TraktStatus.FAILURE;
                            r.error = context.getString(R.string.offline);
                            return r;
                        }

                        // use a separate ServiceManager here to avoid
                        // setting wrong credentials
                        final ServiceManager manager = new ServiceManager();
                        manager.setApiKey(traktApiKey);
                        manager.setAuthentication(username, passwordHash);
                        manager.setUseSsl(true);

                        Response response = null;

                        try {
                            if (isNewAccount) {
                                // create new account
                                response = manager.accountService()
                                        .create(username, passwordHash, email).fire();
                            } else {
                                // validate existing account
                                response = manager.accountService().test().fire();
                            }
                        } catch (TraktException te) {
                            response = te.getResponse();
                        } catch (ApiException ae) {
                            response = null;
                        }

                        return response;
                    }

                    @Override
                    protected void onPostExecute(Response response) {
                        progressbar.setVisibility(View.GONE);
                        connectbtn.setEnabled(true);

                        if (response == null) {
                            status.setText(R.string.trakt_generalerror);
                            return;
                        }
                        if (response.status.equals(TraktStatus.FAILURE)) {
                            status.setText(response.error);
                            return;
                        }

                        String passwordEncr;
                        // try to encrypt the password before storing it
                        try {
                            passwordEncr = SimpleCrypto.encrypt(passwordHash, context);
                        } catch (Exception e) {
                            // password encryption failed
                            status.setText(R.string.trakt_generalerror);
                            return;
                        }

                        // prepare writing credentials to settings
                        Editor editor = prefs.edit();
                        editor.putString(AppPreferences.KEY_TRAKTUSER, username).putString(
                                AppPreferences.KEY_TRAKTPWD, passwordEncr);

                        if (response.status.equals(TraktStatus.SUCCESS)
                                && passwordEncr.length() != 0 && editor.commit()) {
                            // set new auth data for service manager
                            try {
                                Utils.getServiceManagerWithAuth(context, true);
                            } catch (Exception e) {
                                status.setText(R.string.trakt_generalerror);
                                return;
                            }

                            // all went through
                            dismiss();
                        }
                    }
                };

                accountValidatorTask.execute();
            }
        });

        disconnectbtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // clear trakt credentials
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        Editor editor = prefs.edit();
                        editor.putString(AppPreferences.KEY_TRAKTUSER, "").putString(
                                AppPreferences.KEY_TRAKTPWD, "");
                        editor.commit();

                        try {
                            Utils.getServiceManagerWithAuth(context, false).setAuthentication("",
                                    "");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        return null;
                    }
                }.execute();

                dismiss();
            }
        });

        return layout;
    }
}
