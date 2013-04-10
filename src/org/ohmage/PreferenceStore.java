
package org.ohmage;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Map;
import java.util.Set;

/**
 * Preference helper for application wide settings.
 * 
 * @author cketcham
 */
public class PreferenceStore implements SharedPreferences {

    private static final String KEY_MOBILITY_VERSION = "mobility_version";

    private static final String KEY_IS_DISABLED = "is_disabled";
    private static final String KEY_LAST_PROBE_UPLOAD_TIMESTAMP = "last_probe_upload_timestamp";
    private static final String KEY_LOGIN_TIMESTAMP = "login_timestamp";
    private static final String KEY_LAST_SURVEY_TIMESTAMP = "last_timestamp_";
    private static final String KEY_CAMPAIGN_REFRESH_TIME = "campaign_refresh_time";

    private final SharedPreferences mPreferences;

    public PreferenceStore(Context context) {
        mPreferences = context.getSharedPreferences("PreferenceStore", Context.MODE_PRIVATE);
    }

    @Override
    public Map<String, ?> getAll() {
        return mPreferences.getAll();
    }

    @Override
    public String getString(String key, String defValue) {
        return mPreferences.getString(key, defValue);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        return mPreferences.getStringSet(key, defValues);
    }

    @Override
    public int getInt(String key, int defValue) {
        return mPreferences.getInt(key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        return mPreferences.getLong(key, defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return mPreferences.getFloat(key, defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return mPreferences.getBoolean(key, defValue);
    }

    @Override
    public boolean contains(String key) {
        return mPreferences.contains(key);
    }

    @Override
    public PreferenceStoreEditor edit() {
        return new PreferenceStoreEditor(mPreferences.edit());
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static class PreferenceStoreEditor implements Editor {

        Editor mEditor;

        public PreferenceStoreEditor(Editor edit) {
            mEditor = edit;
        }

        @Override
        public Editor putString(String key, String value) {
            mEditor.putString(key, value);
            return this;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public Editor putStringSet(String key, Set<String> values) {
            mEditor.putStringSet(key, values);
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            mEditor.putInt(key, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            mEditor.putLong(key, value);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            mEditor.putFloat(key, value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            mEditor.putBoolean(key, value);
            return this;
        }

        @Override
        public Editor remove(String key) {
            mEditor.remove(key);
            return this;
        }

        @Override
        public Editor clear() {
            mEditor.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return mEditor.commit();
        }

        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        @Override
        public void apply() {
            mEditor.apply();
        }

        public Editor setMobilityVersion(int mobilityVersion) {
            mEditor.putInt(KEY_MOBILITY_VERSION, mobilityVersion);
            return this;
        }

        public Editor putLastProbeUploadTimestamp(Long timestamp) {
            mEditor.putLong(KEY_LAST_PROBE_UPLOAD_TIMESTAMP, timestamp);
            return this;
        }

        public Editor putLoginTimestamp(Long timestamp) {
            mEditor.putLong(KEY_LOGIN_TIMESTAMP, timestamp);
            return this;
        }

        public Editor putLastSurveyTimestamp(String surveyId, Long timestamp) {
            mEditor.putLong(KEY_LAST_SURVEY_TIMESTAMP + surveyId, timestamp);
            return this;
        }

        public Editor setUserDisabled(boolean isDisabled) {
            mEditor.putBoolean(KEY_IS_DISABLED, isDisabled);
            return this;
        }

        public Editor setLastCampaignRefreshTime(long time) {
            mEditor.putLong(KEY_CAMPAIGN_REFRESH_TIME, time);
            return this;
        }
    }

    public int getLastMobilityVersion() {
        return mPreferences.getInt(KEY_MOBILITY_VERSION, -1);
    }

    public long getLastProbeUploadTimestamp() {
        return mPreferences.getLong(KEY_LAST_PROBE_UPLOAD_TIMESTAMP, 0);
    }

    public long getLoginTimestamp() {
        return mPreferences.getLong(KEY_LOGIN_TIMESTAMP, 0);
    }

    public long getLastSurveyTimestamp(String surveyId) {
        return mPreferences.getLong(KEY_LAST_SURVEY_TIMESTAMP + surveyId, 0);
    }

    public boolean isUserDisabled() {
        return mPreferences.getBoolean(KEY_IS_DISABLED, false);
    }

    public long getLastCampaignRefreshTime() {
        return mPreferences.getLong(KEY_CAMPAIGN_REFRESH_TIME, 0);
    }

}
