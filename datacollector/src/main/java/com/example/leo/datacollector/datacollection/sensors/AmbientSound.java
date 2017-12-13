package com.example.leo.datacollector.datacollection.sensors;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Yunlong on 8/8/2017.
 */

public class AmbientSound {

    private MediaRecorder mRecorder = null;
    private static final String TAG = "AmbientSound";
    private Context context;
    private AmbientSoundListener ambientSoundListener;

    public AmbientSound(Context context) {
        this.context = context;
        this.ambientSoundListener = (AmbientSoundListener) context;
        start();
        //getAmbientSound();
    }

    private void checkPermission(Context context) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            Log.e(TAG, "Permission needed!!!");
        }
    }

    public void getAmbientSound() {
        //reset amplitude
        getAmplitude();
        //measure for 500 ms
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                double sound = getAmplitude();
                ambientSoundListener.onReceivedAmbientSound(sound);
                Log.i(TAG, "Sound: " + sound);
        /*        Calendar c = Calendar.getInstance();
                int seconds = c.get(Calendar.MILLISECOND);
                Log.i(TAG,"stop:" + seconds);*/
            }
        }, 1000);
    }

    private void start() {
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();

            mRecorder.reset();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.UNPROCESSED);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null");
            try {
                mRecorder.prepare();
                mRecorder.start();
            } catch (Exception e) {
                Log.e(TAG, "cannot start media recorder");
            }
        }
    }

    public void stop() {
        if (mRecorder != null) {
            try {
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }

        }
    }

    public double getAmplitude() {
        if (mRecorder != null)
            return mRecorder.getMaxAmplitude();
        else
            return -1;
    }
}
