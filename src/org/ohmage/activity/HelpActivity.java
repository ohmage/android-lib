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

package org.ohmage.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TabHost;

import org.ohmage.UserPreferencesHelper;
import org.ohmage.Utilities;
import org.ohmage.library.R;
import org.ohmage.ui.BaseActivity;
import org.ohmage.ui.TabsAdapter;

import java.io.InputStream;

/**
 * Help activity uses a view pager to show different help screens
 * 
 * @author cketcham
 */
public class HelpActivity extends BaseActivity {
    TabHost mTabHost;
    ViewPager mViewPager;
    TabsAdapter mTabsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tab_layout);

        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();

        mViewPager = (ViewPager) findViewById(R.id.pager);

        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);

        mTabsAdapter.addTab(getString(R.string.help_goto_dashboard),
                DashboardWebViewFragment.class, null);
        mTabsAdapter
                .addTab(getString(R.string.help_goto_filter), FilterWebViewFragment.class, null);
        mTabsAdapter.addTab(getString(R.string.help_goto_list), ResourceWebViewFragment.class,
                ResourceWebViewFragment.instanceBundle(R.raw.about_lists));

        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tab", mTabHost.getCurrentTabTag());
    }

    public abstract static class WebViewFragment extends Fragment {

        /**
         * The Fragment's UI is just a simple webview
         */
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            WebView webView = new WebView(getActivity());
            StringBuilder data = loadData();
            if (data == null)
                throw new RuntimeException("No initial data was supplied to the WebViewFragment");

            webView.loadDataWithBaseURL("/", data.toString(), "text/html", null, null);
            webView.getSettings().setSupportZoom(false);
            return webView;
        }

        protected abstract StringBuilder loadData();
    }

    public static class ResourceWebViewFragment extends WebViewFragment {

        int resource;

        /**
         * Create a new instance of WebViewFragment, providing the url as an
         * argument
         */
        static ResourceWebViewFragment newInstance(int urlResource) {
            ResourceWebViewFragment f = new ResourceWebViewFragment();
            f.setArguments(instanceBundle(urlResource));
            return f;
        }

        /**
         * Create the bundle for a new instance of WebViewFragment
         * 
         * @param url
         * @return the bundle which will show this url
         */
        static Bundle instanceBundle(int urlResource) {
            Bundle args = new Bundle();
            args.putInt("url", urlResource);
            return args;
        }

        /**
         * When creating, retrieve this instance's number from its arguments.
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null)
                resource = getArguments().getInt("url");
        }

        /**
         * Creates the resource data string
         * 
         * @return the data string
         */
        @Override
        protected StringBuilder loadData() {
            InputStream is = getResources().openRawResource(resource);
            return new StringBuilder(Utilities.convertStreamToString(is));
        }
    }

    /**
     * Each item on the dashboard has an associated entry in the help which will
     * only be shown if that item is visible on the dashboard
     * 
     * @author cketcham
     */
    public static class DashboardWebViewFragment extends ResourceWebViewFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            resource = R.raw.about_sectioned;
        }

        @Override
        protected StringBuilder loadData() {
            StringBuilder data = super.loadData();

            data.insert(data.length() - 8, getString(R.string.help_dashboard_header_text));

            UserPreferencesHelper userPrefs = UserPreferencesHelper.getInstance();

            if (!userPrefs.isSingleCampaignMode())
                addSection(data, "dash_campaigns.png", R.string.help_dashboard_campaigns_title,
                        R.string.help_dashboard_campaigns_text);

            addSection(data, "dash_surveys.png", R.string.help_dashboard_surveys_title,
                    R.string.help_dashboard_surveys_text);

            if (userPrefs.showFeedback())
                addSection(data, "dash_resphistory.png",
                        R.string.help_dashboard_response_history_title,
                        R.string.help_dashboard_response_history_text);

            if (userPrefs.showUploadQueue())
                addSection(data, "dash_upqueue.png", R.string.help_dashboard_upload_queue_title,
                        R.string.help_dashboard_upload_queue_text);

            if (userPrefs.showProfile())
                addSection(data, "dash_profile.png", R.string.help_dashboard_profile_title,
                        R.string.help_dashboard_profile_text);

            if (userPrefs.showMobility())
                addSection(data, "dash_mobility.png", R.string.help_dashboard_mobility_title,
                        R.string.help_dashboard_mobility_text);

            return data;
        }

        private void addSection(StringBuilder builder, String icon, int title, int text) {
            builder.insert(
                    builder.length() - 8,
                    getString(R.string.help_dashboard_section, "file:///android_res/drawable/"
                            + icon, getString(title), getString(text)));
        }
    }

    public static class FilterWebViewFragment extends ResourceWebViewFragment {

        private static final String FILTER_SUB = "{filter_image}";
        private static final String FILTER_SINGLE_CAMPAIGN = "file:///android_res/raw/filters_single.jpg";
        private static final String FILTER_MULTI_CAMPAIGN = "file:///android_res/raw/filters.jpg";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            resource = R.raw.about_filter;
        }

        @Override
        protected StringBuilder loadData() {
            StringBuilder data = super.loadData();
            int start = data.indexOf(FILTER_SUB);
            String image = FILTER_SINGLE_CAMPAIGN;
            if (UserPreferencesHelper.isSingleCampaignMode())
                image = FILTER_MULTI_CAMPAIGN;
            data.replace(start, start + FILTER_SUB.length(), image);
            return data;
        }
    }
}
