/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.activity;

import java.util.Arrays;

import org.ohmage.BackgroundManager;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApplication;
import org.ohmage.R;
import org.ohmage.SharedPreferencesHelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import edu.ucla.cens.systemlog.Log;

public class LoginActivity extends Activity {
	
	public static final String TAG = "LoginActivity";
	
	/**
	 * The {@link LoginActivity} looks for this extra to determine if
	 * it should update the credentials for the user rather than just passing through
	 */
	public static final String EXTRA_UPDATE_CREDENTIALS = "extra_update_credentials";
	
    private static final int DIALOG_FIRST_RUN = 1;
    private static final int DIALOG_LOGIN_ERROR = 2;
    private static final int DIALOG_NETWORK_ERROR = 3;
    private static final int DIALOG_LOGIN_PROGRESS = 4;
    private static final int DIALOG_INTERNAL_ERROR = 5;
    private static final int DIALOG_USER_DISABLED = 6;

	private static final int LOGIN_FINISHED = 0;
	
	private EditText mUsernameEdit;
	private EditText mPasswordEdit;
	private Button mLoginButton;
	private TextView mVersionText;
	private SharedPreferencesHelper mPreferencesHelper;
	private LoginTask mTask;

	private boolean mUpdateCredentials;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		setContentView(R.layout.login);
		setTitle(getTitle() + " login");
		
		// first see if they are already logged in
		final SharedPreferencesHelper preferencesHelper = new SharedPreferencesHelper(this);
		
		if (preferencesHelper.isUserDisabled()) {
        	((OhmageApplication) getApplication()).resetAll();
        }
		
		mUpdateCredentials = getIntent().getBooleanExtra(EXTRA_UPDATE_CREDENTIALS, false);
		
		// if they are, redirect them to the dashboard
		if (preferencesHelper.isAuthenticated() && !mUpdateCredentials) {
			startActivityForResult(new Intent(this, DashboardActivity.class), LOGIN_FINISHED);
			return;
		}
		
		mLoginButton = (Button) findViewById(R.id.login);
        mUsernameEdit = (EditText) findViewById(R.id.user_input); 
        mPasswordEdit = (EditText) findViewById(R.id.password);
        mVersionText = (TextView) findViewById(R.id.version);
        
        try {
			mVersionText.setText("v" + getPackageManager().getPackageInfo("org.ohmage", 0).versionName);
		} catch (Exception e) {
			Log.e(TAG, "unable to retrieve version", e);
			mVersionText.setText(" ");
		}
        
        mLoginButton.setOnClickListener(mClickListener);
        
        mPreferencesHelper = new SharedPreferencesHelper(this);
        
        Object retained = getLastNonConfigurationInstance();
        
