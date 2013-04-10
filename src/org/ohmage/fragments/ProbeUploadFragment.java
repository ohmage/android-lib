
package org.ohmage.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import org.ohmage.AccountHelper;
import org.ohmage.PreferenceStore;
import org.ohmage.library.R;
import org.ohmage.logprobe.Analytics;
import org.ohmage.probemanager.DbContract.BaseProbeColumns;
import org.ohmage.probemanager.DbContract.ProbeCount;
import org.ohmage.probemanager.DbContract.Probes;
import org.ohmage.service.ProbeUploadService;
import org.ohmage.ui.BaseActivity;

public class ProbeUploadFragment extends Fragment implements LoaderCallbacks<Cursor> {

    private static final String TAG = "ProbeUploadFragment";

    private static final int UPLOAD_LOADER = 0;

    private Button mUploadButton;

    private PreferenceStore mPrefHelper;

    private AccountHelper mAccount;

    private TextView mUploadCountText;

    private TextView mLastUploadText;

    private final String emptyValue = "-";

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mPrefHelper = new PreferenceStore(getActivity());
        mAccount = new AccountHelper(getActivity());

        setLastUploadTimestamp();

        getLoaderManager().initLoader(UPLOAD_LOADER, null, this);
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().registerReceiver(mMobilityUploadReceiver,
                new IntentFilter(ProbeUploadService.PROBE_UPLOAD_STARTED));
        getActivity().registerReceiver(mMobilityUploadReceiver,
                new IntentFilter(ProbeUploadService.PROBE_UPLOAD_ERROR));
        getActivity().registerReceiver(mMobilityUploadReceiver,
                new IntentFilter(ProbeUploadService.RESPONSE_UPLOAD_ERROR));
        getActivity().registerReceiver(mMobilityUploadReceiver,
                new IntentFilter(ProbeUploadService.PROBE_UPLOAD_SERVICE_FINISHED));
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mMobilityUploadReceiver);
    }

    private final BroadcastReceiver mMobilityUploadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (getActivity() instanceof BaseActivity)
                ((BaseActivity) getActivity()).getActionBarControl().setProgressVisible(
                        ProbeUploadService.PROBE_UPLOAD_STARTED.equals(action));

            mUploadButton.setEnabled(!ProbeUploadService.PROBE_UPLOAD_STARTED.equals(action));

            if (ProbeUploadService.PROBE_UPLOAD_STARTED.equals(action)) {
                mUploadButton.setText("Uploading...");
            } else if (ProbeUploadService.PROBE_UPLOAD_ERROR.equals(action)
                    || ProbeUploadService.RESPONSE_UPLOAD_ERROR.equals(action)) {
                String error = intent.getStringExtra(ProbeUploadService.EXTRA_PROBE_ERROR);
                if (error == null)
                    Toast.makeText(getActivity(), R.string.mobility_network_error_message,
                            Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getActivity(),
                            getString(R.string.mobility_upload_error_message, error),
                            Toast.LENGTH_SHORT).show();
            } else if (ProbeUploadService.PROBE_UPLOAD_SERVICE_FINISHED.equals(action)) {
                mUploadButton.setText("Upload Now");
                setLastUploadTimestamp();
            }
        }
    };

    private void setLastUploadTimestamp() {
        long lastMobilityUploadTimestamp = mPrefHelper.getLastProbeUploadTimestamp();
        if (lastMobilityUploadTimestamp == 0) {
            mLastUploadText.setText(emptyValue);
        } else {
            mLastUploadText.setText(DateFormat.format("yyyy-MM-dd kk:mm:ss",
                    lastMobilityUploadTimestamp));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.probe_upload_layout, container, false);

        mUploadButton = (Button) view.findViewById(R.id.upload_button);
        mUploadButton.setOnClickListener(mUploadListener);
        view.findViewById(R.id.upload_all_container).setVisibility(View.VISIBLE);

        Fragment newFragment = new ProbeUploadingListFragment();
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.add(R.id.upload_queue_response_list_fragment, newFragment).commit();

        mUploadCountText = (TextView) view.findViewById(R.id.mobility_count);
        mLastUploadText = (TextView) view.findViewById(R.id.last_upload);

        return view;
    }

    private ProbeUploadingListFragment getProbeUploadingListFragment() {
        return (ProbeUploadingListFragment) getChildFragmentManager().findFragmentById(
                R.id.upload_queue_response_list_fragment);
    }

    private final OnClickListener mUploadListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Analytics.widget(v);
            Intent intent = new Intent(getActivity(), ProbeUploadService.class);
            WakefulIntentService.sendWakefulWork(getActivity(), intent);
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String username = mAccount.getUsername();

        switch (id) {
            case UPLOAD_LOADER:
                return new CursorLoader(getActivity(), Probes.CONTENT_URI, new String[] {
                    BaseColumns._ID
                }, BaseProbeColumns.USERNAME + "=?", new String[] {
                    username
                }, null);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        switch (loader.getId()) {
            case UPLOAD_LOADER:
                if (data != null)
                    mUploadCountText.setText(String.valueOf(data.getCount()));
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        switch (loader.getId()) {
            case UPLOAD_LOADER:
                mUploadCountText.setText(emptyValue);
                break;
        }
    }

    public static class ProbeUploadingListFragment extends ListFragment implements
            LoaderCallbacks<Cursor> {

        public class ProbeListCursorAdapter extends CursorAdapter {

            private final LayoutInflater mInflater;

            public ProbeListCursorAdapter(Context context) {
                super(context, null, 0);
                mInflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                TextView observerText = (TextView) view.findViewById(R.id.main_text);
                TextView countText = (TextView) view.findViewById(R.id.sub_text);
                final String observer_id = cursor.getString(0);
                final String observer_version = cursor.getString(1);

                observerText.setText(observer_id);
                countText.setText(cursor.getString(2));

                view.findViewById(R.id.icon_image).setVisibility(View.GONE);
                view.findViewById(R.id.action_separator).setVisibility(View.VISIBLE);
                ImageView actionButton = (ImageView) view.findViewById(R.id.action_button);
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setFocusable(false);
                actionButton.setEnabled(true);

                actionButton.setContentDescription(getActivity().getString(
                        R.string.probe_list_item_action_button_upload_description));
                actionButton.setImageResource(R.drawable.subaction_upload_response);
                actionButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Analytics.widget(v);
                        Intent intent = new Intent(getActivity(), ProbeUploadService.class);
                        intent.putExtra(ProbeUploadService.EXTRA_OBSERVER_ID, observer_id);
                        intent.putExtra(ProbeUploadService.EXTRA_OBSERVER_VERSION, observer_version);
                        WakefulIntentService.sendWakefulWork(getActivity(), intent);
                    }
                });

            }

            @Override
            public View newView(Context context, Cursor c, ViewGroup parent) {
                return mInflater.inflate(R.layout.ohmage_list_item, null);
            }
        }

        private static final String[] PROJECTION = new String[] {
                Probes.OBSERVER_ID + " as _id", Probes.OBSERVER_VERSION, "count(*)"
        };

        private ProbeListCursorAdapter mAdapter;

        private AccountHelper mAccountHelper;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            mAccountHelper = new AccountHelper(getActivity());

            mAdapter = new ProbeListCursorAdapter(getActivity());
            setListAdapter(mAdapter);

            getLoaderManager().initLoader(0, null, this);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(getActivity(), ProbeCount.CONTENT_URI, PROJECTION,
                    BaseProbeColumns.USERNAME + "=?", new String[] {
                        mAccountHelper.getUsername()
                    }, null);
        }

        @Override
        public void onLoadFinished(Loader loader, Cursor data) {
            mAdapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader loader) {
            mAdapter.swapCursor(null);
        }
    }
}
