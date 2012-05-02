package com.uwetrottmann.movies.ui;

import com.uwetrottmann.movies.R;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ProgressDialog extends DialogFragment {

    public static ProgressDialog newInstance() {
        ProgressDialog f = new ProgressDialog();
        f.setCancelable(false);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.progress_dialog, container, false);
        return v;
    }
}
