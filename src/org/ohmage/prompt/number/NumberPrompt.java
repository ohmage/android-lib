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
package org.ohmage.prompt.number;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import org.ohmage.NumberPicker;
import org.ohmage.NumberPicker.OnChangedListener;
import org.ohmage.library.R;
import org.ohmage.prompt.AbstractPrompt;

import java.math.BigDecimal;

public class NumberPrompt extends AbstractPrompt {

	private BigDecimal mMinimum;
	private BigDecimal mMaximum;
	private BigDecimal mValue;
	protected NumberPicker mNumberPicker;
	private boolean mWholeNumnbers;

	public NumberPrompt() {
		super();
	}

	public void setMinimum(BigDecimal value) {
		mMinimum = value;
	}

	public void setMaximum(BigDecimal value) {
		mMaximum = value;
	}

	public BigDecimal getMinimum(){
		return mMinimum;
	}

	public BigDecimal getMaximum(){
		return mMaximum;
	}

	public BigDecimal getValue(){
		return mValue;
	}

	public void setWholeNumbers(boolean wholeNumbers) {
		mWholeNumnbers = wholeNumbers;
	}

	@Override
	protected void clearTypeSpecificResponseData() {
		if (mDefaultValue != null && ! mDefaultValue.equals("")) {
			mValue = new BigDecimal(mDefaultValue);
		} else {
			mValue = mMinimum;
		}

	}

	/**
	 * Returns true if the current value falls between the minimum and the
	 * maximum.
	 */
	@Override
	public boolean isPromptAnswered() {
		// If there is a number picker, see if the value is valid
		// And check the value is between min and max
		return (mNumberPicker != null && mNumberPicker.forceValidateInput() || mNumberPicker == null)
				&& ((mValue.compareTo(mMinimum) >= 0) && (mValue.compareTo(mMaximum) <= 0));
	}

	@Override
	protected Object getTypeSpecificResponseObject() {
		return mValue;
	}

	/**
	 * The text to be displayed to the user if the prompt is considered
	 * unanswered.
	 */
	@Override
	public String getUnansweredPromptText() {
		return("Please choose a value between " + mMinimum + " and " + mMaximum + ".");
	}

	@Override
	protected Object getTypeSpecificExtrasObject() {
		return null;
	}

	@Override
	public View getView(Context context) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(getLayoutResource(), null);

		mNumberPicker = (NumberPicker) layout.findViewById(R.id.number_picker);
		mNumberPicker.setWholeNumbers(mWholeNumnbers);

		mNumberPicker.setRange(mMinimum, mMaximum);
		mNumberPicker.setCurrent(mValue);

		mNumberPicker.setOnChangeListener(new OnChangedListener() {

			@Override
			public void onChanged(NumberPicker picker, BigDecimal oldVal, BigDecimal newVal) {
				mValue = newVal;				
			}
		});

		return layout;
	}

	protected int getLayoutResource() {
		return R.layout.prompt_number;
	}
}
