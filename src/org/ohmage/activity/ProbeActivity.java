
package org.ohmage.activity;

import android.os.Bundle;

import org.ohmage.library.R;
import org.ohmage.ui.BaseActivity;

public class ProbeActivity extends BaseActivity {

    private static final String TAG = "ProbeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.probe_layout);
    }
}
