
package com.uwetrottmann.movies.util;

import com.jakewharton.trakt.ServiceManager;
import com.uwetrottmann.movies.R;
import com.uwetrottmann.movies.ui.AppPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

public class Utils {

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private static ServiceManager sServiceManagerWithAuthInstance;

    private static ServiceManager sServiceManagerInstance;

    public static boolean isHoneycombOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean isFroyoOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean isExtStorageAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
            return activeNetworkInfo.isConnected();
        }
        return false;
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetworkInfo != null) {
            return wifiNetworkInfo.isConnected();
        }
        return false;
    }

    public static void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }
    }

    public static int copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
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

}
