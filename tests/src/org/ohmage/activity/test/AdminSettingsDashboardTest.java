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

package org.ohmage.activity.test;

import android.content.SharedPreferences;
import android.test.ActivityInstrumentationTestCase2;

import com.jayway.android.robotium.solo.Solo;

import org.ohmage.UserPreferencesHelper;
import org.ohmage.activity.DashboardActivity;
import org.ohmage.activity.MobilityActivity;
import org.ohmage.activity.OhmagePreferenceActivity;

/**
 * <p>
 * This class contains tests for the {@link OhmagePreferenceActivity}
 * </p>
 * TODO: These tests should be done to take into account the fact that the
 * server determines some of the initial state. Some preferences might not be
 * specified.
 * 
 * @author cketcham
 */
public class AdminSettingsDashboardTest extends ActivityInstrumentationTestCase2<DashboardActivity> {

    private Solo solo;
    private boolean oldShowFeedback;
    private boolean oldShowMobility;
    private boolean oldShowMobilityFeedback;
    private boolean oldShowProfile;
    private boolean oldShowUploadQueue;
    private boolean oldSingleCampaignMode;
    private SharedPreferences mPrefs;

    public AdminSettingsDashboardTest() {
        super(DashboardActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        solo = new Solo(getInstrumentation(), getActivity());
        mPrefs = UserPreferencesHelper.getUserSharedPreferences(getActivity());

        // Save old pref values
        oldShowFeedback = UserPreferencesHelper.showFeedback();
        oldShowMobility = UserPreferencesHelper.showMobility();
        oldShowMobilityFeedback = UserPreferencesHelper.showMobilityFeedback();
        oldShowProfile = UserPreferencesHelper.showProfile();
        oldShowUploadQueue = UserPreferencesHelper.showUploadQueue();
        oldSingleCampaignMode = UserPreferencesHelper.isSingleCampaignMode();

        // Set state for tests
        mPrefs.edit().putBoolean(UserPreferencesHelper.KEY_SHOW_FEEDBACK, true)
                .putBoolean(UserPreferencesHelper.KEY_SHOW_MOBILITY, true)
                .putBoolean(UserPreferencesHelper.KEY_SHOW_MOBILITY_FEEDBACK, true)
                .putBoolean(UserPreferencesHelper.KEY_SHOW_PROFILE, true)
                .putBoolean(UserPreferencesHelper.KEY_SHOW_UPLOAD_QUEUE, true)
                .putBoolean(UserPreferencesHelper.KEY_SINGLE_CAMPAIGN_MODE, false).commit();

    }

    @Override
    protected void tearDown() throws Exception {

        // Replace old pref values
        mPrefs.edit()
                .putBoolean(UserPreferencesHelper.KEY_SHOW_FEEDBACK, oldShowFeedback)
                .putBoolean(UserPreferencesHelper.KEY_SHOW_MOBILITY, oldShowMobility)
                .putBoolean(UserPreferencesHelper.KEY_SHOW_MOBILITY_FEEDBACK,
                        oldShowMobilityFeedback)
                .putBoolean(UserPreferencesHelper.KEY_SHOW_PROFILE, oldShowProfile)
                .putBoolean(UserPreferencesHelper.KEY_SHOW_UPLOAD_QUEUE, oldShowUploadQueue)
                .putBoolean(UserPreferencesHelper.KEY_SINGLE_CAMPAIGN_MODE, oldSingleCampaignMode)
                .commit();

        try {
            solo.finalize();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        getActivity().finish();
        super.tearDown();
    }

    public void testSingleCampaignMode() {
        assertTrue(solo.searchText("Campaigns", true));
        clickOnDashPref("Single Campaign Mode");
        assertFalse(solo.searchText("Campaigns", true));
    }

    public void testFeedback() {
        assertTrue(solo.searchText("Response\nHistory", true));
        clickOnDashPref("Show Feedback");
        assertFalse(solo.searchText("Response\nHistory", true));
    }

    public void testProfile() {
        assertTrue(solo.searchText("Profile", true));
        clickOnDashPref("Show Profile");
        assertFalse(solo.searchText("Profile", true));
    }

    public void testUploadQueue() {
        assertTrue(solo.searchText("Upload\nQueue", true));
        clickOnDashPref("Show Upload Queue");
        assertFalse(solo.searchText("Upload\nQueue", true));
    }

    public void testMobility() {
        assertTrue(solo.searchText("Mobility", true));
        clickOnDashPref("Show Mobility");
        assertFalse(solo.searchText("Mobility", true));
    }

    public void testMobilityFeedback() {
        assertTrue(solo.searchText("Mobility", true));
        solo.clickOnText("Mobility");
        solo.assertCurrentActivity("expected MobilityActivity", MobilityActivity.class);
        assertTrue(solo.searchText("ANALYTICS", true));
        solo.goBack();
        clickOnDashPref("Show Mobility Feedback");
        solo.clickOnText("Mobility");
        solo.assertCurrentActivity("expected MobilityActivity", MobilityActivity.class);
        assertFalse(solo.searchText("ANALYTICS", true));
        solo.goBack();
    }

    private void clickOnDashPref(String pref) {
        solo.clickOnMenuItem("Settings");
        solo.clickOnText("Admin Settings");
        solo.clickOnText(pref);
        solo.goBack();
        solo.goBack();
    }

}
