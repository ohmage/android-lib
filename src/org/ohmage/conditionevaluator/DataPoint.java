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
package org.ohmage.conditionevaluator;

import org.ohmage.prompt.Prompt;
import org.ohmage.prompt.PromptFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataPoint {
    // List of data fields in a data point, add more if necessary for extended data points
    private String id;
    private Object value;  
    private PromptType promptType;
    
    // Need to know whether the DataPoint was "SKIPPED" or "NOT_DISPLAYED"
    // If so the value is meaningless
    private boolean isSkipped;
    private boolean isNotDisplayed;
    
    // Map to store all possible metadata
    Map<String, Object> metadata = new HashMap<String, Object>();

    // Possible prompt types
    public static enum PromptType {
        number, hours_before_now, multi_choice, single_choice, single_choice_custom, multi_choice_custom
    }
   
    // Nothing to do here!
    public DataPoint() {}
        
    public DataPoint(String _id) {
        id = _id;
    }
    
    // Large set of putters to create the json map
    public void setId(String _id) {
        id = _id;
    }
    
    public String getId() {
        return id;
    }
    
    public void setValue(Object _value) {
        value = _value;
    }

    public Object getValue() {
        return value;
    }
    
    public void setUnit(String _unit) {
        metadata.put("unit", _unit);
    }
    
    public String getUnit() {
       return (String) metadata.get("unit");
    }
    
    public void setDatetime(String _datetime) {
        metadata.put("datetime", _datetime);
    }
    
    public String getDatetime() {
        return (String) metadata.get("datetime");
    }
    
    public void setTz(String _tz) {
        metadata.put("tz", _tz);
    }
    
    public String getTz() {
        return (String) metadata.get("tz");
    }
    
    public void setLat(String _lat) {
        metadata.put("lat", _lat);
    }
    
    public String getLat() {
        return (String) metadata.get("lat");
    }
    
    public void setLon(String _lon) {
        metadata.put("lon", _lon);
    }
    
    public String getLon() {
        return (String) metadata.get("lon");
    }
    
    public void setIndex(int idx) {
        metadata.put("index", idx);
    }
    
    public Integer getIndex() {
        return (Integer) metadata.get("index");
    }
    
    public void setIndexes(List<Integer> idxs) {
        metadata.put("indexes", idxs);
    }
    
    public List<Integer> getIndexes() {
        return (List<Integer>) metadata.get("indexes");
    }

    public void setPromptType(Prompt prompt) {
        try {
            promptType = PromptType.valueOf(PromptFactory.promptType(prompt));
        } catch(IllegalArgumentException e) {
            // This will happen for prompts which use the basic comparator
        }
    }

    public PromptType getPromptType() {
        return promptType;
    }
    
    public void setSkipped() {
        isSkipped = true;
    }
    
    public boolean isSkipped() {
        return isSkipped;
    }
    
    public void setNotDisplayed() {
        isNotDisplayed = true;
    }
    
    public boolean isNotDisplayed() {
        return isNotDisplayed;
    }

    @Override
    public String toString() {
        return "type " + promptType.toString() + " id " + id + " value " + value;
    }
    
    /**
     * Pass in a DataPoint to compare.  Comparison is done based on
     * node IDs ONLY.
     * 
     * @param toCompare The DataPoint to compare to.
     * @return true/false
     */
    @Override
    public boolean equals(Object toCompare) {
        if (toCompare instanceof DataPoint)
            return getId().equals(((DataPoint) toCompare).getId());
        
        return false;
    }
}
