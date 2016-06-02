package com.vrlearning;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.vr.sdk.audio.GvrAudioEngine;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    GvrAudioEngine gvrAudioEngine;
    private static final String SOUND_FILE = "vr_birds.wav";
    private volatile int soundId = GvrAudioEngine.INVALID_ID;

    private SensorManager mgr;
    private Sensor gyro;
    Sensor accelerometer;
    Sensor magnetometer;

    public float x=0,y=0,z=0;
    float[] mGravity;
    float[] mGeomagnetic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gvrAudioEngine = new GvrAudioEngine(this,GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
        mgr = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        gyro = mgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer = mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        final PagerAdapter adapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return VRFragment.newInstance("video_1.mp4");
                }
                return null;
            }
            @Override
            public int getCount() {
                return 1;
            }
        };

        assert viewPager != null;
        viewPager.setAdapter(adapter);

        gvrAudioEngine.update();

        gvrAudioEngine.preloadSoundFile(SOUND_FILE);
        soundId = gvrAudioEngine.createSoundObject(SOUND_FILE);
        gvrAudioEngine.setHeadPosition(500*x,500*y,500*z);
        gvrAudioEngine.setSoundObjectPosition(
                soundId, 0, 0, 0);
        gvrAudioEngine.setSoundVolume(soundId,50);
        gvrAudioEngine.playSound(soundId, true);


       // updateModelPosition();


        //checkGLError("onSurfaceCreated");
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

                updatePosition(orientation[0],orientation[2]);

                gvrAudioEngine.setSoundObjectPosition(
                        soundId,
                        x, y, z);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1)
    {
        //Do nothing.
    }

    @Override
    protected void onResume() {
        super.onResume();
        mgr.registerListener(this, gyro, SensorManager.SENSOR_DELAY_FASTEST);
        mgr.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mgr.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void updatePosition(float azimuth,float elevation){
        double azimuth_radian = Math.PI + (azimuth);
        double elevation_radian = Math.PI + (elevation);

        float r = 10;

        x = (float)(r * Math.sin(elevation_radian) * Math.cos(azimuth_radian));
        y = (float)(r * Math.sin(elevation_radian) * Math.sin(azimuth_radian));
        z = (float)(r * Math.cos(elevation_radian));

        Log.v("xyz",String.valueOf(azimuth)+String.valueOf(elevation));
    }

}

