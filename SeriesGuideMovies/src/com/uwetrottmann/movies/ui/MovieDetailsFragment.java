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
import android.support.v4.app.LoaderManager;
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
import com.uwetrottmann.movies.entities.MovieDetails;
import com.uwetrottmann.movies.loaders.TmdbMovieLoader;
import com.uwetrottmann.movies.util.ImageDownloader;
import com.uwetrottmann.tmdb.entities.Casts;
import com.uwetrottmann.tmdb.entities.Casts.CastMember;
import com.uwetrottmann.tmdb.entities.Casts.CrewMember;
import com.uwetrottmann.tmdb.entities.Movie;
import com.uwetrottmann.tmdb.entities.Trailers;

public class MovieDetailsFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<MovieDetails> {

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
    public Loader<MovieDetails> onCreateLoader(int id, Bundle args) {
        return new TmdbMovieLoader(getSherlockActivity(), args.getInt(InitBundle.TMDBID));
    }

    @Override
    public void onLoadFinished(Loader<MovieDetails> loader, MovieDetails data) {
        mAdapter.setData(data);

        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<MovieDetails> loader) {
        mAdapter.setData(null);
    }

    private static class MovieSummaryAdapter extends ArrayAdapter<MovieDetails> {

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

            final MovieDetails item = getItem(position);
            final Movie movie = item.movie;
            final Trailers trailers = item.trailers;
            final Casts cast = item.casts;

            ((TextView) view.findViewById(R.id.title)).setText(movie.title);
            ((TextView) view.findViewById(R.id.description)).setText(movie.overview);
            ((RatingBar) view.findViewById(R.id.ratingBar))
                    .setRating((float) (double) movie.vote_average);
            ((TextView) view.findViewById(R.id.votes)).setText(getContext().getResources()
                    .getQuantityString(R.plurals.votes, movie.vote_count, movie.vote_count));
            ((TextView) view.findViewById(R.id.releaseDate)).setText(DateUtils.formatDateTime(
                    getContext(), movie.release_date.getTime(), DateUtils.FORMAT_SHOW_DATE));
            ((TextView) view.findViewById(R.id.runtime)).setText(getContext().getString(
                    R.string.timeunit, movie.runtime));

            if (cast != null) {
                // cast
                StringBuilder actors = new StringBuilder();
                for (CastMember castMember : cast.cast) {
                    if (actors.length() > 0) {
                        actors.append("\n");
                    }
                    actors.append(castMember.name).append(" ");
                    actors.append(getContext().getString(R.string.cast_as)).append(" ");
                    actors.append(castMember.character);
                }
                ((TextView) view.findViewById(R.id.actors)).setText(actors);

                // crew
                StringBuilder crew = new StringBuilder();
                for (CrewMember crewMember : cast.crew) {
                    if (crew.length() > 0) {
                        crew.append("\n");
                    }
                    crew.append(crewMember.name).append(" (");
                    crew.append(crewMember.job).append(")");
                }
                ((TextView) view.findViewById(R.id.crew)).setText(crew);
            }

            view.findViewById(R.id.checkinButton).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckInDialogFragment dialog = CheckInDialogFragment.newInstance(movie.imdb_id,
                            movie.title);
                    dialog.show(mFm, "checkin-dialog");
                }
            });

            // IMDb button
            View buttonImdb = view.findViewById(R.id.buttonIMDB);
            if (!TextUtils.isEmpty(movie.imdb_id)) {
                buttonImdb.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("imdb:///title/"
                                + movie.imdb_id + "/"));
                        try {
                            getContext().startActivity(myIntent);
                        } catch (ActivityNotFoundException e) {
                            myIntent = new Intent(Intent.ACTION_VIEW, Uri
                                    .parse(Constants.IMDB_TITLE_URL + movie.imdb_id));
                            getContext().startActivity(myIntent);
                        }
                    }
                });
            } else {
                buttonImdb.setVisibility(View.GONE);
            }

            // Trailer button
            // TODO use new YouTube API to display inline
            View buttonTrailer = view.findViewById(R.id.buttonTrailer);
            if (trailers != null) {
                if (trailers.youtube.size() > 0) {
                    buttonTrailer.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent myIntent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("http://www.youtube.com/watch?v="
                                            + trailers.youtube.get(0).source));
                            getContext().startActivity(myIntent);
                        }
                    });
                }
            } else {
                buttonTrailer.setVisibility(View.GONE);
            }

            ImageView imageView = (ImageView) view.findViewById(R.id.fanart);
            if (!TextUtils.isEmpty(movie.backdrop_path)) {
                String url = "http://cf2.imgobject.com/t/p/w780" + movie.backdrop_path;
                mImageDownloader.download(url, imageView, false);
            }

            return view;
        }

        public void setData(MovieDetails movie) {
            clear();
            if (movie != null) {
                add(movie);
            }
        }
    }
}
