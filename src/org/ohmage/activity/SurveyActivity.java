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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.format.DateUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.AccountHelper;
import org.ohmage.CampaignPreferencesHelper;
import org.ohmage.OhmageApplication;
import org.ohmage.PreferenceStore;
import org.ohmage.PromptXmlParser;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.conditionevaluator.DataPoint;
import org.ohmage.conditionevaluator.DataPoint.PromptType;
import org.ohmage.conditionevaluator.DataPointConditionEvaluator;
import org.ohmage.db.DbContract.Responses;
import org.ohmage.db.Models.Campaign;
import org.ohmage.db.Models.Response;
import org.ohmage.library.R;
import org.ohmage.logprobe.Analytics;
import org.ohmage.logprobe.Log;
import org.ohmage.logprobe.LogProbe.Status;
import org.ohmage.logprobe.OhmageAnalytics;
import org.ohmage.prompt.AbstractPrompt;
import org.ohmage.prompt.AbstractPromptFragment;
import org.ohmage.prompt.Displayable;
import org.ohmage.prompt.Message;
import org.ohmage.prompt.Prompt;
import org.ohmage.prompt.SurveyElement;
import org.ohmage.prompt.media.MediaPrompt;
import org.ohmage.prompt.media.MediaPromptFragment;
import org.ohmage.prompt.media.PhotoPrompt;
import org.ohmage.prompt.multichoicecustom.MultiChoiceCustomPrompt;
import org.ohmage.prompt.singlechoicecustom.SingleChoiceCustomPrompt;
import org.ohmage.service.SurveyGeotagService;
import org.ohmage.service.WakefulService;
import org.ohmage.triggers.glue.TriggerFramework;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

public class SurveyActivity extends SherlockFragmentActivity implements LocationListener {

    private static final String TAG = "SurveyActivity";

    private static final int DIALOG_CANCEL_ID = 0;
    private static final int DIALOG_INSTRUCTIONS_ID = 1;

    protected static final int PROMPT_RESULT = 0;

    private ProgressBar mProgressBar;
    private TextView mPromptText;
    private FrameLayout mPromptFrame;
    private Button mPrevButton;
    private Button mSkipButton;
    private Button mNextButton;

    private List<SurveyElement> mSurveyElements;
    // private List<PromptResponse> mResponses;
    private int mCurrentPosition;
    private String mCampaignUrn;
    private String mSurveyId;
    private String mSurveyTitle;
    private String mSurveySubmitText;
    private long mLaunchTime;
    private boolean mReachedEnd;
    private boolean mSurveyFinished = false;

    private LocationManager mLocManager;

    private final Handler mHandler = new Handler();

    private String mInstructions;

    private CampaignPreferencesHelper mCampaignPref;

    public String getSurveyId() {
        return mSurveyId;
    }

    public String getCampaignUrn() {
        return mCampaignUrn;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().hasExtra("campaign_urn")) {
            mCampaignUrn = getIntent().getStringExtra("campaign_urn");
        } else if (UserPreferencesHelper.isSingleCampaignMode()) {
            mCampaignUrn = Campaign.getSingleCampaign(this);
        } else {
            throw new RuntimeException("The campaign urn must be passed to the Survey Activity");
        }

        mCampaignPref = new CampaignPreferencesHelper(this, mCampaignUrn);

        mSurveyId = getIntent().getStringExtra("survey_id");
        mSurveyTitle = getIntent().getStringExtra("survey_title");
        mSurveySubmitText = getIntent().getStringExtra("survey_submit_text");

        getSupportActionBar().setTitle(mSurveyTitle);

        // Create the location manager and start listening to the GPS
        mLocManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        NonConfigurationInstance instance = (NonConfigurationInstance) getLastCustomNonConfigurationInstance();

