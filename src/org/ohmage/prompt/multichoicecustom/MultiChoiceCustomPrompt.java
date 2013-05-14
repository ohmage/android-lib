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

package org.ohmage.prompt.multichoicecustom;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
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

import org.json.JSONArray;
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

public class MultiChoiceCustomPrompt extends AbstractPromptFragment {

    private static final String TAG = "MultiChoiceCustomPrompt";

    private List<KVLTriplet> mChoices;
    private final List<KVLTriplet> mCustomChoices;
    private ArrayList<Integer> mSelectedIndexes;

    public MultiChoiceCustomPrompt() {
        super();
        mSelectedIndexes = new ArrayList<Integer>();
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

    @Override
    protected void clearTypeSpecificResponseData() {
        if (mSelectedIndexes == null) {
            mSelectedIndexes = new ArrayList<Integer>();
        } else {
            mSelectedIndexes.clear();
        }
    }

    /**
     * Always returns true as any selection of the items is considered valid.
     */
    @Override
    public boolean isPromptAnswered() {
        return true;
    }

    @Override
    protected Object getTypeSpecificResponseObject() {
        JSONArray jsonArray = new JSONArray();
        for (int index : mSelectedIndexes) {
            if (index >= 0 && index < mChoices.size()) {
                jsonArray.put(mChoices.get(index).label);
            } else if (index < mChoices.size() + mCustomChoices.size()) {
                jsonArray.put(mCustomChoices.get(index - mChoices.size()).label);
            }
        }
        return jsonArray;
    }

    /**
     * The text to be displayed to the user if the prompt is considered
     * unanswered.
     */
    @Override
    public String getUnansweredPromptText() {
        return ("Please choose at least one of the items in the list.");
    }

    @Override
    protected Object getTypeSpecificExtrasObject() {
        return null;
    }

    private View mFooterView;
    private ListView mListView;
    private boolean mIsAddingNewItem;
    private String mEnteredText;
    private int mLastIndex;
    private int mLastTop;

    private ArrayList<HashMap<String, CharSequence>> mData;

    private SimpleAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mCustomChoices.clear();
        MultiChoiceCustomDbAdapter dbAdapter = new MultiChoiceCustomDbAdapter(getActivity());
        String surveyId = ((SurveyActivity) getActivity()).getSurveyId();
        AccountHelper prefs = new AccountHelper(getActivity());
        String campaignUrn = ((SurveyActivity) getActivity()).getCampaignUrn();
        String username = prefs.getUsername();
        if (dbAdapter.open()) {
            Cursor c = dbAdapter.getCustomChoices(username, campaignUrn, surveyId,
                    MultiChoiceCustomPrompt.this.getPromptId());
            c.moveToFirst();
            for (int i = 0; i < c.getCount(); i++) {
                int key = c.getInt(c.getColumnIndex(MultiChoiceCustomDbAdapter.KEY_CHOICE_ID));
                String label = c.getString(c
                        .getColumnIndex(MultiChoiceCustomDbAdapter.KEY_CHOICE_VALUE));
                mCustomChoices.add(new KVLTriplet(String.valueOf(key), null, label));
                c.moveToNext();
            }
            c.close();
            dbAdapter.close();
        }

        mListView = new CustomChoiceListView(getActivity());
        mListView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));
        mListView.setTextFilterEnabled(false);

        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mListView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        mFooterView = inflater.inflate(R.layout.custom_choice_footer, null);

        EditText mEditText = (EditText) mFooterView.findViewById(R.id.new_choice_edit);
        ImageButton mButton = (ImageButton) mFooterView.findViewById(R.id.ok_button);
        ImageButton mCancelButton = (ImageButton) mFooterView.findViewById(R.id.cancel_button);

        showAddItemControls(getActivity(), mIsAddingNewItem);

        mButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mEnteredText = mEnteredText.trim();
                if (!TextUtils.isEmpty(mEnteredText)) {
                    MultiChoiceCustomDbAdapter dbAdapter = new MultiChoiceCustomDbAdapter(
                            getActivity());
                    String surveyId = ((SurveyActivity) getActivity()).getSurveyId();
                    AccountHelper prefs = new AccountHelper(getActivity());
                    String campaignUrn = ((SurveyActivity) getActivity()).getCampaignUrn();
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
                                surveyId, MultiChoiceCustomPrompt.this.getPromptId());
                        dbAdapter.close();

                        KVLTriplet choice = new KVLTriplet(String.valueOf(choiceId), null,
                                mEnteredText);
                        mCustomChoices.add(choice);

                        mSelectedIndexes.add(mListView.getCount() - 1);

                        HashMap<String, CharSequence> map = new HashMap<String, CharSequence>();
                        map.put("key", String.valueOf(choiceId));
                        map.put("value", new SpannableStringBuilder(mEnteredText));
                        mData.add(map);
                        mAdapter.notifyDataSetChanged();
                    }

                    showAddItemControls(getActivity(), false);
                } else {
                    Toast.makeText(getActivity(), "Please enter some text", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });

        mCancelButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showAddItemControls(getActivity(), false);
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

        mData = new ArrayList<HashMap<String, CharSequence>>();
        for (int i = 0; i < mChoices.size(); i++) {
            HashMap<String, CharSequence> map = new HashMap<String, CharSequence>();
            map.put("key", mChoices.get(i).key);
            map.put("value", OhmageMarkdown.parse(mChoices.get(i).label));
            mData.add(map);
        }
        for (int i = 0; i < mCustomChoices.size(); i++) {
            HashMap<String, CharSequence> map = new HashMap<String, CharSequence>();
            map.put("key", mCustomChoices.get(i).key);
            map.put("value", OhmageMarkdown.parse(mCustomChoices.get(i).label));
            mData.add(map);
        }

        mAdapter = new SimpleAdapter(getActivity(), mData, R.layout.multi_choice_list_item, from,
                to);

        mAdapter.setViewBinder(new ViewBinder() {

            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                ((CheckedTextView) view).setText((SpannableStringBuilder) data);
                return true;
            }
        });

        mListView.setAdapter(mAdapter);

        if (mSelectedIndexes.size() > 0) {
            for (int index : mSelectedIndexes) {
                if (index >= 0 && index < mChoices.size() + mCustomChoices.size())
                    mListView.setItemChecked(index, true);
            }
        }

        mListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (position == mListView.getCount() - 1) {
                    showAddItemControls(getActivity(), true);
                    mLastIndex = mListView.getLastVisiblePosition();
                    View v = mListView.getChildAt(mLastIndex);
                    mLastTop = (v == null) ? 0 : v.getTop();
                    mListView.setSelectionFromTop(mLastIndex, mLastTop);
                } else {
                    if (((ListView) parent).isItemChecked(position)) {
                        mSelectedIndexes.add(Integer.valueOf(position));
                    } else {
                        mSelectedIndexes.remove(Integer.valueOf(position));
                    }
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
            if (defaultValue != null) {
                String[] values = defaultValue.split(",");
                for (int i = 0; i < values.length; i++)
                    mSelectedIndexes.add(Integer.valueOf(values[i]));
            }
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

    public List<Integer> getSelectedIndexes() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int index : mSelectedIndexes) {
            if (index >= 0 && index < mChoices.size()) {
                list.add(index);
            }
        }
        return list;
    }
}
