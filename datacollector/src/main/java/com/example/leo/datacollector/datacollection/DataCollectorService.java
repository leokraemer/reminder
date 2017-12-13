package com.example.leo.datacollector.datacollection;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;

import com.example.leo.datacollector.R;
import com.example.leo.datacollector.application.DataCollectorApplication;
import com.example.leo.datacollector.database.SqliteDatabase;
import com.example.leo.datacollector.models.SensorDataSet;
import com.example.leo.datacollector.datacollection.sensors.AmbientSound;
import com.example.leo.datacollector.datacollection.sensors.AmbientSoundListener;
import com.example.leo.datacollector.datacollection.sensors.FourSquareListener;
import com.example.leo.datacollector.datacollection.sensors.FoursquareCaller;
import com.example.leo.datacollector.datacollection.sensors.GoogleFitness;
import com.example.leo.datacollector.datacollection.sensors.GoogleFitnessListener;
import com.example.leo.datacollector.datacollection.sensors.GooglePlacesCaller;
import com.example.leo.datacollector.datacollection.sensors.GooglePlacesListener;
import com.example.leo.datacollector.datacollection.sensors.MyActivity;
import com.example.leo.datacollector.datacollection.sensors.MyActivityListener;
import com.example.leo.datacollector.datacollection.sensors.MyEnvironmentSensor;
import com.example.leo.datacollector.datacollection.sensors.MyEnvironmentSensorListener;
import com.example.leo.datacollector.datacollection.sensors.MyLocation;
import com.example.leo.datacollector.datacollection.sensors.MyLocationListener;
import com.example.leo.datacollector.datacollection.sensors.MyMotion;
import com.example.leo.datacollector.datacollection.sensors.MyMotionListener;
import com.example.leo.datacollector.datacollection.sensors.WeatherCaller;
import com.example.leo.datacollector.datacollection.sensors.WeatherCallerListener;
import com.example.leo.datacollector.services.ActivitiesIntentService;
import com.google.android.gms.location.DetectedActivity;

import org.threeten.bp.LocalDateTime;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.example.leo.datacollector.activityRecording.RecordingActivityKt.RECORDING_ID;
import static com.example.leo.datacollector.utils.ConstantsKt.IS_RECORDING;
import static com.example.leo.datacollector.utils.ConstantsKt.START_RECORDING;
import static com.example.leo.datacollector.utils.ConstantsKt.STOP_RECORDING;
import static com.example.leo.datacollector.utils.UtilsKt.averageDequeue;

