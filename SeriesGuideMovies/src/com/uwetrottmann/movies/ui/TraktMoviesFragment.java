
package com.uwetrottmann.movies.ui;

import com.actionbarsherlock.app.SherlockListFragment;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Movie;
import com.uwetrottmann.movies.R;
import com.uwetrottmann.movies.util.ImageDownloader;
import com.uwetrottmann.movies.util.Utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraktMoviesFragment extends SherlockListFragment implements
        LoaderCallbacks<List<Movie>> {

    public enum TraktCategory {
        TRENDING(0), RELEASED(1);

        private final int index;

        private TraktCategory(int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }

        private static final Map<Integer, TraktCategory> INT_MAPPING = new HashMap<Integer, TraktCategory>();

        static {
            for (TraktCategory via : TraktCategory.values()) {
                INT_MAPPING.put(via.index(), via);
            }
        }

        public static TraktCategory fromValue(int value) {
            return INT_MAPPING.get(value);
        }
    }

    public interface InitBundle {
        String CATEGORY = "category";
    }

    private static final int MOVIES_LOADER_ID = 0;

    private TraktMoviesAdapter mAdapter;

    private boolean mMultiPane;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // set list adapter
        mAdapter = new TraktMoviesAdapter(getSherlockActivity());
        setListAdapter(mAdapter);

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

        // nag about no connectivity
        if (!Utils.isNetworkConnected(getActivity())) {
            setEmptyText(getString(R.string.offline));
            setListShown(true);
        } else {
            setEmptyText(getString(R.string.movies_empty));
            setListShown(false);
            getLoaderManager().initLoader(MOVIES_LOADER_ID, getArguments(), this);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Movie movie = (Movie) l.getItemAtPosition(position);
        if (movie != null && movie.imdbId != null) {
            if (mMultiPane) {
                MovieDetailsFragment newFragment = MovieDetailsFragment.newInstance(movie.imdbId);
                // TODO
            } else {
                Intent i = new Intent(getSherlockActivity(), MovieDetailsActivity.class);
                i.putExtra(MovieDetailsFragment.InitBundle.IMDBID, movie.imdbId);
                startActivity(i);
            }
        }
    }

    @Override
    public Loader<List<Movie>> onCreateLoader(int id, Bundle args) {
        return new TraktMoviesLoader(getSherlockActivity());
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

    private static class TraktMoviesLoader extends AsyncTaskLoader<List<Movie>> {

        private List<Movie> mData;

        public TraktMoviesLoader(Context context) {
            super(context);
        }

        @Override
        public List<Movie> loadInBackground() {
            try {
                return Utils.getServiceManager(getContext()).movieService().trending().fire();
            } catch (TraktException te) {
                return null;
            } catch (ApiException ae) {
                return null;
            }

        }

        /**
         * Called when there is new data to deliver to the client. The super
         * class will take care of delivering it; the implementation here just
         * adds a little more logic.
         */
        @Override
        public void deliverResult(List<Movie> data) {
            if (isReset()) {
                // An async query came in while the loader is stopped. We
                // don't need the result.
                if (data != null) {
                    onReleaseResources(data);
                }
            }
            List<Movie> oldData = data;
            mData = data;

            if (isStarted()) {
                // If the Loader is currently started, we can immediately
                // deliver its results.
                super.deliverResult(data);
            }

            if (oldData != null) {
                onReleaseResources(oldData);
            }
        }

        @Override
        protected void onStartLoading() {
            if (mData != null) {
                deliverResult(mData);
            } else {
                forceLoad();
            }
        }

        /**
         * Handles a request to stop the Loader.
         */
        @Override
        protected void onStopLoading() {
            // Attempt to cancel the current load task if possible.
            cancelLoad();
        }

        /**
         * Handles a request to cancel a load.
         */
        @Override
        public void onCanceled(List<Movie> data) {
            super.onCanceled(data);

            onReleaseResources(data);
        }

        /**
         * Handles a request to completely reset the Loader.
         */
        @Override
        protected void onReset() {
            super.onReset();

            // Ensure the loader is stopped
            onStopLoading();

            // At this point we can release resources
            if (mData != null) {
                onReleaseResources(mData);
                mData = null;
            }
        }

        /**
         * Helper function to take care of releasing resources associated with
         * an actively loaded data set.
         */
        protected void onReleaseResources(List<Movie> data) {
            // For a simple List<> there is nothing to do. For something
            // like a Cursor, we would close it here.
        }
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
