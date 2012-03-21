
package com.uwetrottmann.movies.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.uwetrottmann.movies.R;

import android.os.Bundle;

public class MovieDetailsActivity extends SherlockFragmentActivity {

    private SherlockFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.details_activity);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            mFragment = new MovieDetailsFragment();
            mFragment.setArguments(getIntent().getExtras());

            getSupportFragmentManager().beginTransaction().replace(R.id.fragment, mFragment)
                    .commit();
        }
    }
}
