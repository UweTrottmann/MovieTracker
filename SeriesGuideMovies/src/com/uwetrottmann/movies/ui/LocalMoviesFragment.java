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

import com.actionbarsherlock.app.SherlockListFragment;
import com.uwetrottmann.movies.R;
import com.uwetrottmann.movies.provider.MoviesContract.Movies;
import com.uwetrottmann.movies.util.ImageDownloader;
import com.uwetrottmann.movies.util.TraktMoviesLoader;
import com.uwetrottmann.movies.util.TraktMoviesLoader.TraktCategory;
import com.uwetrottmann.movies.util.Utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class LocalMoviesFragment extends SherlockListFragment implements LoaderCallbacks<Cursor> {

    private static final int MOVIES_LOADER_ID = 0;

    private MoviesCursorAdapter mAdapter;

    private boolean mMultiPane;

    private TraktCategory mListCategory;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // set list adapter
        mAdapter = new MoviesCursorAdapter(getActivity());
        setListAdapter(mAdapter);

        mListCategory = TraktCategory.fromValue(getArguments().getInt(
                TraktMoviesLoader.InitBundle.CATEGORY));

        // style list view
        final ListView list = getListView();
        list.setDivider(getResources().getDrawable(R.drawable.divider_horizontal_holo_dark));
        list.setSelector(R.drawable.list_selector_holo_dark);
        list.setClipToPadding(Utils.isHoneycombOrHigher() ? false : true);
        final float scale = getResources().getDisplayMetrics().density;
        int layoutPadding = (int) (10 * scale + 0.5f);
        int defaultPadding = (int) (8 * scale + 0.5f);
        list.setPadding(layoutPadding, layoutPadding, layoutPadding, defaultPadding);
        list.setFastScrollEnabled(true);

        onListLoad(true);

        View detailsFragment = getSherlockActivity().findViewById(R.id.fragment);
        mMultiPane = detailsFragment != null && detailsFragment.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Cursor movie = (Cursor) l.getItemAtPosition(position);
        if (movie != null && movie.getString(MoviesQuery.IMDBID) != null) {
            final String imdbId = movie.getString(MoviesQuery.IMDBID);
            if (mMultiPane) {
                MovieDetailsFragment newFragment = MovieDetailsFragment.newInstance(imdbId);

                // Execute a transaction, replacing any existing
                // fragment with this one inside the frame.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.fragment, newFragment);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            } else {
                Intent i = new Intent(getSherlockActivity(), MovieDetailsActivity.class);
                i.putExtra(MovieDetailsFragment.InitBundle.IMDBID, imdbId);
                startActivity(i);
            }
        }
    }

    public void onListLoad(boolean isInitialLoad) {
        // nag about a trakt account if trying to display auth-only lists
        if (Utils.isTraktCredentialsValid(getActivity())) {
            setEmptyText(getString(R.string.movies_empty));
            setListShown(false);
            if (isInitialLoad) {
                getLoaderManager().initLoader(MOVIES_LOADER_ID, getArguments(), this);
            } else {
                getLoaderManager().restartLoader(MOVIES_LOADER_ID, getArguments(), this);
            }
        } else {
            setEmptyText(getString(R.string.please_setup_trakt));
            setListShown(true);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), Movies.CONTENT_URI, MoviesQuery.PROJECTION, null,
                null, MoviesQuery.SORTORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);

        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    private static class MoviesCursorAdapter extends SimpleCursorAdapter {

        private LayoutInflater mLayoutInflater;

        private static final int LAYOUT = R.layout.movie_row;

        private ImageDownloader mImageDownloader;

        private final static String[] FROM = new String[] {
                Movies.TITLE, Movies.OVERVIEW, Movies.POSTER
        };

        private final static int[] TO = new int[] {
                R.id.title, R.id.description, R.id.poster
        };

        public MoviesCursorAdapter(Context context) {
            super(context, LAYOUT, null, FROM, TO, 0);
            mLayoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mImageDownloader = ImageDownloader.getInstance(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (!mDataValid) {
                throw new IllegalStateException(
                        "this should only be called when the cursor is valid");
            }
            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }

            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(LAYOUT, null);

                viewHolder = new ViewHolder();
                viewHolder.title = (TextView) convertView.findViewById(R.id.title);
                viewHolder.overview = (TextView) convertView.findViewById(R.id.description);
                viewHolder.poster = (ImageView) convertView.findViewById(R.id.poster);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            // set text properties immediately
            viewHolder.title.setText(mCursor.getString(MoviesQuery.TITLE));
            viewHolder.overview.setText(mCursor.getString(MoviesQuery.OVERVIEW));
            String poster = mCursor.getString(MoviesQuery.POSTER);
            if (poster != null) {
                String posterPath = poster.substring(0, poster.length() - 4) + "-138.jpg";
                mImageDownloader.download(posterPath, viewHolder.poster, false);
            }

            return convertView;
        }

        static class ViewHolder {

            public TextView title;

            public TextView overview;

            public ImageView poster;
        }
    }

    interface MoviesQuery {

        String[] PROJECTION = new String[] {
                Movies._ID, Movies.TITLE, Movies.OVERVIEW, Movies.POSTER, Movies.IMDBID
        };

        String SORTORDER = Movies.TITLE + " ASC";

        int _ID = 0;

        int TITLE = 1;

        int OVERVIEW = 2;

        int POSTER = 3;
        
        int IMDBID = 4;

    }

}
