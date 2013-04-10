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

/**
 * Server Configuration Helper
 * 
 * @author cketcham
 */
public class ConfigHelper {

    private static final String KEY_SERVER_URL = "key_server_url";

    private static String serverUrl;

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

    public static boolean getDefaultShowFeedback() {
        return true;
    }

    public static boolean getDefaultShowMobility() {
        String server = serverUrl();
        if ("https://lausd.mobilizingcs.org/".equals(server)) {
            return false;
        } else if ("https://pilots.mobilizelabs.org/".equals(server)) {
            return false;
        } else if ("https://dev.ohmage.org/".equals(server)
                || "https://test.ohmage.org/".equals(server)) {
            return true;
        } else if ("https://play.ohmage.org/".equals(server)) {
            return true;
        }
        return true;
    }

    public static boolean getDefaultUploadResponsesWifiOnly() {
        return false;
    }

    public static boolean getDefaultUploadProbesWifiOnly() {
        String server = serverUrl();

        if ("https://lausd.mobilizingcs.org/".equals(server)) {
            return true;
        } else if ("https://pilots.mobilizelabs.org/".equals(server)) {
            return true;
        } else if ("https://dev.ohmage.org/".equals(server)
                || "https://test.ohmage.org/".equals(server)) {
            return false;
        } else if ("https://play.ohmage.org/".equals(server)) {
            return true;
        }
        return true;
    }

    public static boolean getAdminMode() {
        String server = serverUrl();

        if ("https://lausd.mobilizingcs.org/".equals(server)) {
            return false;
        } else if ("https://pilots.mobilizelabs.org/".equals(server)) {
            return false;
        } else if ("https://dev.ohmage.org/".equals(server)
                || "https://test.ohmage.org/".equals(server)) {
            return true;
        } else if ("https://play.ohmage.org/".equals(server)) {
            return true;
        }
        return true;
    }

    public static boolean getReminderAdminMode() {
        return true;
    }

    public static String getLogLevel() {
        String server = serverUrl();

        if ("https://lausd.mobilizingcs.org/".equals(server)) {
            return "verbose";
        } else if ("https://pilots.mobilizelabs.org/".equals(server)) {
            return "error";
        } else if ("https://dev.ohmage.org/".equals(server)
                || "https://test.ohmage.org/".equals(server)) {
            return "verbose";
        } else if ("https://play.ohmage.org/".equals(server)) {
            return "error";
        }
        return "none";
    }

    public static boolean getLogAnalytics() {
        String server = serverUrl();

        if ("https://lausd.mobilizingcs.org/".equals(server)) {
            return true;
        } else if ("https://pilots.mobilizelabs.org/".equals(server)) {
            return false;
        } else if ("https://dev.ohmage.org/".equals(server)
                || "https://test.ohmage.org/".equals(server)) {
            return true;
        } else if ("https://play.ohmage.org/".equals(server)) {
            return false;
        }
        return false;
    }
}
