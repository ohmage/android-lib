
package org.ohmage.prompt;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragment;

import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.Utilities.KVLTriplet;
import org.ohmage.activity.SurveyActivity;

import java.util.ArrayList;

/**
 * Fragment prompt which can be used to show the prompt view in the
 * {@link SurveyActivity}
 * 
 * @author cketcham
 */
public abstract class AbstractPromptFragment extends SherlockFragment implements Prompt {

    public static final String SKIPPED_VALUE = "SKIPPED";
    public static final String NOT_DISPLAYED_VALUE = "NOT_DISPLAYED";

    protected String mId;
    protected String mPromptType;
    protected String mDisplayLabel;
    protected String mPromptText;
    protected String mExplanationText;
    protected String mDefaultValue;
    protected String mCondition;
    protected String mSkippable;
    protected String mSkipLabel;
    protected ArrayList<KVLTriplet> mProperties;

    protected boolean mDisplayed;
    protected boolean mSkipped;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public boolean isDisplayed() {
        return mDisplayed;
    }

    @Override
    public boolean isSkipped() {
        return mSkipped;
    }

    @Override
    public void setDisplayed(boolean displayed) {
        this.mDisplayed = displayed;
        // should we clear or not clear?!
        if (!displayed) {
            clearTypeSpecificResponseData();
        }
    }

    @Override
    public void setSkipped(boolean skipped) {
        this.mSkipped = skipped;
        if (skipped) {
            clearTypeSpecificResponseData();
        }
    }

    @Override
    public Object getResponseObject() {
        if (!isDisplayed()) {
            return NOT_DISPLAYED_VALUE;
        } else if (isSkipped()) {
            return SKIPPED_VALUE;
        } else {
            return getTypeSpecificResponseObject();
        }
    }

    @Override
    public Object getExtrasObject() {
        return getTypeSpecificExtrasObject();
    }

    @Override
    public String getResponseJson() {

        JSONObject responseJson = new JSONObject();
        try {
            responseJson.put("prompt_id", this.getId());
            // responseJson.put("value", mSelectedKey);
            responseJson.put("value", getResponseObject());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return responseJson.toString();
    }

    protected abstract Object getTypeSpecificResponseObject();

    protected abstract Object getTypeSpecificExtrasObject();

    protected abstract void clearTypeSpecificResponseData();

    @Override
    public String getPromptId() {
        return mId;
    }

    public String getDisplayLabel() {
        return mDisplayLabel;
    }

    @Override
    public String getPromptText() {
        return mPromptText;
    }

    public String getExplanationText() {
        return mExplanationText;
    }

    public String getDefaultValue() {
        return mDefaultValue;
    }

    @Override
    public String getCondition() {
        return mCondition;
    }

    @Override
    public String getSkippable() {
        return mSkippable;
    }

    @Override
    public String getSkipLabel() {
        return mSkipLabel;
    }

    public ArrayList<KVLTriplet> getProperties() {
        return mProperties;
    }

    public void setPromptId(String id) {
        this.mId = id;
    }

    public void setDisplayLabel(String displayLabel) {
        this.mDisplayLabel = displayLabel;
    }

    public void setPromptText(String promptText) {
        this.mPromptText = promptText;
    }

    public void setExplanationText(String explanationText) {
        this.mExplanationText = explanationText;
    }

    public void setDefaultValue(String defaultValue) {
        this.mDefaultValue = defaultValue;
    }

    public void setCondition(String condition) {
        this.mCondition = condition;
    }

    public void setSkippable(String skippable) {
        this.mSkippable = skippable;
    }

    public void setSkipLabel(String skipLabel) {
        this.mSkipLabel = skipLabel;
    }

    public void setProperties(ArrayList<KVLTriplet> properties) {
        this.mProperties = properties;
    }

    @Override
    public String getUnansweredPromptText() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isPromptAnswered() {
        // TODO Auto-generated method stub
        return false;
    }
}
