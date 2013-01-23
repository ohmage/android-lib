
package org.ohmage.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.ohmage.fragments.CampaignListFragment;
import org.ohmage.library.R;
import org.ohmage.logprobe.Log;

public class CampaignListActivity extends BaseCampaignListActivity {

    static final String TAG = "CampaignListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentFragment(new CampaignListFragment());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.add_campaign, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_add_campaign) {
            startActivity(new Intent(CampaignListActivity.this, CampaignAddActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCampaignActionDownload(String campaignUrn) {
        Toast.makeText(this, R.string.campaign_list_download_action_invalid, Toast.LENGTH_SHORT)
                .show();
        Log.w(TAG, "onCampaignActionDownload should not be exposed in this activity.");
    }
}
