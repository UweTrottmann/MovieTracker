
package com.uwetrottmann.movies.ui;

import com.actionbarsherlock.app.SherlockFragment;
import com.uwetrottmann.movies.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MovieDetailsFragment extends SherlockFragment {

    public interface InitBundle {
        String IMDBID = "imdbid";
    }

    public static MovieDetailsFragment newInstance(String imdbId) {
        MovieDetailsFragment f = new MovieDetailsFragment();

        Bundle args = new Bundle();
        args.putString(InitBundle.IMDBID, imdbId);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        View v = inflater.inflate(R.layout.details_fragment, container, false);
        return v;
    }
}
