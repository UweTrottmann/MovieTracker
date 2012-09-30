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

import android.content.Context;
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

import com.actionbarsherlock.app.SherlockListFragment;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.movies.R;
import com.uwetrottmann.movies.loaders.TmdbMoviesLoader;
import com.uwetrottmann.movies.util.ImageDownloader;
import com.uwetrottmann.tmdb.entities.Movie;

import java.util.List;

public class MoviesFragment extends SherlockListFragment implements LoaderCallbacks<List<Movie>> {

    private static final int MOVIES_LOADER_ID = 0;

    private TmdbMoviesAdapter mAdapter;

    private boolean mMultiPane;

    public static MoviesFragment newInstance() {
        MoviesFragment f = new MoviesFragment();

        return f;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // set list adapter
        mAdapter = new TmdbMoviesAdapter(getActivity());
        setListAdapter(mAdapter);

        // style list view
        final ListView list = getListView();
        list.setDivider(getResources().getDrawable(R.drawable.divider_horizontal_holo_dark));
        list.setSelector(R.drawable.list_selector_holo_dark);
        list.setClipToPadding(AndroidUtils.isHoneycombOrHigher() ? false : true);
        final float scale = getResources().getDisplayMetrics().density;
        int layoutPadding = (int) (10 * scale + 0.5f);
        int defaultPadding = (int) (8 * scale + 0.5f);
        list.setPadding(layoutPadding, layoutPadding, layoutPadding, defaultPadding);
        list.setFastScrollEnabled(true);

        onListLoad(true);

        View detailsFragment = getSherlockActivity().findViewById(R.id.fragment_details);
        mMultiPane = detailsFragment != null && detailsFragment.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Movie movie = (Movie) l.getItemAtPosition(position);
        if (movie != null && movie.id != null) {
            MovieDetailsFragment newFragment = MovieDetailsFragment.newInstance(movie.id);

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            if (mMultiPane) {
                ft.replace(R.id.fragment_details, newFragment);
            } else {
                ft.replace(R.id.fragment_list, newFragment);
                ft.addToBackStack(null);
            }
            ft.commit();
        }
    }

    public void onListLoad(boolean isInitialLoad) {
        // nag about no connectivity
        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            setEmptyText(getString(R.string.offline));
            setListShown(true);
        } else {
            // nag about a trakt account if trying to display auth-only lists
            setEmptyText(getString(R.string.movies_empty));
            setListShown(false);
            if (isInitialLoad) {
                getLoaderManager().initLoader(MOVIES_LOADER_ID, getArguments(), this);
            } else {
                getLoaderManager().restartLoader(MOVIES_LOADER_ID, getArguments(), this);
            }
        }
    }

    @Override
    public Loader<List<Movie>> onCreateLoader(int id, Bundle args) {
        return new TmdbMoviesLoader(getSherlockActivity());
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

    private static class TmdbMoviesAdapter extends ArrayAdapter<Movie> {

        private LayoutInflater mLayoutInflater;

        private static final int LAYOUT = R.layout.movie_row;

        private ImageDownloader mImageDownloader;

        public TmdbMoviesAdapter(Context context) {
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
                viewHolder.overview = (TextView) convertView.findViewById(R.id.description);
                viewHolder.poster = (ImageView) convertView.findViewById(R.id.poster);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            // set text properties immediately
            Movie item = getItem(position);
            viewHolder.title.setText(item.title);
            viewHolder.overview.setText("");
            if (item.poster_path != null) {
                // TODO get image path from TMDb, not static
                String posterPath = "http://cf2.imgobject.com/t/p/w154" + item.poster_path;
                mImageDownloader.download(posterPath, viewHolder.poster, false);
            }

            return convertView;
        }

        public void setData(List<Movie> data) {
            clear();
            if (data != null) {
                for (Movie movie : data) {
                    add(movie);
                }
            }
        }

        static class ViewHolder {

            public TextView title;

            public TextView overview;

            public ImageView poster;
        }
    }

}
