
package org.ohmage.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.async.CampaignReadTask;
import org.ohmage.async.CampaignXmlDownloadTask;
import org.ohmage.controls.ActionBarControl.ActionListener;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;
import org.ohmage.fragments.CampaignListFragment.OnCampaignActionListener;
import org.ohmage.library.R;
import org.ohmage.ui.BaseSingleFragmentActivity;
import org.ohmage.ui.OhmageFilterable.CampaignFilter;

public class BaseCampaignListActivity extends BaseSingleFragmentActivity implements
        OnCampaignActionListener, ActionListener,
        LoaderManager.LoaderCallbacks<CampaignReadResponse> {

    static final String TAG = "BaseCampaignListActivity";

    // action bar commands
    protected static final int ACTION_REFRESH_CAMPAIGNS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();

        // throw some actions on it
        getActionBarControl().addActionBarCommand(ACTION_REFRESH_CAMPAIGNS,
                getString(R.string.campaign_list_refresh_action_button_description),
                R.drawable.btn_title_refresh);

        // and attach handlers for said actions
        getActionBarControl().setOnActionListener(this);
    }

    @Override
    public void onCampaignActionView(String campaignUrn) {
        Intent i = new Intent(this, CampaignInfoActivity.class);
        i.setData(Campaigns.buildCampaignUri(campaignUrn));
        startActivity(i);
    }

    @Override
    public void onCampaignActionDownload(final String campaignUrn) {
        new CampaignXmlDownloadTask(BaseCampaignListActivity.this, campaignUrn).startLoading();
    }

    @Override
    public void onCampaignActionSurveys(String campaignUrn) {
        Intent intent = new Intent(this, SurveyListActivity.class);
        intent.putExtra(CampaignFilter.EXTRA_CAMPAIGN_URN, campaignUrn);
        startActivity(intent);
    }

    @Override
    public void onCampaignActionError(String campaignUrn, int status) {
        Bundle bundle = new Bundle();
        bundle.putString("campaign_urn", campaignUrn);
        showDialog(status, bundle);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int, android.os.Bundle)
     */
    @Override
    protected Dialog onCreateDialog(final int id, Bundle args) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (id) {
            case Campaign.STATUS_STOPPED:
                builder.setMessage(R.string.campaign_list_campaign_stopped);
                break;
            case Campaign.STATUS_OUT_OF_DATE:
                builder.setMessage(R.string.campaign_list_campaign_out_of_date);
                break;
            case Campaign.STATUS_INVALID_USER_ROLE:
                builder.setMessage(R.string.campaign_list_campaign_invalid_user_role);
                break;
            case Campaign.STATUS_NO_EXIST:
                builder.setMessage(R.string.campaign_list_campaign_no_exist);
                break;
            default:
                builder.setMessage(R.string.campaign_list_campaign_unavailable);
        }

        builder.setCancelable(true).setNegativeButton(R.string.ignore, null)
                .setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Campaign.setRemote(BaseCampaignListActivity.this, campaignUrnForDialogs);
                    }
                });

        return builder.create();
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        super.onPrepareDialog(id, dialog, args);
        campaignUrnForDialogs = args.getString("campaign_urn");
    }

    private String campaignUrnForDialogs;

    @Override
    public void onActionClicked(int commandID) {
        switch (commandID) {
            case ACTION_REFRESH_CAMPAIGNS:
                getSupportLoaderManager().restartLoader(0, null, this);
                getActionBarControl().setProgressVisible(true);
                break;
        }
    }

    @Override
    public Loader<CampaignReadResponse> onCreateLoader(int arg0, Bundle arg1) {
        return new CampaignReadTask(this);
    }

    @Override
    public void onLoadFinished(Loader<CampaignReadResponse> loader, CampaignReadResponse data) {
        getActionBarControl().setProgressVisible(false);
    }

    @Override
    public void onLoaderReset(Loader<CampaignReadResponse> arg0) {
        // Nothing to reset
    }
}
