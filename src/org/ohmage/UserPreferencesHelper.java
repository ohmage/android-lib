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

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import org.ohmage.db.DbContract.Responses;
import org.ohmage.library.R;

import java.util.Calendar;

/**
 * Helper class to read data from the users shared preference file
 */
public class UserPreferencesHelper {

    private static final boolean DEFAULT_SHOW_FEEDBACK = true;
    private static final boolean DEFAULT_SHOW_PROFILE = true;
    private static final boolean DEFAULT_SHOW_UPLOAD_QUEUE = true;
    private static final boolean DEFAULT_SHOW_MOBILITY_FEEDBACK = true;

    private static final boolean DEFAULT_UPLOAD_RESPONSES_WIFI_ONLY = false;

    public static final String KEY_UPLOAD_PROBES_WIFI_ONLY = "key_upload_probes_wifi_only";
    public static final String KEY_UPLOAD_RESPONSES_WIFI_ONLY = "key_upload_responses_wifi_only";
    public static final String KEY_SINGLE_CAMPAIGN_MODE = "key_single_campaign_mode";
    public static final String KEY_SHOW_FEEDBACK = "key_show_feedback";
    public static final String KEY_SHOW_PROFILE = "key_show_profile";
    public static final String KEY_SHOW_UPLOAD_QUEUE = "key_show_upload_queue";
    public static final String KEY_SHOW_MOBILITY = "key_show_mobility";
    public static final String KEY_SHOW_MOBILITY_FEEDBACK = "key_show_mobility_feedback";

    private static final String KEY_BASELINE_END_TIME = "key_baseline_end_time";
    private static final String KEY_BASELINE_START_TIME = "key_baseline_start_time";

    private final SharedPreferences mPreferences;

    private static UserPreferencesHelper self;

    public static UserPreferencesHelper getInstance() {
        if (self == null)
            self = new UserPreferencesHelper(OhmageApplication.getContext());
        return self;
    }

    private UserPreferencesHelper(Context activity) {
        mPreferences = getUserSharedPreferences(activity);
    }

    public static SharedPreferences getUserSharedPreferences(Context context) {
        return context.getSharedPreferences(getPreferencesName(context), Context.MODE_PRIVATE);
    }

    public static String getPreferencesName(Context context) {
        return context.getPackageName() + "_user_preferences";
    }

    public static boolean clearAll() {
        return getInstance().mPreferences.edit().clear().commit();
    }

    public static boolean showFeedback() {
        return getInstance().mPreferences.getBoolean(KEY_SHOW_FEEDBACK, DEFAULT_SHOW_FEEDBACK);
    }

    public static boolean showProfile() {
        return getInstance().mPreferences.getBoolean(KEY_SHOW_PROFILE, DEFAULT_SHOW_PROFILE);
    }

    public static boolean showUploadQueue() {
        return getInstance().mPreferences.getBoolean(KEY_SHOW_UPLOAD_QUEUE,
                DEFAULT_SHOW_UPLOAD_QUEUE);
    }

    public static boolean showMobility() {
        return getInstance().mPreferences.getBoolean(KEY_SHOW_MOBILITY,
                ConfigHelper.getDefaultShowMobility());
    }

    public static boolean showMobilityFeedback() {
        return getInstance().mPreferences.getBoolean(KEY_SHOW_MOBILITY_FEEDBACK,
                ConfigHelper.getDefaultShowMobility() && DEFAULT_SHOW_MOBILITY_FEEDBACK);
    }

    public static boolean getUploadProbesWifiOnly() {
        return getInstance().mPreferences.getBoolean(KEY_UPLOAD_PROBES_WIFI_ONLY,
                ConfigHelper.getDefaultUploadProbesWifiOnly());
    }

    public static boolean getUploadResponsesWifiOnly() {
        return getInstance().mPreferences.getBoolean(KEY_UPLOAD_RESPONSES_WIFI_ONLY,
                DEFAULT_UPLOAD_RESPONSES_WIFI_ONLY);
    }

    public static boolean isSingleCampaignMode() {
        return getInstance().mPreferences.getBoolean(KEY_SINGLE_CAMPAIGN_MODE, OhmageApplication
                .getContext().getResources().getBoolean(R.bool.single_campaign_mode));
    }

    /**
     * Returns the baseline, or a time 10 weeks after the start time if it is
     * set, or a time 1 month ago.
     * 
     * @param context
     * @return
     */
    public static long getBaseLineEndTime() {
        long base = getInstance().mPreferences.getLong(KEY_BASELINE_END_TIME, -1);
        if (base == -1) {
            // If baseline is not set, we set it to 10 weeks after the baseline
            // start time
            long startTime = getBaseLineStartTime();
            Calendar cal = Calendar.getInstance();
            if (startTime != 0) {
                // If start time is set, end time is 10 weeks after it
                cal.setTimeInMillis(startTime);
                cal.add(Calendar.DATE, 70);
            } else {
                // If no start time is set, end time is 1 month ago
                Utilities.clearTime(cal);
                cal.add(Calendar.MONTH, -1);
            }
            base = cal.getTimeInMillis();
        }
        return base;
    }

    /**
     * Returns the baseline, or 1 week after the first response, or 0 if not set
     * 
     * @param context
     * @return
     */
    public static long getBaseLineStartTime() {
        long startTime = getInstance().mPreferences.getLong(KEY_BASELINE_START_TIME, -1);
        // If the base line start time isn't set, we set it to 1 week after the
        // first response
        if (startTime == -1) {
            Cursor c = OhmageApplication.getContext().getContentResolver()
                    .query(Responses.CONTENT_URI, new String[] {
                        Responses.RESPONSE_TIME
                    }, null, null, Responses.RESPONSE_TIME + " ASC");
            if (c.moveToFirst()) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(c.getLong(0));
                Utilities.clearTime(cal);
                cal.add(Calendar.DATE, 7);
                startTime = cal.getTimeInMillis();
            } else {
                startTime = 0;
            }
            c.close();
        }
        return startTime;
    }

    public static void clearBaseLineTime(Context context) {
        getInstance().mPreferences.edit().remove(KEY_BASELINE_END_TIME)
                .remove(KEY_BASELINE_START_TIME).commit();
    }
}
