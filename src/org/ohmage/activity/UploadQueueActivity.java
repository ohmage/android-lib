
package org.ohmage.activity;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.widget.TabHost;

import org.ohmage.fragments.ProbeUploadFragment;
import org.ohmage.fragments.ResponseListFragment.OnResponseActionListener;
import org.ohmage.fragments.ResponseUploadFragment;
import org.ohmage.library.R;
import org.ohmage.ui.BaseActivity;
import org.ohmage.ui.ResponseActivityHelper;
import org.ohmage.ui.TabsAdapter;

public class UploadQueueActivity extends BaseActivity implements OnResponseActionListener {

    private static final String TAG = "UploadQueueActivity";

    TabHost mTabHost;
    ViewPager mViewPager;
    TabsAdapter mTabsAdapter;

    private ResponseActivityHelper mResponseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResponseHelper = new ResponseActivityHelper(this);

        setContentView(R.layout.tab_layout);
        setActionBarShadowVisibility(false);

        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();

        mViewPager = (ViewPager) findViewById(R.id.pager);

        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);

        mTabsAdapter.addTab("Responses", ResponseUploadFragment.class, null);
        mTabsAdapter.addTab("Streams", ProbeUploadFragment.class, null);

        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mTabHost != null)
            outState.putString("tab", mTabHost.getCurrentTabTag());
    }

    @Override
    public void onResponseActionView(Uri responseUri) {
        startActivity(new Intent(Intent.ACTION_VIEW, responseUri));
    }

    @Override
    public void onResponseActionUpload(Uri responseUri) {
        mResponseHelper.queueForUpload(responseUri);
    }

    @Override
    public void onResponseActionError(Uri responseUri, int status) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(ResponseActivityHelper.KEY_URI, responseUri);
        showDialog(status, bundle);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        mResponseHelper.onPrepareDialog(id, dialog, args);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        return mResponseHelper.onCreateDialog(id, args);
    }
}
