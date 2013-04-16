
package org.ohmage.async;

import android.content.Context;

import org.ohmage.AccountHelper;
import org.ohmage.OhmageApi.Response;

/**
 * A custom Loader that uses a username and password
 */
public abstract class AuthenticatedTaskLoader<T extends Response> extends PauseableTaskLoader<T> {
    private final AccountHelper mAccount;

    private static final String TAG = "AuthenticatedTask";

    public AuthenticatedTaskLoader(Context context) {
        super(context);
        mAccount = new AccountHelper(context);
        pause(!AccountHelper.accountExists());
    }

    public String getUsername() {
        return mAccount.getUsername();
    }

    public String getHashedPassword() {
        return mAccount.getAuthToken();
    }

    @Override
    public void deliverResult(T response) {
        response.handleError(getContext());
        super.deliverResult(response);
    }
}
