package com.example.yunlong.datacollector.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by Yunlong on 3/2/2016.
 */
public class MyMotion {

    MyMotionListener motionListener;
    SensorManager mSensorManager;
    Sensor mSensorAcc,mSensorRotation;
    float[] accData = {0f,0f,0f};
    float[] rotData = {0f,0f,0f};
    final SensorEventListener myListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    accData = event.values;
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    rotData = event.values;
                    break;
                default:
                    break;
            }

            if(motionListener != null) {
                motionListener.motionDataChanged(accData, rotData);
            }
        }
    };


    public MyMotion(Context context) {

        motionListener = (MyMotionListener)context;

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        mSensorManager.registerListener(myListener,mSensorAcc,SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(myListener,mSensorRotation,SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopMotionSensor(){
        mSensorManager.unregisterListener(myListener);
    }

}
