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

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.movies.R;
import com.uwetrottmann.movies.util.TraktCredentialsDialogFragment;
import com.uwetrottmann.movies.util.TraktMoviesLoader.TraktCategory;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.widget.ArrayAdapter;

public class MoviesActivity extends SherlockFragmentActivity implements
        ActionBar.OnNavigationListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.movies_activity);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        ArrayAdapter<CharSequence> mActionBarList = ArrayAdapter.createFromResource(this,
                R.array.movie_lists, R.layout.sherlock_spinner_item);
        mActionBarList.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
        actionBar.setListNavigationCallbacks(mActionBarList, this);

        // try to restore previously set nav item
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        final int navItem = prefs.getInt(AppPreferences.KEY_NAVSELECTION, 0);
        actionBar.setSelectedNavigationItem(navItem);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.movies, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_login: {
                TraktCredentialsDialogFragment f = TraktCredentialsDialogFragment.newInstance();
                f.show(getSupportFragmentManager(), "credentials-dialog");
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @TargetApi(9)
    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        Fragment newFragment = null;
        switch (itemPosition) {
            case 0:
            default:
                // trending
                newFragment = MoviesFragment.newInstance(TraktCategory.TRENDING);
                break;
            case 1:
                // watchlist
                newFragment = LocalMoviesFragment.newInstance(TraktCategory.WATCHLIST);
                break;
        }
        if (newFragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_list, newFragment);
            ft.commit();

            // save the selected filter back to settings
            Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                    .edit();
            editor.putInt(AppPreferences.KEY_NAVSELECTION, itemPosition);
            if (AndroidUtils.isGingerbreadOrHigher()) {
                editor.apply();
            } else {
                editor.commit();
            }

            return true;
        } else {
            return false;
        }
    }
}
