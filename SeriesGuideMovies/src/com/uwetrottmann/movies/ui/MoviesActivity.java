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
import com.uwetrottmann.movies.R;
import com.uwetrottmann.movies.util.MoviesUpdateTask;
import com.uwetrottmann.movies.util.TaskManager;
import com.uwetrottmann.movies.util.TraktCredentialsDialogFragment;
import com.uwetrottmann.movies.util.TraktMoviesLoader.TraktCategory;

import android.os.Bundle;
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
            case R.id.menu_update: {
                TaskManager.getInstance(this).tryUpdateTask(
                        new MoviesUpdateTask(getApplicationContext()));
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        switch (itemPosition) {
            case 0: {
                // trending
                MoviesFragment newFragment = MoviesFragment.newInstance(TraktCategory.TRENDING);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_list, newFragment);
                ft.commit();
                return true;
            }
            case 1: {
                // watchlist
                LocalMoviesFragment newFragment = LocalMoviesFragment
                        .newInstance(TraktCategory.WATCHLIST);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_list, newFragment);
                ft.commit();
                return true;
            }
        }
        return false;
    }
}
