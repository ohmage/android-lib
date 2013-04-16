
package org.ohmage;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import org.ohmage.activity.OhmageLauncher;

import java.io.IOException;

/**
 * Helper class which makes it easy to show the account management dialogs such
 * as the auth pin dialog, logout dialog, etc.
 * 
 * @author cketcham
 */
public class AccountHelper {

    private static final String TAG = "AccountHelper";
    
    protected final AccountManager mAccountManager;

    public AccountHelper(Context context) {
        mAccountManager = AccountManager.get(context);
    }

    /**
     * Checks to see if there is an account to determine if the user is already
     * authenticated
     * 
     * @return true if there is an account, and false if there isn't
     */
    public static boolean accountExists() {
        Account[] accounts = OhmageApplication.getAccountManager().getAccountsByType(
                OhmageApplication.ACCOUNT_TYPE);
        return accounts != null && accounts.length > 0;
    }

    public Account getAccount() {
        Account[] accounts = mAccountManager.getAccountsByType(OhmageApplication.ACCOUNT_TYPE);
        if (accounts.length == 0)
            return null;
        return accounts[0];
    }

    /**
     * Returns the current account username
     * 
     * @return
     */
    public String getUsername() {
        Account account = getAccount();
        if (account != null)
            return account.name;
        return null;
    }

    /**
     * Retrieve the auth token. Either from our cached token or from the account
     * manager
     * 
     * @return the authtoken or null if we don't have it yet
     */
    public String getAuthToken() {
        if (Thread.currentThread() == Looper.getMainLooper().getThread())
            return mAccountManager.peekAuthToken(getAccount(), OhmageApplication.AUTHTOKEN_TYPE);

        try {
            AccountManagerFuture<Bundle> future = mAccountManager.getAuthToken(getAccount(),
                    OhmageApplication.AUTHTOKEN_TYPE, false, null, null);
            Bundle result = future.getResult();
            if (result != null)
                return result.getString(AccountManager.KEY_AUTHTOKEN);
        } catch (OperationCanceledException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (AuthenticatorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Account no longer exists");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Take the user to the login activity, but allow them to back out of it if
     * they change their mind
     * 
     * @return
     */
    public AccountManagerFuture<Bundle> updatePassword() {
        Account account = getAccount();
        if (account != null) {
            return mAccountManager.confirmCredentials(account, null, null, null, null);
        }
        return null;
    }

    /**
     * Returns the intent to update the password. Should not be run on UI thread
     * 
     * @param context
     * @return intent to update the password
     */
    public static Intent updatePasswordIntent(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(OhmageApplication.ACCOUNT_TYPE);
        AccountManagerFuture<Bundle> future = accountManager.confirmCredentials(accounts[0], null,
                null, null, null);
        try {
            return (Intent) future.getResult().get(AccountManager.KEY_INTENT);
        } catch (OperationCanceledException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (AuthenticatorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static Intent getLoginIntent(Context context) {
        Intent intent = new Intent(context, OhmageLauncher.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}
