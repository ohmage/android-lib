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

package org.ohmage.prompt.media;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.ohmage.library.R;
import org.ohmage.logprobe.Log;

import java.io.File;
import java.io.IOException;

public class AudioPrompt extends MediaPromptFragment {

    private static final String TAG = "AudioPrompt";

    private String mFileName = null;
    private int mDuration = 0;

    private ToggleButton mRecordButton = null;
    private MediaRecorder mRecorder = null;

    private ToggleButton mPlayButton = null;
    private MediaPlayer mPlayer = null;

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlaying();
                    mPlayButton.setChecked(false);
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        // Set max durations
        mRecorder.setMaxDuration(mDuration);
        mRecorder.setMaxFileSize(300 * 1024 * 1024);
        mRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {

            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {

                switch (what) {
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                        onRecord(false);
                        mRecordButton.setChecked(false);
                        ensurePlayButtonState();
                        Toast.makeText(
                                getActivity(),
                                getString((what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) ? R.string.audio_prompt_max_filesize_reached
                                        : R.string.audio_prompt_max_duration_reached),
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setRetainInstance(true);
        setFile(getMedia());
    }

    public void setFile(File file) {
        mFileName = file.getAbsolutePath();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.audio_recorder_layout, container, false);

        boolean checked = (mRecordButton != null) ? mRecordButton.isChecked() : false;
        mRecordButton = (ToggleButton) view.findViewById(R.id.record_button);
        mRecordButton.setChecked(checked);
        mRecordButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onRecord(mRecordButton.isChecked());
                ensurePlayButtonState();
            }
        });

        checked = (mPlayButton != null) ? mPlayButton.isChecked() : false;
        mPlayButton = (ToggleButton) view.findViewById(R.id.play_button);
        mPlayButton.setChecked(checked);
        mPlayButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onPlay(mPlayButton.isChecked());
            }
        });
        ensurePlayButtonState();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void ensurePlayButtonState() {
        mPlayButton.setEnabled(!mRecordButton.isChecked() && new File(mFileName).exists());
    }

    /**
     * The text to be displayed to the user if the prompt is considered
     * unanswered.
     */
    @Override
    public String getUnansweredPromptText() {
        return ("Please record audio of something before continuing.");
    }

    /**
     * Set the max duration in milliseconds
     */
    public void setMaxDuration(int duration) {
        mDuration = duration;
    }
}