        if (retained instanceof LoginTask) {
        	Log.i(TAG, "creating after configuration changed, restored LoginTask instance");
        	mTask = (LoginTask) retained;
        	mTask.setActivity(this);
        } else {
        	Log.i(TAG, "creating from scratch");
        	
        	//clear login fail notification (if such notification existed) 
        	//NotificationHelper.cancel(this, NotificationHelper.NOTIFY_LOGIN_FAIL, null);
        	//move this down so notification is only cleared if login is successful???
        	
        	//the following block is commented out to support single user lock-in, implemented in the code below
        	/*String username = mPreferencesHelper.getUsername();
            if (username.length() > 0) {
            	Log.i(TAG, "saved credentials exist");
    			mUsernameEdit.setText(username);
            	mPasswordEdit.setText(DUMMY_PASSWORD);
            	//launch main activity and finish
            	//WE CAN'T DO THIS BECAUSE IF THE ACTIVITY IS LAUNCHED AFTER AN HTTP ERROR NOTIFICATION, CREDS WILL STILL BE STORED
            	//startActivity(new Intent(LoginActivity.this, MainActivity.class));
            	//finish();
            	//OR do Login?
            	doLogin();
            	
            	
            	//what if this works the other way around? make main activity the starting point, and launch login if needed? 
            } else {
            	Log.i(TAG, "saved credentials do not exist");
            }*/
            
            if (mPreferencesHelper.isUserDisabled()) {
            	((OhmageApplication) getApplication()).resetAll();
            }
        	
        	//code for single user lock-in
        	String username = mPreferencesHelper.getUsername();
        	if (username.length() > 0) {
        		Log.i(TAG, "saved username exists");
    			mUsernameEdit.setText(username);
    			mUsernameEdit.setEnabled(false);
    			mUsernameEdit.setFocusable(false);
    			
//    			String hashedPassword = mPreferencesHelper.getHashedPassword();
//    			if (hashedPassword.length() > 0) {
//    				Log.i(TAG, "saved password exists");
//    				mPasswordEdit.setText(DUMMY_PASSWORD);
//    				//doLogin();
//    			} else {
//    				Log.i(TAG, "no saved password, must have had a login problem");
//    			}
    			
        	} else {
        		Log.i(TAG, "no saved username");
        	}
        }
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		Log.i(TAG, "configuration change");
		if (mTask != null) {
			Log.i(TAG, "retaining LoginTask instance");
			mTask.setActivity(null);
			return mTask;
		}
		return null;
	}

	private final OnClickListener mClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.login:
				Log.i(TAG, "login button clicked");
				doLogin();				
				break;
			}
		}
	};

	private void doLogin() {
		String username = mUsernameEdit.getText().toString();
		String password = mPasswordEdit.getText().toString();
		
		mTask = new LoginTask(LoginActivity.this);
		mTask.execute(username, password);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = super.onCreateDialog(id);
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		switch (id) {
		case DIALOG_FIRST_RUN:
        	dialogBuilder.setTitle("Welcome")
        				.setMessage("Disclaimer/Agreement stuff goes here.")
        				.setCancelable(false)
        				.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								mPreferencesHelper.setFirstRun(false);
							}
						})
						.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								LoginActivity.this.finish();
							}
						});
        	dialog = dialogBuilder.create();
        	break;
        	
		case DIALOG_LOGIN_ERROR:
        	dialogBuilder.setTitle("Error")
        				.setMessage("Unable to authenticate. Please check username and update the password.")
        				.setCancelable(true)
        				.setPositiveButton("OK", null)
        				/*.setNeutralButton("Help", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startActivity(new Intent(LoginActivity.this, HelpActivity.class));
								//put extras for specific help on login error
							}
						})*/;
				//add button for contact
				dialog = dialogBuilder.create();        	
				break;

			case DIALOG_USER_DISABLED:
				dialogBuilder.setTitle(R.string.login_error)
				.setMessage(R.string.login_account_disabled)
				.setCancelable(true)
				.setPositiveButton(R.string.ok, null)
				/*.setNeutralButton("Help", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startActivity(new Intent(LoginActivity.this, HelpActivity.class));
								//put extras for specific help on login error
							}
						})*/;
				//add button for contact
				dialog = dialogBuilder.create();        	
				break;

			case DIALOG_NETWORK_ERROR:
				dialogBuilder.setTitle(R.string.login_error)
				.setMessage(R.string.login_network_error)
				.setCancelable(true)
				.setPositiveButton(R.string.ok, null)
				/*.setNeutralButton("Help", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startActivity(new Intent(LoginActivity.this, HelpActivity.class));
								//put extras for specific help on http error
							}
						})*/;
				//add button for contact
				dialog = dialogBuilder.create();
				break;

			case DIALOG_INTERNAL_ERROR:
				dialogBuilder.setTitle(R.string.login_error)
				.setMessage(R.string.login_server_error)
				.setCancelable(true)
				.setPositiveButton(R.string.ok, null)
				/*.setNeutralButton("Help", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startActivity(new Intent(LoginActivity.this, HelpActivity.class));
								//put extras for specific help on http error
							}
						})*/;
        	//add button for contact
        	dialog = dialogBuilder.create();
        	break;
        	
		case DIALOG_LOGIN_PROGRESS:
			ProgressDialog pDialog = new ProgressDialog(this);
			pDialog.setMessage(getString(R.string.login_authenticating, getString(R.string.server_name)));
			pDialog.setCancelable(false);
			//pDialog.setIndeterminate(true);
			dialog = pDialog;
        	break;
		}
		
		return dialog;
	}
	
	private void onLoginTaskDone(OhmageApi.AuthenticateResponse response, String username) {
		
		mTask = null;
		
		try {
			dismissDialog(DIALOG_LOGIN_PROGRESS);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Attempting to dismiss dialog that had not been shown.");
			e.printStackTrace();
		}
		
		switch (response.getResult()) {
		case SUCCESS:
			Log.i(TAG, "login success");
			
			String hashedPassword = response.getHashedPassword();
			//save creds
			mPreferencesHelper.putUsername(username);
			mPreferencesHelper.putHashedPassword(hashedPassword);
			mPreferencesHelper.putLastMobilityUploadTimestamp(System.currentTimeMillis());
			
			//clear related notifications
			//NotificationHelper.cancel(LoginActivity.this, NotificationHelper.NOTIFY_LOGIN_FAIL);
			//makes more sense to clear notification on launch, so moved to oncreate
			
			//start services
			//set alarms
			//register receivers
			//BackgroundManager.initAuthComponents(this);
			
			
			
			boolean isFirstRun = mPreferencesHelper.isFirstRun();
            
			if (isFirstRun) {
            	Log.i(TAG, "this is the first run");
            	
            	BackgroundManager.initComponents(this);
            	
            	//cancel get started notification. this works regardless of how we start the app (notification or launcher)
            	//NotificationHelper.cancel(this, NotificationHelper.NOTIFY_GET_STARTED, null);
            	
            	//show intro dialog
            	//showDialog(DIALOG_FIRST_RUN);
            	mPreferencesHelper.setFirstRun(false);
            } else {
            	Log.i(TAG, "this is not the first run");
            }
			
			if(mUpdateCredentials)
				finish();
			else
				startActivityForResult(new Intent(this, DashboardActivity.class), LOGIN_FINISHED);
			break;
		case FAILURE:
			Log.e(TAG, "login failure");
			for (String s : response.getErrorCodes()) {
				Log.e(TAG, "error code: " + s);
			}
			
			//clear creds
			//mPreferencesHelper.clearCredentials();
			//just clear password, keep username for single user lock-in
			// FAISAL: commenting this out so the user gets a chance to back out of a password change attempt
			/*mPreferencesHelper.putHashedPassword("");*/
			
			//clear password so user will re-enter it
			mPasswordEdit.setText("");
			
			//show error dialog
			if (Arrays.asList(response.getErrorCodes()).contains("0201")) {
				mPreferencesHelper.setUserDisabled(true);
				showDialog(DIALOG_USER_DISABLED);
			} else {
				showDialog(DIALOG_LOGIN_ERROR);
			}
			break;
		case HTTP_ERROR:
			Log.e(TAG, "login http error");
			
			//show error dialog
			showDialog(DIALOG_NETWORK_ERROR);
			break;
		case INTERNAL_ERROR:
			Log.e(TAG, "login internal error");
			
			//show error dialog
			showDialog(DIALOG_INTERNAL_ERROR);
			break;
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
			case LOGIN_FINISHED:
				finish();
			break;
			default:
				this.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	private static class LoginTask extends AsyncTask<String, Void, OhmageApi.AuthenticateResponse>{
		
		private LoginActivity mActivity;
		private boolean mIsDone = false;
		private String mUsername;
		private String mPassword;
		private OhmageApi.AuthenticateResponse mResponse = null;

		private LoginTask(LoginActivity activity) {
			this.mActivity = activity;
		}
		
		public void setActivity(LoginActivity activity) {
			this.mActivity = activity;
			if (mIsDone) {
				notifyTaskDone();
			}
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mActivity.showDialog(DIALOG_LOGIN_PROGRESS);
		}

		@Override
		protected OhmageApi.AuthenticateResponse doInBackground(String... params) {
			mUsername = params[0];
			mPassword = params[1];
			
//			if (mPassword.equals(DUMMY_PASSWORD)) {
//				Log.i(TAG, "using stored hashed password");
//			} else {
//				Log.i(TAG, "password field modified, attempting to hash");
//				try {
//		        	mHashedPassword = BCrypt.hashpw(mPassword, BCRYPT_KEY);
//		        	Log.i(TAG, "hash complete");
//		        } catch (Exception e) {
//		        	Log.e(TAG, "unable to hash password");
//		        	e.printStackTrace();
//		        }
//			}
			OhmageApi api = new OhmageApi(mActivity);
			return api.authenticate(SharedPreferencesHelper.DEFAULT_SERVER_URL, mUsername, mPassword, SharedPreferencesHelper.CLIENT_STRING);
		}

		@Override
		protected void onPostExecute(OhmageApi.AuthenticateResponse response) {
			super.onPostExecute(response);
			
			mResponse = response;
			mIsDone = true;
			notifyTaskDone();			
		}
		
		private void notifyTaskDone() {
			if (mActivity != null) {
				mActivity.onLoginTaskDone(mResponse, mUsername);
			}
		}
	}
}