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

package com.uwetrottmann.movies.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class MoviesContract {

    public static final String CONTENT_AUTHORITY = "com.uwetrottmann.movies.provider";

    static final String PATH_MOVIES = "movies";

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
        
        String TMDBID = "movie_tmdbid";

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

        public static Uri buildMovieUri(long movieId) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(movieId)).build();
        }

        public static String getId(Uri uri) {
            return uri.getLastPathSegment();
        }
    }

    private MoviesContract() {
    }

}
