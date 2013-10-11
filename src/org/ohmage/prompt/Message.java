
package org.ohmage.prompt;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import org.ohmage.library.R;

public class Message extends SherlockFragment implements SurveyElement {

    private static final String KEY_MESSAGE_TEXT = "key_message_text";
    private static final String KEY_CONDITION = "key_condition";

    public static Message getInstance(String messageText, String condition) {
        Message fragment = new Message();
        Bundle args = new Bundle();
        args.putString(KEY_MESSAGE_TEXT, messageText);
        args.putString(KEY_CONDITION, condition);
        fragment.setArguments(args);
        return fragment;
    }

    public String getMessageText() {
        return getArguments().getString(KEY_MESSAGE_TEXT);
    }

    public void setMessageText(String messageText) {
        getArguments().putString(KEY_MESSAGE_TEXT, messageText);
    }

    public String getCondition() {
        return getArguments().getString(KEY_CONDITION);
    }

    public void setCondition(String condition) {
        getArguments().putString(KEY_CONDITION, condition);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.message, container, false);
        TextView messageText = (TextView) layout.findViewById(R.id.message_text);
        messageText.setText(getMessageText());
        return layout;
    }
}
