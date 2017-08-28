package com.example.yunlong.datacollector.services;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;

import com.example.yunlong.datacollector.application.DataCollectorApplication;
import com.example.yunlong.datacollector.sensors.AmbientSound;
import com.example.yunlong.datacollector.sensors.AmbientSoundListener;
import com.example.yunlong.datacollector.sensors.GoogleFitness;
import com.example.yunlong.datacollector.sensors.GoogleFitnessListener;
import com.example.yunlong.datacollector.sensors.GooglePlacesCaller;
import com.example.yunlong.datacollector.sensors.GooglePlacesListener;
import com.example.yunlong.datacollector.sensors.WeatherCaller;
import com.example.yunlong.datacollector.sensors.WeatherCallerListener;
import com.example.yunlong.datacollector.utils.TimeUtils;

import com.example.yunlong.datacollector.R;
import com.example.yunlong.datacollector.models.SensorDataSet;
import com.example.yunlong.datacollector.sensors.FoursquareCaller;
import com.example.yunlong.datacollector.sensors.MyActivity;
import com.example.yunlong.datacollector.sensors.MyActivityListener;
import com.example.yunlong.datacollector.sensors.MyEnvironmentSensor;
import com.example.yunlong.datacollector.sensors.MyEnvironmentSensorListener;
import com.example.yunlong.datacollector.sensors.FourSquareListener;
import com.example.yunlong.datacollector.sensors.MyLocation;
import com.example.yunlong.datacollector.sensors.MyLocationListener;
import com.example.yunlong.datacollector.sensors.MyMotion;
import com.example.yunlong.datacollector.sensors.MyMotionListener;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataCollectorService extends Service implements MyLocationListener, MyMotionListener, FourSquareListener,MyActivityListener,
        MyEnvironmentSensorListener, GooglePlacesListener, AmbientSoundListener, GoogleFitnessListener,WeatherCallerListener{

    public static final String TAG = "DataCollectorService";
    public static final int notificationID = 1001;
    private static final int miniSeconds = 5;
    private static final boolean useGooglePlaces = false;
    private int weatherUpdateCnt = 0;

    MyMotion myMotion;
    MyLocation myLocation;
    MyActivity myActivity;
    FoursquareCaller foursquareCaller;
    GooglePlacesCaller googlePlacesCaller;
    Location currentLocation;
    MyEnvironmentSensor myEnvironmentSensor;
    AmbientSound ambientSound;
    GoogleFitness googleFitness;
    WeatherCaller weatherCaller;

    double currentLatitude, currentLongitude, currentAccurate,currentAmbientSound;
    boolean ifLocationChanged, isRunning,isPreScreenOn,isCurrentScreenOn;
    String preActivity,currentActivity,preWifiName,currentWifiName,prePlaceName,currentPlaceName,currentLabel,currentWeatherCondition;
    float preAmbientLight,currentAmbientLight,currentTemperature;
    long preSteps,currentSteps;

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
            if(DataCollectorApplication.LOCATION_ENABLED) {
                myLocation = new MyLocation(this);
                foursquareCaller = new FoursquareCaller(this, currentLocation);
                googlePlacesCaller = new GooglePlacesCaller(this);
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

            if(DataCollectorApplication.GOOGLE_FITNESS_ENABLED) {
                googleFitness = new GoogleFitness(this);
                currentSteps = 0;
                preSteps = 0;
            }

            if(DataCollectorApplication.AMBIENT_SOUND_ENABLED) {
                ambientSound = new AmbientSound(this);
                currentAmbientSound = 0;
            }

            if(DataCollectorApplication.WEATHER_ENABLED) {
                weatherCaller = new WeatherCaller(this);
                currentWeatherCondition = "null";
                currentTemperature = 0;
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
        if(DataCollectorApplication.LOCATION_ENABLED) {
            stopLocationUpdate();
        }
        if(DataCollectorApplication.ACTIVITY_ENABLED) {
            stopActivityDetection();
        }
        if(DataCollectorApplication.ENVIRONMENT_SENSOR_ENABLED) {
            stopEnvironmentSensor();
        }
        if(DataCollectorApplication.GOOGLE_FITNESS_ENABLED){
            stopGoogleFitness();
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
                                if(weatherUpdateCnt==1){
                                    weatherCaller.getCurrentWeather();
                                }else if(weatherUpdateCnt==3600/miniSeconds){//update per hour
                                    weatherUpdateCnt=0;
                                }
                                weatherUpdateCnt++;
                                updateUnAutomaticData();
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
            if(useGooglePlaces){
                googlePlacesCaller.getCurrentPlace();
            }else {
                foursquareCaller.findPlaces();
            }
        } catch (Exception e) {
            Log.d(TAG, "getPlaces Exception");
        }

    }

    private void updateUnAutomaticData(){
        getWiFiName();
        checkScreenOn();
        ambientSound.getAmbientSound();
        googleFitness.readData();
    }
    private void getWiFiName(){
        try{
            WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            String wifi = wifiInfo.getSSID()+":"+wifiInfo.getBSSID()+ ":" +wifiInfo.getIpAddress()+":"+wifiInfo.getNetworkId()+":"+wifiInfo.getRssi();
            //String wifi = wifiInfo.getSSID()+":"+wifiInfo.getIpAddress()+":"+wifiInfo.getNetworkId();
            if(wifi == null){
                currentWifiName = "no wifi";
            }else {
                currentWifiName = wifi;
            }

        }catch (Exception e){
            Log.d(TAG,"get wifi name error");
        }

    }

    public void checkScreenOn() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            DisplayManager dm = (DisplayManager) getApplicationContext().getSystemService(Context.DISPLAY_SERVICE);
            for (Display display : dm.getDisplays()) {
                if (display.getState() == Display.STATE_OFF) {
                    isCurrentScreenOn = false;
                }else {
                    isCurrentScreenOn = true;
                }
            }
        } else {
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            //noinspection deprecation
            isCurrentScreenOn = pm.isScreenOn();
        }
    }

    private void uploadDataSet(){
        SensorDataSet sensorDataSet = new SensorDataSet();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String userName = sharedPreferences.getString("fingerprint_user_name", "userName");
        if(userName.equals("userName")){
            //Toast.makeText(context,"please type in your name in settings",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"please type in your name in settings");
        }else {
            sensorDataSet.setTitle(DataCollectorApplication.ParseObjectTitle);//change this later
            sensorDataSet.setAuthor(ParseUser.getCurrentUser());
            sensorDataSet.setUserName(userName);
            if(DataCollectorApplication.ACTIVITY_ENABLED) {
                sensorDataSet.setActivity(currentActivity);
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
            if(DataCollectorApplication.LOCATION_ENABLED) {
                Log.d(TAG,currentLatitude+"");
                Log.d(TAG,currentLongitude+"");
                Log.d(TAG,""+currentAccurate);
                sensorDataSet.setGPS(currentLatitude+","+currentLongitude+","+currentAccurate);
                sensorDataSet.setLocation(currentPlaceName);
            }
            if(DataCollectorApplication.GOOGLE_FITNESS_ENABLED){
                sensorDataSet.setSteps(currentSteps);
            }
            if(DataCollectorApplication.AMBIENT_SOUND_ENABLED){
                sensorDataSet.setAmbientSound(currentAmbientSound);
            }
            if(DataCollectorApplication.WEATHER_ENABLED){
                sensorDataSet.setWeather(currentWeatherCondition+":"+currentTemperature);
            }
            sensorDataSet.setTime(TimeUtils.getCurrentTimeStr());
            sensorDataSet.setLabel(currentLabel);
            sensorDataSet.setScreenState(isCurrentScreenOn);

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
    public void locationChanged(Location location) {
        if(location != null) {
            currentLocation = location;
            currentLatitude = location.getLatitude();
            currentLongitude = location.getLongitude();
            currentAccurate = location.getAccuracy();
            getPlaces();
        }
    }

    // google places handler
    @Override
    public void onReceivedPlaces(HashMap<String, Float> places) {
    //get the most likely one
        String placeName  = null;
        float probability = 0;
        for(Map.Entry<String, Float> pair : places.entrySet()){
            if((float)pair.getValue()>=probability) {
                probability = (float)pair.getValue();
                placeName = pair.getKey();
            }
        }

        if(placeName==null) {
            currentPlaceName = "null";
        }else {
            currentPlaceName = placeName + ":" + probability;
        }

        //get all potential places
/*        Iterator it = places.entrySet().iterator();
        String placeName  = "";
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            if((float)pair.getValue()>0) {
                placeName += pair.getKey() + " : " + pair.getValue() + ";";
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
        if(placeName.isEmpty()) {
            if(places.entrySet().size()>0){
                placeName = "Unknown Places";
            }else {
                placeName = "null";
            }
        }else {
            currentPlaceName = placeName;
        }*/

    }

    //foursquare place handler
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
    public void stopLocationUpdate() {
        myLocation.stopLocationUpdate();
        googlePlacesCaller.disconnect();
    }

    @Override
    public void motionDataChanged(float[] accData, float[] rotData) {

    }

    @Override
    public void stopMotionSensor() {
        myMotion.stopMotionSensor();
    }

    public void stopGoogleFitness(){
        googleFitness.disconnect();
    }

    @Override
    public void onReceivedAmbientSound(double volume) {
        currentAmbientSound = volume;
    }

    @Override
    public void onReceivedStepsCounter(long steps) {
        currentSteps = steps;
    }

    @Override
    public void onReceivedWeather(String condition, float temperature) {
        currentWeatherCondition = condition;
        currentTemperature = temperature;
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
        int change=0;
        if(!currentActivity.equals(preActivity)) {
            preActivity = currentActivity;
            change++;
        }
        if(currentSteps!=preSteps){
            preSteps = currentSteps;
            change++;
        }
        if(!currentWifiName.equals(preWifiName)){
            preWifiName = currentWifiName;
            change++;
        }
        if(!currentPlaceName.equals(prePlaceName)){
            prePlaceName = currentPlaceName;
            change++;
        }
        if(!isCurrentScreenOn == isPreScreenOn){
            isPreScreenOn = isCurrentScreenOn;
            change++;
        }
        if(change>0){
            return true;
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
