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

package org.ohmage.triggers.types.location;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.ohmage.Utilities;
import org.ohmage.library.R;
import org.ohmage.logprobe.Analytics;
import org.ohmage.logprobe.Log;
import org.ohmage.logprobe.LogProbe.Status;
import org.ohmage.triggers.config.LocTrigConfig;
import org.ohmage.triggers.utils.TrigTextInput;
import org.ohmage.ui.TouchableSupportMapFragment;
import org.ohmage.ui.TouchableSupportMapFragment.TouchableWrapper.OnTouchListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * The maps activity to display and modify the coordinates associated with each
 * place (category). Updated to use Android Google Maps v2.
 * 
 * @author cketcham
 */
public class LocTrigMapsActivity extends FragmentActivity {

    private static final String TAG = "LocTrigMapsActivity";

    public static final int AlPHA_LIGHT = 20;

    public static final String TOOL_TIP_PREF_NAME = LocTrigMapsActivity.class.getName()
            + "tool_tip_pref";
    public static final String KEY_TOOL_TIP_DO_NT_SHOW = LocTrigMapsActivity.class.getName()
            + "tool_tip_do_not_show";

    // The delay before showing the tool tip
    private static long TOOL_TIP_DELAY = 500; // ms

    /* Menu ids */
    private static final int MENU_SEARCH_ID = Menu.FIRST;
    private static final int MENU_SATELLITE_ID = Menu.FIRST + 1;
    private static final int MENU_HELP_ID = Menu.FIRST + 2;

    // GoogleMap instance
    private GoogleMap mMap;
    // Db instance
    private LocTrigDB mDb;
    // The category id for which this activity is opened
    private int mCategId = 0;
    // The async task for address search by query
    private AsyncTask<String, Void, Address> mSearchTask = null;
    // The async task for address search by lat, lng
    private FetchAddressTask mMarkerAddressTask = null;

    // Info about all markers in db
    private final HashMap<Marker, MarkerInfo> markerInfos = new HashMap<Marker, MarkerInfo>();

    // Temporary marker used to mark the location of places the user might add
    private Marker mAddMarker;

