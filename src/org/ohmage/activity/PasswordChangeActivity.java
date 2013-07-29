/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ohmage.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.ohmage.AccountHelper;
import org.ohmage.ConfigHelper;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApi.AuthenticateResponse;
import org.ohmage.OhmageApi.Result;
import org.ohmage.library.R;

/**
 * Activity which displays login screen to the user.
 */
public class PasswordChangeActivity extends SherlockFragmentActivity {
    private static final String TAG = "PasswordChangeActivity";
    public static final String OLD_PASSWORD = "old_password";
    public static final String NEW_PASSWORD = "new_password";
    public static final String ACCOUNT_NAME = "account_name";

    private String mUsername;
    private String mOldPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUsername = getIntent().getStringExtra(ACCOUNT_NAME);
        mOldPassword = getIntent().getStringExtra(OLD_PASSWORD);

        if (TextUtils.isEmpty(mUsername)) {
            Account account = new AccountHelper(this).getAccount();
            if (account == null) {
                // If there is no account we can't change the password
                Toast.makeText(this, "No Account exists on this device", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            mUsername = account.name;
        }

        if (savedInstanceState == null) {
            Fragment newFragment = PasswordChangeFragment.getInstance(mUsername, mOldPassword);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(android.R.id.content, newFragment);
            ft.commit();
        }

    }

    public static class PasswordChangeFragment extends Fragment {

        private EditText mOldPassword;
        private EditText mNewPassword;
        private EditText mPasswordCheck;
        private String mAccountName;

        public static PasswordChangeFragment getInstance(String accountName, String oldPassword) {
            Bundle args = new Bundle();
            args.putString(ACCOUNT_NAME, accountName);
            args.putString(OLD_PASSWORD, oldPassword);
            PasswordChangeFragment f = new PasswordChangeFragment();
            f.setArguments(args);
            return f;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.change_password, container, false);

            String pass = getArguments().getString(OLD_PASSWORD);
            mOldPassword = (EditText) view.findViewById(R.id.old_password);
            if(!TextUtils.isEmpty(pass)) {
                mOldPassword.setText(pass);
                mOldPassword.setEnabled(false);
            }
            mNewPassword = (EditText) view.findViewById(R.id.new_password1);
            mPasswordCheck = (EditText) view.findViewById(R.id.new_password2);

            mAccountName = getArguments().getString(ACCOUNT_NAME);

            view.findViewById(R.id.change_password).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    String new1 = mNewPassword.getText().toString().trim();
                    String new2 = mPasswordCheck.getText().toString().trim();
                    if (!new1.equals(new2)) {
                        Toast.makeText(getActivity(), R.string.change_password_not_same,
                                Toast.LENGTH_SHORT).show();
                    } else if (!TextUtils.isEmpty(new1)) {
                        String old = mOldPassword.getText().toString().trim();
                        if (!TextUtils.isEmpty(old)) {
                            new PasswordChangeTask(getActivity(), mAccountName, old, new1)
                                    .execute();
                        }
                    }
                }
            });

            return view;
        }
    }

    public static class PasswordChangeTask extends AsyncTask<Void, Void, AuthenticateResponse> {

        private final String mOldPassword;
        private final String mNewPassword;
        private WaitDialog dialog;
        private final FragmentManager mFragmentManager;
        private final String mUsername;

        public PasswordChangeTask(FragmentActivity activity, String username, String oldPassword,
                String newPassword) {
            super();
            mFragmentManager = activity.getSupportFragmentManager();
            mUsername = username;
            mOldPassword = oldPassword;
            mNewPassword = newPassword;
        }

        @Override
        protected void onPreExecute() {
            dialog = new WaitDialog();
            dialog.show(mFragmentManager, "dialog");
        }

        @Override
        protected AuthenticateResponse doInBackground(Void... params) {
            return new OhmageApi().changePassword(ConfigHelper.serverUrl(), mUsername,
                    mOldPassword, mNewPassword);
        }

        @Override
        protected void onPostExecute(AuthenticateResponse result) {
            dialog.dismiss();
            Log.d(TAG, "error codes: " + result.getErrorCodes());
            if (result.hasAuthError())
                new InvalidPasswordDialog().show(mFragmentManager, "invalid_password");
            else if (result.getErrorCodes().contains("1001")) {
                new PasswordRequirementsDialog().show(mFragmentManager, "invalid_password");
            } else if (result.getResult() == Result.SUCCESS) {
                mFragmentManager.beginTransaction()
                        .add(FinishFragment.getInstance(result.getHashedPassword()), "finish")
                        .commit();
            }
        }
    }

    public static class WaitDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ProgressDialog dialog = new ProgressDialog(getActivity());
            dialog.setMessage(getString(R.string.changing_password));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            return dialog;
        }
    }

    public static class InvalidPasswordDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity()).setTitle("Invalid password")
                    .setMessage("The password you have entered is not your current password")
                    .setPositiveButton(R.string.continue_string, null).create();
        }
    }

    public static class PasswordRequirementsDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.change_password_invalid_title)
                    .setMessage(Html.fromHtml(getString(R.string.change_password_requirements)))
                    .setPositiveButton(R.string.continue_string, null).create();
        }
    }

    public static class FinishFragment extends Fragment {

        public static FinishFragment getInstance(String newPassword) {
            Bundle args = new Bundle();
            args.putString(NEW_PASSWORD, newPassword);
            FinishFragment f = new FinishFragment();
            f.setArguments(args);
            return f;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            Account account = new AccountHelper(getActivity()).getAccount();
            if (account != null) {
                AccountManager accountManager = AccountManager.get(getActivity());
                accountManager.setPassword(account, getArguments().getString(NEW_PASSWORD));
            }

            Toast.makeText(getActivity(), "Password changed successfully", Toast.LENGTH_SHORT)
                    .show();

            Intent data = new Intent();
            data.putExtra(NEW_PASSWORD, getArguments().getString(NEW_PASSWORD));
            getActivity().setResult(RESULT_OK, data);
            getActivity().finish();
        }
    }
}
