
package com.uwetrottmann.movies.ui;

import com.actionbarsherlock.app.SherlockListFragment;
import com.jakewharton.trakt.entities.Movie;
import com.uwetrottmann.movies.R;
import com.uwetrottmann.movies.util.ImageDownloader;
import com.uwetrottmann.movies.util.TraktMoviesLoader;
import com.uwetrottmann.movies.util.TraktMoviesLoader.TraktCategory;
import com.uwetrottmann.movies.util.Utils;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class MovieDetailsFragment extends SherlockListFragment implements
        LoaderCallbacks<List<Movie>> {

    public static MovieDetailsFragment newInstance(String imdbId) {
        MovieDetailsFragment f = new MovieDetailsFragment();

        Bundle args = new Bundle();
        args.putString(InitBundle.IMDBID, imdbId);
        f.setArguments(args);

        return f;
    }

    public interface InitBundle {
        String IMDBID = "imdbid";
    }

    // Use layout id for always in-app unique id
    private static final int MOVIE_LOADER_ID = R.layout.details_row;

    private TraktMovieSummaryAdapter mAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // set list adapter
        mAdapter = new TraktMovieSummaryAdapter(getSherlockActivity(), getFragmentManager());
        setListAdapter(mAdapter);

        // style list view
        final ListView list = getListView();
        list.setDivider(null);
        list.setSelector(R.color.transparent);

        // nag about no connectivity
        if (!Utils.isNetworkConnected(getActivity())) {
            setEmptyText(getString(R.string.offline));
            setListShown(true);
        } else {
            setEmptyText(getString(R.string.details_empty));
            setListShown(false);
            getArguments().putInt(TraktMoviesLoader.InitBundle.CATEGORY,
                    TraktCategory.SUMMARY.index());
            getLoaderManager().initLoader(MOVIE_LOADER_ID, getArguments(), this);
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

    private static class TraktMovieSummaryAdapter extends ArrayAdapter<Movie> {

        private LayoutInflater mLayoutInflater;

        private static final int LAYOUT = R.layout.details_row;

        private ImageDownloader mImageDownloader;

        private FragmentManager mFm;

        public TraktMovieSummaryAdapter(Context context, FragmentManager fm) {
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

            Movie item = getItem(position);

            ((TextView) view.findViewById(R.id.title)).setText(item.title);
            ((TextView) view.findViewById(R.id.description)).setText(item.overview);
            ((TextView) view.findViewById(R.id.lovevalue)).setText(item.ratings.percentage + "%");
            ((TextView) view.findViewById(R.id.lovevotes)).setText(getContext().getResources()
                    .getQuantityString(R.plurals.votes, item.ratings.votes, item.ratings.votes));

            view.findViewById(R.id.checkinButton).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckInDialogFragment dialog = CheckInDialogFragment.newInstance();
                    dialog.show(mFm, "checkin-dialog");
                }
            });

            ImageView imageView = (ImageView) view.findViewById(R.id.fanart);
            if (item.images.fanart != null) {
                String url = item.images.fanart.substring(0, item.images.fanart.length() - 4)
                        + "-940.jpg";
                mImageDownloader.download(url, imageView, false);
            }

            return view;
        }

        public void setData(List<Movie> data) {
            clear();
            if (data != null) {
                for (Movie userProfile : data) {
                    add(userProfile);
                }
            }
        }
    }
}
