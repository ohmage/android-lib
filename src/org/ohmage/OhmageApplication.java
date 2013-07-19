/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.ohmage;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.os.Build;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.commonsware.cwac.wakeful.WakefulIntentService;

import org.apache.http.params.HttpConnectionParams;
import org.ohmage.authenticator.Authenticator;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbHelper;
import org.ohmage.db.Models.Response;
import org.ohmage.library.R;
import org.ohmage.logprobe.Analytics;
import org.ohmage.logprobe.Log;
import org.ohmage.logprobe.LogProbe;
import org.ohmage.logprobe.LogProbe.Status;
import org.ohmage.prompt.multichoicecustom.MultiChoiceCustomDbAdapter;
import org.ohmage.prompt.singlechoicecustom.SingleChoiceCustomDbAdapter;
import org.ohmage.responsesync.ResponseSyncService;
import org.ohmage.service.SurveyGeotagService;
import org.ohmage.service.UploadService;
import org.ohmage.triggers.glue.TriggerFramework;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class OhmageApplication extends Application {

    private static final String TAG = "OhmageApplication";

    public static final boolean DEBUG_BUILD = true;

    /**
     * Account type string.
     */
    public static final String ACCOUNT_TYPE = "org.ohmage";

    /**
     * Authtoken type string.
     */
    public static final String AUTHTOKEN_TYPE = "org.ohmage";

    public static final String VIEW_MAP = "ohmage.intent.action.VIEW_MAP";

    public static final String ACTION_VIEW_REMOTE_IMAGE = "org.ohmage.action.VIEW_REMOTE_IMAGE";

    /**
     * Get max available VM memory, exceeding this amount will throw an
     * OutOfMemory exception. Stored in kilobytes as LruCache takes an
     * int in its constructor.
     */
    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

    /**
     * A bitmap cache to use for response images
     */
    private final BitmapLruCache mImageCache = new BitmapLruCache(maxMemory/8);

    /**
     * An image loader which caches less important user images to disk
     */
    private ImageLoader mImageLoader;
    /**
     * The {@link RequestQueue} that handles response images
     */
    private RequestQueue mRequestQueue;

    private static OhmageApplication self;

    private static ContentResolver mFakeContentResolver;

    private static AndroidHttpClient mHttpClient;

    private static AccountManager mAccountManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Analytics.activity(this, Status.ON);

        self = this;

        updateLogLevel();
        LogProbe.get(this);

        mRequestQueue = Volley.newRequestQueue(this);
        mImageLoader = new ImageLoader(mRequestQueue, mImageCache);

        // Initialize background components which consists of triggers
        BackgroundManager.initComponents(this);

        verifyState();

        // If they can't set a custom server, verify the server that is set is
        // the first in the list of servers
        if (!getResources().getBoolean(R.bool.allow_custom_server)) {
            List<String> servers = Arrays.asList(getResources().getStringArray(R.array.servers));
            if (servers.isEmpty())
                throw new RuntimeException("At least one server must be specified in config.xml");
            ConfigHelper.setServerUrl(servers.get(0));
        }
    }

    public void updateLogLevel() {
        LogProbe.setLevel(ConfigHelper.getLogAnalytics(), ConfigHelper.getLogLevel());
    }

    /**
     * This method verifies that the state of ohmage is correct when it starts
     * up. fixes response state for crashes while waiting for:
     * <ul>
     * <li>location from the {@link SurveyGeotagService}, waiting for location
     * status</li>
     * <li>{@link UploadService}, uploading or queued status</li>
     * <ul>
     * It also deletes any responses which have no uuid
     */
    private void verifyState() {
        ContentValues values = new ContentValues();
        values.put(Responses.RESPONSE_STATUS, Response.STATUS_STANDBY);
        getContentResolver().update(
                Responses.CONTENT_URI,
                values,
                Responses.RESPONSE_STATUS + "=" + Response.STATUS_QUEUED + " OR "
                        + Responses.RESPONSE_STATUS + "=" + Response.STATUS_UPLOADING + " OR "
                        + Responses.RESPONSE_STATUS + "=" + Response.STATUS_WAITING_FOR_LOCATION,
                null);

        if (getContentResolver().delete(Responses.CONTENT_URI,
                Responses.RESPONSE_UUID + " is null", null) != 0) {
            // If there were some responses with no uuid, start the feedback
            // service
            Intent fbIntent = new Intent(getContext(), ResponseSyncService.class);
            WakefulIntentService.sendWakefulWork(getContext(), fbIntent);
        }
    }

    public void resetAll() {
        // clear everything?
        Log.v(TAG, "Reseting all data");

        // clear the user account
        AccountManager accountManager = AccountManager.get(self);
        Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
        final CountDownLatch latch = new CountDownLatch(accounts.length);
        Authenticator.setAllowRemovingAccounts(true);
        for (Account account : accounts) {
            accountManager.removeAccount(account, new AccountManagerCallback<Boolean>() {

                @Override
                public void run(AccountManagerFuture<Boolean> future) {
                    latch.countDown();
                }
            }, null);
        }

        // clear user prefs
        UserPreferencesHelper.clearAll();

        // clear all global preferences
        new PreferenceStore(this).edit().clear().commit();

        // clear triggers
        TriggerFramework.resetAllTriggerSettings(this);

        // delete campaign specific settings
        CampaignPreferencesHelper.clearAll(this);

        // clear db
        new DbHelper(this).clearAll();

        // clear custom type dbs
        SingleChoiceCustomDbAdapter singleChoiceDbAdapter = new SingleChoiceCustomDbAdapter(this);
        if (singleChoiceDbAdapter.open()) {
            singleChoiceDbAdapter.clearAll();
            singleChoiceDbAdapter.close();
        }
        MultiChoiceCustomDbAdapter multiChoiceDbAdapter = new MultiChoiceCustomDbAdapter(this);
        if (multiChoiceDbAdapter.open()) {
            multiChoiceDbAdapter.clearAll();
            multiChoiceDbAdapter.close();
        }

        // clear local images
        Utilities.delete(getExternalCacheDir());

        // clear cached images
        mRequestQueue.getCache().clear();
    }

    /**
     * Returns the default {@link ImageLoader} which can be used to download temporary images
     * @return
     */
    public static ImageLoader getImageLoader() {
        return self.mImageLoader;
    }

    /**
     * Returns a reference to the default {@link RequestQueue}
     * @return
     */
    public static RequestQueue getRequestQueue() {
        return self.mRequestQueue;
    }

    @Override
    public void onTerminate() {
        if (mHttpClient != null) {
            mHttpClient.close();
            mHttpClient = null;
        }
        LogProbe.close(this);
        Analytics.activity(this, Status.OFF);
        super.onTerminate();
    }

    public static void setFakeContentResolver(ContentResolver resolver) {
        mFakeContentResolver = resolver;
    }

    public static ContentResolver getFakeContentResolver() {
        return mFakeContentResolver;
    }

    @Override
    public ContentResolver getContentResolver() {
        if (mFakeContentResolver != null)
            return mFakeContentResolver;
        return super.getContentResolver();
    }

    /**
     * Static reference from the Application to return the context
     * 
     * @return the application context
     */
    public static OhmageApplication getContext() {
        return self;
    }

    public static AndroidHttpClient getHttpClient() {
        if (mHttpClient == null) {
            mHttpClient = AndroidHttpClient.newInstance(Build.MANUFACTURER + " " + Build.MODEL
                    + " (" + Build.VERSION.RELEASE + ")");
            HttpConnectionParams.setSoTimeout(mHttpClient.getParams(), 60000);
        }
        return mHttpClient;
    }

    public static AccountManager getAccountManager() {
        if (mAccountManager == null)
            mAccountManager = AccountManager.get(self);
        return mAccountManager;
    }
}
