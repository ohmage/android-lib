
package org.ohmage.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import org.ohmage.ConfigHelper;
import org.ohmage.adapters.ResponseListCursorAdapter;
import org.ohmage.adapters.UploadingResponseListCursorAdapter;
import org.ohmage.controls.FilterControl;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbHelper.Tables;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.Response;
import org.ohmage.library.R;
import org.ohmage.logprobe.Analytics;
import org.ohmage.service.UploadService;
import org.ohmage.ui.OhmageFilterable.CampaignFilter;

public class ResponseUploadFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "ResponseUploadFragment";

    private static final int CAMPAIGN_LOADER = 0;

    private Button mUploadButton;

    private FilterControl mCampaignFilter;

    private String mDefaultCampaign;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.response_upload_layout, container, false);

        mUploadButton = (Button) view.findViewById(R.id.upload_button);
        mUploadButton.setOnClickListener(mUploadListener);

        mCampaignFilter = (FilterControl) view.findViewById(R.id.campaign_filter);
        if (mCampaignFilter == null)
            throw new RuntimeException(
                    "Your activity must have a FilterControl with the id campaign_filter");

        mCampaignFilter.setOnChangeListener(new FilterControl.FilterChangeListener() {

            @Override
            public void onFilterChanged(boolean selfChange, String curValue) {
                if (!selfChange)
                    onCampaignFilterChanged(curValue);
            }
        });

        mDefaultCampaign = getActivity().getIntent().getStringExtra(
                CampaignFilter.EXTRA_CAMPAIGN_URN);

        if (mDefaultCampaign == null)
            mCampaignFilter.add(0, new Pair<String, String>(
                    getString(R.string.filter_all_campaigns), null));

        if (!ConfigHelper.isSingleCampaignMode())
            getLoaderManager().initLoader(CAMPAIGN_LOADER, null, this);
        else {
            mCampaignFilter.setVisibility(View.GONE);
        }

        // Show the upload button immediately in single campaign mode since we
        // don't query for the campaign
        if (ConfigHelper.isSingleCampaignMode())
            ensureButtons(view);

        return view;
    }

    protected void onCampaignFilterChanged(String filter) {
        if (getUploadingResponseListFragment() != null) {
            getUploadingResponseListFragment().setCampaignUrn(filter);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        // Populate the filter
        mCampaignFilter.populate(data, Campaigns.CAMPAIGN_NAME, Campaigns.CAMPAIGN_URN);
        mCampaignFilter.add(0, new Pair<String, String>(getString(R.string.filter_all_campaigns),
                null));

        if (mDefaultCampaign != null) {
            mCampaignFilter.setValue(mDefaultCampaign);
            mDefaultCampaign = null;
        }

        ensureButtons(getView());
    }

    private void ensureButtons(View view) {
        view.findViewById(R.id.upload_all_container).setVisibility(View.VISIBLE);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), Campaigns.CONTENT_URI, new String[] {
                Campaigns.CAMPAIGN_URN, Campaigns.CAMPAIGN_NAME
        }, Campaigns.CAMPAIGN_STATUS + "=" + Campaign.STATUS_READY, null, Campaigns.CAMPAIGN_NAME);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCampaignFilter.clearAll();
    }

    private ResponseListFragment getUploadingResponseListFragment() {
        return (UploadingResponseListFragment) getFragmentManager().findFragmentById(
                R.id.upload_queue_response_list_fragment);
    }

    private final OnClickListener mUploadListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Analytics.widget(v);
            Intent intent = new Intent(v.getContext(), UploadService.class);
            intent.setData(Responses.CONTENT_URI);
            WakefulIntentService.sendWakefulWork(v.getContext(), intent);
        }
    };

    public static class UploadingResponseListFragment extends ResponseListFragment {

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // Set the empty text
            setEmptyText(getString(R.string.upload_queue_empty));
        }

        @Override
        protected ResponseListCursorAdapter createAdapter() {
            return new UploadingResponseListCursorAdapter(getActivity(), null, this, 0);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            CursorLoader loader = (CursorLoader) super.onCreateLoader(id, args);

            StringBuilder selection = new StringBuilder(loader.getSelection());
            if (selection.length() != 0)
                selection.append(" AND ");
            selection.append(Tables.RESPONSES + "." + Responses.RESPONSE_STATUS + "!="
                    + Response.STATUS_UPLOADED + " AND " + Tables.RESPONSES + "."
                    + Responses.RESPONSE_STATUS + "!=" + Response.STATUS_DOWNLOADED);
            loader.setSelection(selection.toString());
            return loader;
        }

        @Override
        protected boolean ignoreTimeBounds() {
            return true;
        }
    }
}
