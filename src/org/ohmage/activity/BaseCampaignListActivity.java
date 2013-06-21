
package org.ohmage.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.ohmage.OhmageApi.CampaignReadResponse;
import org.ohmage.async.CampaignReadTask;
import org.ohmage.async.CampaignXmlDownloadTask;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.Models.Campaign;
import org.ohmage.fragments.CampaignListFragment.OnCampaignActionListener;
import org.ohmage.library.R;
import org.ohmage.ui.BaseSingleFragmentActivity;
import org.ohmage.ui.OhmageFilterable.CampaignFilter;

public class BaseCampaignListActivity extends BaseSingleFragmentActivity implements
        OnCampaignActionListener,
        LoaderManager.LoaderCallbacks<CampaignReadResponse> {

    static final String TAG = "BaseCampaignListActivity";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.refresh, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            getSupportLoaderManager().restartLoader(0, null, this);
            setSupportProgressBarIndeterminateVisibility(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                        Campaign.setCampaignsRemote(BaseCampaignListActivity.this, campaignUrnForDialogs);
                    }
                });

        return builder.create();
    }

    private String campaignUrnForDialogs;

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        super.onPrepareDialog(id, dialog, args);
        campaignUrnForDialogs = args.getString("campaign_urn");
    }

    @Override
    public Loader<CampaignReadResponse> onCreateLoader(int arg0, Bundle arg1) {
        return new CampaignReadTask(this);
    }

    @Override
    public void onLoadFinished(Loader<CampaignReadResponse> loader, CampaignReadResponse data) {
        setSupportProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onLoaderReset(Loader<CampaignReadResponse> arg0) {
        // Nothing to reset
    }
}
