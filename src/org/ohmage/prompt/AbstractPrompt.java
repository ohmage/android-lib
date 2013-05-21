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
package org.ohmage.prompt;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.Utilities.KVLTriplet;

import java.util.ArrayList;


public abstract class AbstractPrompt implements Prompt, Displayable {

	public static final String SKIPPED_VALUE = "SKIPPED";
	public static final String NOT_DISPLAYED_VALUE = "NOT_DISPLAYED";

	// TODO change private to protected
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
	
	// should these be here?
	protected boolean mDisplayed;
	protected boolean mSkipped;
	
	public boolean isDisplayed() {
		return mDisplayed;
	}
	
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
	
	public Object getExtrasObject() {
		return getTypeSpecificExtrasObject();
	}
	
	@Override
	public String getResponseJson() {
		
		JSONObject responseJson = new JSONObject();
		try {
			responseJson.put("prompt_id", this.getPromptId());
			//responseJson.put("value", mSelectedKey);
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

	public AbstractPrompt() {
		setDisplayed(false);
		setSkipped(false);
	}
	
	/*public AbstractPrompt(	String id, String displayLabel,
					String promptText, String explanationText,
					String defaultValue, String condition, 
					String skippable, String skipLabel) {
		
		this.mId = id;
		this.mPromptText = promptText;
		this.mExplanationText = explanationText;
		this.mDefaultValue = defaultValue;
		this.mCondition = condition;
		this.mSkippable = skippable;
		this.mSkipLabel = skipLabel;
	}*/
	
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
	public void onHidden() {
		// By default there is nothing we need to do
	}

	@Override
	public void handleActivityResult(Context context, int resultCode, Intent data) {
		//  by default there is nothing we need to do
	}

	@Override
	public View inflateView(Context context, ViewGroup parent) {
		View view = getView(context);
		if(view != null)
			parent.addView(view);
		return view;
	}

	protected View getView(Context context) {
		return null;
	}
    }
