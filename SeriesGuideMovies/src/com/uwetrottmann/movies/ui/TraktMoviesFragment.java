
package com.uwetrottmann.movies.ui;

import com.actionbarsherlock.app.SherlockListFragment;
import com.jakewharton.trakt.entities.Movie;
import com.uwetrottmann.movies.R;
import com.uwetrottmann.movies.util.ImageDownloader;
import com.uwetrottmann.movies.util.Utils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraktMoviesFragment extends SherlockListFragment {

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

    private TraktMovieAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // so we don't have to do a network op each config change
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /*
         * never use this here (on config change the view needed before removing
         * the fragment)
         */
        // if (container == null) {
        // return null;
        // }
        return inflater.inflate(R.layout.moviefragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // only create and fill a new adapter if there is no previous one
        // (e.g. after config/page changed)
        if (mAdapter == null) {
            mAdapter = new TraktMovieAdapter(getActivity(), R.layout.movie_row,
                    new ArrayList<Movie>());

            TraktCategory cat = TraktCategory.fromValue(getArguments().getInt(InitBundle.CATEGORY));
            new GetTraktMoviesTask(getActivity()).execute(cat);
        }
    }

    public class GetTraktMoviesTask extends AsyncTask<TraktCategory, Void, List<Movie>> {

        private Context mContext;

        public GetTraktMoviesTask(Context context) {
            mContext = context;
        }

        @Override
        protected List<Movie> doInBackground(TraktCategory... params) {
            TraktCategory type = params[0];
            List<Movie> movies = new ArrayList<Movie>();

            try {
                switch (type) {
                    case TRENDING:
                        movies = Utils.getServiceManager(mContext).movieService().trending().fire();
                        break;
                    default:
                        movies = Utils.getServiceManager(mContext).movieService().trending().fire();
                        break;
                }
            } catch (Exception e) {
                // we don't care
            }

            return movies;
        }

        @Override
        protected void onPostExecute(List<Movie> result) {
            setMovieList(result);
        }
    }

    private void setMovieList(List<Movie> results) {
        mAdapter.clear();
        if (Utils.isHoneycombOrHigher()) {
            mAdapter.addAll(results);
        } else {
            for (Movie movie : results) {
                mAdapter.add(movie);
            }
        }
        setListAdapter(mAdapter);
    }

    public class TraktMovieAdapter extends ArrayAdapter<Movie> {

        private LayoutInflater mLayoutInflater;

        private int mLayout;

        private ImageDownloader mImageDownloader;

        public TraktMovieAdapter(Context context, int layout, List<Movie> objects) {
            super(context, layout, objects);
            mLayoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mLayout = layout;
            mImageDownloader = ImageDownloader.getInstance(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(mLayout, null);

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
    }

    public final class ViewHolder {

        public TextView title;

        public TextView description;

        public ImageView poster;
    }

}
