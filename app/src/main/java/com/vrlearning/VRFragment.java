package com.vrlearning;


import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.widgets.video.VrVideoEventListener;
import com.google.vr.sdk.widgets.video.VrVideoView;

import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link VRFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VRFragment extends Fragment implements SensorEventListener {
    private static final String TAG            = "VRFragment";
    private static final String ARG_VIDEO_NAME = "video_name";

    /**
     * Preserve the video's state and duration when rotating the phone. This improves
     * performance when rotating or reloading the video.
     */
    private static final String STATE_IS_PAUSED      = "isPaused";
    private static final String STATE_VIDEO_DURATION = "videoDuration";
    private static final String STATE_PROGRESS_TIME  = "progressTime";

    private String mVideoName;

    /**
     * The video view and its custom UI elements.
     */
    private VrVideoView videoWidgetView;

    /**
     * Seeking UI & progress indicator. The seekBar's progress value represents milliseconds in the
     * video.
     */
    private SeekBar  seekBar;
    private TextView statusText;

    /**
     * By default, the video will start playing as soon as it is loaded.
     */
    private boolean isPaused = false;

    private GvrAudioEngine gvrAudioEngine;
    private GvrAudioEngine gvrAudioEngineMeditation;

    private static final String SOUND_FILE            = "vr_birds.wav";
    private volatile     int    soundId               = GvrAudioEngine.INVALID_ID;
    private static final String SOUND_FILE_MEDITATION = "focus.wav";

    private SensorManager mgr;
    private Sensor        accelerometer;
    private Sensor        magnetometer;

    public float x = 0, y = 0, z = 0;
    float[] mGravity;
    float[] mGeomagnetic;

    public VRFragment() {
        // Required empty public constructor
    }

    public static VRFragment newInstance(String param1) {
        VRFragment fragment = new VRFragment();
        Bundle args = new Bundle();
        args.putString(ARG_VIDEO_NAME, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mVideoName = getArguments().getString(ARG_VIDEO_NAME);
        }

        gvrAudioEngine = new GvrAudioEngine(getActivity(), GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
        gvrAudioEngineMeditation = new GvrAudioEngine(getActivity(), GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);

        mgr = (SensorManager) getActivity().getSystemService(Activity.SENSOR_SERVICE);
        accelerometer = mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        gvrAudioEngine.update();
        gvrAudioEngine.preloadSoundFile(SOUND_FILE);
        soundId = gvrAudioEngine.createSoundObject(SOUND_FILE);
        gvrAudioEngine.setHeadPosition(500 * x, 500 * y, 500 * z);
        gvrAudioEngine.setSoundObjectPosition(soundId, 0, 0, 0);
        gvrAudioEngine.setSoundVolume(soundId, 20);
        gvrAudioEngine.playSound(soundId, true);

        gvrAudioEngineMeditation.update();
        gvrAudioEngineMeditation.preloadSoundFile(SOUND_FILE_MEDITATION);
        int soundMeditationId = gvrAudioEngineMeditation.createSoundObject(SOUND_FILE_MEDITATION);
        gvrAudioEngineMeditation.setHeadPosition(500 * x, 500 * y, 500 * z);
        gvrAudioEngineMeditation.setSoundObjectPosition(
                soundMeditationId, 0, 0, 0);
        gvrAudioEngineMeditation.setSoundVolume(soundMeditationId, 1);
        gvrAudioEngineMeditation.playSound(soundMeditationId, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vr, container, false);
        seekBar = (SeekBar) view.findViewById(R.id.seek_bar);
        statusText = (TextView) view.findViewById(R.id.status_text);
        videoWidgetView = (VrVideoView) view.findViewById(R.id.video_view);

        // Add the restore state code here.
        if (savedInstanceState != null) {
            long progressTime = savedInstanceState.getLong(STATE_PROGRESS_TIME);
            videoWidgetView.seekTo(progressTime);
            seekBar.setMax((int) savedInstanceState.getLong(STATE_VIDEO_DURATION));
            seekBar.setProgress((int) progressTime);

            isPaused = savedInstanceState.getBoolean(STATE_IS_PAUSED);
            if (isPaused) {
                videoWidgetView.pauseVideo();
            }
        } else {
            seekBar.setEnabled(false);
            try {
                if (videoWidgetView.getDuration() <= 0) {
                    videoWidgetView.loadVideoFromAsset(mVideoName);
                }
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error opening video: " + e.getMessage(), Toast.LENGTH_LONG)
                        .show();
            }
            isPaused = true;
            if (videoWidgetView != null) {
                videoWidgetView.pauseVideo();
            }
        }

        // Add the seekbar listener here.
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // if the user changed the position, seek to the new position.
                if (fromUser) {
                    videoWidgetView.seekTo(progress);
                    updateStatusText();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // ignore for now.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // ignore for now.
            }
        });

        // Add the VrVideoView listener here
        videoWidgetView.setEventListener(new VrVideoEventListener() {
            /**
             * Called by video widget on the UI thread when it's done loading the video.
             */
            @Override
            public void onLoadSuccess() {
                Log.i(TAG, "Successfully loaded video " + videoWidgetView.getDuration());
                seekBar.setMax((int) videoWidgetView.getDuration());
                seekBar.setEnabled(true);
                updateStatusText();
            }

            /**
             * Called by video widget on the UI thread on any asynchronous error.
             */
            @Override
            public void onLoadError(String errorMessage) {
                Toast.makeText(getActivity(), "Error loading video: " + errorMessage, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error loading video: " + errorMessage);
            }

            @Override
            public void onClick() {
                if (isPaused) {
                    videoWidgetView.playVideo();
                } else {
                    videoWidgetView.pauseVideo();
                }

                isPaused = !isPaused;
                updateStatusText();
            }

            @Override
            public void onNewFrame() {
                updateStatusText();
                seekBar.setProgress((int) videoWidgetView.getCurrentPosition());
            }

            /**
             * Make the video play in a loop. This method could also be used to move to the next video in
             * a playlist.
             */
            @Override
            public void onCompletion() {
                videoWidgetView.seekTo(0);
            }

        });
        return view;
    }

    @Override
    public void onPause() {
        gvrAudioEngine.pause();
        gvrAudioEngineMeditation.pause();
        super.onPause();
        // Prevent the view from rendering continuously when in the background.
        videoWidgetView.pauseRendering();
        // If the video was playing when onPause() is called, the default behavior will be to pause
        // the video and keep it paused when onResume() is called.
        isPaused = true;
        mgr.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Resume the 3D rendering.
        videoWidgetView.resumeRendering();
        // Update the text to account for the paused video in onPause().
        updateStatusText();
        gvrAudioEngine.resume();
        gvrAudioEngineMeditation.resume();
        mgr.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mgr.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onDestroy() {
        // Destroy the widget and free memory.
        videoWidgetView.shutdown();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putLong(STATE_PROGRESS_TIME, videoWidgetView.getCurrentPosition());
        savedInstanceState.putLong(STATE_VIDEO_DURATION, videoWidgetView.getDuration());
        savedInstanceState.putBoolean(STATE_IS_PAUSED, isPaused);
        super.onSaveInstanceState(savedInstanceState);
    }

//    @Override
//    public void setUserVisibleHint(boolean isVisibleToUser) {
//        super.setUserVisibleHint(isVisibleToUser);
//
//        if (isVisibleToUser) {
//            try {
//                if (videoWidgetView.getDuration() <= 0) {
//                    videoWidgetView.loadVideoFromAsset("video_1.mp4");
//                }
//            } catch (Exception e) {
//                Toast.makeText(getActivity(), "Error opening video: " + e.getMessage(), Toast.LENGTH_LONG)
//                        .show();
//            }
//        } else {
//            isPaused = true;
//            if (videoWidgetView != null) {
//                videoWidgetView.pauseVideo();
//            }
//        }
//    }

    private void updateStatusText() {
        String status = (isPaused ? "Paused: " : "Playing: ") +
                String.format(Locale.getDefault(), "%.2f", videoWidgetView.getCurrentPosition() / 1000f) +
                " / " +
                videoWidgetView.getDuration() / 1000f +
                " seconds.";
        statusText.setText(status);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                updatePosition(orientation[0], orientation[2]);

                gvrAudioEngine.setSoundObjectPosition(
                        soundId,
                        x, y, z);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void updatePosition(float azimuth, float elevation) {
        double azimuth_radian = Math.PI + (azimuth);
        double elevation_radian = Math.PI + (elevation);

        float r = 10;

        x = (float) (r * Math.sin(elevation_radian) * Math.cos(azimuth_radian));
        y = (float) (r * Math.sin(elevation_radian) * Math.sin(azimuth_radian));
        z = (float) (r * Math.cos(elevation_radian));

        Log.v("xyz", String.valueOf(azimuth) + String.valueOf(elevation));
    }
}
