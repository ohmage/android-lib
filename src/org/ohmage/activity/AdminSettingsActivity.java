package org.ohmage.activity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import org.ohmage.AccountActivityHelper;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.library.R;
import org.ohmage.logprobe.Analytics;
import org.ohmage.logprobe.LogProbe.Status;

public class AdminSettingsActivity extends PreferenceActivity  {

	private static final String KEY_UPDATE_PASSWORD = "key_update_password";
	private static final String KEY_LOGOUT = "key_logout";
	private static final String KEY_QUERY_TEST = "key_querytest";
	private static final String KEY_BASELINE_START_TIME = "key_baseline_start_time";
	private static final String KEY_BASELINE_END_TIME = "key_baseline_end_time";
	private static final String KEY_RESET_PREFERENCES = "key_reset_preferences";
	private static final String KEY_BASELINE_CLEAR = "key_baseline_clear";

	private PreferenceScreen mUpdatePassword;
	private PreferenceScreen mLogout;
	private PreferenceScreen mQueryTest;

	private AccountActivityHelper mAccountHelper;
	private Preference mBaseLineClear;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(R.string.admin_settings);

		mAccountHelper = new AccountActivityHelper(this);

		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName(UserPreferencesHelper.getPreferencesName(this));
		prefMgr.setSharedPreferencesMode(MODE_PRIVATE);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.admin_preferences);

		setPreferences();

		mUpdatePassword = (PreferenceScreen) findPreference(KEY_UPDATE_PASSWORD);
		mUpdatePassword.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				mAccountHelper.updatePassword();
				return true;
			}
		});

		mLogout = (PreferenceScreen) findPreference(KEY_LOGOUT);
		mLogout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				mAccountHelper.logout();
				return true;
			}
		});

		mQueryTest = (PreferenceScreen) findPreference(KEY_QUERY_TEST);
		mQueryTest.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(getBaseContext(), QueryTestActivity.class));
				return true;
			}
		});

		mBaseLineClear = findPreference(KEY_BASELINE_CLEAR);
		mBaseLineClear.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				UserPreferencesHelper.clearBaseLineTime(AdminSettingsActivity.this);
				findPreference(KEY_BASELINE_START_TIME).setSummary(null);
				findPreference(KEY_BASELINE_END_TIME).setSummary(null);
				return true;
			}
		});

		Preference reset = findPreference(KEY_RESET_PREFERENCES);
		reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                UserPreferencesHelper.clearAll();
                findPreference(KEY_BASELINE_START_TIME).setSummary(null);
                findPreference(KEY_BASELINE_END_TIME).setSummary(null);
                setPreferences();
                return true;
            }
        });
	}

	private void setPreferences() {
	       // We should make sure the state is always consistant with the UserPrefHelper since the defaults are set there
        UserPreferencesHelper userPrefHelper = UserPreferencesHelper.getInstance();
        ((CheckBoxPreference) findPreference(UserPreferencesHelper.KEY_SHOW_FEEDBACK)).setChecked(userPrefHelper.showFeedback());
        ((CheckBoxPreference) findPreference(UserPreferencesHelper.KEY_SHOW_PROFILE)).setChecked(userPrefHelper.showProfile());
        ((CheckBoxPreference) findPreference(UserPreferencesHelper.KEY_SHOW_UPLOAD_QUEUE)).setChecked(userPrefHelper.showUploadQueue());
        ((CheckBoxPreference) findPreference(UserPreferencesHelper.KEY_SHOW_MOBILITY)).setChecked(userPrefHelper.showMobility());
        ((CheckBoxPreference) findPreference(UserPreferencesHelper.KEY_SHOW_MOBILITY_FEEDBACK)).setChecked(userPrefHelper.showMobilityFeedback());

        ((CheckBoxPreference) findPreference(UserPreferencesHelper.KEY_SINGLE_CAMPAIGN_MODE)).setChecked(userPrefHelper.isSingleCampaignMode());
	}

	@Override
	public void onResume() {
		super.onResume();
		Analytics.activity(this, Status.ON);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Analytics.activity(this, Status.OFF);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		mAccountHelper.onPrepareDialog(id, dialog);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = mAccountHelper.onCreateDialog(id);
		if(dialog == null)
			dialog = super.onCreateDialog(id);
		return dialog;
	}
}