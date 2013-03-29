package org.ohmage.activity;

import android.content.ContentUris;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import org.ohmage.fragments.ResponseMapFragment;
import org.ohmage.library.R;
import org.ohmage.ui.BaseActivity;

public class ResponseInfoMapActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView();

		FragmentManager fm = getSupportFragmentManager();

		if (fm.findFragmentById(R.id.root_container) == null) {
			ResponseMapFragment map = ResponseMapFragment.newInstance(ContentUris.parseId(getIntent().getData()));
			fm.beginTransaction().add(R.id.root_container, map, "map").commit();
		}
	}
}
