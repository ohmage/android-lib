
package org.ohmage.responsesync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageContainer;

import org.ohmage.AccountHelper;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApplication;
import org.ohmage.db.Models.Response;
import org.ohmage.logprobe.Log;

import java.util.ArrayList;

public class ResponseImageLoader extends Service {
    public static final String EXTRA_IMAGES = "extra_images";
    private static final String TAG = "ResponseImageLoader";

    public ResponseImageLoader() {
    }

    public static class ResponseImage implements Parcelable {
        public ResponseImage(String c, String id) {
            campaign = c;
            uuid = id;
        }

        String campaign;
        String uuid;

        @Override
        public int describeContents() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(campaign);
            dest.writeString(uuid);
        }

        public static final Parcelable.Creator<ResponseImage> CREATOR = new Parcelable.Creator<ResponseImage>() {
            @Override
            public ResponseImage createFromParcel(Parcel in) {
                return new ResponseImage(in);
            }

            @Override
            public ResponseImage[] newArray(int size) {
                return new ResponseImage[size];
            }
        };

        private ResponseImage(Parcel in) {
            campaign = in.readString();
            uuid = in.readString();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        ArrayList<ResponseImage> responsePhotos = intent.getExtras().getParcelableArrayList(
                EXTRA_IMAGES);

        // We can now download the thumbnails for each response from newest to
        // oldest.
        ImageLoader imageLoader = OhmageApplication.getImageLoader();

        String url;
        for (int i = 0; i < responsePhotos.size(); i++) {
            final ResponseImage responseImage = responsePhotos.get(i);
            if (!AccountHelper.accountExists()) {
                Log.e(TAG, "User isn't logged in, terminating task");
                return START_NOT_STICKY;
            }
            url = OhmageApi
                    .defaultImageReadUrl(responseImage.uuid, responseImage.campaign, "small");
            Log.d(TAG, "downloading: " + responseImage.uuid);
            imageLoader.get(url, new ImageLoader.ImageListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                }

                @Override
                public void onResponse(ImageContainer response, boolean isImmediate) {
                    Log.d(TAG, "finished: " + responseImage.uuid);
                    Response.getTemporaryResponsesMedia(responseImage.uuid).delete();
                }
            });
        }

        Log.d(TAG, "finished downloading images");
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