        if (instance == null) {

            Calendar now = Calendar.getInstance();
            mLaunchTime = now.getTimeInMillis();

            final PreferenceStore preferencesHelper = new PreferenceStore(this);

            if (preferencesHelper.isUserDisabled()) {
                ((OhmageApplication) getApplication()).resetAll();
            }

            if (!AccountHelper.accountExists()) {
                Log.v(TAG, "no credentials saved, so launch Login");
                startActivity(AccountHelper.getLoginIntent(this));
                finish();
                return;
            } else {

                mInstructions = null;

                try {
                    mInstructions = PromptXmlParser.parseCampaignInstructions(Campaign
                            .loadCampaignXml(this, mCampaignUrn));
                } catch (XmlPullParserException e) {
                    Log.e(TAG, "Error parsing campaign instructions from xml", e);
                } catch (IOException e) {
                    Log.e(TAG, "Error parsing campaign instructions from xml", e);
                }

                if (mInstructions != null && mCampaignPref.showInstructions())
                    showDialog(DIALOG_INSTRUCTIONS_ID);

                mSurveyElements = null;

                try {
                    mSurveyElements = PromptXmlParser.parseSurveyElements(this, mCampaignUrn, mSurveyId);
                } catch (NotFoundException e) {
                    Log.e(TAG, "Error parsing prompts from xml", e);
                } catch (XmlPullParserException e) {
                    Log.e(TAG, "Error parsing prompts from xml", e);
                } catch (IOException e) {
                    Log.e(TAG, "Error parsing prompts from xml", e);
                }

                if (mSurveyElements == null || mSurveyElements.isEmpty()) {
                    // If there are no survey elements, something is wrong
                    finish();
                    Toast.makeText(this, R.string.invalid_survey, Toast.LENGTH_SHORT).show();
                    return;
                }

                mCurrentPosition = 0;
                mReachedEnd = false;
            }
        } else {
            mSurveyElements = instance.surveyElements;
            mCurrentPosition = instance.index;
            mLaunchTime = instance.launchTime;
            mReachedEnd = instance.reachedEnd;
            mLastElement = instance.lastElement;
            mSurveyFinished = instance.surveyFinished;
            mInstructions = instance.instructions;
        }

        setContentView(R.layout.survey_activity);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mPromptText = (TextView) findViewById(R.id.prompt_text);
        mPromptText.setMovementMethod(ScrollingMovementMethod.getInstance());
        mPromptFrame = (FrameLayout) findViewById(R.id.prompt_frame);
        mPrevButton = (Button) findViewById(R.id.prev_button);
        mSkipButton = (Button) findViewById(R.id.skip_button);
        mNextButton = (Button) findViewById(R.id.next_button);

