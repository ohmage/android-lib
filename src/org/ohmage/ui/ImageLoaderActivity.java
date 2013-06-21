
package org.ohmage.ui;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageContainer;

import org.ohmage.OhmageApplication;
import org.ohmage.library.R;
import org.ohmage.logprobe.Analytics;
import org.ohmage.logprobe.LogProbe.Status;
import org.ohmage.widget.TouchImageView;

/**
 * Will download a remote image into memory using the {@link ImageLoader} and
 * display it in an image view
 * 
 * @author Cameron Ketcham
 */
public class ImageLoaderActivity extends FragmentActivity {

    private static final String TAG = "ImageLoaderActivity";

    private ImageLoader mImageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.image_loader_layout);

        final TouchImageView image = (TouchImageView) findViewById(R.id.image);

        mImageLoader = OhmageApplication.getImageLoader();

        Uri data = getIntent().getData();

        if (data == null || !URLUtil.isNetworkUrl(data.toString())) {
            finish();
            Toast.makeText(getApplicationContext(), R.string.image_loader_failed,
                    Toast.LENGTH_SHORT).show();
        }

        mImageLoader.get(data.toString(), new ImageLoader.ImageListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                finish();
                Toast.makeText(getApplicationContext(), R.string.image_loader_failed,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(ImageContainer response, boolean isImmediate) {
                if(response.getBitmap() != null) {
                    image.setImageBitmap(response.getBitmap());
                }
            }
        });
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
}
