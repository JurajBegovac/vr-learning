package com.vrlearning;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.widgets.video.VrVideoEventListener;
import com.google.vr.sdk.widgets.video.VrVideoView;
import com.vrlearning.model.Exercise;

import java.io.IOException;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link VRFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VRFragment extends Fragment implements SensorEventListener, OnPreparedListener {
    private static final String TAG = "VRFragment";

    private static final String ARG_EXERCISE = "arg_exercise";

    /**
     * Preserve the video's state and duration when rotating the phone. This improves
     * performance when rotating or reloading the video.
     */
    private static final String STATE_IS_PAUSED      = "isPaused";
    private static final String STATE_VIDEO_DURATION = "videoDuration";
    private static final String STATE_PROGRESS_TIME  = "progressTime";

    private Exercise mExercise;

    /**
     * The video view and its custom UI elements.
     */
    private VrVideoView videoWidgetView;

    private ImageView mPlayButton;
    /**
     * By default, the video will start playing as soon as it is loaded.
     */
    private boolean isPaused = false;

    private GvrAudioEngine gvrAudioEngine;

    private volatile int soundId = GvrAudioEngine.INVALID_ID;

    private SensorManager mgr;
    private Sensor        accelerometer;
    private Sensor        magnetometer;

    public float x = 0, y = 0, z = 0;
    float[] mGravity;
    float[] mGeomagnetic;

    private MediaPlayer mPlayer;

    private boolean mStartedForFirstTime = false;

    public VRFragment() {
        // Required empty public constructor
    }

    public static VRFragment newInstance(Exercise p_exercise) {
        VRFragment fragment = new VRFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_EXERCISE, p_exercise);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mExercise = getArguments().getParcelable(ARG_EXERCISE);
        }

        mgr = (SensorManager) getActivity().getSystemService(Activity.SENSOR_SERVICE);
        accelerometer = mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gvrAudioEngine = new GvrAudioEngine(getActivity(), GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
        gvrAudioEngine.update();
        gvrAudioEngine.preloadSoundFile(mExercise.bckgAudioName());
        soundId = gvrAudioEngine.createSoundObject(mExercise.bckgAudioName());
        gvrAudioEngine.setHeadPosition(500 * x, 500 * y, 500 * z);
        gvrAudioEngine.setSoundObjectPosition(soundId, 0, 0, 0);
        gvrAudioEngine.setSoundVolume(soundId, mExercise.bckgAudioVolume());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vr, container, false);
        mPlayButton = (ImageView) view.findViewById(R.id.play_btn);
        videoWidgetView = (VrVideoView) view.findViewById(R.id.video_view);

        // Add the restore state code here.
        if (savedInstanceState != null) {
            long progressTime = savedInstanceState.getLong(STATE_PROGRESS_TIME);
            videoWidgetView.seekTo(progressTime);

            isPaused = savedInstanceState.getBoolean(STATE_IS_PAUSED);
            mPlayButton.setVisibility(isPaused ? View.VISIBLE : View.INVISIBLE);
            if (isPaused) {
                videoWidgetView.pauseVideo();
                pause();
            }
        } else {
            try {
                if (videoWidgetView.getDuration() <= 0) {
                    videoWidgetView.loadVideoFromAsset(mExercise.videoName());
                }
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error opening video: " + e.getMessage(), Toast.LENGTH_LONG)
                        .show();
            }
            isPaused = true;
            if (videoWidgetView != null) {
                videoWidgetView.pauseVideo();
                pause();
            }
        }

        // Add the VrVideoView listener here
        videoWidgetView.setEventListener(new VrVideoEventListener() {
            /**
             * Called by video widget on the UI thread when it's done loading the video.
             */
            @Override
            public void onLoadSuccess() {
                mPlayButton.setVisibility(View.VISIBLE);
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
                    if (mStartedForFirstTime) {
                        resume();
                    } else {
                        mStartedForFirstTime = true;
                        start();
                    }
                } else {
                    videoWidgetView.pauseVideo();
                    pause();
                }
                isPaused = !isPaused;
                mPlayButton.setVisibility(isPaused ? View.VISIBLE : View.INVISIBLE);
            }

            @Override
            public void onNewFrame() {
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
    public void onResume() {
        super.onResume();
        // Resume the 3D rendering.
        videoWidgetView.resumeRendering();
        if (mStartedForFirstTime) resume();
        mgr.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mgr.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        pause();
        super.onPause();
        // Prevent the view from rendering continuously when in the background.
        videoWidgetView.pauseRendering();
        // If the video was playing when onPause() is called, the default behavior will be to pause
        // the video and keep it paused when onResume() is called.
        isPaused = true;
        mgr.unregisterListener(this);
    }

    @Override
    public void onDestroy() {
        // Destroy the widget and free memory.
        videoWidgetView.shutdown();
        mPlayer.release();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putLong(STATE_PROGRESS_TIME, videoWidgetView.getCurrentPosition());
        savedInstanceState.putLong(STATE_VIDEO_DURATION, videoWidgetView.getDuration());
        savedInstanceState.putBoolean(STATE_IS_PAUSED, isPaused);
        super.onSaveInstanceState(savedInstanceState);
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

//                if (gvrAudioEngine.isSoundPlaying(soundId))
                gvrAudioEngine.setSoundObjectPosition(soundId, x, y, z);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mPlayer.start();
        gvrAudioEngine.playSound(soundId, true);
    }

    private void start() {
        AssetFileDescriptor afd = null;
        try {
            afd = getActivity().getAssets().openFd(mExercise.audioName());
            mPlayer = new MediaPlayer();
            mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mPlayer.setOnPreparedListener(this);
            mPlayer.prepareAsync();
        } catch (IOException p_e) {
            p_e.printStackTrace();
        }
    }

    private void resume() {
        if (mPlayer != null && !mPlayer.isPlaying()) mPlayer.start();
        if (gvrAudioEngine != null && !gvrAudioEngine.isSoundPlaying(soundId)) gvrAudioEngine.resume();
    }

    private void pause() {
        if (mPlayer != null && mPlayer.isPlaying()) mPlayer.pause();
        if (gvrAudioEngine != null && gvrAudioEngine.isSoundPlaying(soundId)) gvrAudioEngine.pause();
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
