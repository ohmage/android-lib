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

package org.ohmage.prompt.singlechoicecustom;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TextView;
import android.widget.Toast;

import org.ohmage.AccountHelper;
import org.ohmage.OhmageMarkdown;
import org.ohmage.Utilities.KVLTriplet;
import org.ohmage.activity.SurveyActivity;
import org.ohmage.library.R;
import org.ohmage.prompt.AbstractPromptFragment;
import org.ohmage.prompt.CustomChoiceListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SingleChoiceCustomPrompt extends AbstractPromptFragment {

    private static final String TAG = "SingleChoiceCustomPrompt";

    private List<KVLTriplet> mChoices;
    private final List<KVLTriplet> mCustomChoices;
    private int mSelectedIndex;

    public SingleChoiceCustomPrompt() {
        super();
        mSelectedIndex = -1;
        mCustomChoices = new ArrayList<KVLTriplet>();
        mEnteredText = "";
        mIsAddingNewItem = false;
    }

    public void setChoices(List<KVLTriplet> choices) {
        if (choices != null) {
            mChoices = choices;
        } else {
            mChoices = new ArrayList<KVLTriplet>();
        }
    }

    public List<KVLTriplet> getChoices() {
        return mChoices;
    }

    /**
     * Returns true if the selected index falls within the range of possible
     * indices within either the preset list of items or the new list of items.
     */
    @Override
    public boolean isPromptAnswered() {
        return ((mSelectedIndex >= 0 && mSelectedIndex < mChoices.size()) || (mSelectedIndex >= 0 && mSelectedIndex < mChoices
                .size() + mCustomChoices.size()));
    }

    @Override
    protected Object getTypeSpecificResponseObject() {

        if (mSelectedIndex >= 0 && mSelectedIndex < mChoices.size()) {
            return mChoices.get(mSelectedIndex).label;
        } else if (mSelectedIndex >= 0 && mSelectedIndex < mChoices.size() + mCustomChoices.size()) {
            return mCustomChoices.get(mSelectedIndex - mChoices.size()).label;
        } else {
            return null;
        }
    }

    /**
     * The text to be displayed to the user if the prompt is considered
     * unanswered.
     */
    @Override
    public String getUnansweredPromptText() {
        return ("Please select an item or add your own.");
    }

    @Override
    protected void clearTypeSpecificResponseData() {
        mSelectedIndex = -1;
    }

    @Override
    protected Object getTypeSpecificExtrasObject() {
        return null;
    }

    private View mFooterView;
    private CustomChoiceListView mListView;
    private boolean mIsAddingNewItem;
    private String mEnteredText;
    private int mLastIndex;
    private int mLastTop;

    private SimpleAdapter mAdapter;

    private ArrayList<HashMap<String, CharSequence>> mdata;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final FragmentActivity context = getActivity();

        mCustomChoices.clear();
        SingleChoiceCustomDbAdapter dbAdapter = new SingleChoiceCustomDbAdapter(context);
        String surveyId = ((SurveyActivity) context).getSurveyId();
        AccountHelper prefs = new AccountHelper(context);
        String campaignUrn = ((SurveyActivity) context).getCampaignUrn();
        String username = prefs.getUsername();
        if (dbAdapter.open()) {
            Cursor c = dbAdapter.getCustomChoices(username, campaignUrn, surveyId,
                    SingleChoiceCustomPrompt.this.getPromptId());
            c.moveToFirst();
            for (int i = 0; i < c.getCount(); i++) {
                int key = c.getInt(c.getColumnIndex(SingleChoiceCustomDbAdapter.KEY_CHOICE_ID));
                String label = c.getString(c
                        .getColumnIndex(SingleChoiceCustomDbAdapter.KEY_CHOICE_VALUE));
                mCustomChoices.add(new KVLTriplet(String.valueOf(key), null, label));
                c.moveToNext();
            }
            c.close();
            dbAdapter.close();
        }

        mListView = new CustomChoiceListView(context);
        mListView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));
        mListView.setTextFilterEnabled(false);

        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        mFooterView = inflater.inflate(R.layout.custom_choice_footer, null);

        EditText mEditText = (EditText) mFooterView.findViewById(R.id.new_choice_edit);
        ImageButton mButton = (ImageButton) mFooterView.findViewById(R.id.ok_button);
        ImageButton mCancelButton = (ImageButton) mFooterView.findViewById(R.id.cancel_button);

        showAddItemControls(context, mIsAddingNewItem);

        mButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mEnteredText = mEnteredText.trim();
                if (!TextUtils.isEmpty(mEnteredText)) {
                    SingleChoiceCustomDbAdapter dbAdapter = new SingleChoiceCustomDbAdapter(context);
                    String surveyId = ((SurveyActivity) context).getSurveyId();
                    AccountHelper prefs = new AccountHelper(context);
                    String campaignUrn = ((SurveyActivity) context).getCampaignUrn();
                    String username = prefs.getUsername();

                    boolean duplicate = false;
                    int choiceId = 100;
                    ArrayList<String> keys = new ArrayList<String>();
                    for (KVLTriplet choice : mChoices) {
                        keys.add(choice.key.trim());
                        if (mEnteredText.toLowerCase().equals(choice.label.toLowerCase()))
                            duplicate = true;
                    }
                    for (KVLTriplet choice : mCustomChoices) {
                        keys.add(choice.key.trim());
                        if (mEnteredText.toLowerCase().equals(choice.label.toLowerCase()))
                            duplicate = true;
                    }
                    while (keys.contains(String.valueOf(choiceId))) {
                        choiceId++;
                    }

                    if (duplicate) {
                        Toast.makeText(v.getContext(),
                                v.getContext().getString(R.string.prompt_custom_choice_duplicate),
                                Toast.LENGTH_SHORT).show();
                    } else if (!dbAdapter.open()) {
                        Toast.makeText(
                                v.getContext(),
                                v.getContext().getString(
                                        R.string.prompt_custom_choice_db_open_error),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        dbAdapter.addCustomChoice(choiceId, mEnteredText, username, campaignUrn,
                                surveyId, SingleChoiceCustomPrompt.this.getPromptId());
                        dbAdapter.close();

                        KVLTriplet choice = new KVLTriplet(String.valueOf(choiceId), null,
                                mEnteredText);
                        mCustomChoices.add(choice);

                        mSelectedIndex = mListView.getCount() - 1;
                        mListView.setSelection(mSelectedIndex);

                        HashMap<String, CharSequence> map = new HashMap<String, CharSequence>();
                        map.put("key", choice.key);
                        map.put("value", new SpannableStringBuilder(choice.label));
                        mdata.add(map);
                        mAdapter.notifyDataSetChanged();
                    }

                    showAddItemControls(context, false);
                } else {
                    Toast.makeText(context, "Please enter some text", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mCancelButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showAddItemControls(context, false);
            }
        });

        mEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                mEnteredText = s.toString();
            }
        });

        mListView.addFooterView(mFooterView);

        String[] from = new String[] {
            "value"
        };
        int[] to = new int[] {
            android.R.id.text1
        };

        mdata = new ArrayList<HashMap<String, CharSequence>>();
        for (int i = 0; i < mChoices.size(); i++) {
            HashMap<String, CharSequence> map = new HashMap<String, CharSequence>();
            map.put("key", mChoices.get(i).key);
            map.put("value", OhmageMarkdown.parse(mChoices.get(i).label));
            mdata.add(map);
        }
        for (int i = 0; i < mCustomChoices.size(); i++) {
            HashMap<String, CharSequence> map = new HashMap<String, CharSequence>();
            map.put("key", mCustomChoices.get(i).key);
            map.put("value", OhmageMarkdown.parse(mCustomChoices.get(i).label));
            mdata.add(map);
        }

        mAdapter = new SimpleAdapter(context, mdata, R.layout.single_choice_list_item, from, to);

        mAdapter.setViewBinder(new ViewBinder() {

            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                ((CheckedTextView) view).setText((SpannableStringBuilder) data);
                return true;
            }
        });

        mListView.setAdapter(mAdapter);

        if (mSelectedIndex >= 0 && mSelectedIndex < mChoices.size() + mCustomChoices.size()) {
            mListView.setItemChecked(mSelectedIndex, true);
        }

        mListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (position == mListView.getCount() - 1) {
                    showAddItemControls(context, true);
                    mSelectedIndex = -1;
                    mLastIndex = mListView.getLastVisiblePosition();
                    View v = mListView.getChildAt(mLastIndex);
                    mLastTop = (v == null) ? 0 : v.getTop();
                    mListView.setSelectionFromTop(mLastIndex, mLastTop);
                } else {
                    mSelectedIndex = position;
                }
            }
        });

        mListView.setSelectionFromTop(mLastIndex, mLastTop);

        return mListView;
    }

    private void showAddItemControls(Context context, boolean show) {
        ImageView imageView = (ImageView) mFooterView.findViewById(R.id.image);
        TextView textView = (TextView) mFooterView.findViewById(R.id.text);
        EditText editText = (EditText) mFooterView.findViewById(R.id.new_choice_edit);
        ImageButton mButton = (ImageButton) mFooterView.findViewById(R.id.ok_button);
        ImageButton mCancelButton = (ImageButton) mFooterView.findViewById(R.id.cancel_button);

        InputMethodManager imm = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        if (show) {
            editText.setText(mEnteredText);
            mIsAddingNewItem = true;
            imageView.setVisibility(View.GONE);
            textView.setVisibility(View.GONE);
            editText.setVisibility(View.VISIBLE);
            mButton.setVisibility(View.VISIBLE);
            mCancelButton.setVisibility(View.VISIBLE);
            editText.requestFocus();
            imm.showSoftInput(editText, 0);
        } else {
            mEnteredText = "";
            mIsAddingNewItem = false;
            imageView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
            editText.setVisibility(View.GONE);
            mButton.setVisibility(View.GONE);
            mCancelButton.setVisibility(View.GONE);
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
    }

    @Override
    public void setDefaultValue(String defaultValue) {
        this.mDefaultValue = defaultValue;
        try {
            mSelectedIndex = Integer.valueOf(defaultValue);
        } catch (NumberFormatException e) {
            // No number...
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (isRemoving()) {
            mEnteredText = "";
            mIsAddingNewItem = false;
        }
    }

    public int getSelectedIndex() {
        if (mSelectedIndex >= 0 && mSelectedIndex < mChoices.size()) {
            return mSelectedIndex;
        }
        return -1;
    }
}
