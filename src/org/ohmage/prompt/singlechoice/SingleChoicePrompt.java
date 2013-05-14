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

package org.ohmage.prompt.singlechoice;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;

import org.ohmage.OhmageMarkdown;
import org.ohmage.Utilities.KVLTriplet;
import org.ohmage.library.R;
import org.ohmage.prompt.AbstractPromptFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SingleChoicePrompt extends AbstractPromptFragment {

    private static final String TAG = "SingleChoicePrompt";

    private List<KVLTriplet> mChoices;
    private int mSelectedIndex;

    public List<KVLTriplet> getChoices() {
        return mChoices;
    }

    public SingleChoicePrompt() {
        super();
        mSelectedIndex = -1;
    }

    public void setChoices(List<KVLTriplet> choices) {
        mChoices = choices;
    }

    /**
     * Returns true if the selected index falls within the range of possible
     * indices.
     */
    @Override
    public boolean isPromptAnswered() {
        return (mSelectedIndex >= 0 && mSelectedIndex < mChoices.size());
    }

    @Override
    protected Object getTypeSpecificResponseObject() {
        if (mSelectedIndex >= 0 && mSelectedIndex < mChoices.size()) {
            return Integer.decode(mChoices.get(mSelectedIndex).key);
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
        return ("Please select an item.");
    }

    @Override
    protected void clearTypeSpecificResponseData() {
        mSelectedIndex = -1;
    }

    @Override
    protected Object getTypeSpecificExtrasObject() {
        return null;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ListView listView = (ListView) inflater.inflate(R.layout.prompt_single_choice, container,
                false);

        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        String[] from = new String[] {
            "value"
        };
        int[] to = new int[] {
            android.R.id.text1
        };

        List<HashMap<String, CharSequence>> data = new ArrayList<HashMap<String, CharSequence>>();
        for (int i = 0; i < mChoices.size(); i++) {
            HashMap<String, CharSequence> map = new HashMap<String, CharSequence>();
            map.put("key", mChoices.get(i).key);
            map.put("value", OhmageMarkdown.parse(mChoices.get(i).label));
            data.add(map);
        }

        SimpleAdapter adapter = new SimpleAdapter(getActivity(), data,
                R.layout.single_choice_list_item, from, to);

        adapter.setViewBinder(new ViewBinder() {

            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                ((CheckedTextView) view).setText((SpannableStringBuilder) data);
                return true;
            }
        });

        listView.setAdapter(adapter);

        if (mSelectedIndex >= 0 && mSelectedIndex < listView.getCount()) {
            listView.setItemChecked(mSelectedIndex, true);
        }

        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSelectedIndex = position;
            }
        });

        return listView;
    }

}
