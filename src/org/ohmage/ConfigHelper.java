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

import android.preference.PreferenceManager;

import org.ohmage.library.R;
import org.yaml.snakeyaml.Yaml;

import java.net.URI;
import java.util.HashMap;

/**
 * Server Configuration Helper
 * 
 * @author cketcham
 */
public class ConfigHelper {

    private static final String TAG = "ConfigHelper";

    private static final String KEY_SERVER_URL = "key_server_url";

    private static String serverUrl;

    private static ServerConfigMap mServerConf;

    public static void setServerUrl(String url) {
        serverUrl = url;
        PreferenceManager.getDefaultSharedPreferences(OhmageApplication.getContext()).edit()
                .putString(KEY_SERVER_URL, url).commit();
    }

    public static String serverUrl() {
        if (serverUrl == null)
            serverUrl = PreferenceManager.getDefaultSharedPreferences(
                    OhmageApplication.getContext()).getString(KEY_SERVER_URL, null);
        return serverUrl;
    }

    public static boolean getDefaultShowMobility() {
        return getConfigBoolean("show_mobility", R.bool.show_mobility);
    }

    public static boolean getDefaultShowProbes() {
        return getConfigBoolean("show_streams", R.bool.show_streams);
    }

    public static boolean getDefaultUploadProbesWifiOnly() {
        return getConfigBoolean("upload_probes_wifi", R.bool.upload_probes_wifi);
    }

    public static boolean getAdminMode() {
        return getConfigBoolean("admin_mode", R.bool.admin_mode);
    }

    public static String getLogLevel() {
        return getConfigString("log_level", R.string.log_level);
    }

    public static boolean getLogAnalytics() {
        return getConfigBoolean("log_analytics", R.bool.log_analytics);
    }

    private static String getConfigString(String key, int defValueId) {
        return (String) getConfigValue(key, OhmageApplication.getContext().getResources()
                .getString(defValueId));
    }

    private static boolean getConfigBoolean(String key, int defValueId) {
        return (Boolean) getConfigValue(key, OhmageApplication.getContext().getResources()
                .getBoolean(defValueId));
    }

    private static Object getConfigValue(String key, Object defValue) {
        HashMap<String, Object> config = getConfig();
        if (config != null && config.containsKey(key)) {
            return config.get(key);
        }
        return defValue;
    }

    private static HashMap<String, Object> getConfig() {
        if (serverUrl() == null)
            return null;
        String host = URI.create(serverUrl()).getHost();
        if (host != null && getServerConfig().servers.containsKey(host))
            return getServerConfig().servers.get(host);
        return null;
    }

    private static ServerConfigMap getServerConfig() {
        if (mServerConf == null) {
            Yaml yaml = new Yaml();
            mServerConf = yaml.loadAs(OhmageApplication.getContext().getResources()
                    .openRawResource(R.raw.config), ServerConfigMap.class);
        }
        return mServerConf;
    }

    public static class ServerConfigMap {
        public HashMap<String, HashMap<String, Object>> servers;
    }
}
