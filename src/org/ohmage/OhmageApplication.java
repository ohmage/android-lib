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
import android.content.Context;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.os.Handler;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.android.imageloader.BitmapContentHandler;
import com.google.android.imageloader.ImageLoader;

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

import java.io.IOException;
import java.net.ContentHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class OhmageApplication extends Application {

    private static final String TAG = "OhmageApplication";

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

    private static final int IMAGE_TASK_LIMIT = 3;

    // 50% of available memory, up to a maximum of 32MB
    private static final long IMAGE_CACHE_SIZE = Math.min(Runtime.getRuntime().maxMemory() / 2,
            32 * 1024 * 1024);

    /**
     * 10MB max for cached thumbnails
     */
    public static final int MAX_DISK_CACHE_SIZE = 10 * 1024 * 1024;

    private ImageLoader mImageLoader;

    private static OhmageApplication self;

    private static ContentResolver mFakeContentResolver;

    private static AndroidHttpClient mHttpClient;

    private static AccountManager mAccountManager;

    private ConfigHelper config;

    private UserPreferencesHelper userPrefs;

    @Override
    public void onCreate() {
        super.onCreate();
        Analytics.activity(this, Status.ON);

        self = this;

        config = new ConfigHelper(this);
        userPrefs = new UserPreferencesHelper(this);

        LogProbe.setLevel(config.getLogAnalytics(), config.getLogLevel());
        LogProbe.get(this);

        mImageLoader = createImageLoader(this);

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

    /**
     * Configures some settings based on the deployment. Looks at the server url
     * and deployment name to figure out what the settings should be
     * 
     * @param server
     */
    public void configureForDeployment(String server) {
        if (server == null)
            return;

        server = server.split(" ")[0];

        ConfigHelper config = new ConfigHelper(this);

        if ("https://lausd.mobilizingcs.org/".equals(server)) {
            userPrefs.setShowFeedback(true);
            userPrefs.setShowMobility(false);
            userPrefs.setUploadResponsesWifiOnly(false);
            userPrefs.setUploadProbesWifiOnly(true);
            config.setAdminMode(false);
            config.setLogLevel("verbose");
            config.setLogAnalytics(true);
            updateLogLevel();
        } else if ("https://pilots.mobilizelabs.org/".equals(server)) {
            userPrefs.setShowFeedback(true);
            userPrefs.setShowMobility(false);
            userPrefs.setUploadResponsesWifiOnly(false);
            userPrefs.setUploadProbesWifiOnly(true);
            config.setAdminMode(false);
            config.setLogLevel("error");
            config.setLogAnalytics(false);
            updateLogLevel();
        } else if ("https://dev.ohmage.org/".equals(server)
                || "https://test.ohmage.org/".equals(server)) {
            userPrefs.setShowFeedback(true);
            userPrefs.setShowMobility(true);
            userPrefs.setUploadResponsesWifiOnly(false);
            userPrefs.setUploadProbesWifiOnly(false);
            config.setAdminMode(true);
            config.setLogLevel("verbose");
            config.setLogAnalytics(true);
            updateLogLevel();
        } else if ("https://play.ohmage.org/".equals(server)) {
            userPrefs.setShowFeedback(true);
            userPrefs.setShowMobility(true);
            userPrefs.setUploadResponsesWifiOnly(false);
            userPrefs.setUploadProbesWifiOnly(true);
            config.setAdminMode(true);
            config.setLogLevel("error");
            config.setLogAnalytics(false);
            updateLogLevel();
        }
    }

    public void updateLogLevel() {
        ConfigHelper config = new ConfigHelper(this);
        LogProbe.setLevel(config.getLogAnalytics(), config.getLogLevel());
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
        new UserPreferencesHelper(this).clearAll();

        // clear all deployment settings
        new ConfigHelper(this).clearDeploymentSettings();

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

        // clear images
        try {
            Utilities.delete(getExternalCacheDir());
        } catch (IOException e) {
            Log.e(TAG, "Error deleting external cache directory", e);
        }

        try {
            Utilities.delete(getCacheDir());
        } catch (IOException e) {
            Log.e(TAG, "Error deleting cache directory", e);
        }
    }

    private static ImageLoader createImageLoader(Context context) {
        // Install the file cache (if it is not already installed)
        OhmageCache.install(context);
        checkCacheUsage();

        // Just use the default URLStreamHandlerFactory because
        // it supports all of the required URI schemes (http).
        URLStreamHandlerFactory streamFactory = null;

        // Load images using a BitmapContentHandler
        // and cache the image data in the file cache.
        ContentHandler bitmapHandler = OhmageCache.capture(new BitmapContentHandler(), null);

        // For pre-fetching, use a "sink" content handler so that the
        // the binary image data is captured by the cache without actually
        // parsing and loading the image data into memory. After pre-fetching,
        // the image data can be loaded quickly on-demand from the local cache.
        ContentHandler prefetchHandler = OhmageCache.capture(OhmageCache.sink(), null);

        // Perform callbacks on the main thread
        Handler handler = null;

        return new ImageLoader(IMAGE_TASK_LIMIT, streamFactory, bitmapHandler, prefetchHandler,
                IMAGE_CACHE_SIZE, handler);
    }

    /**
     * check the cache usage
     */
    public static void checkCacheUsage() {
        OhmageCache.checkCacheUsage(self, MAX_DISK_CACHE_SIZE);
    }

    @Override
    public void onTerminate() {
        if (mHttpClient != null) {
            mHttpClient.close();
            mHttpClient = null;
        }
        LogProbe.close(this);
        mImageLoader = null;
        Analytics.activity(this, Status.OFF);
        super.onTerminate();
    }

    @Override
    public Object getSystemService(String name) {
        if (ImageLoader.IMAGE_LOADER_SERVICE.equals(name)) {
            return mImageLoader;
        } else {
            return super.getSystemService(name);
        }
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
    public static Application getContext() {
        return self;
    }

    /**
     * Determines if this is a build for developers
     * @return
     */
    public static boolean isDebugBuild() {
        return "org.ohmage.dev".equals(getContext().getPackageName());
    }

    public static AndroidHttpClient getHttpClient() {
        if (mHttpClient == null)
            mHttpClient = AndroidHttpClient.newInstance(Build.MANUFACTURER + " " + Build.MODEL
                    + " (" + Build.VERSION.RELEASE + ")");
        return mHttpClient;
    }

    public static AccountManager getAccountManager() {
        if (mAccountManager == null)
            mAccountManager = AccountManager.get(self);
        return mAccountManager;
    }
}
