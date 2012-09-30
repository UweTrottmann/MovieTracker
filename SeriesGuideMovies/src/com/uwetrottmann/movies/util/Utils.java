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
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jakewharton.trakt.ServiceManager;
import com.uwetrottmann.movies.R;
import com.uwetrottmann.movies.ui.AppPreferences;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {

    private static final String TAG = "Utils";

    private static ServiceManager sServiceManagerWithAuthInstance;

    private static ServiceManager sServiceManagerInstance;

    private static com.uwetrottmann.tmdb.ServiceManager sTmdbServiceManagerInstance;

    public static boolean isTraktCredentialsValid(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext());
        String username = prefs.getString(AppPreferences.KEY_TRAKTUSER, "");
        String password = prefs.getString(AppPreferences.KEY_TRAKTPWD, "");

        return (!username.equals("") && !password.equals(""));
    }

    /**
     * Get the trakt-java ServiceManger with user credentials and our API key
     * set.
     * 
     * @param context
     * @param refreshCredentials Set this flag to refresh the user credentials.
     * @return
     * @throws Exception When decrypting the password failed.
     */
    public static synchronized ServiceManager getServiceManagerWithAuth(Context context,
            boolean refreshCredentials) throws Exception {
        if (sServiceManagerWithAuthInstance == null) {
            sServiceManagerWithAuthInstance = new ServiceManager();
            sServiceManagerWithAuthInstance.setReadTimeout(10000);
            sServiceManagerWithAuthInstance.setConnectionTimeout(15000);
            sServiceManagerWithAuthInstance.setApiKey(context.getResources().getString(
                    R.string.trakt_apikey));
            // this made some problems, so sadly disabled for now
            // manager.setUseSsl(true);

            refreshCredentials = true;
        }

        if (refreshCredentials) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                    .getApplicationContext());

            final String username = prefs.getString(AppPreferences.KEY_TRAKTUSER, "");
            String password = prefs.getString(AppPreferences.KEY_TRAKTPWD, "");
            password = SimpleCrypto.decrypt(password, context);

            sServiceManagerWithAuthInstance.setAuthentication(username, password);
        }

        return sServiceManagerWithAuthInstance;
    }

    /**
     * Get a trakt-java ServiceManager with just our API key set. NO user auth
     * data.
     * 
     * @param context
     * @return
     */
    public static synchronized ServiceManager getServiceManager(Context context) {
        if (sServiceManagerInstance == null) {
            sServiceManagerInstance = new ServiceManager();
            sServiceManagerInstance.setReadTimeout(10000);
            sServiceManagerInstance.setConnectionTimeout(15000);
            sServiceManagerInstance.setApiKey(context.getResources().getString(
                    R.string.trakt_apikey));
            // this made some problems, so sadly disabled for now
            // manager.setUseSsl(true);
        }

        return sServiceManagerInstance;
    }

    public static String getTraktUsername(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext());

        return prefs.getString(AppPreferences.KEY_TRAKTUSER, "");
    }

    public static String getVersion(Context context) {
        String version;
        try {
            version = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA).versionName;
        } catch (NameNotFoundException e) {
            version = "UnknownVersion";
        }
        return version;
    }

    public static String toSHA1(byte[] convertme) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] b = md.digest(convertme);

            String result = "";
            for (int i = 0; i < b.length; i++) {
                result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            }

            return result;
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, "Could not get SHA-1 message digest instance", e);
        }
        return null;
    }

    /**
     * Get a tmdb-java ServiceManager with our API key set.
     */
    public static synchronized com.uwetrottmann.tmdb.ServiceManager getTmdbServiceManager(
            Context context) {
        if (sTmdbServiceManagerInstance == null) {
            sTmdbServiceManagerInstance = new com.uwetrottmann.tmdb.ServiceManager();
            sTmdbServiceManagerInstance.setReadTimeout(10000);
            sTmdbServiceManagerInstance.setConnectionTimeout(15000);
            sTmdbServiceManagerInstance.setApiKey(context.getResources().getString(
                    R.string.tmdb_apikey));
        }

        return sTmdbServiceManagerInstance;
    }

}
