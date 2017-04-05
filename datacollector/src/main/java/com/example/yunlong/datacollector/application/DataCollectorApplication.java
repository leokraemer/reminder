package com.example.yunlong.datacollector.application;

import android.app.Application;
import android.content.Context;

import com.example.yunlong.datacollector.models.LabelData;
import com.example.yunlong.datacollector.models.SensorDataSet;
import com.facebook.stetho.Stetho;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.interceptors.ParseLogInterceptor;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by Yunlong on 4/22/2016.
 */
public class DataCollectorApplication extends Application {

    public static boolean WIFI_NAME_ENABLED = true;
    public static boolean LOCATION_ENABLED = true;
    public static boolean INERTIAL_SENSOR_ENABLED = false;
    public static boolean ACTIVITY_ENABLED = true;
    public static boolean ENVIRONMENT_SENSOR_ENABLED = true;
    public static String ParseObjectTitle = "Test2017April";
    public static String BROADCAST_EVENT = "com.example.yunlong.datacollector";

    @Override
    public void onCreate() {
        super.onCreate();
        //realm
        Realm.init(this);
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder().build();
        Realm.setDefaultConfiguration(realmConfiguration);
        //stetho
        //Stetho.initializeWithDefaults(this);
        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
                        .build());
        //parse
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


}
