package com.example.yunlong.datacollector.application;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.example.yunlong.datacollector.models.LabelData;
import com.example.yunlong.datacollector.models.SensorDataSet;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.interceptors.ParseLogInterceptor;

/**
 * Created by Yunlong on 4/22/2016.
 */
public class DataCollectorApplication extends Application {

    public static boolean WIFI_NAME_ENABLED = true;
    public static boolean LOCATION__ENABLED = true;
    public static boolean INERTIAL_SENSOR_ENABLED = false;
    public static boolean ACTIVITY_ENABLED = true;
    public static boolean ENVIRONMENT_SENSOR_ENABLED = true;
    public static String ParseObjectTitle = "TestMay";
    public static String BROADCAST_EVENT = "com.example.yunlong.datacollector";

    @Override
    public void onCreate() {
        super.onCreate();

        ParseObject.registerSubclass(SensorDataSet.class);
        ParseObject.registerSubclass(LabelData.class);

        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("fingerprint") // should correspond to APP_ID env variable
                .clientKey(null)  // set explicitly unless clientKey is explicitly configured on Parse server
                .addNetworkInterceptor(new ParseLogInterceptor())
                        //.server("http://46.101.169.213:1337/parse/").build());  //digital ocean droplet server
                .server("http://134.34.226.148:1337/parse/")
                .enableLocalDataStore()
                .build());

        ParseUser.enableAutomaticUser();
        ParseACL defaultACL = new ParseACL();
        ParseACL.setDefaultACL(defaultACL, true);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
