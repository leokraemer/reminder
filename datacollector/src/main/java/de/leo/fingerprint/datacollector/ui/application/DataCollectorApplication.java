package de.leo.fingerprint.datacollector.ui.application;

import android.app.Application;

import com.jakewharton.threetenabp.AndroidThreeTen;

/**
 * Created by Yunlong on 4/22/2016.
 */
public class DataCollectorApplication extends Application {

    public static boolean WIFI_NAME_ENABLED = true;
    public static boolean LOCATION_ENABLED = true;
    public static boolean ACCELEROMETER_MAGNETOMETER_GYROSCOPE_ORIENTATION_ENABLED = true;
    public static boolean ACTIVITY_ENABLED = true;
    public static boolean ENVIRONMENT_SENSOR_ENABLED = true;
    public static boolean GOOGLE_FITNESS_ENABLED = true;
    public static boolean AMBIENT_SOUND_ENABLED = true;
    public static boolean WEATHER_ENABLED = true;
    public static String BROADCAST_EVENT = "com.example.leo.datacollector";


    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);
    }
}