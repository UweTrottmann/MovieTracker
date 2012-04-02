
package com.uwetrottmann.movies.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.uwetrottmann.movies.R;
import com.uwetrottmann.movies.util.TraktMoviesLoader;
import com.uwetrottmann.movies.util.TraktMoviesLoader.TraktCategory;
import com.uwetrottmann.movies.util.Utils;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import java.util.ArrayList;

public class MoviesActivity extends SherlockFragmentActivity {
    private ViewPager mViewPager;

    private TabsAdapter mTabsAdapter;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.movies_activity);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mViewPager = (ViewPager) findViewById(R.id.pager);

        mTabsAdapter = new TabsAdapter(this, actionBar, mViewPager);

        ActionBar.Tab trendingTab = actionBar.newTab().setText(R.string.trending);
        Bundle trendingArgs = new Bundle();
        trendingArgs.putInt(TraktMoviesLoader.InitBundle.CATEGORY, TraktCategory.TRENDING.index());
        mTabsAdapter.addTab(trendingTab, MoviesFragment.class, trendingArgs);

        if (Utils.isTraktCredentialsValid(this)) {
            ActionBar.Tab watchlistTab = actionBar.newTab().setText(R.string.watchlist);
            Bundle watchlistArgs = new Bundle();
            watchlistArgs.putInt(TraktMoviesLoader.InitBundle.CATEGORY,
                    TraktCategory.WATCHLIST.index());
            mTabsAdapter.addTab(watchlistTab, MoviesFragment.class, watchlistArgs);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.movies, menu);
        return true;
    }

    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost. It relies on a
     * trick. Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show. This is not sufficient for switching
     * between pages. So instead we make the content part of the tab host 0dp
     * high (it is not shown) and the TabsAdapter supplies its own dummy view to
     * show as the tab content. It listens to changes in tabs, and takes care of
     * switch to the correct paged in the ViewPager whenever the selected tab
     * changes.
     */
    public static class TabsAdapter extends FragmentPagerAdapter implements
            ViewPager.OnPageChangeListener, ActionBar.TabListener {
        private final Context mContext;

        private final ActionBar mActionBar;

        private final ViewPager mViewPager;

        private final ArrayList<String> mTabs = new ArrayList<String>();

        private final ArrayList<Bundle> mArgs = new ArrayList<Bundle>();

        public TabsAdapter(FragmentActivity activity, ActionBar actionBar, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mActionBar = actionBar;
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
            mTabs.add(clss.getName());
            mArgs.add(args);
            mActionBar.addTab(tab.setTabListener(this));
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            return Fragment.instantiate(mContext, mTabs.get(position), mArgs.get(position));
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            mViewPager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
    }
}
