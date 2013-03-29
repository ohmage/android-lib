
package org.ohmage.triggers.types.location;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

import org.ohmage.library.R;

public class LocTrigInfoAdapter implements InfoWindowAdapter {

    private LinearLayout mInfoView;

    LayoutInflater inflater = null;

    public LocTrigInfoAdapter(LayoutInflater inflater) {
        this.inflater = inflater;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {

        if (mInfoView == null) {
            mInfoView = (LinearLayout) inflater.inflate(R.layout.trigger_loc_maps_balloon, null);
        }

        TextView text = (TextView) mInfoView.findViewById(R.id.trigger_add_address);
        text.setText(marker.getTitle());

        return mInfoView;
    }
}
