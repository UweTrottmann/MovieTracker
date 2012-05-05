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
import com.jakewharton.trakt.entities.Movie;
import com.uwetrottmann.movies.R;
import com.uwetrottmann.movies.util.ImageDownloader;
import com.uwetrottmann.movies.util.TraktMoviesLoader;
import com.uwetrottmann.movies.util.TraktMoviesLoader.TraktCategory;
import com.uwetrottmann.movies.util.Utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class MoviesFragment extends SherlockListFragment implements LoaderCallbacks<List<Movie>> {

    private static final int MOVIES_LOADER_ID = 0;

    private TraktMoviesAdapter mAdapter;

    private boolean mMultiPane;

    private TraktCategory mListCategory;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // set list adapter
        mAdapter = new TraktMoviesAdapter(getSherlockActivity());
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
        Movie movie = (Movie) l.getItemAtPosition(position);
        if (movie != null && movie.imdbId != null) {
            if (mMultiPane) {
                MovieDetailsFragment newFragment = MovieDetailsFragment.newInstance(movie.imdbId);

                // Execute a transaction, replacing any existing
                // fragment with this one inside the frame.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.fragment, newFragment);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            } else {
                Intent i = new Intent(getSherlockActivity(), MovieDetailsActivity.class);
                i.putExtra(MovieDetailsFragment.InitBundle.IMDBID, movie.imdbId);
                startActivity(i);
            }
        }
    }

    public void onListLoad(boolean isInitialLoad) {
        // nag about no connectivity
        if (!Utils.isNetworkConnected(getActivity())) {
            setEmptyText(getString(R.string.offline));
            setListShown(true);
        } else {
            // nag about a trakt account if trying to display auth-only lists
            if (mListCategory != TraktCategory.WATCHLIST
                    || Utils.isTraktCredentialsValid(getActivity())) {
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
    }

    @Override
    public Loader<List<Movie>> onCreateLoader(int id, Bundle args) {
        return new TraktMoviesLoader(getSherlockActivity(), args);
    }

    @Override
    public void onLoadFinished(Loader<List<Movie>> loader, List<Movie> data) {
        mAdapter.setData(data);

        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Movie>> loader) {
        mAdapter.setData(null);
    }

    private static class TraktMoviesAdapter extends ArrayAdapter<Movie> {

        private LayoutInflater mLayoutInflater;

        private static final int LAYOUT = R.layout.movie_row;

        private ImageDownloader mImageDownloader;

        public TraktMoviesAdapter(Context context) {
            super(context, LAYOUT);
            mLayoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mImageDownloader = ImageDownloader.getInstance(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(LAYOUT, null);

                viewHolder = new ViewHolder();
                viewHolder.title = (TextView) convertView.findViewById(R.id.title);
                viewHolder.description = (TextView) convertView.findViewById(R.id.description);
                viewHolder.poster = (ImageView) convertView.findViewById(R.id.poster);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            // set text properties immediately
            Movie item = getItem(position);
            viewHolder.title.setText(item.title);
            viewHolder.description.setText(item.overview);
            if (item.images.poster != null) {
                String posterPath = item.images.poster
                        .substring(0, item.images.poster.length() - 4) + "-138.jpg";
                mImageDownloader.download(posterPath, viewHolder.poster, false);
            }

            return convertView;
        }

        public void setData(List<Movie> data) {
            clear();
            if (data != null) {
                for (Movie userProfile : data) {
                    add(userProfile);
                }
            }
        }

        static class ViewHolder {

            public TextView title;

            public TextView description;

            public ImageView poster;
        }
    }

}
