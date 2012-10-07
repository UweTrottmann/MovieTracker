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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.movies.Constants;
import com.uwetrottmann.movies.R;
import com.uwetrottmann.movies.loaders.TmdbMovieLoader;
import com.uwetrottmann.movies.util.ImageDownloader;
import com.uwetrottmann.tmdb.entities.Movie;

public class MovieDetailsFragment extends SherlockListFragment implements
        LoaderCallbacks<Movie> {

    public static MovieDetailsFragment newInstance(int tmdbId) {
        MovieDetailsFragment f = new MovieDetailsFragment();

        Bundle args = new Bundle();
        args.putInt(InitBundle.TMDBID, tmdbId);
        f.setArguments(args);

        return f;
    }

    public interface InitBundle {
        String TMDBID = "tmdbid";
    }

    // Use layout id for always in-app unique id
    private static final int MOVIE_LOADER_ID = R.layout.details_row;

    private MovieSummaryAdapter mAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // set list adapter
        mAdapter = new MovieSummaryAdapter(getSherlockActivity(), getFragmentManager());
        setListAdapter(mAdapter);

        // style list view
        final ListView list = getListView();
        list.setDivider(null);
        list.setSelector(R.color.transparent);

        // nag about no connectivity
        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            setEmptyText(getString(R.string.offline));
            setListShown(true);
        } else {
            setEmptyText(getString(R.string.details_empty));
            setListShown(false);
            getLoaderManager().initLoader(MOVIE_LOADER_ID, getArguments(), this);
        }
    }

    @Override
    public Loader<Movie> onCreateLoader(int id, Bundle args) {
        return new TmdbMovieLoader(getSherlockActivity(), args.getInt(InitBundle.TMDBID));
    }

    @Override
    public void onLoadFinished(Loader<Movie> loader, Movie data) {
        mAdapter.setData(data);

        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Movie> loader) {
        mAdapter.setData(null);
    }

    private static class MovieSummaryAdapter extends ArrayAdapter<Movie> {

        private LayoutInflater mLayoutInflater;

        private static final int LAYOUT = R.layout.details_row;

        private ImageDownloader mImageDownloader;

        private FragmentManager mFm;

        public MovieSummaryAdapter(Context context, FragmentManager fm) {
            super(context, LAYOUT);
            mLayoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mImageDownloader = ImageDownloader.getInstance(context);
            mFm = fm;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                view = mLayoutInflater.inflate(LAYOUT, parent, false);
            } else {
                view = convertView;
            }

            final Movie item = getItem(position);

            ((TextView) view.findViewById(R.id.title)).setText(item.title);
            ((TextView) view.findViewById(R.id.description)).setText(item.overview);
            ((RatingBar) view.findViewById(R.id.ratingBar))
                    .setRating((float) (double) item.vote_average);
            ((TextView) view.findViewById(R.id.votes)).setText(getContext().getString(
                    R.string.votes, item.vote_count));
            ((TextView) view.findViewById(R.id.releaseDate)).setText(DateUtils.formatDateTime(
                    getContext(), item.release_date.getTime(), DateUtils.FORMAT_SHOW_DATE));
            ((TextView) view.findViewById(R.id.runtime)).setText(getContext().getString(
                    R.string.timeunit, item.runtime));

            view.findViewById(R.id.checkinButton).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckInDialogFragment dialog = CheckInDialogFragment.newInstance(item.imdb_id,
                            item.title);
                    dialog.show(mFm, "checkin-dialog");
                }
            });

            // IMDb button
            View buttonImdb = view.findViewById(R.id.buttonIMDB);
            if (!TextUtils.isEmpty(item.imdb_id)) {
                buttonImdb.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("imdb:///title/"
                                + item.imdb_id + "/"));
                        try {
                            getContext().startActivity(myIntent);
                        } catch (ActivityNotFoundException e) {
                            myIntent = new Intent(Intent.ACTION_VIEW, Uri
                                    .parse(Constants.IMDB_TITLE_URL + item.imdb_id));
                            getContext().startActivity(myIntent);
                        }
                    }
                });
            } else {
                buttonImdb.setVisibility(View.GONE);
            }

            ImageView imageView = (ImageView) view.findViewById(R.id.fanart);
            if (!TextUtils.isEmpty(item.backdrop_path)) {
                String url = "http://cf2.imgobject.com/t/p/w780" + item.backdrop_path;
                mImageDownloader.download(url, imageView, false);
            }

            return view;
        }

        public void setData(Movie movie) {
            clear();
            if (movie != null) {
                add(movie);
            }
        }
    }
}
