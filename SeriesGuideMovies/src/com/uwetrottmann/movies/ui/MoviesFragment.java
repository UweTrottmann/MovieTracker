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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.movies.R;
import com.uwetrottmann.movies.loaders.TmdbMoviesLoader;
import com.uwetrottmann.movies.util.ImageDownloader;
import com.uwetrottmann.tmdb.entities.Movie;

import java.util.List;

public class MoviesFragment extends SherlockFragment implements LoaderCallbacks<List<Movie>>,
        OnItemClickListener {

    private static final int MOVIES_LOADER_ID = 0;

    private TmdbMoviesAdapter mAdapter;

    private boolean mMultiPane;

    private GridView mGrid;

    public static MoviesFragment newInstance() {
        MoviesFragment f = new MoviesFragment();

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        return inflater.inflate(R.layout.grid_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // set list adapter
        mAdapter = new TmdbMoviesAdapter(getActivity());

        // basic setup of grid view
        mGrid = (GridView) getView().findViewById(android.R.id.list);
        mGrid.setOnItemClickListener(this);
        View emptyView = getView().findViewById(android.R.id.empty);
        if (emptyView != null) {
            mGrid.setEmptyView(emptyView);
        }

        // restore an existing adapter
        if (mAdapter != null) {
            mGrid.setAdapter(mAdapter);
        }

        onListLoad(true);

        View detailsFragment = getSherlockActivity().findViewById(R.id.fragment_details);
        mMultiPane = detailsFragment != null && detailsFragment.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        Movie movie = (Movie) parent.getItemAtPosition(position);
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
            Toast.makeText(getActivity(), getString(R.string.offline), Toast.LENGTH_LONG).show();
        } else {
            // nag about a trakt account if trying to display auth-only lists
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
    }

    @Override
    public void onLoaderReset(Loader<List<Movie>> loader) {
        mAdapter.setData(null);
    }

    private static class TmdbMoviesAdapter extends ArrayAdapter<Movie> {

        private LayoutInflater mLayoutInflater;

        private static final int LAYOUT = R.layout.movies_row;

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
                viewHolder.poster = (ImageView) convertView.findViewById(R.id.poster);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            // set text properties immediately
            Movie item = getItem(position);
            viewHolder.title.setText(item.title);
            if (item.poster_path != null) {
                // TODO get image path from TMDb, not static
                String posterPath = "http://cf2.imgobject.com/t/p/w185" + item.poster_path;
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

            public ImageView poster;
        }
    }

}
