package com.example.leo.datacollector.datacollection.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by Yunlong on 4/23/2016.
 */
public class MyEnvironmentSensor implements SensorEventListener {

    SensorManager mSensorManager;
    Sensor mLight, mAmbientTemperature, mAmbientPressure, mRelativeHumidity, mProximity;
    Context context;
    public float light, temperature, pressure, humidity, proximity;
    MyEnvironmentSensorListener environmentSensorListener;

    public MyEnvironmentSensor(Context context) {
        this.context = context;
        environmentSensorListener = (MyEnvironmentSensorListener) context;
        initSensor();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_LIGHT:
                light = event.values[0];
                break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                temperature = event.values[0];
                break;
            case Sensor.TYPE_PRESSURE:
                pressure = event.values[0];
                break;
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                humidity = event.values[0];
                break;
            case Sensor.TYPE_PROXIMITY:
                proximity = event.values[0];
                break;
            default:
                break;
        }
        environmentSensorListener.environmentSensorDataChanged(light, temperature, pressure,
                humidity, proximity);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void initSensor() {
        // Get an instance of the sensor service, and use that to get an instance of
        // a particular sensor.
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mAmbientTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mAmbientPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mRelativeHumidity = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        registerListener();
    }

    public void registerListener() {
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mAmbientPressure, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mAmbientTemperature, SensorManager
                .SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mRelativeHumidity, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopEnvironmentSensor() {
        mSensorManager.unregisterListener(this);
    }
}
