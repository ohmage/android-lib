package org.ohmage.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.widget.Toast;

import org.ohmage.ConfigHelper;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.Utilities;
import org.ohmage.db.Models.Campaign;
import org.ohmage.library.R;
import org.ohmage.logprobe.Analytics;
import org.ohmage.logprobe.Log;
import org.ohmage.logprobe.LogProbe.Status;

public class OhmageSettingsActivity extends PreferenceActivity  {
	private static final String TAG = "OhmageSettingsActivity";

	private static final String KEY_REMINDERS = "key_reminders";
	private static final String KEY_ADMIN_SETTINGS = "key_admin_settings";

	private static final String STATUS_CAMPAIGN_URN = "status_campaign_urn";
	private static final String STATUS_SERVER_URL = "status_server_url";
	private static final String STATUS_FEEDBACK_VISIBILITY = "status_feedback_visibility";
	private static final String STATUS_PROFILE_VISIBILITY = "status_profile_visibility";
	private static final String STATUS_UPLOAD_QUEUE_VISIBILITY = "status_upload_queue_visibility";
	private static final String STATUS_MOBILITY_VISIBILITY = "status_mobility_visibility";

	private static final String INFO_OHMAGE_VERSION = "info_ohmage_version";

	protected static final int CODE_ADMIN_SETTINGS = 0;

	private PreferenceScreen mReminders;
	private PreferenceScreen mAdmin;

	private UserPreferencesHelper mUserPreferenceHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        PreferenceManager prefMgr = getPreferenceManager();
        prefMgr.setSharedPreferencesName(UserPreferencesHelper.getPreferencesName(this));
        prefMgr.setSharedPreferencesMode(MODE_PRIVATE);

		mUserPreferenceHelper = UserPreferencesHelper.getInstance();

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		mReminders = (PreferenceScreen) findPreference(KEY_REMINDERS);
		mReminders.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				String urn = Campaign.getSingleCampaign(OhmageSettingsActivity.this);
				if(!TextUtils.isEmpty(urn)) {
					Intent triggers = Campaign.launchTriggerIntent(OhmageSettingsActivity.this, Campaign.getSingleCampaign(OhmageSettingsActivity.this));
					startActivity(triggers);
				} else
					Toast.makeText(OhmageSettingsActivity.this, R.string.preferences_no_single_campaign, Toast.LENGTH_LONG).show();
				return true;
			}
		});

		mAdmin = (PreferenceScreen) findPreference(KEY_ADMIN_SETTINGS);
		mAdmin.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivityForResult(new Intent(OhmageSettingsActivity.this, AdminPincodeActivity.class), CODE_ADMIN_SETTINGS);
				return true;
			}
		});

		findPreference(STATUS_SERVER_URL).setSummary(ConfigHelper.serverUrl());

		try {
            Preference v = findPreference(INFO_OHMAGE_VERSION);
            v.setSummary(Utilities.getVersion(this));
            v.setOnPreferenceClickListener(new OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Toast.makeText(preference.getContext(), R.string.commit_hash, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
		} catch (Exception e) {
			Log.e(TAG, "unable to retrieve version", e);
		}

	}

	@Override
	public void onResume() {
		super.onResume();
		Analytics.activity(this, Status.ON);

		// Hide and show reminders setting if we are in single campaign mode or not
		if(mUserPreferenceHelper.isSingleCampaignMode()) {
			getPreferenceScreen().addPreference(mReminders);
		} else {
			getPreferenceScreen().removePreference(mReminders);
		}

        CheckBoxPreference v = (CheckBoxPreference) findPreference(UserPreferencesHelper.KEY_UPLOAD_PROBES_WIFI_ONLY);
        v.setChecked(UserPreferencesHelper.getUploadProbesWifiOnly());

        v = (CheckBoxPreference) findPreference(UserPreferencesHelper.KEY_UPLOAD_RESPONSES_WIFI_ONLY);
        v.setChecked(UserPreferencesHelper.getUploadResponsesWifiOnly());

        v = (CheckBoxPreference) findPreference(UserPreferencesHelper.KEY_PRESERVE_INVALID_POINTS);
        Boolean b = UserPreferencesHelper.getPreserveInvalidPoints();
        if(b != null)
            v.setChecked(b);

		setStatusInfo();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Analytics.activity(this, Status.OFF);
	}

	private void setStatusInfo() {
		Preference campaignUrnStatus = findPreference(STATUS_CAMPAIGN_URN);
		if(mUserPreferenceHelper.isSingleCampaignMode()) {
			campaignUrnStatus.setTitle(R.string.preferences_single_campaign_status);
			campaignUrnStatus.setSummary(Campaign.getSingleCampaign(this));
			if(campaignUrnStatus.getSummary() == null)
				campaignUrnStatus.setSummary(R.string.unknown);
		} else {
			campaignUrnStatus.setTitle(R.string.preferences_muli_campaign_status);
			campaignUrnStatus.setSummary(null);
		}

		findPreference(STATUS_FEEDBACK_VISIBILITY).setSummary(mUserPreferenceHelper.showFeedback() ? R.string.shown : R.string.hidden);
		findPreference(STATUS_PROFILE_VISIBILITY).setSummary(mUserPreferenceHelper.showProfile() ? R.string.shown : R.string.hidden);
		findPreference(STATUS_UPLOAD_QUEUE_VISIBILITY).setSummary(mUserPreferenceHelper.showUploadQueue() ? R.string.shown : R.string.hidden);
		findPreference(STATUS_MOBILITY_VISIBILITY).setSummary(mUserPreferenceHelper.showMobility() ? R.string.shown : R.string.hidden);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
			case CODE_ADMIN_SETTINGS:
				if(resultCode == RESULT_OK)
					startActivity(new Intent(OhmageSettingsActivity.this, AdminSettingsActivity.class));
				break;
		}
	}
}
