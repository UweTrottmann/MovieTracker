
package com.uwetrottmann.movies.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class MoviesContract {

    static final String PATH_MOVIES = "movies";

    static final String CONTENT_AUTHORITY = "com.uwetrottmann.movies.provider";

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    interface MoviesColumns {
        String TITLE = "movie_title";

        String YEAR = "movie_year";

        String RELEASED = "movie_released";

        String URL = "movie_url";

        String TRAILER = "movie_trailer";

        String RUNTIME = "movie_runtime";

        String TAGLINE = "movie_tagline";

        String OVERVIEW = "movie_overview";

        String CERTIFICATION = "movie_certification";

        String IMDBID = "movie_imdbid";

        String LASTUPDATED = "movie_lastupdate";

        String POSTER = "movie_poster";

        String FANART = "movie_fanart";

        String GENRES = "movie_genres";

        String RATINGS_PERCENTAGE = "movie_ratepercentage";

        String RATINGS_VOTES = "movie_ratevotes";

        String WATCHED = "movie_watched";

        String INWATCHLIST = "movie_inwatchlist";

        String INCOLLECTION = "movie_incollecetion";
    }

    public static class Movies implements MoviesColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_MOVIES)
                .build();

        /** Use if multiple items get returned */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.sgmovies.movie";

        /** Use if a single item is returned */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.sgmovies.movie";

        public static Uri buildMovieUri(int movieId) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(movieId)).build();
        }

        public static String getId(Uri uri) {
            return uri.getLastPathSegment();
        }
    }

    private MoviesContract() {
    }

}
