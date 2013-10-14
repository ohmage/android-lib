
package org.ohmage.ui;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import org.ohmage.AccountHelper;
import org.ohmage.UIUtils;
import org.ohmage.activity.DashboardActivity;
import org.ohmage.library.R;
import org.ohmage.logprobe.Analytics;
import org.ohmage.logprobe.Log;
import org.ohmage.logprobe.LogProbe.Status;

/**
 * This ListActivity is needed some some of the components for reminders doesn't
 * use fragments
 * 
 * @author Cameron Ketcham
 */
public abstract class BaseListActivity extends SherlockListActivity {

    private static final String TAG = "BaseListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (!AccountHelper.accountExists()) {
            Log.v(TAG, "no credentials saved, so launch Login");
            startActivity(AccountHelper.getLoginIntent(this));
            finish();
            return;
        }
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        setSupportProgressBarIndeterminateVisibility(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Analytics.activity(this, Status.ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Analytics.activity(this, Status.OFF);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Analytics.widget(this, "Menu Button");
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // Analytics.widget(item);
            startActivity(new Intent(this, DashboardActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            if (!UIUtils.isHoneycomb()) {
                overridePendingTransition(R.anim.home_enter, R.anim.home_exit);
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
