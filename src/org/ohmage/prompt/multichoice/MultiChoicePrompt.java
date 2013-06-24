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

package org.ohmage.prompt.multichoice;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;

import org.json.JSONArray;
import org.ohmage.Utilities.KVLTriplet;
import org.ohmage.db.Models.Campaign;
import org.ohmage.library.R;
import org.ohmage.prompt.AbstractPromptFragment;
import org.ohmage.prompt.ChoicePrompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MultiChoicePrompt extends AbstractPromptFragment implements ChoicePrompt {

    private static final String TAG = "MultiChoicePrompt";

    private List<KVLTriplet> mChoices;
    private ArrayList<Integer> mSelectedIndexes;

    public MultiChoicePrompt() {
        super();
        mSelectedIndexes = new ArrayList<Integer>();
    }

    @Override
    public void setChoices(List<KVLTriplet> choices) {
        mChoices = choices;
    }

    @Override
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
     * Always returns true as an number of selected items is valid.
     */
    @Override
    public boolean isPromptAnswered() {
        return true;
    }

    @Override
    protected Object getTypeSpecificResponseObject() {
        JSONArray jsonArray = new JSONArray();
        for (int index : mSelectedIndexes) {
            if (index >= 0 && index < mChoices.size())
                jsonArray.put(Integer.decode(mChoices.get(index).key));
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ListView listView = (ListView) inflater.inflate(R.layout.prompt_multi_choice, container,
                false);

        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

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
            map.put("value", Campaign.parseForImages(inflater.getContext(), mChoices.get(i).label, getCampaignUrn()));
            data.add(map);
        }

        SimpleAdapter adapter = new SimpleAdapter(getActivity(), data,
                R.layout.multi_choice_list_item, from, to);

        adapter.setViewBinder(new ViewBinder() {

            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                ((CheckedTextView) view).setText((CharSequence) data);
                return true;
            }
        });

        listView.setAdapter(adapter);

        if (mSelectedIndexes.size() > 0) {
            for (int index : mSelectedIndexes) {
                if (index >= 0 && index < mChoices.size())
                    listView.setItemChecked(index, true);
            }
        }

        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (((ListView) parent).isItemChecked(position)) {
                    mSelectedIndexes.add(Integer.valueOf(position));
                } else {
                    mSelectedIndexes.remove(Integer.valueOf(position));
                }
            }
        });

        return listView;
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
}
