
package org.ohmage.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.ohmage.UserPreferencesHelper;
import org.ohmage.Utilities;
import org.ohmage.adapters.PopupAdapter;
import org.ohmage.db.DbContract.Campaigns;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.DbContract.Surveys;
import org.ohmage.db.utils.ISO8601Utilities;
import org.ohmage.library.R;
import org.ohmage.service.SurveyGeotagService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;

public class ResponseMapFragment extends FilterableMapFragment {

    private static final String RESPONSE_ID = "response_id";

    private Button mMapPinNext;
    private Button mMapPinPrevious;
    private TextView mMapPinIdxButton;
    private int mPinIndex;

    private final ArrayList<Marker> mMarkers = new ArrayList<Marker>();
    private final HashMap<Marker, Integer> mMarkerIds = new HashMap<Marker, Integer>();

    private Long mResponseId;

    /**
     * Create an instance of {@link ResponseMapFragment} which creates a point
     * for a single response
     * 
     * @param the response id
     */
    public static ResponseMapFragment newInstance(long responseId) {
        ResponseMapFragment f = new ResponseMapFragment();

        Bundle args = new Bundle();
        args.putLong(RESPONSE_ID, responseId);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle args) {
        super.onCreate(args);

        if (getArguments() != null && getArguments().containsKey(RESPONSE_ID))
            mResponseId = getArguments().getLong(RESPONSE_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        if (getMap() == null)
            return view;

        // Start by showing the current location of the user
        Location myLocation = Utilities.getCurrentLocation(getActivity());
        if (myLocation != null) {
            getMap().moveCamera(
                    CameraUpdateFactory.newLatLng(new LatLng(myLocation.getLatitude(), myLocation
                            .getLongitude())));
        }

        // Set the map to use our popup layout
        getMap().setInfoWindowAdapter(new PopupAdapter(inflater));
        if (mResponseId == null) {
            getMap().setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

                @Override
                public void onInfoWindowClick(Marker marker) {
                    Uri uri = Responses.buildResponseUri(mMarkerIds.get(marker));
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                }
            });
        }

        // Add navigation controls
        inflater.inflate(R.layout.response_map_navigator_layout, (ViewGroup) view);

        mMapPinNext = (Button) view.findViewById(R.id.map_pin_next);
        mMapPinPrevious = (Button) view.findViewById(R.id.map_pin_previous);
        mMapPinIdxButton = (TextView) view.findViewById(R.id.map_pin_index);

        mMapPinNext.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int overlayListSize = mMarkers.size();
                if (overlayListSize > 0) {
                    if (mPinIndex < (overlayListSize - 1)) {
                        mPinIndex = (mPinIndex + 1) % overlayListSize;
                        setNavigatorButtons();
                    }
                }
            }
        });

        mMapPinPrevious.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int overlayListSize = mMarkers.size();
                if (overlayListSize > 0) {
                    if (mPinIndex > 0) {
                        mPinIndex = (mPinIndex - 1) % overlayListSize;
                        setNavigatorButtons();
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // When the map comes back it could have data from another map on it, so
        // we need to restart our loader
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mResponseId == null) {
            return new ResponseLoader(this, ResponseMapQuery.PROJECTION,
                    Responses.RESPONSE_LOCATION_STATUS + "='" + SurveyGeotagService.LOCATION_VALID
                            + "'").onCreateLoader(id, args);
        } else
            return new CursorLoader(getActivity(), Responses.buildResponseUri(mResponseId),
                    ResponseMapQuery.PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        GoogleMap map = getMap();
        if (map == null)
            return;

        mMarkerIds.clear();
        mMarkers.clear();
        Builder bounds = new LatLngBounds.Builder();
        map.clear();

        while (cursor.moveToNext()) {
            int id = cursor.getInt(ResponseMapQuery.ID);

            Double lat = cursor.getDouble(ResponseMapQuery.LOCATION_LATITUDE);
            Double lon = cursor.getDouble(ResponseMapQuery.LOCATION_LONGITUDE);
            LatLng point = new LatLng(lat, lon);

            StringBuilder title = new StringBuilder(cursor.getString(ResponseMapQuery.TITLE));

            if (!UserPreferencesHelper.isSingleCampaignMode()) {
                title.insert(0, cursor.getString(ResponseMapQuery.CAMPAIGN_NAME) + "\n");
            }

            long millis = cursor.getLong(ResponseMapQuery.TIME);
            TimeZone timezone = TimeZone.getTimeZone(cursor.getString(ResponseMapQuery.TIMEZONE));

            Marker marker = map.addMarker(new MarkerOptions().position(point)
                    .title(title.toString()).snippet(ISO8601Utilities.print(millis, timezone)));
            mMarkers.add(0, marker);
            mMarkerIds.put(marker, id);
            bounds.include(marker.getPosition());

        }

        if (!mMarkers.isEmpty()) {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), getResources()
                    .getDimensionPixelSize(R.dimen.map_marker_gutter)));
        }

        // Set Map Pin Navigators.
        if (mResponseId == null) {
            mMapPinIdxButton.setVisibility(View.VISIBLE);
            mMapPinNext.setVisibility(View.VISIBLE);
            mMapPinPrevious.setVisibility(View.VISIBLE);
        } else {
            mMapPinIdxButton.setVisibility(View.GONE);
            mMapPinNext.setVisibility(View.GONE);
            mMapPinPrevious.setVisibility(View.GONE);
        }

        mPinIndex = -1;
        mMapPinIdxButton.setText("");
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mMarkerIds.clear();
        mMarkers.clear();
        GoogleMap map = getMap();
        if (map != null)
            map.clear();
    }

    protected static class ResponseMapQuery {
        public static final String[] PROJECTION = new String[] {
                Responses._ID, Responses.RESPONSE_LOCATION_STATUS,
                Responses.RESPONSE_LOCATION_LATITUDE, Responses.RESPONSE_LOCATION_LONGITUDE,
                Surveys.SURVEY_TITLE, Campaigns.CAMPAIGN_NAME, Responses.RESPONSE_TIME,
                Responses.RESPONSE_TIMEZONE
        };

        public static final int ID = 0;
        public static final int LOCATION_STATUS = 1;
        public static final int LOCATION_LATITUDE = 2;
        public static final int LOCATION_LONGITUDE = 3;
        public static final int TITLE = 4;
        public static final int CAMPAIGN_NAME = 5;
        public static final int TIME = 6;
        public static final int TIMEZONE = 7;
    }

    public void setNavigatorButtons() {
        if (mPinIndex == -1)
            mMapPinIdxButton.setText(null);
        else {
            mMapPinIdxButton.setText("" + (mPinIndex + 1) + "/" + mMarkers.size());
            mMarkers.get(mPinIndex).showInfoWindow();
            getMap().animateCamera(
                    CameraUpdateFactory.newLatLng(mMarkers.get(mPinIndex).getPosition()));
        }
    }

}
