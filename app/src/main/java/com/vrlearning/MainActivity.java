package com.vrlearning;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.widgets.video.VrVideoEventListener;
import com.google.vr.sdk.widgets.video.VrVideoView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG            = "VRFragment";

    private static final String ARG_VIDEO_NAME = "video_name";

    private static final String STATE_IS_PAUSED      = "isPaused";
    private static final String STATE_VIDEO_DURATION = "videoDuration";
    private static final String STATE_PROGRESS_TIME  = "progressTime";

    private String mVideoName;

    /**
     * The video view and its custom UI elements.
     */
    private VrVideoView videoWidgetView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = (TextView) findViewById(R.id.status_text);
        videoWidgetView = (VrVideoView) findViewById(R.id.video_view);

        mVideoName = "video_2.mp4";

        // Add the restore state code here.
        if (savedInstanceState != null) {
            long progressTime = savedInstanceState.getLong(STATE_PROGRESS_TIME);
            videoWidgetView.seekTo(progressTime);

            isPaused = savedInstanceState.getBoolean(STATE_IS_PAUSED);
            if (isPaused) {
                videoWidgetView.pauseVideo();
            }
        } else {
            try {
                if (videoWidgetView.getDuration() <= 0) {
                    videoWidgetView.loadVideoFromAsset(mVideoName);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error opening video: " + e.getMessage(), Toast.LENGTH_LONG)
                        .show();
            }
            isPaused = true;
            if (videoWidgetView != null) {
                videoWidgetView.pauseVideo();
            }
        }

        // Add the VrVideoView listener here
        videoWidgetView.setEventListener(new VrVideoEventListener() {
            /**
             * Called by video widget on the UI thread when it's done loading the video.
             */
            @Override
            public void onLoadSuccess() {
                Log.i(TAG, "Successfully loaded video " + videoWidgetView.getDuration());
                updateStatusText();
            }

            /**
             * Called by video widget on the UI thread on any asynchronous error.
             */
            @Override
            public void onLoadError(String errorMessage) {
                Toast.makeText(MainActivity.this, "Error loading video: " + errorMessage, Toast.LENGTH_LONG).show();
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

        gvrAudioEngine = new GvrAudioEngine(this, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
        gvrAudioEngineMeditation = new GvrAudioEngine(this, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);

        mgr = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        accelerometer = mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

//        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
//        final PagerAdapter adapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {
//            @Override
//            public Fragment getItem(int position) {
//                switch (position) {
//                    case 0:
//                        return VRFragment.newInstance("video_1.mp4");
//                }
//                return null;
//            }
//            @Override
//            public int getCount() {
//                return 1;
//            }
//        };
//
//        assert viewPager != null;
//        viewPager.setAdapter(adapter);

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
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        //Do nothing.
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
    }

    @Override
    protected void onResume() {
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

    public void updatePosition(float azimuth, float elevation) {
        double azimuth_radian = Math.PI + (azimuth);
        double elevation_radian = Math.PI + (elevation);

        float r = 10;

        x = (float) (r * Math.sin(elevation_radian) * Math.cos(azimuth_radian));
        y = (float) (r * Math.sin(elevation_radian) * Math.sin(azimuth_radian));
        z = (float) (r * Math.cos(elevation_radian));

        Log.v("xyz", String.valueOf(azimuth) + String.valueOf(elevation));
    }

    private void updateStatusText() {
        String status = isPaused ? "Paused: " : "Playing: ";
        statusText.setText(status);
    }

}

