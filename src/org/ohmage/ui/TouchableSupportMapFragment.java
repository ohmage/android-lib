
package org.ohmage.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.maps.SupportMapFragment;

import org.ohmage.ui.TouchableSupportMapFragment.TouchableWrapper.OnTouchListener;

public class TouchableSupportMapFragment extends SupportMapFragment {
    public View mOriginalContentView;
    public TouchableWrapper mTouchView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        mOriginalContentView = super.onCreateView(inflater, parent, savedInstanceState);

        mTouchView = new TouchableWrapper(getActivity());
        mTouchView.addView(mOriginalContentView);

        return mTouchView;
    }

    @Override
    public View getView() {
        return mOriginalContentView;
    }

    public void setOnTouchListener(OnTouchListener listener) {
        mTouchView.setOnTouchListener(listener);
    }

    public static class TouchableWrapper extends FrameLayout {

        public static interface OnTouchListener {
            public boolean onTouch(MotionEvent event);
        }

        private OnTouchListener mListener;

        public TouchableWrapper(Context context) {
            super(context);
        }

        public void setOnTouchListener(OnTouchListener listener) {
            mListener = listener;
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            if (mListener != null && mListener.onTouch(ev))
                return true;
            return super.dispatchTouchEvent(ev);

        }
    }
}
