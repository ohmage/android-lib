
package org.ohmage.fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.commonsware.cwac.wakeful.WakefulIntentService;

import org.ohmage.fragments.ProbeListFragment.ProbeAppEntry;
import org.ohmage.library.R;
import org.ohmage.logprobe.Log;
import org.ohmage.probemanager.DbContract.Probe;
import org.ohmage.probemanager.DbContract.Probes;
import org.ohmage.probemanager.ProbeManager;
import org.ohmage.service.ProbeUploadService;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProbeListFragment extends SherlockListFragment implements LoaderCallbacks<List<ProbeAppEntry>> {

    private static final String TAG = "ProbeListFragment";

    private static final int CONTEXT_MENU_CONFIGURE_ID = 0;
    private static final int CONTEXT_MENU_ANALYTICS_ID = 1;
    private static final int CONTEXT_MENU_DELETE_ID = 2;
    private static final int CONTEXT_MENU_UPLOAD_ID = 3;

    AppListAdapter mAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText("No Probes");

        // We have a menu item to show in action bar.
        setHasOptionsMenu(false);

        registerForContextMenu(getListView());

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new AppListAdapter(getActivity());

        setListAdapter(mAdapter);
        setListShown(false);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().registerReceiver(mProbeUploadReceiver,
                new IntentFilter(ProbeUploadService.PROBE_UPLOAD_STARTED));
        getActivity().registerReceiver(mProbeUploadReceiver,
                new IntentFilter(ProbeUploadService.PROBE_UPLOAD_ERROR));
        getActivity().registerReceiver(mProbeUploadReceiver,
                new IntentFilter(ProbeUploadService.RESPONSE_UPLOAD_ERROR));
        getActivity().registerReceiver(mProbeUploadReceiver,
                new IntentFilter(ProbeUploadService.PROBE_UPLOAD_SERVICE_FINISHED));
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mProbeUploadReceiver);
    }

    private final BroadcastReceiver mProbeUploadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            getSherlockActivity().setSupportProgressBarIndeterminateVisibility(
                    ProbeUploadService.PROBE_UPLOAD_STARTED.equals(action));

            if (ProbeUploadService.PROBE_UPLOAD_ERROR.equals(action)
                    || ProbeUploadService.RESPONSE_UPLOAD_ERROR.equals(action)) {
                String error = intent.getStringExtra(ProbeUploadService.EXTRA_PROBE_ERROR);
                if (error == null)
                    Toast.makeText(getActivity(), R.string.mobility_network_error_message,
                            Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getActivity(),
                            getString(R.string.mobility_upload_error_message, error),
                            Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Toast.makeText(getActivity(), "Go to probe info activity", Toast.LENGTH_SHORT).show();
    }

    @Override
    public Loader<List<ProbeAppEntry>> onCreateLoader(int id, Bundle args) {
        return new AppListLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<ProbeAppEntry>> loader, List<ProbeAppEntry> data) {
        // Set the new data in the adapter.
        mAdapter.setData(data);

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<ProbeAppEntry>> loader) {
        mAdapter.setData(null);
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Log.d(TAG, "pos" + info.position);

        ProbeAppEntry entry = (ProbeAppEntry) getListAdapter().getItem(info.position);

        if (createAnalyticsIntent(entry.observerId) != null)
            contextMenu.add(0, CONTEXT_MENU_ANALYTICS_ID, 0, "Analytics");
        if (createConfigureIntent(entry.observerId) != null)
            contextMenu.add(0, CONTEXT_MENU_CONFIGURE_ID, 1, "Configure");
        contextMenu.add(0, CONTEXT_MENU_DELETE_ID, 2, "Delete");
        contextMenu.add(0, CONTEXT_MENU_UPLOAD_ID, 3, "Upload");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        ProbeAppEntry entry = (ProbeAppEntry) getListAdapter().getItem(info.position);

        Intent intent;
        switch (item.getItemId()) {
            case CONTEXT_MENU_ANALYTICS_ID:
                intent = createAnalyticsIntent(entry.observerId);
                if (intent != null)
                    startActivity(intent);
                else
                    Toast.makeText(getActivity(), "Probe Analytics no longer exists",
                            Toast.LENGTH_SHORT).show();
                return true;
            case CONTEXT_MENU_CONFIGURE_ID:
                intent = createConfigureIntent(entry.observerId);
                if (intent != null)
                    startActivity(intent);
                else
                    Toast.makeText(getActivity(), "Probe Configuration no longer exists",
                            Toast.LENGTH_SHORT).show();
                return true;
            case CONTEXT_MENU_UPLOAD_ID:
                intent = new Intent(getActivity(), ProbeUploadService.class);
                intent.putExtra(ProbeUploadService.EXTRA_OBSERVER_ID, entry.observerId);
                WakefulIntentService.sendWakefulWork(getActivity(), intent);
                return true;
            case CONTEXT_MENU_DELETE_ID:
                ProbeDeleteTask task = new ProbeDeleteTask();
                task.execute(new Probe(entry.observerId, entry.observerVersion));
                return true;
        }
        return super.onContextItemSelected(item);
    }

    public Intent createConfigureIntent(String observerId) {
        return createIntent(ProbeManager.ACTION_CONFIGURE, observerId);
    }

    public Intent createAnalyticsIntent(String observerId) {
        return createIntent(ProbeManager.ACTION_VIEW_ANALYTICS, observerId);
    }

    public Intent createIntent(String action, String observerId) {
        Intent intent = new Intent(action);
        intent.setType("probe/" + observerId);
        ResolveInfo info = getActivity().getPackageManager().resolveActivity(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (info != null) {
            return intent;
        } else {
            return null;
        }
    }

    /**
     * This class holds the per-item data in our Loader.
     */
    public static class ProbeAppEntry {
        public ProbeAppEntry(AppListLoader loader, ApplicationInfo info, String observerName,
                String observerId, String observerVersionName, String observerVersion) {
            mLoader = loader;
            mInfo = info;
            mApkFile = new File(info.sourceDir);

            this.observerName = observerName;
            this.observerId = observerId;
            this.observerVersionName = observerVersionName;
            this.observerVersion = observerVersion;
        }

        public ApplicationInfo getApplicationInfo() {
            return mInfo;
        }

        public String getLabel() {
            return mLabel;
        }

        public Drawable getIcon() {
            if (mIcon == null) {
                if (mApkFile.exists()) {
                    mIcon = mInfo.loadIcon(mLoader.mPm);
                    return mIcon;
                } else {
                    mMounted = false;
                }
            } else if (!mMounted) {
                // If the app wasn't mounted but is now mounted, reload
                // its icon.
                if (mApkFile.exists()) {
                    mMounted = true;
                    mIcon = mInfo.loadIcon(mLoader.mPm);
                    return mIcon;
                }
            } else {
                return mIcon;
            }

            return mLoader.getContext().getResources()
                    .getDrawable(android.R.drawable.sym_def_app_icon);
        }

        @Override
        public String toString() {
            return mLabel;
        }

        /**
         * Formats the observer name nicely
         * 
         * @return
         */
        public String getObserverName() {
            StringBuilder builder = new StringBuilder();
            if (TextUtils.isEmpty(observerName))
                return "Unknown probe metadata";
            else {
                builder.append(observerName).append(" ");
                if (!TextUtils.isEmpty(observerVersionName)) {
                    if (!observerVersionName.startsWith("v"))
                        builder.append("v");
                    builder.append(observerVersionName);
                }
                return builder.toString();
            }
        }

        void loadLabel(Context context) {
            if (mLabel == null || !mMounted) {
                if (!mApkFile.exists()) {
                    mMounted = false;
                    mLabel = mInfo.packageName;
                } else {
                    mMounted = true;
                    CharSequence label = mInfo.loadLabel(context.getPackageManager());
                    mLabel = label != null ? label.toString() : mInfo.packageName;
                }
            }
        }

        private final AppListLoader mLoader;
        private final ApplicationInfo mInfo;
        private final File mApkFile;
        private String mLabel;
        private Drawable mIcon;
        private boolean mMounted;
        private final String observerName;
        private final String observerId;
        private final String observerVersionName;
        private final String observerVersion;
    }

    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<ProbeAppEntry> ALPHA_COMPARATOR = new Comparator<ProbeAppEntry>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(ProbeAppEntry object1, ProbeAppEntry object2) {
            return sCollator.compare(object1.getLabel(), object2.getLabel());
        }
    };

    /**
     * Helper for determining if the configuration has changed in an interesting
     * way so we need to rebuild the app list.
     */
    public static class InterestingConfigChanges {
        final Configuration mLastConfiguration = new Configuration();
        int mLastDensity;

        boolean applyNewConfig(Resources res) {
            int configChanges = mLastConfiguration.updateFrom(res.getConfiguration());
            boolean densityChanged = mLastDensity != res.getDisplayMetrics().densityDpi;
            if (densityChanged
                    || (configChanges & (ActivityInfo.CONFIG_LOCALE | ActivityInfo.CONFIG_UI_MODE | ActivityInfo.CONFIG_SCREEN_LAYOUT)) != 0) {
                mLastDensity = res.getDisplayMetrics().densityDpi;
                return true;
            }
            return false;
        }
    }

    /**
     * Helper class to look for interesting changes to the installed apps so
     * that the loader can be updated.
     */
    public static class PackageIntentReceiver extends BroadcastReceiver {
        final AppListLoader mLoader;

        public PackageIntentReceiver(AppListLoader loader) {
            mLoader = loader;
            IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme("package");
            mLoader.getContext().registerReceiver(this, filter);
            // Register for events related to sdcard installation.
            IntentFilter sdFilter = new IntentFilter();
            sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
            sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
            mLoader.getContext().registerReceiver(this, sdFilter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // Tell the loader about the change.
            mLoader.onContentChanged();
        }
    }

    /**
     * A custom Loader that loads all of the installed applications which write
     * probes to ohmage
     */
    public static class AppListLoader extends AsyncTaskLoader<List<ProbeAppEntry>> {
        final InterestingConfigChanges mLastConfig = new InterestingConfigChanges();
        final PackageManager mPm;

        List<ProbeAppEntry> mApps;
        PackageIntentReceiver mPackageObserver;

        public AppListLoader(Context context) {
            super(context);
            mPm = getContext().getPackageManager();
        }

        public List<ProbeAppEntry> parseObserversFromApp(ApplicationInfo info) {
            ArrayList<ProbeAppEntry> ret = new ArrayList<ProbeAppEntry>();

            final Context context = getContext();

            PackageManager pm = context.getPackageManager();
            XmlResourceParser parser = null;
            try {
                parser = info.loadXmlMetaData(pm, ProbeManager.PROBE_META_DATA);
                if (parser == null) {
                    throw new XmlPullParserException("No " + ProbeManager.PROBE_META_DATA
                            + " meta-data");
                }

                Resources res = pm.getResourcesForApplication(info);

                AttributeSet attrs = Xml.asAttributeSet(parser);

                int type;
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {

                    if (type == XmlPullParser.START_TAG) {

                        String nodeName = parser.getName();

                        if (!"probe".equals(nodeName) && !"probes".equals(nodeName)) {
                            throw new XmlPullParserException(
                                    "Meta-data does not start with probe tag");
                        }

                        if ("probe".equals(nodeName)) {
                            TypedArray sa = res.obtainAttributes(attrs, R.styleable.probe);

                            String observerName = sa.getString(R.styleable.probe_observerName);
                            String observerId = sa.getString(R.styleable.probe_observerId);
                            String observerVersionName = sa
                                    .getString(R.styleable.probe_observerVersionName);
                            String observerVersion = sa
                                    .getString(R.styleable.probe_observerVersionCode);

                            ProbeAppEntry entry = new ProbeAppEntry(this, info, observerName,
                                    observerId, observerVersionName, observerVersion);
                            entry.loadLabel(context);
                            ret.add(entry);

                            sa.recycle();
                        }
                    }
                }

            } catch (XmlPullParserException e) {
                Log.e(TAG, "Unable to parse probe metadata", e);
            } catch (IOException e) {
                Log.e(TAG, "IOException parsing probe metadata", e);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Unable to create context for: " + info.packageName, e);
            } finally {
                if (parser != null)
                    parser.close();
            }

            return ret;
        }

        /**
         * Find all apps which have metadata describing the probe urn and
         * version. These are the apps we will show in the list
         */
        @Override
        public List<ProbeAppEntry> loadInBackground() {
            // Retrieve all known applications.
            List<ApplicationInfo> apps = mPm.getInstalledApplications(PackageManager.GET_META_DATA);
            if (apps == null) {
                apps = new ArrayList<ApplicationInfo>();
            }

            // Create corresponding array of entries and load their labels.
            List<ProbeAppEntry> entries = new ArrayList<ProbeAppEntry>();
            for (int i = 0; i < apps.size(); i++) {

                if (apps.get(i).metaData != null && apps.get(i).metaData.containsKey(ProbeManager.PROBE_META_DATA)) {
                    entries.addAll(parseObserversFromApp(apps.get(i)));
                }
            }

            // Sort the list.
            Collections.sort(entries, ALPHA_COMPARATOR);

            return entries;
        }

        /**
         * Called when there is new data to deliver to the client. The super
         * class will take care of delivering it; the implementation here just
         * adds a little more logic.
         */
        @Override
        public void deliverResult(List<ProbeAppEntry> apps) {
            if (isReset()) {
                // An async query came in while the loader is stopped. We
                // don't need the result.
                if (apps != null) {
                    onReleaseResources(apps);
                }
            }
            List<ProbeAppEntry> oldApps = apps;
            mApps = apps;

            if (isStarted()) {
                // If the Loader is currently started, we can immediately
                // deliver its results.
                super.deliverResult(apps);
            }

            if (oldApps != null) {
                onReleaseResources(oldApps);
            }
        }

        /**
         * Handles a request to start the Loader.
         */
        @Override
        protected void onStartLoading() {
            if (mApps != null) {
                // If we currently have a result available, deliver it
                // immediately.
                deliverResult(mApps);
            }

            // Start watching for changes in the app data.
            if (mPackageObserver == null) {
                mPackageObserver = new PackageIntentReceiver(this);
            }

            // Has something interesting in the configuration changed since we
            // last built the app list?
            boolean configChange = mLastConfig.applyNewConfig(getContext().getResources());

            if (takeContentChanged() || mApps == null || configChange) {
                // If the data has changed since the last time it was loaded
                // or is not currently available, start a load.
                forceLoad();
            }
        }

        /**
         * Handles a request to stop the Loader.
         */
        @Override
        protected void onStopLoading() {
            cancelLoad();
        }

        /**
         * Handles a request to cancel a load.
         */
        @Override
        public void onCanceled(List<ProbeAppEntry> apps) {
            super.onCanceled(apps);

            onReleaseResources(apps);
        }

        /**
         * Handles a request to completely reset the Loader.
         */
        @Override
        protected void onReset() {
            super.onReset();

            // Ensure the loader is stopped
            onStopLoading();

            // At this point we can release the resources associated with 'apps'
            // if needed.
            if (mApps != null) {
                onReleaseResources(mApps);
                mApps = null;
            }

            // Stop monitoring for changes.
            if (mPackageObserver != null) {
                getContext().unregisterReceiver(mPackageObserver);
                mPackageObserver = null;
            }
        }

        /**
         * Helper function to take care of releasing resources associated with
         * an actively loaded data set.
         */
        protected void onReleaseResources(List<ProbeAppEntry> apps) {
            // For a simple List<> there is nothing to do. For something
            // like a Cursor, we would close it here.
        }
    }

    public static class AppListAdapter extends ArrayAdapter<ProbeAppEntry> {
        private final LayoutInflater mInflater;

        public AppListAdapter(Context context) {
            super(context, R.layout.probe_list_item);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(List<ProbeAppEntry> data) {
            clear();
            if (data != null) {
                addAll(data);
            }
        }

        @SuppressLint("NewApi")
        @Override
        public void addAll(Collection<? extends ProbeAppEntry> collection) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                super.addAll(collection);
            } else {
                for (ProbeAppEntry element : collection) {
                    add(element);
                }
            }
        }

        /**
         * Populate new items in the list.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                view = mInflater.inflate(R.layout.probe_list_item, parent, false);
            } else {
                view = convertView;
            }

            ProbeAppEntry item = getItem(position);
            ((ImageView) view.findViewById(R.id.icon)).setImageDrawable(item.getIcon());
            ((TextView) view.findViewById(R.id.text1)).setText(item.getLabel());
            ((TextView) view.findViewById(R.id.text2)).setText(item.getObserverName());

            view.findViewById(R.id.context_menu).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    v.showContextMenu();
                }
            });

            return view;
        }
    }

    public class ProbeDeleteTask extends AsyncTask<Probe, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected Boolean doInBackground(Probe... params) {
            if (params.length == 1) {
                return getActivity().getContentResolver().delete(Probes.CONTENT_URI,
                        Probes.OBSERVER_ID + "=? AND " + Probes.OBSERVER_VERSION + "=?",
                        new String[] {
                                params[0].observer_id, params[0].observer_version
                        }) > 0;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);
        }
    }
}
