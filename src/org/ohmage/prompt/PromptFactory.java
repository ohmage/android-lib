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

import org.ohmage.prompt.hoursbeforenow.HoursBeforeNowPrompt;
import org.ohmage.prompt.media.AudioPrompt;
import org.ohmage.prompt.media.PhotoPrompt;
import org.ohmage.prompt.media.VideoPrompt;
import org.ohmage.prompt.multichoice.MultiChoicePrompt;
import org.ohmage.prompt.multichoicecustom.MultiChoiceCustomPrompt;
import org.ohmage.prompt.number.NumberPrompt;
import org.ohmage.prompt.remoteactivity.RemoteActivityPrompt;
import org.ohmage.prompt.singlechoice.SingleChoicePrompt;
import org.ohmage.prompt.singlechoicecustom.SingleChoiceCustomPrompt;
import org.ohmage.prompt.text.TextPrompt;
import org.ohmage.prompt.timestamp.TimestampPrompt;

public class PromptFactory {

    public static final String SINGLE_CHOICE = "single_choice";
    public static final String SINGLE_CHOICE_CUSTOM = "single_choice_custom";
    public static final String MULTI_CHOICE = "multi_choice";
    public static final String MULTI_CHOICE_CUSTOM = "multi_choice_custom";
    public static final String NUMBER = "number";
    public static final String HOURS_BEFORE_NOW = "hours_before_now";
    public static final String TIMESTAMP = "timestamp";
    public static final String TEXT = "text";
    public static final String PHOTO = "photo";
    public static final String REMOTE_ACTIVITY = "remote_activity";
    public static final String VIDEO = "video";
    public static final String AUDIO = "audio";

    private PromptFactory() {
    };

    public static Prompt createPrompt(String promptType) {

        if (promptType.equals(SINGLE_CHOICE)) {
            return new SingleChoicePrompt();
        } else if (promptType.equals(SINGLE_CHOICE_CUSTOM)) {
            return new SingleChoiceCustomPrompt();
        } else if (promptType.equals(MULTI_CHOICE)) {
            return new MultiChoicePrompt();
        } else if (promptType.equals(MULTI_CHOICE_CUSTOM)) {
            return new MultiChoiceCustomPrompt();
        } else if (promptType.equals(NUMBER)) {
            return new NumberPrompt();
        } else if (promptType.equals(HOURS_BEFORE_NOW)) {
            return new HoursBeforeNowPrompt();
        } else if (promptType.equals(TIMESTAMP)) {
            return new TimestampPrompt();
        } else if (promptType.equals(TEXT)) {
            return new TextPrompt();
        } else if (promptType.equals(PHOTO)) {
            return new PhotoPrompt();
        } else if (promptType.equals(VIDEO)) {
            return new VideoPrompt();
        } else if (promptType.equals(AUDIO)) {
            return new AudioPrompt();
        } else if (promptType.equals(REMOTE_ACTIVITY)) {
            return new RemoteActivityPrompt();
        }

        throw new IllegalArgumentException("Unsupported prompt type: " + promptType);
    }

    public static String promptType(Prompt prompt) {
        if (prompt instanceof SingleChoicePrompt) {
            return SINGLE_CHOICE;
        } else if (prompt instanceof SingleChoiceCustomPrompt) {
            return SINGLE_CHOICE_CUSTOM;
        } else if (prompt instanceof MultiChoicePrompt) {
            return MULTI_CHOICE;
        } else if (prompt instanceof MultiChoiceCustomPrompt) {
            return MULTI_CHOICE_CUSTOM;
        } else if (prompt instanceof NumberPrompt) {
            return NUMBER;
        } else if (prompt instanceof HoursBeforeNowPrompt) {
            return HOURS_BEFORE_NOW;
        } else if (prompt instanceof TimestampPrompt) {
            return TIMESTAMP;
        } else if (prompt instanceof TextPrompt) {
            return TEXT;
        } else if (prompt instanceof PhotoPrompt) {
            return PHOTO;
        } else if (prompt instanceof VideoPrompt) {
            return VIDEO;
        } else if (prompt instanceof AudioPrompt) {
            return AUDIO;
        } else if (prompt instanceof RemoteActivityPrompt) {
            return REMOTE_ACTIVITY;
        }

        throw new IllegalArgumentException("Unsupported prompt");
    }
}
