
package com.uwetrottmann.movies.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class MoviesContract {

    private static final String CONTENT_AUTHORITY = "com.uwetrottmann.movies.provider";

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final String PATH_MOVIES = "movies";

    interface MoviesColumns {

    }

    public static class Shows implements MoviesColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_MOVIES)
                .build();

        /** Use if multiple items get returned */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.sgmovies.movie";

        /** Use if a single item is returned */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.sgmovies.movie";

        public static Uri buildMovieUri(int movieId) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(movieId)).build();
        }
    }

    private MoviesContract() {
    }

}
