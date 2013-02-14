package org.ohmage.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.imageloader.ImageLoader;

import org.ohmage.OhmageCache;
import org.ohmage.library.R;
import org.ohmage.logprobe.Analytics;
import org.ohmage.logprobe.LogProbe.Status;
import org.ohmage.widget.TouchImageView;

import java.io.File;
import java.net.URI;

/**
 * Will download a remote image into memory using the {@link ImageLoader} and 
 * display it in an image view
 * 
 * @author Cameron Ketcham
 *
 */
public class ImageLoaderActivity extends FragmentActivity {

	private static final String TAG = "ImageLoaderActivity";

	private ImageLoader mImageLoader;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.image_loader_layout);

		TouchImageView image = (TouchImageView) findViewById(R.id.image);

		mImageLoader = ImageLoader.get(this);
		mImageLoader.bind(image, getIntent().getData().toString(), new ImageLoader.Callback() {

			@Override
			public void onImageLoaded(ImageView view, String url) {
				// remove cache file from the sdcard if it exists
				// it will still remain in memory until android reclaims it
				File image = OhmageCache.getCachedFile(getApplicationContext(), URI.create(url));
				image.delete();
			}

			@Override
			public void onImageError(ImageView view, String url, Throwable error) {
				finish();
				Toast.makeText(getApplicationContext(), R.string.image_loader_failed, Toast.LENGTH_SHORT).show();
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
