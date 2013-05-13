
package org.ohmage.prompt;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

public interface Displayable {

    final static int REQUEST_CODE = 0;

    View inflateView(Context context, ViewGroup parent);

    void handleActivityResult(Context context, int resultCode, Intent data);

    // Called by the survey activity when a prompt leaves the screen
    void onHidden();

}