        mPrevButton.setOnClickListener(mClickListener);
        mSkipButton.setOnClickListener(mClickListener);
        mNextButton.setOnClickListener(mClickListener);
    }

    /**
     * Stops the gps from running
     */
    Runnable stopUpdates = new Runnable() {
        @Override
        public void run() {
            mLocManager.removeUpdates(SurveyActivity.this);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        Analytics.activity(this, Status.ON);

        if (mReachedEnd == false) {
            showElement(mCurrentPosition);
        } else {
            showSubmitScreen();
        }

        // Start the gps location listener to just listen until it gets a lock
        // or until a minute passes and then turn off
        // This is just to warm up the gps for when the response is actually
        // submitted
        if(mLocManager.getAllProviders().contains(LocationManager.GPS_PROVIDER))
            mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        mHandler.removeCallbacks(stopUpdates);
        mHandler.postDelayed(stopUpdates, DateUtils.MINUTE_IN_MILLIS);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mSurveyElements != null && mCurrentPosition < mSurveyElements.size()
                && mSurveyElements.get(mCurrentPosition) instanceof PhotoPrompt)
            PhotoPrompt.clearView(mPromptFrame);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (SurveyGeotagService.locationValid(location)) {
            // We got a good enough location so lets stop the gps
            mLocManager.removeUpdates(this);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return new NonConfigurationInstance(mSurveyElements, mCurrentPosition, mLaunchTime,
                mReachedEnd, mLastElement, mSurveyFinished, mInstructions);
    }

    private class NonConfigurationInstance {
        List<SurveyElement> surveyElements;
        int index;
        long launchTime;
        boolean reachedEnd;
        SurveyElement lastElement;
        boolean surveyFinished;
        String instructions;

        public NonConfigurationInstance(List<SurveyElement> surveyElements, int index,
                long launchTime, boolean reachedEnd,
                SurveyElement element, boolean surveyFinished, String instructions) {
            this.surveyElements = surveyElements;
            this.index = index;
            this.launchTime = launchTime;
            this.reachedEnd = reachedEnd;
            this.lastElement = element;
            this.surveyFinished = surveyFinished;
            this.instructions = instructions;
        }
    }

    private final OnClickListener mClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (mCurrentPosition < mSurveyElements.size()
                    && mSurveyElements.get(mCurrentPosition) instanceof AbstractPrompt) {
                // Tell the current prompt that it is being hidden
                ((AbstractPrompt) mSurveyElements.get(mCurrentPosition)).onHidden();
            }

            int id = v.getId();
            if (id == R.id.next_button) {
                if (mReachedEnd) {
                    if (!mSurveyFinished) {
                        mSurveyFinished = true;
                        String uuid = storeResponse();
                        Analytics.widget(v, null, uuid);
                        TriggerFramework.notifySurveyTaken(SurveyActivity.this, mCampaignUrn,
                                mSurveyTitle);
                        PreferenceStore prefs = new PreferenceStore(SurveyActivity.this);
                        prefs.edit().putLastSurveyTimestamp(mSurveyId, System.currentTimeMillis()).commit();
                        finish();
                    }
                } else {
                    if (mSurveyElements.get(mCurrentPosition) instanceof Prompt
                            || mSurveyElements.get(mCurrentPosition) instanceof Message) {
                        // show toast if not answered
                        if (mSurveyElements.get(mCurrentPosition) instanceof Message
                                || ((Prompt) mSurveyElements.get(mCurrentPosition))
                                        .isPromptAnswered()) {
                            while (mCurrentPosition < mSurveyElements.size()) {
                                // increment position
                                mCurrentPosition++;

                                // if survey end reached, show submit screen
                                if (mCurrentPosition == mSurveyElements.size()) {
                                    mReachedEnd = true;
                                    showSubmitScreen();

                                } else {
                                    if (mSurveyElements.get(mCurrentPosition) instanceof Prompt) {
                                        // if new position is prompt, check
                                        // condition
                                        String condition = ((Prompt) mSurveyElements
                                                .get(mCurrentPosition)).getCondition();
                                        if (condition == null)
                                            condition = "";
                                        if (DataPointConditionEvaluator.evaluateCondition(
                                                condition, getPreviousResponses())) {
                                            // if true, show new prompt
                                            showPrompt(mCurrentPosition);
                                            break;
                                        } else {
                                            // if false, loop up and
                                            // increment
                                            ((Prompt) mSurveyElements.get(mCurrentPosition))
                                                    .setDisplayed(false);
                                        }
                                    } else if (mSurveyElements.get(mCurrentPosition) instanceof Message) {
                                        String condition = ((Message) mSurveyElements
                                                .get(mCurrentPosition)).getCondition();
                                        if (condition == null)
                                            condition = "";
                                        if (DataPointConditionEvaluator.evaluateCondition(
                                                condition, getPreviousResponses())) {
                                            // if true, show message
                                            showMessage(mCurrentPosition);
                                            break;
                                        }
                                    } else {
                                        // something is wrong!
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(
                                    SurveyActivity.this,
                                    ((Prompt) mSurveyElements.get(mCurrentPosition))
                                            .getUnansweredPromptText(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            } else if (id == R.id.skip_button) {
                if (mSurveyElements.get(mCurrentPosition) instanceof Prompt) {
                    ((Prompt) mSurveyElements.get(mCurrentPosition)).setSkipped(true);

                    while (mCurrentPosition < mSurveyElements.size()) {
                        // increment position
                        mCurrentPosition++;

                        // if survey end reached, show submit screen
                        if (mCurrentPosition == mSurveyElements.size()) {
                            mReachedEnd = true;
                            showSubmitScreen();

                        } else {
                            if (mSurveyElements.get(mCurrentPosition) instanceof Prompt) {
                                // if new position is prompt, check
                                // condition
                                String condition = ((Prompt) mSurveyElements
                                        .get(mCurrentPosition)).getCondition();
                                if (condition == null)
                                    condition = "";
                                if (DataPointConditionEvaluator.evaluateCondition(condition,
                                        getPreviousResponses())) {
                                    // if true, show new prompt
                                    showPrompt(mCurrentPosition);
                                    break;
                                } else {
                                    // if false, loop up and increment
                                    ((Prompt) mSurveyElements.get(mCurrentPosition))
                                            .setDisplayed(false);
                                }
                            } else if (mSurveyElements.get(mCurrentPosition) instanceof Message) {
                                String condition = ((Message) mSurveyElements.get(mCurrentPosition))
                                        .getCondition();
                                if (condition == null)
                                    condition = "";
                                if (DataPointConditionEvaluator.evaluateCondition(condition,
                                        getPreviousResponses())) {
                                    // if true, show message
                                    showMessage(mCurrentPosition);
                                    break;
                                }
                            } else {
                                // something is wrong!
                            }
                        }
                    }
                }
            } else if (id == R.id.prev_button) {
                if (mReachedEnd || mSurveyElements.get(mCurrentPosition) instanceof Prompt
                        || mSurveyElements.get(mCurrentPosition) instanceof Message) {
                    mReachedEnd = false;
                    while (mCurrentPosition > 0) {
                        // decrement position
                        mCurrentPosition--;

                        if (mSurveyElements.get(mCurrentPosition) instanceof Prompt) {
                            // if element is prompt, check condition
                            String condition = ((Prompt) mSurveyElements
                                    .get(mCurrentPosition)).getCondition();
                            if (condition == null)
                                condition = "";
                            if (DataPointConditionEvaluator.evaluateCondition(condition,
                                    getPreviousResponses())) {
                                // if true, show prompt
                                showPrompt(mCurrentPosition);
                                break;
                            } else {
                                // if false, decrement again and loop
                                ((Prompt) mSurveyElements.get(mCurrentPosition))
                                        .setDisplayed(false);
                            }
                        } else if (mSurveyElements.get(mCurrentPosition) instanceof Message) {
                            String condition = ((Message) mSurveyElements.get(mCurrentPosition))
                                    .getCondition();
                            if (condition == null)
                                condition = "";
                            if (DataPointConditionEvaluator.evaluateCondition(condition,
                                    getPreviousResponses())) {
                                // if true, show message
                                showMessage(mCurrentPosition);
                                break;
                            }
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Displayable.REQUEST_CODE
                && mSurveyElements.get(mCurrentPosition) instanceof Displayable) {
            ((Displayable) mSurveyElements.get(mCurrentPosition)).handleActivityResult(this,
                    resultCode, data);
        }
    }

    public void reloadCurrentPrompt() {
        showPrompt(mCurrentPosition);
    }

    private void showSubmitScreen() {
        handlePromptChangeLogging(null);

        mNextButton.setText(R.string.submit);
        mPrevButton.setText(R.string.previous);
        mPrevButton.setVisibility(View.VISIBLE);
        mSkipButton.setVisibility(View.INVISIBLE);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mPromptText.getWindowToken(), 0);

        mPromptText.setText(R.string.survey_complete);
        mProgressBar.setProgress(mProgressBar.getMax());

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ScrollView layout = (ScrollView) inflater.inflate(R.layout.submit, null);
        TextView submitText = (TextView) layout.findViewById(R.id.submit_text);
        // submitText.setText("Thank you for completing the survey!");
        submitText.setText(mSurveySubmitText);

        clearPromptFrame();
        mPromptFrame.addView(layout);
    }

    private void showElement(int index) {
        if (mSurveyElements.get(index) instanceof AbstractPrompt
                || mSurveyElements.get(index) instanceof AbstractPromptFragment) {
            showPrompt(index);
        } else if (mSurveyElements.get(index) instanceof Message) {
            showMessage(index);
        }
    }

    private void showMessage(int index) {
        if (mSurveyElements.get(index) instanceof Message) {
            Message message = (Message) mSurveyElements.get(index);
            handlePromptChangeLogging(message);

            mNextButton.setText(R.string.next);
            mPrevButton.setText(R.string.previous);
            mSkipButton.setVisibility(View.INVISIBLE);

            if (index == 0) {
                mPrevButton.setVisibility(View.INVISIBLE);
            } else {
                mPrevButton.setVisibility(View.VISIBLE);
            }

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mPromptText.getWindowToken(), 0);

            mPromptText.setText(R.string.survey_message_title);
            mProgressBar.setProgress(index * mProgressBar.getMax() / mSurveyElements.size());

            Fragment old = getSupportFragmentManager().findFragmentById(R.id.prompt_frame);
            if (old != mSurveyElements.get(index)) {
                clearPromptFrame();
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(R.id.prompt_frame, message).commit();
            }
        } else {
            Log.e(TAG, "trying to showMessage for element that is not a message!");
        }
    }

    private void showPrompt(int index) {

        if (mSurveyElements.get(index) instanceof AbstractPrompt
            || mSurveyElements.get(index) instanceof AbstractPromptFragment) {

            // If its a photo prompt we need to recycle the image
            if (mLastElement instanceof PhotoPrompt)
                PhotoPrompt.clearView(mPromptFrame);

            Prompt prompt = (Prompt) mSurveyElements.get(index);
            handlePromptChangeLogging(prompt);

            mNextButton.setText(R.string.next);
            mPrevButton.setText(R.string.previous);

            if (index == 0) {
                mPrevButton.setVisibility(View.INVISIBLE);
            } else {
                mPrevButton.setVisibility(View.VISIBLE);
            }

            // someone needs to check condition before showing prompt

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mPromptText.getWindowToken(), 0);

            prompt.setDisplayed(true);
            prompt.setSkipped(false);

            // TODO for now I'm casting, but maybe I should move getters/setters
            // to interface?
            // or just use a list of AbstractPrompt
            mPromptText.setText(Campaign.parseForImages(this, prompt.getPromptText(), mCampaignUrn));
            mProgressBar.setProgress(index * mProgressBar.getMax() / mSurveyElements.size());

            if (prompt.getSkippable().equals("true")) {
                mSkipButton.setVisibility(View.VISIBLE);
                mSkipButton.setText(prompt.getSkipLabel());
                mSkipButton.invalidate();
            } else {
                mSkipButton.setVisibility(View.INVISIBLE);
            }

            if (mSurveyElements.get(index) instanceof AbstractPromptFragment) {
                Fragment old = getSupportFragmentManager().findFragmentById(R.id.prompt_frame);
                if (old != mSurveyElements.get(index)) {
                    clearPromptFrame();
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.add(R.id.prompt_frame, ((AbstractPromptFragment) mSurveyElements.get(index))).commit();
                }
            } else {
                clearPromptFrame();
                ((Displayable)prompt).inflateView(this, mPromptFrame);
            }
        } else {
            Log.e(TAG, "trying to showPrompt for element that is not a prompt!");
        }
    }

    private void clearPromptFrame() {
        mPromptFrame.removeAllViews();
        Fragment old = getSupportFragmentManager().findFragmentById(R.id.prompt_frame);
        if (old != null)
            getSupportFragmentManager().beginTransaction().remove(old).commit();
    }

    private SurveyElement mLastElement;

    private void handlePromptChangeLogging(SurveyElement element) {
        // Don't log anything if its the same element
        if (element == mLastElement)
            return;

        if (mLastElement instanceof Prompt) {
            OhmageAnalytics.prompt((Prompt) mLastElement, Status.OFF);
        }
        if (element instanceof Prompt) {
            OhmageAnalytics.prompt((Prompt) element, Status.ON);
        }
        mLastElement = element;
    }

    /*
     * public void setResponse(int index, String id, String value) { // prompt
     * doesn't know it's own index... :( mResponses.set(index, new
     * PromptResponse(id, value)); }
     */

    private List<DataPoint> getPreviousResponses() {
        ArrayList<DataPoint> previousResponses = new ArrayList<DataPoint>();
        for (int i = 0; i < mCurrentPosition; i++) {
            if (mSurveyElements.get(i) instanceof Prompt) {
                Prompt prompt = ((Prompt) mSurveyElements.get(i));

                DataPoint dataPoint = new DataPoint(prompt.getPromptId());

                dataPoint.setPromptType(prompt);

                if (prompt.isSkipped()) {
                    dataPoint.setSkipped();
                } else if (!prompt.isDisplayed()) {
                    dataPoint.setNotDisplayed();
                } else {
                    if (PromptType.single_choice.equals(dataPoint.getPromptType())) {
                        dataPoint.setValue(prompt.getResponseObject());
                    } else if (PromptType.single_choice_custom.equals(dataPoint.getPromptType())) {
                        dataPoint.setValue(prompt.getResponseObject());

                        // The condition evaluator needs to know the index of
                        // hardcoded options
                        if (prompt instanceof SingleChoiceCustomPrompt) {
                            int idx = ((SingleChoiceCustomPrompt) prompt).getSelectedIndex();
                            if (idx != -1)
                                dataPoint.setIndex(idx);
                        }
                    } else if (PromptType.multi_choice.equals(dataPoint.getPromptType())) {
                        JSONArray jsonArray;
                        ArrayList<Integer> dataPointValue = new ArrayList<Integer>();
                        try {
                            jsonArray = (JSONArray) prompt.getResponseObject();
                            for (int j = 0; j < jsonArray.length(); j++) {
                                dataPointValue.add((Integer) jsonArray.get(j));
                            }
                        } catch (JSONException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        dataPoint.setValue(dataPointValue);
                    } else if (PromptType.multi_choice_custom.equals(dataPoint.getPromptType())) {
                        JSONArray jsonArray;
                        ArrayList<String> dataPointValue = new ArrayList<String>();
                        try {
                            jsonArray = (JSONArray) prompt.getResponseObject();
                            for (int j = 0; j < jsonArray.length(); j++) {
                                dataPointValue.add((String) jsonArray.get(j));
                            }
                        } catch (JSONException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        dataPoint.setValue(dataPointValue);

                        // The condition evaluator needs to know the index of
                        // hardcoded options
                        if (prompt instanceof MultiChoiceCustomPrompt) {
                            dataPoint.setIndexes(((MultiChoiceCustomPrompt) prompt)
                                    .getSelectedIndexes());
                        }
                    } else if (PromptType.number.equals(dataPoint.getPromptType())) {
                        dataPoint.setValue(prompt.getResponseObject());
                    } else if (PromptType.hours_before_now.equals(dataPoint.getPromptType())) {
                        dataPoint.setValue(prompt.getResponseObject());
                    }
                }

                previousResponses.add(dataPoint);
            }
        }
        return previousResponses;
    }

    private String storeResponse() {
        return storeResponse(this, mSurveyId, mLaunchTime, mCampaignUrn, mSurveyTitle,
                mSurveyElements);
    }

    public static String storeResponse(Context context, String surveyId, long launchTime,
            String campaignUrn, String surveyTitle, List<SurveyElement> surveyElements) {

        AccountHelper helper = new AccountHelper(context);
        String username = helper.getUsername();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar now = Calendar.getInstance();
        String date = dateFormat.format(now.getTime());
        long time = now.getTimeInMillis();
        String timezone = TimeZone.getDefault().getID();

        // get launch context from trigger glue
        JSONObject surveyLaunchContextJson = new JSONObject();
        try {
            surveyLaunchContextJson.put("launch_time", launchTime);
            surveyLaunchContextJson.put("launch_timezone", timezone);
            surveyLaunchContextJson.put("active_triggers",
                    TriggerFramework.getActiveTriggerInfo(context, campaignUrn, surveyTitle));
        } catch (JSONException e) {
            Log.e(TAG, "JSONException when trying to generate survey launch context json", e);
            throw new RuntimeException(e);
        }
        String surveyLaunchContext = surveyLaunchContextJson.toString();

        JSONArray responseJson = new JSONArray();
        JSONArray media = new JSONArray();
        JSONObject itemJson = null;

        for (int i = 0; i < surveyElements.size(); i++) {
            if (surveyElements.get(i) instanceof MediaPrompt || surveyElements.get(i) instanceof MediaPromptFragment) {
                Prompt m = (Prompt) surveyElements.get(i);
                if(m.isDisplayed() && !m.isSkipped())
                media.put(((Prompt)surveyElements.get(i)).getResponseObject());
            }

            if (surveyElements.get(i) instanceof Prompt) {
                    itemJson = new JSONObject();
                    try {
                    itemJson.put("prompt_id", ((Prompt) surveyElements.get(i)).getPromptId());
                        itemJson.put("value",
                            ((Prompt) surveyElements.get(i)).getResponseObject());
                    Object extras = ((Prompt) surveyElements.get(i)).getExtrasObject();
                        if (extras != null) {
                            // as of now we don't actually have "extras" we only
                            // have "custom_choices" for the custom types
                            // so this is currently only used by
                            // single_choice_custom and multi_choice_custom
                            itemJson.put("custom_choices", extras);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException when trying to generate response json", e);
                        throw new RuntimeException(e);
                    }
                    responseJson.put(itemJson);
                        }
                    }
        String response = responseJson.toString();

        // insert the response, which indirectly populates the prompt response
        // tables, etc.
        Response candidate = new Response();

        candidate.uuid = UUID.randomUUID().toString();
        candidate.campaignUrn = campaignUrn;
        candidate.username = username;
        candidate.date = date;
        candidate.time = time;
        candidate.timezone = timezone;
        candidate.surveyId = surveyId;
        candidate.surveyLaunchContext = surveyLaunchContext;
        candidate.response = response;
        candidate.media = media.toString();
        candidate.locationStatus = SurveyGeotagService.LOCATION_UNAVAILABLE;
        candidate.locationLatitude = -1;
        candidate.locationLongitude = -1;
        candidate.locationProvider = null;
        candidate.locationAccuracy = -1;
        candidate.locationTime = -1;
        candidate.status = Response.STATUS_WAITING_FOR_LOCATION;

        ContentResolver cr = context.getContentResolver();
        Uri responseUri = cr.insert(Responses.CONTENT_URI, candidate.toCV());

        Intent intent = new Intent(context, SurveyGeotagService.class);
        intent.setData(responseUri);
        WakefulService.sendWakefulWork(context, intent);

        // create an intent and broadcast it to any interested receivers
        Intent i = new Intent("org.ohmage.SURVEY_COMPLETE");

        i.putExtra(Responses.CAMPAIGN_URN, campaignUrn);
        i.putExtra(Responses.RESPONSE_USERNAME, username);
        i.putExtra(Responses.RESPONSE_DATE, date);
        i.putExtra(Responses.RESPONSE_TIME, time);
        i.putExtra(Responses.RESPONSE_TIMEZONE, timezone);
        i.putExtra(Responses.RESPONSE_LOCATION_STATUS, SurveyGeotagService.LOCATION_UNAVAILABLE);
        i.putExtra(Responses.RESPONSE_STATUS, Response.STATUS_WAITING_FOR_LOCATION);
        i.putExtra(Responses.SURVEY_ID, surveyId);
        i.putExtra(Responses.RESPONSE_SURVEY_LAUNCH_CONTEXT, surveyLaunchContext);
        i.putExtra(Responses.RESPONSE_JSON, response);

        context.sendBroadcast(i);

        return candidate.uuid;
    }

    @Override
    public void onPause() {
        super.onPause();
        Analytics.activity(this, Status.OFF);

        // If we are finishing
        if (isFinishing()) {
            // Stop listenting to the gps
            mLocManager.removeUpdates(this);

            // clean up the survey photo prompt
            if (!mSurveyFinished) {
                for (SurveyElement element : mSurveyElements)
                    if (element instanceof MediaPrompt)
                        ((MediaPrompt) element).delete();
                    else if(element instanceof MediaPromptFragment) {
                        ((MediaPromptFragment) element).delete();
                    }
            }
        }
    }

    @Override
    public void onBackPressed() {
        showDialog(DIALOG_CANCEL_ID);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = super.onCreateDialog(id);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        switch (id) {
            case DIALOG_CANCEL_ID:
                dialogBuilder.setTitle(R.string.discard_survey_title)
                        .setMessage(R.string.discard_survey_message).setCancelable(true)
                        .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        }).setNegativeButton(R.string.cancel, null);
                dialog = dialogBuilder.create();
                break;
            case DIALOG_INSTRUCTIONS_ID:
                View view = getLayoutInflater().inflate(R.layout.checkable_dialog_layout, null);
                TextView text = (TextView) view.findViewById(R.id.text);
                final CheckBox skip = (CheckBox) view.findViewById(R.id.skip);
                text.setText(mInstructions);
                dialogBuilder
                        .setTitle(R.string.survey_campaign_instructions_title)
                        .setView(view)
                        .setCancelable(true)
                        .setPositiveButton(R.string.continue_string,
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mCampaignPref.setShowInstructions(!skip.isChecked());
                                    }
                                });
                dialog = dialogBuilder.create();
                break;
        }
        return dialog;
    }
}
