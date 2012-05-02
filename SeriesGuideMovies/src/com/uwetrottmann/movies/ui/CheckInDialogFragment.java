
package com.uwetrottmann.movies.ui;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.uwetrottmann.movies.R;
import com.uwetrottmann.movies.util.AnalyticsUtils;
import com.uwetrottmann.movies.util.TraktCredentialsDialogFragment;
import com.uwetrottmann.movies.util.TraktTask;
import com.uwetrottmann.movies.util.Utils;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

public class CheckInDialogFragment extends SherlockDialogFragment {

    interface InitBundle {
        String IMDBID = "imdbid";

        String TITLE = "title";
    }

    public static CheckInDialogFragment newInstance(String imdbId, String title) {
        CheckInDialogFragment f = new CheckInDialogFragment();

        Bundle args = new Bundle();
        args.putString(InitBundle.IMDBID, imdbId);
        args.putString(InitBundle.TITLE, title);
        f.setArguments(args);

        return f;
    }

    protected boolean mGetGlueChecked;

    protected boolean mTraktChecked;

    private CompoundButton mToggleTraktButton;

    private CompoundButton mToggleGetGlueButton;

    private EditText mMessageBox;

    private View mCheckinButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AnalyticsUtils.getInstance(getActivity()).trackPageView("/CheckInDialog");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getSherlockActivity());

        getDialog().setTitle(R.string.checkin);
        final View layout = inflater.inflate(R.layout.checkin_dialog, null);

        final String imdbId = getArguments().getString(InitBundle.IMDBID);

        // get share service enabled settings
        mGetGlueChecked = prefs.getBoolean(AppPreferences.KEY_SHAREWITHGETGLUE, false);
        mTraktChecked = prefs.getBoolean(AppPreferences.KEY_SHAREWITHTRAKT, false);

        // Message box
        mMessageBox = (EditText) layout.findViewById(R.id.message);

        // Paste title button
        final String title = getArguments().getString(InitBundle.TITLE);
        layout.findViewById(R.id.pasteTitle).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int start = mMessageBox.getSelectionStart();
                int end = mMessageBox.getSelectionEnd();
                mMessageBox.getText().replace(Math.min(start, end), Math.max(start, end), title, 0,
                        title.length());
            }
        });

        // GetGlue toggle
        // TODO

        // trakt toggle
        mToggleTraktButton = (CompoundButton) layout.findViewById(R.id.toggleTrakt);
        mToggleTraktButton.setChecked(mTraktChecked);
        mToggleTraktButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!Utils.isTraktCredentialsValid(getActivity())) {
                        // authenticate already here
                        TraktCredentialsDialogFragment newFragment = TraktCredentialsDialogFragment
                                .newInstance();
                        newFragment.show(getFragmentManager(), "traktdialog");
                    }
                }

                mTraktChecked = isChecked;
                prefs.edit().putBoolean(AppPreferences.KEY_SHAREWITHTRAKT, isChecked).commit();
                updateCheckInButtonState();
            }
        });

        // Check in button
        mCheckinButton = layout.findViewById(R.id.checkinButton);
        updateCheckInButtonState();
        mCheckinButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imdbId == null || imdbId.length() == 0) {
                    return;
                }
                if (!Utils.isNetworkConnected(getActivity())) {
                    Toast.makeText(getActivity(), R.string.offline, Toast.LENGTH_LONG).show();
                    return;
                }

                final String message = mMessageBox.getText().toString();

                if (mGetGlueChecked) {
                    // TODO
                    // if (!GetGlue.isAuthenticated(prefs)) {
                    // // cancel if required auth data is missing
                    // mToggleGetGlueButton.setChecked(false);
                    // mGetGlueChecked = false;
                    // updateCheckInButtonState();
                    // return;
                    // } else {
                    // // check in
                    // new CheckInTask(imdbid, message,
                    // getActivity()).execute();
                    // }
                }

                if (mTraktChecked) {
                    if (!Utils.isTraktCredentialsValid(getActivity())) {
                        // cancel if required auth data is missing
                        mToggleTraktButton.setChecked(false);
                        mTraktChecked = false;
                        updateCheckInButtonState();
                        return;
                    } else {
                        // check in

                        // We want to remove any currently showing
                        // dialog, so make our own transaction and
                        // take care of that here.
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        Fragment prev = getFragmentManager().findFragmentByTag("progress-dialog");
                        if (prev != null) {
                            ft.remove(prev);
                        }
                        ProgressDialog newFragment = ProgressDialog.newInstance();
                        newFragment.show(ft, "progress-dialog");

                        // start the trakt check in task
                        new TraktTask(getActivity(), getFragmentManager(), null).checkin(imdbId,
                                message).execute();
                    }
                }

                dismiss();
            }
        });

        return layout;
    }

    private void updateCheckInButtonState() {
        if (mGetGlueChecked || mTraktChecked) {
            mCheckinButton.setEnabled(true);
        } else {
            mCheckinButton.setEnabled(false);
        }
    }
}