    // Marker that is focused
    private Marker mRadiusMarker;
    // Circle around marker
    private Circle mRadiusCircle;
    // True if we are in the middle of a radius update
    private boolean mRadiusUpdate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.trigger_loc_maps);

        // Get the category id from the intent
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Log.e(TAG, "Maps: Intent extras is null");

            finish();
            return;
        }

        mCategId = extras.getInt(LocTrigDB.KEY_ID);
        Log.v(TAG, "Maps: category id = " + mCategId);

        TouchableSupportMapFragment mapFragment = (TouchableSupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.triggers_map);
        mMap = mapFragment.getMap();
        mMap.setMyLocationEnabled(true);

        // Set the info window so it has an add button
        mMap.setInfoWindowAdapter(new LocTrigInfoAdapter(
                (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)));

        // Set the on Long click handler. This takes care of adding a pin,
        // deleting a pin and entering radius update mode
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

            @Override
            public void onMapLongClick(LatLng point) {

                // First check to see if there is a marker close enough that the
                // user is trying to remove
                if (removeLocation(point))
                    return;

                // Then check to see if we are in the radius of the marker to
                // start an update
                if (testMarkerRadiusUpdate(point)) {
                    onRadiusUpdateStart();
                    return;
                }

                // The default action for a long press is to add a new temporary
                // marker
                onAddTemporaryMarker(point);
            }
        });

        // When the user taps the map, reset all temporary pins
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {
                resetMapPins();
            }
        });

        // When a marker is clicked, focus the marker so the radius can be
        // edited
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker marker) {
                if (!marker.equals(mAddMarker)) {
                    onRadiusUpdateFocus(marker);
                    return true;
                }
                return false;
            }
        });

        // When the info window is clicked, interpret as adding a point
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

            @Override
            public void onInfoWindowClick(Marker marker) {
                onAddLocation();
            }
        });

        // Watch for touches to handle resizing of the radius
        mapFragment.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(MotionEvent event) {

                if (mRadiusUpdate) {
                    // Update the radius movement if we are in the middle of a
                    // radius update

                    if (MotionEvent.ACTION_MOVE == event.getAction()) {
                        // Continue the radius update since we got a move touch
                        // event
                        Point p = new Point((int) event.getX(), (int) event.getY());
                        onRadiusUpdate(p);
                        return true;
                    } else {
                        // End the radius update since we got a touch movement
                        // which wasn't move
                        onRadiusUpdateStop();
                    }
                }
                return false;
            }
        });

        // Handle done button and exit this activity
        Button bDone = (Button) findViewById(R.id.button_maps_done);
        bDone.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mDb = new LocTrigDB(this);
        mDb.open();

        final Builder bounds = new LatLngBounds.Builder();

        // Add locations to the map
        Cursor c = mDb.getLocations(mCategId);
        while (c.moveToNext()) {
            MarkerInfo info = new MarkerInfo();
            int latE6 = c.getInt(c.getColumnIndexOrThrow(LocTrigDB.KEY_LAT));
            int longE6 = c.getInt(c.getColumnIndexOrThrow(LocTrigDB.KEY_LONG));
            info.radius = c.getFloat(c.getColumnIndexOrThrow(LocTrigDB.KEY_RADIUS));
            info.id = c.getLong(c.getColumnIndexOrThrow(LocTrigDB.KEY_ID));
            LatLng position = new LatLng(latE6 / 1E6, longE6 / 1E6);

            Marker marker = mMap.addMarker(new MarkerOptions().position(position));
            bounds.include(marker.getPosition());
            markerInfos.put(marker, info);
        }

        // We need to wait for the map to be layed out before we can animate to
        // show all the points
        final LinearLayout map = (LinearLayout) findViewById(R.id.trigger_maps_container);
        if (map.getViewTreeObserver().isAlive()) {
            map.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    map.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                    Location myLocation = Utilities.getCurrentLocation(LocTrigMapsActivity.this);
                    if (myLocation != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(myLocation.getLatitude(), myLocation.getLongitude()), 16));
                    }

                    if (!markerInfos.isEmpty()) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(),
                                getResources().getDimensionPixelSize(R.dimen.map_marker_gutter)));
                    }
                }
            });
        }

        // Display appropriate title text
        updateTitleText();

        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                showHelpDialog();
            }
        };

        if (!shouldSkipToolTip()) {
            // Show the tool tip after a small delay
            new Handler().postDelayed(runnable, TOOL_TIP_DELAY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Analytics.activity(this, Status.ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Analytics.activity(this, Status.OFF);
    }

    @Override
    public void onDestroy() {
        if (mSearchTask != null) {
            mSearchTask.cancel(true);
            mSearchTask = null;
        }

        mDb.close();
        mDb = null;

        super.onDestroy();
    }

    /**
     * Called when the user wants to add a new trigger location
     */
    public void onAddLocation() {
        // Add this location with default radius. But before that,
        // check if it is too close to any other category
        if (mAddMarker == null)
            return;

        LatLng gp = mAddMarker.getPosition();

        String cName = chekLocOverlap(mCategId, gp, LocTrigConfig.LOC_RADIUS_DEFAULT);

        if (cName != null) {
            displayMessage(cName, R.string.loc_overlap_msg);
            resetMapPins();
            return;
        }

        MarkerInfo info = new MarkerInfo();
        info.id = mDb.addLocation(mCategId, gp, LocTrigConfig.LOC_RADIUS_DEFAULT);
        info.radius = LocTrigConfig.LOC_RADIUS_DEFAULT;

        mAddMarker.hideInfoWindow();
        Marker m = mAddMarker;
        mAddMarker = null;
        markerInfos.put(m, info);

        onRadiusUpdateFocus(m);

        // We need to notify the location service that a point has been added
        notifyService();
    }

    /**
     * Called when the user long presses on the map to add a new location marker
     * on the map.
     * 
     * @param point
     */
    private void onAddTemporaryMarker(LatLng point) {
        resetMapPins();

        showAddMarker(new MarkerOptions().position(point).title(
                getString(R.string.trigger_loc_loading_address)));

        // Start a task to determine the address of the location
        mMarkerAddressTask = new FetchAddressTask();
        mMarkerAddressTask.execute(point);
    }

    /**
     * Test to see if we are pressing inside the radius of the current focused
     * marker
     * 
     * @param point
     * @return
     */
    private boolean testMarkerRadiusUpdate(LatLng point) {
        if (mRadiusCircle != null) {
            float[] results = new float[1];
            Location.distanceBetween(point.latitude, point.longitude,
                    mRadiusCircle.getCenter().latitude, mRadiusCircle.getCenter().longitude,
                    results);
            return results[0] < mRadiusCircle.getRadius();
        }
        return false;
    }

    /**
     * Called when the user long presses inside the radius circle for focused
     * marker. Starts the radius resize.
     * 
     * @param marker
     */
    private void onRadiusUpdateFocus(Marker marker) {
        resetMapPins();

        mRadiusMarker = marker;
        mRadiusCircle = mMap.addCircle(new CircleOptions().center(marker.getPosition())
                .radius(markerInfos.get(marker).radius)
                .fillColor(Utilities.setAlpha(Color.RED, AlPHA_LIGHT))
                .strokeColor(Color.TRANSPARENT));
    }

    /**
     * Called when the radius is starting to be resized.
     */
    private void onRadiusUpdateStart() {
        mRadiusUpdate = true;
        mRadiusCircle.setFillColor(Color.TRANSPARENT);
        mRadiusCircle.setStrokeColor(Utilities.setAlpha(Color.RED, 100));
    }

    /**
     * Called when the radius is updated with the new location p of which can be
     * used to calculate the new radius
     * 
     * @param p
     */
    private void onRadiusUpdate(Point p) {
        LatLng touch = mMap.getProjection().fromScreenLocation(p);
        float[] results = new float[1];
        Location.distanceBetween(mRadiusCircle.getCenter().latitude,
                mRadiusCircle.getCenter().longitude, touch.latitude, touch.longitude, results);

        mRadiusCircle.setRadius(Math.min(Math.max(LocTrigConfig.LOC_RADIUS_MIN, results[0]),
                LocTrigConfig.LOC_RADIUS_MAX));
    }

    /**
     * Called when the radius update operation is finished. When the user clicks
     * outside of the circle.
     */
    private void onRadiusUpdateStop() {
        String cName = chekLocOverlap(mCategId, mRadiusCircle.getCenter(),
                mRadiusCircle.getRadius());
        MarkerInfo info = markerInfos.get(mRadiusMarker);

        if (cName != null) {
            // Overlapping, restore the radius
            displayMessage(cName, R.string.loc_too_big_msg);
            mRadiusCircle.setRadius(info.radius);
        } else {
            // Everything ok, update the radius in the db and
            // notify service
            info.radius = mRadiusCircle.getRadius();
            mDb.updateLocationRadius(info.id, info.radius);
            notifyService();
        }
        mRadiusCircle.setFillColor(Utilities.setAlpha(Color.RED, AlPHA_LIGHT));
        mRadiusCircle.setStrokeColor(Color.TRANSPARENT);

        mRadiusUpdate = false;
    }

    /**
     * Called when the user long presses on a marker. Will delete the marker
     * from the db.
     * 
     * @param point
     * @return
     */
    private boolean removeLocation(LatLng point) {
        Point click = mMap.getProjection().toScreenLocation(point);

        for (Marker marker : markerInfos.keySet()) {
            Point pin = mMap.getProjection().toScreenLocation(marker.getPosition());

            if (Math.abs(click.x - pin.x) < Utilities.dpToPixels(15)
                    && Math.abs(click.y - pin.y) < Utilities.dpToPixels(15)) {
                new DeleteLocDialog(marker).show(LocTrigMapsActivity.this);
                return true;
            }
        }
        return false;
    }

    /**
     * Resets the state of the map to show only pins which are in the db with no
     * pins having focus
     */
    private void resetMapPins() {
        if (mRadiusMarker != null)
            mRadiusMarker = null;

        mRadiusUpdate = false;

        if (mRadiusCircle != null) {
            mRadiusCircle.remove();
            mRadiusCircle = null;
        }

        if (mAddMarker != null) {
            mAddMarker.remove();
            mAddMarker = null;
        }

        if (mMarkerAddressTask != null) {
            mMarkerAddressTask.cancel(true);
            mMarkerAddressTask = null;
        }
    }

    private boolean shouldSkipToolTip() {
        SharedPreferences pref = getSharedPreferences(TOOL_TIP_PREF_NAME, Context.MODE_PRIVATE);
        return pref.getBoolean(KEY_TOOL_TIP_DO_NT_SHOW, false);
    }

    private void showHelpDialog() {
        Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.trigger_loc_maps_tips);
        dialog.setTitle(R.string.trigger_loc_defining_locations);
        dialog.setOwnerActivity(this);
        dialog.show();

        WebView webView = (WebView) dialog.findViewById(R.id.web_view);
        webView.loadUrl("file:///android_res/raw/trigger_loc_maps_help.html");

        CheckBox checkBox = (CheckBox) dialog.findViewById(R.id.check_do_not_show);
        checkBox.setChecked(shouldSkipToolTip());
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                SharedPreferences pref = LocTrigMapsActivity.this.getSharedPreferences(
                        TOOL_TIP_PREF_NAME, Context.MODE_PRIVATE);

                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean(KEY_TOOL_TIP_DO_NT_SHOW, isChecked);
                editor.commit();
            }
        });

        Button button = (Button) dialog.findViewById(R.id.button_close);
        button.setTag(dialog);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Object tag = v.getTag();
                if (tag != null && tag instanceof Dialog) {
                    ((Dialog) tag).dismiss();
                }
            }
        });
    }

    /**
     * Notify the {@link LocTrigService} of changes to the set of locations
     */
    private void notifyService() {
        Intent i = new Intent(this, LocTrigService.class);
        i.setAction(LocTrigService.ACTION_UPDATE_LOCATIONS);
        startService(i);
    }

    /**
     * Handles the search result. Will show a marker to display
     * 
     * @param adr
     */
    private void handleSearchResult(Address adr) {
        mSearchTask = null;
        showAddMarker(addressToMarker(adr));
    }

    /**
     * Converts an address to a marker to show on the map
     * 
     * @param adr
     * @return
     */
    private MarkerOptions addressToMarker(Address adr) {
        StringBuilder addrText = new StringBuilder();

        int addrLines = adr.getMaxAddressLineIndex();
        for (int i = 0; i < Math.min(2, addrLines); i++) {
            addrText.append(adr.getAddressLine(i)).append("\n");
        }
        return new MarkerOptions().position(new LatLng(adr.getLatitude(), adr.getLongitude()))
                .title(addrText.toString());
    }

    /**
     * Shows a marker on the map and animates to it. This is used when the user
     * is given the option of adding a new location.
     * 
     * @param options
     * @param point
     */
    private void showAddMarker(MarkerOptions options) {
        mAddMarker = mMap.addMarker(options);
        mAddMarker.showInfoWindow();
        CameraUpdate update;
        if(mMap.getCameraPosition().zoom < 16)
            update = CameraUpdateFactory.newLatLngZoom(options.getPosition(), 16);
        else
            update = CameraUpdateFactory.newLatLng(options.getPosition());
        mMap.animateCamera(update);
    }

    /**
     * Set dynamic help on the title
     */
    private void updateTitleText() {
        String title = null; // ;

        if (mRadiusCircle != null) {
            // a red marker is in focus
            title = getString(R.string.maps_title_focused);
        } else if (!markerInfos.isEmpty()) {
            // no focus and there are markers present
            title = getString(R.string.maps_title_nofocus);
        } else {
            String categName = mDb.getCategoryName(mCategId);
            title = getString(R.string.maps_tile_default, categName);
        }

        TextView header = (TextView) findViewById(R.id.text_maps_header);
        header.setText(title);
    }

    /**
     * Display a dialog message TODO: make a fragment
     * 
     * @param cName
     * @param resId
     */
    private void displayMessage(String cName, int resId) {
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.loc_overlap_title)
                .setMessage(getString(resId, cName))
                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();
        dialog.setOwnerActivity(this);
    }

    /**
     * Check if a location overlaps a location is another category.
     * 
     * @param categId
     * @param gp
     * @param radius
     * @return the name of the overlapping category. Returns null otherwise.
     */
    private String chekLocOverlap(int categId, LatLng gp, double radius) {

        String cName = null;
        Cursor c = mDb.getAllLocations();
        if (c.moveToFirst()) {
            do {
                int cId = c.getInt(c.getColumnIndexOrThrow(LocTrigDB.KEY_CATEGORY_ID));

                if (cId != categId) {

                    int lat = c.getInt(c.getColumnIndexOrThrow(LocTrigDB.KEY_LAT));
                    int lng = c.getInt(c.getColumnIndexOrThrow(LocTrigDB.KEY_LONG));

                    float locr = c.getFloat(c.getColumnIndexOrThrow(LocTrigDB.KEY_RADIUS));

                    // TODO: change the db to use decimal values
                    float[] dist = new float[1];
                    Location.distanceBetween(gp.latitude, gp.longitude, lat / 1E6, lng / 1E6, dist);

                    // Check overlap
                    if (dist[0] < locr + radius + LocTrigConfig.MIN_LOC_GAP) {
                        cName = mDb.getCategoryName(cId);
                        break;
                    }
                }

            } while (c.moveToNext());
        }

        c.close();
        // return the name of overlapping category
        return cName;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean ret = super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_SEARCH_ID, 1, R.string.menu_search).setIcon(
                android.R.drawable.ic_menu_search);
        menu.add(0, MENU_HELP_ID, 4, R.string.menu_help).setIcon(android.R.drawable.ic_menu_help);

        return ret;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean ret = super.onPrepareOptionsMenu(menu);

        menu.removeItem(MENU_SATELLITE_ID);

        int txt = R.string.trigger_loc_menu_satellite_mode;
        if (mMap.getMapType() == GoogleMap.MAP_TYPE_SATELLITE) {
            txt = R.string.trigger_loc_menu_map_mode;
        }

        menu.add(0, MENU_SATELLITE_ID, 3, txt).setIcon(android.R.drawable.ic_menu_mapmode);

        return ret;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SEARCH_ID: // Search an address
                // get the address
                TrigTextInput ti = new TrigTextInput(this);
                ti.setTitle(getString(R.string.search_addr_title));
                ti.setPositiveButtonText(getString(R.string.menu_search));
                ti.setNegativeButtonText(getString(R.string.cancel));
                ti.setAllowEmptyText(false);
                ti.setOnClickListener(new TrigTextInput.onClickListener() {
                    @Override
                    public void onClick(TrigTextInput ti, int which) {
                        if (which == TrigTextInput.BUTTON_POSITIVE) {
                            // Start the search task
                            mSearchTask = new SearchAddressTask().execute(ti.getText());
                        }
                    }
                });
                ti.showDialog().setOwnerActivity(this);

                return true;

            case MENU_SATELLITE_ID:
                mMap.setMapType((mMap.getMapType() == GoogleMap.MAP_TYPE_SATELLITE) ? GoogleMap.MAP_TYPE_NORMAL
                        : GoogleMap.MAP_TYPE_SATELLITE);
                return true;

            case MENU_HELP_ID: // Show help
                showHelpDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class MarkerInfo {
        public long id;
        public double radius;
    }

    /*
     * The class to display the delete message and delete the overlay item
     * (location) is required
     */
    private class DeleteLocDialog implements DialogInterface.OnClickListener {

        private final Marker mMarker;

        public DeleteLocDialog(Marker marker) {
            this.mMarker = marker;
        }

        /* Display the dialog */
        public void show(Context context) {
            AlertDialog dialog = new AlertDialog.Builder(context).setTitle(R.string.exisiting_loc)
                    .setMessage(R.string.delete_loc).setPositiveButton(R.string.delete, this)
                    .setNegativeButton(R.string.cancel, this).create();
            dialog.show();
            dialog.setOwnerActivity(LocTrigMapsActivity.this);
        }

        /* Delete the location */
        private void deleteLocation() {
            MarkerInfo info = markerInfos.get(mMarker);
            mDb.removeLocation(info.id);
            markerInfos.remove(mMarker);
            mMarker.remove();
            resetMapPins();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == AlertDialog.BUTTON_POSITIVE) {
                deleteLocation();
                notifyService();
            }

            dialog.dismiss();
        }
    }

    /* The search address task class. Performs address search in the bg thread */
    private class SearchAddressTask extends AddressTask<String> {

        private ProgressDialog mBusyPrg = null;

        @Override
        protected void onPreExecute() {
            // Start progress bar
            mBusyPrg = ProgressDialog.show(LocTrigMapsActivity.this, "",
                    getString(R.string.searching_msg), true);
            mBusyPrg.setOwnerActivity(LocTrigMapsActivity.this);
        }

        @Override
        protected List<Address> queryAddress(Geocoder geoCoder, String name) throws IOException {
            return geoCoder.getFromLocationName(name, 5);
        }

        @Override
        protected void onPostExecute(Address adr) {
            // Address search done, kill progressbar and notify

            if (mBusyPrg != null) {
                mBusyPrg.cancel();
                mBusyPrg = null;
            }

            if (adr != null && adr.hasLongitude() && adr.hasLatitude()) {
                handleSearchResult(adr);
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.search_fail),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /* Async task to fetch the address */
    private class FetchAddressTask extends AddressTask<LatLng> {

        @Override
        protected List<Address> queryAddress(Geocoder geoCoder, LatLng point) throws IOException {
            return geoCoder.getFromLocation(point.latitude, point.longitude, 2);
        }

        @Override
        protected void onPostExecute(Address address) {
            if (isCancelled())
                return;

            StringBuilder addr = new StringBuilder();

            int addrLines = address.getMaxAddressLineIndex();

            for (int i = 0; i < Math.min(2, addrLines); i++) {
                addr.append(address.getAddressLine(i)).append("\n");
            }

            if (TextUtils.isEmpty(addr)) {
                mAddMarker.setTitle(getApplicationContext().getString(
                        R.string.trigger_loc_address_error));
            } else {
                mAddMarker.setTitle(addr.toString());
            }
            mAddMarker.showInfoWindow();
        }
    }

    private abstract class AddressTask<T> extends AsyncTask<T, Void, Address> {

        // Number of retries of address look up in case of failures
        private static final int GEOCODING_RETRIES = 5;
        // Interval between consecutive address lookups
        private static final long GEOCODING_RETRY_INTERVAL = 500; // ms

        @Override
        protected Address doInBackground(T... params) {

            // Get the address
            Geocoder geoCoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            for (int cTry = 0; cTry < GEOCODING_RETRIES; cTry++) {
                try {
                    List<Address> addresses = queryAddress(geoCoder, params[0]);

                    if (isCancelled())
                        return null;

                    if (addresses.size() > 0) {
                        return addresses.get(0);
                    }
                } catch (Exception e) {
                }

                try {
                    Thread.sleep(GEOCODING_RETRY_INTERVAL);
                } catch (InterruptedException e) {
                }
            }

            return null;
        }

        protected abstract List<Address> queryAddress(Geocoder geoCoder, T input)
                throws IOException;
    }
}