public class DataCollectorService extends Service implements MyLocationListener,
        MyMotionListener, FourSquareListener, MyActivityListener,
        MyEnvironmentSensorListener, GooglePlacesListener, AmbientSoundListener,
        GoogleFitnessListener, WeatherCallerListener {

    public static final String TAG = "DataCollectorService";
    public static final int notificationID = 1001;

    private static final int secondsUntilNextUpdate = 5;

    private final int numberOfSamples = 60 / secondsUntilNextUpdate * 5;
    private int sample = 0;
    //rolling buffer of sensorData
    private SensorDataSet[] sensorDataBuffer = new SensorDataSet[numberOfSamples];

    private static final boolean useGooglePlaces = false;
    public static final String ACTIVITY = "activity";
    public static final String TYPE = "type";
    public static final String MINUTES = "minutes";
    public static final String SNACK = "snack";
    public static final String DELETED = "deleted";
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

    double currentLatitude, currentLongitude, currentAccurate, currentAmbientSound;
    boolean ifLocationChanged, isRunning, isPreScreenOn, isCurrentScreenOn;
    String preWifiName, currentWifiName, prePlaceName, currentPlaceName, currentLabel,
            currentWeatherCondition;
    private DetectedActivity preActivity = new DetectedActivity(DetectedActivity.UNKNOWN, 0);
    private DetectedActivity currentActivity = new DetectedActivity(DetectedActivity.UNKNOWN, 0);
    float preAmbientLight, currentAmbientLight, currentTemperature;
    private long currentWeatherId = -1;
    long preSteps, currentSteps;
    private String userName;
    private SqliteDatabase db;
    private int recordingId = -1;

    public DataCollectorService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        db = SqliteDatabase.Companion.getInstance(getApplicationContext());
        currentWeatherId = db.getLatestWeather().id;
        Log.d(TAG, "service started");
        if (!this.isRunning) {
            startDataCollection();
        }
        if (intent != null && intent.getAction() != null)
            switch (intent.getAction()) {
                case (START_RECORDING): {
                    recordingId = intent.getIntExtra(RECORDING_ID, -1);
                    Intent answer = new Intent(START_RECORDING);
                    answer.putExtra(RECORDING_ID, recordingId);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(answer);
                    break;
                }
                case (STOP_RECORDING): {
                    recordingId = -1;
                    Intent answer = new Intent(STOP_RECORDING);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(answer);
                    break;
                }
                case (IS_RECORDING): {
                    Intent answer = new Intent(IS_RECORDING);
                    answer.putExtra(RECORDING_ID, recordingId);
                    answer.putExtra(IS_RECORDING, isRunning);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(answer);
                    break;
                }
            }
        return START_STICKY;
    }

    private void startDataCollection() {
        this.isRunning = true;
        if (DataCollectorApplication.LOCATION_ENABLED) {
            myLocation = new MyLocation(this);
            foursquareCaller = new FoursquareCaller(this, currentLocation);
            googlePlacesCaller = new GooglePlacesCaller(this);
            prePlaceName = "null";
            currentPlaceName = "null";
            ifLocationChanged = true;
        }
        if (DataCollectorApplication.ACCELEROMETER_MAGNETOMETER_GYROSCOPE_ORIENTATION_ENABLED) {
            myMotion = new MyMotion(this);
        }
        if (DataCollectorApplication.ACTIVITY_ENABLED) {
            myActivity = new MyActivity(this);
            preActivity = new DetectedActivity(DetectedActivity.UNKNOWN, 0);
            currentActivity = new DetectedActivity(DetectedActivity.UNKNOWN, 0);
        }
        if (DataCollectorApplication.ENVIRONMENT_SENSOR_ENABLED) {
            myEnvironmentSensor = new MyEnvironmentSensor(this);
            preAmbientLight = 0;
            currentAmbientLight = 0;
        }
        if (DataCollectorApplication.GOOGLE_FITNESS_ENABLED) {
            googleFitness = new GoogleFitness(this);
            currentSteps = 0;
            preSteps = 0;
        }
        if (DataCollectorApplication.AMBIENT_SOUND_ENABLED) {
            ambientSound = new AmbientSound(this);
            currentAmbientSound = 0;
        }
        if (DataCollectorApplication.WEATHER_ENABLED) {
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

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = false;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        userName = sharedPreferences.getString("fingerprint_user_name", "userName");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        cancelNotification();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

        if (DataCollectorApplication.ACCELEROMETER_MAGNETOMETER_GYROSCOPE_ORIENTATION_ENABLED) {
            stopMotionSensor();
        }
        if (DataCollectorApplication.LOCATION_ENABLED) {
            stopLocationUpdate();
        }
        if (DataCollectorApplication.ACTIVITY_ENABLED) {
            stopActivityDetection();
        }
        if (DataCollectorApplication.ENVIRONMENT_SENSOR_ENABLED) {
            stopEnvironmentSensor();
        }
        if (DataCollectorApplication.GOOGLE_FITNESS_ENABLED) {
            stopGoogleFitness();
        }
        if (ambientSound != null) {
            ambientSound.stop();
            ambientSound = null;
        }
    }

    private void startScheduledUpdate() {
        final Handler handler = new Handler();
        handler.post((new Runnable() {
            public void run() {
                //we assume that the operation finishes before it must be called again
                handler.postDelayed(this, TimeUnit.SECONDS.toMillis(secondsUntilNextUpdate));
                if (isRunning) {
                    if (weatherUpdateCnt == 1) {
                        weatherCaller.getCurrentWeather();
                    } else if (weatherUpdateCnt == 3600 / secondsUntilNextUpdate) {//update
                        // per hour
                        weatherUpdateCnt = 0;
                    }
                    weatherUpdateCnt++;
                    updateUnAutomaticData();
                    uploadDataSet();
                }
            }
        }));
    }

    private void getPlaces() {
        try {
            if (useGooglePlaces) {
                googlePlacesCaller.getCurrentPlace();
            } else {
                foursquareCaller.findPlaces();
            }
        } catch (Exception e) {
            Log.d(TAG, "getPlaces Exception");
        }

    }

    private void updateUnAutomaticData() {
        getWiFiName();
        checkScreenOn();
        ambientSound.getAmbientSound();
        googleFitness.readData();
    }

    private void getWiFiName() {
        try {
            WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context
                    .WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            String wifi = wifiInfo.getSSID() + ":" + wifiInfo.getBSSID() + ":" + wifiInfo
                    .getIpAddress() + ":" + wifiInfo.getNetworkId() + ":" + wifiInfo.getRssi();
            //String wifi = wifiInfo.getSSID()+":"+wifiInfo.getIpAddress()+":"+wifiInfo
            // .getNetworkId();
            if (wifiInfo.getSSID() == null) {
                currentWifiName = "null";
            } else {
                currentWifiName = wifi;
            }

        } catch (Exception e) {
            Log.d(TAG, "get wifi name error");
        }

    }

    public void checkScreenOn() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            DisplayManager dm = (DisplayManager) getApplicationContext().getSystemService(Context
                    .DISPLAY_SERVICE);
            for (Display display : dm.getDisplays()) {
                if (display.getState() == Display.STATE_OFF) {
                    isCurrentScreenOn = false;
                } else {
                    isCurrentScreenOn = true;
                }
            }
        } else {
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context
                    .POWER_SERVICE);
            //noinspection deprecation
            isCurrentScreenOn = pm.isScreenOn();
        }
    }

    float Rotation[] = new float[9];
    float Inclination[] = new float[9];

    private void uploadDataSet() {
        SensorDataSet sensorDataSet = new SensorDataSet(LocalDateTime.now(), "username");
        sensorDataSet.setRecordingId(recordingId);
        if (DataCollectorApplication.ACTIVITY_ENABLED) {
            sensorDataSet.setActivity(currentActivity);
        }
        if (DataCollectorApplication.WIFI_NAME_ENABLED) {
            sensorDataSet.setWifiName(currentWifiName);
        }
        if (DataCollectorApplication.ENVIRONMENT_SENSOR_ENABLED) {
            sensorDataSet.setHumidityPercent(myEnvironmentSensor.humidity);
            sensorDataSet.setAmbientLight(myEnvironmentSensor.light);
            sensorDataSet.setAirPressure(myEnvironmentSensor.pressure);
            sensorDataSet.setTemperature(myEnvironmentSensor.temperature);
        }
        if (DataCollectorApplication.LOCATION_ENABLED) {
            Log.d(TAG, currentLatitude + "");
            Log.d(TAG, currentLongitude + "");
            Log.d(TAG, "" + currentAccurate);
            Location location = new Location("gps");
            location.setLatitude(currentLatitude);
            location.setLongitude(currentLongitude);
            location.setAccuracy((float) currentAccurate);
            sensorDataSet.setGps(location);
            sensorDataSet.setLocation(currentPlaceName);
        }
        if (DataCollectorApplication.GOOGLE_FITNESS_ENABLED) {
            sensorDataSet.setStepsSinceLast(currentSteps);
        }
        if (DataCollectorApplication.AMBIENT_SOUND_ENABLED) {
            sensorDataSet.setAmbientSound(currentAmbientSound);
        }
        if (DataCollectorApplication.WEATHER_ENABLED) {
            sensorDataSet.setWeather(currentWeatherId);
        }
        if (DataCollectorApplication.ACCELEROMETER_MAGNETOMETER_GYROSCOPE_ORIENTATION_ENABLED) {
            float[] avgAcc = averageDequeue(accData);
            sensorDataSet.setAcc_x(avgAcc[0]);
            sensorDataSet.setAcc_y(avgAcc[1]);
            sensorDataSet.setAcc_z(avgAcc[2]);
            float[] avgRot = averageDequeue(rotData);
            sensorDataSet.setGyro_x(avgRot[0]);
            sensorDataSet.setGyro_y(avgRot[1]);
            sensorDataSet.setGyro_z(avgRot[2]);
            float[] avgMag = averageDequeue(magData);
            sensorDataSet.setMag_x(avgMag[0]);
            sensorDataSet.setMag_y(avgMag[1]);
            sensorDataSet.setMag_z(avgMag[2]);
            if (avgAcc != null && avgMag != null) {
                boolean success = SensorManager.getRotationMatrix(Rotation, Inclination, avgAcc,
                        avgMag);
                if (success) {
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(Rotation, orientation);
                    sensorDataSet.setAzimuth(orientation[0]); // orientation contains: azimut,
                    // pitch and roll
                    sensorDataSet.setPitch(orientation[1]);
                    sensorDataSet.setRoll(orientation[2]);
                }
            }
        }
        sensorDataSet.setScreenState(isCurrentScreenOn);
        sensorDataBuffer[sample] = sensorDataSet;
        sample++;
        if (sample >= sensorDataBuffer.length) {
            sample = 0;
        }
        db.insertSensorDataSet(sensorDataSet);
    }

    @Override
    public void activityUpdate(DetectedActivity activity) {
        currentActivity = activity;
    }

    @Override
    public void stopActivityDetection() {
        myActivity.removeActivityUpdates();
        myActivity.disconnect();
        myActivity.unregisterReceiver();
    }

    @Override
    public void environmentSensorDataChanged(float light, float temperature, float pressure,
                                             float humidity) {
        currentAmbientLight = light;
    }

    @Override
    public void stopEnvironmentSensor() {
        myEnvironmentSensor.stopEnvironmentSensor();
    }


    @Override
    public void locationChanged(Location location) {
        if (location != null) {
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
        String placeName = null;
        float probability = 0;
        for (Map.Entry<String, Float> pair : places.entrySet()) {
            if ((float) pair.getValue() >= probability) {
                probability = (float) pair.getValue();
                placeName = pair.getKey();
            }
        }

        if (placeName == null) {
            currentPlaceName = "null";
        } else {
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
        if (place != null) {
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


    private ArrayDeque<float[]> accData = new ArrayDeque<>(50);
    private ArrayDeque<float[]> rotData = new ArrayDeque<>(50);
    private ArrayDeque<float[]> magData = new ArrayDeque<>(50);

    @Override
    public void motionDataChanged(float[] accData, float[] rotData, float[] magData) {
        this.accData.add(accData);
        this.rotData.add(rotData);
        this.magData.add(magData);
    }

    @Override
    public void stopMotionSensor() {
        myMotion.stopMotionSensor();
    }

    public void stopGoogleFitness() {
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
    public void onReceivedWeather(String weather) {
        currentWeatherId = db.enterWeather(weather, currentWeatherId + 1);
    }

    public void startNotification() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("DataCollector")
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentText("Background service is running.")
                .setSmallIcon(R.drawable.fp_s).addAction(0, "10 minutes", PendingIntent
                        .getService(this, 19921, new Intent(this, ActivitiesIntentService.class)
                                .putExtra(TYPE, MINUTES), PendingIntent.FLAG_ONE_SHOT))
                .addAction(0, "activity", PendingIntent.getService(this, 19922, new Intent(this,
                        ActivitiesIntentService.class).putExtra(TYPE, ACTIVITY), PendingIntent
                        .FLAG_ONE_SHOT))
                .addAction(0, "snack", PendingIntent.getService(this, 19923, new Intent(this,
                        ActivitiesIntentService.class).putExtra(TYPE, SNACK), PendingIntent
                        .FLAG_ONE_SHOT));


        mNotificationManager.notify(
                notificationID,
                mNotifyBuilder.build());
    }

    public void cancelNotification() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(notificationID);
    }

    private boolean checkChange() {
        int change = 0;
        if (!currentActivity.equals(preActivity)) {
            preActivity = currentActivity;
            change++;
        }
        if (currentSteps != preSteps) {
            preSteps = currentSteps;
            change++;
        }
        if (!currentWifiName.equals(preWifiName)) {
            preWifiName = currentWifiName;
            change++;
        }
        if (!currentPlaceName.equals(prePlaceName)) {
            prePlaceName = currentPlaceName;
            change++;
        }
        if (!isCurrentScreenOn == isPreScreenOn) {
            isPreScreenOn = isCurrentScreenOn;
            change++;
        }
        if (change > 0) {
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
