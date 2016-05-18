package com.example.yunlong.datacollector.services;

import android.app.Application;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.yunlong.datacollector.application.DataCollectorApplication;
import com.example.yunlong.datacollector.utils.TimeUtils;

import com.example.yunlong.datacollector.R;
import com.example.yunlong.datacollector.models.SensorDataSet;
import com.example.yunlong.datacollector.sensors.FoursquareCaller;
import com.example.yunlong.datacollector.sensors.MyActivity;
import com.example.yunlong.datacollector.sensors.MyActivityListener;
import com.example.yunlong.datacollector.sensors.MyEnvironmentSensor;
import com.example.yunlong.datacollector.sensors.MyEnvironmentSensorListener;
import com.example.yunlong.datacollector.sensors.MyFourSquareListener;
import com.example.yunlong.datacollector.sensors.MyLocation;
import com.example.yunlong.datacollector.sensors.MyLocationListener;
import com.example.yunlong.datacollector.sensors.MyMotion;
import com.example.yunlong.datacollector.sensors.MyMotionListener;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataCollectorService extends Service implements MyLocationListener, MyMotionListener, MyFourSquareListener,MyActivityListener,MyEnvironmentSensorListener {

    public static String TAG = "DataCollectorService";
    public static int notificationID = 1001;
    MyMotion myMotion;
    MyLocation myLocation;
    MyActivity myActivity;
    FoursquareCaller foursquareCaller;
    Location currentLocation;
    boolean ifLocationChanged;
    MyEnvironmentSensor myEnvironmentSensor;
    boolean isRunning;
    private int miniSeconds = 5;
    String preActivity,currentActivity,preWifiName,currentWifiName,prePlaceName,currentPlaceName,currentLabel;
    float preAmbientLight,currentAmbientLight;


    public DataCollectorService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"service started");
        if(!this.isRunning) {
            this.isRunning = true;
            if(DataCollectorApplication.LOCATION__ENABLED) {
                myLocation = new MyLocation(this);
                prePlaceName = "null";
                currentPlaceName = "null";
                ifLocationChanged = true;
            }
            if(DataCollectorApplication.INERTIAL_SENSOR_ENABLED) {
                myMotion = new MyMotion(this);
            }
            if(DataCollectorApplication.ACTIVITY_ENABLED) {
                myActivity = new MyActivity(this);
                preActivity = "null";
                currentActivity = "null";
            }
            if(DataCollectorApplication.ENVIRONMENT_SENSOR_ENABLED) {
                myEnvironmentSensor = new MyEnvironmentSensor(this);
                preAmbientLight = 0;
                currentAmbientLight = 0;
            }

            preWifiName = "null";
            currentWifiName = "null";
            currentLabel = "null";

            LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                    new IntentFilter(DataCollectorApplication.BROADCAST_EVENT));

            startScheduledUpdate();
            startNotification();
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        cancelNotification();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

        if(DataCollectorApplication.INERTIAL_SENSOR_ENABLED) {
            stopMotionSensor();
        }
        if(DataCollectorApplication.LOCATION__ENABLED) {
            stopLocationUpdate();
        }
        if(DataCollectorApplication.ACTIVITY_ENABLED) {
            stopActivityDetection();
        }
        if(DataCollectorApplication.ENVIRONMENT_SENSOR_ENABLED) {
            stopEnvironmentSensor();
        }
    }

    private void startScheduledUpdate(){
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        try {
                            if(isRunning) {
                                getWiFiName();
                                if(checkChange()){
                                    uploadDataSet();
                                }
                            }
                        }catch (Exception e){
                            System.err.println("error in executing: " + ". It will no longer be run!");
                            e.printStackTrace();
                            // and re throw it so that the Executor also gets this error so that it can do what it would
                            throw new RuntimeException(e);
                        }

                    }
                }, 0, miniSeconds, TimeUnit.SECONDS);
    }

    private void getPlaces(){
        try {
            foursquareCaller = new FoursquareCaller(this, currentLocation);
            foursquareCaller.findPlaces();
        } catch (Exception e) {
            Log.d(TAG, "Foursquare API Exception");
        }

    }
    private void getWiFiName(){
        try{
            WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            String name = wifiInfo.getSSID();
            if(name == null){
                currentWifiName = "no wifi";
            }else {
                currentWifiName = name;
            }

        }catch (Exception e){
            Log.d(TAG,"get wifi name error");
        }

    }

    private void uploadDataSet(){
        SensorDataSet sensorDataSet = new SensorDataSet();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String userName = sharedPreferences.getString("fingerprint_user_name", "userName");
        if(userName.equals("userName")){
            //Toast.makeText(context,"please type in your name in settings",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"please type in your name in settings");
            return;
        }else {
            sensorDataSet.setTitle(DataCollectorApplication.ParseObjectTitle);//change this later
            sensorDataSet.setAuthor(ParseUser.getCurrentUser());
            sensorDataSet.setUserName(userName);
            if(DataCollectorApplication.ACTIVITY_ENABLED) {
                sensorDataSet.setActivity(myActivity.getConfidentActivity());
            }
            if(DataCollectorApplication.WIFI_NAME_ENABLED) {
                sensorDataSet.setWifiName(currentWifiName);
            }
            if(DataCollectorApplication.ENVIRONMENT_SENSOR_ENABLED) {
                sensorDataSet.setHumidity(myEnvironmentSensor.humidity);
                sensorDataSet.setLight(myEnvironmentSensor.light);
                sensorDataSet.setPressure(myEnvironmentSensor.pressure);
                sensorDataSet.setTemperature(myEnvironmentSensor.temperature);
            }
            if(DataCollectorApplication.LOCATION__ENABLED) {
                sensorDataSet.setGPS(currentLocation.getLatitude()+","+currentLocation.getLongitude()+","+currentLocation.getAccuracy());
                sensorDataSet.setLocation(currentPlaceName);
            }
            sensorDataSet.setTime(TimeUtils.getCurrentTimeStr());
            sensorDataSet.setLabel(currentLabel);

            try {
                sensorDataSet.saveEventually(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if(e==null) {
                            Log.d("DataCollector", "parse save done");
                        }else {
                            Log.d("DataCollector", "parse save error");
                        }
                    }
                });
            }catch (Exception e){
                //Toast.makeText(context,"upload data exception",Toast.LENGTH_SHORT).show();
                Log.d(TAG,"upload data exception");
            }
        }
    }

    @Override
    public void activityUpdate(String activity) {
        currentActivity = activity;
    }

    @Override
    public void stopActivityDetection() {
        myActivity.removeActivityUpdates();
        myActivity.disconnect();
        myActivity.unregisterReceiver();
    }

    @Override
    public void environmentSensorDataChanged(float light, float temperature, float pressure, float humidity) {
        currentAmbientLight = light;
    }

    @Override
    public void stopEnvironmentSensor() {
        myEnvironmentSensor.stopEnvironmentSensor();
    }

    @Override
    public void placesFound(String place) {
        if(place !=null) {
            String[] places = place.split("\n");
            currentPlaceName = places[0];
            return;
        }
        currentPlaceName = "null";
    }

    @Override
    public void locationChanged(Location location) {
        if(location != null) {
            currentLocation = location;
            getPlaces();
        }
    }

    @Override
    public void stopLocationUpdate() {
        myLocation.stopLocationUpdate();
    }

    @Override
    public void motionDataChanged(float[] accData, float[] rotData) {

    }

    @Override
    public void stopMotionSensor() {
        myMotion.stopMotionSensor();
    }


    public void startNotification(){
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("DataCollector")
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentText("Background service is running.")
                .setSmallIcon(R.drawable.fp_s);

        mNotificationManager.notify(
                notificationID,
                mNotifyBuilder.build());
    }
    public void cancelNotification(){
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(notificationID);
    }

    private boolean checkChange(){
        if(!currentActivity.equals(preActivity)) {
            preActivity = currentActivity;
            return true;
        }
        if(!currentWifiName.equals(preWifiName)){
            preWifiName = currentWifiName;
            return true;
        }
        if(!currentPlaceName.equals(prePlaceName)){
            prePlaceName = currentPlaceName;
            return  true;
        }
        return false;
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentLabel = intent.getStringExtra("label");
            //Log.d("receiver", "Got message: " + message);
        }
    };
}
